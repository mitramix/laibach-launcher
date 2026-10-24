package dev.laibach.launcher;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The state broadcaster's feed: the channel's public Atom feeds read and
 * filtered, fetching over the network, and what the citizen keeps of it (where
 * each video was left off, and the watch-later list). Shared by Laibach TV and
 * the home screen's bulletins.
 */
final class Feed {
    static final String CHANNEL = "UCE1VRNkKm2u6xI_No7SCKVQ";
    /** Where each video was left off, and the watch-later list. */
    private static final String PREFS = "tv";

    /** One video in a feed. */
    static final class Video {
        String id, title, published;
        long views = -1;
    }

    private Feed() {}

    /** Reads an Atom feed from YouTube, keeping only the channel's own videos. */
    static List<Video> parse(String xml) throws Exception {
        XmlPullParser p = Xml.newPullParser();
        // Keep the prefixes: "yt:videoId", "media:statistics".
        p.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
        p.setInput(new StringReader(xml));
        List<Video> out = new ArrayList<>();
        Video entry = null;
        String channel = null;
        for (int e = p.getEventType(); e != XmlPullParser.END_DOCUMENT; e = p.next()) {
            if (e == XmlPullParser.START_TAG) {
                String tag = p.getName();
                if ("entry".equals(tag)) {
                    entry = new Video();
                    channel = null;
                } else if (entry != null) {
                    switch (tag) {
                        case "yt:videoId": entry.id = p.nextText().trim(); break;
                        case "yt:channelId": channel = p.nextText().trim(); break;
                        case "title": entry.title = plain(p.nextText()); break;
                        case "published": entry.published = p.nextText().trim(); break;
                        case "media:statistics":
                            try {
                                entry.views = Long.parseLong(p.getAttributeValue(null, "views"));
                            } catch (NumberFormatException ignored) {
                                // Unknown.
                            }
                            break;
                        default: break;
                    }
                }
            } else if (e == XmlPullParser.END_TAG && "entry".equals(p.getName()) && entry != null) {
                if (CHANNEL.equals(channel) && entry.id != null && entry.id.matches("[A-Za-z0-9_-]{11}")
                        && entry.title != null) {
                    out.add(entry);
                }
                entry = null;
            }
        }
        return out;
    }

    /** The title without the band's name in front: on this channel there is no other band. */
    private static String plain(String title) {
        String t = title.trim();
        String lower = t.toLowerCase(Locale.ROOT);
        for (String prefix : new String[]{"laibach - ", "laibach – ", "laibach — ", "laibach: "}) {
            if (lower.startsWith(prefix)) return t.substring(prefix.length()).trim();
        }
        return t;
    }

    /** The body at url, refused past limit bytes. */
    static byte[] bytes(String url, int limit) throws Exception {
        HttpURLConnection c = (HttpURLConnection) URI.create(url).toURL().openConnection();
        c.setConnectTimeout(10_000);
        c.setReadTimeout(15_000);
        c.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android) LaibachLauncher");
        try (InputStream in = c.getInputStream()) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[16384];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
                if (out.size() > limit) throw new IllegalStateException("too large");
            }
            return out.toByteArray();
        } finally {
            c.disconnect();
        }
    }

    // ---- Resume and watch later -----------------------------------------------

    static int position(Context context, String id) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt("at." + id, 0);
    }

    static void remember(Context context, String id, int seconds) {
        SharedPreferences p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (seconds <= 5) p.edit().remove("at." + id).apply();
        else p.edit().putInt("at." + id, seconds).apply();
    }

    /** The watch-later list, newest set aside first: lines of id, title, published. */
    static List<Video> later(Context context) {
        List<Video> out = new ArrayList<>();
        for (String line : context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("later", "").split("\n")) {
            String[] f = line.split("\t", -1);
            if (f.length < 3) continue;
            Video video = new Video();
            video.id = f[0];
            video.title = f[1];
            video.published = f[2].isEmpty() ? null : f[2];
            out.add(video);
        }
        return out;
    }

    static boolean isLater(Context context, String id) {
        for (Video video : later(context)) if (video.id.equals(id)) return true;
        return false;
    }

    /** Adds the video to the list, or takes it off; returns whether it is on it now. */
    static boolean toggleLater(Context context, Video video) {
        List<Video> later = later(context);
        boolean removed = later.removeIf(x -> x.id.equals(video.id));
        if (!removed) later.add(0, video);
        StringBuilder b = new StringBuilder();
        for (Video x : later) {
            if (b.length() > 0) b.append('\n');
            b.append(x.id).append('\t').append(x.title.replace('\t', ' ').replace('\n', ' '))
                    .append('\t').append(x.published == null ? "" : x.published);
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("later", b.toString()).apply();
        return !removed;
    }
}
