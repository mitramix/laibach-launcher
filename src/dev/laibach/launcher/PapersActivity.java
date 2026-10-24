package dev.laibach.launcher;

import android.graphics.Insets;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;


/**
 * The citizen's papers: identity card, the day's statistics, the article of the
 * day, self-criticism, and the permanent record. Opened from the papers line on
 * the home screen; with EXTRA_CONSTITUTION it opens on the full constitution.
 */
public class PapersActivity extends StateActivity {
    static final String EXTRA_CONSTITUTION = "constitution";

    LinearLayout content;
    private boolean revealed;
    /** The identity card as drawn, for the copies that leave the launcher. */
    View card;
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        // The papers are read through the same glass as the home screen.
        FrameLayout glass = new FrameLayout(this);
        glass.addView(buildUi());
        glass.addView(new Crt(this, v));
        wear(glass);
        if (state == null && getIntent().getBooleanExtra(EXTRA_CONSTITUTION, false)) new PapersDesk(this).showConstitution();
    }

    @Override
    protected void onResume() {
        super.onResume();
        fill();
    }

    // ---- Layout -------------------------------------------------------------

    private View buildUi() {
        LinearLayout root = column();
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            view.setPadding(bars.left + dp(20), bars.top + dp(6), bars.right + dp(20), 0);
            content.setPadding(0, 0, 0, bars.bottom + dp(24));
            return WindowInsets.CONSUMED;
        });

        LinearLayout head = row();
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = type(26, v.accent, 25, 1000);
        back.setText("◀");
        back.setPadding(0, 0, dp(12), 0);
        back.setOnClickListener(view -> finish());
        head.addView(back);
        TextView title = type(30, v.ink, 25, 1000);
        title.setText(t("PAPERS", "PAPIERE"));
        title.setLetterSpacing(0.14f);
        head.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView seal = type(14, v.accent, 40, 900);
        seal.setText(t("CONFIDENTIAL", "VERTRAULICH"));
        seal.setLetterSpacing(0.16f);
        head.addView(seal);
        root.addView(head);
        root.addView(rule(v.accent), ruleParams(dp(5), dp(6), 0));

        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        content = column();
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        return root;
    }

    /** Builds the papers afresh from the current state, section by section. */
    void fill() {
        content.removeAllViews();
        card = new PapersCard(this).build();
        content.addView(card);
        new PapersFigures(this).fill();
        new PapersAlbum(this).fill();
        new PapersDesk(this).fill();
        new PapersExports(this).fill();
        new PapersRecord(this).fill();
        Motion.pressLater(content);
        if (!revealed) {
            revealed = true;
            Motion.reveal(content, 80);
        }
    }
}
