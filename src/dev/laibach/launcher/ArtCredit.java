package dev.laibach.launcher;

import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.TextView;
import java.io.IOException;

/** The credit for the approved art behind the home screen, and the fetching of the next work. */
final class ArtCredit extends HomePart {
    TextView caption;
    boolean fetching;

    ArtCredit(HomeActivity home) {
        super(home);
    }

    /** Credit for the picture behind: tap for another, long-press for its gallery. */
    View build() {
        caption = type(10, v.muted, 80, 650);
        caption.setLetterSpacing(0.12f);
        caption.setMaxLines(2);
        caption.setEllipsize(TextUtils.TruncateAt.END);
        caption.setPadding(0, dp(12), 0, 0);
        caption.setOnClickListener(view -> next());
        caption.setOnLongClickListener(view -> {
            open(new Intent(Intent.ACTION_VIEW, Uri.parse(WallArt.SITE)));
            return true;
        });
        show(home.art.current);
        caption.setGravity(home.formation.gravity);
        if (!home.formation.has(Formation.ART)) caption.setVisibility(View.GONE);
        return caption;
    }

    /** Fetches another work from the Volkskunst gallery in the background. */
    void next() {
        if (fetching) return;
        ConnectivityManager cm = home.getSystemService(ConnectivityManager.class);
        if (cm.getActiveNetwork() == null) {
            if (home.art.current == null) show(null);
            return;
        }
        fetching = true;
        if (home.art.current == null) {
            caption.setText(t("■ ART IS BEING APPROVED …", "■ KUNST WIRD GENEHMIGT …"));
        }
        int width = home.getResources().getDisplayMetrics().widthPixels;
        new Thread(() -> {
            WallArt.Art fetched = null;
            try {
                fetched = WallArt.fetch(home, width);
            } catch (IOException | RuntimeException e) {
                android.util.Log.w("Laibach", "art of the day: " + e);
            }
            WallArt.Art result = fetched;
            home.runOnUiThread(() -> {
                fetching = false;
                if (home.isDestroyed()) return;
                if (result != null) home.art.setArt(result);
                show(home.art.current);
            });
        }, "wallart").start();
    }

    void show(WallArt.Art a) {
        String head = a == null
                ? t("■ ART WITHHELD: NO CONNECTION", "■ KUNST ZURÜCKGEHALTEN: KEINE VERBINDUNG")
                : t("■ APPROVED ART OF THE DAY", "■ GENEHMIGTE KUNST DES TAGES");
        SpannableStringBuilder s = new SpannableStringBuilder(head);
        s.setSpan(new ForegroundColorSpan(v.accent), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        if (a != null) {
            String title = a.title.isEmpty() ? t("UNTITLED", "OHNE TITEL") : a.title;
            String author = a.author.isEmpty() ? t("UNKNOWN", "UNBEKANNT") : a.author;
            s.append("\n").append(title.toUpperCase(locale)).append(" — ")
                    .append(t("FROM: ", "VON: ")).append(author.toUpperCase(locale));
        }
        caption.setText(s);
    }
}
