package dev.laibach.launcher;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Insets;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Layout;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * The censor's desk: text shared from any app ("Submit to the censor") is
 * laid on the desk with names, numbers, addresses and unsuitable words
 * already blacked out. The owner taps a word to black it out or lift the bar,
 * then issues the text again, as text with bars or as a memo on paper with
 * the seal and the stamp. The text is kept only while it is on the desk.
 */
public class CensorActivity extends StateActivity {
    /** The longest text the desk takes. */
    private static final int MAX_TEXT = 20_000;
    /** The longest text drawn on a memo. */
    private static final int MAX_MEMO = 6_000;
    private static final String KEY_ISSUED = "censorIssued";

    private SharedPreferences prefs;

    private String text;
    private List<TextCensor.Word> words;
    private boolean busy;

    private View empty, work;
    private TextView counter, sheet, tally;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences(Variant.PREFS, MODE_PRIVATE);

        wear(buildUi());
        DarkroomProvider.sweep(this);
        receive(getIntent());
        show();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        receive(intent);
        show();
    }

    /** Text shared from another app. */
    private void receive(Intent intent) {
        if (intent == null || !Intent.ACTION_SEND.equals(intent.getAction())) return;
        CharSequence shared = intent.getCharSequenceExtra(Intent.EXTRA_TEXT);
        intent.setAction(Intent.ACTION_MAIN);
        if (shared == null || shared.toString().trim().isEmpty()) {
            Toast.makeText(this, t("NOTHING TO CENSOR.", "NICHTS ZU ZENSIEREN."), Toast.LENGTH_SHORT).show();
            return;
        }
        lay(shared.toString());
    }

    private void lay(String received) {
        text = received.length() > MAX_TEXT ? received.substring(0, MAX_TEXT) + " …" : received;
        words = TextCensor.read(text);
        Tones.play(this, Tones.stamp());
    }

    // ---- Layout -------------------------------------------------------------

    private View buildUi() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(v.bg);
        LinearLayout chrome = new LinearLayout(this);
        chrome.setOrientation(LinearLayout.VERTICAL);
        root.addView(chrome);

        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = type(26, v.accent, 25, 1000);
        back.setText("◀");
        back.setPadding(0, 0, dp(12), 0);
        back.setOnClickListener(view -> finish());
        head.addView(back);
        TextView title = type(30, v.ink, 25, 1000);
        title.setText(t("THE CENSOR", "DIE ZENSUR"));
        title.setLetterSpacing(0.14f);
        title.setSingleLine();
        head.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        counter = type(15, v.accent, 25, 1000);
        counter.setLetterSpacing(0.08f);
        head.addView(counter);
        chrome.addView(head);
        View rule = new View(this);
        rule.setBackgroundColor(v.accent);
        LinearLayout.LayoutParams ruleLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(5));
        ruleLp.topMargin = dp(6);
        chrome.addView(rule, ruleLp);

        FrameLayout desk = new FrameLayout(this);
        chrome.addView(desk, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        // An empty desk: what the censor does, and a specimen to try it on.
        LinearLayout none = new LinearLayout(this);
        none.setOrientation(LinearLayout.VERTICAL);
        none.setGravity(Gravity.CENTER);
        TextView noneTitle = type(34, v.ink, 25, 1000);
        noneTitle.setText(t("THE CENSOR'S DESK", "DER TISCH DES ZENSORS"));
        noneTitle.setGravity(Gravity.CENTER);
        none.addView(noneTitle);
        TextView noneText = new TextView(this);
        noneText.setTypeface(Typeface.create("serif", Typeface.BOLD));
        noneText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        noneText.setTextColor(v.muted);
        noneText.setGravity(Gravity.CENTER);
        noneText.setLetterSpacing(0.04f);
        noneText.setText(t("SHARE A TEXT FROM ANY APP AND CHOOSE \"SUBMIT TO THE CENSOR\".\nNAMES, NUMBERS AND ADDRESSES ARE BLACKED OUT.\nTHE TEXT IS NOT KEPT.",
                "TEILEN SIE EINEN TEXT AUS EINER APP UND WÄHLEN SIE „DER ZENSUR VORLEGEN“.\nNAMEN, NUMMERN UND ADRESSEN WERDEN GESCHWÄRZT.\nDER TEXT WIRD NICHT AUFBEWAHRT."));
        noneText.setPadding(0, dp(12), 0, dp(24));
        none.addView(noneText);
        TextView specimen = button(t("SPECIMEN (TEST TEXT)", "MUSTER (TESTTEXT)"), v.accent, view -> {
            lay(specimen());
            show();
        });
        specimen.setGravity(Gravity.CENTER);
        none.addView(specimen, full());
        empty = none;
        desk.addView(none);

        // The sheet on the desk, the tally and the foot.
        LinearLayout table = new LinearLayout(this);
        table.setOrientation(LinearLayout.VERTICAL);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        sheet = new TextView(this);
        sheet.setTypeface(Typeface.MONOSPACE);
        sheet.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        sheet.setLineSpacing(0, 1.25f);
        sheet.setTextColor(0xFF151515);
        sheet.setBackgroundColor(Censor.paper(v));
        sheet.setPadding(dp(18), dp(18), dp(18), dp(18));
        sheet.setOnTouchListener(new Pen());
        scroll.addView(sheet, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        scrollLp.topMargin = dp(12);
        scrollLp.bottomMargin = dp(8);
        table.addView(scroll, scrollLp);
        tally = type(11, v.muted, 50, 700);
        tally.setLetterSpacing(0.06f);
        tally.setGravity(Gravity.CENTER);
        table.addView(tally, full());
        View footRule = new View(this);
        footRule.setBackgroundColor(v.accent);
        LinearLayout.LayoutParams footRuleLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(2));
        footRuleLp.topMargin = dp(8);
        table.addView(footRule, footRuleLp);
        LinearLayout foot = new LinearLayout(this);
        foot.addView(button(t("■ ISSUE", "■ HERAUSGEBEN"), v.accent, view -> issueText()), weight());
        TextView memo = button(t("AS A MEMO", "ALS AKTENNOTIZ"), v.ink, view -> issueMemo());
        memo.setGravity(Gravity.CENTER_HORIZONTAL);
        foot.addView(memo, weight());
        TextView copy = button(t("COPY", "KOPIEREN"), v.muted, view -> copy());
        copy.setGravity(Gravity.END);
        foot.addView(copy, weight());
        table.addView(foot);
        work = table;
        desk.addView(table);

        root.setOnApplyWindowInsetsListener((view, in) -> {
            Insets b = in.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            chrome.setPadding(b.left + dp(20), b.top + dp(6), b.right + dp(20), b.bottom + dp(4));
            return WindowInsets.CONSUMED;
        });
        return root;
    }

    private LinearLayout.LayoutParams weight() {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
    }

    private LinearLayout.LayoutParams full() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private TextView button(String text, int color, View.OnClickListener click) {
        TextView b = type(13, color, 50, 850);
        b.setText(text);
        b.setLetterSpacing(0.08f);
        b.setPadding(0, dp(14), 0, dp(14));
        b.setOnClickListener(click);
        return b;
    }

    /** Brings the desk in line with what is on it. */
    private void show() {
        boolean onDesk = text != null;
        empty.setVisibility(onDesk ? View.INVISIBLE : View.VISIBLE);
        work.setVisibility(onDesk ? View.VISIBLE : View.INVISIBLE);
        counter.setText(busy ? t("TYPING …", "WIRD GETIPPT …") : onDesk ? t("DECREE ", "ERLASS ") + number() : "");
        if (!onDesk) return;
        sheet.setText(barred(text, words));
        tally.setText(String.format(locale, t("%d WORD(S) BLACKED OUT · TAP A WORD TO BLACK IT OUT OR LIFT THE BAR",
                "%d WORT/WÖRTER GESCHWÄRZT · WORT ANTIPPEN ZUM SCHWÄRZEN ODER FREIGEBEN"), TextCensor.count(words)));
    }

    /** The text with black bars over the blacked-out words, as wide as the words under them. */
    private static SpannableString barred(String text, List<TextCensor.Word> words) {
        SpannableString s = new SpannableString(text);
        for (TextCensor.Word w : words) {
            if (!w.redacted) continue;
            s.setSpan(new BackgroundColorSpan(0xFF000000), w.start, w.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            s.setSpan(new ForegroundColorSpan(0xFF000000), w.start, w.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return s;
    }

    /** A tap on a word lays a bar on it or lifts the bar; a drag scrolls. */
    private final class Pen implements View.OnTouchListener {
        private final float slop = ViewConfiguration.get(CensorActivity.this).getScaledTouchSlop();
        private float downX, downY;

        @Override
        public boolean onTouch(View view, MotionEvent e) {
            if (words == null) return false;
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downX = e.getX();
                    downY = e.getY();
                    return true;
                case MotionEvent.ACTION_UP:
                    if (Math.hypot(e.getX() - downX, e.getY() - downY) > slop) return true;
                    TextCensor.Word w = TextCensor.at(words, sheet.getOffsetForPosition(e.getX(), e.getY()));
                    if (w != null) {
                        w.redacted = !w.redacted;
                        show();
                    }
                    view.performClick();
                    return true;
                default:
                    return true;
            }
        }
    }

    // ---- Issuing ------------------------------------------------------------

    private String stamp() {
        return TextCensor.count(words) == 0 ? t("APPROVED", "GENEHMIGT") : t("CENSORED", "ZENSIERT");
    }

    /** The number this text is issued under: the day of the year and the count of texts issued. */
    private String number() {
        return Calendar.getInstance().get(Calendar.DAY_OF_YEAR) + "/" + (prefs.getInt(KEY_ISSUED, 0) + 1);
    }

    private String decree() {
        return t("DECREE ", "ERLASS ") + number() + " · " + Lang.pick(german, new String[]{
                Ministry.ENLIGHTENMENT.en, Ministry.ENLIGHTENMENT.de});
    }

    private String foot() {
        return t("DEPARTMENT OF CORRESPONDENCE", "ABTEILUNG FÜR SCHRIFTVERKEHR") + " · "
                + new SimpleDateFormat("dd.MM.yyyy", locale).format(new Date()) + " · "
                + String.format(locale, t("%d WORD(S) BLACKED OUT", "%d WORT/WÖRTER GESCHWÄRZT"), TextCensor.count(words));
    }

    /** The text as it leaves the desk: the decree, the stamp, the text with bars, the foot. */
    private String issued() {
        return "■ " + decree() + "\n" + stamp() + "\n\n" + TextCensor.redact(text, words) + "\n\n— " + foot()
                + " · LAIBACH LIFE™";
    }

    private void issueText() {
        if (busy || text == null) return;
        String out = issued();
        count();
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT, out);
        startActivity(chooser(send));
    }

    private void copy() {
        if (busy || text == null) return;
        ClipboardManager clipboard = getSystemService(ClipboardManager.class);
        clipboard.setPrimaryClip(ClipData.newPlainText(t("Censored text", "Zensierter Text"), issued()));
        count();
        Toast.makeText(this, t("COPIED. THE ORIGINAL WAS NOT.", "KOPIERT. DAS ORIGINAL NICHT."), Toast.LENGTH_SHORT).show();
    }

    /** Types the memo away from the screen's thread, then shares it from the darkroom's cache. */
    private void issueMemo() {
        if (busy || text == null) return;
        busy = true;
        show();
        String body = text.length() > MAX_MEMO ? text.substring(0, MAX_MEMO) + " …" : text;
        List<TextCensor.Word> bars = TextCensor.read(body);
        // The owner's own choices carry over; words past the cut are dropped with the text.
        for (TextCensor.Word b : bars) {
            TextCensor.Word w = TextCensor.at(words, b.start);
            if (w != null && w.start == b.start) b.redacted = w.redacted;
        }
        String stamp = stamp(), decree = decree(), foot = foot();
        new Thread(() -> {
            String name = "memo-" + System.currentTimeMillis() + ".jpg";
            boolean ok;
            try (OutputStream out = new FileOutputStream(new File(DarkroomProvider.dir(this), name))) {
                Bitmap memo = memo(body, bars, stamp, decree, foot);
                ok = memo.compress(Bitmap.CompressFormat.JPEG, 90, out);
                memo.recycle();
            } catch (IOException | OutOfMemoryError e) {
                ok = false;
            }
            boolean written = ok;
            runOnUiThread(() -> {
                busy = false;
                show();
                if (!written) {
                    Toast.makeText(this, t("THE MEMO COULD NOT BE TYPED.", "DIE AKTENNOTIZ LIESS SICH NICHT TIPPEN."), Toast.LENGTH_SHORT).show();
                    return;
                }
                count();
                Uri uri = DarkroomProvider.uri(name);
                Intent send = new Intent(Intent.ACTION_SEND);
                send.setType("image/jpeg");
                send.putExtra(Intent.EXTRA_STREAM, uri);
                send.setClipData(ClipData.newRawUri("", uri));
                send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(chooser(send));
            });
        }, "censor-memo").start();
    }

    /** The share menu, without the state's own desks: what was censored is not censored again. */
    private Intent chooser(Intent send) {
        Intent chooser = Intent.createChooser(send, t("ISSUE THE TEXT", "DEN TEXT HERAUSGEBEN"));
        chooser.putExtra(Intent.EXTRA_EXCLUDE_COMPONENTS, new ComponentName[]{
                new ComponentName(this, CensorActivity.class), new ComponentName(this, DarkroomActivity.class)});
        return chooser;
    }

    /** Counts the text as issued and notes it in the record. */
    private void count() {
        int n = TextCensor.count(words);
        prefs.edit().putInt(KEY_ISSUED, prefs.getInt(KEY_ISSUED, 0) + 1).apply();
        Record.note(this, String.format(Locale.ROOT, "A TEXT WAS CENSORED AND ISSUED. %d WORD(S) BLACKED OUT.", n),
                String.format(Locale.ROOT, "EIN TEXT WURDE ZENSIERT UND HERAUSGEGEBEN. %d WORT/WÖRTER GESCHWÄRZT.", n));
        Tones.play(this, Tones.stamp());
        Stamps.award(this, "censor");
        show();
    }

    /**
     * The memo: a band in the uniform with the seal, the decree and the foot
     * line, over a typed sheet with the bars and the stamp.
     */
    private Bitmap memo(String body, List<TextCensor.Word> bars, String stamp, String decree, String foot) {
        int w = 1240, margin = 90, band = 190;
        TextPaint type = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        type.setTypeface(Typeface.MONOSPACE);
        type.setTextSize(30);
        type.setColor(0xFF151515);
        StaticLayout sheet = StaticLayout.Builder.obtain(barred(body, bars), 0, body.length(), type, w - 2 * margin)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0, 1.3f)
                .build();
        int sheetH = Math.max(sheet.getHeight() + 2 * margin, 700);
        int h = band + sheetH;
        Bitmap out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(out);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(Censor.paper(v));
        c.drawRect(0, 0, w, h, p);

        // The band: the seal, the decree and the foot line, as on a print from the darkroom.
        p.setColor(v.bg);
        c.drawRect(0, 0, w, band, p);
        p.setColor(v.accent);
        c.drawRect(0, band - 8, w, band, p);
        float r = band * 0.3f, cx = margin * 0.6f + r, cy = band / 2f;
        Censor.seal(c, cx, cy, r, v);
        Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
        line.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        line.setLetterSpacing(0.08f);
        float left = cx + r * 1.6f, room = w - margin * 0.6f - left;
        line.setColor(v.accent);
        Censor.fit(line, decree, room, band * 0.2f);
        c.drawText(decree, left, cy - band * 0.04f, line);
        line.setColor(v.muted);
        line.setTypeface(Typeface.create("sans-serif-condensed", Typeface.NORMAL));
        Censor.fit(line, foot, room, band * 0.13f);
        c.drawText(foot, left, cy + band * 0.2f, line);

        c.save();
        c.translate(margin, band + margin);
        sheet.draw(c);
        c.restore();
        c.save();
        c.translate(0, band);
        Censor.rubberStamp(c, stamp, w, sheetH, v);
        c.restore();
        return out;
    }

    // ---- The specimen -------------------------------------------------------

    /** A test text written here, with something in it for every rule. */
    private String specimen() {
        return german
                ? "An die Genossen der Abteilung 7.\n\nTreffen am Freitag um 18:30 am Bahnhof. Bringt die Flugblätter über die Freiheit mit. "
                + "Rückfragen an Anna Novak, Telefon +386 41 000 000, oder anna@example.org.\n\nDie Wahrheit wird nachgereicht."
                : "To the comrades of section 7.\n\nWe meet on Friday at 18:30 by the station. Bring the leaflets about freedom. "
                + "Questions to Anna Novak, telephone +386 41 000 000, or anna@example.org.\n\nThe truth will follow later.";
    }

}
