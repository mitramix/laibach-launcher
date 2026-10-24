package dev.laibach.launcher;

import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Draws the register: a programme to a row, a floor of a grid, or a ministry over its programmes. */
final class ProgrammeAdapter extends BaseAdapter {
    final HomeActivity home;
    final Variant v;
    final boolean german;
    final Locale locale;

    /** One row of a grid formation: as many programmes as it has columns, or fewer. */
    static final class Floor {
        final List<App> apps = new ArrayList<>();
    }

    ProgrammeAdapter(HomeActivity home) {
        this.home = home;
        this.v = home.v;
        this.german = home.german;
        this.locale = home.locale;
    }

    @Override public int getCount() { return home.rows.size(); }
    @Override public Object getItem(int i) { return home.rows.get(i); }
    @Override public long getItemId(int i) { return i; }
    @Override public int getViewTypeCount() { return 3; }
    @Override public int getItemViewType(int i) {
        Object o = home.rows.get(i);
        return o instanceof App ? 0 : o instanceof Ministry ? 1 : 2;
    }
    // A floor is not one item: each of its programmes answers for itself.
    @Override public boolean isEnabled(int i) { return home.rows.get(i) instanceof App; }
    @Override public boolean areAllItemsEnabled() { return false; }

    @Override
    public View getView(int i, View convertView, ViewGroup parent) {
        if (home.rows.get(i) instanceof Ministry) return header((Ministry) home.rows.get(i), (LinearLayout) convertView);
        if (home.rows.get(i) instanceof Floor) return floor((Floor) home.rows.get(i), (LinearLayout) convertView);
        boolean terminal = home.formation.list == Formation.TERMINAL;
        LinearLayout row = (LinearLayout) convertView;
        if (row == null) {
            row = home.row();
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, home.dp(home.formation.rowPad), 0, home.dp(home.formation.rowPad));
            TextView number = home.type(terminal ? home.formation.labelSp : 13, v.accent, 25, 900);
            number.setMinWidth(home.dp(30));
            if (!home.formation.has(Formation.NUMBERS)) number.setVisibility(View.GONE);
            row.addView(number);
            ImageView icon = new ImageView(home);
            if (!home.formation.icons) icon.setVisibility(View.GONE);
            row.addView(icon, new LinearLayout.LayoutParams(home.iconPx, home.iconPx));
            // A register has room for a wide name; a parade sets it narrow, heavy and large.
            boolean parade = home.formation.list == Formation.LABELS;
            TextView label = home.type(home.formation.labelSp, v.ink, parade ? 25 : 40, parade ? 900 : 800);
            label.setLetterSpacing(parade ? 0.1f : 0.06f);
            label.setSingleLine(true);
            label.setEllipsize(TextUtils.TruncateAt.END);
            label.setGravity(home.formation.gravity);
            label.setPadding(home.formation.icons || home.formation.has(Formation.NUMBERS) ? home.dp(14) : 0, 0, 0, 0);
            row.addView(label, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            TextView serial = home.type(9, v.muted, 70, 700);
            serial.setLetterSpacing(0.14f);
            serial.setPadding(home.dp(8), 0, 0, 0);
            if (parade) serial.setVisibility(View.GONE);
            row.addView(serial);
            if (terminal) {
                Typeface mono = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD);
                number.setTypeface(mono);
                label.setTypeface(mono);
                label.setLetterSpacing(0);
                serial.setTypeface(Typeface.MONOSPACE);
            }
        }
        App a = (App) home.rows.get(i);
        int n = home.shown.indexOf(a);
        TextView number = (TextView) row.getChildAt(0);
        ImageView icon = (ImageView) row.getChildAt(1);
        TextView label = (TextView) row.getChildAt(2);
        boolean first = home.querying && n == 0;
        number.setText(first ? "▶" : String.format(locale, "%02d", n + 1));
        if (home.formation.icons) icon.setImageDrawable(home.iconFor(a));
        label.setText((terminal ? "> " : "") + a.label.toUpperCase(locale));
        // Every programme has its file number.
        ((TextView) row.getChildAt(3)).setText(String.format(Locale.ROOT, "ID·%04X", a.key.hashCode() & 0xFFFF));
        // The first match is what the Go key opens.
        label.setTextColor(first ? v.accent : v.ink);
        row.setAlpha(a.suspended || a.hidden ? 0.4f : 1f);
        return row;
    }

    /** A floor of a grid: a cell per column, each a programme that opens on a tap. */
    View floor(Floor fl, LinearLayout row) {
        if (row == null) {
            row = home.row();
            int gap = home.formation.boxed ? home.dp(3) : 0;
            row.setPadding(0, gap, 0, gap);
            for (int c = 0; c < home.formation.columns; c++) {
                LinearLayout cell = home.column();
                cell.setGravity(home.formation.icons ? Gravity.CENTER_HORIZONTAL : Gravity.START);
                cell.setPadding(home.dp(6), home.dp(home.formation.rowPad), home.dp(6), home.dp(home.formation.rowPad));
                TextView number = home.type(10, v.accent, 25, 900);
                number.setLetterSpacing(0.1f);
                if (!home.formation.boxed) number.setVisibility(View.GONE);
                cell.addView(number, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
                ImageView icon = new ImageView(home);
                if (!home.formation.icons) icon.setVisibility(View.GONE);
                cell.addView(icon, new LinearLayout.LayoutParams(home.iconPx, home.iconPx));
                TextView caption = home.type(home.formation.labelSp, v.ink, home.formation.icons ? 60 : 40, 800);
                caption.setLetterSpacing(home.formation.icons ? 0.04f : 0.06f);
                // A long name takes a second line rather than losing its end.
                caption.setMaxLines(2);
                caption.setEllipsize(TextUtils.TruncateAt.END);
                caption.setGravity(home.formation.icons ? Gravity.CENTER_HORIZONTAL : Gravity.START);
                if (home.formation.icons) caption.setPadding(0, home.dp(6), 0, 0);
                if (home.formation.serif) caption.setTypeface(Typeface.create("serif", Typeface.BOLD));
                if (!home.formation.captions) caption.setVisibility(View.GONE);
                cell.addView(caption, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
                if (home.formation.boxed) {
                    GradientDrawable box = new GradientDrawable();
                    box.setStroke(Math.max(1, home.dp(1)), v.rule);
                    cell.setBackground(box);
                }
                cell.setForeground(new RippleDrawable(ColorStateList.valueOf(v.rule), null,
                        new ColorDrawable(0xFFFFFFFF)));
                LinearLayout.LayoutParams at = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                at.leftMargin = gap;
                at.rightMargin = gap;
                row.addView(cell, at);
            }
        }
        for (int c = 0; c < home.formation.columns; c++) {
            LinearLayout cell = (LinearLayout) row.getChildAt(c);
            if (c >= fl.apps.size()) {
                // An empty flat at the end of the last floor keeps the columns straight.
                cell.setVisibility(View.INVISIBLE);
                cell.setOnClickListener(null);
                cell.setOnLongClickListener(null);
                continue;
            }
            App a = fl.apps.get(c);
            int n = home.shown.indexOf(a);
            boolean first = home.querying && n == 0;
            cell.setVisibility(View.VISIBLE);
            ((TextView) cell.getChildAt(0)).setText(first ? "▶" : String.format(locale, "%02d", n + 1));
            if (home.formation.icons) ((ImageView) cell.getChildAt(1)).setImageDrawable(home.iconFor(a));
            TextView caption = (TextView) cell.getChildAt(2);
            caption.setText(a.label.toUpperCase(locale));
            caption.setTextColor(first ? v.accent : v.ink);
            if (home.formation.boxed) {
                ((GradientDrawable) cell.getBackground()).setStroke(Math.max(1, home.dp(first ? 2 : 1)), first ? v.accent : v.rule);
            }
            cell.setContentDescription(a.label);
            cell.setAlpha(a.suspended || a.hidden ? 0.4f : 1f);
            cell.setOnClickListener(view -> home.counter.launch(a, view));
            cell.setOnLongClickListener(view -> {
                home.programmeMenu.show(a, view);
                return true;
            });
        }
        return row;
    }

    /** A ministry's name over its programmes, with how many answer to it. */
    View header(Ministry m, LinearLayout row) {
        if (row == null) {
            row = home.row();
            row.setGravity(Gravity.BOTTOM);
            row.setPadding(0, home.dp(14), 0, home.dp(4));
            TextView title = home.type(11, v.accent, 70, 900);
            title.setLetterSpacing(0.2f);
            title.setGravity(home.formation.gravity);
            row.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            TextView members = home.type(11, v.muted, 70, 800);
            row.addView(members);
            if (home.formation.mono) {
                title.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
                title.setLetterSpacing(0.05f);
                members.setTypeface(Typeface.MONOSPACE);
            }
        }
        int members = 0;
        for (App a : home.shown) if (a.ministry == m) members++;
        String title = "■ " + m.title(german) + (m == Ministry.JOY ? home.t(" (RATIONED)", " (RATIONIERT)") : "");
        ((TextView) row.getChildAt(0)).setText(title);
        ((TextView) row.getChildAt(1)).setText(String.format(locale, "%02d", members));
        return row;
    }
}
