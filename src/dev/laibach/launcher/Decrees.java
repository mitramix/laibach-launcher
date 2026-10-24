package dev.laibach.launcher;

import android.content.Intent;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The decrees under the monument: the waiting notifications, counted per
 * ministry and never read. Tap to obey (open the app concerned), long-press
 * for the next one.
 */
final class Decrees extends HomePart {
    /** What each ministry decrees about its waiting notifications; %d is how many. */
    static final Map<Ministry, String[]> NOTICES = new HashMap<>();
    static {
        NOTICES.put(Ministry.COMMUNICATION, new String[]{"%d COMMUNIQUÉ(S) AWAIT YOUR REPLY. SILENCE WILL BE NOTED.",
                "%d MITTEILUNG(EN) ERWARTEN IHRE ANTWORT. SCHWEIGEN WIRD VERMERKT."});
        NOTICES.put(Ministry.ENLIGHTENMENT, new String[]{"%d NEW TRUTH(S) ISSUED. BELIEVE THEM IN ORDER.",
                "%d NEUE WAHRHEIT(EN) AUSGEGEBEN. GLAUBEN SIE DER REIHE NACH."});
        NOTICES.put(Ministry.LABOUR, new String[]{"%d TASK(S) ASSIGNED. GRATITUDE IS OPTIONAL. COMPLETION IS NOT.",
                "%d AUFGABE(N) ZUGETEILT. DANK IST FREIWILLIG. ERLEDIGUNG NICHT."});
        NOTICES.put(Ministry.JOY, new String[]{"%d INVITATION(S) TO JOY. JOY IS RATIONED. CHOOSE WISELY.",
                "%d EINLADUNG(EN) ZUR FREUDE. FREUDE IST RATIONIERT. WÄHLEN SIE WEISE."});
        NOTICES.put(Ministry.MOVEMENT, new String[]{"%d MOVEMENT(S) RECORDED. YOURS INCLUDED.",
                "%d BEWEGUNG(EN) ERFASST. IHRE EINGESCHLOSSEN."});
        NOTICES.put(Ministry.PROSPERITY, new String[]{"%d STATEMENT(S) OF PROSPERITY. YOU HAVE NEVER BEEN RICHER.",
                "%d WOHLSTANDSMELDUNG(EN). SIE WAREN NIE REICHER."});
        NOTICES.put(Ministry.ADMINISTRATION, new String[]{"%d NOTICE(S) FILED ON YOUR BEHALF.",
                "%d BESCHEID(E) IN IHREM NAMEN ABGELEGT."});
    }

    /** Standing decrees, for when no notification waits or none may be read. */
    static final Object[][] STANDING_DECREES = {
            {Ministry.LABOUR, "TODAY'S QUOTA WAS MET IN ADVANCE. ON YOUR BEHALF.",
                    "DAS HEUTIGE SOLL WURDE IM VORAUS ERFÜLLT. IN IHREM NAMEN."},
            {Ministry.JOY, "SMILING IS PERMITTED BETWEEN 12:00 AND 12:15.",
                    "LÄCHELN IST ZWISCHEN 12:00 UND 12:15 UHR GESTATTET."},
            {Ministry.MOVEMENT, "ALL JOURNEYS END WHERE THEY BEGAN.", "ALLE REISEN ENDEN, WO SIE BEGANNEN."},
            {Ministry.ENLIGHTENMENT, "TODAY'S TRUTH HAS BEEN DELIVERED. NO FURTHER QUESTIONS.",
                    "DIE WAHRHEIT VON HEUTE WURDE ZUGESTELLT. KEINE WEITEREN FRAGEN."},
            {Ministry.PROSPERITY, "YOUR SAVINGS HAVE BEEN SAVED FOR YOU.", "IHRE ERSPARNISSE WURDEN FÜR SIE ERSPART."},
            {Ministry.COMMUNICATION, "CONVERSATIONS ARE ENCOURAGED. TRANSCRIPTS ARE KEPT.",
                    "GESPRÄCHE SIND ERWÜNSCHT. PROTOKOLLE WERDEN GEFÜHRT."},
            {Ministry.ADMINISTRATION, "NO DECREES TODAY. THIS IS ALSO A DECREE.",
                    "HEUTE KEINE ERLASSE. AUCH DAS IST EIN ERLASS."},
    };

    /** A decree on the home screen; package is the app that tapping it opens, or null. */
    static final class Decree {
        final Ministry ministry;
        final String text;
        final String pkg;

        Decree(Ministry ministry, String text, String pkg) {
            this.ministry = ministry;
            this.text = text;
            this.pkg = pkg;
        }
    }

    final List<Decree> list = new ArrayList<>();
    int index;
    /** Decrees waiting at the last count, for the Morse tap; -1 before the first. */
    int lastWaiting = -1;
    TextView view;

    Decrees(HomeActivity home) {
        super(home);
    }

    TextView build() {
        view = type(11, v.ink, 70, 700);
        view.setLetterSpacing(0.1f);
        view.setMaxLines(3);
        view.setEllipsize(TextUtils.TruncateAt.END);
        view.setPadding(0, dp(12), 0, 0);
        view.setOnClickListener(view -> obey());
        view.setOnLongClickListener(view -> {
            next();
            return true;
        });
        view.setGravity(home.formation.gravity);
        if (home.formation.mono) view.setTypeface(Typeface.MONOSPACE);
        if (!home.formation.has(Formation.DECREE)) view.setVisibility(View.GONE);
        return view;
    }

    /** Counts the waiting notifications again, per ministry, and shows the decree in turn. */
    void update() {
        list.clear();
        Map<String, Integer> waiting = DecreeListener.waiting(home);
        int total = 0;
        for (int n : waiting.values()) total += n;
        // A new decree is tapped out in Morse, if the citizen asked for it.
        if (lastWaiting >= 0 && total > lastWaiting) Morse.tap(home, false);
        lastWaiting = total;
        Map<Ministry, Integer> perMinistry = new HashMap<>();
        Map<Ministry, String> busiest = new HashMap<>();
        Map<Ministry, Integer> busiestCount = new HashMap<>();
        for (Map.Entry<String, Integer> e : waiting.entrySet()) {
            Ministry m;
            try {
                m = Ministry.of(home, home.getPackageManager().getApplicationInfo(e.getKey(), 0));
            } catch (android.content.pm.PackageManager.NameNotFoundException ex) {
                m = Ministry.ADMINISTRATION;
            }
            perMinistry.merge(m, e.getValue(), Integer::sum);
            if (e.getValue() > busiestCount.getOrDefault(m, 0)) {
                busiestCount.put(m, e.getValue());
                busiest.put(m, e.getKey());
            }
        }
        for (Ministry m : Ministry.values()) {
            Integer n = perMinistry.get(m);
            if (n == null) continue;
            list.add(new Decree(m, String.format(locale, Lang.pick(german, NOTICES.get(m)), n), busiest.get(m)));
        }
        if (list.isEmpty()) {
            if (DecreeListener.granted(home)) {
                list.add(new Decree(Ministry.ADMINISTRATION, t("ALL NOTICES ANSWERED. THE COLLECTIVE IS PLEASED.",
                        "ALLE BESCHEIDE ERLEDIGT. DAS KOLLEKTIV IST ZUFRIEDEN."), null));
            }
            int day = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
            for (int i = 0; i < STANDING_DECREES.length; i++) {
                Object[] d = STANDING_DECREES[(day + i) % STANDING_DECREES.length];
                list.add(new Decree((Ministry) d[0], (german ? (String) d[2] : Lang.en((String) d[1])), null));
            }
        }
        if (index >= list.size()) index = 0;
        show();
    }

    void show() {
        Decree d = list.get(index);
        String head = "■ " + t("DECREE ", "ERLASS ") + Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
                + "/" + (index + 1) + " · " + d.ministry.title(german);
        SpannableStringBuilder s = new SpannableStringBuilder(head);
        s.setSpan(new ForegroundColorSpan(v.accent), 0, head.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        s.append("\n").append(d.text);
        view.setText(s);
    }

    void next() {
        index = (index + 1) % list.size();
        show();
    }

    /** Opens the app a decree is about; a standing decree just gives way to the next. */
    void obey() {
        Decree d = list.get(index);
        Intent intent = d.pkg == null ? null : home.getPackageManager().getLaunchIntentForPackage(d.pkg);
        if (intent == null) {
            next();
            return;
        }
        Record.note(home, "DECREE OF THE " + d.ministry.title(false) + " OBEYED. PROMPTLY.",
                "ERLASS BEFOLGT. UMGEHEND. ABSENDER: " + d.ministry.title(true) + ".");
        open(intent);
    }
}
