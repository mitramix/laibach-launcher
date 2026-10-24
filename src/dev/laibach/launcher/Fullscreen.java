package dev.laibach.launcher;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.webkit.WebChromeClient;
import android.widget.FrameLayout;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;

/**
 * A video taken over the whole window: laid over the glass, the bars hidden,
 * the phone turned on its side, and back leaving it before it leaves the
 * screen.
 */
final class Fullscreen {
    private final Activity activity;
    private final FrameLayout glass;
    private final OnBackInvokedCallback back = this::close;
    private View view;
    private WebChromeClient.CustomViewCallback leave;

    Fullscreen(Activity activity, FrameLayout glass) {
        this.activity = activity;
        this.glass = glass;
    }

    void open(View view, WebChromeClient.CustomViewCallback callback) {
        if (this.view != null) {
            callback.onCustomViewHidden();
            return;
        }
        this.view = view;
        leave = callback;
        view.setBackgroundColor(0xFF000000);
        glass.addView(view, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        activity.getWindow().getInsetsController().hide(WindowInsets.Type.systemBars());
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        activity.getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT, back);
    }

    void close() {
        if (view == null) return;
        glass.removeView(view);
        view = null;
        WebChromeClient.CustomViewCallback done = leave;
        leave = null;
        activity.getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(back);
        activity.getWindow().getInsetsController().show(WindowInsets.Type.navigationBars());
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        if (done != null) done.onCustomViewHidden();
    }
}
