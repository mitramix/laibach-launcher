package dev.laibach.launcher;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Insets;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.util.TypedValue;
import android.util.Xml;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;

import org.xmlpull.v1.XmlPullParser;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Laibach TV: the state broadcaster, which carries one channel. Programmes are
 * the Laibach YouTube channel's uploads and its own playlists, read from
 * YouTube's public feeds (no account, no search, no comments, no
 * recommendations). Videos play in the embedded player from youtube-nocookie;
 * every way out of it to the wider YouTube is closed. A video whose owner does
 * not allow embedding can be opened, alone, in the YouTube app.
 */
public class BroadcastActivity extends StateActivity {
    static final String CHANNEL = "UCE1VRNkKm2u6xI_No7SCKVQ";
    private static final String FEEDS = "https://www.youtube.com/feeds/videos.xml?";
    /** The page the player is served from, so that YouTube sees where it is embedded. */
    private static final String ORIGIN = "https://dev.laibach.launcher";
    private static final long FRESH = 30 * 60_000L;

    /** The programme guide: English, German, the feed. Only the channel's own videos are shown. */
    private static final String[][] PROGRAMMES = {
            {"LATEST", "NEUESTE", "channel_id=" + CHANNEL},
            {"OFFICIAL VIDEOS", "OFFIZIELLE VIDEOS", "playlist_id=PLAOH3RTMh-JMVmtD-EbBLa3eSIgir5hgG"},
            {"FEATURED", "AUSGEWÄHLT", "playlist_id=PL086F2B9E26CB6545"},
            {"ALAMUT", "ALAMUT", "playlist_id=PLAOH3RTMh-JMdqYAWjG_4rlAF-ITNv4Ak"},
            {"MUSICK", "MUSICK", "playlist_id=PLAOH3RTMh-JNCATj0CIkliV5BhVxITXT8"},
            {"JESUS CHRIST SUPERSTARS", "JESUS CHRIST SUPERSTARS", "playlist_id=PLAOH3RTMh-JN4XvJvOXnDZMQjUaEnJdVO"},
            {"OPUS DEI", "OPUS DEI", "playlist_id=PLAOH3RTMh-JMNfHqyk8JiyXfOPjjjioTS"},
            {"EARLY VIDEOS", "FRÜHE VIDEOS", "playlist_id=PLAOH3RTMh-JNkm_a_3IOnDsKqMY_dPFhu"},
            {"REMIXES", "REMIXE", "playlist_id=PLAOH3RTMh-JNs5UEvEgPx2_Rudz0Bfda5"},
            {"DOCUMENTS", "DOKUMENTE", "playlist_id=PLAOH3RTMh-JOXSCijDUnuaUqyEiChd84j"},
            {"DOCUMENTARIES", "DOKUMENTARFILME", "playlist_id=PLAOH3RTMh-JMPZ6vNtFAEYbP_Chn8Pt1H"},
    };

    /** Opens the broadcaster on this video, playing (from the home screen's ON AIR strip). */
    static final String EXTRA_VIDEO_ID = "video", EXTRA_TITLE = "title";
    /** Where each video was left off, and the watch-later list. */
    private static final String PREFS = "tv";

    /** One video in a feed. */
    static final class Video {
        String id, title, published;
        long views = -1;
    }

    /** Feeds already received in this process, with when they were received. */
    private static final Map<String, List<Video>> guide = new HashMap<>();
    private static final Map<String, Long> received = new HashMap<>();
    private static final LruCache<String, Bitmap> stills = new LruCache<>(60);
    private static int programme;
    /** The chip after the programmes: the citizen's own list. */
    private static final int LATER = PROGRAMMES.length;

    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService fetch = Executors.newFixedThreadPool(3);
    private FrameLayout glass, screen;
    private LinearLayout chips, list;
    private TextView standby, caption, meta, signal;
    private WebView player;
    private Video playing;
    private View fullscreen;
    private WebChromeClient.CustomViewCallback leaveFullscreen;
    private final OnBackInvokedCallback backFromFullscreen = this::closeFullscreen;
    private final ColorMatrixColorFilter grey;

    {
        ColorMatrix m = new ColorMatrix();
        m.setSaturation(0);
        grey = new ColorMatrixColorFilter(m);
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        // Watched through the same glass as the home screen.
        glass = new FrameLayout(this);
        glass.addView(buildUi());
        glass.addView(new Crt(this, v));
        wear(glass);
        tune(programme);
        playFrom(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        playFrom(intent);
    }

    /** Plays the video the intent names, if any. */
    private void playFrom(Intent intent) {
        String id = intent == null ? null : intent.getStringExtra(EXTRA_VIDEO_ID);
        if (id == null || !id.matches("[A-Za-z0-9_-]{11}")) return;
        intent.removeExtra(EXTRA_VIDEO_ID);
        Video video = new Video();
        video.id = id;
        String title = intent.getStringExtra(EXTRA_TITLE);
        video.title = title == null ? "" : title;
        play(video);
    }

    @Override
    protected void onPause() {
        // The broadcast does not follow the citizen out of the room.
        if (player != null) {
            player.evaluateJavascript("if (window.player && player.pauseVideo) player.pauseVideo();", null);
            player.onPause();
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (player != null) player.onResume();
    }

    @Override
    protected void onDestroy() {
        switchOff();
        fetch.shutdownNow();
        super.onDestroy();
    }

    // ---- Layout -------------------------------------------------------------

    private View buildUi() {
        LinearLayout root = column();
        LinearLayout content = column();
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            view.setPadding(bars.left + dp(20), bars.top + dp(6), bars.right + dp(20), 0);
            content.setPadding(0, 0, 0, bars.bottom + dp(24));
            return WindowInsets.CONSUMED;
        });

        LinearLayout head = row();
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = type(26, v.accent, 25, 1000);
        back.setText("◀");
        back.setPadding(0, 0, dp(12), 0);
        back.setOnClickListener(view -> finish());
        head.addView(back);
        TextView title = type(30, v.ink, 25, 1000);
        title.setText("LAIBACH TV");
        title.setLetterSpacing(0.14f);
        head.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView seal = type(14, v.accent, 40, 900);
        seal.setText(t("ONE CHANNEL", "EIN SENDER"));
        seal.setLetterSpacing(0.16f);
        head.addView(seal);
        root.addView(head);
        root.addView(rule(v.accent), ruleParams(dp(5), dp(6), dp(14)));

        // The screen, always 16:9; the player is put in it when a programme is chosen.
        screen = new FrameLayout(this) {
            @Override
            protected void onMeasure(int widthSpec, int heightSpec) {
                int w = MeasureSpec.getSize(widthSpec);
                super.onMeasure(widthSpec, MeasureSpec.makeMeasureSpec(w * 9 / 16, MeasureSpec.EXACTLY));
            }
        };
        screen.setBackgroundColor(0xFF000000);
        standby = type(15, v.accent, 40, 900);
        standby.setLetterSpacing(0.16f);
        standby.setGravity(Gravity.CENTER);
        standby.setLineSpacing(dp(4), 1f);
        standby.setPadding(dp(16), dp(16), dp(16), dp(16));
        screen.addView(standby, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        standby(t("STAND BY FOR TRANSMISSION", "BITTE WARTEN SIE AUF DIE SENDUNG"));
        root.addView(screen, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        caption = type(17, v.ink, 40, 900);
        caption.setLetterSpacing(0.04f);
        caption.setMaxLines(2);
        caption.setPadding(0, dp(10), 0, 0);
        caption.setText(t("NO PROGRAMME SELECTED", "KEIN PROGRAMM GEWÄHLT"));
        root.addView(caption);
        meta = type(11, v.muted, 70, 700);
        meta.setLetterSpacing(0.14f);
        meta.setText(t("THE STATE BROADCASTS. THE CITIZEN RECEIVES.", "DER STAAT SENDET. DER BÜRGER EMPFÄNGT."));
        root.addView(meta);

        // The programme guide, one chip per playlist.
        HorizontalScrollView strip = new HorizontalScrollView(this);
        strip.setHorizontalScrollBarEnabled(false);
        chips = row();
        strip.addView(chips);
        LinearLayout.LayoutParams stripAt = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        stripAt.topMargin = dp(14);
        root.addView(strip, stripAt);
        for (int i = 0; i <= LATER; i++) {
            int index = i;
            TextView chip = type(12, v.accent, 60, 900);
            chip.setText(i == LATER ? t("★ WATCH LATER", "★ SPÄTER ANSEHEN") : Lang.pick(german, PROGRAMMES[i]));
            chip.setLetterSpacing(0.14f);
            chip.setPadding(dp(12), dp(9), dp(12), dp(9));
            chip.setOnClickListener(view -> tune(index));
            LinearLayout.LayoutParams at = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            at.rightMargin = dp(8);
            chips.addView(chip, at);
        }
        root.addView(rule(v.rule), ruleParams(Math.max(1, dp(1)), dp(12), 0));

        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        signal = type(12, v.muted, 70, 700);
        signal.setLetterSpacing(0.14f);
        signal.setPadding(0, dp(12), 0, dp(4));
        content.addView(signal);
        list = column();
        content.addView(list);
        TextView footer = type(11, v.muted, 70, 700);
        footer.setLetterSpacing(0.14f);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, dp(18), 0, 0);
        footer.setText(t("✚ OTHER CHANNELS HAVE BEEN DISCONTINUED ✚", "✚ ANDERE SENDER WURDEN EINGESTELLT ✚"));
        content.addView(footer);
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        return root;
    }

    /** Shows a message on the empty screen. */
    private void standby(String message) {
        standby.setText(message + "\n▮ ▮ ▮");
        standby.setVisibility(View.VISIBLE);
    }

    // ---- The programme guide --------------------------------------------------

    /** Switches the guide to programme p, from memory if it was received lately. */
    private void tune(int p) {
        programme = p;
        for (int i = 0; i < chips.getChildCount(); i++) {
            TextView chip = (TextView) chips.getChildAt(i);
            GradientDrawable frame = new GradientDrawable();
            frame.setStroke(Math.max(1, dp(1.5f)), v.accent);
            if (i == p) frame.setColor(v.accent);
            chip.setBackground(frame);
            chip.setTextColor(i == p ? v.bg : v.accent);
        }
        if (p == LATER) {
            List<Video> later = later();
            if (later.isEmpty()) {
                list.removeAllViews();
                signal.setOnClickListener(null);
                signal.setText(t("NOTHING SET ASIDE. HOLD A TRANSMISSION TO KEEP IT FOR LATER.",
                        "NICHTS ZURÜCKGELEGT. EINE SENDUNG GEDRÜCKT HALTEN, UM SIE FÜR SPÄTER ZU BEHALTEN."));
            } else {
                show(later);
            }
            return;
        }
        String feed = PROGRAMMES[p][2];
        Long at = received.get(feed);
        if (at != null && System.currentTimeMillis() - at < FRESH) {
            show(guide.get(feed));
            return;
        }
        list.removeAllViews();
        signal.setText(t("RECEIVING THE PROGRAMME ...", "PROGRAMM WIRD EMPFANGEN ..."));
        signal.setOnClickListener(null);
        fetch.execute(() -> {
            List<Video> videos;
            try {
                videos = parse(new String(bytes(FEEDS + feed, 2_000_000), StandardCharsets.UTF_8));
            } catch (Exception e) {
                videos = null;
            }
            List<Video> got = videos;
            main.post(() -> {
                if (isDestroyed() || programme != p) return;
                if (got == null) {
                    signal.setText(t("NO SIGNAL. THE STATE'S CABLE IS DOWN. TAP TO TRY AGAIN.",
                            "KEIN SIGNAL. DAS KABEL DES STAATES IST UNTERBROCHEN. ZUM WIEDERHOLEN TIPPEN."));
                    signal.setOnClickListener(view -> tune(p));
                    return;
                }
                guide.put(feed, got);
                received.put(feed, System.currentTimeMillis());
                show(got);
            });
        });
    }

    /** Lists a programme's videos. */
    private void show(List<Video> videos) {
        signal.setOnClickListener(null);
        signal.setText(String.format(locale, t("%d TRANSMISSIONS ON THIS PROGRAMME", "%d SENDUNGEN IN DIESEM PROGRAMM"),
                videos.size()));
        list.removeAllViews();
        for (Video video : videos) list.addView(item(video));
    }

    private View item(Video video) {
        LinearLayout row = row();
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(8), 0, dp(8));
        row.setTag(video.id);
        ImageView still = new ImageView(this);
        still.setScaleType(ImageView.ScaleType.CENTER_CROP);
        still.setBackgroundColor(0xFF000000);
        still.setColorFilter(video == playing ? null : grey);
        row.addView(still, new LinearLayout.LayoutParams(dp(128), dp(72)));
        Bitmap cached = stills.get(video.id);
        if (cached != null) {
            still.setImageBitmap(cached);
        } else {
            fetch.execute(() -> {
                try {
                    byte[] b = bytes("https://i.ytimg.com/vi/" + video.id + "/mqdefault.jpg", 500_000);
                    Bitmap bmp = BitmapFactory.decodeByteArray(b, 0, b.length);
                    if (bmp == null) return;
                    stills.put(video.id, bmp);
                    main.post(() -> still.setImageBitmap(bmp));
                } catch (Exception ignored) {
                    // The still stays black.
                }
            });
        }

        LinearLayout words = column();
        words.setPadding(dp(12), 0, 0, 0);
        TextView name = type(14, video == playing ? v.accent : v.ink, 40, 800);
        name.setText(video.title.toUpperCase(locale));
        name.setMaxLines(3);
        words.addView(name);
        TextView facts = type(10, v.muted, 70, 700);
        facts.setLetterSpacing(0.12f);
        facts.setPadding(0, dp(4), 0, 0);
        facts.setText(facts(video));
        words.addView(facts);
        row.addView(words, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.setOnClickListener(view -> play(video));
        row.setOnLongClickListener(view -> {
            boolean kept = toggleLater(video);
            Toast.makeText(this, kept ? t("SET ASIDE FOR LATER.", "FÜR SPÄTER ZURÜCKGELEGT.")
                    : t("REMOVED FROM WATCH LATER.", "VON SPÄTER ANSEHEN ENTFERNT."), Toast.LENGTH_SHORT).show();
            if (programme == LATER) tune(LATER);
            else restyle(video);
            return true;
        });
        return row;
    }

    private String facts(Video video) {
        StringBuilder b = new StringBuilder();
        if (isLater(video.id)) b.append("★  ");
        try {
            b.append(OffsetDateTime.parse(video.published)
                    .format(DateTimeFormatter.ofPattern("d MMM yyyy", locale)).toUpperCase(locale));
        } catch (Exception e) {
            b.append(video.published == null ? "" : video.published);
        }
        if (video.views >= 0) {
            b.append("  ·  ").append(NumberFormat.getIntegerInstance(locale).format(video.views))
                    .append(t(" VIEWS", " AUFRUFE"));
        }
        int at = position(video.id);
        if (at > 0) {
            b.append("  ·  ").append(t("RESUMES AT ", "WEITER BEI "))
                    .append(String.format(Locale.ROOT, "%d:%02d", at / 60, at % 60));
        }
        return b.toString();
    }

    // ---- The player -----------------------------------------------------------

    private void play(Video video) {
        Video before = playing;
        playing = video;
        restyle(before);
        restyle(video);
        caption.setText(video.title.toUpperCase(locale));
        meta.setText(t("NOW TRANSMITTING", "JETZT AUF SENDUNG") + "  ·  " + facts(video));
        Record.note(this, "TRANSMISSION RECEIVED: " + video.title.toUpperCase(Locale.ROOT),
                "SENDUNG EMPFANGEN: " + video.title.toUpperCase(Locale.GERMAN));
        if (player == null) player = newPlayer();
        standby.setVisibility(View.GONE);
        player.setVisibility(View.VISIBLE);
        player.loadDataWithBaseURL(ORIGIN + "/", page(video.id, position(video.id)), "text/html", "utf-8", null);
    }

    /** Redraws a video's row after it starts or stops playing. */
    private void restyle(Video video) {
        if (video == null) return;
        for (int i = 0; i < list.getChildCount(); i++) {
            View row = list.getChildAt(i);
            if (!video.id.equals(row.getTag())) continue;
            list.removeViewAt(i);
            list.addView(item(video), i);
            return;
        }
    }

    private WebView newPlayer() {
        WebView web = new WebView(this);
        web.setBackgroundColor(0xFF000000);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setSupportMultipleWindows(false);
        s.setJavaScriptCanOpenWindowsAutomatically(false);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        web.addJavascriptInterface(new Receiver(), "TV");
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (permitted(request)) return false;
                // The logo, the title, "watch on YouTube", the end screen: all lead out. None may.
                Toast.makeText(BroadcastActivity.this, t("THE WIDER YOUTUBE IS NOT PERMITTED.",
                        "DAS WEITERE YOUTUBE IST NICHT GESTATTET."), Toast.LENGTH_SHORT).show();
                return true;
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                openFullscreen(view, callback);
            }

            @Override
            public void onHideCustomView() {
                closeFullscreen();
            }
        });
        screen.addView(web, 0, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        return web;
    }

    /** Only the player itself may load; everything else is the wider YouTube. */
    private static boolean permitted(WebResourceRequest request) {
        Uri u = request.getUrl();
        String host = u.getHost() == null ? "" : u.getHost();
        String path = u.getPath() == null ? "" : u.getPath();
        if (request.isForMainFrame()) return u.toString().startsWith(ORIGIN) || "about".equals(u.getScheme());
        return (host.equals("www.youtube-nocookie.com") || host.equals("www.youtube.com")) && path.startsWith("/embed/");
    }

    /** The player page: the IFrame API, no related videos, no annotations, reports back to Receiver. */
    private static String page(String id, int start) {
        return "<!doctype html><html><head><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
                + "<style>html,body{margin:0;height:100%;background:#000;overflow:hidden}#p{width:100%;height:100%}</style>"
                + "</head><body><div id=\"p\"></div>"
                + "<script>"
                + "var player;"
                + "function onYouTubeIframeAPIReady(){player=new YT.Player('p',{host:'https://www.youtube-nocookie.com',"
                + "videoId:'" + id + "',playerVars:{rel:0,playsinline:1,iv_load_policy:3,autoplay:1,fs:1,start:" + start + ","
                + "origin:'" + ORIGIN + "'},events:{"
                + "onError:function(e){TV.error(e.data)},"
                + "onStateChange:function(e){TV.state(e.data)}}});"
                // Where the citizen is, every five seconds, so the broadcast resumes there.
                + "setInterval(function(){if(player&&player.getCurrentTime&&player.getPlayerState()==1)"
                + "TV.at(player.getCurrentTime(),player.getDuration())},5000);}"
                + "</script><script src=\"https://www.youtube.com/iframe_api\"></script>"
                + "</body></html>";
    }

    /** The player's reports; they arrive on a background thread. */
    private final class Receiver {
        @JavascriptInterface
        public void error(int code) {
            main.post(() -> refused(code));
        }

        @JavascriptInterface
        public void at(double seconds, double duration) {
            Video video = playing;
            if (video == null) return;
            // The last half minute counts as seen; seen videos start from the beginning again.
            int at = duration > 0 && seconds > duration - 30 ? 0 : (int) seconds;
            remember(video.id, at);
        }

        @JavascriptInterface
        public void state(int state) {
            // 0 is the end. YouTube would offer more; the state offers silence.
            Video video = playing;
            if (state == 0 && video != null) remember(video.id, 0);
            if (state == 0) main.post(() -> {
                switchOff();
                standby(t("TRANSMISSION ENDED.\nREMAIN TUNED.", "SENDUNG BEENDET.\nBLEIBEN SIE EMPFANGSBEREIT."));
            });
        }
    }

    /** The video will not play here; for an embedding refusal, offer it alone in YouTube. */
    private void refused(int code) {
        Video video = playing;
        switchOff();
        boolean embedding = code == 101 || code == 150 || code == 153;
        if (!embedding || video == null) {
            standby(t("SIGNAL LOST. THIS TRANSMISSION IS UNAVAILABLE.", "SIGNAL VERLOREN. DIESE SENDUNG IST NICHT VERFÜGBAR."));
            return;
        }
        standby(t("TRANSMISSION RESTRICTED BY THE BROADCASTER.\nTAP TO OPEN THIS VIDEO IN YOUTUBE ▶",
                "SENDUNG VOM SENDER EINGESCHRÄNKT.\nTIPPEN, UM DIESES VIDEO IN YOUTUBE ZU ÖFFNEN ▶"));
        standby.setOnClickListener(view -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=" + video.id)));
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, t("NO RECEIVER FOR THIS SIGNAL.", "KEIN EMPFÄNGER FÜR DIESES SIGNAL."),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /** Takes the player off the screen and frees it. */
    private void switchOff() {
        closeFullscreen();
        standby.setOnClickListener(null);
        if (player == null) return;
        screen.removeView(player);
        player.destroy();
        player = null;
    }

    private void openFullscreen(View view, WebChromeClient.CustomViewCallback callback) {
        if (fullscreen != null) {
            callback.onCustomViewHidden();
            return;
        }
        fullscreen = view;
        leaveFullscreen = callback;
        view.setBackgroundColor(0xFF000000);
        glass.addView(view, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        getWindow().getInsetsController().hide(WindowInsets.Type.systemBars());
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT, backFromFullscreen);
    }

    private void closeFullscreen() {
        if (fullscreen == null) return;
        glass.removeView(fullscreen);
        fullscreen = null;
        WebChromeClient.CustomViewCallback done = leaveFullscreen;
        leaveFullscreen = null;
        getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(backFromFullscreen);
        getWindow().getInsetsController().show(WindowInsets.Type.navigationBars());
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        if (done != null) done.onCustomViewHidden();
    }

    // ---- Resume and watch later -----------------------------------------------

    private int position(String id) {
        return getSharedPreferences(PREFS, MODE_PRIVATE).getInt("at." + id, 0);
    }

    private void remember(String id, int seconds) {
        android.content.SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);
        if (seconds <= 5) p.edit().remove("at." + id).apply();
        else p.edit().putInt("at." + id, seconds).apply();
    }

    /** The watch-later list, newest set aside first: lines of id, title, published. */
    private List<Video> later() {
        List<Video> out = new ArrayList<>();
        for (String line : getSharedPreferences(PREFS, MODE_PRIVATE).getString("later", "").split("\n")) {
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

    private boolean isLater(String id) {
        for (Video video : later()) if (video.id.equals(id)) return true;
        return false;
    }

    /** Adds the video to the list, or takes it off; returns whether it is on it now. */
    private boolean toggleLater(Video video) {
        List<Video> later = later();
        boolean removed = later.removeIf(x -> x.id.equals(video.id));
        if (!removed) later.add(0, video);
        StringBuilder b = new StringBuilder();
        for (Video x : later) {
            if (b.length() > 0) b.append('\n');
            b.append(x.id).append('\t').append(x.title.replace('\t', ' ').replace('\n', ' '))
                    .append('\t').append(x.published == null ? "" : x.published);
        }
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString("later", b.toString()).apply();
        return !removed;
    }

    // ---- The feed -------------------------------------------------------------

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

    // ---- Helpers ------------------------------------------------------------

}
