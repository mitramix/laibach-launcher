package dev.laibach.launcher;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Insets;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.PathShape;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * The state's menus and dialogs, in the uniform: its paper, ink and accent,
 * the condensed capitals of the home screen and a rule of the accent along
 * the top. A menu is filled like a {@link PopupMenu} and shown here instead;
 * a submenu takes the menu's place, with the way back at its head.
 */
final class StateMenu {
    private StateMenu() {
    }

    /**
     * Shows menu at anchor, aligned to its end edge when gravity is
     * {@link Gravity#END}. title (if any) heads the menu in small type.
     */
    static void show(View anchor, Variant v, CharSequence title, Menu menu, int gravity,
                     PopupMenu.OnMenuItemClickListener pick) {
        open(anchor, v, title, menu, gravity, pick, null);
    }

    /** back (if any) is where the head of the menu leads: the menu it came from. */
    private static void open(View anchor, Variant v, CharSequence title, Menu menu, int gravity,
                             PopupMenu.OnMenuItemClickListener pick, Runnable back) {
        Context c = anchor.getContext();
        Tones.play(c, Tones.relay());
        LinearLayout list = new LinearLayout(c);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(0, dp(c, 3), 0, dp(c, 6));
        ScrollView scroll = new ScrollView(c);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.addView(list);

        PopupWindow popup = new PopupWindow(scroll, ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, true);
        popup.setBackgroundDrawable(panel(c, v));
        popup.setElevation(dp(c, 14));
        popup.setOutsideTouchable(true);
        popup.setAnimationStyle(0);

        boolean headed = title != null;
        if (headed) {
            TextView head = caption(c, v, (back != null ? "◀  " : "■  ") + title);
            if (back != null) {
                head.setBackground(ripple(v));
                head.setOnClickListener(view -> {
                    popup.dismiss();
                    back.run();
                });
            }
            list.addView(head);
        }
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            if (!item.isVisible()) continue;
            if (list.getChildCount() > (headed ? 1 : 0)) list.addView(rule(c, v));
            TextView row = row(c, v, item);
            if (item.isEnabled()) {
                row.setOnClickListener(view -> {
                    popup.dismiss();
                    if (item.hasSubMenu()) {
                        open(anchor, v, item.getTitle(), item.getSubMenu(), gravity, pick,
                                () -> open(anchor, v, title, menu, gravity, pick, back));
                    } else {
                        pick.onMenuItemClick(item);
                    }
                });
            }
            list.addView(row);
        }
        place(anchor, popup, scroll, gravity);
    }

    /** Below the anchor if it fits, else above it, else as low as the screen allows. */
    private static void place(View anchor, PopupWindow popup, ScrollView content, int gravity) {
        Context c = anchor.getContext();
        View root = anchor.getRootView();
        int margin = dp(c, 12);
        Insets bars = Insets.NONE;
        WindowInsets insets = root.getRootWindowInsets();
        if (insets != null) bars = insets.getInsets(WindowInsets.Type.systemBars());
        int top = bars.top + margin, bottom = root.getHeight() - bars.bottom - margin;
        int maxWidth = root.getWidth() - 2 * margin;

        content.measure(View.MeasureSpec.makeMeasureSpec(maxWidth, View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        int w = Math.min(maxWidth, Math.max(dp(c, 248), content.getMeasuredWidth()));
        int h = Math.min(bottom - top, content.getMeasuredHeight());
        popup.setWidth(w);
        popup.setHeight(h);

        int[] at = new int[2];
        anchor.getLocationInWindow(at);
        int x = gravity == Gravity.END ? at[0] + anchor.getWidth() - w : at[0];
        x = Math.max(margin, Math.min(x, root.getWidth() - margin - w));
        int below = at[1] + anchor.getHeight();
        int y;
        if (below + h <= bottom) y = below;
        else if (at[1] - h >= top) y = at[1] - h;
        else y = Math.max(top, bottom - h);

        popup.showAtLocation(root, Gravity.TOP | Gravity.START, x, y);
        if (!Fx.pacified(c)) {
            content.setAlpha(0f);
            content.setTranslationY(-dp(c, 6));
            content.animate().alpha(1f).translationY(0f).setDuration(140).start();
        }
    }

    private static TextView row(Context c, Variant v, MenuItem item) {
        TextView t = new TextView(c);
        t.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        t.setLetterSpacing(0.08f);
        t.setAllCaps(true);
        t.setTextColor(item.isEnabled() ? v.ink : v.muted);
        t.setGravity(Gravity.CENTER_VERTICAL);
        t.setMinHeight(dp(c, 50));
        t.setPadding(dp(c, 20), dp(c, 10), dp(c, 20), dp(c, 10));
        t.setCompoundDrawablePadding(dp(c, 14));
        t.setText(item.getTitle());
        // Choices carry a square in front, submenus an arrow behind.
        t.setCompoundDrawablesRelative(item.isCheckable() ? mark(c, item.isChecked() ? v.accent : 0, v.muted) : null,
                null, item.hasSubMenu() ? arrow(c, v) : null, null);
        if (item.isEnabled()) t.setBackground(ripple(v));
        return t;
    }

    /** The small type above a menu: its subject, or the way back. */
    private static TextView caption(Context c, Variant v, CharSequence text) {
        TextView t = new TextView(c);
        t.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        t.setLetterSpacing(0.18f);
        t.setTextColor(v.accent);
        t.setSingleLine(true);
        t.setEllipsize(android.text.TextUtils.TruncateAt.END);
        t.setAllCaps(true);
        t.setPadding(dp(c, 20), dp(c, 14), dp(c, 20), dp(c, 10));
        t.setText(text);
        return t;
    }

    private static View rule(Context c, Variant v) {
        View r = new View(c);
        r.setBackgroundColor(v.rule);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, dp(c, 1)));
        lp.setMargins(dp(c, 20), 0, dp(c, 20), 0);
        r.setLayoutParams(lp);
        return r;
    }

    /** A square: filled in the accent when chosen, an outline otherwise. */
    private static Drawable mark(Context c, int fill, int outline) {
        GradientDrawable d = new GradientDrawable();
        int s = dp(c, 12);
        d.setSize(s, s);
        d.setColor(fill);
        d.setStroke(dp(c, 2), fill != 0 ? fill : outline);
        d.setBounds(0, 0, s, s);
        return d;
    }

    /** ▸ in the accent: a submenu lies behind it. */
    private static Drawable arrow(Context c, Variant v) {
        ShapeDrawable d = new ShapeDrawable(new PathShape(triangle(), 10, 10));
        d.getPaint().setColor(v.accent);
        int s = dp(c, 10);
        d.setIntrinsicWidth(s);
        d.setIntrinsicHeight(s);
        d.setBounds(0, 0, s, s);
        return d;
    }

    private static Path triangle() {
        Path p = new Path();
        p.moveTo(2, 0);
        p.lineTo(10, 5);
        p.lineTo(2, 10);
        p.close();
        return p;
    }

    private static Drawable ripple(Variant v) {
        return new RippleDrawable(ColorStateList.valueOf((v.accent & 0x00FFFFFF) | 0x40000000), null,
                new ColorDrawable(0xFFFFFFFF));
    }

    /** The uniform's paper, edged in its rule, with the accent along the top. */
    private static Drawable panel(Context c, Variant v) {
        GradientDrawable paper = new GradientDrawable();
        paper.setColor(v.bg);
        paper.setStroke(Math.max(1, dp(c, 1)), v.rule);
        LayerDrawable d = new LayerDrawable(new Drawable[]{paper, new ColorDrawable(v.accent)});
        d.setLayerHeight(1, dp(c, 3));
        d.setLayerGravity(1, Gravity.TOP | Gravity.FILL_HORIZONTAL);
        return d;
    }

    // ---- Dialogs ------------------------------------------------------------

    /**
     * Puts a dialog that is showing into the uniform: paper, accent rule,
     * condensed capitals for its title and buttons, the ink for its text.
     */
    static AlertDialog dress(AlertDialog d, Variant v) {
        Context c = d.getContext();
        d.getWindow().setBackgroundDrawable(new InsetDrawable(panel(c, v), dp(c, 20), dp(c, 24), dp(c, 20), dp(c, 24)));
        TextView title = d.findViewById(c.getResources().getIdentifier("alertTitle", "id", "android"));
        if (title != null) {
            title.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
            // The title keeps its own transformation, so the capitals are written in.
            title.setText(title.getText().toString().toUpperCase(c.getResources().getConfiguration().getLocales().get(0)));
            title.setLetterSpacing(0.1f);
            title.setTextColor(v.ink);
        }
        TextView message = d.findViewById(android.R.id.message);
        if (message != null) {
            message.setTypeface(Typeface.create("sans-serif-condensed", Typeface.NORMAL));
            message.setTextColor(v.ink);
        }
        int[] which = {AlertDialog.BUTTON_POSITIVE, AlertDialog.BUTTON_NEGATIVE, AlertDialog.BUTTON_NEUTRAL};
        for (int w : which) {
            Button b = d.getButton(w);
            if (b == null) continue;
            b.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
            b.setAllCaps(true);
            b.setLetterSpacing(0.1f);
            b.setTextColor(w == AlertDialog.BUTTON_POSITIVE ? v.accent : v.muted);
            b.setBackground(ripple(v));
        }
        paint(d.getWindow().getDecorView(), v);
        ListView list = d.getListView();
        if (list != null) {
            list.setDivider(new ColorDrawable(v.rule));
            list.setDividerHeight(Math.max(1, dp(c, 1)));
            list.setOnHierarchyChangeListener(new ViewGroup.OnHierarchyChangeListener() {
                @Override
                public void onChildViewAdded(View parent, View child) {
                    paint(child, v);
                }

                @Override
                public void onChildViewRemoved(View parent, View child) {
                }
            });
        }
        return d;
    }

    /** Fields and choices inside a dialog take the uniform too. */
    private static void paint(View view, Variant v) {
        if (view instanceof CheckedTextView) {
            CheckedTextView t = (CheckedTextView) view;
            t.setTextColor(v.ink);
            t.setTypeface(Typeface.create("sans-serif-condensed", Typeface.NORMAL));
            t.setCheckMarkTintList(new ColorStateList(new int[][]{{android.R.attr.state_checked}, {}},
                    new int[]{v.accent, v.muted}));
        } else if (view instanceof EditText) {
            EditText e = (EditText) view;
            e.setTextColor(v.ink);
            e.setHintTextColor(v.muted);
            e.setBackgroundTintList(ColorStateList.valueOf(v.accent));
        } else if (view instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) view;
            for (int i = 0; i < g.getChildCount(); i++) paint(g.getChildAt(i), v);
        }
    }

    private static int dp(Context c, float value) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                c.getResources().getDisplayMetrics()));
    }
}
