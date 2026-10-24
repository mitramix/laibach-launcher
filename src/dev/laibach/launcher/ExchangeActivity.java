package dev.laibach.launcher;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * The citizen exchange: two phones register each other as comrades. Each
 * shows a QR code of its citizen number as a {@code laibach://comrade/} link;
 * the other phone's own camera app reads it and opens this screen, which asks
 * before registering. The number can also be typed in. Nothing goes over a
 * network and no camera permission is asked for: only the numbers pass, and
 * the register of them is kept by Comrades.
 */
public final class ExchangeActivity extends StateActivity {
    static final String SCHEME = "laibach", HOST = "comrade";

    private LinearLayout content;
    /** The question now on screen, so a second link read replaces it rather than stacking on it. */
    private AlertDialog asking;

    /** 21 digits set out as the state prints them: 0438-2177-6512-0941-3378-7. */
    static String formatted(String digits) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < digits.length(); i++) {
            if (i > 0 && i % 4 == 0 && i <= 20) out.append('-');
            out.append(digits.charAt(i));
        }
        return out.toString();
    }

    static String link(String digits) {
        return SCHEME + "://" + HOST + "/" + digits;
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        wear(buildUi());
        fill();
        if (state == null) receive(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        receive(intent);
    }

    /** A comrade's link, read by the camera app: checked, then registered only if the owner agrees. */
    private void receive(Intent intent) {
        Uri link = intent == null ? null : intent.getData();
        if (link == null || !SCHEME.equals(link.getScheme()) || !HOST.equals(link.getHost())) return;
        intent.setData(null);
        offer(NumberCheck.digits(link.getPath() == null ? "" : link.getPath()));
    }

    private void offer(String digits) {
        if (asking != null) asking.dismiss();
        String own = NumberCheck.digits(Citizen.number(this));
        if (!NumberCheck.valid(digits)) {
            Tones.play(this, Tones.stamp());
            toast(t("The check digit does not agree. Forged papers are not registered.",
                    "Die Prüfziffer stimmt nicht. Gefälschte Papiere werden nicht registriert."));
            return;
        }
        if (digits.equals(own)) {
            toast(t("That is your own number. A citizen cannot be their own comrade.",
                    "Das ist Ihre eigene Nummer. Ein Bürger kann nicht sein eigener Genosse sein."));
            return;
        }
        if (Comrades.registered(this, digits)) {
            toast(t("This comrade is already registered.", "Dieser Genosse ist bereits registriert."));
            return;
        }
        asking = StateMenu.dress(new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle(t("Register a comrade", "Einen Genossen registrieren"))
                .setMessage(t("Citizen № ", "Bürger Nr. ") + formatted(digits) + "\n\n"
                        + t("The check digit agrees. Register this citizen as your comrade?",
                        "Die Prüfziffer stimmt. Diesen Bürger als Ihren Genossen registrieren?"))
                .setPositiveButton(t("Register", "Registrieren"), (d, w) -> register(digits))
                .setNegativeButton(t("Cancel", "Abbrechen"), null)
                .show(), v);
    }

    private void register(String digits) {
        Comrades.add(this, digits);
        Record.note(this, "COMRADESHIP REGISTERED WITH CITIZEN № " + formatted(digits) + ".",
                "GENOSSENSCHAFT REGISTRIERT MIT BÜRGER NR. " + formatted(digits) + ".");
        Tones.play(this, Tones.stamp());
        Stamps.award(this, "comrade");
        toast(t("Comradeship registered.", "Genossenschaft registriert."));
        fill();
    }

    private void strike(String digits) {
        StateMenu.dress(new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle(t("Citizen № ", "Bürger Nr. ") + formatted(digits))
                .setPositiveButton(t("Strike from the register", "Aus dem Register streichen"), (d, w) -> {
                    Comrades.remove(this, digits);
                    Record.note(this, "A COMRADE WAS STRUCK FROM THE REGISTER.",
                            "EIN GENOSSE WURDE AUS DEM REGISTER GESTRICHEN.");
                    fill();
                })
                .setNegativeButton(t("Cancel", "Abbrechen"), null)
                .show(), v);
    }

    /** The form for a number read out or written down, for a comrade without a camera to hand. */
    private void typeIn() {
        EditText field = new EditText(this);
        field.setInputType(InputType.TYPE_CLASS_PHONE);
        field.setHint("0000-0000-0000-0000-0000-0");
        field.setTextColor(v.ink);
        field.setHintTextColor(v.muted);
        field.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        field.setSingleLine(true);
        LinearLayout box = column();
        box.setPadding(dp(22), dp(8), dp(22), 0);
        box.addView(field);
        StateMenu.dress(new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle(t("A comrade's number", "Die Nummer eines Genossen"))
                .setView(box)
                .setPositiveButton(t("Check", "Prüfen"), (d, w) -> offer(NumberCheck.digits(field.getText())))
                .setNegativeButton(t("Cancel", "Abbrechen"), null)
                .show(), v);
    }

    // ---- Layout -------------------------------------------------------------

    private View buildUi() {
        LinearLayout root = column();
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            view.setPadding(bars.left + dp(20), bars.top + dp(6), bars.right + dp(20), 0);
            content.setPadding(0, 0, 0, bars.bottom + dp(24));
            return WindowInsets.CONSUMED;
        });
        LinearLayout head = row();
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = type(26, v.accent, 25, 1000);
        back.setText("◀");
        back.setPadding(0, 0, dp(12), 0);
        back.setOnClickListener(view -> finish());
        head.addView(back);
        TextView title = type(30, v.ink, 25, 1000);
        title.setText(t("EXCHANGE", "AUSTAUSCH"));
        title.setLetterSpacing(0.14f);
        title.setSingleLine();
        head.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        root.addView(head);
        root.addView(rule(v.accent), ruleParams(dp(5), dp(6), 0));

        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        content = column();
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        return root;
    }

    private void fill() {
        content.removeAllViews();
        String own = NumberCheck.digits(Citizen.number(this));

        heading(t("YOUR PAPERS", "IHRE PAPIERE"));
        content.addView(new Code(this, Qr.encode(link(own))), fullWidth());
        TextView number = type(20, v.ink, 25, 900);
        number.setText(Citizen.number(this));
        number.setLetterSpacing(0.08f);
        number.setGravity(Gravity.CENTER);
        number.setPadding(0, dp(12), 0, dp(6));
        content.addView(number);
        TextView how = type(11, v.muted, 80, 650);
        how.setText(t("A COMRADE SCANS THIS WITH THEIR PHONE'S OWN CAMERA. ONLY THE NUMBERS ARE EXCHANGED; NOTHING GOES OVER A NETWORK.",
                "EIN GENOSSE SCANNT DIES MIT DER KAMERA SEINES TELEFONS. NUR DIE NUMMERN WERDEN GETAUSCHT; NICHTS GEHT ÜBER EIN NETZ."));
        how.setLetterSpacing(0.1f);
        how.setGravity(Gravity.CENTER);
        content.addView(how);
        LinearLayout.LayoutParams lp = fullWidth();
        lp.topMargin = dp(16);
        content.addView(button(t("TYPE IN A COMRADE'S NUMBER", "NUMMER EINES GENOSSEN EINGEBEN"), view -> typeIn()), lp);

        List<String[]> comrades = Comrades.all(this);
        heading(t("REGISTER OF COMRADES", "REGISTER DER GENOSSEN"));
        if (comrades.isEmpty()) {
            TextView none = type(11, v.muted, 80, 650);
            none.setText(t("NO COMRADES REGISTERED. THE CITIZEN STANDS ALONE.",
                    "KEINE GENOSSEN REGISTRIERT. DER BÜRGER STEHT ALLEIN."));
            none.setLetterSpacing(0.1f);
            content.addView(none);
            return;
        }
        SimpleDateFormat date = new SimpleDateFormat(t("d MMM yyyy", "d. MMM yyyy"), locale);
        for (String[] c : comrades) {
            LinearLayout r = row();
            r.setGravity(Gravity.CENTER_VERTICAL);
            r.setPadding(0, dp(8), 0, dp(8));
            r.setBackground(pressable());
            TextView who = type(15, v.ink, 25, 850);
            who.setText("№ " + formatted(c[0]));
            who.setLetterSpacing(0.04f);
            r.addView(who, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            TextView when = type(10, v.muted, 80, 750);
            when.setText(date.format(new Date(Long.parseLong(c[1]))).toUpperCase(locale));
            when.setLetterSpacing(0.12f);
            r.addView(when);
            r.setOnClickListener(view -> strike(c[0]));
            content.addView(r);
        }
    }

    private void heading(String text) {
        content.addView(rule(v.accent), ruleParams(dp(2), dp(24), dp(8)));
        TextView h = type(16, v.ink, 25, 1000);
        h.setText(text);
        h.setLetterSpacing(0.24f);
        h.setPadding(0, 0, 0, dp(10));
        content.addView(h);
    }

    private TextView button(String text, View.OnClickListener click) {
        TextView b = type(14, v.accent, 40, 900);
        b.setText(text);
        b.setLetterSpacing(0.12f);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(12), dp(14), dp(12), dp(14));
        GradientDrawable frame = new GradientDrawable();
        frame.setStroke(Math.max(1, dp(1.5f)), v.accent);
        b.setBackground(frame);
        b.setOnClickListener(click);
        return b;
    }

    /** The code, black on white whatever the uniform, with its quiet zone and a frame of the accent. */
    private final class Code extends View {
        private final boolean[][] modules;
        private final Paint paint = new Paint();

        Code(Context c, boolean[][] modules) {
            super(c);
            this.modules = modules;
        }

        @Override
        protected void onMeasure(int w, int h) {
            int side = Math.min(MeasureSpec.getSize(w), dp(280));
            setMeasuredDimension(MeasureSpec.getSize(w), side);
        }

        @Override
        protected void onDraw(Canvas c) {
            int n = Qr.SIZE + 8;
            int cell = getHeight() / n, side = cell * n;
            float left = (getWidth() - side) / 2f, top = (getHeight() - side) / 2f;
            paint.setColor(v.accent);
            c.drawRect(left - dp(3), top - dp(3), left + side + dp(3), top + side + dp(3), paint);
            paint.setColor(Color.WHITE);
            c.drawRect(left, top, left + side, top + side, paint);
            paint.setColor(Color.BLACK);
            for (int y = 0; y < Qr.SIZE; y++) {
                for (int x = 0; x < Qr.SIZE; x++) {
                    if (!modules[y][x]) continue;
                    float l = left + (x + 4) * cell, t = top + (y + 4) * cell;
                    c.drawRect(l, t, l + cell, t + cell, paint);
                }
            }
        }
    }
}
