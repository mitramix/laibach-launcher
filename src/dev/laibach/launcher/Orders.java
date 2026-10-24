package dev.laibach.launcher;

import android.app.TimePickerDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.widget.Toast;
import java.util.Locale;

/** The orders the gear menu and the regulations give, carried out: one id, one order. */
final class Orders extends HomePart {
    Orders(HomeActivity home) {
        super(home);
    }

    /** Carries out an order from the gear menu or the regulations. */
    boolean act(int id) {
        switch (id) {
            case 12:
                home.startActivity(new Intent(home, PapersActivity.class));
                return true;
            case 13:
                home.startActivity(new Intent(home, PapersActivity.class).putExtra(PapersActivity.EXTRA_CONSTITUTION, true));
                return true;
            case 14:
                home.rollCall.summon();
                return true;
            case 5:
                home.startActivity(new Intent(home, VolkskunstActivity.class));
                return true;
            case 6:
                home.artCredit.next();
                return true;
            case 71:
            case 72:
            case 73: {
                String to = id == 71 ? Lang.EN : id == 72 ? Lang.DE : Lang.SL;
                if (to.equals(Lang.code(home))) return true;
                String[] names = id == 71 ? new String[]{"ENGLISH", "ENGLISCH"}
                        : id == 72 ? new String[]{"GERMAN", "DEUTSCH"} : new String[]{"SLOVENIAN", "SLOWENISCH"};
                Record.note(home, "CITIZEN NOW THINKS IN " + names[0] + ".", "BÜRGER DENKT JETZT AUF " + names[1] + ".");
                Lang.set(home, to);
                if (LockWallpaper.dressed(home)) LockWallpaper.dress(home, v, Lang.DE.equals(to), null);
                home.recreate();
                return true;
            }
            case 18:
                home.callUps.show();
                return true;
            case 20:
                Report.show(home, v);
                return true;
            case 30:
            case 31:
            case 32: {
                String to = id == 31 ? Variant.WEEKLY : id == 32 ? Variant.WEEKDAY : "";
                Variant.setPlan(home, to);
                Record.note(home, to.isEmpty() ? "FIVE-YEAR PLAN SUSPENDED. THE CITIZEN DRESSES UNSUPERVISED."
                                : "FIVE-YEAR PLAN ADOPTED: THE UNIFORM IS ISSUED BY THE CALENDAR.",
                        to.isEmpty() ? "FÜNFJAHRESPLAN AUSGESETZT. DER BÜRGER KLEIDET SICH SELBST."
                                : "FÜNFJAHRESPLAN ANGENOMMEN: DIE UNIFORM GIBT DER KALENDER AUS.");
                Variant next = Variant.current(home);
                if (next != v) {
                    if (LockWallpaper.dressed(home)) LockWallpaper.dress(home, next, german, null);
                    home.recreate();
                }
                return true;
            }
            case 41:
            case 42:
            case 43: {
                int minutes = id == 41 ? 30 : id == 42 ? 60 : 120;
                Citizen.mobilise(home, minutes * 60_000L);
                Record.note(home, "CITIZEN MOBILISED FOR " + minutes + " MINUTES. NON-ESSENTIAL PROGRAMMES STOOD DOWN.",
                        "BÜRGER FÜR " + minutes + " MINUTEN MOBILISIERT. NICHT UNENTBEHRLICHE PROGRAMME ZURÜCKGESTELLT.");
                Toast.makeText(home, t("Mobilised. Mark a programme essential by holding it.",
                        "Mobilisiert. Halten Sie ein Programm, um es als unentbehrlich zu markieren."), Toast.LENGTH_LONG).show();
                home.applyFilter();
                return true;
            }
            case 44:
                demobilise();
                return true;
            case 51:
                setReveille();
                return true;
            case 52:
            case 53:
                Alarm.setTone(home, id == 52 ? Alarm.BELL : Alarm.ANTHEM);
                return true;
            case 54:
                Alarm.set(home, -1);
                Record.note(home, "REVEILLE CANCELLED. THE CITIZEN WAKES UNASSISTED.",
                        "WECKRUF AUFGEHOBEN. DER BÜRGER ERWACHT VON SELBST.");
                return true;
            case 23:
                home.startActivity(new Intent(home, DarkroomActivity.class));
                return true;
            case 24: {
                boolean issue = !cameraIssued();
                home.getPackageManager().setComponentEnabledSetting(new ComponentName(home, DarkroomActivity.class),
                        issue ? PackageManager.COMPONENT_ENABLED_STATE_DEFAULT : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP);
                Record.note(home, issue ? "THE CAMERA WAS ISSUED." : "THE CAMERA WAS WITHDRAWN.",
                        issue ? "DIE KAMERA WURDE AUSGEGEBEN." : "DIE KAMERA WURDE EINGEZOGEN.");
                return true;
            }
            case 26: {
                boolean issue = !censorIssued();
                home.getPackageManager().setComponentEnabledSetting(new ComponentName(home, CensorActivity.class),
                        issue ? PackageManager.COMPONENT_ENABLED_STATE_DEFAULT : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP);
                Record.note(home, issue ? "THE CENSOR'S DESK WAS OPENED." : "THE CENSOR'S DESK WAS CLOSED.",
                        issue ? "DER TISCH DES ZENSORS WURDE AUFGESTELLT." : "DER TISCH DES ZENSORS WURDE GERÄUMT.");
                return true;
            }
            case 25:
                issueSounds();
                return true;
            case 21:
                Tones.setOn(home, !Tones.on(home));
                if (Tones.on(home)) Tones.play(home, Tones.charging());
                return true;
            case 28:
                Tones.setTypewriter(home, !Tones.typewriter(home));
                Tones.type(home, Tones.typing(6));
                return true;
            case 29:
                Hourly.setOn(home, !Hourly.on(home));
                if (Hourly.on(home)) Tones.play(home, Tones.hourly(java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)));
                return true;
            case 33:
                home.deadDrop.edit();
                return true;
            case 34:
                PosterPress.show(home);
                return true;
            case 35:
                home.startActivity(new Intent(home, ExchangeActivity.class));
                return true;
            case 36:
                home.drills.edit();
                return true;
            case 37:
                keepRecord(!Record.on(home));
                return true;
            case 22:
                Morse.setOn(home, !Morse.on(home));
                if (Morse.on(home)) Morse.tap(home, true);
                return true;
            case 8:
                open(DecreeListener.settings());
                return true;
            case 9:
                open(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
                return true;
            case 10:
                Record.note(home, "CURFEW DRILL ORDERED.", "SPERRSTUNDENÜBUNG ANGEORDNET.");
                home.curfew.startDrill();
                return true;
            case 11:
                Citizen.setCurfewOn(home, !Citizen.curfewOn(home));
                if (Citizen.curfewOn(home)) {
                    Record.note(home, "CURFEW RESTORED BY POPULAR DEMAND.", "SPERRSTUNDE AUF VIELFACHEN WUNSCH WIEDER EINGEFÜHRT.");
                } else {
                    Record.note(home, "CURFEW ABOLISHED. THE CITIZEN WILL BE WATCHED MORE CLOSELY.",
                            "SPERRSTUNDE ABGESCHAFFT. DER BÜRGER WIRD GENAUER BEOBACHTET.");
                }
                Toast.makeText(home, Citizen.curfewOn(home)
                        ? t("Curfew restored. Rest well.", "Sperrstunde wieder eingeführt. Ruhen Sie wohl.")
                        : t("Curfew abolished. The Collective trusts you. For now.",
                                "Sperrstunde abgeschafft. Das Kollektiv vertraut Ihnen. Noch."),
                        Toast.LENGTH_LONG).show();
                home.curfew.update();
                return true;
            case 27: {
                boolean on = !Quotas.on(home);
                Quotas.setOn(home, on);
                Record.note(home, on ? "MINISTRY QUOTAS INTRODUCED. EVERY MINISTRY NOW KNOWS ITS LIMIT."
                                : "MINISTRY QUOTAS LIFTED. THE MINISTRIES ARE OPEN AROUND THE CLOCK.",
                        on ? "KONTINGENTE DER MINISTERIEN EINGEFÜHRT. JEDES MINISTERIUM KENNT NUN SEINE GRENZE."
                                : "KONTINGENTE DER MINISTERIEN AUFGEHOBEN. DIE MINISTERIEN SIND RUND UM DIE UHR GEÖFFNET.");
                toast(on && !Citizen.usageAccess(home)
                        ? t("Quotas introduced. They are measured once usage access is granted.",
                                "Kontingente eingeführt. Gemessen wird, sobald Nutzungszugriff erteilt ist.")
                        : on ? t("Quotas introduced. Spend wisely.", "Kontingente eingeführt. Haushalten Sie gut.")
                        : t("Quotas lifted. The ministries thank you.", "Kontingente aufgehoben. Die Ministerien danken Ihnen."));
                return true;
            }
            case 15:
                boolean calm = !Fx.pacified(home);
                Fx.setPacified(home, calm);
                Record.note(home, calm ? "DISPLAY PACIFIED. NOTHING MOVES. NOTHING NEEDS TO."
                                : "DISPLAY AGITATED. THE STATE IS IN MOTION AGAIN.",
                        calm ? "ANZEIGE BEFRIEDET. NICHTS BEWEGT SICH. NICHTS MUSS."
                                : "ANZEIGE AUFGEWIEGELT. DER STAAT IST WIEDER IN BEWEGUNG.");
                home.recreate();
                return true;
            case 16:
                boolean seal = !LockActivity.enabled(home);
                LockActivity.setEnabled(home, seal);
                Record.note(home, seal ? "TERMINAL SEALED. EVERY WAKING NOW BEGINS WITH SUBMISSION."
                                : "TERMINAL UNSEALED. THE CITIZEN ENTERS UNANNOUNCED.",
                        seal ? "TERMINAL VERSIEGELT. JEDES ERWACHEN BEGINNT NUN MIT UNTERWERFUNG."
                                : "TERMINAL ENTSIEGELT. DER BÜRGER TRITT UNANGEMELDET EIN.");
                Toast.makeText(home, seal
                        ? t("Terminal sealed. It will be there when the screen wakes.",
                                "Terminal versiegelt. Es erwartet Sie beim nächsten Einschalten.")
                        : t("Terminal unsealed. Your phone's own lock is unchanged.",
                                "Terminal entsiegelt. Die eigene Sperre Ihres Telefons bleibt unverändert."),
                        Toast.LENGTH_LONG).show();
                return true;
            case 17:
                dressLockScreen();
                return true;
            case 1:
                home.showHidden = !home.showHidden;
                home.applyFilter();
                return true;
            case 3:
                Record.note(home, "CITIZEN CONSIDERED DEFECTION.", "BÜRGER ERWOG DAS ÜBERLAUFEN.");
                open(new Intent(Settings.ACTION_HOME_SETTINGS));
                return true;
            case 4:
                home.regulations.show(0);
                return true;
            case 7:
                open(new Intent(Settings.ACTION_SETTINGS));
                return true;
            default:
                return pickVariant(id) || pickFormation(id);
        }
    }

    void demobilise() {
        Citizen.mobilise(home, 0);
        Record.note(home, "CITIZEN STOOD DOWN. ALL PROGRAMMES RETURN TO THEIR POSTS.",
                "BÜRGER DEMOBILISIERT. ALLE PROGRAMME KEHREN AUF IHRE POSTEN ZURÜCK.");
        home.applyFilter();
    }

    /** Sets the reveille's time in a clock dressed in the uniform. */
    void setReveille() {
        int at = Alarm.minutes(home);
        if (at < 0) at = 6 * 60;
        TimePickerDialog d = new TimePickerDialog(home, android.R.style.Theme_DeviceDefault_Dialog_Alert,
                (picker, hour, minute) -> {
                    Alarm.set(home, hour * 60 + minute);
                    String time = String.format(Locale.ROOT, "%02d:%02d", hour, minute);
                    Record.note(home, "REVEILLE ORDERED FOR " + time + ".", "WECKRUF ANGEORDNET FÜR " + time + ".");
                    Toast.makeText(home, Alarm.permitted(home)
                            ? t("Reveille at ", "Weckruf um ") + time + t(". Answer the roll call to silence it.",
                                    ". Beantworten Sie den Appell, um ihn zu beenden.")
                            : t("Android does not allow the state to ring at an exact time.",
                                    "Android erlaubt dem Staat nicht, zu einer genauen Zeit zu läuten."),
                            Toast.LENGTH_LONG).show();
                    home.regulations.refresh();
                }, at / 60, at % 60, true);
        d.setTitle(t("Reveille", "Weckruf"));
        d.show();
        StateMenu.dress(d, v);
    }

    /** Whether the camera's icon and share entry are out (the owner can withdraw them). */
    boolean cameraIssued() {
        return home.getPackageManager().getComponentEnabledSetting(new ComponentName(home, DarkroomActivity.class))
                != PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
    }

    /** Whether the censor's desk is in the share menu (the owner can dismiss it). */
    boolean censorIssued() {
        return home.getPackageManager().getComponentEnabledSetting(new ComponentName(home, CensorActivity.class))
                != PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
    }

    /** Writes the state's sounds where the phone's sound settings find them; the owner picks them there. */
    void issueSounds() {
        Toast.makeText(home, t("Pressing the records …", "Die Platten werden gepresst …"), Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            int n = Exports.sounds(home);
            home.runOnUiThread(() -> {
                if (n > 0) {
                    Record.note(home, n + " STATE SOUNDS WERE ISSUED TO THE PHONE.", n + " KLÄNGE DES STAATES WURDEN ANS TELEFON AUSGEGEBEN.");
                }
                Toast.makeText(home, n > 0
                        ? n + t(" sounds issued. Choose them in the phone's sound settings, under Laibach.",
                                " Klänge ausgegeben. Wählen Sie sie in den Toneinstellungen des Telefons, unter Laibach.")
                        : t("The sounds could not be issued.", "Die Klänge ließen sich nicht ausgeben."), Toast.LENGTH_LONG).show();
            });
        }, "sounds").start();
    }

    boolean pickVariant(int id) {
        int i = id - 100;
        if (i < 0 || i >= Variant.ALL.length) return false;
        // Choosing a uniform by hand suspends the five-year plan.
        if (!Variant.plan(home).isEmpty()) {
            Variant.setPlan(home, "");
            if (Variant.ALL[i] == v) home.prefs.edit().putString(Variant.KEY, v.name).apply();
        }
        if (Variant.ALL[i] != v) {
            Record.note(home, "UNIFORM CHANGED TO " + Variant.ALL[i].name + ".",
                    "UNIFORM GEWECHSELT ZU " + Variant.ALL[i].name + ".");
            home.prefs.edit().putString(Variant.KEY, Variant.ALL[i].name).apply();
            if (LockWallpaper.dressed(home)) LockWallpaper.dress(home, Variant.ALL[i], german, null);
            Tones.play(home, Tones.signature(Variant.ALL[i].name));
            home.recreate();
        }
        return true;
    }

    boolean pickFormation(int id) {
        int i = id - 200;
        if (i < 0 || i >= Formation.ALL.length) return false;
        Formation f = Formation.ALL[i];
        if (f != home.formation) {
            Record.note(home, "FORMATION CHANGED TO " + f.title[0] + ".", "FORMATION GEÄNDERT ZU " + f.title[1] + ".");
            Formation.set(home, f);
            Tones.play(home, Tones.stamp());
            home.recreate();
        }
        return true;
    }

    /**
     * Puts the uniform on the phone's own lock and PIN screens as their picture,
     * or takes it off again. The seal is lifted when the lock screen is dressed:
     * it would only cover the uniform, and it keeps the fingerprint sensor off.
     */
    void dressLockScreen() {
        if (LockWallpaper.dressed(home)) {
            boolean ownLost = LockWallpaper.hadOwn(home);
            if (!LockWallpaper.undress(home)) {
                Toast.makeText(home, t("The lock screen refused to undress.", "Der Sperrbildschirm verweigert das Entkleiden."),
                        Toast.LENGTH_LONG).show();
                return;
            }
            Record.note(home, "LOCK SCREEN UNDRESSED. IT WEARS ITS OWN CLOTHES AGAIN.",
                    "SPERRBILDSCHIRM ENTKLEIDET. ER TRÄGT WIEDER SEINE EIGENEN KLEIDER.");
            Toast.makeText(home, ownLost
                    ? t("Lock screen undressed. It shows your home picture; choose its own again in Settings → Wallpaper.",
                            "Sperrbildschirm entkleidet. Er zeigt Ihr Startbild; sein eigenes wählen Sie in Einstellungen → Hintergrund.")
                    : t("Lock screen undressed. Your old picture is back.", "Sperrbildschirm entkleidet. Ihr altes Bild ist zurück."),
                    Toast.LENGTH_LONG).show();
            return;
        }
        Variant uniform = v;
        LockWallpaper.dress(home, uniform, german, ok -> {
            if (!ok) {
                Toast.makeText(home, t("The lock screen refused the uniform.", "Der Sperrbildschirm verweigert die Uniform."),
                        Toast.LENGTH_LONG).show();
                return;
            }
            boolean wasSealed = LockActivity.enabled(home);
            LockActivity.setEnabled(home, false);
            Record.note(home, "LOCK SCREEN DRESSED IN " + uniform.name + ".",
                    "SPERRBILDSCHIRM IN " + uniform.name + " GEKLEIDET.");
            Toast.makeText(home, wasSealed
                    ? t("Lock screen dressed in " + uniform.name + ". The seal is lifted; your finger goes straight to the sensor.",
                            "Sperrbildschirm in " + uniform.name + " gekleidet. Das Siegel ist aufgehoben; Ihr Finger geht direkt zum Sensor.")
                    : t("Lock screen dressed in " + uniform.name + ".", "Sperrbildschirm in " + uniform.name + " gekleidet."),
                    Toast.LENGTH_LONG).show();
            home.regulations.refresh();
        });
    }

    /** Opens the permanent record, or closes it after asking: closed, its entries are destroyed. */
    private void keepRecord(boolean on) {
        if (on) {
            Record.setOn(home, true);
            Record.note(home, "PERMANENT RECORD OPENED. FROM NOW ON, EVERYTHING IS NOTED.",
                    "STÄNDIGE AKTE ANGELEGT. AB JETZT WIRD ALLES VERMERKT.");
            Tones.play(home, Tones.stamp());
            toast(t("The record is kept. It stays on this phone.", "Die Akte wird geführt. Sie bleibt auf diesem Telefon."));
            return;
        }
        StateMenu.dress(new android.app.AlertDialog.Builder(home, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle(t("Close the record?", "Die Akte schließen?"))
                .setMessage(t("Its entries are destroyed and nothing more is written down.",
                        "Ihre Einträge werden vernichtet, und nichts wird mehr aufgeschrieben."))
                .setPositiveButton(t("Close it", "Schließen"), (d, w) -> {
                    Record.setOn(home, false);
                    toast(t("The record is closed. Nothing is kept.", "Die Akte ist geschlossen. Nichts wird aufbewahrt."));
                    home.regulations.refresh();
                })
                .setNegativeButton(t("Cancel", "Abbrechen"), null)
                .show(), v);
    }
}
