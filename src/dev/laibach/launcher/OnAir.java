package dev.laibach.launcher;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** The on-air strip under the monument: the newest Laibach TV transmission. */
final class OnAir extends HomePart {
    LinearLayout strip;
    ImageView still;
    TextView title;

    OnAir(HomeActivity home) {
        super(home);
    }

    /** On air: the newest Laibach TV transmission; tap to watch it on the state broadcaster. */
    View build() {
        strip = row();
        strip.setGravity(Gravity.CENTER_VERTICAL);
        strip.setPadding(0, dp(12), 0, 0);
        still = new ImageView(home);
        still.setScaleType(ImageView.ScaleType.CENTER_CROP);
        still.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        GradientDrawable frame = new GradientDrawable();
        frame.setColor(v.rule);
        frame.setStroke(Math.max(1, dp(1)), v.accent);
        still.setBackground(frame);
        still.setPadding(Math.max(1, dp(1)), Math.max(1, dp(1)), Math.max(1, dp(1)), Math.max(1, dp(1)));
        strip.addView(still, new LinearLayout.LayoutParams(dp(80), dp(45)));
        title = type(11, v.ink, 70, 700);
        title.setLetterSpacing(0.08f);
        title.setMaxLines(2);
        title.setEllipsize(TextUtils.TruncateAt.END);
        title.setPadding(dp(12), 0, 0, 0);
        strip.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        strip.setVisibility(View.GONE);
        return strip;
    }

    /** The strip from the news last received; it stands down when there is nothing on air. */
    void show() {
        String[] tv = Bulletins.latestTv(home);
        if (tv == null || !home.formation.has(Formation.TV)) {
            strip.setVisibility(View.GONE);
            return;
        }
        SpannableStringBuilder s = new SpannableStringBuilder(t("■ ON AIR · LAIBACH TV", "■ AUF SENDUNG · LAIBACH TV"));
        s.setSpan(new ForegroundColorSpan(v.accent), 0, s.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        s.append("\n").append(tv[1].toUpperCase(locale));
        title.setText(s);
        android.graphics.Bitmap picture = Bulletins.still(tv[0]);
        still.setImageBitmap(picture);
        still.setVisibility(picture == null ? View.GONE : View.VISIBLE);
        strip.setContentDescription(t("Laibach TV, newest: ", "Laibach TV, neueste Sendung: ") + tv[1]);
        strip.setOnClickListener(view -> home.startActivity(new Intent(home, BroadcastActivity.class)
                .putExtra(BroadcastActivity.EXTRA_VIDEO_ID, tv[0])
                .putExtra(BroadcastActivity.EXTRA_TITLE, tv[1])));
        strip.setVisibility(View.VISIBLE);
    }
}
