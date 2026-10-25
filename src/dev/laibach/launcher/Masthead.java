package dev.laibach.launcher;

import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * The masthead: the emblem (one tap opens the menu) and the band's name over
 * the device line. The watching eye and the network status stand apart, in
 * {@link #watch}, for the monument to set beside the clock.
 */
final class Masthead extends HomePart {
    private TextView labels;
    private TextView values;
    private TextView device;
    /** The eye over the status: the network, the VPN and the uniform in force. */
    View watch;

    Masthead(HomeActivity home) {
        super(home);
    }

    void build(LinearLayout root) {
        LinearLayout mast = row();
        mast.setGravity(Gravity.CENTER_VERTICAL);
        EmblemView emblem = new EmblemView(home, v);
        emblem.setContentDescription(t("Menu", "Menü"));
        emblem.setOnClickListener(view -> home.gearMenu.show(view));
        mast.addView(emblem, new LinearLayout.LayoutParams(dp(44), dp(44)));
        TextView name = glitch(34, v.ink, 25, 1000);
        name.setText("LAIBACH");
        name.setLetterSpacing(0.14f);
        // One line, but not setSingleLine: that measures as endlessly wide and the name never shrinks.
        name.setMaxLines(1);
        name.setPadding(dp(12), 0, 0, 0);
        name.setGravity(Gravity.CENTER_VERTICAL);
        name.setAutoSizeTextTypeUniformWithConfiguration(18, 34, 1, TypedValue.COMPLEX_UNIT_SP);
        mast.addView(name, new LinearLayout.LayoutParams(0, dp(44), 1f));
        root.addView(mast);
        root.addView(rule(v.accent), ruleParams(dp(5), dp(8), dp(6)));
        device = type(10, v.muted, 80, 600);
        device.setLetterSpacing(0.16f);
        device.setSingleLine(true);
        device.setEllipsize(TextUtils.TruncateAt.END);
        device.setGravity(home.formation.gravity);
        if (!home.formation.has(Formation.DEVICE)) device.setVisibility(View.GONE);
        root.addView(device);
        watch = buildWatch();
    }

    /** The eye over a small table, on the side of the clock away from the hour. */
    private View buildWatch() {
        int side = home.formation.gravity == Gravity.END ? Gravity.START : Gravity.END;
        LinearLayout box = column();
        box.setGravity(side);
        View eye = new Eye(home, v);
        eye.setContentDescription(t("The state is watching", "Der Staat sieht zu"));
        box.addView(eye, new LinearLayout.LayoutParams(dp(34), dp(20)));
        LinearLayout table = row();
        labels = type(10, v.muted, 80, 600);
        labels.setLetterSpacing(0.12f);
        labels.setText(t("NETWORK", "NETZ") + "\nVPN\n" + t("UNIFORM", "UNIFORM"));
        table.addView(labels);
        values = type(10, v.ink, 70, 750);
        values.setLetterSpacing(0.12f);
        values.setPadding(dp(10), 0, 0, 0);
        table.addView(values);
        LinearLayout.LayoutParams under = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        under.topMargin = dp(8);
        box.addView(table, under);
        return box;
    }

    /** The network as the state sees it, the uniform worn, and the system the citizen runs. */
    void update() {
        ConnectivityManager cm = home.getSystemService(ConnectivityManager.class);
        NetworkCapabilities caps = cm.getNetworkCapabilities(cm.getActiveNetwork());
        SpannableStringBuilder s = new SpannableStringBuilder();
        if (caps == null) {
            flag(s, t("OFF GRID", "NETZFREI"));
            s.append("\n—");
        } else {
            s.append(caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ? t("WI-FI", "WLAN")
                    : caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ? t("MOBILE", "MOBILFUNK")
                    : t("NETWORK", "NETZ"));
            s.append('\n');
            // A tunnel is something to hide; the state notes it.
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) flag(s, t("NOTED", "VERMERKT"));
            else s.append(t("NONE", "KEINE"));
        }
        s.append('\n').append(v.name);
        values.setText(s);
        device.setText((t("LAIBACH LIFE™ ", "LAIBACH LEBEN™ ") + home.version()
                + " — ANDROID " + Build.VERSION.RELEASE).toUpperCase(locale));
    }

    private void flag(SpannableStringBuilder s, String text) {
        int at = s.length();
        s.append(text);
        s.setSpan(new ForegroundColorSpan(v.accent), at, s.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }
}
