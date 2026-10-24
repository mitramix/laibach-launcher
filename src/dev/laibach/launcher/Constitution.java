package dev.laibach.launcher;

import java.util.Calendar;

/** The constitution of the Laibach Collective, English and German in step. */
final class Constitution {
    private Constitution() {}

    static final String[][] ARTICLES = {
            {"All citizens are equal. Their screen time is not.",
                    "Alle Bürger sind gleich. Ihre Bildschirmzeit ist es nicht."},
            {"The Collective is always online, so that the citizen need not be.",
                    "Das Kollektiv ist immer online, damit der Bürger es nicht sein muss."},
            {"Every citizen has the right to remain silent. Silence is recorded.",
                    "Jeder Bürger hat das Recht zu schweigen. Das Schweigen wird aufgezeichnet."},
            {"Attention is the property of the State. It is lent to the citizen daily, in rations.",
                    "Aufmerksamkeit ist Eigentum des Staates. Sie wird dem Bürger täglich zugeteilt."},
            {"Rest is compulsory from 23:00 to 06:00. Dreams are encouraged but not required.",
                    "Ruhe ist von 23:00 bis 06:00 Uhr Pflicht. Träume sind erwünscht, aber nicht vorgeschrieben."},
            {"Joy is permitted in measured quantities. Excess joy shall be reported.",
                    "Freude ist in bemessenen Mengen erlaubt. Übermäßige Freude ist zu melden."},
            {"Every notification is a decree. Answer it, or ignore it with dignity.",
                    "Jede Benachrichtigung ist ein Erlass. Beantworten Sie ihn oder übergehen Sie ihn mit Würde."},
            {"Programmes serve the ministries, the ministries serve the Collective, the Collective serves the citizen. The circle is closed.",
                    "Programme dienen den Ministerien, die Ministerien dem Kollektiv, das Kollektiv dem Bürger. Der Kreis ist geschlossen."},
            {"Freedom of choice is guaranteed. The choices are listed on the home screen.",
                    "Die Wahlfreiheit ist garantiert. Die Wahlmöglichkeiten stehen auf dem Startbildschirm."},
            {"History is updated automatically. Previous versions are not supported.",
                    "Die Geschichte wird automatisch aktualisiert. Frühere Versionen werden nicht unterstützt."},
            {"Every exemption is a confession.",
                    "Jede Ausnahme ist ein Geständnis."},
            {"This constitution may be amended without notice. You have been notified.",
                    "Diese Verfassung kann ohne Ankündigung geändert werden. Sie wurden benachrichtigt."},
            {"Nothing is forbidden that has not been anticipated.",
                    "Nichts ist verboten, was nicht vorhergesehen wurde."},
    };

    /** Index of today's article. */
    static int ofTheDay() {
        return Calendar.getInstance().get(Calendar.DAY_OF_YEAR) % ARTICLES.length;
    }

    /** Roman numeral for an article number, 1 to 39. */
    static String numeral(int n) {
        String[] tens = {"", "X", "XX", "XXX"};
        String[] ones = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX"};
        return tens[n / 10] + ones[n % 10];
    }
}
