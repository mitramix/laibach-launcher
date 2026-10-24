package dev.laibach.launcher;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * The passport: each time the phone enters a Wi-Fi network, after a quarter
 * of an hour or more away from every one, the papers earn an entry visa.
 * Only the count and the dates of the latest entries are kept, in the
 * preferences file {@code passport}. The network is never named: its name
 * is not asked for, so no location permission is needed.
 */
final class Passport {
    private static final String PREFS = "passport";
    /** Away from Wi-Fi this long, a return is an entry; less, it was only a stumble. */
    private static final long AWAY = 15 * 60_000L;
    /** The visas the passport has pages for; older ones are only counted. */
    static final int PAGES = 24;

    private final Context context;
    private final ConnectivityManager connectivity;
    private final Handler handler = new Handler(Looper.getMainLooper());
    /** The Wi-Fi networks up at the moment: two may overlap while the phone changes over. */
    private int up;

    private final ConnectivityManager.NetworkCallback watch = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(Network network) {
            if (up++ == 0) entered();
        }

        @Override
        public void onLost(Network network) {
            if (up > 0 && --up == 0) prefs(context).edit().putLong("leftAt", System.currentTimeMillis()).apply();
        }
    };

    private Passport(Context c) {
        context = c.getApplicationContext();
        connectivity = c.getSystemService(ConnectivityManager.class);
    }

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** Starts watching the border; stop the watch it returns when the home screen goes. */
    static Passport watch(Context c) {
        Passport p = new Passport(c);
        NetworkRequest wifi = new NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build();
        try {
            p.connectivity.registerNetworkCallback(wifi, p.watch, p.handler);
        } catch (RuntimeException e) {
            return p;
        }
        // Started away from every network, the time away begins now.
        p.handler.postDelayed(() -> {
            if (p.up == 0 && prefs(p.context).getLong("leftAt", 0) == 0) {
                prefs(p.context).edit().putLong("leftAt", System.currentTimeMillis()).apply();
            }
        }, 2_000);
        return p;
    }

    void stop() {
        handler.removeCallbacksAndMessages(null);
        try {
            connectivity.unregisterNetworkCallback(watch);
        } catch (RuntimeException ignored) {
            // It was never registered.
        }
    }

    /** Wi-Fi is up again: a visa if the phone was away long enough, or never entered before. */
    private void entered() {
        SharedPreferences p = prefs(context);
        long now = System.currentTimeMillis(), left = p.getLong("leftAt", 0);
        int count = p.getInt("count", 0);
        // Back within the quarter hour, or the home screen drawn again over the same network: no visa.
        boolean entry = count == 0 || (left > 0 && now - left >= AWAY);
        SharedPreferences.Editor e = p.edit().putLong("leftAt", 0);
        if (!entry) {
            e.apply();
            return;
        }
        List<Long> dates = dates(context);
        dates.add(now);
        while (dates.size() > PAGES) dates.remove(0);
        StringBuilder b = new StringBuilder();
        for (long d : dates) b.append(b.length() > 0 ? "," : "").append(d);
        e.putInt("count", count + 1).putString("dates", b.toString()).apply();
        Record.noteDaily(context, "passport", "ENTRY VISA STAMPED. THE NETWORK IS NOT NAMED.",
                "EINREISEVISUM GESTEMPELT. DAS NETZ WIRD NICHT GENANNT.");
        Stamps.award(context, "passport");
    }

    /** Entry visas stamped so far. */
    static int count(Context c) {
        return prefs(c).getInt("count", 0);
    }

    /** When the latest visas were stamped, oldest first. */
    static List<Long> dates(Context c) {
        List<Long> out = new ArrayList<>();
        for (String s : prefs(c).getString("dates", "").split(",")) {
            if (!s.isEmpty()) out.add(Long.parseLong(s));
        }
        return out;
    }

    /**
     * A page of the passport: the latest visas, newest first, each a framed
     * stamp set down at its own slight angle, as border guards do.
     */
    static final class Page extends View {
        private static final int ACROSS = 3;
        private final Variant v;
        private final boolean german;
        private final List<Long> dates;
        private final int first;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        Page(Context c, Variant v, boolean german) {
            super(c);
            this.v = v;
            this.german = german;
            List<Long> all = dates(c);
            dates = new ArrayList<>();
            for (int i = all.size() - 1; i >= 0 && dates.size() < 9; i--) dates.add(all.get(i));
            first = count(c);
        }

        @Override
        protected void onMeasure(int w, int h) {
            int width = MeasureSpec.getSize(w);
            int rows = Math.max(1, (dates.size() + ACROSS - 1) / ACROSS);
            setMeasuredDimension(width, Math.round(width / (float) ACROSS * 0.62f * rows));
        }

        @Override
        protected void onDraw(Canvas c) {
            float cell = getWidth() / (float) ACROSS, high = cell * 0.62f;
            SimpleDateFormat day = new SimpleDateFormat(german ? "d. MMM yyyy" : "d MMM yyyy", Lang.locale(german));
            Typeface condensed = Typeface.create("sans-serif-condensed", Typeface.BOLD);
            paint.setTypeface(condensed);
            paint.setTextAlign(Paint.Align.CENTER);
            for (int i = 0; i < dates.size(); i++) {
                float cx = cell * (i % ACROSS + 0.5f), cy = high * (i / ACROSS + 0.5f);
                // Each guard's hand is a little different, and always the same for the same visa.
                int number = first - i;
                float turn = ((number * 37) % 17) - 8;
                c.save();
                c.rotate(turn, cx, cy);
                RectF box = new RectF(cx - cell * 0.42f, cy - high * 0.38f, cx + cell * 0.42f, cy + high * 0.38f);
                paint.setStyle(Paint.Style.STROKE);
                paint.setColor(i == 0 ? v.accent : v.ink);
                paint.setAlpha(i == 0 ? 255 : 200);
                paint.setStrokeWidth(cell * 0.025f);
                c.drawRoundRect(box, cell * 0.06f, cell * 0.06f, paint);
                paint.setStrokeWidth(cell * 0.01f);
                RectF inner = new RectF(box);
                inner.inset(cell * 0.03f, cell * 0.03f);
                c.drawRoundRect(inner, cell * 0.04f, cell * 0.04f, paint);
                paint.setStyle(Paint.Style.FILL);
                paint.setTextSize(cell * 0.12f);
                paint.setLetterSpacing(0.18f);
                c.drawText(german ? "EINREISE" : Lang.en("ENTRY"), cx, cy - high * 0.12f, paint);
                paint.setTextSize(cell * 0.085f);
                paint.setLetterSpacing(0.06f);
                c.drawText(day.format(new Date(dates.get(i))).toUpperCase(Lang.locale(german)), cx, cy + high * 0.06f, paint);
                paint.setTextSize(cell * 0.07f);
                paint.setLetterSpacing(0.14f);
                c.drawText(String.format(Locale.ROOT, "№ %04d", Math.max(1, number)), cx, cy + high * 0.24f, paint);
                c.restore();
            }
        }
    }
}
