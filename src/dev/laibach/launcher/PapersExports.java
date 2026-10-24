package dev.laibach.launcher;

import android.app.AlertDialog;
import android.net.Uri;
import android.widget.TextView;
import android.widget.Toast;

/** Papers that leave the launcher: the weekly report, the card as a specimen, the case file and the settings' backup. */
final class PapersExports extends PapersPart {
    PapersExports(PapersActivity papers) {
        super(papers);
    }

    void fill() {
        heading(t("PAPERS FOR EXPORT", "PAPIERE ZUR AUSFUHR"));
        content.addView(button(t("WEEKLY REPORT", "WOCHENBERICHT"), view -> Report.show(papers, v)), fullWidth());
        content.addView(button(t("SAVE THE CARD AS A PICTURE", "AUSWEIS ALS BILD SPEICHERN"), view -> saveCard()),
                spaced());
        content.addView(button(t("SHARE THE CARD (SPECIMEN)", "AUSWEIS TEILEN (MUSTER)"), view -> shareCard()),
                spaced());
        content.addView(button(t("PRINT THE CASE FILE (PDF)", "FALLAKTE DRUCKEN (PDF)"), view -> caseFile()), spaced());
        content.addView(button(t("BACK UP THE SETTINGS", "EINSTELLUNGEN SICHERN"), view -> backup()), spaced());
        content.addView(button(t("RESTORE THE SETTINGS", "EINSTELLUNGEN WIEDERHERSTELLEN"), view -> restore()),
                spaced());
        TextView where = type(10, v.muted, 80, 650);
        where.setText(t("SAVED IN PICTURES/LAIBACH AND DOWNLOAD/LAIBACH ON THIS PHONE. THE CARD IS A SPECIMEN, NOT A DOCUMENT.",
                "GESPEICHERT IN PICTURES/LAIBACH UND DOWNLOAD/LAIBACH AUF DIESEM TELEFON. DER AUSWEIS IST EIN MUSTER, KEIN DOKUMENT."));
        where.setLetterSpacing(0.1f);
        where.setPadding(0, dp(10), 0, 0);
        content.addView(where);
    }

    private void saveCard() {
        boolean ok = Exports.saveCard(papers, papers.card, v);
        if (ok) Record.note(papers, "CITIZEN CARD COPIED AS A SPECIMEN.", "BÜRGERAUSWEIS ALS MUSTER KOPIERT.");
        Toast.makeText(papers, ok ? t("Specimen saved in Pictures/Laibach.", "Muster in Pictures/Laibach gespeichert.")
                : t("The card could not be copied.", "Der Ausweis konnte nicht kopiert werden."), Toast.LENGTH_LONG).show();
    }

    private void shareCard() {
        Uri uri = Exports.shareCard(papers, papers.card, v);
        if (uri == null) {
            Toast.makeText(papers, t("The card could not be copied.", "Der Ausweis konnte nicht kopiert werden."),
                    Toast.LENGTH_LONG).show();
            return;
        }
        Record.note(papers, "CITIZEN CARD SHARED AS A SPECIMEN. IT CONFERS NO RIGHTS.",
                "BÜRGERAUSWEIS ALS MUSTER GETEILT. ER VERLEIHT KEINE RECHTE.");
        Exports.send(papers, uri, t("SHARE THE SPECIMEN", "DAS MUSTER TEILEN"));
    }

    private void caseFile() {
        boolean ok = CaseFile.caseFile(papers, german);
        if (ok) Record.note(papers, "CASE FILE PRINTED BY THE CITIZEN.", "FALLAKTE VOM BÜRGER GEDRUCKT.");
        Toast.makeText(papers, ok ? t("Case file saved in Download/Laibach.", "Fallakte in Download/Laibach gespeichert.")
                : t("The case file could not be printed.", "Die Fallakte konnte nicht gedruckt werden."), Toast.LENGTH_LONG).show();
    }

    private void backup() {
        boolean ok = Backup.backup(papers);
        Toast.makeText(papers, ok ? t("Settings saved in Download/Laibach.", "Einstellungen in Download/Laibach gesichert.")
                : t("The settings could not be saved.", "Die Einstellungen konnten nicht gesichert werden."), Toast.LENGTH_LONG).show();
    }

    private void restore() {
        String latest = Backup.latestBackup(papers);
        if (latest == null) {
            Toast.makeText(papers, t("No backup made by this launcher was found.",
                    "Keine Sicherung dieses Launchers gefunden."), Toast.LENGTH_LONG).show();
            return;
        }
        StateMenu.dress(new AlertDialog.Builder(papers, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle(t("Restore the settings?", "Einstellungen wiederherstellen?"))
                .setMessage(t("The settings, the record and the lists will be replaced by ",
                        "Einstellungen, Akte und Listen werden ersetzt durch ") + latest + ".")
                .setPositiveButton(t("Restore", "Wiederherstellen"), (d, which) -> {
                    boolean ok = Backup.restore(papers);
                    if (ok) {
                        Alarm.schedule(papers);
                        Record.note(papers, "SETTINGS RESTORED FROM " + latest + ". HISTORY HAS BEEN REVISED.",
                                "EINSTELLUNGEN AUS " + latest + " WIEDERHERGESTELLT. DIE GESCHICHTE WURDE ÜBERARBEITET.");
                        papers.recreate();
                    } else {
                        Toast.makeText(papers, t("The backup could not be read.", "Die Sicherung konnte nicht gelesen werden."),
                                Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton(t("Keep the present", "Die Gegenwart behalten"), null)
                .show(), v);
    }
}
