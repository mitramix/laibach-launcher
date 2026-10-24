package dev.laibach.launcher;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Insets;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Calendar;
import java.util.Locale;

/**
 * The waiting room in front of a rationed programme: the citizen draws a
 * number, three are served ahead of it, about three seconds, and then the
 * programme opens. Leaving the queue gives the place away.
 */
final class WaitingRoom extends HomePart {
    private static final String PREFS = "queue";
    private static final long STEP = 900;

    /** The room while a citizen waits in it; null when nobody does. */
    View view;
    TextView serving;
    Runnable next;

    WaitingRoom(HomeActivity home) {
        super(home);
    }

    boolean open() {
        return view != null;
    }

    /** Draws a number for the programme and waits; then lets it through to the counter. */
    void enter(App a, Runnable admitted) {
        leave();
        int number = draw(home);
        LinearLayout c = column();
        c.setBackgroundColor(v.bg);
        c.setClickable(true);
        c.setOnApplyWindowInsetsListener((view, insets) -> {
            Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            view.setPadding(bars.left + dp(28), bars.top + dp(10), bars.right + dp(28), bars.bottom + dp(28));
            return insets;
        });
        TextView name = type(16, v.ink, 25, 1000);
        name.setText(a.ministry.title(german));
        name.setLetterSpacing(0.2f);
        c.addView(name);
        c.addView(rule(v.accent), ruleParams(dp(3), dp(6), 0));

        LinearLayout middle = column();
        middle.setGravity(Gravity.CENTER);
        TextView head = label(t("WAITING ROOM", "WARTEZIMMER") + " · " + a.label.toUpperCase(locale));
        head.setGravity(Gravity.CENTER);
        middle.addView(head);
        TextView yours = type(16, v.muted, 60, 800);
        yours.setText(t("YOUR NUMBER IS", "IHRE NUMMER IST"));
        yours.setLetterSpacing(0.2f);
        yours.setGravity(Gravity.CENTER);
        yours.setPadding(0, dp(28), 0, 0);
        middle.addView(yours);
        TextView ticket = glitch(96, v.ink, 25, 1000);
        ticket.setText(ticket(number));
        ticket.setGravity(Gravity.CENTER);
        ticket.setLetterSpacing(0.06f);
        middle.addView(ticket);
        serving = type(20, v.accent, 40, 900);
        serving.setLetterSpacing(0.16f);
        serving.setGravity(Gravity.CENTER);
        middle.addView(serving);
        TextView seated = type(12, v.muted, 80, 650);
        seated.setText(t("PLEASE REMAIN SEATED. YOUR PATIENCE IS RECORDED.",
                "BITTE BLEIBEN SIE SITZEN. IHRE GEDULD WIRD VERMERKT."));
        seated.setGravity(Gravity.CENTER);
        seated.setLetterSpacing(0.1f);
        seated.setPadding(0, dp(18), 0, 0);
        middle.addView(seated);
        c.addView(middle, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        TextView leave = type(14, v.accent, 40, 900);
        leave.setText(t("LEAVE THE QUEUE", "DIE SCHLANGE VERLASSEN"));
        leave.setLetterSpacing(0.12f);
        leave.setGravity(Gravity.CENTER);
        leave.setPadding(dp(12), dp(16), dp(12), dp(16));
        GradientDrawable frame = new GradientDrawable();
        frame.setStroke(Math.max(1, dp(1.5f)), v.accent);
        leave.setBackground(frame);
        leave.setOnClickListener(x -> {
            leave();
            toast(t("You have left the queue. Your place was given to another.",
                    "Sie haben die Schlange verlassen. Ihr Platz ging an einen anderen."));
        });
        c.addView(leave);

        view = c;
        home.frame.addView(c, home.frame.indexOfChild(home.crt));
        c.requestApplyInsets();
        int[] now = {number - 3};
        next = new Runnable() {
            @Override
            public void run() {
                serving.setText(String.format(locale, t("NOW SERVING %s", "ES WIRD BEDIENT: %s"), ticket(now[0])));
                if (now[0] == number) {
                    serving.setText(t("PROCEED TO THE COUNTER", "TRETEN SIE AN DEN SCHALTER"));
                    Tones.play(home, Tones.relay());
                    home.handler.postDelayed(() -> {
                        leave();
                        admitted.run();
                    }, STEP / 2);
                    return;
                }
                now[0]++;
                home.handler.postDelayed(this, STEP);
            }
        };
        next.run();
    }

    /** Leaves the room, if anyone is waiting; the programme is not opened. */
    void leave() {
        if (next != null) home.handler.removeCallbacks(next);
        next = null;
        if (view == null) return;
        home.frame.removeView(view);
        view = null;
    }

    static String ticket(int n) {
        return String.format(Locale.ROOT, "%04d", n);
    }

    /** The next number of the day: the first lies somewhere in the forties, as if others came before. */
    static int draw(Context c) {
        SharedPreferences p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        Calendar now = Calendar.getInstance();
        String day = now.get(Calendar.YEAR) + "-" + now.get(Calendar.DAY_OF_YEAR);
        int n = day.equals(p.getString("day", ""))
                ? p.getInt("number", 40) + 1 + Norm.seed(System.currentTimeMillis()) % 3
                : 40 + Norm.seed(now.get(Calendar.DAY_OF_YEAR)) % 10;
        p.edit().putString("day", day).putInt("number", n).apply();
        return n;
    }
}
