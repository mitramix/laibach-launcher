package dev.laibach.launcher;

import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * The masthead: the emblem (one tap opens the menu), the band's name, the
 * network and the uniform, the watching eye, then the device line.
 */
final class Masthead extends HomePart {
    private TextView status;
    private TextView device;

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
        // Long uniform names take room from the band's, which shrinks rather than loses letters.
        name.setAutoSizeTextTypeUniformWithConfiguration(18, 34, 1, TypedValue.COMPLEX_UNIT_SP);
        mast.addView(name, new LinearLayout.LayoutParams(0, dp(44), 1f));
        status = type(11, v.ink, 70, 700);
        status.setGravity(Gravity.END);
        status.setLetterSpacing(0.12f);
        status.setPadding(dp(8), 0, 0, 0);
        mast.addView(status);
        LinearLayout.LayoutParams eyeAt = new LinearLayout.LayoutParams(dp(34), dp(20));
        eyeAt.leftMargin = dp(10);
        View eye = new Eye(home, v);
        eye.setContentDescription(t("The state is watching", "Der Staat sieht zu"));
        mast.addView(eye, eyeAt);
        root.addView(mast);
        root.addView(rule(v.accent), ruleParams(dp(5), dp(8), dp(6)));
        device = type(10, v.muted, 80, 600);
        device.setLetterSpacing(0.16f);
        device.setSingleLine(true);
        device.setEllipsize(TextUtils.TruncateAt.END);
        device.setGravity(home.formation.gravity);
        if (!home.formation.has(Formation.DEVICE)) device.setVisibility(View.GONE);
        root.addView(device);
    }

    /** The network as the state sees it, the uniform worn, and the system the citizen runs. */
    void update() {
        ConnectivityManager cm = home.getSystemService(ConnectivityManager.class);
        NetworkCapabilities caps = cm.getNetworkCapabilities(cm.getActiveNetwork());
        String net;
        if (caps == null) {
            net = t("OFF GRID\n(REPORTED)", "NETZFREI\n(GEMELDET)");
        } else {
            net = (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ? t("WI-FI", "WLAN") : t("NETWORK", "NETZ"))
                    + (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) ? t("\nVPN (NOTED)", "\nVPN (VERMERKT)") : "");
        }
        status.setText(net + "\n" + v.name);
        device.setText((t("LAIBACH LIFE™ ", "LAIBACH LEBEN™ ") + home.version()
                + " — ANDROID " + Build.VERSION.RELEASE).toUpperCase(locale));
    }
}
