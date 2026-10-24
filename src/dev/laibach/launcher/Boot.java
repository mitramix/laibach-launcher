package dev.laibach.launcher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;

/**
 * The operating system of the state coming up: lines typed one after the
 * other with their verdicts, then the whole screen glitches away. A tap
 * skips it.
 */
final class Boot extends Fx.FxView {
    private static final long LINE = 230, TYPE = 170, HOLD = 450, OUT = 320;
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint verdict = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint wheelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path wheel = new Path();
    private String[][] lines = new String[0][];
    private long start;
    private boolean glitched;

    Boot(Context context, Variant v) {
        super(context, v);
        Typeface mono = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD);
        text.setTypeface(mono);
        text.setTextSize(12 * dp);
        text.setColor(v.ink);
        verdict.set(text);
        verdict.setColor(v.accent);
        wheelPaint.setStyle(Paint.Style.STROKE);
        wheelPaint.setStrokeWidth(3 * dp);
        setVisibility(GONE);
        setClickable(true);
        setOnClickListener(view -> finish());
    }

    /** Runs the sequence; each line is {text, verdict}. */
    void run(String[][] lines) {
        this.lines = lines;
        start = Fx.now();
        glitched = false;
        setAlpha(1f);
        setVisibility(VISIBLE);
    }

    private long length() {
        return lines.length * LINE + HOLD + OUT;
    }

    private void finish() {
        setVisibility(GONE);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        Cyber.emblem(wheel, w / 2f, h * 0.7f, w * 0.28f, v);
    }

    @Override
    protected void onDraw(Canvas c) {
        long t = Fx.now() - start;
        if (t > length()) {
            post(this::finish);
            return;
        }
        long out = t - lines.length * LINE - HOLD;
        if (out > 0 && !glitched) {
            glitched = true;
            Fx.glitchNow(OUT);
        }
        setAlpha(out > 0 ? 1 - out / (float) OUT : 1f);
        c.drawColor(v.bg);
        int w = getWidth(), h = getHeight();

        c.save();
        c.rotate(t / 12f, w / 2f, h * 0.7f);
        wheelPaint.setColor(Cyber.alpha(v.accent, 0x60));
        c.drawPath(wheel, wheelPaint);
        c.restore();

        float margin = 24 * dp, y = h * 0.2f, lineH = text.getTextSize() * 1.9f;
        int columns = Math.max(20, (int) ((w - 2 * margin) / text.measureText("M")));
        for (int i = 0; i < lines.length; i++) {
            long since = t - i * LINE;
            if (since < 0) break;
            String body = lines[i][0], verdictText = lines[i][1];
            String row = body;
            if (!verdictText.isEmpty()) {
                int dots = Math.max(2, columns - body.length() - verdictText.length() - 2);
                row = body + " " + ".".repeat(dots) + " ";
            }
            int shown = (int) Math.min(row.length(), row.length() * since / TYPE);
            c.drawText(row, 0, shown, margin, y, text);
            if (shown == row.length() && !verdictText.isEmpty()) {
                c.drawText(verdictText, margin + text.measureText(row), y, verdict);
            } else if (since < TYPE + LINE && (t / 120) % 2 == 0) {
                float x = margin + text.measureText(row, 0, shown);
                c.drawRect(x, y + text.ascent(), x + text.measureText("M"), y + text.descent(), verdict);
            }
            y += lineH;
        }
    }
}
