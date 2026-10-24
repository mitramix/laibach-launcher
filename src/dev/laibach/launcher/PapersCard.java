package dev.laibach.launcher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Date;

/** The identity card at the head of the papers: emblem, number, standing, dates and a barcode. */
final class PapersCard extends PapersPart {
    PapersCard(PapersActivity papers) {
        super(papers);
    }

    /** The identity card: emblem, number, standing, dates and a barcode. */
    View build() {
        LinearLayout card = column();
        GradientDrawable frame = new GradientDrawable();
        frame.setStroke(Math.max(1, dp(1.5f)), v.accent);
        frame.setCornerRadius(dp(4));
        card.setBackground(frame);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));

        TextView of = type(10, v.accent, 80, 800);
        of.setText(t("CITIZEN OF THE LAIBACH COLLECTIVE", "BÜRGER DES LAIBACH-KOLLEKTIVS"));
        of.setLetterSpacing(0.2f);
        card.addView(of);

        LinearLayout middle = row();
        middle.setGravity(Gravity.CENTER_VERTICAL);
        middle.setPadding(0, dp(12), 0, dp(12));
        middle.addView(new EmblemView(papers, v), new LinearLayout.LayoutParams(dp(72), dp(72)));
        LinearLayout who = column();
        who.setPadding(dp(16), 0, 0, 0);
        // The number is 26 characters long: it takes the largest size that fits on one line.
        TextView number = type(26, v.ink, 25, 1000);
        number.setText("№ " + Citizen.number(papers));
        number.setLetterSpacing(0.06f);
        number.setMaxLines(1);
        number.setAutoSizeTextTypeUniformWithConfiguration(10, 26, 1, TypedValue.COMPLEX_UNIT_SP);
        who.addView(number, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(30)));
        int grade = Citizen.grade(papers);
        TextView standing = type(12, grade >= 3 ? v.accent : v.ink, 70, 850);
        standing.setText(t("STANDING ", "ANSEHEN ") + Lang.pick(german, Citizen.STANDING[grade]));
        standing.setLetterSpacing(0.1f);
        who.addView(standing);
        middle.addView(who, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        card.addView(middle);

        long issued;
        try {
            issued = papers.getPackageManager().getPackageInfo(papers.getPackageName(), 0).firstInstallTime;
        } catch (android.content.pm.PackageManager.NameNotFoundException e) {
            issued = System.currentTimeMillis();
        }
        LinearLayout dates = row();
        dates.addView(field(t("ISSUED", "AUSGESTELLT"),
                new SimpleDateFormat(t("d MMM yyyy", "d. MMM yyyy"), locale).format(new Date(issued)).toUpperCase(locale)),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        dates.addView(field(t("VALID", "GÜLTIG"), t("PERMANENTLY", "DAUERHAFT")),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        dates.addView(field(t("ISSUED BY", "BEHÖRDE"), t("THE STATE", "DER STAAT")),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        card.addView(dates);

        LinearLayout.LayoutParams code = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(44));
        code.topMargin = dp(14);
        card.addView(new Barcode(papers, Citizen.number(papers).replace("-", ""), v.ink), code);
        TextView foot = type(9, v.muted, 80, 700);
        foot.setText(t("THIS CARD IS THE PROPERTY OF THE COLLECTIVE. SO IS THE BEARER.",
                "DIESER AUSWEIS IST EIGENTUM DES KOLLEKTIVS. DER INHABER AUCH."));
        foot.setLetterSpacing(0.12f);
        foot.setPadding(0, dp(8), 0, 0);
        card.addView(foot);
        return card;
    }

    private View field(String label, String value) {
        LinearLayout f = column();
        TextView l = type(9, v.accent, 80, 800);
        l.setText(label);
        l.setLetterSpacing(0.2f);
        f.addView(l);
        TextView val = type(13, v.ink, 50, 850);
        val.setText(value);
        val.setLetterSpacing(0.04f);
        f.addView(val);
        return f;
    }

    /** Bars drawn from the citizen number, in the manner of a shop barcode. */
    private static final class Barcode extends View {
        private static final String[] DIGITS = {"0001101", "0011001", "0010011", "0111101", "0100011",
                "0110001", "0101111", "0111011", "0110111", "0001011"};
        private final Paint paint = new Paint();
        private final String bits;

        Barcode(Context context, String number, int ink) {
            super(context);
            StringBuilder b = new StringBuilder("101");
            for (char c : number.toCharArray()) if (Character.isDigit(c)) b.append(DIGITS[c - '0']);
            bits = b.append("101").toString();
            paint.setColor(ink);
        }

        @Override
        protected void onDraw(Canvas c) {
            float unit = getWidth() / (float) bits.length();
            for (int i = 0; i < bits.length(); i++) {
                if (bits.charAt(i) == '1') c.drawRect(i * unit, 0, (i + 1) * unit + 0.5f, getHeight(), paint);
            }
        }
    }
}
