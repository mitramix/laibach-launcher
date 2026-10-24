package dev.laibach.launcher;

import android.content.Intent;
import android.net.Uri;
import android.view.Gravity;
import android.view.Menu;
import android.view.SubMenu;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.Toast;
import java.util.Locale;

/** A programme's long-press menu: redact, transfer, ration, dossier, banish. */
final class ProgrammeMenu extends HomePart {
    ProgrammeMenu(HomeActivity home) {
        super(home);
    }

    void show(App a, View anchor) {
        PopupMenu popup = new PopupMenu(home, anchor, Gravity.END);
        Menu menu = popup.getMenu();
        if (a.suspended) {
            menu.add(0, 0, 0, t("Suspended by order", "Auf Anordnung gesperrt")).setEnabled(false);
        } else {
            menu.add(0, 1, 0, a.hidden ? t("Declassify", "Freigeben") : t("Redact", "Schwärzen"));
        }
        // Laibach TV is part of the launcher: it stays in Enlightenment, and banishing it would banish the state.
        boolean own = a.packageName().equals(home.getPackageName());
        if (!own) {
            SubMenu ministries = menu.addSubMenu(0, 4, 0, t("Transfer to ministry", "Ministerium wechseln"));
            for (Ministry m : Ministry.values()) {
                ministries.add(2, 200 + m.ordinal(), m.ordinal(), m.title(german)).setChecked(m == a.ministry);
            }
            ministries.setGroupCheckable(2, true, true);
            if (Ministry.transferred(home, a.packageName())) {
                ministries.add(0, 5, Ministry.values().length, t("Return to its own ministry", "Ins eigene Ministerium zurück"));
            }
        }
        menu.add(0, 6, 0, t("Rationed (warn once the day's ration is spent)",
                "Rationiert (warnen, wenn die Zuteilung verbraucht ist)"))
                .setCheckable(true).setChecked(Citizen.rationed(home).contains(a.key));
        menu.add(0, 7, 0, t("Essential (stays when mobilised)", "Unentbehrlich (bleibt bei Mobilisierung)"))
                .setCheckable(true).setChecked(Citizen.essential(home).contains(a.key));
        menu.add(0, 2, 0, t("Dossier (app info)", "Akte (App-Info)"));
        if (!a.system && !own) menu.add(0, 3, 0, t("Banish (uninstall)", "Verbannen (deinstallieren)"));
        StateMenu.show(anchor, v, a.label, menu, Gravity.END, item -> {
            int id = item.getItemId();
            if (id >= 200 && id < 200 + Ministry.values().length) {
                Ministry to = Ministry.values()[id - 200];
                Ministry.transfer(home, a.packageName(), to);
                Record.note(home, a.label.toUpperCase(Locale.ROOT) + " TRANSFERRED TO THE " + to.title(false) + ".",
                        a.label.toUpperCase(Locale.GERMAN) + " NEU UNTERSTELLT: " + to.title(true) + ".");
                home.reload();
                Toast.makeText(home, t(a.label + " now answers to the " + to.title(false).toLowerCase(locale),
                        a.label + " untersteht jetzt dem " + to.title(true).toLowerCase(locale)), Toast.LENGTH_SHORT).show();
                return true;
            }
            switch (id) {
                case 4:
                    return false;
                case 5:
                    Ministry.transfer(home, a.packageName(), null);
                    home.reload();
                    return true;
                case 6: {
                    boolean on = Citizen.toggle(home, "rationed", a.key);
                    String name = a.label.toUpperCase(Locale.ROOT);
                    Record.note(home, on ? name + " PLACED ON THE RATION." : name + " TAKEN OFF THE RATION.",
                            on ? name + " RATIONIERT." : name + " NICHT MEHR RATIONIERT.");
                    Toast.makeText(home, on ? t(a.label + " is rationed.", a.label + " ist rationiert.")
                            : t(a.label + " is no longer rationed.", a.label + " ist nicht mehr rationiert."),
                            Toast.LENGTH_SHORT).show();
                    return true;
                }
                case 7: {
                    boolean on = Citizen.toggle(home, "essential", a.key);
                    Toast.makeText(home, on ? t(a.label + " is essential.", a.label + " ist unentbehrlich.")
                            : t(a.label + " is no longer essential.", a.label + " ist nicht mehr unentbehrlich."),
                            Toast.LENGTH_SHORT).show();
                    home.applyFilter();
                    return true;
                }
                case 1:
                    // Declassifying is a return and needs no papers; redacting is a departure.
                    if (a.hidden) home.setHidden(a, false);
                    else ExitVisa.apply(home, a.label, a.ministry, false, () -> home.setHidden(a, true));
                    return true;
                case 2:
                    home.launcherApps.startAppDetailsActivity(a.info.getComponentName(), a.info.getUser(), null, null);
                    return true;
                case 3:
                    ExitVisa.apply(home, a.label, a.ministry, true, () ->
                            open(new Intent(Intent.ACTION_DELETE, Uri.fromParts("package", a.packageName(), null))));
                    return true;
                default:
                    return false;
            }
        });
    }
}
