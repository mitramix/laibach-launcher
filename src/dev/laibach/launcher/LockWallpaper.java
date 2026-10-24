package dev.laibach.launcher;

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.os.Looper;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.view.Display;
import android.view.View;

import java.io.IOException;
import java.util.Calendar;
import java.util.function.Consumer;

/**
 * The uniform's scene as the phone's own lock-screen picture, so that the
 * phone's lock and PIN screens wear the uniform too. Only the lock screen's
 * picture is set: the home picture is never touched, and nothing of the
 * citizen's own picture is ever read. Undressing takes it away again, and the
 * lock screen goes back to the picture it showed before.
 *
 * Dressed and undressed in the long-press menu (Public order → the lock screen).
 */
final class LockWallpaper {
    /** The name of the uniform the lock screen wears; absent while undressed. */
    private static final String KEY = "lock_wallpaper";
    /** Whether the lock screen had a picture of its own before it was dressed. */
    private static final String KEY_HAD_OWN = "lock_wallpaper_had_own";
    /** The day of the year the picture was drawn for: its slogan and plan day change at midnight. */
    private static final String KEY_DAY = "lock_wallpaper_day";

    private LockWallpaper() {
    }

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(Variant.PREFS, Context.MODE_PRIVATE);
    }

    static boolean dressed(Context c) {
        return prefs(c).contains(KEY);
    }

    /** Whether undressing will not bring back a lock-screen picture of the citizen's own choosing. */
    static boolean hadOwn(Context c) {
        return prefs(c).getBoolean(KEY_HAD_OWN, false);
    }

    /**
     * Dresses the lock screen in v. The picture is drawn here and set in the
     * background; done (if any) hears on the main thread whether it was set.
     */
    static void dress(Activity a, Variant v, boolean german, Consumer<Boolean> done) {
        Rect screen = a.getWindowManager().getMaximumWindowMetrics().getBounds();
        dress(a, screen.width(), screen.height(), v, german, done);
    }

    /** As above, from a tile or the background, at the size of the built-in display. */
    static void dress(Context c, Variant v, boolean german, Consumer<Boolean> done) {
        Display.Mode mode = c.getSystemService(DisplayManager.class).getDisplay(Display.DEFAULT_DISPLAY).getMode();
        dress(c, mode.getPhysicalWidth(), mode.getPhysicalHeight(), v, german, done);
    }

    private static void dress(Context c, int width, int height, Variant v, boolean german, Consumer<Boolean> done) {
        Context app = c.getApplicationContext();
        int w = Math.min(width, height), h = Math.max(width, height);
        Bitmap picture = render(c, v, german, w, h);
        Handler main = new Handler(Looper.getMainLooper());
        new Thread(() -> {
            boolean ok = set(app, v, picture);
            if (done != null) main.post(() -> done.accept(ok));
        }, "lock-wallpaper").start();
    }

    /** Whether the dressed lock screen still shows yesterday's slogan, or another uniform. */
    static boolean stale(Context c, Variant v) {
        SharedPreferences p = prefs(c);
        return dressed(c) && (p.getInt(KEY_DAY, -1) != Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
                || !v.name.equals(p.getString(KEY, "")));
    }

    private static boolean set(Context app, Variant v, Bitmap picture) {
        WallpaperManager wallpapers = WallpaperManager.getInstance(app);
        SharedPreferences.Editor edit = prefs(app).edit();
        if (!dressed(app)) {
            // A lock screen without a picture of its own shows the home picture.
            edit.putBoolean(KEY_HAD_OWN, wallpapers.getWallpaperId(WallpaperManager.FLAG_LOCK) > 0);
        }
        try {
            wallpapers.setBitmap(picture, null, true, WallpaperManager.FLAG_LOCK);
        } catch (IOException | RuntimeException e) {
            return false;
        } finally {
            picture.recycle();
        }
        edit.putString(KEY, v.name).putInt(KEY_DAY, Calendar.getInstance().get(Calendar.DAY_OF_YEAR)).apply();
        return true;
    }

    /** Takes the uniform off the lock screen, which goes back to the home picture. */
    static boolean undress(Context c) {
        try {
            WallpaperManager.getInstance(c).clear(WallpaperManager.FLAG_LOCK);
        } catch (IOException | RuntimeException e) {
            return false;
        }
        prefs(c).edit().remove(KEY).remove(KEY_HAD_OWN).remove(KEY_DAY).apply();
        return true;
    }

    /**
     * The scene behind the home screen, held still under the CRT, with the
     * uniform's motto low down: above it the phone puts its own clock, below
     * it the fingerprint sensor and the shortcuts.
     */
    static Bitmap render(Context c, Variant v, boolean german, int w, int h) {
        float dp = c.getResources().getDisplayMetrics().density;
        Bitmap picture = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(picture);
        canvas.drawColor(v.bg);
        for (View layer : new View[]{new Cyber.Backdrop(c, v), new Cyber.Crt(c, v)}) {
            layer.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY));
            layer.layout(0, 0, w, h);
            layer.draw(canvas);
        }

        int width = w - Math.round(64 * dp);
        TextPaint motto = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
        motto.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        motto.setTextSize(15 * dp);
        motto.setLetterSpacing(0.14f);
        motto.setColor(v.ink);
        motto.setShadowLayer(10 * dp, 0, 0, v.bg);
        StaticLayout lines = StaticLayout.Builder.obtain(v.motto(german), 0, v.motto(german).length(), motto, width)
                .setAlignment(Layout.Alignment.ALIGN_CENTER).setLineSpacing(4 * dp, 1f).build();

        TextPaint stamp = new TextPaint(motto);
        stamp.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        stamp.setTextSize(10 * dp);
        stamp.setLetterSpacing(0.22f);
        stamp.setColor(v.accent);
        stamp.setTextAlign(TextPaint.Align.CENTER);

        // Above the motto: the proclamation of the day, under the plan day's number.
        int day = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
        String[] slogan = HomeActivity.SLOGANS[day % HomeActivity.SLOGANS.length];
        String sloganText = Lang.pick(german, slogan);
        TextPaint daily = new TextPaint(motto);
        daily.setTypeface(Typeface.create("sans-serif-condensed", Typeface.NORMAL));
        daily.setTextSize(12 * dp);
        daily.setColor(v.muted);
        daily.setLetterSpacing(0.1f);
        StaticLayout sloganLines = StaticLayout.Builder.obtain(sloganText, 0, sloganText.length(), daily, width)
                .setAlignment(Layout.Alignment.ALIGN_CENTER).setLineSpacing(2 * dp, 1f).build();

        float bottom = h * 0.80f;
        float top = bottom - lines.getHeight() - 22 * dp;
        canvas.save();
        canvas.translate((w - width) / 2f, top);
        lines.draw(canvas);
        canvas.restore();
        float sloganTop = top - sloganLines.getHeight() - 14 * dp;
        canvas.save();
        canvas.translate((w - width) / 2f, sloganTop);
        sloganLines.draw(canvas);
        canvas.restore();
        canvas.drawText((german ? "■ PLANTAG " : Lang.en("■ PLAN DAY ")) + day + " ■", w / 2f, sloganTop - 10 * dp, stamp);
        canvas.drawText("✚  LAIBACH LIFE™ · " + v.name + "  ✚", w / 2f, bottom, stamp);
        return picture;
    }
}
