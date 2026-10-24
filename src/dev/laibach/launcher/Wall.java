package dev.laibach.launcher;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Draws the current picture and its neighbours side by side, each in a frame,
 * and follows the finger: swipe for the next or previous one, long-press to
 * write a caption.
 */
final class Wall extends View {
    /** What the wall shows, and where it reports. */
    interface Host {
        int size();

        /** The picture in place i. */
        File picture(int i);

        int index();

        /** The finger has moved the wall on by step pictures. */
        void turned(int step);

        /** The finger was held on the picture. */
        void held();
    }

    private final Host host;
    private final Variant v;
    private final float dp;
    private final Map<String, Bitmap> cache = new HashMap<>();
    private final Paint picture = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Paint fill = new Paint();
    private final RectF rect = new RectF();
    private final GestureDetector gestures;
    private final float frame, gap, margin;
    private VelocityTracker velocity;
    private ValueAnimator settle;
    private float downX, offset;
    private int top, bottom;

    Wall(Context context, Variant v, Host host) {
        super(context);
        this.v = v;
        this.host = host;
        dp = context.getResources().getDisplayMetrics().density;
        frame = 10 * dp;
        gap = 3 * dp;
        margin = 26 * dp;
        ColorMatrix grey = new ColorMatrix();
        grey.setSaturation(0f);
        // A little extra contrast, like a photocopy.
        float c = 1.18f, t = (1 - c) * 127.5f;
        grey.postConcat(new ColorMatrix(new float[]{
                c, 0, 0, 0, t,
                0, c, 0, 0, t,
                0, 0, c, 0, t,
                0, 0, 0, 1, 0}));
        picture.setColorFilter(new ColorMatrixColorFilter(grey));
        gestures = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public void onLongPress(MotionEvent e) {
                if (Math.abs(offset) < 8 * dp) host.held();
            }
        });
    }

    void setBounds(int top, int bottom) {
        this.top = top;
        this.bottom = bottom;
        invalidate();
    }

    /** Drops cached pictures after the collection changed. */
    void forget() {
        for (Bitmap b : cache.values()) b.recycle();
        cache.clear();
        offset = 0;
    }

    private Bitmap bitmap(int i) {
        String file = host.picture(i).getPath();
        Bitmap b = cache.get(file);
        if (b == null) {
            b = BitmapFactory.decodeFile(file);
            if (b != null) cache.put(file, b);
        }
        return b;
    }

    /** Keeps only the current picture and its two neighbours in memory. */
    private void trim() {
        List<String> keep = new ArrayList<>();
        for (int i = host.index() - 1; i <= host.index() + 1; i++) {
            if (i >= 0 && i < host.size()) keep.add(host.picture(i).getPath());
        }
        cache.entrySet().removeIf(e -> {
            if (keep.contains(e.getKey())) return false;
            e.getValue().recycle();
            return true;
        });
    }

    @Override
    protected void onDraw(Canvas c) {
        if (host.size() == 0) return;
        int w = getWidth();
        for (int i = host.index() - 1; i <= host.index() + 1; i++) {
            if (i < 0 || i >= host.size()) continue;
            float left = (i - host.index()) * w + offset;
            if (left >= w || left <= -w) continue;
            Bitmap b = bitmap(i);
            if (b != null) drawFramed(c, b, left, w);
        }
    }

    private void drawFramed(Canvas c, Bitmap b, float left, int w) {
        float border = frame + gap;
        float availW = w - 2 * (margin + border);
        float availH = getHeight() - top - bottom - 2 * border;
        if (availW <= 0 || availH <= 0) return;
        float s = Math.min(availW / b.getWidth(), availH / b.getHeight());
        float pw = b.getWidth() * s, ph = b.getHeight() * s;
        float x = left + (w - pw) / 2f, y = top + (getHeight() - top - bottom - ph) / 2f;
        // Frame in ink, a thin line of background, then the picture.
        fill.setColor(v.ink);
        rect.set(x - border, y - border, x + pw + border, y + ph + border);
        c.drawRect(rect, fill);
        fill.setColor(v.bg);
        rect.set(x - gap, y - gap, x + pw + gap, y + ph + gap);
        c.drawRect(rect, fill);
        rect.set(x, y, x + pw, y + ph);
        c.drawBitmap(b, null, rect, picture);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (host.size() == 0) return false;
        gestures.onTouchEvent(e);
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (settle != null) settle.cancel();
                velocity = VelocityTracker.obtain();
                velocity.addMovement(e);
                downX = e.getX() - offset;
                return true;
            case MotionEvent.ACTION_MOVE: {
                velocity.addMovement(e);
                float dx = e.getX() - downX;
                boolean edge = (dx > 0 && host.index() == 0) || (dx < 0 && host.index() == host.size() - 1);
                offset = edge ? dx / 3 : dx;
                invalidate();
                return true;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                velocity.addMovement(e);
                velocity.computeCurrentVelocity(1000);
                float vx = velocity.getXVelocity();
                velocity.recycle();
                velocity = null;
                int w = getWidth();
                boolean fling = Math.abs(vx) > 600 * dp;
                int step = 0;
                if ((offset < -w / 5f || (fling && vx < 0)) && host.index() < host.size() - 1) step = 1;
                if ((offset > w / 5f || (fling && vx > 0)) && host.index() > 0) step = -1;
                slide(step);
                return true;
            }
            default:
                return false;
        }
    }

    /** Animates to the next (1), previous (-1) or the same (0) picture. */
    private void slide(int step) {
        float target = -step * getWidth();
        settle = ValueAnimator.ofFloat(offset, target);
        settle.setDuration(220);
        settle.setInterpolator(new DecelerateInterpolator());
        settle.addUpdateListener(a -> {
            offset = (float) a.getAnimatedValue();
            invalidate();
        });
        settle.addListener(new AnimatorListenerAdapter() {
            private boolean cancelled;

            @Override
            public void onAnimationCancel(Animator animation) {
                cancelled = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (cancelled || step == 0) return;
                offset = 0;
                host.turned(step);
                trim();
            }
        });
        settle.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (settle != null) settle.cancel();
        forget();
        super.onDetachedFromWindow();
    }
}
