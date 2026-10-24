package dev.laibach.launcher;

import android.content.Context;
import android.view.Gravity;

/**
 * The formations: how the home screen is drawn up. The uniform decides what
 * the screen wears, the formation where everything stands and how the
 * programmes line up, so any uniform can be worn in any formation.
 */
final class Formation {
    static final String KEY = "formation";

    /** How the programmes line up. */
    static final int ROWS = 0, LABELS = 1, GRID = 2, TERMINAL = 3;

    /** The parts of the home screen a formation can stand down. */
    static final int SLOGAN = 1, PAPERS = 1 << 1, STATS = 1 << 2, PULSE = 1 << 3, DECREE = 1 << 4,
            TV = 1 << 5, ART = 1 << 6, DEVICE = 1 << 7, TICKER = 1 << 8, MINISTRIES = 1 << 9, NUMBERS = 1 << 10,
            SCENE = 1 << 11;
    private static final int EVERYTHING = (1 << 12) - 1;

    static final Formation[] ALL = {
            // The standing order: the monument over the full register.
            new Formation("MONUMENT", "MONUMENT", "the monument over the register (standard)",
                    "das Denkmal über dem Register (Standard)"),
            // Names march past in large type, centred, without badges.
            new Formation("PARADE", "PARADE", "the names march past in large type",
                    "die Namen marschieren in großer Schrift vorbei")
                    .list(LABELS).centre().clock(100).label(26, 10)
                    .without(STATS | PULSE | TV | DEVICE | PAPERS | NUMBERS),
            // Badges in a block of four flats to a floor.
            new Formation("HOUSING BLOCK", "PLATTENBAU", "badges in a grid, four to a floor",
                    "Abzeichen im Raster, vier pro Etage")
                    .grid(4, true, true).clock(80).icon(44).label(11, 12)
                    .without(PULSE | TV | STATS | NUMBERS),
            // The state newspaper: a headline, the decree as the lead story, programmes in two columns.
            new Formation("FRONT PAGE", "TITELSEITE", "the state newspaper: headline and two columns",
                    "die Staatszeitung: Schlagzeile und zwei Spalten")
                    .grid(2, false, true).serif().clock(44).headline(24).label(17, 7)
                    .without(PULSE | STATS | TV | NUMBERS),
            // A control panel of numbered, framed switches; the gauges stay.
            new Formation("SWITCHBOARD", "SCHALTTAFEL", "numbered switches in frames, gauges kept",
                    "nummerierte Schalter in Rahmen, Anzeigen bleiben")
                    .grid(3, true, true).boxed().clock(64).icon(36).label(10, 10)
                    .without(SLOGAN | TV | ART | MINISTRIES),
            // The file on everyone: small print, every programme on one screen.
            new Formation("DOSSIER", "DOSSIER", "small print, every programme on one page",
                    "Kleingedrucktes, jedes Programm auf einer Seite")
                    .clock(48).icon(24).label(15, 4)
                    .without(SLOGAN | STATS | PULSE | TV | ART | PAPERS | DEVICE),
            // The hours stacked over the minutes like a column of stone, everything to the right.
            new Formation("OBELISK", "OBELISK", "the hour stacked over the minute, to the right",
                    "die Stunde über der Minute, nach rechts")
                    .list(LABELS).right().stacked().clock(150).label(22, 8)
                    .without(STATS | PULSE | TV | PAPERS | DEVICE | MINISTRIES | NUMBERS | SLOGAN),
            // Nothing but the hour and the names. Silence is a duty.
            new Formation("SILENCE", "STILLE", "only the hour and the names",
                    "nur die Stunde und die Namen")
                    .list(LABELS).clock(110).label(22, 9)
                    .without(SLOGAN | PAPERS | STATS | PULSE | DECREE | TV | ART | DEVICE | TICKER | MINISTRIES | NUMBERS | SCENE),
            // The machine room: fixed-width type, the command line on top.
            new Formation("TERMINAL", "TERMINAL", "fixed-width type, the command line on top",
                    "Schreibmaschinenschrift, die Befehlszeile oben")
                    .list(TERMINAL).mono().commandOnTop().clock(64).label(15, 6)
                    .without(SLOGAN | PULSE | TV | ART),
            // Badges alone, five to a row, one unbroken wall of icons.
            new Formation("ICONOSTASIS", "IKONOSTASE", "badges alone, five to a row",
                    "nur Abzeichen, fünf pro Reihe")
                    .grid(5, true, false).centre().clock(96).icon(48).label(11, 12)
                    .without(STATS | PULSE | TV | PAPERS | DEVICE | DECREE | NUMBERS | MINISTRIES),
    };

    final String name;
    final String[] title;
    final String[] note;
    int list = ROWS;
    int columns = 1;
    boolean icons = true;
    boolean captions = true;
    boolean boxed;
    boolean serif;
    boolean mono;
    boolean stacked;
    boolean commandTop;
    int gravity = Gravity.START;
    float clockSp = 120;
    float headlineSp = 13;
    float labelSp = 20;
    /** Padding above and below each programme, in dp. */
    float rowPad = 9;
    float iconDp = 36;
    private int parts = EVERYTHING;

    private Formation(String name, String nameDe, String note, String noteDe) {
        this.name = name;
        this.title = new String[]{name, nameDe};
        this.note = new String[]{note, noteDe};
    }

    private Formation list(int style) {
        list = style;
        if (style != ROWS) icons = false;
        return this;
    }

    private Formation grid(int columns, boolean icons, boolean captions) {
        this.list = GRID;
        this.columns = columns;
        this.icons = icons;
        this.captions = captions;
        return this;
    }

    private Formation without(int parts) {
        this.parts &= ~parts;
        return this;
    }

    private Formation centre() {
        gravity = Gravity.CENTER_HORIZONTAL;
        return this;
    }

    private Formation right() {
        gravity = Gravity.END;
        return this;
    }

    private Formation clock(float sp) {
        clockSp = sp;
        return this;
    }

    private Formation headline(float sp) {
        headlineSp = sp;
        return this;
    }

    private Formation label(float sp, float pad) {
        labelSp = sp;
        rowPad = pad;
        return this;
    }

    private Formation icon(float dp) {
        iconDp = dp;
        return this;
    }

    private Formation boxed() {
        boxed = true;
        return this;
    }

    private Formation serif() {
        serif = true;
        return this;
    }

    private Formation mono() {
        mono = true;
        return this;
    }

    private Formation stacked() {
        stacked = true;
        return this;
    }

    private Formation commandOnTop() {
        commandTop = true;
        return this;
    }

    boolean has(int part) {
        return (parts & part) != 0;
    }

    static Formation current(Context c) {
        String name = c.getSharedPreferences(Variant.PREFS, Context.MODE_PRIVATE).getString(KEY, ALL[0].name);
        for (Formation f : ALL) if (f.name.equals(name)) return f;
        return ALL[0];
    }

    static void set(Context c, Formation f) {
        c.getSharedPreferences(Variant.PREFS, Context.MODE_PRIVATE).edit().putString(KEY, f.name).apply();
    }
}
