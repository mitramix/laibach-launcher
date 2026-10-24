package dev.laibach.launcher;

import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The register of permitted programmes: every launchable app filed under its
 * ministry, filtered by the command line, by redaction and by mobilisation,
 * with the footer that says what was left out. Suspended apps are left out,
 * any other app can be hidden ("redacted"), and hidden apps still come up in
 * search.
 */
final class Register extends HomePart {
    static final String KEY_HIDDEN = "hidden";

    final List<App> apps = new ArrayList<>();
    final List<App> shown = new ArrayList<>();
    /** The list as drawn: Apps (or Floors of them in a grid), and Ministry headers between them while nothing is typed. */
    final List<Object> rows = new ArrayList<>();
    private final Map<String, Drawable> icons = new HashMap<>();
    final int iconPx;
    private final LauncherApps launcherApps;
    private final ProgrammeAdapter adapter;
    private ListView list;
    private TextView count;
    private TextView footer;
    boolean showHidden;
    boolean querying;
    /** Whether the list was last filtered for mobilisation. */
    boolean mobilisedShown;

    private final LauncherApps.Callback appsChanged = new LauncherApps.Callback() {
        @Override public void onPackageRemoved(String pkg, UserHandle user) { changed(pkg); }
        @Override public void onPackageAdded(String pkg, UserHandle user) { changed(pkg); }
        @Override public void onPackageChanged(String pkg, UserHandle user) { changed(pkg); }
        @Override public void onPackagesAvailable(String[] pkgs, UserHandle user, boolean replacing) { reload(); }
        @Override public void onPackagesUnavailable(String[] pkgs, UserHandle user, boolean replacing) { reload(); }
        @Override public void onPackagesSuspended(String[] pkgs, UserHandle user) { reload(); }
        @Override public void onPackagesUnsuspended(String[] pkgs, UserHandle user) { reload(); }
    };

    Register(HomeActivity home) {
        super(home);
        launcherApps = home.launcherApps;
        iconPx = dp(home.formation.iconDp);
        adapter = new ProgrammeAdapter(this);
    }

    /** The monument and the list heading scroll with the programmes, then the footer. */
    void build(LinearLayout root, View monument) {
        // The monument and the list heading scroll with the programmes, so the
        // citizen's papers leave room for the list.
        LinearLayout top = column();
        top.addView(monument);

        top.addView(rule(v.accent), ruleParams(dp(2), dp(14), dp(8)));
        LinearLayout head = row();
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = type(18, v.ink, 25, 1000);
        title.setText(t("PERMITTED PROGRAMMES", "ERLAUBTE PROGRAMME"));
        title.setLetterSpacing(0.28f);
        head.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        count = type(18, v.accent, 25, 1000);
        head.addView(count);
        top.addView(head);

        Formation formation = home.formation;
        list = new ListView(home);
        list.addHeaderView(top, null, false);
        // Rules between the lines of a register or a newspaper; badges and names stand free.
        if (formation.list == Formation.ROWS || (formation.list == Formation.GRID && !formation.icons)) {
            list.setDivider(new ColorDrawable(v.rule));
            list.setDividerHeight(Math.max(1, dp(1)));
        } else {
            list.setDivider(null);
        }
        list.setVerticalScrollBarEnabled(false);
        list.setSelector(new ColorDrawable(v.rule));
        list.setAdapter(adapter);
        list.setOnItemClickListener((parent, view, pos, id) -> {
            if (parent.getItemAtPosition(pos) instanceof App) home.counter.launch((App) parent.getItemAtPosition(pos), view);
        });
        list.setOnItemLongClickListener((parent, view, pos, id) -> {
            if (!(parent.getItemAtPosition(pos) instanceof App)) return false;
            home.programmeMenu.show((App) parent.getItemAtPosition(pos), view);
            return true;
        });
        root.addView(list, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        footer = type(11, v.muted, 80, 600);
        footer.setLetterSpacing(0.14f);
        footer.setPadding(0, dp(8), 0, dp(6));
        footer.setOnClickListener(view -> {
            if (mobilisedShown && !showHidden && !querying) {
                home.orders.demobilise();
                return;
            }
            showHidden = !showHidden;
            applyFilter();
        });
        root.addView(footer);
    }

    /** Listens for apps coming and going while the home screen lives. */
    void watch() {
        launcherApps.registerCallback(appsChanged, home.handler);
    }

    void unwatch() {
        launcherApps.unregisterCallback(appsChanged);
    }

    void changed(String pkg) {
        icons.keySet().removeIf(key -> key.startsWith(pkg + "/"));
        reload();
    }

    void reload() {
        Set<String> hiddenKeys = home.prefs.getStringSet(KEY_HIDDEN, Collections.emptySet());
        apps.clear();
        String own = home.getPackageName();
        for (UserHandle user : launcherApps.getProfiles()) {
            long serial = home.userManager.getSerialNumberForUser(user);
            for (LauncherActivityInfo info : launcherApps.getActivityList(null, user)) {
                // Of the launcher's own entries only the state broadcaster and the camera are
                // programmes; watching and photographing are duties, not joys.
                String cls = info.getComponentName().getClassName();
                boolean tv = cls.equals(BroadcastActivity.class.getName()) || cls.equals(DarkroomActivity.class.getName());
                if (info.getComponentName().getPackageName().equals(own) && !tv) continue;
                String key = info.getComponentName().flattenToShortString() + "#" + serial;
                apps.add(new App(info, key, hiddenKeys.contains(key),
                        tv ? Ministry.ENLIGHTENMENT : Ministry.of(home, info.getApplicationInfo())));
            }
        }
        Collator collator = Collator.getInstance();
        collator.setStrength(Collator.PRIMARY);
        apps.sort((a, b) -> a.ministry != b.ministry ? a.ministry.compareTo(b.ministry)
                : collator.compare(a.label, b.label));
        applyFilter();
    }

    void applyFilter() {
        String query = App.fold(home.command.query());
        querying = !query.isEmpty();
        shown.clear();
        int hiddenCount = 0;
        long mobilised = Citizen.mobilisedUntil(home);
        mobilisedShown = mobilised > 0;
        Set<String> essential = Citizen.essential(home);
        int stoodDown = 0;
        for (App a : apps) {
            if (a.suspended || a.hidden) hiddenCount++;
            boolean visible;
            if (showHidden) {
                visible = true;
            } else if (query.isEmpty()) {
                visible = !a.suspended && !a.hidden;
                // Mobilised: only communication, movement and the essential programmes report for duty.
                if (visible && mobilised > 0 && a.ministry != Ministry.COMMUNICATION && a.ministry != Ministry.MOVEMENT
                        && !essential.contains(a.key)) {
                    visible = false;
                    stoodDown++;
                }
            } else {
                // Hidden apps stay reachable by name; suspended ones can't open anyway.
                visible = !a.suspended;
            }
            if (visible && (query.isEmpty() || a.searchKey.contains(query))) shown.add(a);
        }
        file();
        count.setText(String.format(locale, "%02d", shown.size()));
        if (querying && shown.isEmpty()) {
            footer.setVisibility(View.VISIBLE);
            footer.setText(t("NO SUCH PROGRAMME HAS EVER EXISTED.", "EIN SOLCHES PROGRAMM HAT ES NIE GEGEBEN."));
            return;
        }
        if (mobilised > 0 && !showHidden && !querying) {
            footer.setVisibility(View.VISIBLE);
            footer.setText(String.format(locale, t("MOBILISED UNTIL %s · %d STOOD DOWN — STAND DOWN",
                    "MOBILISIERT BIS %s · %d ZURÜCKGESTELLT — DEMOBILISIEREN"),
                    DateFormat.format(DateFormat.is24HourFormat(home) ? "HH:mm" : "h:mm a", mobilised), stoodDown));
            return;
        }
        footer.setVisibility(hiddenCount == 0 && !showHidden ? View.GONE : View.VISIBLE);
        footer.setText(showHidden
                ? t("ALL PROGRAMMES DECLASSIFIED — HIDE REDACTED", "ALLE PROGRAMME FREIGEGEBEN — GESCHWÄRZTE VERBERGEN")
                : hiddenCount + t(" REDACTED — DECLASSIFY", " GESCHWÄRZT — FREIGEBEN"));
    }

    /**
     * Ministries head their programmes; a search shows the matches alone, best first.
     * In a grid the programmes are housed by the floor, and each ministry starts a new one.
     */
    private void file() {
        Formation formation = home.formation;
        rows.clear();
        Ministry last = null;
        boolean headed = !querying && formation.has(Formation.MINISTRIES);
        ProgrammeAdapter.Floor floor = null;
        for (App a : shown) {
            if (headed && a.ministry != last) {
                rows.add(a.ministry);
                last = a.ministry;
                floor = null;
            }
            if (formation.list != Formation.GRID) {
                rows.add(a);
                continue;
            }
            if (floor == null || floor.apps.size() == formation.columns) {
                floor = new ProgrammeAdapter.Floor();
                rows.add(floor);
            }
            floor.apps.add(a);
        }
        adapter.notifyDataSetChanged();
    }

    /** Back to the top, with the redacted hidden again. */
    void reset() {
        if (showHidden) {
            showHidden = false;
            applyFilter();
        }
        list.setSelection(0);
    }

    void setHidden(App a, boolean hide) {
        Set<String> keys = new HashSet<>(home.prefs.getStringSet(KEY_HIDDEN, Collections.emptySet()));
        if (hide) keys.add(a.key); else keys.remove(a.key);
        String name = a.label.toUpperCase(Locale.ROOT);
        Record.note(home, hide ? name + " REDACTED. IT NEVER EXISTED." : name + " DECLASSIFIED. IT ALWAYS EXISTED.",
                hide ? name + " GESCHWÄRZT. ES HAT NIE EXISTIERT." : name + " FREIGEGEBEN. ES HAT IMMER EXISTIERT.");
        home.prefs.edit().putStringSet(KEY_HIDDEN, keys).apply();
        a.hidden = hide;
        applyFilter();
    }

    Drawable iconFor(App a) {
        Drawable icon = icons.get(a.key);
        if (icon == null) {
            // Every programme wears the state's badge.
            icon = new BitmapDrawable(home.getResources(), Cyber.icon(a.info.getIcon(0).mutate(), iconPx, v));
            icons.put(a.key, icon);
        }
        return icon;
    }
}
