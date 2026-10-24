package dev.laibach.launcher;

import java.util.List;

/** The ticker's news: the state's bulletins, the call-ups and what is on air. */
final class News extends HomePart {

    News(HomeActivity home) {
        super(home);
    }

    /** The ticker's text: the state's news, with the citizen's own number in it. */
    String text() {
        String n = Citizen.number(home);
        String[][] news = {
                {"YOUR THOUGHTS ARE BACKED UP HOURLY", "IHRE GEDANKEN WERDEN STÜNDLICH GESICHERT"},
                {"THE ALGORITHM LOVES YOU", "DER ALGORITHMUS LIEBT SIE"},
                {"WEATHER: CONTROLLED", "WETTER: KONTROLLIERT"},
                {"THE FUTURE ARRIVED ON SCHEDULE", "DIE ZUKUNFT KAM PÜNKTLICH"},
                {"CITIZEN № " + n + " IS OBSERVED FOR THEIR OWN SAFETY",
                        "BÜRGER NR. " + n + " WIRD ZU SEINER SICHERHEIT BEOBACHTET"},
                {"DISSENT HAS BEEN DEPRECATED", "ABWEICHUNG WURDE ABGEKÜNDIGT"},
                {"NOSTALGIA IS AVAILABLE BY SUBSCRIPTION", "NOSTALGIE GIBT ES IM ABONNEMENT"},
                {"ALL CLOCKS ARE CORRECT", "ALLE UHREN GEHEN RICHTIG"},
                {"THE COLLECTIVE HAS NO BUGS, ONLY FEATURES", "DAS KOLLEKTIV HAT KEINE FEHLER, NUR FUNKTIONEN"},
                {"SLEEP IS A SCHEDULED UPDATE", "SCHLAF IST EIN GEPLANTES UPDATE"},
                {"REMAIN CALM. REMAIN ONLINE.", "BLEIBEN SIE RUHIG. BLEIBEN SIE ONLINE."},
        };
        StringBuilder b = new StringBuilder();
        if (Fx.austerity) {
            b.append("   ✚   ").append(t("AUSTERITY MEASURES IN FORCE. COLOUR AND MOTION ARE SUSPENDED UNTIL THE CITIZEN CHARGES",
                    "SPARMASSNAHMEN IN KRAFT. FARBE UND BEWEGUNG SIND BIS ZUM AUFLADEN AUSGESETZT"));
        }
        List<Bulletins.Order> orders = Bulletins.orders(home);
        if (!orders.isEmpty()) {
            b.append("   ✚   ").append(t("MOBILISATION ORDER: ", "EINBERUFUNG: ")).append(home.callUps.order(orders.get(0)));
            if (orders.size() > 1) {
                b.append("   ✚   ").append(String.format(locale, t("%d FURTHER CALL-UPS ISSUED. TAP FOR ORDERS",
                        "%d WEITERE EINBERUFUNGEN ERGANGEN. TIPPEN FÜR BEFEHLE"), orders.size() - 1));
            }
        }
        String[] tv = Bulletins.latestTv(home);
        if (tv != null) {
            b.append("   ✚   ").append(t("NOW ON LAIBACH TV: ", "JETZT IM LAIBACH-FERNSEHEN: "))
                    .append(tv[1].toUpperCase(locale));
        }
        for (String[] line : news) b.append("   ✚   ").append(Lang.pick(german, line));
        return b.append("   ✚   ").append(v.name).append(": ").append(v.motto(german))
                .append("   ✚   LAIBACH OS 2.0").toString();
    }
}
