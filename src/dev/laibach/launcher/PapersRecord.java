package dev.laibach.launcher;

import android.graphics.drawable.GradientDrawable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/** The permanent record, newest first, if the owner keeps one; an offer to keep it if not. */
final class PapersRecord extends PapersPart {
    PapersRecord(PapersActivity papers) {
        super(papers);
    }

    void fill() {
        List<String[]> entries = Record.entries(papers);
        heading(t("PERMANENT RECORD", "STÄNDIGE AKTE"));
        if (!Record.on(papers)) {
            TextView closed = serif(13, v.muted);
            closed.setText(t("THE RECORD IS NOT KEPT. NOTHING YOU DO IN THE LAUNCHER IS WRITTEN DOWN.",
                    "DIE AKTE WIRD NICHT GEFÜHRT. NICHTS, WAS SIE IM LAUNCHER TUN, WIRD AUFGESCHRIEBEN."));
            content.addView(closed);
            TextView keep = button(t("KEEP THE RECORD", "DIE AKTE FÜHREN"), view -> {
                Record.setOn(papers, true);
                Record.note(papers, "PERMANENT RECORD OPENED. FROM NOW ON, EVERYTHING IS NOTED.",
                        "STÄNDIGE AKTE ANGELEGT. AB JETZT WIRD ALLES VERMERKT.");
                papers.fill();
            });
            LinearLayout.LayoutParams lp = fullWidth();
            lp.topMargin = dp(20);
            content.addView(keep, lp);
            return;
        }
        if (entries.isEmpty()) {
            TextView none = serif(13, v.muted);
            none.setText(t("NOTHING HAS BEEN RECORDED. THIS IS SUSPICIOUS.",
                    "ES WURDE NICHTS AUFGEZEICHNET. DAS IST VERDÄCHTIG."));
            content.addView(none);
        }
        SimpleDateFormat when = new SimpleDateFormat(t("d MMM HH:mm", "d. MMM HH:mm"), locale);
        for (String[] e : entries) {
            String time;
            try {
                time = when.format(new Date(Long.parseLong(e[0]))).toUpperCase(locale);
            } catch (NumberFormatException ex) {
                time = "—";
            }
            SpannableStringBuilder s = new SpannableStringBuilder(time);
            s.setSpan(new ForegroundColorSpan(v.accent), 0, s.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            s.append("  ").append((german ? e[2] : Lang.en(e[1])));
            TextView line = type(12, v.ink, 70, 700);
            line.setText(s);
            line.setLetterSpacing(0.06f);
            line.setPadding(0, dp(7), 0, dp(7));
            content.addView(line);
            content.addView(rule(v.rule), ruleParams(Math.max(1, dp(1)), 0, 0));
        }
        TextView delete = button(t("HOLD TO DELETE THE RECORD", "HALTEN, UM DIE AKTE ZU LÖSCHEN"), view ->
                Toast.makeText(papers, t("The record is permanent. Hold to test this.",
                        "Die Akte ist dauerhaft. Halten Sie, um das zu prüfen."), Toast.LENGTH_SHORT).show());
        delete.setTextColor(v.muted);
        ((GradientDrawable) delete.getBackground()).setStroke(Math.max(1, dp(1)), v.muted);
        delete.setOnLongClickListener(view -> {
            Record.clear(papers);
            Toast.makeText(papers, t("Record deleted. The deletion has been noted.",
                    "Akte gelöscht. Die Löschung wurde vermerkt."), Toast.LENGTH_LONG).show();
            papers.fill();
            return true;
        });
        LinearLayout.LayoutParams lp = fullWidth();
        lp.topMargin = dp(20);
        content.addView(delete, lp);
    }
}
