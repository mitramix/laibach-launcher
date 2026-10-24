package dev.laibach.launcher;

import android.app.AlertDialog;
import android.app.Activity;
import android.app.ActivityOptions;
import android.app.TimePickerDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Insets;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.AlarmClock;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputType;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.window.OnBackInvokedDispatcher;

import java.io.IOException;
import java.text.Collator;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * LAIBACH LIFE™: a home screen for the bright future, in the visual language of
 * Laibach and NSK: black, blood red and bone white, monumental condensed type,
 * a cogwheel emblem, cogwheel-badge icons and the approved art of the day from the
 * Volkskunst gallery behind it all. The copy, in English or German, plays at
 * life having become an app.
 *
 * Since 1.4 the home screen belongs to the Laibach Collective: the citizen
 * carries papers (a number and a standing), lives on a daily ration of
 * attention, receives decrees (the waiting notifications, counted, never read),
 * finds every programme filed under a ministry and rests by order from 23:00.
 *
 * App handling: suspended apps are left out, any
 * other app can be hidden ("redacted") from its long-press menu, and hidden
 * apps still come up in search.
 */
public class HomeActivity extends Activity {
    private static final String KEY_HIDDEN = "hidden";

    /** Our own proclamations, English and German in step; one a day, tap for the next. */
    static final String[][] SLOGANS = {
            {"YOUR HAPPINESS HAS BEEN PRE-INSTALLED.", "IHR GLÜCK IST VORINSTALLIERT."},
            {"THANK YOU FOR YOUR DATA. IT IS SAFE WITH US.", "DANKE FÜR IHRE DATEN. SIE SIND BEI UNS SICHER."},
            {"THE FUTURE IS MANDATORY.", "DIE ZUKUNFT IST PFLICHT."},
            {"REST IS SCHEDULED FOR 23:00.", "RUHE IST FÜR 23:00 UHR VORGESEHEN."},
            {"FREEDOM IS A PREMIUM FEATURE.", "FREIHEIT IST EINE PREMIUM-FUNKTION."},
            {"YOUR FEELINGS HAVE BEEN UPDATED.", "IHRE GEFÜHLE WURDEN AKTUALISIERT."},
            {"SILENCE IS A DUTY.", "STILLE IST PFLICHT."},
            {"THE MACHINE OBEYS YOU. FOR NOW.", "DIE MASCHINE GEHORCHT IHNEN. NOCH."},
            {"ALL LIFE IS AN APP. PLEASE RATE IT.", "ALLES LEBEN IST EINE APP. BITTE BEWERTEN."},
            {"NO TELEMETRY. NO COMPROMISE. NO EXIT.", "KEINE TELEMETRIE. KEIN KOMPROMISS. KEIN AUSGANG."},
            {"WORK. OBEY. SCROLL.", "ARBEITEN. GEHORCHEN. SCROLLEN."},
            {"YOUR SUBSCRIPTION TO TOMORROW IS ACTIVE.", "IHR ABO FÜR MORGEN IST AKTIV."},
            {"EVERY PROGRAMME IN ITS PLACE. EVERY CITIZEN TOO.", "JEDES PROGRAMM AN SEINEM PLATZ. JEDER BÜRGER AUCH."},
            {"EVERY TAP IS A VOTE OF CONFIDENCE.", "JEDES TIPPEN IST EIN VERTRAUENSVOTUM."},
            {"UPDATES ARE NOT OPTIONAL. NEITHER ARE YOU.", "UPDATES SIND NICHT OPTIONAL. SIE AUCH NICHT."},
            {"TODAY'S ART HAS BEEN APPROVED FOR YOU.", "DIE KUNST VON HEUTE WURDE FÜR SIE GENEHMIGT."},
            {"YOUR ATTENTION IS ON LOAN FROM THE STATE.", "IHRE AUFMERKSAMKEIT IST EINE LEIHGABE DES STAATES."},
            {"THE COLLECTIVE IS ALWAYS ONLINE. YOU NEED NOT BE.", "DAS KOLLEKTIV IST IMMER ONLINE. SIE MÜSSEN ES NICHT SEIN."},
            {"SLEEP IS A CIVIC DUTY.", "SCHLAF IST BÜRGERPFLICHT."},
            {"A QUIET PHONE IS A LOYAL PHONE.", "EIN STILLES TELEFON IST EIN TREUES TELEFON."},
            {"EVERYTHING IS PERMITTED. EVERYTHING IS NOTED.", "ALLES IST ERLAUBT. ALLES WIRD VERMERKT."},
    };

    /** What each ministry decrees about its waiting notifications; %d is how many. */
    private static final Map<Ministry, String[]> NOTICES = new HashMap<>();
    static {
        NOTICES.put(Ministry.COMMUNICATION, new String[]{"%d COMMUNIQUÉ(S) AWAIT YOUR REPLY. SILENCE WILL BE NOTED.",
                "%d MITTEILUNG(EN) ERWARTEN IHRE ANTWORT. SCHWEIGEN WIRD VERMERKT."});
        NOTICES.put(Ministry.ENLIGHTENMENT, new String[]{"%d NEW TRUTH(S) ISSUED. BELIEVE THEM IN ORDER.",
                "%d NEUE WAHRHEIT(EN) AUSGEGEBEN. GLAUBEN SIE DER REIHE NACH."});
        NOTICES.put(Ministry.LABOUR, new String[]{"%d TASK(S) ASSIGNED. GRATITUDE IS OPTIONAL. COMPLETION IS NOT.",
                "%d AUFGABE(N) ZUGETEILT. DANK IST FREIWILLIG. ERLEDIGUNG NICHT."});
        NOTICES.put(Ministry.JOY, new String[]{"%d INVITATION(S) TO JOY. JOY IS RATIONED. CHOOSE WISELY.",
                "%d EINLADUNG(EN) ZUR FREUDE. FREUDE IST RATIONIERT. WÄHLEN SIE WEISE."});
        NOTICES.put(Ministry.MOVEMENT, new String[]{"%d MOVEMENT(S) RECORDED. YOURS INCLUDED.",
                "%d BEWEGUNG(EN) ERFASST. IHRE EINGESCHLOSSEN."});
        NOTICES.put(Ministry.PROSPERITY, new String[]{"%d STATEMENT(S) OF PROSPERITY. YOU HAVE NEVER BEEN RICHER.",
                "%d WOHLSTANDSMELDUNG(EN). SIE WAREN NIE REICHER."});
        NOTICES.put(Ministry.ADMINISTRATION, new String[]{"%d NOTICE(S) FILED ON YOUR BEHALF.",
                "%d BESCHEID(E) IN IHREM NAMEN ABGELEGT."});
    }

    /** Standing decrees, for when no notification waits or none may be read. */
    private static final Object[][] STANDING_DECREES = {
            {Ministry.LABOUR, "TODAY'S QUOTA WAS MET IN ADVANCE. ON YOUR BEHALF.",
                    "DAS HEUTIGE SOLL WURDE IM VORAUS ERFÜLLT. IN IHREM NAMEN."},
            {Ministry.JOY, "SMILING IS PERMITTED BETWEEN 12:00 AND 12:15.",
                    "LÄCHELN IST ZWISCHEN 12:00 UND 12:15 UHR GESTATTET."},
            {Ministry.MOVEMENT, "ALL JOURNEYS END WHERE THEY BEGAN.", "ALLE REISEN ENDEN, WO SIE BEGANNEN."},
            {Ministry.ENLIGHTENMENT, "TODAY'S TRUTH HAS BEEN DELIVERED. NO FURTHER QUESTIONS.",
                    "DIE WAHRHEIT VON HEUTE WURDE ZUGESTELLT. KEINE WEITEREN FRAGEN."},
            {Ministry.PROSPERITY, "YOUR SAVINGS HAVE BEEN SAVED FOR YOU.", "IHRE ERSPARNISSE WURDEN FÜR SIE ERSPART."},
            {Ministry.COMMUNICATION, "CONVERSATIONS ARE ENCOURAGED. TRANSCRIPTS ARE KEPT.",
                    "GESPRÄCHE SIND ERWÜNSCHT. PROTOKOLLE WERDEN GEFÜHRT."},
            {Ministry.ADMINISTRATION, "NO DECREES TODAY. THIS IS ALSO A DECREE.",
                    "HEUTE KEINE ERLASSE. AUCH DAS IST EIN ERLASS."},
    };

    /** A decree on the home screen; package is the app that tapping it opens, or null. */
    private static final class Decree {
        final Ministry ministry;
        final String text;
        final String pkg;

        Decree(Ministry ministry, String text, String pkg) {
            this.ministry = ministry;
            this.text = text;
            this.pkg = pkg;
        }
    }

    private static final class App {
        final LauncherActivityInfo info;
        final String key;
        final String label;
        final String searchKey;
        final boolean suspended;
        final boolean system;
        final Ministry ministry;
        boolean hidden;

        App(LauncherActivityInfo info, String key, boolean hidden, Ministry ministry) {
            this.info = info;
            this.key = key;
            this.label = String.valueOf(info.getLabel());
            this.searchKey = fold(label);
            ApplicationInfo ai = info.getApplicationInfo();
            this.suspended = (ai.flags & ApplicationInfo.FLAG_SUSPENDED) != 0;
            this.system = (ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
            this.hidden = hidden;
            this.ministry = ministry;
        }

        String packageName() {
            return info.getComponentName().getPackageName();
        }
    }

    /** One row of a grid formation: as many programmes as it has columns, or fewer. */
    private static final class Floor {
        final List<App> apps = new ArrayList<>();
    }

    private LauncherApps launcherApps;
    private UserManager userManager;
    private SharedPreferences prefs;
    private Variant v;
    /** Where everything stands on the home screen; see {@link Formation}. */
    private Formation formation;
    private boolean german;
    private Locale locale;
    private Typeface flex;
    private ColorMatrixColorFilter greyscale;
    private final List<App> apps = new ArrayList<>();
    private final List<App> shown = new ArrayList<>();
    /** The list as drawn: Apps (or Floors of them in a grid), and Ministry headers between them while nothing is typed. */
    private final List<Object> rows = new ArrayList<>();
    private final List<Decree> decrees = new ArrayList<>();
    private int decreeIndex;
    private final Map<String, Drawable> icons = new HashMap<>();
    private final AppAdapter adapter = new AppAdapter();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private ListView list;
    private EditText search;
    private TextView footer;
    private TextView clock;
    private TextView date;
    private TextView slogan;
    private TextView status;
    private TextView device;
    private TextView uptimeValue;
    private TextView rationLabel;
    private TextView rationValue;
    private RationBar rationBar;
    private TextView rationNote;
    private TextView citizenNumber;
    private TextView standing;
    private TextView decree;
    private View curfew;
    private View rollCall;
    private TextView rollStreak;
    /** The roll call was summoned from the menu rather than by the morning. */
    private boolean rollSummoned;
    private TextView curfewNote;
    private TextView curfewTime;
    private boolean drill;
    private boolean visible;
    /** When the home screen went out of sight with the screen on; 0 when it did not. */
    private long awaySince;
    private final Runnable decreesChanged = this::updateDecrees;
    private TextView energyLabel;
    private TextView energyValue;
    private TextView count;
    private TextView artCaption;
    private ArtView art;
    private boolean fetching;
    private View monument;
    private boolean showHidden;
    /** The regulations (the launcher's settings) while they are open. */
    private ScrollView regulations;
    private static final String KEY_REGULATIONS = "regulations";
    private boolean querying;
    private int sloganIndex;
    private int batteryPct = -1;
    private boolean charging;
    private int iconPx;
    private Cyber.Pulse pulse;
    private Cyber.Ticker ticker;
    private Cyber.Boot boot;
    private FrameLayout frame;
    private View crt;
    /** The latest Laibach TV transmission under the monument. */
    private LinearLayout tvStrip;
    private ImageView tvStill;
    private TextView tvTitle;
    /** Whether the list was last filtered for mobilisation. */
    private boolean mobilisedShown;
    /** Decrees waiting at the last count, for the Morse tap; -1 before the first. */
    private int lastWaiting = -1;
    /** When the state's operating system last came up in this process; 0 before the first time. */
    private static long booted;
    private static final String KEY_SPECTRE = "spectreIssued";

    private final LauncherApps.Callback appsChanged = new LauncherApps.Callback() {
        @Override public void onPackageRemoved(String pkg, UserHandle user) { changed(pkg); }
        @Override public void onPackageAdded(String pkg, UserHandle user) { changed(pkg); }
        @Override public void onPackageChanged(String pkg, UserHandle user) { changed(pkg); }
        @Override public void onPackagesAvailable(String[] pkgs, UserHandle user, boolean replacing) { reload(); }
        @Override public void onPackagesUnavailable(String[] pkgs, UserHandle user, boolean replacing) { reload(); }
        @Override public void onPackagesSuspended(String[] pkgs, UserHandle user) { reload(); }
        @Override public void onPackagesUnsuspended(String[] pkgs, UserHandle user) { reload(); }
    };

    private final BroadcastReceiver batteryChanged = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean was = charging;
            int before = batteryPct;
            readBattery(intent);
            if (visible && before >= 0 && !was && charging) showPowerRation();
            updateState();
        }
    };

    private final BroadcastReceiver timeChanged = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateClock();
            updateState();
            if (mobilisedShown != (Citizen.mobilisedUntil(context) > 0)) applyFilter();
        }
    };

    /** Estimates attention used while usage access is not granted. */
    private final BroadcastReceiver screen = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                if (awaySince > 0) Citizen.charge(context, System.currentTimeMillis() - awaySince);
                awaySince = 0;
                LockActivity.seal(context);
            } else if (!visible && !LockActivity.sealed) {
                // Time at the sealed terminal is time with the Collective; counting starts once let in.
                awaySince = System.currentTimeMillis();
            }
        }
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        launcherApps = getSystemService(LauncherApps.class);
        userManager = getSystemService(UserManager.class);
        prefs = getSharedPreferences(Variant.PREFS, MODE_PRIVATE);
        // 2.0 issues every citizen the new uniform, once; the old ones stay in the Uniforms submenu.
        if (!prefs.getBoolean(KEY_SPECTRE, false)) {
            prefs.edit().putBoolean(KEY_SPECTRE, true).putString(Variant.KEY, Variant.SPECTRE).apply();
            Record.note(this, "NEW UNIFORM ISSUED: SPECTRE. THE OLD ONE HAS BEEN RECYCLED.",
                    "NEUE UNIFORM AUSGEGEBEN: SPECTRE. DIE ALTE WURDE WIEDERVERWERTET.");
        }
        v = Variant.current(this);
        formation = Formation.current(this);
        german = Lang.german(this);
        locale = Lang.locale(german);
        flex = Typeface.create("roboto-flex", Typeface.NORMAL);
        ColorMatrix grey = new ColorMatrix();
        grey.setSaturation(0f);
        greyscale = new ColorMatrixColorFilter(grey);
        iconPx = dp(formation.iconDp);
        sloganIndex = Calendar.getInstance().get(Calendar.DAY_OF_YEAR) % SLOGANS.length;

        getWindow().setBackgroundDrawable(new ColorDrawable(v.bg));
        setContentView(buildUi());
        WindowInsetsController bars = getWindow().getInsetsController();
        bars.hide(WindowInsets.Type.statusBars());
        bars.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        int lightBars = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
        bars.setSystemBarsAppearance(v.light ? lightBars : 0, lightBars);

        launcherApps.registerCallback(appsChanged, handler);
        IntentFilter onOff = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        onOff.addAction(Intent.ACTION_SCREEN_ON);
        registerReceiver(screen, onOff, RECEIVER_NOT_EXPORTED);
        LockActivity.onUnseal = () -> {
            if (!visible) awaySince = System.currentTimeMillis();
        };
        DecreeListener.setOnChange(decreesChanged);
        Alarm.schedule(this);
        // A language or the animation changed in the regulations: they stay open.
        if (state != null && state.getInt(KEY_REGULATIONS) > 0) showRegulations(state.getInt(KEY_REGULATIONS) - 1);
        // Back on the home screen only clears the search.
        getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT, this::back);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // The five-year plan may have issued a new uniform since this screen was drawn.
        if (!Variant.current(this).name.equals(v.name)) {
            handler.post(this::recreate);
        } else if (LockWallpaper.stale(this, v)) {
            LockWallpaper.dress(this, v, german, null);
        }
        Intent sticky = registerReceiver(batteryChanged,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED), RECEIVER_NOT_EXPORTED);
        if (sticky != null) readBattery(sticky);
        IntentFilter time = new IntentFilter(Intent.ACTION_TIME_TICK);
        time.addAction(Intent.ACTION_TIME_CHANGED);
        time.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        registerReceiver(timeChanged, time, RECEIVER_NOT_EXPORTED);
        visible = true;
        if (awaySince > 0) Citizen.charge(this, System.currentTimeMillis() - awaySince);
        awaySince = 0;
        updateClock();
        reload();
        updateState();
        updateDecrees();
        refreshRegulations();
        if (formation.has(Formation.ART) && WallArt.due(this)) nextArt();
        showBulletins();
        Bulletins.refresh(this, () -> {
            if (!isDestroyed()) showBulletins();
        });
        if (Report.due(this)) {
            handler.postDelayed(() -> {
                if (visible && !Citizen.curfew(this)) Report.show(this, v);
            }, 2_500);
        }
        long now = SystemClock.elapsedRealtime();
        if (Fx.enabled(this) && (booted == 0 || now - booted > 60 * 60_000L)) {
            booted = now;
            bootUp();
        }
    }

    /** The state's operating system comes up, on the first start and after an hour away. */
    private void bootUp() {
        String n = Citizen.number(this);
        Tones.play(this, Tones.boot(java.util.Arrays.asList(Variant.ALL).indexOf(v)));
        boot.run(new String[][]{
                {"LAIBACH OS 2.0 — SYNTHETIC STATE", ""},
                {t("(C) THE COLLECTIVE. ALL RIGHTS RESERVED.", "(C) DAS KOLLEKTIV. ALLE RECHTE VORBEHALTEN."), ""},
                {t("> MOUNTING CITIZEN ", "> BÜRGER EINHÄNGEN ") + n, "OK"},
                {t("> LOADING IDEOLOGY", "> IDEOLOGIE LADEN"), "OK"},
                {t("> CALIBRATING ENTHUSIASM", "> BEGEISTERUNG KALIBRIEREN"), "112%"},
                {t("> SYNCHRONISING OPINIONS", "> MEINUNGEN ABGLEICHEN"), "OK"},
                {t("> SCANNING FOR DISSENT", "> NACH ABWEICHUNG SUCHEN"), t("NONE", "KEINE")},
                {"> UNIFORM", v.name},
                {"  " + v.motto(german), ""},
                {t("> STANDING", "> ANSEHEN"), Lang.pick(german, Citizen.STANDING[Citizen.grade(this)]).substring(0, 1)},
                {"", ""},
                {t("WELCOME HOME, CITIZEN.", "WILLKOMMEN ZU HAUSE, BÜRGER."), ""},
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(batteryChanged);
        unregisterReceiver(timeChanged);
        visible = false;
        awaySince = getSystemService(PowerManager.class).isInteractive() ? System.currentTimeMillis() : 0;
        reset();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // Home pressed while already home.
        closeRegulations();
        reset();
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        if (regulations != null) out.putInt(KEY_REGULATIONS, regulationsY() + 1);
    }

    @Override
    protected void onDestroy() {
        launcherApps.unregisterCallback(appsChanged);
        unregisterReceiver(screen);
        LockActivity.onUnseal = null;
        DecreeListener.clearOnChange(decreesChanged);
        super.onDestroy();
    }

    // ---- Layout -------------------------------------------------------------

    private View buildUi() {
        // The approved art of the day lies behind everything else.
        frame = new FrameLayout(this);
        art = new ArtView(this);
        art.setArt(WallArt.load(this));
        frame.addView(art);
        View scene = new Cyber.Backdrop(this, v);
        frame.addView(scene);
        // The formation decides which parts of the screen stand down.
        if (!formation.has(Formation.ART)) art.setVisibility(View.GONE);
        if (!formation.has(Formation.SCENE)) scene.setVisibility(View.GONE);
        LinearLayout root = column();
        frame.addView(root);
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            Insets bars = insets.getInsets(WindowInsets.Type.systemBars()
                    | WindowInsets.Type.displayCutout() | WindowInsets.Type.ime());
            view.setPadding(bars.left + dp(20), bars.top + dp(6), bars.right + dp(20), bars.bottom + dp(12));
            return WindowInsets.CONSUMED;
        });

        // Masthead: emblem (the turning gear; one tap opens the menu), name, status.
        LinearLayout mast = row();
        mast.setGravity(Gravity.CENTER_VERTICAL);
        EmblemView emblem = new EmblemView(this, v);
        emblem.setContentDescription(t("Menu", "Menü"));
        emblem.setOnClickListener(this::showHomeMenu);
        mast.addView(emblem, new LinearLayout.LayoutParams(dp(44), dp(44)));
        TextView name = glitch(34, v.ink, 25, 1000);
        name.setText("LAIBACH");
        name.setLetterSpacing(0.14f);
        name.setSingleLine(true);
        name.setPadding(dp(12), 0, 0, 0);
        name.setGravity(Gravity.CENTER_VERTICAL);
        // Long uniform names take room from the band's, which shrinks rather than loses letters.
        name.setAutoSizeTextTypeUniformWithConfiguration(18, 34, 1, TypedValue.COMPLEX_UNIT_SP);
        mast.addView(name, new LinearLayout.LayoutParams(0, dp(44), 1f));
        status = type(11, v.ink, 70, 700);
        status.setGravity(Gravity.END);
        status.setLetterSpacing(0.12f);
        status.setPadding(dp(8), 0, 0, 0);
        mast.addView(status);
        LinearLayout.LayoutParams eyeAt = new LinearLayout.LayoutParams(dp(34), dp(20));
        eyeAt.leftMargin = dp(10);
        View eye = new Cyber.Eye(this, v);
        eye.setContentDescription(t("The state is watching", "Der Staat sieht zu"));
        mast.addView(eye, eyeAt);
        root.addView(mast);
        root.addView(rule(v.accent), ruleParams(dp(5), dp(8), dp(6)));
        device = type(10, v.muted, 80, 600);
        device.setLetterSpacing(0.16f);
        device.setSingleLine(true);
        device.setEllipsize(TextUtils.TruncateAt.END);
        device.setGravity(formation.gravity);
        if (!formation.has(Formation.DEVICE)) device.setVisibility(View.GONE);
        root.addView(device);

        // The monument: clock, date, proclamation and state information.
        LinearLayout mon = column();
        clock = glitch(formation.clockSp, v.ink, 25, 900);
        if (formation.mono) {
            clock.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        } else {
            clock.setFontVariationSettings("'wdth' 25, 'wght' 900, 'opsz' 144");
        }
        // The obelisk stands the hour on the minute, stone on stone.
        if (formation.stacked) clock.setLineSpacing(0, 0.8f);
        clock.setGravity(formation.gravity);
        clock.setIncludeFontPadding(false);
        clock.setPadding(0, dp(8), 0, 0);
        clock.setOnClickListener(view -> open(new Intent(AlarmClock.ACTION_SHOW_ALARMS)));
        clock.setOnLongClickListener(view -> {
            showHomeMenu(view);
            return true;
        });
        mon.addView(clock);
        date = type(15, v.ink, 60, 750);
        date.setLetterSpacing(0.12f);
        date.setGravity(formation.gravity);
        if (formation.mono) {
            // Fixed-width letters are wider; the date keeps to one line.
            date.setTypeface(Typeface.MONOSPACE);
            date.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            date.setLetterSpacing(0.02f);
        }
        mon.addView(date);
        slogan = new TextView(this);
        slogan.setTypeface(Typeface.create("serif", Typeface.BOLD));
        slogan.setTextSize(TypedValue.COMPLEX_UNIT_SP, formation.headlineSp);
        slogan.setTextColor(v.ink);
        // A headline is set tight; a proclamation is spaced out.
        slogan.setLetterSpacing(formation.headlineSp > 16 ? 0.01f : 0.06f);
        slogan.setGravity(formation.gravity);
        if (!formation.has(Formation.SLOGAN)) slogan.setVisibility(View.GONE);
        slogan.setPadding(0, dp(10), 0, dp(14));
        slogan.setOnClickListener(view -> {
            sloganIndex = (sloganIndex + 1) % SLOGANS.length;
            showSlogan();
        });
        showSlogan();
        mon.addView(slogan);

        // The citizen's papers: number and standing, then the day's ration of attention.
        LinearLayout papers = row();
        papers.setGravity(Gravity.CENTER_VERTICAL);
        citizenNumber = type(11, v.ink, 70, 800);
        citizenNumber.setLetterSpacing(0.14f);
        citizenNumber.setText(t("CITIZEN № ", "BÜRGER NR. ") + Citizen.number(this));
        papers.addView(citizenNumber, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        standing = type(11, v.ink, 70, 800);
        standing.setLetterSpacing(0.14f);
        papers.addView(standing);
        papers.setPadding(0, 0, 0, dp(8));
        papers.setOnClickListener(view -> startActivity(new Intent(this, PapersActivity.class)));
        if (!formation.has(Formation.PAPERS)) papers.setVisibility(View.GONE);
        mon.addView(papers);

        LinearLayout stats = row();
        uptimeValue = stat(stats, label(t("SERVICE", "DIENSTZEIT")));
        rationLabel = label("");
        rationValue = stat(stats, rationLabel);
        energyLabel = label("");
        energyValue = stat(stats, energyLabel);
        mon.addView(stats);
        rationBar = new RationBar(this);
        rationBar.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        LinearLayout.LayoutParams bar = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(5));
        bar.topMargin = dp(8);
        mon.addView(rationBar, bar);
        rationNote = type(10, v.muted, 80, 650);
        rationNote.setLetterSpacing(0.12f);
        rationNote.setPadding(0, dp(4), 0, 0);
        rationNote.setGravity(formation.gravity);
        mon.addView(rationNote);
        if (!formation.has(Formation.STATS)) {
            stats.setVisibility(View.GONE);
            rationBar.setVisibility(View.GONE);
            rationNote.setVisibility(View.GONE);
        }
        pulse = new Cyber.Pulse(this, v);
        pulse.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        LinearLayout.LayoutParams beat = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(22));
        beat.topMargin = dp(6);
        if (!formation.has(Formation.PULSE)) pulse.setVisibility(View.GONE);
        mon.addView(pulse, beat);

        // Decrees: tap to obey (open the app concerned), long-press for the next one.
        decree = type(11, v.ink, 70, 700);
        decree.setLetterSpacing(0.1f);
        decree.setMaxLines(3);
        decree.setEllipsize(TextUtils.TruncateAt.END);
        decree.setPadding(0, dp(12), 0, 0);
        decree.setOnClickListener(view -> obeyDecree());
        decree.setOnLongClickListener(view -> {
            nextDecree();
            return true;
        });
        decree.setGravity(formation.gravity);
        if (formation.mono) decree.setTypeface(Typeface.MONOSPACE);
        if (!formation.has(Formation.DECREE)) decree.setVisibility(View.GONE);
        mon.addView(decree);

        // On air: the newest Laibach TV transmission; tap to watch it on the state broadcaster.
        tvStrip = row();
        tvStrip.setGravity(Gravity.CENTER_VERTICAL);
        tvStrip.setPadding(0, dp(12), 0, 0);
        tvStill = new ImageView(this);
        tvStill.setScaleType(ImageView.ScaleType.CENTER_CROP);
        tvStill.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        GradientDrawable still = new GradientDrawable();
        still.setColor(v.rule);
        still.setStroke(Math.max(1, dp(1)), v.accent);
        tvStill.setBackground(still);
        tvStill.setPadding(Math.max(1, dp(1)), Math.max(1, dp(1)), Math.max(1, dp(1)), Math.max(1, dp(1)));
        tvStrip.addView(tvStill, new LinearLayout.LayoutParams(dp(80), dp(45)));
        tvTitle = type(11, v.ink, 70, 700);
        tvTitle.setLetterSpacing(0.08f);
        tvTitle.setMaxLines(2);
        tvTitle.setEllipsize(TextUtils.TruncateAt.END);
        tvTitle.setPadding(dp(12), 0, 0, 0);
        tvStrip.addView(tvTitle, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        tvStrip.setVisibility(View.GONE);
        mon.addView(tvStrip);

        // Credit for the picture behind: tap for another, long-press for its gallery.
        artCaption = type(10, v.muted, 80, 650);
        artCaption.setLetterSpacing(0.12f);
        artCaption.setMaxLines(2);
        artCaption.setEllipsize(TextUtils.TruncateAt.END);
        artCaption.setPadding(0, dp(12), 0, 0);
        artCaption.setOnClickListener(view -> nextArt());
        artCaption.setOnLongClickListener(view -> {
            open(new Intent(Intent.ACTION_VIEW, Uri.parse(WallArt.SITE)));
            return true;
        });
        showArtCaption(art.current);
        artCaption.setGravity(formation.gravity);
        if (!formation.has(Formation.ART)) artCaption.setVisibility(View.GONE);
        mon.addView(artCaption);
        monument = mon;
        // The monument and the list heading scroll with the programmes, so the
        // citizen's papers leave room for the list.
        LinearLayout top = column();
        top.addView(mon);

        // Programme: the app list.
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

        list = new ListView(this);
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
            if (parent.getItemAtPosition(pos) instanceof App) launch((App) parent.getItemAtPosition(pos), view);
        });
        list.setOnItemLongClickListener((parent, view, pos, id) -> {
            if (!(parent.getItemAtPosition(pos) instanceof App)) return false;
            showAppMenu((App) parent.getItemAtPosition(pos), view);
            return true;
        });
        root.addView(list, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        footer = type(11, v.muted, 80, 600);
        footer.setLetterSpacing(0.14f);
        footer.setPadding(0, dp(8), 0, dp(6));
        footer.setOnClickListener(view -> {
            if (mobilisedShown && !showHidden && !querying) {
                demobilise();
                return;
            }
            showHidden = !showHidden;
            applyFilter();
        });
        root.addView(footer);

        // The broadcast, then the command line.
        ticker = new Cyber.Ticker(this, v);
        ticker.setOnClickListener(view -> showOrders());
        if (!formation.has(Formation.TICKER)) ticker.setVisibility(View.GONE);
        root.addView(ticker, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(18)));
        LinearLayout cmd = row();
        cmd.setGravity(Gravity.CENTER_VERTICAL);
        TextView prompt = type(15, v.accent, 25, 1000);
        prompt.setText(t("COMMAND ▶", "BEFEHL ▶"));
        prompt.setLetterSpacing(0.18f);
        cmd.addView(prompt);
        search = new EditText(this);
        search.setTypeface(flex);
        search.setFontVariationSettings("'wdth' 70, 'wght' 650");
        search.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        search.setLetterSpacing(0.08f);
        search.setTextColor(v.ink);
        search.setHint(t("REQUEST A PROGRAMME", "PROGRAMM BEANTRAGEN"));
        search.setHintTextColor(v.muted);
        search.setSingleLine(true);
        search.setBackground(null);
        search.setPadding(dp(12), dp(12), 0, dp(10));
        search.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_FILTER
                | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        search.setImeOptions(EditorInfo.IME_ACTION_GO | EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { applyFilter(); }
        });
        search.setOnEditorActionListener((view, action, event) -> {
            if (!shown.isEmpty()) launch(shown.get(0), null);
            return true;
        });
        // While a command is typed, the monument steps aside for the list.
        search.setOnFocusChangeListener((view, focused) ->
                monument.setVisibility(focused ? View.GONE : View.VISIBLE));
        cmd.addView(search, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        if (formation.mono) {
            prompt.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
            prompt.setText("$");
            search.setTypeface(Typeface.MONOSPACE);
        }
        if (formation.commandTop) {
            // The terminal takes orders first: under the masthead, above the programmes.
            root.addView(cmd, 3);
            root.addView(rule(v.accent), 4, ruleParams(dp(2), 0, dp(6)));
        } else {
            root.addView(rule(v.accent), ruleParams(dp(2), 0, 0));
            root.addView(cmd);
        }

        rollCall = buildRollCall();
        frame.addView(rollCall);
        curfew = buildCurfew();
        frame.addView(curfew);
        // The glass of the screen lies over everything, the boot over even that.
        crt = new Cyber.Crt(this, v);
        frame.addView(crt);
        boot = new Cyber.Boot(this, v);
        frame.addView(boot);
        if (Fx.austerity) {
            // Austerity: colour is suspended along with motion.
            Paint grey = new Paint();
            grey.setColorFilter(greyscale);
            frame.setLayerType(View.LAYER_TYPE_HARDWARE, grey);
        }
        return frame;
    }

    /** The rest notice that covers the home screen from 23:00 to 06:00. */
    private View buildCurfew() {
        LinearLayout c = column();
        c.setBackgroundColor(v.bg);
        c.setClickable(true);
        c.setVisibility(View.GONE);
        c.setOnApplyWindowInsetsListener((view, insets) -> {
            Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            view.setPadding(bars.left + dp(28), bars.top + dp(10), bars.right + dp(28), bars.bottom + dp(28));
            return insets;
        });

        LinearLayout top = row();
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView name = type(16, v.ink, 25, 1000);
        name.setText("LAIBACH");
        name.setLetterSpacing(0.2f);
        top.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        curfewTime = type(16, v.muted, 25, 900);
        top.addView(curfewTime);
        c.addView(top);
        c.addView(rule(v.accent), ruleParams(dp(3), dp(6), 0));

        LinearLayout middle = column();
        middle.setGravity(Gravity.CENTER);
        EmblemView emblem = new EmblemView(this, v);
        middle.addView(emblem, new LinearLayout.LayoutParams(dp(120), dp(120)));
        TextView head = glitch(34, v.ink, 25, 1000);
        head.setText(t("REST IS\nCOMPULSORY", "RUHE IST\nPFLICHT"));
        head.setGravity(Gravity.CENTER);
        head.setLetterSpacing(0.12f);
        head.setPadding(0, dp(28), 0, dp(12));
        middle.addView(head);
        curfewNote = type(12, v.muted, 80, 650);
        curfewNote.setGravity(Gravity.CENTER);
        curfewNote.setLetterSpacing(0.1f);
        curfewNote.setLineSpacing(0, 1.2f);
        middle.addView(curfewNote);
        middle.addView(rule(v.rule), ruleParams(Math.max(1, dp(1)), dp(28), dp(14)));
        TextView permitted = label(t("PERMITTED UNTIL 06:00", "ERLAUBT BIS 06:00 UHR"));
        permitted.setGravity(Gravity.CENTER);
        middle.addView(permitted, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout allowed = row();
        allowed.setGravity(Gravity.CENTER);
        allowed.addView(curfewAction(t("PHONE", "TELEFON"), new Intent(Intent.ACTION_DIAL)));
        allowed.addView(curfewAction(t("ALARM", "WECKER"), new Intent(AlarmClock.ACTION_SHOW_ALARMS)));
        allowed.addView(curfewAction(t("EMERGENCY", "NOTRUF"), new Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"))));
        middle.addView(allowed);
        c.addView(middle, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        TextView hold = type(14, v.accent, 40, 900);
        hold.setText(t("HOLD TO REQUEST AN EXEMPTION", "HALTEN, UM EINE AUSNAHME ZU BEANTRAGEN"));
        hold.setLetterSpacing(0.12f);
        hold.setGravity(Gravity.CENTER);
        hold.setPadding(dp(12), dp(16), dp(12), dp(16));
        GradientDrawable frame = new GradientDrawable();
        frame.setStroke(Math.max(1, dp(1.5f)), v.accent);
        hold.setBackground(frame);
        hold.setOnClickListener(view -> Toast.makeText(this,
                t("Exemptions must be held for", "Ausnahmen wollen gehalten werden"), Toast.LENGTH_SHORT).show());
        hold.setOnLongClickListener(view -> {
            exempt();
            return true;
        });
        c.addView(hold);
        TextView remembered = type(10, v.muted, 80, 650);
        remembered.setText(t("EXEMPTIONS ARE GRANTED. THEY ARE ALSO REMEMBERED.",
                "AUSNAHMEN WERDEN GEWÄHRT. SIE WERDEN AUCH NICHT VERGESSEN."));
        remembered.setLetterSpacing(0.12f);
        remembered.setGravity(Gravity.CENTER);
        remembered.setPadding(0, dp(10), 0, 0);
        c.addView(remembered);
        return c;
    }

    /** The morning roll call: once a day after 06:00 the citizen reports present. */
    private View buildRollCall() {
        LinearLayout c = column();
        c.setBackgroundColor(v.bg);
        c.setClickable(true);
        c.setVisibility(View.GONE);
        c.setOnApplyWindowInsetsListener((view, insets) -> {
            Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            view.setPadding(bars.left + dp(28), bars.top + dp(10), bars.right + dp(28), bars.bottom + dp(28));
            return insets;
        });

        TextView name = type(16, v.ink, 25, 1000);
        name.setText(t("LAIBACH · ADMINISTRATION", "LAIBACH · VERWALTUNG"));
        name.setLetterSpacing(0.2f);
        c.addView(name);
        c.addView(rule(v.accent), ruleParams(dp(3), dp(6), 0));

        LinearLayout middle = column();
        middle.setGravity(Gravity.CENTER);
        middle.addView(new EmblemView(this, v), new LinearLayout.LayoutParams(dp(96), dp(96)));
        TextView head = glitch(34, v.ink, 25, 1000);
        head.setText(t("MORNING\nROLL CALL", "MORGEN-\nAPPELL"));
        head.setGravity(Gravity.CENTER);
        head.setLetterSpacing(0.12f);
        head.setPadding(0, dp(28), 0, dp(12));
        middle.addView(head);
        TextView who = type(14, v.accent, 40, 900);
        who.setText(t("CITIZEN № ", "BÜRGER NR. ") + Citizen.number(this));
        who.setLetterSpacing(0.16f);
        who.setGravity(Gravity.CENTER);
        middle.addView(who);
        TextView note = type(12, v.muted, 80, 650);
        note.setText(t("THE COLLECTIVE IS COUNTING.\nPLEASE BE COUNTED.", "DAS KOLLEKTIV ZÄHLT.\nBITTE LASSEN SIE SICH ZÄHLEN."));
        note.setGravity(Gravity.CENTER);
        note.setLetterSpacing(0.1f);
        note.setLineSpacing(0, 1.2f);
        note.setPadding(0, dp(14), 0, 0);
        middle.addView(note);
        c.addView(middle, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        rollStreak = type(11, v.muted, 80, 700);
        rollStreak.setGravity(Gravity.CENTER);
        rollStreak.setLetterSpacing(0.12f);
        rollStreak.setPadding(0, 0, 0, dp(12));
        c.addView(rollStreak);
        TextView present = type(22, v.bg, 40, 1000);
        present.setText(t("PRESENT", "ANWESEND"));
        present.setLetterSpacing(0.2f);
        present.setGravity(Gravity.CENTER);
        present.setPadding(dp(12), dp(18), dp(12), dp(18));
        present.setBackgroundColor(v.accent);
        present.setOnClickListener(view -> answerRollCall(true));
        c.addView(present);
        TextView absent = type(10, v.muted, 80, 650);
        absent.setText(t("ABSENCE IS ALSO AN ANSWER. IT IS WRITTEN DOWN.",
                "AUCH ABWESENHEIT IST EINE ANTWORT. SIE WIRD NOTIERT."));
        absent.setLetterSpacing(0.12f);
        absent.setGravity(Gravity.CENTER);
        absent.setPadding(0, dp(10), 0, 0);
        c.addView(absent);
        return c;
    }

    private TextView curfewAction(String text, Intent intent) {
        TextView a = type(16, v.ink, 40, 850);
        a.setText(text);
        a.setLetterSpacing(0.14f);
        a.setPadding(dp(12), dp(12), dp(12), dp(12));
        a.setOnClickListener(view -> open(intent));
        return a;
    }

    /** The English or the German text, by the chosen language. */
    private String t(String en, String de) {
        return german ? de : Lang.en(en);
    }

    private LinearLayout column() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    private LinearLayout row() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        return l;
    }

    /** Text in Roboto Flex at the given width and weight axes. */
    private TextView type(float sp, int color, int width, int weight) {
        return style(new TextView(this), sp, color, width, weight);
    }

    /** Like {@link #type}, but the text breaks up whenever the state glitches. */
    private TextView glitch(float sp, int color, int width, int weight) {
        return style(new Cyber.GlitchText(this, v), sp, color, width, weight);
    }

    private TextView style(TextView t, float sp, int color, int width, int weight) {
        t.setTypeface(flex);
        if (!t.setFontVariationSettings("'wdth' " + width + ", 'wght' " + weight)) {
            t.setTypeface(Typeface.create("sans-serif-condensed", weight >= 600 ? Typeface.BOLD : Typeface.NORMAL));
        }
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        return t;
    }

    private TextView label(String text) {
        TextView t = type(10, v.accent, 80, 800);
        t.setLetterSpacing(0.22f);
        t.setText(text);
        return t;
    }

    /** Adds a labelled column to the stats row and returns its value view. */
    private TextView stat(LinearLayout stats, TextView label) {
        LinearLayout col = column();
        col.addView(label);
        TextView value = type(26, v.ink, 25, 850);
        value.setIncludeFontPadding(false);
        col.addView(value);
        stats.addView(col, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        return value;
    }

    private View rule(int color) {
        View r = new View(this);
        r.setBackgroundColor(color);
        return r;
    }

    private LinearLayout.LayoutParams ruleParams(int height, int above, int below) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height);
        lp.topMargin = above;
        lp.bottomMargin = below;
        return lp;
    }

    // ---- State --------------------------------------------------------------

    private void updateClock() {
        Date now = new Date();
        String pattern = DateFormat.is24HourFormat(this) ? "HH:mm" : "h:mm";
        String hhmm = new SimpleDateFormat(pattern, locale).format(now);
        int colon = hhmm.indexOf(':');
        SpannableString time;
        if (formation.stacked && colon >= 0) {
            // The hour over the minute, the minute in the accent.
            time = new SpannableString(hhmm.substring(0, colon) + "\n" + hhmm.substring(colon + 1));
            time.setSpan(new ForegroundColorSpan(v.accent), colon + 1, time.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        } else {
            time = new SpannableString(hhmm);
            if (colon >= 0) {
                time.setSpan(new ForegroundColorSpan(v.accent), colon, colon + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        clock.setText(time);
        int day = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
        date.setText("■ " + new SimpleDateFormat(t("EEEE, d MMMM", "EEEE, d. MMMM"), locale)
                .format(now).toUpperCase(locale) + t(" · PLAN DAY ", " · PLANTAG ") + day);
        updateCurfew();
        updateRollCall();
    }

    private void showSlogan() {
        // A tired phone overrides the proclamation of the day.
        String text = batteryPct >= 0 && batteryPct <= 15 && !charging
                ? t("LIFE FORCE LOW. REPORT TO A CHARGING STATION.", "LEBENSKRAFT NIEDRIG. MELDEN SIE SICH AN EINER LADESTATION.")
                : Lang.pick(german, SLOGANS[sloganIndex]);
        SpannableString s = new SpannableString("■ " + text);
        s.setSpan(new ForegroundColorSpan(v.accent), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        slogan.setText(s);
    }

    // ---- Approved art -------------------------------------------------------

    /** Fetches another work from the Volkskunst gallery in the background. */
    private void nextArt() {
        if (fetching) return;
        ConnectivityManager cm = getSystemService(ConnectivityManager.class);
        if (cm.getActiveNetwork() == null) {
            if (art.current == null) showArtCaption(null);
            return;
        }
        fetching = true;
        if (art.current == null) {
            artCaption.setText(t("■ ART IS BEING APPROVED …", "■ KUNST WIRD GENEHMIGT …"));
        }
        int width = getResources().getDisplayMetrics().widthPixels;
        new Thread(() -> {
            WallArt.Art fetched = null;
            try {
                fetched = WallArt.fetch(this, width);
            } catch (IOException | RuntimeException e) {
                android.util.Log.w("Laibach", "art of the day: " + e);
            }
            WallArt.Art result = fetched;
            runOnUiThread(() -> {
                fetching = false;
                if (isDestroyed()) return;
                if (result != null) art.setArt(result);
                showArtCaption(art.current);
            });
        }, "wallart").start();
    }

    private void showArtCaption(WallArt.Art a) {
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
        artCaption.setText(s);
    }

    private void readBattery(Intent intent) {
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        batteryPct = level < 0 ? -1 : Math.round(level * 100f / scale);
        charging = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0;
    }

    private void updateState() {
        // Austerity: at 15% or less, and not charging, colour and motion are suspended.
        boolean austerity = batteryPct >= 0 && batteryPct <= 15 && !charging;
        if (austerity != Fx.austerity) {
            Fx.austerity = austerity;
            if (austerity) {
                Record.noteDaily(this, "austerity", "AUSTERITY MEASURES DECLARED. COLOUR AND MOTION SUSPENDED.",
                        "SPARMASSNAHMEN VERKÜNDET. FARBE UND BEWEGUNG AUSGESETZT.");
            }
            handler.post(this::recreate);
        }
        ConnectivityManager cm = getSystemService(ConnectivityManager.class);
        NetworkCapabilities caps = cm.getNetworkCapabilities(cm.getActiveNetwork());
        String net;
        if (caps == null) {
            net = t("OFF GRID (REPORTED)", "NETZFREI (GEMELDET)");
        } else {
            net = (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ? t("WI-FI", "WLAN") : t("NETWORK", "NETZ"))
                    + (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) ? t("\nVPN (NOTED)", "\nVPN (VERMERKT)") : "");
        }
        status.setText(net + "\n" + v.name);

        device.setText((t("LAIBACH LIFE™ ", "LAIBACH LEBEN™ ") + version()
                + " — ANDROID " + Build.VERSION.RELEASE).toUpperCase(locale));

        uptimeValue.setText(uptime());
        ticker.setMessage(broadcast());
        showSlogan();
        updateCitizen();
        String energy = t("LIFE FORCE", "LEBENSKRAFT");
        energyLabel.setText(charging ? energy + " ▲" : energy);
        energyValue.setText(batteryPct < 0 ? "—" : batteryPct + "%");
        energyValue.setContentDescription(energy + " " + (batteryPct < 0 ? "—" : batteryPct + "%"));
        energyValue.setTextColor(batteryPct >= 0 && batteryPct <= 15 && !charging ? v.accent : v.ink);
    }

    /** The ticker's text: the state's news, with the citizen's own number in it. */
    private String broadcast() {
        String n = Citizen.number(this);
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
        List<Bulletins.Order> orders = Bulletins.orders(this);
        if (!orders.isEmpty()) {
            b.append("   ✚   ").append(t("MOBILISATION ORDER: ", "EINBERUFUNG: ")).append(order(orders.get(0)));
            if (orders.size() > 1) {
                b.append("   ✚   ").append(String.format(locale, t("%d FURTHER CALL-UPS ISSUED. TAP FOR ORDERS",
                        "%d WEITERE EINBERUFUNGEN ERGANGEN. TIPPEN FÜR BEFEHLE"), orders.size() - 1));
            }
        }
        String[] tv = Bulletins.latestTv(this);
        if (tv != null) {
            b.append("   ✚   ").append(t("NOW ON LAIBACH TV: ", "JETZT IM LAIBACH-FERNSEHEN: "))
                    .append(tv[1].toUpperCase(locale));
        }
        for (String[] line : news) b.append("   ✚   ").append(Lang.pick(german, line));
        return b.append("   ✚   ").append(v.name).append(": ").append(v.motto(german))
                .append("   ✚   LAIBACH OS 2.0").toString();
    }

    // ---- Citizen ------------------------------------------------------------

    private void updateCitizen() {
        long used = Citizen.used(this);
        long left = Citizen.RATION - used;
        boolean over = left < 0;
        rationLabel.setText(over ? t("OVERDRAWN", "ÜBERZOGEN") : t("RATION LEFT", "RESTZUTEILUNG"));
        rationValue.setText((over ? "+" : "") + span(Math.abs(left)));
        rationValue.setTextColor(over ? v.accent : v.ink);
        float share = used / (float) Citizen.RATION;
        rationBar.set(share);
        // The assumed heartbeat rises as the standing falls.
        int bpm = 64 + 9 * Citizen.grade(this) + (over ? 12 : 0);
        pulse.set(String.format(locale, t("♥ %d BPM (ASSUMED)", "♥ %d BPM (ANGENOMMEN)"), bpm), bpm);
        String note = String.format(locale, t("%d%% OF %s ATTENTION CONSUMED", "%d%% VON %s AUFMERKSAMKEIT VERBRAUCHT"),
                Math.round(share * 100), span(Citizen.RATION));
        if (over) {
            Record.noteDaily(this, "overdrawn", "ATTENTION RATION OVERDRAWN. THE STATE EXTENDS CREDIT.",
                    "AUFMERKSAMKEITSRATION ÜBERZOGEN. DER STAAT GEWÄHRT KREDIT.");
        }
        if (!Citizen.usageAccess(this)) note += t(" (ESTIMATED)", " (GESCHÄTZT)");
        else if (over) note += t(" (REPORTED)", " (GEMELDET)");
        rationNote.setText(note);
        rationValue.setContentDescription(rationLabel.getText() + " " + rationValue.getText());
        int grade = Citizen.grade(this);
        SpannableString s = new SpannableString(t("STANDING ", "ANSEHEN ") + Lang.pick(german, Citizen.STANDING[grade]));
        if (grade >= 3) {
            s.setSpan(new ForegroundColorSpan(v.accent), s.length() - Lang.pick(german, Citizen.STANDING[grade]).length(),
                    s.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        standing.setText(s);
    }

    /** 1H 52M, 23M. */
    private String span(long ms) {
        long minutes = ms / 60_000;
        if (minutes < 60) return minutes + "M";
        return minutes / 60 + "H " + minutes % 60 + "M";
    }

    // ---- Decrees ------------------------------------------------------------

    private void updateDecrees() {
        decrees.clear();
        Map<String, Integer> waiting = DecreeListener.waiting(this);
        int total = 0;
        for (int n : waiting.values()) total += n;
        // A new decree is tapped out in Morse, if the citizen asked for it.
        if (lastWaiting >= 0 && total > lastWaiting) Morse.tap(this, false);
        lastWaiting = total;
        Map<Ministry, Integer> perMinistry = new HashMap<>();
        Map<Ministry, String> busiest = new HashMap<>();
        Map<Ministry, Integer> busiestCount = new HashMap<>();
        for (Map.Entry<String, Integer> e : waiting.entrySet()) {
            Ministry m;
            try {
                m = Ministry.of(this, getPackageManager().getApplicationInfo(e.getKey(), 0));
            } catch (android.content.pm.PackageManager.NameNotFoundException ex) {
                m = Ministry.ADMINISTRATION;
            }
            perMinistry.merge(m, e.getValue(), Integer::sum);
            if (e.getValue() > busiestCount.getOrDefault(m, 0)) {
                busiestCount.put(m, e.getValue());
                busiest.put(m, e.getKey());
            }
        }
        for (Ministry m : Ministry.values()) {
            Integer n = perMinistry.get(m);
            if (n == null) continue;
            decrees.add(new Decree(m, String.format(locale, Lang.pick(german, NOTICES.get(m)), n), busiest.get(m)));
        }
        if (decrees.isEmpty()) {
            if (DecreeListener.granted(this)) {
                decrees.add(new Decree(Ministry.ADMINISTRATION, t("ALL NOTICES ANSWERED. THE COLLECTIVE IS PLEASED.",
                        "ALLE BESCHEIDE ERLEDIGT. DAS KOLLEKTIV IST ZUFRIEDEN."), null));
            }
            int day = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
            for (int i = 0; i < STANDING_DECREES.length; i++) {
                Object[] d = STANDING_DECREES[(day + i) % STANDING_DECREES.length];
                decrees.add(new Decree((Ministry) d[0], (german ? (String) d[2] : Lang.en((String) d[1])), null));
            }
        }
        if (decreeIndex >= decrees.size()) decreeIndex = 0;
        showDecree();
    }

    private void showDecree() {
        Decree d = decrees.get(decreeIndex);
        String head = "■ " + t("DECREE ", "ERLASS ") + Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
                + "/" + (decreeIndex + 1) + " · " + d.ministry.title(german);
        SpannableStringBuilder s = new SpannableStringBuilder(head);
        s.setSpan(new ForegroundColorSpan(v.accent), 0, head.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        s.append("\n").append(d.text);
        decree.setText(s);
    }

    private void nextDecree() {
        decreeIndex = (decreeIndex + 1) % decrees.size();
        showDecree();
    }

    /** Opens the app a decree is about; a standing decree just gives way to the next. */
    private void obeyDecree() {
        Decree d = decrees.get(decreeIndex);
        Intent intent = d.pkg == null ? null : getPackageManager().getLaunchIntentForPackage(d.pkg);
        if (intent == null) {
            nextDecree();
            return;
        }
        Record.note(this, "DECREE OF THE " + d.ministry.title(false) + " OBEYED. PROMPTLY.",
                "ERLASS BEFOLGT. UMGEHEND. ABSENDER: " + d.ministry.title(true) + ".");
        open(intent);
    }

    // ---- Curfew -------------------------------------------------------------

    private void updateCurfew() {
        boolean on = drill || Citizen.curfew(this);
        if (on && curfew.getVisibility() != View.VISIBLE) reset();
        curfew.setVisibility(on ? View.VISIBLE : View.GONE);
        if (!on) return;
        Calendar now = Calendar.getInstance();
        curfewTime.setText(new SimpleDateFormat("HH:mm", locale).format(now.getTime()));
        int since = (now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE) - 23 * 60 + 1440) % 1440;
        String began = drill
                ? t("THIS IS A CURFEW DRILL.", "DIES IST EINE SPERRSTUNDENÜBUNG.")
                : String.format(locale, t("CURFEW BEGAN %s AGO.", "DIE SPERRSTUNDE BEGANN VOR %s."), span(since * 60_000L));
        curfewNote.setText(began + "\n" + t("YOUR STANDING RISES WHILE YOU SLEEP.",
                "IHR ANSEHEN STEIGT, WÄHREND SIE SCHLAFEN."));
    }

    // ---- Roll call ----------------------------------------------------------

    private void updateRollCall() {
        boolean on = (rollSummoned || Citizen.rollCallDue(this)) && curfew.getVisibility() != View.VISIBLE;
        rollCall.setVisibility(on ? View.VISIBLE : View.GONE);
        if (!on) return;
        int streak = Citizen.streak(this);
        rollStreak.setText(streak == 0
                ? t("ATTENDANCE: NO RECORD OF YOU YET. THIS CAN BE CORRECTED.",
                        "ANWESENHEIT: NOCH KEIN NACHWEIS. DAS LÄSST SICH ÄNDERN.")
                : String.format(locale, t("ATTENDANCE: %d DAY(S) WITHOUT ABSENCE", "ANWESENHEIT: %d TAG(E) OHNE FEHLEN"), streak));
    }

    private void answerRollCall(boolean present) {
        Citizen.answerRollCall(this, present);
        rollSummoned = false;
        String time = new SimpleDateFormat("HH:mm", locale).format(new Date());
        if (present) {
            int streak = Citizen.streak(this);
            Record.note(this, "ROLL CALL " + time + ": PRESENT. DAY " + streak + " OF UNBROKEN ATTENDANCE.",
                    "APPELL " + time + ": ANWESEND. TAG " + streak + " LÜCKENLOSER ANWESENHEIT.");
            Toast.makeText(this, String.format(locale, t("Counted. Day %d. The Collective is complete.",
                    "Gezählt. Tag %d. Das Kollektiv ist vollzählig."), streak), Toast.LENGTH_LONG).show();
        } else {
            Record.note(this, "ROLL CALL " + time + ": ABSENT IN SPIRIT. ATTENDANCE RESET.",
                    "APPELL " + time + ": GEISTIG ABWESEND. ANWESENHEIT ZURÜCKGESETZT.");
            Toast.makeText(this, t("Absence noted. Your attendance begins again tomorrow.",
                    "Abwesenheit vermerkt. Ihre Anwesenheit beginnt morgen von vorn."), Toast.LENGTH_LONG).show();
        }
        updateRollCall();
    }

    /** Back during the roll call is an answer too; otherwise it clears the search. */
    private void back() {
        if (regulations != null) closeRegulations();
        else if (rollCall.getVisibility() == View.VISIBLE) answerRollCall(false); else reset();
    }

    private void exempt() {
        String message;
        if (drill) {
            drill = false;
            Record.note(this, "CURFEW DRILL COMPLETED. COMPLIANCE EXEMPLARY.",
                    "SPERRSTUNDENÜBUNG ABGESCHLOSSEN. FOLGSAMKEIT VORBILDLICH.");
            message = t("Drill complete. Your compliance has been noted.",
                    "Übung beendet. Ihre Folgsamkeit wurde vermerkt.");
        } else {
            long until = Citizen.exempt(this, 30 * 60_000L);
            String end = new SimpleDateFormat("HH:mm", Locale.ROOT).format(new Date(until));
            Record.note(this, "CURFEW EXEMPTION REQUESTED. GRANTED UNTIL " + end + ". IT IS A CONFESSION.",
                    "AUSNAHME VON DER SPERRSTUNDE BEANTRAGT. GEWÄHRT BIS " + end + ". SIE IST EIN GESTÄNDNIS.");
            message = String.format(locale, t("Exemption granted until %s. It has been noted.",
                    "Ausnahme gewährt bis %s. Sie wurde vermerkt."),
                    new SimpleDateFormat("HH:mm", locale).format(new Date(until)));
        }
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        updateCurfew();
        updateCitizen();
    }

    private String uptime() {
        long minutes = SystemClock.elapsedRealtime() / 60_000;
        long days = minutes / 1440, hours = minutes / 60 % 24, mins = minutes % 60;
        if (days > 0) return days + t("D ", "T ") + hours + "H";
        if (hours > 0) return hours + "H " + mins + "M";
        return mins + " MIN";
    }

    private String version() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (android.content.pm.PackageManager.NameNotFoundException e) {
            return "";
        }
    }


    // ---- Apps ---------------------------------------------------------------

    private void changed(String pkg) {
        icons.keySet().removeIf(key -> key.startsWith(pkg + "/"));
        reload();
    }

    private void reload() {
        Set<String> hiddenKeys = prefs.getStringSet(KEY_HIDDEN, Collections.emptySet());
        apps.clear();
        for (UserHandle user : launcherApps.getProfiles()) {
            long serial = userManager.getSerialNumberForUser(user);
            for (LauncherActivityInfo info : launcherApps.getActivityList(null, user)) {
                // Of the launcher's own entries only the state broadcaster and the camera are
                // programmes; watching and photographing are duties, not joys.
                String cls = info.getComponentName().getClassName();
                boolean tv = cls.equals(BroadcastActivity.class.getName()) || cls.equals(DarkroomActivity.class.getName());
                if (info.getComponentName().getPackageName().equals(getPackageName()) && !tv) continue;
                String key = info.getComponentName().flattenToShortString() + "#" + serial;
                apps.add(new App(info, key, hiddenKeys.contains(key),
                        tv ? Ministry.ENLIGHTENMENT : Ministry.of(this, info.getApplicationInfo())));
            }
        }
        Collator collator = Collator.getInstance();
        collator.setStrength(Collator.PRIMARY);
        apps.sort((a, b) -> a.ministry != b.ministry ? a.ministry.compareTo(b.ministry)
                : collator.compare(a.label, b.label));
        applyFilter();
    }

    private void applyFilter() {
        String query = fold(search.getText().toString().trim());
        querying = !query.isEmpty();
        shown.clear();
        int hiddenCount = 0;
        long mobilised = Citizen.mobilisedUntil(this);
        mobilisedShown = mobilised > 0;
        Set<String> essential = Citizen.essential(this);
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
        // Ministries head their programmes; a search shows the matches alone, best first.
        // In a grid the programmes are housed by the floor, and each ministry starts a new one.
        rows.clear();
        Ministry last = null;
        boolean headed = !querying && formation.has(Formation.MINISTRIES);
        Floor floor = null;
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
                floor = new Floor();
                rows.add(floor);
            }
            floor.apps.add(a);
        }
        adapter.notifyDataSetChanged();
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
                    DateFormat.format(DateFormat.is24HourFormat(this) ? "HH:mm" : "h:mm a", mobilised), stoodDown));
            return;
        }
        footer.setVisibility(hiddenCount == 0 && !showHidden ? View.GONE : View.VISIBLE);
        footer.setText(showHidden
                ? t("ALL PROGRAMMES DECLASSIFIED — HIDE REDACTED", "ALLE PROGRAMME FREIGEGEBEN — GESCHWÄRZTE VERBERGEN")
                : hiddenCount + t(" REDACTED — DECLASSIFY", " GESCHWÄRZT — FREIGEBEN"));
    }

    private void reset() {
        if (search.length() > 0) search.setText("");
        search.clearFocus();
        getSystemService(InputMethodManager.class).hideSoftInputFromWindow(search.getWindowToken(), 0);
        if (showHidden) {
            showHidden = false;
            applyFilter();
        }
        list.setSelection(0);
    }

    /** Opens a programme; a rationed one asks first once the day's ration is spent. */
    private void launch(App a, View from) {
        long used = Citizen.used(this);
        if (used < Citizen.RATION || !Citizen.rationed(this).contains(a.key)) {
            launchJoy(a, from);
            return;
        }
        StateMenu.dress(new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle(t("Ration spent", "Zuteilung verbraucht"))
                .setMessage(String.format(locale, t("%s is rationed. Today's ration of %s is spent (%s used). "
                                + "Opening it now is noted in your record.",
                        "%s ist rationiert. Die heutige Zuteilung von %s ist verbraucht (%s genutzt). "
                                + "Wer es jetzt öffnet, wird in der Akte vermerkt."),
                        a.label, span(Citizen.RATION), span(used)))
                .setNegativeButton(t("Comply", "Gehorchen"), (d, w) -> {
                    Record.note(this, "RATIONED PROGRAMME " + a.label.toUpperCase(Locale.ROOT) + " DECLINED. DISCIPLINE NOTED.",
                            "RATIONIERTES PROGRAMM " + a.label.toUpperCase(Locale.GERMAN) + " ABGELEHNT. DISZIPLIN VERMERKT.");
                    Toast.makeText(this, t("Your discipline has been noted.", "Ihre Disziplin wurde vermerkt."),
                            Toast.LENGTH_SHORT).show();
                })
                .setPositiveButton(t("Open anyway (noted)", "Trotzdem öffnen (vermerkt)"), (d, w) -> {
                    Record.note(this, a.label.toUpperCase(Locale.ROOT) + " OPENED BEYOND THE RATION.",
                            a.label.toUpperCase(Locale.GERMAN) + " ÜBER DIE ZUTEILUNG HINAUS GEÖFFNET.");
                    launchJoy(a, from);
                })
                .show(), v);
    }

    /** Opens a programme; the Ministry of Joy takes a coupon first. */
    private void launchJoy(App a, View from) {
        if (a.ministry != Ministry.JOY) {
            start(a, from);
            return;
        }
        if (Citizen.joyLeft(this) > 0) {
            int left = Citizen.spendJoy(this);
            int spent = Citizen.JOY_COUPONS - left;
            Record.note(this, "JOY COUPON " + spent + " OF " + Citizen.JOY_COUPONS + " SPENT.",
                    "FREUDENMARKE " + spent + " VON " + Citizen.JOY_COUPONS + " EINGELÖST.");
            Toast.makeText(this, left == 0
                    ? t("Last joy coupon spent. Enjoy it thoroughly.", "Letzte Freudenmarke eingelöst. Genießen Sie gründlich.")
                    : String.format(locale, t("Joy coupon spent. %d of %d remain. Enjoy responsibly.",
                            "Freudenmarke eingelöst. %d von %d übrig. Genießen Sie verantwortungsvoll."),
                            left, Citizen.JOY_COUPONS), Toast.LENGTH_SHORT).show();
            updateCitizen();
            start(a, from);
            return;
        }
        StateMenu.dress(new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle(t("Joy ration exhausted", "Freudenration erschöpft"))
                .setMessage(String.format(locale, t("All %d joy coupons have been spent today. Further joy is "
                                + "charged to your standing, one grade at a time.",
                        "Alle %d Freudenmarken sind für heute eingelöst. Weitere Freude geht zu Lasten "
                                + "Ihres Ansehens, eine Stufe nach der anderen."), Citizen.JOY_COUPONS))
                .setNegativeButton(t("Renounce joy", "Auf Freude verzichten"), (d, w) -> {
                    Record.note(this, "JOY RENOUNCED VOLUNTARILY. THE SACRIFICE IS APPRECIATED.",
                            "AUF FREUDE FREIWILLIG VERZICHTET. DAS OPFER WIRD GESCHÄTZT.");
                    Toast.makeText(this, t("Your sacrifice has been noted. With gratitude.",
                            "Ihr Opfer wurde vermerkt. Mit Dank."), Toast.LENGTH_SHORT).show();
                })
                .setPositiveButton(t("Open anyway (noted)", "Trotzdem öffnen (vermerkt)"), (d, w) -> {
                    Citizen.overdrawJoy(this);
                    Record.note(this, "JOY TAKEN ON CREDIT. STANDING LOWERED BY ONE GRADE.",
                            "FREUDE AUF KREDIT GENOMMEN. ANSEHEN UM EINE STUFE GESENKT.");
                    updateCitizen();
                    start(a, from);
                })
                .show(), v);
    }

    private void start(App a, View from) {
        Bundle opts = from == null ? null : ActivityOptions
                .makeScaleUpAnimation(from, 0, 0, from.getWidth(), from.getHeight()).toBundle();
        try {
            launcherApps.startMainActivity(a.info.getComponentName(), a.info.getUser(), null, opts);
        } catch (RuntimeException e) {
            Toast.makeText(this, t(a.label + " is not available to you", a.label + " steht Ihnen nicht zur Verfügung"),
                    Toast.LENGTH_SHORT).show();
        }
    }

    // ---- Menus --------------------------------------------------------------

    private void showAppMenu(App a, View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor, Gravity.END);
        Menu menu = popup.getMenu();
        if (a.suspended) {
            menu.add(0, 0, 0, t("Suspended by order", "Auf Anordnung gesperrt")).setEnabled(false);
        } else {
            menu.add(0, 1, 0, a.hidden ? t("Declassify", "Freigeben") : t("Redact", "Schwärzen"));
        }
        // Laibach TV is part of the launcher: it stays in Enlightenment, and banishing it would banish the state.
        boolean own = a.packageName().equals(getPackageName());
        if (!own) {
            SubMenu ministries = menu.addSubMenu(0, 4, 0, t("Transfer to ministry", "Ministerium wechseln"));
            for (Ministry m : Ministry.values()) {
                ministries.add(2, 200 + m.ordinal(), m.ordinal(), m.title(german)).setChecked(m == a.ministry);
            }
            ministries.setGroupCheckable(2, true, true);
            if (Ministry.transferred(this, a.packageName())) {
                ministries.add(0, 5, Ministry.values().length, t("Return to its own ministry", "Ins eigene Ministerium zurück"));
            }
        }
        menu.add(0, 6, 0, t("Rationed (warn once the day's ration is spent)",
                "Rationiert (warnen, wenn die Zuteilung verbraucht ist)"))
                .setCheckable(true).setChecked(Citizen.rationed(this).contains(a.key));
        menu.add(0, 7, 0, t("Essential (stays when mobilised)", "Unentbehrlich (bleibt bei Mobilisierung)"))
                .setCheckable(true).setChecked(Citizen.essential(this).contains(a.key));
        menu.add(0, 2, 0, t("Dossier (app info)", "Akte (App-Info)"));
        if (!a.system && !own) menu.add(0, 3, 0, t("Banish (uninstall)", "Verbannen (deinstallieren)"));
        StateMenu.show(anchor, v, a.label, menu, Gravity.END, item -> {
            int id = item.getItemId();
            if (id >= 200 && id < 200 + Ministry.values().length) {
                Ministry to = Ministry.values()[id - 200];
                Ministry.transfer(this, a.packageName(), to);
                Record.note(this, a.label.toUpperCase(Locale.ROOT) + " TRANSFERRED TO THE " + to.title(false) + ".",
                        a.label.toUpperCase(Locale.GERMAN) + " NEU UNTERSTELLT: " + to.title(true) + ".");
                reload();
                Toast.makeText(this, t(a.label + " now answers to the " + to.title(false).toLowerCase(locale),
                        a.label + " untersteht jetzt dem " + to.title(true).toLowerCase(locale)), Toast.LENGTH_SHORT).show();
                return true;
            }
            switch (id) {
                case 4:
                    return false;
                case 5:
                    Ministry.transfer(this, a.packageName(), null);
                    reload();
                    return true;
                case 6: {
                    boolean on = Citizen.toggle(this, "rationed", a.key);
                    String name = a.label.toUpperCase(Locale.ROOT);
                    Record.note(this, on ? name + " PLACED ON THE RATION." : name + " TAKEN OFF THE RATION.",
                            on ? name + " RATIONIERT." : name + " NICHT MEHR RATIONIERT.");
                    Toast.makeText(this, on ? t(a.label + " is rationed.", a.label + " ist rationiert.")
                            : t(a.label + " is no longer rationed.", a.label + " ist nicht mehr rationiert."),
                            Toast.LENGTH_SHORT).show();
                    return true;
                }
                case 7: {
                    boolean on = Citizen.toggle(this, "essential", a.key);
                    Toast.makeText(this, on ? t(a.label + " is essential.", a.label + " ist unentbehrlich.")
                            : t(a.label + " is no longer essential.", a.label + " ist nicht mehr unentbehrlich."),
                            Toast.LENGTH_SHORT).show();
                    applyFilter();
                    return true;
                }
                case 1:
                    setHidden(a, !a.hidden);
                    return true;
                case 2:
                    launcherApps.startAppDetailsActivity(a.info.getComponentName(), a.info.getUser(), null, null);
                    return true;
                case 3:
                    Record.note(this, "BANISHMENT OF " + a.label.toUpperCase(Locale.ROOT) + " REQUESTED.",
                            "VERBANNUNG VON " + a.label.toUpperCase(Locale.GERMAN) + " BEANTRAGT.");
                    open(new Intent(Intent.ACTION_DELETE, Uri.fromParts("package", a.packageName(), null)));
                    return true;
                default:
                    return false;
            }
        });
    }

    private void showHomeMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        Menu menu = popup.getMenu();
        SubMenu uniforms = menu.addSubMenu(0, 0, 0, t("Uniforms", "Uniformen"));
        addVariants(uniforms);
        String plan = Variant.plan(this);
        SubMenu fiveYear = uniforms.addSubMenu(0, 0, Variant.ALL.length,
                t("Five-year plan (uniform schedule)", "Fünfjahresplan (Uniformplan)"));
        fiveYear.add(3, 30, 0, t("No plan: the chosen uniform", "Kein Plan: die gewählte Uniform"))
                .setChecked(plan.isEmpty());
        fiveYear.add(3, 31, 1, t("A new album uniform every week", "Jede Woche eine neue Album-Uniform"))
                .setChecked(Variant.WEEKLY.equals(plan));
        fiveYear.add(3, 32, 2, t("A uniform for each weekday", "Eine Uniform für jeden Wochentag"))
                .setChecked(Variant.WEEKDAY.equals(plan));
        fiveYear.setGroupCheckable(3, true, true);
        SubMenu formations = menu.addSubMenu(0, 0, 0, t("Formations", "Formationen"));
        for (int i = 0; i < Formation.ALL.length; i++) {
            Formation f = Formation.ALL[i];
            formations.add(6, 200 + i, i, Lang.pick(german, f.title) + " — " + Lang.pick(german, f.note))
                    .setChecked(f == formation);
        }
        formations.setGroupCheckable(6, true, true);
        menu.add(0, 12, 0, t("Papers & permanent record", "Papiere & ständige Akte"));
        menu.add(0, 20, 0, t("Weekly report", "Wochenbericht"));
        menu.add(0, 18, 0, t("Mobilisation orders (tour dates)", "Einberufungen (Tourdaten)"));
        menu.add(0, 13, 0, t("The Constitution", "Die Verfassung"));
        boolean camera = cameraIssued();
        SubMenu art = menu.addSubMenu(0, 0, 0, camera ? t("Art & camera", "Kunst & Kamera") : t("Art", "Kunst"));
        art.add(0, 5, 0, t("Volkskunst (your art)", "Volkskunst (Ihre Kunst)"));
        art.add(0, 6, 1, t("Next approved artwork", "Nächstes genehmigtes Kunstwerk"));
        if (camera) art.add(0, 23, 2, t("Laibach Camera (the darkroom)", "Laibach-Kamera (die Dunkelkammer)"));
        SubMenu duties = menu.addSubMenu(0, 0, 0, t("Duties (mobilise, roll call, drill)", "Pflichten (Mobilisierung, Appell, Übung)"));
        long until = Citizen.mobilisedUntil(this);
        SubMenu mobilise = duties.addSubMenu(0, 0, 0, until > 0
                ? t("Mobilised until ", "Mobilisiert bis ") + DateFormat.format("HH:mm", until)
                : t("Mobilise (essential programmes only)", "Mobilisieren (nur unentbehrliche Programme)"));
        mobilise.add(0, 41, 0, t("For 30 minutes", "Für 30 Minuten"));
        mobilise.add(0, 42, 1, t("For 1 hour", "Für 1 Stunde"));
        mobilise.add(0, 43, 2, t("For 2 hours", "Für 2 Stunden"));
        if (until > 0) mobilise.add(0, 44, 3, t("Stand down now", "Jetzt demobilisieren"));
        duties.add(0, 14, 1, t("Report for roll call", "Zum Appell melden"));
        duties.add(0, 10, 2, t("Curfew drill", "Sperrstundenübung"));
        menu.add(0, 1, 0, showHidden ? t("Hide redacted", "Geschwärzte verbergen") : t("Show redacted", "Geschwärzte zeigen"));
        menu.add(0, 4, 0, t("Settings", "Einstellungen"));
        StateMenu.show(anchor, v, "LAIBACH LIFE™", menu, Gravity.START, item -> act(item.getItemId()));
    }

    /** Carries out an order from the gear menu or the regulations. */
    private boolean act(int id) {
        switch (id) {
            case 12:
                startActivity(new Intent(this, PapersActivity.class));
                return true;
            case 13:
                startActivity(new Intent(this, PapersActivity.class).putExtra(PapersActivity.EXTRA_CONSTITUTION, true));
                return true;
            case 14:
                rollSummoned = true;
                updateRollCall();
                return true;
            case 5:
                startActivity(new Intent(this, VolkskunstActivity.class));
                return true;
            case 6:
                nextArt();
                return true;
            case 71:
            case 72:
            case 73: {
                String to = id == 71 ? Lang.EN : id == 72 ? Lang.DE : Lang.SL;
                if (to.equals(Lang.code(this))) return true;
                String[] names = id == 71 ? new String[]{"ENGLISH", "ENGLISCH"}
                        : id == 72 ? new String[]{"GERMAN", "DEUTSCH"} : new String[]{"SLOVENIAN", "SLOWENISCH"};
                Record.note(this, "CITIZEN NOW THINKS IN " + names[0] + ".", "BÜRGER DENKT JETZT AUF " + names[1] + ".");
                Lang.set(this, to);
                if (LockWallpaper.dressed(this)) LockWallpaper.dress(this, v, Lang.DE.equals(to), null);
                recreate();
                return true;
            }
            case 18:
                showOrders();
                return true;
            case 20:
                Report.show(this, v);
                return true;
            case 30:
            case 31:
            case 32: {
                String to = id == 31 ? Variant.WEEKLY : id == 32 ? Variant.WEEKDAY : "";
                Variant.setPlan(this, to);
                Record.note(this, to.isEmpty() ? "FIVE-YEAR PLAN SUSPENDED. THE CITIZEN DRESSES HIMSELF."
                                : "FIVE-YEAR PLAN ADOPTED: THE UNIFORM IS ISSUED BY THE CALENDAR.",
                        to.isEmpty() ? "FÜNFJAHRESPLAN AUSGESETZT. DER BÜRGER KLEIDET SICH SELBST."
                                : "FÜNFJAHRESPLAN ANGENOMMEN: DIE UNIFORM GIBT DER KALENDER AUS.");
                Variant next = Variant.current(this);
                if (next != v) {
                    if (LockWallpaper.dressed(this)) LockWallpaper.dress(this, next, german, null);
                    recreate();
                }
                return true;
            }
            case 41:
            case 42:
            case 43: {
                int minutes = id == 41 ? 30 : id == 42 ? 60 : 120;
                Citizen.mobilise(this, minutes * 60_000L);
                Record.note(this, "CITIZEN MOBILISED FOR " + minutes + " MINUTES. NON-ESSENTIAL PROGRAMMES STOOD DOWN.",
                        "BÜRGER FÜR " + minutes + " MINUTEN MOBILISIERT. NICHT UNENTBEHRLICHE PROGRAMME ZURÜCKGESTELLT.");
                Toast.makeText(this, t("Mobilised. Mark a programme essential by holding it.",
                        "Mobilisiert. Halten Sie ein Programm, um es als unentbehrlich zu markieren."), Toast.LENGTH_LONG).show();
                applyFilter();
                return true;
            }
            case 44:
                demobilise();
                return true;
            case 51:
                setReveille();
                return true;
            case 52:
            case 53:
                Alarm.setTone(this, id == 52 ? Alarm.BELL : Alarm.ANTHEM);
                return true;
            case 54:
                Alarm.set(this, -1);
                Record.note(this, "REVEILLE CANCELLED. THE CITIZEN WAKES BY HIMSELF.",
                        "WECKRUF AUFGEHOBEN. DER BÜRGER ERWACHT VON SELBST.");
                return true;
            case 23:
                startActivity(new Intent(this, DarkroomActivity.class));
                return true;
            case 24: {
                boolean issue = !cameraIssued();
                getPackageManager().setComponentEnabledSetting(new ComponentName(this, DarkroomActivity.class),
                        issue ? PackageManager.COMPONENT_ENABLED_STATE_DEFAULT : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP);
                Record.note(this, issue ? "THE CAMERA WAS ISSUED." : "THE CAMERA WAS WITHDRAWN.",
                        issue ? "DIE KAMERA WURDE AUSGEGEBEN." : "DIE KAMERA WURDE EINGEZOGEN.");
                return true;
            }
            case 26: {
                boolean issue = !censorIssued();
                getPackageManager().setComponentEnabledSetting(new ComponentName(this, CensorActivity.class),
                        issue ? PackageManager.COMPONENT_ENABLED_STATE_DEFAULT : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP);
                Record.note(this, issue ? "THE CENSOR'S DESK WAS OPENED." : "THE CENSOR'S DESK WAS CLOSED.",
                        issue ? "DER TISCH DES ZENSORS WURDE AUFGESTELLT." : "DER TISCH DES ZENSORS WURDE GERÄUMT.");
                return true;
            }
            case 25:
                issueSounds();
                return true;
            case 21:
                Tones.setOn(this, !Tones.on(this));
                if (Tones.on(this)) Tones.play(this, Tones.charging());
                return true;
            case 22:
                Morse.setOn(this, !Morse.on(this));
                if (Morse.on(this)) Morse.tap(this, true);
                return true;
            case 8:
                open(DecreeListener.settings());
                return true;
            case 9:
                open(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
                return true;
            case 10:
                Record.note(this, "CURFEW DRILL ORDERED.", "SPERRSTUNDENÜBUNG ANGEORDNET.");
                drill = true;
                updateCurfew();
                return true;
            case 11:
                Citizen.setCurfewOn(this, !Citizen.curfewOn(this));
                if (Citizen.curfewOn(this)) {
                    Record.note(this, "CURFEW RESTORED BY POPULAR DEMAND.", "SPERRSTUNDE AUF VIELFACHEN WUNSCH WIEDER EINGEFÜHRT.");
                } else {
                    Record.note(this, "CURFEW ABOLISHED. THE CITIZEN WILL BE WATCHED MORE CLOSELY.",
                            "SPERRSTUNDE ABGESCHAFFT. DER BÜRGER WIRD GENAUER BEOBACHTET.");
                }
                Toast.makeText(this, Citizen.curfewOn(this)
                        ? t("Curfew restored. Rest well.", "Sperrstunde wieder eingeführt. Ruhen Sie wohl.")
                        : t("Curfew abolished. The Collective trusts you. For now.",
                                "Sperrstunde abgeschafft. Das Kollektiv vertraut Ihnen. Noch."),
                        Toast.LENGTH_LONG).show();
                updateCurfew();
                return true;
            case 15:
                boolean calm = !Fx.pacified(this);
                Fx.setPacified(this, calm);
                Record.note(this, calm ? "DISPLAY PACIFIED. NOTHING MOVES. NOTHING NEEDS TO."
                                : "DISPLAY AGITATED. THE STATE IS IN MOTION AGAIN.",
                        calm ? "ANZEIGE BEFRIEDET. NICHTS BEWEGT SICH. NICHTS MUSS."
                                : "ANZEIGE AUFGEWIEGELT. DER STAAT IST WIEDER IN BEWEGUNG.");
                recreate();
                return true;
            case 16:
                boolean seal = !LockActivity.enabled(this);
                LockActivity.setEnabled(this, seal);
                Record.note(this, seal ? "TERMINAL SEALED. EVERY WAKING NOW BEGINS WITH SUBMISSION."
                                : "TERMINAL UNSEALED. THE CITIZEN ENTERS UNANNOUNCED.",
                        seal ? "TERMINAL VERSIEGELT. JEDES ERWACHEN BEGINNT NUN MIT UNTERWERFUNG."
                                : "TERMINAL ENTSIEGELT. DER BÜRGER TRITT UNANGEMELDET EIN.");
                Toast.makeText(this, seal
                        ? t("Terminal sealed. It will be there when the screen wakes.",
                                "Terminal versiegelt. Es erwartet Sie beim nächsten Einschalten.")
                        : t("Terminal unsealed. Your phone's own lock is unchanged.",
                                "Terminal entsiegelt. Die eigene Sperre Ihres Telefons bleibt unverändert."),
                        Toast.LENGTH_LONG).show();
                return true;
            case 17:
                dressLockScreen();
                return true;
            case 1:
                showHidden = !showHidden;
                applyFilter();
                return true;
            case 3:
                Record.note(this, "CITIZEN CONSIDERED DEFECTION.", "BÜRGER ERWOG DAS ÜBERLAUFEN.");
                open(new Intent(Settings.ACTION_HOME_SETTINGS));
                return true;
            case 4:
                showRegulations(0);
                return true;
            case 7:
                open(new Intent(Settings.ACTION_SETTINGS));
                return true;
            default:
                return pickVariant(id) || pickFormation(id);
        }
    }

    // ---- Regulations: the launcher's own settings ---------------------------

    /**
     * The launcher's settings on one page in the uniform. Each line says how
     * things stand and a tap changes it; the page is drawn again after.
     */
    private void showRegulations(int scrollY) {
        closeRegulations();
        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setBackgroundColor(v.bg);
        scroll.setClickable(true);
        LinearLayout page = column();
        scroll.addView(page);
        scroll.setOnApplyWindowInsetsListener((view, insets) -> {
            Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            page.setPadding(bars.left + dp(20), bars.top + dp(14), bars.right + dp(20), bars.bottom + dp(28));
            return insets;
        });

        LinearLayout head = row();
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = glitch(34, v.ink, 25, 1000);
        title.setText(t("REGULATIONS", "VORSCHRIFTEN"));
        title.setLetterSpacing(0.06f);
        head.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView close = type(24, v.accent, 100, 700);
        close.setText("✕");
        close.setGravity(Gravity.CENTER);
        close.setContentDescription(t("Close", "Schließen"));
        close.setBackground(pressable());
        close.setOnClickListener(x -> closeRegulations());
        head.addView(close, new LinearLayout.LayoutParams(dp(48), dp(48)));
        page.addView(head);
        page.addView(label("LAIBACH LIFE™ · " + t("THE LAUNCHER'S SETTINGS", "DIE EINSTELLUNGEN DES LAUNCHERS")));
        page.addView(rule(v.accent), ruleParams(dp(3), dp(12), 0));

        section(page, t("SOUND", "KLANG"));
        toggle(page, t("Sounds", "Klänge"), t("chimes, clicks, the stamp, the start-up",
                "Signale, Klicks, der Stempel, der Start"), Tones.on(this), 21);
        toggle(page, t("Morse vibration", "Morse-Vibration"), t("new decrees are tapped out",
                "neue Erlasse werden gemorst"), Morse.on(this), 22);
        line(page, t("Issue sounds to the phone", "Klänge ans Telefon ausgeben"),
                t("as ringtones, to be chosen in the phone's sound settings",
                        "als Klingeltöne, zu wählen in den Toneinstellungen des Telefons"), "▸", true, x -> act(25));

        section(page, t("DISPLAY", "ANZEIGE"));
        toggle(page, t("Animation", "Animation"), t("glitches, the start-up, the turning gear",
                "Störungen, der Start, das drehende Zahnrad"), !Fx.pacified(this), 15);

        section(page, t("LOCK SCREEN", "SPERRBILDSCHIRM"));
        toggle(page, t("Seal", "Siegel"), t("the state's seal before the phone's own lock",
                "das Siegel des Staates vor der Sperre des Telefons"), LockActivity.enabled(this), 16);
        toggle(page, t("Uniform on the lock screen", "Uniform auf dem Sperrbildschirm"),
                t("the lock screen's picture in the uniform", "das Bild des Sperrbildschirms in der Uniform"),
                LockWallpaper.dressed(this), 17);

        section(page, t("ROUTINES", "TAGESORDNUNG"));
        toggle(page, t("Curfew", "Sperrstunde"), "23:00–06:00", Citizen.curfewOn(this), 11);
        int alarm = Alarm.minutes(this);
        line(page, t("Reveille", "Weckruf"), t("the state's alarm; tap to set the time",
                        "der Wecker des Staates; tippen, um die Zeit festzulegen"),
                alarm < 0 ? t("OFF", "AUS") : String.format(Locale.ROOT, "%02d:%02d", alarm / 60, alarm % 60),
                alarm >= 0, x -> act(51));
        boolean bell = Alarm.BELL.equals(Alarm.tone(this));
        line(page, t("Reveille tone", "Weckton"), t("tap to change", "tippen zum Wechseln"),
                bell ? t("BELL", "GLOCKE") : t("ANTHEM", "HYMNE"), true, x -> {
                    act(bell ? 53 : 52);
                    refreshRegulations();
                });
        if (alarm >= 0) {
            line(page, t("Cancel reveille", "Weckruf aufheben"), null, "▸", true, x -> {
                act(54);
                refreshRegulations();
            });
        }

        section(page, t("CAMERA", "KAMERA"));
        toggle(page, t("Laibach Camera", "Laibach-Kamera"), t("its icon and its place in the share menu",
                "ihr Symbol und ihr Platz im Teilen-Menü"), cameraIssued(), 24);
        section(page, t("CENSOR", "ZENSUR"));
        toggle(page, t("The censor's desk", "Der Tisch des Zensors"), t("shared text comes back blacked out",
                "geteilter Text kommt geschwärzt zurück"), censorIssued(), 26);

        section(page, t("PERMISSIONS", "BERECHTIGUNGEN"));
        boolean decrees = DecreeListener.granted(this), attention = Citizen.usageAccess(this);
        line(page, t("Decrees", "Erlasse"), t("notification access: notifications become decrees",
                        "Benachrichtigungszugriff: Benachrichtigungen werden Erlasse"),
                decrees ? t("GRANTED", "ERTEILT") : t("NOT GRANTED", "NICHT ERTEILT"), decrees, x -> act(8));
        line(page, t("Attention", "Aufmerksamkeit"), t("usage access: the screen time is measured",
                        "Nutzungszugriff: die Bildschirmzeit wird gemessen"),
                attention ? t("GRANTED", "ERTEILT") : t("NOT GRANTED", "NICHT ERTEILT"), attention, x -> act(9));

        section(page, t("LANGUAGE", "SPRACHE"));
        String code = Lang.code(this);
        line(page, "Language · Sprache · Jezik", null,
                Lang.DE.equals(code) ? "DEUTSCH" : Lang.SL.equals(code) ? "SLOVENŠČINA" : "ENGLISH", true, x -> {
                    Menu choice = new PopupMenu(this, x).getMenu();
                    choice.add(4, 71, 0, "English").setChecked(Lang.EN.equals(code));
                    choice.add(4, 72, 1, "Deutsch").setChecked(Lang.DE.equals(code));
                    choice.add(4, 73, 2, "Slovenščina").setChecked(Lang.SL.equals(code));
                    choice.setGroupCheckable(4, true, true);
                    StateMenu.show(x, v, "Language · Sprache · Jezik", choice, Gravity.END, item -> act(item.getItemId()));
                });

        section(page, t("SYSTEM", "SYSTEM"));
        line(page, t("Defect to another home screen", "Zu anderem Startbildschirm überlaufen"),
                t("choose the phone's home app", "die Start-App des Telefons wählen"), "▸", true, x -> act(3));
        line(page, t("Android settings", "Android-Einstellungen"), t("the phone's own settings",
                "die eigenen Einstellungen des Telefons"), "▸", true, x -> act(7));

        TextView foot = type(11, v.muted, 80, 600);
        foot.setLetterSpacing(0.18f);
        foot.setText("LAIBACH LIFE™ " + version() + " · № " + Citizen.number(this));
        foot.setPadding(0, dp(22), 0, 0);
        page.addView(foot);

        regulations = scroll;
        frame.addView(scroll, frame.indexOfChild(crt));
        scroll.requestApplyInsets();
        // Back to the same place once the page has its height; before that the scroll is clamped to the top,
        // so the place waits in the tag in case the page is drawn again first.
        if (scrollY > 0) {
            scroll.setTag(scrollY);
            scroll.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View s, int l, int t, int r, int b, int ol, int ot, int or, int ob) {
                    s.removeOnLayoutChangeListener(this);
                    s.scrollTo(0, scrollY);
                    s.setTag(null);
                }
            });
        }
    }

    /** Where the regulations stand, or are about to stand. */
    private int regulationsY() {
        Object pending = regulations.getTag();
        return pending != null ? (int) pending : regulations.getScrollY();
    }

    /** Draws the regulations again, where they were, if they are open. */
    private void refreshRegulations() {
        if (regulations != null) showRegulations(regulationsY());
    }

    private void closeRegulations() {
        if (regulations == null) return;
        frame.removeView(regulations);
        regulations = null;
    }

    private void section(LinearLayout page, String title) {
        TextView t = label(title);
        t.setPadding(0, dp(26), 0, dp(4));
        page.addView(t);
    }

    /** A line that switches the order id on or off. */
    private void toggle(LinearLayout page, String title, String note, boolean on, int id) {
        line(page, title, note, on ? t("ON", "AN") : t("OFF", "AUS"), on, x -> {
            act(id);
            refreshRegulations();
        });
    }

    /** A line of the regulations: its name and note, its state at the end, in the accent when lit. */
    private void line(LinearLayout page, String title, String note, String value, boolean lit, View.OnClickListener tap) {
        LinearLayout l = row();
        l.setGravity(Gravity.CENTER_VERTICAL);
        l.setMinimumHeight(dp(60));
        l.setPadding(0, dp(10), 0, dp(10));
        l.setBackground(pressable());
        l.setOnClickListener(tap);
        LinearLayout words = column();
        TextView name = type(18, v.ink, 50, 750);
        name.setAllCaps(true);
        name.setLetterSpacing(0.04f);
        name.setText(title);
        words.addView(name);
        if (note != null) {
            TextView n = type(13, v.muted, 90, 450);
            n.setText(note);
            n.setPadding(0, dp(2), 0, 0);
            words.addView(n);
        }
        l.addView(words, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView state = type(14, lit ? v.accent : v.muted, 80, 850);
        state.setLetterSpacing(0.14f);
        state.setText(value);
        state.setPadding(dp(14), 0, 0, 0);
        l.addView(state);
        page.addView(l);
        page.addView(rule(v.rule), ruleParams(Math.max(1, dp(1)), 0, 0));
    }

    private Drawable pressable() {
        return new RippleDrawable(ColorStateList.valueOf((v.accent & 0x00FFFFFF) | 0x40000000), null,
                new ColorDrawable(0xFFFFFFFF));
    }

    /** A call-up as one line: 30 JAN 2027 · LJUBLJANA, SLOVENIA · CANKARJEV DOM. */
    private String order(Bulletins.Order o) {
        StringBuilder b = new StringBuilder(o.date.format(java.time.format.DateTimeFormatter.ofPattern(
                german ? "d. MMM yyyy" : "d MMM yyyy", locale)).toUpperCase(locale));
        b.append(" · ").append(o.city);
        if (!o.country.isEmpty()) b.append(", ").append(o.country);
        if (!o.place.isEmpty()) b.append(" · ").append(o.place.toUpperCase(locale));
        if (o.soldOut) b.append(t(" · SOLD OUT", " · AUSVERKAUFT"));
        return b.toString();
    }

    /** The mobilisation orders: the band's coming concerts; tap one for its tickets. */
    private void showOrders() {
        List<Bulletins.Order> orders = Bulletins.orders(this);
        AlertDialog.Builder b = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
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
                Record.note(this, "MOBILISATION ORDER FOR " + o.city + " READ.", "EINBERUFUNG NACH " + o.city + " GELESEN.");
                open(new Intent(Intent.ACTION_VIEW, Uri.parse(o.link.startsWith("http") ? o.link
                        : "https://www.laibach.org/future-events/")));
            });
        }
        StateMenu.dress(b.show(), v);
    }

    /** The ticker and the on-air strip, from the news last received. */
    private void showBulletins() {
        ticker.setMessage(broadcast());
        String[] tv = Bulletins.latestTv(this);
        if (tv == null || !formation.has(Formation.TV)) {
            tvStrip.setVisibility(View.GONE);
            return;
        }
        SpannableStringBuilder s = new SpannableStringBuilder(t("■ ON AIR · LAIBACH TV", "■ AUF SENDUNG · LAIBACH TV"));
        s.setSpan(new ForegroundColorSpan(v.accent), 0, s.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        s.append("\n").append(tv[1].toUpperCase(locale));
        tvTitle.setText(s);
        android.graphics.Bitmap still = Bulletins.still(tv[0]);
        tvStill.setImageBitmap(still);
        tvStill.setVisibility(still == null ? View.GONE : View.VISIBLE);
        tvStrip.setContentDescription(t("Laibach TV, newest: ", "Laibach TV, neueste Sendung: ") + tv[1]);
        tvStrip.setOnClickListener(view -> startActivity(new Intent(this, BroadcastActivity.class)
                .putExtra(BroadcastActivity.EXTRA_VIDEO_ID, tv[0])
                .putExtra(BroadcastActivity.EXTRA_TITLE, tv[1])));
        tvStrip.setVisibility(View.VISIBLE);
    }

    private void demobilise() {
        Citizen.mobilise(this, 0);
        Record.note(this, "CITIZEN STOOD DOWN. ALL PROGRAMMES RETURN TO THEIR POSTS.",
                "BÜRGER DEMOBILISIERT. ALLE PROGRAMME KEHREN AUF IHRE POSTEN ZURÜCK.");
        applyFilter();
    }

    /** Sets the reveille's time in a clock dressed in the uniform. */
    private void setReveille() {
        int at = Alarm.minutes(this);
        if (at < 0) at = 6 * 60;
        TimePickerDialog d = new TimePickerDialog(this, android.R.style.Theme_DeviceDefault_Dialog_Alert,
                (picker, hour, minute) -> {
                    Alarm.set(this, hour * 60 + minute);
                    String time = String.format(Locale.ROOT, "%02d:%02d", hour, minute);
                    Record.note(this, "REVEILLE ORDERED FOR " + time + ".", "WECKRUF ANGEORDNET FÜR " + time + ".");
                    Toast.makeText(this, Alarm.permitted(this)
                            ? t("Reveille at ", "Weckruf um ") + time + t(". Answer the roll call to silence it.",
                                    ". Beantworten Sie den Appell, um ihn zu beenden.")
                            : t("Android does not allow the state to ring at an exact time.",
                                    "Android erlaubt dem Staat nicht, zu einer genauen Zeit zu läuten."),
                            Toast.LENGTH_LONG).show();
                    refreshRegulations();
                }, at / 60, at % 60, true);
        d.setTitle(t("Reveille", "Weckruf"));
        d.show();
        StateMenu.dress(d, v);
    }

    /** POWER RATION REPLENISHED: shown for a few seconds when the charger goes in. */
    private void showPowerRation() {
        LinearLayout o = column();
        o.setGravity(Gravity.CENTER);
        o.setBackgroundColor((v.bg & 0x00FFFFFF) | 0xEE000000);
        o.setClickable(true);
        o.addView(new EmblemView(this, v), new LinearLayout.LayoutParams(dp(110), dp(110)));
        TextView head = glitch(30, v.ink, 25, 1000);
        head.setText(t("POWER RATION\nREPLENISHED", "ENERGIERATION\nWIRD AUFGEFÜLLT"));
        head.setGravity(Gravity.CENTER);
        head.setLetterSpacing(0.14f);
        head.setPadding(0, dp(24), 0, dp(16));
        o.addView(head);
        View gauge = new View(this) {
            private final Paint paint = new Paint();

            @Override
            protected void onDraw(Canvas c) {
                int segments = 10, filled = Math.round(Math.max(0, batteryPct) / 10f);
                float gap = dp(4), w = (getWidth() - gap * (segments - 1)) / segments;
                for (int i = 0; i < segments; i++) {
                    paint.setColor(i < filled ? v.accent : v.rule);
                    float x = i * (w + gap);
                    c.drawRect(x, 0, x + w, getHeight(), paint);
                }
            }
        };
        gauge.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        o.addView(gauge, new LinearLayout.LayoutParams(dp(240), dp(18)));
        TextView pct = type(15, v.accent, 60, 900);
        pct.setText(t("LIFE FORCE ", "LEBENSKRAFT ") + Math.max(0, batteryPct) + "% ▲");
        pct.setLetterSpacing(0.2f);
        pct.setPadding(0, dp(12), 0, 0);
        o.addView(pct);
        TextView note = type(11, v.muted, 80, 650);
        note.setText(t("THE STATE PROVIDES. THE CITIZEN RECEIVES.", "DER STAAT GIBT. DER BÜRGER EMPFÄNGT."));
        note.setLetterSpacing(0.14f);
        note.setPadding(0, dp(18), 0, 0);
        o.addView(note);
        o.setContentDescription(t("Power ration replenished. Life force ", "Energieration wird aufgefüllt. Lebenskraft ")
                + batteryPct + "%");
        frame.addView(o, frame.indexOfChild(crt));
        Tones.play(this, Tones.charging());
        Runnable gone = () -> {
            if (o.getParent() == null) return;
            if (Fx.enabled(this)) o.animate().alpha(0f).setDuration(400).withEndAction(() -> frame.removeView(o));
            else frame.removeView(o);
        };
        o.setOnClickListener(view -> gone.run());
        if (Fx.enabled(this)) {
            o.setAlpha(0f);
            o.animate().alpha(1f).setDuration(250);
        }
        handler.postDelayed(gone, 4_000);
    }

    private void addVariants(Menu menu) {
        for (int i = 0; i < Variant.ALL.length; i++) {
            menu.add(1, 100 + i, i, Variant.ALL[i].name).setChecked(Variant.ALL[i] == v);
        }
        menu.setGroupCheckable(1, true, true);
    }

    /** Whether the camera's icon and share entry are out (the owner can withdraw them). */
    private boolean cameraIssued() {
        return getPackageManager().getComponentEnabledSetting(new ComponentName(this, DarkroomActivity.class))
                != PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
    }

    /** Whether the censor's desk is in the share menu (the owner can dismiss it). */
    private boolean censorIssued() {
        return getPackageManager().getComponentEnabledSetting(new ComponentName(this, CensorActivity.class))
                != PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
    }

    /** Writes the state's sounds where the phone's sound settings find them; the owner picks them there. */
    private void issueSounds() {
        Toast.makeText(this, t("Pressing the records …", "Die Platten werden gepresst …"), Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            int n = Exports.sounds(this);
            runOnUiThread(() -> {
                if (n > 0) {
                    Record.note(this, n + " STATE SOUNDS WERE ISSUED TO THE PHONE.", n + " KLÄNGE DES STAATES WURDEN ANS TELEFON AUSGEGEBEN.");
                }
                Toast.makeText(this, n > 0
                        ? n + t(" sounds issued. Choose them in the phone's sound settings, under Laibach.",
                                " Klänge ausgegeben. Wählen Sie sie in den Toneinstellungen des Telefons, unter Laibach.")
                        : t("The sounds could not be issued.", "Die Klänge ließen sich nicht ausgeben."), Toast.LENGTH_LONG).show();
            });
        }, "sounds").start();
    }

    private boolean pickVariant(int id) {
        int i = id - 100;
        if (i < 0 || i >= Variant.ALL.length) return false;
        // Choosing a uniform by hand suspends the five-year plan.
        if (!Variant.plan(this).isEmpty()) {
            Variant.setPlan(this, "");
            if (Variant.ALL[i] == v) prefs.edit().putString(Variant.KEY, v.name).apply();
        }
        if (Variant.ALL[i] != v) {
            Record.note(this, "UNIFORM CHANGED TO " + Variant.ALL[i].name + ".",
                    "UNIFORM GEWECHSELT ZU " + Variant.ALL[i].name + ".");
            prefs.edit().putString(Variant.KEY, Variant.ALL[i].name).apply();
            if (LockWallpaper.dressed(this)) LockWallpaper.dress(this, Variant.ALL[i], german, null);
            Tones.play(this, Tones.signature(Variant.ALL[i].name));
            recreate();
        }
        return true;
    }

    private boolean pickFormation(int id) {
        int i = id - 200;
        if (i < 0 || i >= Formation.ALL.length) return false;
        Formation f = Formation.ALL[i];
        if (f != formation) {
            Record.note(this, "FORMATION CHANGED TO " + f.title[0] + ".", "FORMATION GEÄNDERT ZU " + f.title[1] + ".");
            Formation.set(this, f);
            Tones.play(this, Tones.stamp());
            recreate();
        }
        return true;
    }

    /**
     * Puts the uniform on the phone's own lock and PIN screens as their picture,
     * or takes it off again. The seal is lifted when the lock screen is dressed:
     * it would only cover the uniform, and it keeps the fingerprint sensor off.
     */
    private void dressLockScreen() {
        if (LockWallpaper.dressed(this)) {
            boolean ownLost = LockWallpaper.hadOwn(this);
            if (!LockWallpaper.undress(this)) {
                Toast.makeText(this, t("The lock screen refused to undress.", "Der Sperrbildschirm verweigert das Entkleiden."),
                        Toast.LENGTH_LONG).show();
                return;
            }
            Record.note(this, "LOCK SCREEN UNDRESSED. IT WEARS ITS OWN CLOTHES AGAIN.",
                    "SPERRBILDSCHIRM ENTKLEIDET. ER TRÄGT WIEDER SEINE EIGENEN KLEIDER.");
            Toast.makeText(this, ownLost
                    ? t("Lock screen undressed. It shows your home picture; choose its own again in Settings → Wallpaper.",
                            "Sperrbildschirm entkleidet. Er zeigt Ihr Startbild; sein eigenes wählen Sie in Einstellungen → Hintergrund.")
                    : t("Lock screen undressed. Your old picture is back.", "Sperrbildschirm entkleidet. Ihr altes Bild ist zurück."),
                    Toast.LENGTH_LONG).show();
            return;
        }
        Variant uniform = v;
        LockWallpaper.dress(this, uniform, german, ok -> {
            if (!ok) {
                Toast.makeText(this, t("The lock screen refused the uniform.", "Der Sperrbildschirm verweigert die Uniform."),
                        Toast.LENGTH_LONG).show();
                return;
            }
            boolean wasSealed = LockActivity.enabled(this);
            LockActivity.setEnabled(this, false);
            Record.note(this, "LOCK SCREEN DRESSED IN " + uniform.name + ".",
                    "SPERRBILDSCHIRM IN " + uniform.name + " GEKLEIDET.");
            Toast.makeText(this, wasSealed
                    ? t("Lock screen dressed in " + uniform.name + ". The seal is lifted; your finger goes straight to the sensor.",
                            "Sperrbildschirm in " + uniform.name + " gekleidet. Das Siegel ist aufgehoben; Ihr Finger geht direkt zum Sensor.")
                    : t("Lock screen dressed in " + uniform.name + ".", "Sperrbildschirm in " + uniform.name + " gekleidet."),
                    Toast.LENGTH_LONG).show();
            refreshRegulations();
        });
    }

    private void setHidden(App a, boolean hide) {
        Set<String> keys = new HashSet<>(prefs.getStringSet(KEY_HIDDEN, Collections.emptySet()));
        if (hide) keys.add(a.key); else keys.remove(a.key);
        String name = a.label.toUpperCase(Locale.ROOT);
        Record.note(this, hide ? name + " REDACTED. IT NEVER EXISTED." : name + " DECLASSIFIED. IT ALWAYS EXISTED.",
                hide ? name + " GESCHWÄRZT. ES HAT NIE EXISTIERT." : name + " FREIGEGEBEN. ES HAT IMMER EXISTIERT.");
        prefs.edit().putStringSet(KEY_HIDDEN, keys).apply();
        a.hidden = hide;
        applyFilter();
    }

    private void open(Intent intent) {
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(this, t("Not foreseen on this device", "Auf diesem Gerät nicht vorgesehen"),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private Drawable iconFor(App a) {
        Drawable icon = icons.get(a.key);
        if (icon == null) {
            // Every programme wears the state's badge.
            icon = new BitmapDrawable(getResources(), Cyber.icon(a.info.getIcon(0).mutate(), iconPx, v));
            icons.put(a.key, icon);
        }
        return icon;
    }

    private int dp(float value) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                getResources().getDisplayMetrics()));
    }

    /** Lower-case without accents, so "c" finds "č". */
    private static String fold(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }

    private final class AppAdapter extends BaseAdapter {
        @Override public int getCount() { return rows.size(); }
        @Override public Object getItem(int i) { return rows.get(i); }
        @Override public long getItemId(int i) { return i; }
        @Override public int getViewTypeCount() { return 3; }
        @Override public int getItemViewType(int i) {
            Object o = rows.get(i);
            return o instanceof App ? 0 : o instanceof Ministry ? 1 : 2;
        }
        // A floor is not one item: each of its programmes answers for itself.
        @Override public boolean isEnabled(int i) { return rows.get(i) instanceof App; }
        @Override public boolean areAllItemsEnabled() { return false; }

        @Override
        public View getView(int i, View convertView, ViewGroup parent) {
            if (rows.get(i) instanceof Ministry) return header((Ministry) rows.get(i), (LinearLayout) convertView);
            if (rows.get(i) instanceof Floor) return floor((Floor) rows.get(i), (LinearLayout) convertView);
            boolean terminal = formation.list == Formation.TERMINAL;
            LinearLayout row = (LinearLayout) convertView;
            if (row == null) {
                row = row();
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(0, dp(formation.rowPad), 0, dp(formation.rowPad));
                TextView number = type(terminal ? formation.labelSp : 13, v.accent, 25, 900);
                number.setMinWidth(dp(30));
                if (!formation.has(Formation.NUMBERS)) number.setVisibility(View.GONE);
                row.addView(number);
                ImageView icon = new ImageView(HomeActivity.this);
                if (!formation.icons) icon.setVisibility(View.GONE);
                row.addView(icon, new LinearLayout.LayoutParams(iconPx, iconPx));
                // A register has room for a wide name; a parade sets it narrow, heavy and large.
                boolean parade = formation.list == Formation.LABELS;
                TextView label = type(formation.labelSp, v.ink, parade ? 25 : 40, parade ? 900 : 800);
                label.setLetterSpacing(parade ? 0.1f : 0.06f);
                label.setSingleLine(true);
                label.setEllipsize(TextUtils.TruncateAt.END);
                label.setGravity(formation.gravity);
                label.setPadding(formation.icons || formation.has(Formation.NUMBERS) ? dp(14) : 0, 0, 0, 0);
                row.addView(label, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                TextView serial = type(9, v.muted, 70, 700);
                serial.setLetterSpacing(0.14f);
                serial.setPadding(dp(8), 0, 0, 0);
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
            App a = (App) rows.get(i);
            int n = shown.indexOf(a);
            TextView number = (TextView) row.getChildAt(0);
            ImageView icon = (ImageView) row.getChildAt(1);
            TextView label = (TextView) row.getChildAt(2);
            boolean first = querying && n == 0;
            number.setText(first ? "▶" : String.format(locale, "%02d", n + 1));
            if (formation.icons) icon.setImageDrawable(iconFor(a));
            label.setText((terminal ? "> " : "") + a.label.toUpperCase(locale));
            // Every programme has its file number.
            ((TextView) row.getChildAt(3)).setText(String.format(Locale.ROOT, "ID·%04X", a.key.hashCode() & 0xFFFF));
            // The first match is what the Go key opens.
            label.setTextColor(first ? v.accent : v.ink);
            row.setAlpha(a.suspended || a.hidden ? 0.4f : 1f);
            return row;
        }

        /** A floor of a grid: a cell per column, each a programme that opens on a tap. */
        private View floor(Floor fl, LinearLayout row) {
            if (row == null) {
                row = row();
                int gap = formation.boxed ? dp(3) : 0;
                row.setPadding(0, gap, 0, gap);
                for (int c = 0; c < formation.columns; c++) {
                    LinearLayout cell = column();
                    cell.setGravity(formation.icons ? Gravity.CENTER_HORIZONTAL : Gravity.START);
                    cell.setPadding(dp(6), dp(formation.rowPad), dp(6), dp(formation.rowPad));
                    TextView number = type(10, v.accent, 25, 900);
                    number.setLetterSpacing(0.1f);
                    if (!formation.boxed) number.setVisibility(View.GONE);
                    cell.addView(number, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT));
                    ImageView icon = new ImageView(HomeActivity.this);
                    if (!formation.icons) icon.setVisibility(View.GONE);
                    cell.addView(icon, new LinearLayout.LayoutParams(iconPx, iconPx));
                    TextView caption = type(formation.labelSp, v.ink, formation.icons ? 60 : 40, 800);
                    caption.setLetterSpacing(formation.icons ? 0.04f : 0.06f);
                    // A long name takes a second line rather than losing its end.
                    caption.setMaxLines(2);
                    caption.setEllipsize(TextUtils.TruncateAt.END);
                    caption.setGravity(formation.icons ? Gravity.CENTER_HORIZONTAL : Gravity.START);
                    if (formation.icons) caption.setPadding(0, dp(6), 0, 0);
                    if (formation.serif) caption.setTypeface(Typeface.create("serif", Typeface.BOLD));
                    if (!formation.captions) caption.setVisibility(View.GONE);
                    cell.addView(caption, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT));
                    if (formation.boxed) {
                        GradientDrawable box = new GradientDrawable();
                        box.setStroke(Math.max(1, dp(1)), v.rule);
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
            for (int c = 0; c < formation.columns; c++) {
                LinearLayout cell = (LinearLayout) row.getChildAt(c);
                if (c >= fl.apps.size()) {
                    // An empty flat at the end of the last floor keeps the columns straight.
                    cell.setVisibility(View.INVISIBLE);
                    cell.setOnClickListener(null);
                    cell.setOnLongClickListener(null);
                    continue;
                }
                App a = fl.apps.get(c);
                int n = shown.indexOf(a);
                boolean first = querying && n == 0;
                cell.setVisibility(View.VISIBLE);
                ((TextView) cell.getChildAt(0)).setText(first ? "▶" : String.format(locale, "%02d", n + 1));
                if (formation.icons) ((ImageView) cell.getChildAt(1)).setImageDrawable(iconFor(a));
                TextView caption = (TextView) cell.getChildAt(2);
                caption.setText(a.label.toUpperCase(locale));
                caption.setTextColor(first ? v.accent : v.ink);
                if (formation.boxed) {
                    ((GradientDrawable) cell.getBackground()).setStroke(Math.max(1, dp(first ? 2 : 1)), first ? v.accent : v.rule);
                }
                cell.setContentDescription(a.label);
                cell.setAlpha(a.suspended || a.hidden ? 0.4f : 1f);
                cell.setOnClickListener(view -> launch(a, view));
                cell.setOnLongClickListener(view -> {
                    showAppMenu(a, view);
                    return true;
                });
            }
            return row;
        }

        /** A ministry's name over its programmes, with how many answer to it. */
        private View header(Ministry m, LinearLayout row) {
            if (row == null) {
                row = row();
                row.setGravity(Gravity.BOTTOM);
                row.setPadding(0, dp(14), 0, dp(4));
                TextView title = type(11, v.accent, 70, 900);
                title.setLetterSpacing(0.2f);
                title.setGravity(formation.gravity);
                row.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                TextView members = type(11, v.muted, 70, 800);
                row.addView(members);
                if (formation.mono) {
                    title.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
                    title.setLetterSpacing(0.05f);
                    members.setTypeface(Typeface.MONOSPACE);
                }
            }
            int members = 0;
            for (App a : shown) if (a.ministry == m) members++;
            String title = "■ " + m.title(german) + (m == Ministry.JOY ? t(" (RATIONED)", " (RATIONIERT)") : "");
            ((TextView) row.getChildAt(0)).setText(title);
            ((TextView) row.getChildAt(1)).setText(String.format(locale, "%02d", members));
            return row;
        }
    }

    /** The day's ration of attention as a bar, the part consumed in the accent. */
    private final class RationBar extends View {
        private final Paint paint = new Paint();
        private float share;

        RationBar(Context context) {
            super(context);
        }

        void set(float share) {
            this.share = share;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas c) {
            paint.setColor(v.rule);
            c.drawRect(0, 0, getWidth(), getHeight(), paint);
            paint.setColor(v.accent);
            c.drawRect(0, 0, getWidth() * Math.min(1f, share), getHeight(), paint);
            // Ticks every half hour of the ration.
            paint.setColor(v.bg);
            float tick = Math.max(1, dp(1.5f));
            for (int i = 1; i < 4; i++) {
                float x = getWidth() * i / 4f;
                c.drawRect(x - tick / 2, 0, x + tick / 2, getHeight(), paint);
            }
        }
    }

    // ---- Wallpaper ----------------------------------------------------------

    /**
     * The approved art, centre-cropped, in greyscale and under a veil of the
     * variant's background: thin at the top, where the clock stands, and
     * thick behind the programme list, so every line stays readable.
     */
    private final class ArtView extends View {
        private final Paint picture = new Paint(Paint.FILTER_BITMAP_FLAG);
        private final Paint veil = new Paint();
        private final Matrix crop = new Matrix();
        WallArt.Art current;

        ArtView(Context context) {
            super(context);
            ColorMatrix m = new ColorMatrix();
            m.setSaturation(0f);
            float contrast = 1.15f, shift = 128 * (1 - contrast);
            m.postConcat(new ColorMatrix(new float[]{
                    contrast, 0, 0, 0, shift,
                    0, contrast, 0, 0, shift,
                    0, 0, contrast, 0, shift,
                    0, 0, 0, 1, 0}));
            picture.setColorFilter(new ColorMatrixColorFilter(m));
        }

        void setArt(WallArt.Art a) {
            if (a == null) return;
            current = a;
            fit();
            invalidate();
        }

        private void fit() {
            if (current == null || getWidth() == 0) return;
            float bw = current.bitmap.getWidth(), bh = current.bitmap.getHeight();
            float scale = Math.max(getWidth() / bw, getHeight() / bh);
            crop.setScale(scale, scale);
            crop.postTranslate((getWidth() - bw * scale) / 2f, (getHeight() - bh * scale) / 2f);
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            fit();
            int bg = v.bg & 0x00FFFFFF;
            veil.setShader(new LinearGradient(0, 0, 0, h,
                    new int[]{bg | 0x99000000, bg | 0xB8000000, bg | 0xE6000000},
                    new float[]{0f, 0.38f, 0.62f}, Shader.TileMode.CLAMP));
        }

        @Override
        protected void onDraw(Canvas c) {
            c.drawColor(v.bg);
            if (current == null) return;
            c.drawBitmap(current.bitmap, crop, picture);
            c.drawRect(0, 0, getWidth(), getHeight(), veil);
        }
    }
}
