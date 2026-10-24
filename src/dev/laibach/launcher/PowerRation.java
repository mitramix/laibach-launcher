package dev.laibach.launcher;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/** POWER RATION REPLENISHED: shown for a few seconds when the charger goes in. */
final class PowerRation extends HomePart {
    PowerRation(HomeActivity home) {
        super(home);
    }

    /** POWER RATION REPLENISHED: shown for a few seconds when the charger goes in. */
    void show() {
        LinearLayout o = column();
        o.setGravity(Gravity.CENTER);
        o.setBackgroundColor((v.bg & 0x00FFFFFF) | 0xEE000000);
        o.setClickable(true);
        o.addView(new EmblemView(home, v), new LinearLayout.LayoutParams(dp(110), dp(110)));
        TextView head = glitch(30, v.ink, 25, 1000);
        head.setText(t("POWER RATION\nREPLENISHED", "ENERGIERATION\nWIRD AUFGEFÜLLT"));
        head.setGravity(Gravity.CENTER);
        head.setLetterSpacing(0.14f);
        head.setPadding(0, dp(24), 0, dp(16));
        o.addView(head);
        View gauge = new View(home) {
            private final Paint paint = new Paint();

            @Override
            protected void onDraw(Canvas c) {
                int segments = 10, filled = Math.round(Math.max(0, home.batteryPct) / 10f);
                float gap = dp(4), w = (getWidth() - gap * (segments - 1)) / segments;
                for (int i = 0; i < segments; i++) {
                    paint.setColor(i < filled ? v.accent : v.rule);
                    float x = i * (w + gap);
                    c.drawRect(x, 0, x + w, getHeight(), paint);
                }
            }
        };
        gauge.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        o.addView(gauge, new LinearLayout.LayoutParams(dp(240), dp(18)));
        TextView pct = type(15, v.accent, 60, 900);
        pct.setText(t("LIFE FORCE ", "LEBENSKRAFT ") + Math.max(0, home.batteryPct) + "% ▲");
        pct.setLetterSpacing(0.2f);
        pct.setPadding(0, dp(12), 0, 0);
        o.addView(pct);
        TextView note = type(11, v.muted, 80, 650);
        note.setText(t("THE STATE PROVIDES. THE CITIZEN RECEIVES.", "DER STAAT GIBT. DER BÜRGER EMPFÄNGT."));
        note.setLetterSpacing(0.14f);
        note.setPadding(0, dp(18), 0, 0);
        o.addView(note);
        o.setContentDescription(t("Power ration replenished. Life force ", "Energieration wird aufgefüllt. Lebenskraft ")
                + home.batteryPct + "%");
        home.frame.addView(o, home.frame.indexOfChild(home.crt));
        Tones.play(home, Tones.charging());
        Runnable gone = () -> {
            if (o.getParent() == null) return;
            if (Fx.enabled(home)) o.animate().alpha(0f).setDuration(400).withEndAction(() -> home.frame.removeView(o));
            else home.frame.removeView(o);
        };
        o.setOnClickListener(view -> gone.run());
        if (Fx.enabled(home)) {
            o.setAlpha(0f);
            o.animate().alpha(1f).setDuration(250);
        }
        home.handler.postDelayed(gone, 4_000);
    }
}
