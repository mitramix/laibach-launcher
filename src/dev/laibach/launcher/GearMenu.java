package dev.laibach.launcher;

import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.Menu;
import android.view.SubMenu;
import android.view.View;
import android.widget.PopupMenu;

/** The gear menu on the masthead: uniforms, formations, papers, art and duties. */
final class GearMenu extends HomePart {
    GearMenu(HomeActivity home) {
        super(home);
    }

    void show(View anchor) {
        PopupMenu popup = new PopupMenu(home, anchor);
        Menu menu = popup.getMenu();
        SubMenu uniforms = menu.addSubMenu(0, 0, 0, t("Uniforms", "Uniformen"));
        addVariants(uniforms);
        String plan = Variant.plan(home);
        SubMenu fiveYear = uniforms.addSubMenu(0, 0, Variant.ALL.length,
                t("Five-year plan (uniform schedule)", "Fünfjahresplan (Uniformplan)"));
        fiveYear.add(3, 30, 0, t("No plan: the chosen uniform", "Kein Plan: die gewählte Uniform"))
                .setChecked(plan.isEmpty());
        fiveYear.add(3, 31, 1, t("A new album uniform every week", "Jede Woche eine neue Album-Uniform"))
                .setChecked(Variant.WEEKLY.equals(plan));
        fiveYear.add(3, 32, 2, t("A uniform for each weekday", "Eine Uniform für jeden Wochentag"))
                .setChecked(Variant.WEEKDAY.equals(plan));
        fiveYear.setGroupCheckable(3, true, true);
        SubMenu formations = menu.addSubMenu(0, 0, 0, t("Formations", "Formationen"));
        for (int i = 0; i < Formation.ALL.length; i++) {
            Formation f = Formation.ALL[i];
            formations.add(6, 200 + i, i, Lang.pick(german, f.title) + " — " + Lang.pick(german, f.note))
                    .setChecked(f == home.formation);
        }
        formations.setGroupCheckable(6, true, true);
        menu.add(0, 12, 0, t("Papers & permanent record", "Papiere & ständige Akte"));
        menu.add(0, 20, 0, t("Weekly report", "Wochenbericht"));
        menu.add(0, 18, 0, t("Mobilisation orders (tour dates)", "Einberufungen (Tourdaten)"));
        menu.add(0, 13, 0, t("The Constitution", "Die Verfassung"));
        boolean camera = home.orders.cameraIssued();
        SubMenu art = menu.addSubMenu(0, 0, 0, camera ? t("Art & camera", "Kunst & Kamera") : t("Art", "Kunst"));
        art.add(0, 5, 0, t("Volkskunst (your art)", "Volkskunst (Ihre Kunst)"));
        art.add(0, 6, 1, t("Next approved artwork", "Nächstes genehmigtes Kunstwerk"));
        art.add(0, 34, 1, t("Poster press", "Plakatpresse"));
        if (camera) art.add(0, 23, 2, t("Laibach Camera (the darkroom)", "Laibach-Kamera (die Dunkelkammer)"));
        SubMenu duties = menu.addSubMenu(0, 0, 0, t("Duties (mobilise, roll call, drill)", "Pflichten (Mobilisierung, Appell, Übung)"));
        long until = Citizen.mobilisedUntil(home);
        SubMenu mobilise = duties.addSubMenu(0, 0, 0, until > 0
                ? t("Mobilised until ", "Mobilisiert bis ") + DateFormat.format("HH:mm", until)
                : t("Mobilise (essential programmes only)", "Mobilisieren (nur unentbehrliche Programme)"));
        mobilise.add(0, 41, 0, t("For 30 minutes", "Für 30 Minuten"));
        mobilise.add(0, 42, 1, t("For 1 hour", "Für 1 Stunde"));
        mobilise.add(0, 43, 2, t("For 2 hours", "Für 2 Stunden"));
        if (until > 0) mobilise.add(0, 44, 3, t("Stand down now", "Jetzt demobilisieren"));
        duties.add(0, 14, 1, t("Report for roll call", "Zum Appell melden"));
        duties.add(0, 10, 2, t("Curfew drill", "Sperrstundenübung"));
        duties.add(0, 36, 3, t("Drill schedule", "Übungsplan"));
        menu.add(0, 35, 0, t("Citizen exchange (register a comrade)", "Bürgeraustausch (einen Genossen registrieren)"));
        menu.add(0, 1, 0, home.register.showHidden ? t("Hide redacted", "Geschwärzte verbergen") : t("Show redacted", "Geschwärzte zeigen"));
        menu.add(0, 4, 0, t("Settings", "Einstellungen"));
        StateMenu.show(anchor, v, "LAIBACH LIFE™", menu, Gravity.START, item -> home.orders.act(item.getItemId()));
    }

    void addVariants(Menu menu) {
        for (int i = 0; i < Variant.ALL.length; i++) {
            menu.add(1, 100 + i, i, Variant.ALL[i].name).setChecked(Variant.ALL[i] == v);
        }
        menu.setGroupCheckable(1, true, true);
    }
}
