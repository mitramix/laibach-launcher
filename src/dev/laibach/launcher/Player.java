package dev.laibach.launcher;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * The state broadcaster's set: YouTube's embedded player from
 * youtube-nocookie, on a page of its own that may not lead anywhere else. It
 * remembers where the citizen stopped, and reports what it cannot play.
 */
@SuppressLint({"SetJavaScriptEnabled", "ViewConstructor"})
final class Player extends WebView {
    /** The page the player is served from, so that YouTube sees where it is embedded. */
    private static final String ORIGIN = "https://dev.laibach.launcher";

    /** What the player asks of the screen it stands in. All but playing() are called on the screen's thread. */
    interface Host {
        Feed.Video playing();
        void refused(int code);
        void ended();
        void forbidden();
        void fullscreen(View view, WebChromeClient.CustomViewCallback callback);
        void windowed();
    }

    private final Host host;

    Player(Context context, Host host) {
        super(context);
        this.host = host;
        setBackgroundColor(0xFF000000);
        WebSettings s = getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setSupportMultipleWindows(false);
        s.setJavaScriptCanOpenWindowsAutomatically(false);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        addJavascriptInterface(new Receiver(), "TV");
        setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (permitted(request)) return false;
                // The logo, the title, "watch on YouTube", the end screen: all lead out. None may.
                host.forbidden();
                return true;
            }
        });
        setWebChromeClient(new WebChromeClient() {
            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                host.fullscreen(view, callback);
            }

            @Override
            public void onHideCustomView() {
                host.windowed();
            }
        });
    }

    /** Plays a video from the given second. */
    void tune(String id, int start) {
        loadDataWithBaseURL(ORIGIN + "/", page(id, start), "text/html", "utf-8", null);
    }

    /** Only the player itself may load; everything else is the wider YouTube. */
    private static boolean permitted(WebResourceRequest request) {
        Uri u = request.getUrl();
        String host = u.getHost() == null ? "" : u.getHost();
        String path = u.getPath() == null ? "" : u.getPath();
        if (request.isForMainFrame()) return u.toString().startsWith(ORIGIN) || "about".equals(u.getScheme());
        return (host.equals("www.youtube-nocookie.com") || host.equals("www.youtube.com")) && path.startsWith("/embed/");
    }

    /** The player page: the IFrame API, no related videos, no annotations, reports back to the Receiver. */
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
            post(() -> host.refused(code));
        }

        @JavascriptInterface
        public void at(double seconds, double duration) {
            Feed.Video video = host.playing();
            if (video == null) return;
            // The last half minute counts as seen; seen videos start from the beginning again.
            int at = duration > 0 && seconds > duration - 30 ? 0 : (int) seconds;
            Feed.remember(getContext(), video.id, at);
        }

        @JavascriptInterface
        public void state(int state) {
            // 0 is the end. YouTube would offer more; the state offers silence.
            Feed.Video video = host.playing();
            if (state == 0 && video != null) Feed.remember(getContext(), video.id, 0);
            if (state == 0) post(host::ended);
        }
    }
}
