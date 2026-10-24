package dev.laibach.launcher;

import android.graphics.Insets;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Locale;

/** The regulations: the launcher's own settings, on one page over the home screen. */
final class Regulations extends HomePart {
    /** The page while it is open; null when it is not. */
    ScrollView scroll;

    Regulations(HomeActivity home) {
        super(home);
    }

    boolean open() {
        return scroll != null;
    }

    /**
     * The launcher's settings on one page in the uniform. Each line says how
     * things stand and a tap changes it; the page is drawn again after.
     */
    void show(int scrollY) {
        close();
        ScrollView scroll = new ScrollView(home);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setBackgroundColor(v.bg);
        scroll.setClickable(true);
        LinearLayout page = column();
        scroll.addView(page);
        scroll.setOnApplyWindowInsetsListener((view, insets) -> {
            Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            page.setPadding(bars.left + dp(20), bars.top + dp(14), bars.right + dp(20), bars.bottom + dp(28));
            return insets;
        });

        LinearLayout head = row();
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = glitch(34, v.ink, 25, 1000);
        title.setText(t("REGULATIONS", "VORSCHRIFTEN"));
        title.setLetterSpacing(0.06f);
        head.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView close = type(24, v.accent, 100, 700);
        close.setText("✕");
        close.setGravity(Gravity.CENTER);
        close.setContentDescription(t("Close", "Schließen"));
        close.setBackground(pressable());
        close.setOnClickListener(x -> close());
        head.addView(close, new LinearLayout.LayoutParams(dp(48), dp(48)));
        page.addView(head);
        page.addView(label("LAIBACH LIFE™ · " + t("THE LAUNCHER'S SETTINGS", "DIE EINSTELLUNGEN DES LAUNCHERS")));
        page.addView(rule(v.accent), ruleParams(dp(3), dp(12), 0));

        section(page, t("SOUND", "KLANG"));
        toggle(page, t("Sounds", "Klänge"), t("chimes, clicks, the stamp, the start-up",
                "Signale, Klicks, der Stempel, der Start"), Tones.on(home), 21);
        toggle(page, t("Anthem of the hour", "Hymne zur vollen Stunde"),
                t("on the hour while the home screen is open; never when silent, in do not disturb or from 22:00 to 07:00",
                        "zur vollen Stunde bei offenem Startbildschirm; nie lautlos, bei Nicht stören oder von 22:00 bis 07:00"),
                Hourly.on(home), 29);
        toggle(page, t("Typewriter", "Schreibmaschine"), t("keys strike at the censor's desk and on the memo",
                "Tasten schlagen am Tisch des Zensors und auf der Aktennotiz"), Tones.typewriter(home), 28);
        toggle(page, t("Morse vibration", "Morse-Vibration"), t("new decrees are tapped out",
                "neue Erlasse werden gemorst"), Morse.on(home), 22);
        line(page, t("Issue sounds to the phone", "Klänge ans Telefon ausgeben"),
                t("as ringtones, to be chosen in the phone's sound settings",
                        "als Klingeltöne, zu wählen in den Toneinstellungen des Telefons"), "▸", true, x -> home.orders.act(25));

        section(page, t("DISPLAY", "ANZEIGE"));
        toggle(page, t("Animation", "Animation"), t("glitches, the start-up, the turning gear",
                "Störungen, der Start, das drehende Zahnrad"), !Fx.pacified(home), 15);

        section(page, t("LOCK SCREEN", "SPERRBILDSCHIRM"));
        toggle(page, t("Seal", "Siegel"), t("the state's seal before the phone's own lock",
                "das Siegel des Staates vor der Sperre des Telefons"), LockActivity.enabled(home), 16);
        toggle(page, t("Uniform on the lock screen", "Uniform auf dem Sperrbildschirm"),
                t("the lock screen's picture in the uniform", "das Bild des Sperrbildschirms in der Uniform"),
                LockWallpaper.dressed(home), 17);

        section(page, t("ROUTINES", "TAGESORDNUNG"));
        toggle(page, t("Curfew", "Sperrstunde"), "23:00–06:00", Citizen.curfewOn(home), 11);
        toggle(page, t("Ministry quotas", "Kontingente der Ministerien"),
                t("a ministry closes for inventory once its daily allowance is spent; needs usage access",
                        "ein Ministerium schließt wegen Inventur, wenn seine Tageszuteilung verbraucht ist; braucht Nutzungszugriff"),
                Quotas.on(home), 27);
        int[] drills = Drills.times(home);
        StringBuilder hours = new StringBuilder();
        for (int d : drills) hours.append(hours.length() > 0 ? " · " : "").append(Drills.time(d));
        line(page, t("Drill schedule", "Übungsplan"), t("roll calls at your own hours, answered present or absent",
                        "Appelle zu Ihren eigenen Stunden, beantwortet mit anwesend oder abwesend"),
                drills.length == 0 ? t("OFF", "AUS") : hours.toString(), drills.length > 0, x -> home.orders.act(36));
        int alarm = Alarm.minutes(home);
        line(page, t("Reveille", "Weckruf"), t("the state's alarm; tap to set the time",
                        "der Wecker des Staates; tippen, um die Zeit festzulegen"),
                alarm < 0 ? t("OFF", "AUS") : String.format(Locale.ROOT, "%02d:%02d", alarm / 60, alarm % 60),
                alarm >= 0, x -> home.orders.act(51));
        boolean bell = Alarm.BELL.equals(Alarm.tone(home));
        line(page, t("Reveille tone", "Weckton"), t("tap to change", "tippen zum Wechseln"),
                bell ? t("BELL", "GLOCKE") : t("ANTHEM", "HYMNE"), true, x -> {
                    home.orders.act(bell ? 53 : 52);
                    refresh();
                });
        if (alarm >= 0) {
            line(page, t("Cancel reveille", "Weckruf aufheben"), null, "▸", true, x -> {
                home.orders.act(54);
                refresh();
            });
        }

        section(page, t("DEAD DROP", "TOTER BRIEFKASTEN"));
        boolean dropped = !DeadDrop.note(home).isEmpty();
        line(page, t("Sealed note", "Versiegelte Notiz"), t("a note of your own on the home screen, blacked out; hold it to read it",
                        "eine eigene Notiz auf dem Startbildschirm, geschwärzt; halten, um sie zu lesen"),
                dropped ? t("SEALED", "VERSIEGELT") : t("EMPTY", "LEER"), dropped, x -> home.orders.act(33));

        section(page, t("CAMERA", "KAMERA"));
        toggle(page, t("Laibach Camera", "Laibach-Kamera"), t("its icon and its place in the share menu",
                "ihr Symbol und ihr Platz im Teilen-Menü"), home.orders.cameraIssued(), 24);
        section(page, t("CENSOR", "ZENSUR"));
        toggle(page, t("The censor's desk", "Der Tisch des Zensors"), t("shared text comes back blacked out",
                "geteilter Text kommt geschwärzt zurück"), home.orders.censorIssued(), 26);

        section(page, t("PERMISSIONS", "BERECHTIGUNGEN"));
        boolean decrees = DecreeListener.granted(home), attention = Citizen.usageAccess(home);
        line(page, t("Decrees", "Erlasse"), t("notification access: notifications become decrees",
                        "Benachrichtigungszugriff: Benachrichtigungen werden Erlasse"),
                decrees ? t("GRANTED", "ERTEILT") : t("NOT GRANTED", "NICHT ERTEILT"), decrees, x -> home.orders.act(8));
        line(page, t("Attention", "Aufmerksamkeit"), t("usage access: the screen time is measured",
                        "Nutzungszugriff: die Bildschirmzeit wird gemessen"),
                attention ? t("GRANTED", "ERTEILT") : t("NOT GRANTED", "NICHT ERTEILT"), attention, x -> home.orders.act(9));

        section(page, t("LANGUAGE", "SPRACHE"));
        String code = Lang.code(home);
        line(page, "Language · Sprache · Jezik", null,
                Lang.DE.equals(code) ? "DEUTSCH" : Lang.SL.equals(code) ? "SLOVENŠČINA" : "ENGLISH", true, x -> {
                    Menu choice = new PopupMenu(home, x).getMenu();
                    choice.add(4, 71, 0, "English").setChecked(Lang.EN.equals(code));
                    choice.add(4, 72, 1, "Deutsch").setChecked(Lang.DE.equals(code));
                    choice.add(4, 73, 2, "Slovenščina").setChecked(Lang.SL.equals(code));
                    choice.setGroupCheckable(4, true, true);
                    StateMenu.show(x, v, "Language · Sprache · Jezik", choice, Gravity.END, item -> home.orders.act(item.getItemId()));
                });

        section(page, t("SYSTEM", "SYSTEM"));
        line(page, t("Defect to another home screen", "Zu anderem Startbildschirm überlaufen"),
                t("choose the phone's home app", "die Start-App des Telefons wählen"), "▸", true, x -> home.orders.act(3));
        line(page, t("Android settings", "Android-Einstellungen"), t("the phone's own settings",
                "die eigenen Einstellungen des Telefons"), "▸", true, x -> home.orders.act(7));

        TextView foot = type(11, v.muted, 80, 600);
        foot.setLetterSpacing(0.18f);
        foot.setText("LAIBACH LIFE™ " + home.version() + "\n№ " + Citizen.number(home));
        foot.setPadding(0, dp(22), 0, 0);
        page.addView(foot);

        this.scroll = scroll;
        home.frame.addView(scroll, home.frame.indexOfChild(home.crt));
        scroll.requestApplyInsets();
        // Back to the same place once the page has its height; before that the scroll is clamped to the top,
        // so the place waits in the tag in case the page is drawn again first.
        if (scrollY > 0) {
            scroll.setTag(scrollY);
            scroll.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View s, int l, int t, int r, int b, int ol, int ot, int or, int ob) {
                    s.removeOnLayoutChangeListener(this);
                    s.scrollTo(0, scrollY);
                    s.setTag(null);
                }
            });
        }
    }

    /** Where the regulations stand, or are about to stand. */
    int y() {
        Object pending = scroll.getTag();
        return pending != null ? (int) pending : scroll.getScrollY();
    }

    /** Draws the regulations again, where they were, if they are open. */
    void refresh() {
        if (scroll != null) show(y());
    }

    void close() {
        if (scroll == null) return;
        home.frame.removeView(scroll);
        scroll = null;
    }

    void section(LinearLayout page, String title) {
        TextView t = label(title);
        t.setPadding(0, dp(26), 0, dp(4));
        page.addView(t);
    }

    /** A line that switches the order id on or off. */
    void toggle(LinearLayout page, String title, String note, boolean on, int id) {
        line(page, title, note, on ? t("ON", "AN") : t("OFF", "AUS"), on, x -> {
            home.orders.act(id);
            refresh();
        });
    }

    /** A line of the regulations: its name and note, its state at the end, in the accent when lit. */
    void line(LinearLayout page, String title, String note, String value, boolean lit, View.OnClickListener tap) {
        LinearLayout l = row();
        l.setGravity(Gravity.CENTER_VERTICAL);
        l.setMinimumHeight(dp(60));
        l.setPadding(0, dp(10), 0, dp(10));
        l.setBackground(pressable());
        l.setOnClickListener(tap);
        LinearLayout words = column();
        TextView name = type(18, v.ink, 50, 750);
        name.setAllCaps(true);
        name.setLetterSpacing(0.04f);
        name.setText(title);
        words.addView(name);
        if (note != null) {
            TextView n = type(13, v.muted, 90, 450);
            n.setText(note);
            n.setPadding(0, dp(2), 0, 0);
            words.addView(n);
        }
        l.addView(words, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView state = type(14, lit ? v.accent : v.muted, 80, 850);
        state.setLetterSpacing(0.14f);
        state.setText(value);
        state.setPadding(dp(14), 0, 0, 0);
        l.addView(state);
        page.addView(l);
        page.addView(rule(v.rule), ruleParams(Math.max(1, dp(1)), 0, 0));
    }
}
