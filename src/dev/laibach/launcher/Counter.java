package dev.laibach.launcher;

import android.app.AlertDialog;
import android.app.ActivityOptions;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import java.util.Locale;

/** The issuing counter: every programme is opened here, past the ration and the joy coupons. */
final class Counter extends HomePart {
    Counter(HomeActivity home) {
        super(home);
    }

    /** Opens a programme; a rationed one asks first once the day's ration is spent. */
    void launch(App a, View from) {
        long used = Citizen.used(home);
        if (used < Citizen.RATION || !Citizen.rationed(home).contains(a.key)) {
            launchJoy(a, from);
            return;
        }
        StateMenu.dress(new AlertDialog.Builder(home, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle(t("Ration spent", "Zuteilung verbraucht"))
                .setMessage(String.format(locale, t("%s is rationed. Today's ration of %s is spent (%s used). "
                                + "Opening it now is noted in your record.",
                        "%s ist rationiert. Die heutige Zuteilung von %s ist verbraucht (%s genutzt). "
                                + "Wer es jetzt öffnet, wird in der Akte vermerkt."),
                        a.label, span(Citizen.RATION), span(used)))
                .setNegativeButton(t("Comply", "Gehorchen"), (d, w) -> {
                    Record.note(home, "RATIONED PROGRAMME " + a.label.toUpperCase(Locale.ROOT) + " DECLINED. DISCIPLINE NOTED.",
                            "RATIONIERTES PROGRAMM " + a.label.toUpperCase(Locale.GERMAN) + " ABGELEHNT. DISZIPLIN VERMERKT.");
                    Toast.makeText(home, t("Your discipline has been noted.", "Ihre Disziplin wurde vermerkt."),
                            Toast.LENGTH_SHORT).show();
                })
                .setPositiveButton(t("Open anyway (noted)", "Trotzdem öffnen (vermerkt)"), (d, w) -> {
                    Record.note(home, a.label.toUpperCase(Locale.ROOT) + " OPENED BEYOND THE RATION.",
                            a.label.toUpperCase(Locale.GERMAN) + " ÜBER DIE ZUTEILUNG HINAUS GEÖFFNET.");
                    launchJoy(a, from);
                })
                .show(), v);
    }

    /** Opens a programme; the Ministry of Joy takes a coupon first. */
    void launchJoy(App a, View from) {
        if (a.ministry != Ministry.JOY) {
            start(a, from);
            return;
        }
        if (Citizen.joyLeft(home) > 0) {
            int left = Citizen.spendJoy(home);
            int spent = Citizen.JOY_COUPONS - left;
            Record.note(home, "JOY COUPON " + spent + " OF " + Citizen.JOY_COUPONS + " SPENT.",
                    "FREUDENMARKE " + spent + " VON " + Citizen.JOY_COUPONS + " EINGELÖST.");
            Toast.makeText(home, left == 0
                    ? t("Last joy coupon spent. Enjoy it thoroughly.", "Letzte Freudenmarke eingelöst. Genießen Sie gründlich.")
                    : String.format(locale, t("Joy coupon spent. %d of %d remain. Enjoy responsibly.",
                            "Freudenmarke eingelöst. %d von %d übrig. Genießen Sie verantwortungsvoll."),
                            left, Citizen.JOY_COUPONS), Toast.LENGTH_SHORT).show();
            home.ledger.update();
            start(a, from);
            return;
        }
        StateMenu.dress(new AlertDialog.Builder(home, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle(t("Joy ration exhausted", "Freudenration erschöpft"))
                .setMessage(String.format(locale, t("All %d joy coupons have been spent today. Further joy is "
                                + "charged to your standing, one grade at a time.",
                        "Alle %d Freudenmarken sind für heute eingelöst. Weitere Freude geht zu Lasten "
                                + "Ihres Ansehens, eine Stufe nach der anderen."), Citizen.JOY_COUPONS))
                .setNegativeButton(t("Renounce joy", "Auf Freude verzichten"), (d, w) -> {
                    Record.note(home, "JOY RENOUNCED VOLUNTARILY. THE SACRIFICE IS APPRECIATED.",
                            "AUF FREUDE FREIWILLIG VERZICHTET. DAS OPFER WIRD GESCHÄTZT.");
                    Toast.makeText(home, t("Your sacrifice has been noted. With gratitude.",
                            "Ihr Opfer wurde vermerkt. Mit Dank."), Toast.LENGTH_SHORT).show();
                })
                .setPositiveButton(t("Open anyway (noted)", "Trotzdem öffnen (vermerkt)"), (d, w) -> {
                    Citizen.overdrawJoy(home);
                    Record.note(home, "JOY TAKEN ON CREDIT. STANDING LOWERED BY ONE GRADE.",
                            "FREUDE AUF KREDIT GENOMMEN. ANSEHEN UM EINE STUFE GESENKT.");
                    home.ledger.update();
                    start(a, from);
                })
                .show(), v);
    }

    void start(App a, View from) {
        Bundle opts = from == null ? null : ActivityOptions
                .makeScaleUpAnimation(from, 0, 0, from.getWidth(), from.getHeight()).toBundle();
        try {
            home.launcherApps.startMainActivity(a.info.getComponentName(), a.info.getUser(), null, opts);
        } catch (RuntimeException e) {
            Toast.makeText(home, t(a.label + " is not available to you", a.label + " steht Ihnen nicht zur Verfügung"),
                    Toast.LENGTH_SHORT).show();
        }
    }
}
