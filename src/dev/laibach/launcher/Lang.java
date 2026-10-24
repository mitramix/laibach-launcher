package dev.laibach.launcher;

import android.content.Context;

import java.util.Locale;

/**
 * The interface language: English, German or Slovenian. English unless another
 * was picked in the menu. The texts are written in English and German side by
 * side; Slovenian is looked up from the English in {@link Slovene}, and any
 * text without a Slovenian version stays in English.
 */
final class Lang {
    static final String KEY = "lang";
    static final String EN = "en", DE = "de", SL = "sl";
    private static String code;

    private Lang() {}

    static String code(Context context) {
        if (code == null) {
            code = context.getSharedPreferences(Variant.PREFS, Context.MODE_PRIVATE).getString(KEY, EN);
        }
        return code;
    }

    static boolean german(Context context) {
        return DE.equals(code(context));
    }

    static void set(Context context, String to) {
        code = to;
        context.getSharedPreferences(Variant.PREFS, Context.MODE_PRIVATE).edit().putString(KEY, to).apply();
    }

    /** Forgets the cached choice, after the settings were restored from a file. */
    static void reset() {
        code = null;
    }

    /** The English text, or its Slovenian version when Slovenian is chosen. */
    static String en(String en) {
        return SL.equals(code) ? Slovene.of(en) : en;
    }

    /** From a pair {English, German}: the German, or the English (in Slovenian if chosen). */
    static String pick(boolean german, String[] enDe) {
        return german ? enDe[1] : en(enDe[0]);
    }

    static Locale locale(boolean german) {
        return german ? Locale.GERMAN : SL.equals(code) ? Locale.forLanguageTag("sl") : Locale.UK;
    }
}
