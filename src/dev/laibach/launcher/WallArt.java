package dev.laibach.launcher;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.Html;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The approved art of the day: one fan work from the Volkskunst (Volkart) gallery
 * on laibach.org, fetched at most once a day (or when asked for the next one) and
 * kept as a single cached file. Nothing is bundled with the app; the artist is
 * always credited next to the picture.
 */
final class WallArt {
    static final String SITE = "https://www.laibach.org/volkskunst/";
    private static final String GALLERY = SITE + "volkart/";
    private static final String PREFS = "wallart";
    private static final int MAX_HTML = 2 * 1024 * 1024;
    private static final int MAX_IMAGE = 12 * 1024 * 1024;

    private static final Pattern LINK = Pattern.compile("<a\\s[^>]*class=\"rl-gallery-link\"[^>]*>");
    private static final Pattern HREF = Pattern.compile(
            "href=\"(https://www\\.laibach\\.org/wp-content/uploads/[^\"]+\\.(?:jpe?g|png))\"",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern TITLE = Pattern.compile("title=\"([^\"]*)\"");
    /** "Title - From: Artist", "Title - Author: Artist", "Title - Artist" or only "Title". */
    private static final Pattern CREDIT = Pattern.compile(
            "(.*\\S)\\s+-\\s+(?:(?:from|author|by)\\s*:\\s*)?(\\S.*)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PAGE = Pattern.compile("/volkart/page/(\\d+)/");

    /** A work: the picture, its title and its author as the site gives them. */
    static final class Art {
        final Bitmap bitmap;
        final String title;
        final String author;

        Art(Bitmap bitmap, String title, String author) {
            this.bitmap = bitmap;
            this.title = title;
            this.author = author;
        }
    }

    private WallArt() {}

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static File file(Context c) {
        return new File(c.getFilesDir(), "wallart.jpg");
    }

    private static String today() {
        Calendar now = Calendar.getInstance();
        return now.get(Calendar.YEAR) + "-" + now.get(Calendar.DAY_OF_YEAR);
    }

    /** True when today's work has not been fetched yet. */
    static boolean due(Context c) {
        return !today().equals(prefs(c).getString("day", ""));
    }

    /** The cached work, or null when there is none yet. */
    static Art load(Context c) {
        Bitmap b = BitmapFactory.decodeFile(file(c).getPath());
        if (b == null) return null;
        SharedPreferences p = prefs(c);
        return new Art(b, p.getString("title", ""), p.getString("author", ""));
    }

    /** Fetches a random work from the gallery, caches it and returns it. Blocks; call off the main thread. */
    static Art fetch(Context c, int width) throws IOException {
        SharedPreferences p = prefs(c);
        Random random = new Random();
        int pages = p.getInt("pages", 0);
        String html = null;
        if (pages == 0 || !today().equals(p.getString("pagesDay", ""))) {
            html = text(GALLERY);
            pages = 1;
            Matcher m = PAGE.matcher(html);
            while (m.find()) pages = Math.max(pages, Integer.parseInt(m.group(1)));
            p.edit().putInt("pages", pages).putString("pagesDay", today()).apply();
        }
        int page = 1 + random.nextInt(pages);
        if (page != 1 || html == null) html = text(page == 1 ? GALLERY : GALLERY + "page/" + page + "/");

        List<String[]> works = new ArrayList<>();
        Matcher link = LINK.matcher(html);
        while (link.find()) {
            Matcher href = HREF.matcher(link.group());
            if (!href.find()) continue;
            Matcher title = TITLE.matcher(link.group());
            works.add(new String[]{href.group(1), title.find() ? title.group(1) : ""});
        }
        if (works.isEmpty()) throw new IOException("no works on page " + page);
        String last = p.getString("url", "");
        String[] work = works.get(random.nextInt(works.size()));
        if (work[0].equals(last) && works.size() > 1) work = works.get((works.indexOf(work) + 1) % works.size());

        String caption = Html.fromHtml(work[1], Html.FROM_HTML_MODE_LEGACY).toString()
                .replace('\u00A0', ' ').trim();
        Matcher credit = CREDIT.matcher(caption);
        boolean credited = credit.matches();
        String title = credited ? credit.group(1).trim() : caption;
        String author = credited ? credit.group(2).trim() : "";

        byte[] data = bytes(work[0], MAX_IMAGE);
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, data.length, bounds);
        if (bounds.outWidth <= 0) throw new IOException("not a picture: " + work[0]);
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = 1;
        while (bounds.outWidth / (opts.inSampleSize * 2) >= width) opts.inSampleSize *= 2;
        Bitmap b = BitmapFactory.decodeByteArray(data, 0, data.length, opts);
        if (b == null) throw new IOException("unreadable picture: " + work[0]);

        File tmp = new File(c.getFilesDir(), "wallart.tmp");
        try (OutputStream o = new FileOutputStream(tmp)) {
            b.compress(Bitmap.CompressFormat.JPEG, 88, o);
        }
        if (!tmp.renameTo(file(c))) throw new IOException("could not keep the picture");
        p.edit().putString("day", today()).putString("url", work[0])
                .putString("title", title).putString("author", author).apply();
        return new Art(b, title, author);
    }

    private static String text(String url) throws IOException {
        return new String(bytes(url, MAX_HTML), StandardCharsets.UTF_8);
    }

    private static byte[] bytes(String url, int limit) throws IOException {
        HttpURLConnection http = (HttpURLConnection) URI.create(url).toURL().openConnection();
        http.setConnectTimeout(10_000);
        http.setReadTimeout(15_000);
        http.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android) LaibachLauncher");
        try {
            if (http.getResponseCode() != 200) throw new IOException(url + ": HTTP " + http.getResponseCode());
            try (InputStream in = http.getInputStream()) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buf = new byte[16 * 1024];
                for (int n; (n = in.read(buf)) > 0; ) {
                    out.write(buf, 0, n);
                    if (out.size() > limit) throw new IOException(url + ": too large");
                }
                return out.toByteArray();
            }
        } finally {
            http.disconnect();
        }
    }
}
