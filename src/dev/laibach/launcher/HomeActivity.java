package dev.laibach.launcher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Insets;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
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
import android.text.Editable;
import android.text.InputType;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.window.OnBackInvokedDispatcher;

import java.text.Collator;
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
public class HomeActivity extends StateActivity {
    static final String KEY_HIDDEN = "hidden";

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

    LauncherApps launcherApps;
    UserManager userManager;
    SharedPreferences prefs;
    /** Where everything stands on the home screen; see {@link Formation}. */
    Formation formation;
    ColorMatrixColorFilter greyscale;
    final List<App> apps = new ArrayList<>();
    final List<App> shown = new ArrayList<>();
    /** The list as drawn: Apps (or Floors of them in a grid), and Ministry headers between them while nothing is typed. */
    final List<Object> rows = new ArrayList<>();
    final Map<String, Drawable> icons = new HashMap<>();
    ProgrammeAdapter adapter;
    final Handler handler = new Handler(Looper.getMainLooper());
    ListView list;
    EditText search;
    TextView footer;
    TextView clock;
    TextView date;
    TextView slogan;
    TextView status;
    TextView device;
    Decrees decree;
    /** The curfew and the roll call that cover the screen; made with the rest of it. */
    Curfew curfew;
    RollCall rollCall;
    boolean visible;
    /** When the home screen went out of sight with the screen on; 0 when it did not. */
    long awaySince;
    final Runnable decreesChanged = () -> {
        if (decree != null) decree.update();
    };
    TextView count;
    ArtView art;
    View monument;
    boolean showHidden;
    /** The regulations: the launcher's settings, over the home screen while they are open. */
    Regulations regulations;
    /** The queue in front of a rationed programme, over the home screen while someone waits. */
    WaitingRoom waitingRoom;
    /** What the menus offer and what their orders do; made with the rest of the screen. */
    GearMenu gearMenu;
    ProgrammeMenu programmeMenu;
    Orders orders;
    CallUps callUps;
    PowerRation powerRation;
    Counter counter;
    Ledger ledger;
    News news;
    OnAir onAir;
    ArtCredit artCredit;
    static final String KEY_REGULATIONS = "regulations";
    boolean querying;
    int sloganIndex;
    int batteryPct = -1;
    boolean charging;
    int iconPx;
    Ticker ticker;
    DeadDrop deadDrop;
    Boot boot;
    FrameLayout frame;
    View crt;
    /** Whether the list was last filtered for mobilisation. */
    boolean mobilisedShown;
    /** When the state's operating system last came up in this process; 0 before the first time. */
    static long booted;
    static final String KEY_SPECTRE = "spectreIssued";

    final LauncherApps.Callback appsChanged = new LauncherApps.Callback() {
        @Override public void onPackageRemoved(String pkg, UserHandle user) { changed(pkg); }
        @Override public void onPackageAdded(String pkg, UserHandle user) { changed(pkg); }
        @Override public void onPackageChanged(String pkg, UserHandle user) { changed(pkg); }
        @Override public void onPackagesAvailable(String[] pkgs, UserHandle user, boolean replacing) { reload(); }
        @Override public void onPackagesUnavailable(String[] pkgs, UserHandle user, boolean replacing) { reload(); }
        @Override public void onPackagesSuspended(String[] pkgs, UserHandle user) { reload(); }
        @Override public void onPackagesUnsuspended(String[] pkgs, UserHandle user) { reload(); }
    };

    final BroadcastReceiver batteryChanged = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean was = charging;
            int before = batteryPct;
            readBattery(intent);
            if (visible && before >= 0 && !was && charging) powerRation.show();
            updateState();
        }
    };

    final BroadcastReceiver timeChanged = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateClock();
            updateState();
            Hourly.tick(context);
            if (mobilisedShown != (Citizen.mobilisedUntil(context) > 0)) applyFilter();
        }
    };

    /** Estimates attention used while usage access is not granted. */
    final BroadcastReceiver screen = new BroadcastReceiver() {
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
            enlist();
        }
        formation = Formation.current(this);
        ColorMatrix grey = new ColorMatrix();
        grey.setSaturation(0f);
        greyscale = new ColorMatrixColorFilter(grey);
        iconPx = dp(formation.iconDp);
        sloganIndex = Calendar.getInstance().get(Calendar.DAY_OF_YEAR) % SLOGANS.length;

        wear(buildUi());

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
        if (state != null && state.getInt(KEY_REGULATIONS) > 0) regulations.show(state.getInt(KEY_REGULATIONS) - 1);
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
        if (!curfew.showing()) {
            Promotion.check(this);
            Foundation.celebrate(this);
        }
        decree.update();
        regulations.refresh();
        if (formation.has(Formation.ART) && WallArt.due(this)) artCredit.next();
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
    void bootUp() {
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
                {t("> CONTROLLING THE WEATHER", "> WETTER KONTROLLIEREN"), String.format(java.util.Locale.ROOT, "%+d°",
                        Math.round(Weather.now()))},
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
        waitingRoom.leave();
        awaySince = getSystemService(PowerManager.class).isInteractive() ? System.currentTimeMillis() : 0;
        reset();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // Home pressed while already home.
        regulations.close();
        waitingRoom.leave();
        reset();
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        if (regulations.open()) out.putInt(KEY_REGULATIONS, regulations.y() + 1);
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

    View buildUi() {
        gearMenu = new GearMenu(this);
        counter = new Counter(this);
        news = new News(this);
        adapter = new ProgrammeAdapter(this);
        programmeMenu = new ProgrammeMenu(this);
        orders = new Orders(this);
        callUps = new CallUps(this);
        powerRation = new PowerRation(this);
        // The approved art of the day lies behind everything else.
        frame = new FrameLayout(this);
        art = new ArtView(this, v);
        art.setArt(WallArt.load(this));
        frame.addView(art);
        View scene = new Backdrop(this, v);
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

        buildMasthead(root);
        buildMonument();
        buildRegister(root);
        buildCommand(root);
        rollCall = new RollCall(this);
        frame.addView(rollCall.build());
        curfew = new Curfew(this);
        frame.addView(curfew.buildDusk());
        frame.addView(curfew.build());
        regulations = new Regulations(this);
        waitingRoom = new WaitingRoom(this);
        // The glass of the screen lies over everything, the boot over even that.
        crt = new Crt(this, v);
        frame.addView(crt);
        boot = new Boot(this, v);
        frame.addView(boot);
        if (Fx.austerity) {
            // Austerity: colour is suspended along with motion.
            Paint grey = new Paint();
            grey.setColorFilter(greyscale);
            frame.setLayerType(View.LAYER_TYPE_HARDWARE, grey);
        }
        return frame;
    }

    /** Masthead: the emblem (one tap opens the menu), the name and the status, then the device line. */
    void buildMasthead(LinearLayout root) {
        LinearLayout mast = row();
        mast.setGravity(Gravity.CENTER_VERTICAL);
        EmblemView emblem = new EmblemView(this, v);
        emblem.setContentDescription(t("Menu", "Menü"));
        emblem.setOnClickListener(view -> gearMenu.show(view));
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
        View eye = new Eye(this, v);
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
    }

    /** The monument: clock, date, proclamation, the ledger, the decree, on air and the art credit. */
    void buildMonument() {
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
            gearMenu.show(view);
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
        deadDrop = new DeadDrop(this);
        mon.addView(deadDrop.build());

        ledger = new Ledger(this);
        mon.addView(ledger.build());

        decree = new Decrees(this);
        mon.addView(decree.build());

        onAir = new OnAir(this);
        mon.addView(onAir.build());

        artCredit = new ArtCredit(this);
        mon.addView(artCredit.build());
        monument = mon;
    }

    /** The register: the monument and the list heading scroll with the programmes, then the footer. */
    void buildRegister(LinearLayout root) {
        // The monument and the list heading scroll with the programmes, so the
        // citizen's papers leave room for the list.
        LinearLayout top = column();
        top.addView(monument);

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
            if (parent.getItemAtPosition(pos) instanceof App) counter.launch((App) parent.getItemAtPosition(pos), view);
        });
        list.setOnItemLongClickListener((parent, view, pos, id) -> {
            if (!(parent.getItemAtPosition(pos) instanceof App)) return false;
            programmeMenu.show((App) parent.getItemAtPosition(pos), view);
            return true;
        });
        root.addView(list, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        footer = type(11, v.muted, 80, 600);
        footer.setLetterSpacing(0.14f);
        footer.setPadding(0, dp(8), 0, dp(6));
        footer.setOnClickListener(view -> {
            if (mobilisedShown && !showHidden && !querying) {
                orders.demobilise();
                return;
            }
            showHidden = !showHidden;
            applyFilter();
        });
        root.addView(footer);
    }

    /** The broadcast, then the command line, under the programmes or (in the terminal) over them. */
    void buildCommand(LinearLayout root) {
        ticker = new Ticker(this, v);
        ticker.setOnClickListener(view -> callUps.show());
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
            if (!shown.isEmpty()) counter.launch(shown.get(0), null);
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
    }

    // ---- State --------------------------------------------------------------

    void updateClock() {
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
        curfew.update();
        rollCall.update();
    }

    void showSlogan() {
        // A tired phone overrides the proclamation of the day.
        String text = batteryPct >= 0 && batteryPct <= 15 && !charging
                ? t("LIFE FORCE LOW. REPORT TO A CHARGING STATION.", "LEBENSKRAFT NIEDRIG. MELDEN SIE SICH AN EINER LADESTATION.")
                : Lang.pick(german, SLOGANS[sloganIndex]);
        SpannableString s = new SpannableString("■ " + text);
        s.setSpan(new ForegroundColorSpan(v.accent), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        slogan.setText(s);
    }

    // ---- Approved art -------------------------------------------------------

    void readBattery(Intent intent) {
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        batteryPct = level < 0 ? -1 : Math.round(level * 100f / scale);
        charging = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0;
    }

    void updateState() {
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

        ledger.updateService();
        ticker.setMessage(news.text());
        showSlogan();
        ledger.update();
        ledger.updateEnergy();
    }

    // ---- Citizen ------------------------------------------------------------

    /** Back during the roll call is an answer too; otherwise it clears the search. */
    void back() {
        if (waitingRoom.open()) waitingRoom.leave();
        else if (regulations.open()) regulations.close();
        else if (rollCall.showing()) rollCall.answer(false); else reset();
    }

    String version() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (android.content.pm.PackageManager.NameNotFoundException e) {
            return "";
        }
    }

    // ---- Apps ---------------------------------------------------------------

    void changed(String pkg) {
        icons.keySet().removeIf(key -> key.startsWith(pkg + "/"));
        reload();
    }

    void reload() {
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

    void applyFilter() {
        String query = App.fold(search.getText().toString().trim());
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

    void reset() {
        if (search.length() > 0) search.setText("");
        search.clearFocus();
        getSystemService(InputMethodManager.class).hideSoftInputFromWindow(search.getWindowToken(), 0);
        if (showHidden) {
            showHidden = false;
            applyFilter();
        }
        list.setSelection(0);
    }

    /** The ticker and the on-air strip, from the news last received. */
    void showBulletins() {
        ticker.setMessage(news.text());
        onAir.show();
    }

    void setHidden(App a, boolean hide) {
        Set<String> keys = new HashSet<>(prefs.getStringSet(KEY_HIDDEN, Collections.emptySet()));
        if (hide) keys.add(a.key); else keys.remove(a.key);
        String name = a.label.toUpperCase(Locale.ROOT);
        Record.note(this, hide ? name + " REDACTED. IT NEVER EXISTED." : name + " DECLASSIFIED. IT ALWAYS EXISTED.",
                hide ? name + " GESCHWÄRZT. ES HAT NIE EXISTIERT." : name + " FREIGEGEBEN. ES HAT IMMER EXISTIERT.");
        prefs.edit().putStringSet(KEY_HIDDEN, keys).apply();
        a.hidden = hide;
        applyFilter();
    }

    Drawable iconFor(App a) {
        Drawable icon = icons.get(a.key);
        if (icon == null) {
            // Every programme wears the state's badge.
            icon = new BitmapDrawable(getResources(), Cyber.icon(a.info.getIcon(0).mutate(), iconPx, v));
            icons.put(a.key, icon);
        }
        return icon;
    }
}
