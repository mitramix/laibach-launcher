package dev.laibach.launcher;

import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Date;

/** What the citizen has been stamped with: the stamp album, then the passport's entry visas. */
final class PapersAlbum extends PapersPart {
    PapersAlbum(PapersActivity papers) {
        super(papers);
    }

    void fill() {
        album();
        passport();
    }

    /** The stamp album: every stamp the state issues, three to a row, the missing ones in outline. */
    private void album() {
        heading(t("STAMP ALBUM", "MARKENALBUM"));
        TextView count = type(11, v.muted, 80, 650);
        count.setText(String.format(locale, t("%d OF %d ISSUED. TAP A STAMP TO SEE IT AWARDED AGAIN.",
                "%d VON %d AUSGEGEBEN. TIPPEN SIE EINE MARKE AN, UM SIE ERNEUT VERLIEHEN ZU SEHEN."),
                Stamps.count(papers), Stamps.ALL.length));
        count.setLetterSpacing(0.1f);
        count.setPadding(0, 0, 0, dp(10));
        content.addView(count);
        SimpleDateFormat date = new SimpleDateFormat(t("d MMM yyyy", "d. MMM yyyy"), locale);
        LinearLayout r = null;
        for (int i = 0; i < Stamps.ALL.length; i++) {
            if (i % 3 == 0) {
                r = row();
                r.setPadding(0, 0, 0, dp(12));
                content.addView(r);
            }
            Stamps.Stamp s = Stamps.ALL[i];
            long when = Stamps.issued(papers, s.key);
            LinearLayout cell = column();
            cell.setGravity(Gravity.CENTER_HORIZONTAL);
            cell.addView(new StampArt.StampView(papers, v, s.glyph, when > 0),
                    new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            TextView name = type(9, when > 0 ? v.ink : v.muted, 70, 850);
            name.setText(Lang.pick(german, s.name));
            name.setGravity(Gravity.CENTER);
            name.setLetterSpacing(0.08f);
            name.setPadding(0, dp(4), 0, 0);
            cell.addView(name);
            TextView issued = type(8, when > 0 ? v.accent : v.rule, 80, 700);
            issued.setText(when > 0 ? date.format(new Date(when)).toUpperCase(locale) : t("NOT YET ISSUED", "NOCH NICHT AUSGEGEBEN"));
            issued.setGravity(Gravity.CENTER);
            issued.setLetterSpacing(0.1f);
            cell.addView(issued);
            if (when > 0) {
                cell.setOnClickListener(view -> Ceremony.show(papers, s.glyph, Lang.pick(german, s.name),
                        t("ISSUED ON ", "AUSGEGEBEN AM ") + date.format(new Date(when)).toUpperCase(locale)));
            }
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            lp.setMargins(i % 3 == 0 ? 0 : dp(6), 0, i % 3 == 2 ? 0 : dp(6), 0);
            r.addView(cell, lp);
        }
        // The last row keeps its columns even when it is short.
        while (r != null && r.getChildCount() % 3 != 0) {
            r.addView(new View(papers), new LinearLayout.LayoutParams(0, 0, 1f));
        }
    }

    /** The passport's latest page: the entry visas, newest first. The networks are never named. */
    private void passport() {
        heading(t("PASSPORT", "REISEPASS"));
        TextView note = type(11, v.muted, 80, 650);
        int n = Passport.count(papers);
        note.setText(n == 0
                ? t("NO ENTRY VISAS YET. EACH WI-FI NETWORK THE PHONE ENTERS EARNS ONE; NONE IS NAMED.",
                        "NOCH KEINE EINREISEVISA. JEDES WLAN, DAS DAS TELEFON BETRITT, BRINGT EINES; KEINES WIRD GENANNT.")
                : String.format(locale, t("ENTRY VISAS STAMPED: %d. ONLY THE DATES ARE KEPT; THE NETWORKS ARE NEVER NAMED.",
                        "GESTEMPELTE EINREISEVISA: %d. NUR DIE DATEN WERDEN AUFBEWAHRT; DIE NETZE WERDEN NIE GENANNT."), n));
        note.setLetterSpacing(0.1f);
        note.setPadding(0, 0, 0, dp(10));
        content.addView(note);
        if (n > 0) content.addView(new Passport.Page(papers, v, german), fullWidth());
    }
}
