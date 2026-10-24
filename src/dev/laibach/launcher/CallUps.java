package dev.laibach.launcher;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import java.util.List;

/** The mobilisation orders: the band's coming concerts; tap one for its tickets. */
final class CallUps extends HomePart {
    CallUps(HomeActivity home) {
        super(home);
    }

    /** A call-up as one line: 30 JAN 2027 · LJUBLJANA, SLOVENIA · CANKARJEV DOM. */
    String order(Bulletins.Order o) {
        StringBuilder b = new StringBuilder(o.date.format(java.time.format.DateTimeFormatter.ofPattern(
                german ? "d. MMM yyyy" : "d MMM yyyy", locale)).toUpperCase(locale));
        b.append(" · ").append(o.city);
        if (!o.country.isEmpty()) b.append(", ").append(o.country);
        if (!o.place.isEmpty()) b.append(" · ").append(o.place.toUpperCase(locale));
        if (o.soldOut) b.append(t(" · SOLD OUT", " · AUSVERKAUFT"));
        return b.toString();
    }

    /** The mobilisation orders: the band's coming concerts; tap one for its tickets. */
    void show() {
        List<Bulletins.Order> orders = Bulletins.orders(home);
        AlertDialog.Builder b = new AlertDialog.Builder(home, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle(t("Mobilisation orders", "Einberufungen"))
                .setPositiveButton(t("All orders (laibach.org)", "Alle Befehle (laibach.org)"), (d, w) ->
                        open(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.laibach.org/future-events/"))))
                .setNegativeButton(t("Dismiss", "Wegtreten"), null);
        if (orders.isEmpty()) {
            b.setMessage(t("No call-ups have been received. Stand by.", "Keine Einberufungen empfangen. Halten Sie sich bereit."));
        } else {
            String[] lines = new String[orders.size()];
            for (int i = 0; i < lines.length; i++) lines[i] = order(orders.get(i));
            b.setItems(lines, (d, which) -> {
                Bulletins.Order o = orders.get(which);
                Record.note(home, "MOBILISATION ORDER FOR " + o.city + " READ.", "EINBERUFUNG NACH " + o.city + " GELESEN.");
                open(new Intent(Intent.ACTION_VIEW, Uri.parse(o.link.startsWith("http") ? o.link
                        : "https://www.laibach.org/future-events/")));
            });
        }
        StateMenu.dress(b.show(), v);
    }
}
