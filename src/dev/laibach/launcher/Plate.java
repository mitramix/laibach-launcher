package dev.laibach.launcher;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

/** The print as it stands, shown to fit; bars are laid and paint applied with a finger. */
final class Plate extends View {
    /** The darkroom the plate lies in. */
    interface Host {
        /** The print as developed, or null while there is none. */
        Bitmap developed();

        Censor.Marks marks();

        /** Whether a finger paints out rather than lays bars. */
        boolean painting();

        /** Whether the print is being developed. */
        boolean busy();

        /** Draws the whole print, its marks and its words, at the print's own size. */
        void print(Canvas c);
    }

    private final Host host;
    private final float slop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
    private float scale, ox, oy;
    private float downX, downY, lastX, lastY;
    private boolean moved;
    private RectF dragging, drawing;
    private Censor.Stroke stroke;

    Plate(Context context, Host host) {
        super(context);
        this.host = host;
    }

    @Override
    protected void onDraw(Canvas c) {
        if (host.developed() == null) return;
        fit();
        c.save();
        c.translate(ox, oy);
        c.scale(scale, scale);
        host.print(c);
        c.restore();
    }

    private void fit() {
        int[] size = Censor.size(host.developed().getWidth(), host.developed().getHeight());
        scale = Math.min(getWidth() / (float) size[0], getHeight() / (float) size[1]);
        ox = (getWidth() - size[0] * scale) / 2;
        oy = (getHeight() - size[1] * scale) / 2;
    }

    /** Where x lies across the photograph, from 0 to 1. */
    private float nx(float x) {
        return ((x - ox) / scale - Censor.margin(host.developed().getWidth())) / host.developed().getWidth();
    }

    private float ny(float y) {
        return ((y - oy) / scale - Censor.margin(host.developed().getWidth())) / host.developed().getHeight();
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (host.developed() == null || host.busy()) return false;
        fit();
        float x = e.getX(), y = e.getY();
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = lastX = x;
                downY = lastY = y;
                moved = false;
                dragging = drawing = null;
                stroke = null;
                if (host.painting()) {
                    stroke = new Censor.Stroke(0.05f);
                    stroke.points.add(new PointF(nx(x), ny(y)));
                    host.marks().all.add(stroke);
                } else {
                    dragging = hit(nx(x), ny(y));
                }
                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE:
                if (Math.hypot(x - downX, y - downY) > slop) moved = true;
                if (stroke != null) {
                    for (int i = 0; i < e.getHistorySize(); i++) {
                        stroke.points.add(new PointF(nx(e.getHistoricalX(i)), ny(e.getHistoricalY(i))));
                    }
                    stroke.points.add(new PointF(nx(x), ny(y)));
                } else if (dragging != null) {
                    dragging.offset(nx(x) - nx(lastX), ny(y) - ny(lastY));
                } else if (moved) {
                    if (drawing == null) {
                        drawing = new RectF();
                        host.marks().all.add(drawing);
                    }
                    drawing.set(Math.min(nx(downX), nx(x)), Math.min(ny(downY), ny(y)),
                            Math.max(nx(downX), nx(x)), Math.max(ny(downY), ny(y)));
                }
                lastX = x;
                lastY = y;
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
                if (!host.painting() && !moved) {
                    if (dragging != null) {
                        host.marks().all.remove(dragging);
                    } else {
                        // A bar the width of a pair of eyes, centred where the finger was.
                        float bw = 0.1f, bh = 0.025f * host.developed().getWidth() / host.developed().getHeight();
                        float cx = nx(x), cy = ny(y);
                        if (cx >= 0 && cx <= 1 && cy >= 0 && cy <= 1) {
                            host.marks().all.add(new RectF(cx - bw, cy - bh, cx + bw, cy + bh));
                        }
                    }
                } else if (drawing != null && (drawing.width() < 0.02f || drawing.height() < 0.01f)) {
                    host.marks().all.remove(drawing);
                }
                dragging = drawing = null;
                stroke = null;
                invalidate();
                performClick();
                return true;
            case MotionEvent.ACTION_CANCEL:
                dragging = drawing = null;
                stroke = null;
                return true;
            default:
                return true;
        }
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    /** The topmost bar under a finger at (x, y), with a little room around it. */
    private RectF hit(float x, float y) {
        for (int i = host.marks().all.size() - 1; i >= 0; i--) {
            Object o = host.marks().all.get(i);
            if (!(o instanceof RectF)) continue;
            RectF r = (RectF) o;
            if (x >= r.left - 0.015f && x <= r.right + 0.015f && y >= r.top - 0.015f && y <= r.bottom + 0.015f) return r;
        }
        return null;
    }
}
