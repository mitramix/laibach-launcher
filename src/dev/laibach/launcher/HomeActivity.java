package dev.laibach.launcher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Insets;
import android.graphics.Paint;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.UserManager;
import android.view.View;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.window.OnBackInvokedDispatcher;

import java.util.Locale;

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
 * The screen is drawn from parts in files of their own (the {@link Masthead},
 * the {@link Monument}, the {@link Register} of programmes, the
 * {@link Command} line and the rest); this activity makes them, wires the
 * system's broadcasts to them and keeps their lifecycle.
 */
public class HomeActivity extends StateActivity {
    LauncherApps launcherApps;
    UserManager userManager;
    SharedPreferences prefs;
    /** Where everything stands on the home screen; see {@link Formation}. */
    Formation formation;
    ColorMatrixColorFilter greyscale;
    final Handler handler = new Handler(Looper.getMainLooper());
    /** The parts the screen is drawn from, top to bottom. */
    Masthead masthead;
    Monument monument;
    Register register;
    Command command;
    Decrees decree;
    /** The curfew and the roll call that cover the screen; made with the rest of it. */
    Curfew curfew;
    RollCall rollCall;
    /** The drills the owner has scheduled, called by the roll call. */
    Drills drills;
    /** The passport's watch on the border, kept while the home screen lives. */
    Passport passport;
    boolean visible;
    /** When the home screen went out of sight with the screen on; 0 when it did not. */
    long awaySince;
    final Runnable decreesChanged = () -> {
        if (decree != null) decree.update();
    };
    ArtView art;
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
    int batteryPct = -1;
    boolean charging;
    DeadDrop deadDrop;
    Boot boot;
    FrameLayout frame;
    View crt;
    /** When the state's operating system last came up in this process; 0 before the first time. */
    static long booted;
    static final String KEY_SPECTRE = "spectreIssued";

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
            if (register.mobilisedShown != (Citizen.mobilisedUntil(context) > 0)) register.applyFilter();
        }
    };

    /** Estimates attention used while usage access is not granted. */
    final BroadcastReceiver screen = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                if (awaySince > 0) Citizen.charge(context, System.currentTimeMillis() - awaySince);
                awaySince = 0;
                Terminal.seal(context);
            } else if (!visible && !Terminal.sealed) {
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

        wear(buildUi());

        register.watch();
        IntentFilter onOff = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        onOff.addAction(Intent.ACTION_SCREEN_ON);
        registerReceiver(screen, onOff, RECEIVER_NOT_EXPORTED);
        Record.forgetIfClosed(this);
        passport = Passport.watch(this);
        Terminal.onUnseal = () -> {
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
        register.reload();
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
        register.unwatch();
        unregisterReceiver(screen);
        passport.stop();
        Terminal.onUnseal = null;
        DecreeListener.clearOnChange(decreesChanged);
        super.onDestroy();
    }

    // ---- Layout -------------------------------------------------------------

    View buildUi() {
        gearMenu = new GearMenu(this);
        counter = new Counter(this);
        news = new News(this);
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

        masthead = new Masthead(this);
        masthead.build(root);
        monument = new Monument(this);
        deadDrop = new DeadDrop(this);
        ledger = new Ledger(this);
        decree = new Decrees(this);
        onAir = new OnAir(this);
        artCredit = new ArtCredit(this);
        View standing = monument.build(masthead.watch, deadDrop.build(), ledger.build(), decree.build(), onAir.build(), artCredit.build());
        register = new Register(this);
        register.build(root, standing);
        command = new Command(this);
        command.build(root, standing);
        drills = new Drills(this);
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

    // ---- State --------------------------------------------------------------

    void updateClock() {
        monument.updateClock();
        curfew.update();
        rollCall.update();
    }

    // ---- Power ---------------------------------------------------------------

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
        masthead.update();
        ledger.updateService();
        command.broadcast(news.text());
        monument.showSlogan();
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

    void reset() {
        command.clear();
        register.reset();
    }

    /** The ticker and the on-air strip, from the news last received. */
    void showBulletins() {
        command.broadcast(news.text());
        onAir.show();
    }

}
