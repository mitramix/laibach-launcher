package dev.laibach.launcher;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;

import java.util.Locale;

/**
 * The ministries of the Collective. Every programme answers to one of them: picked
 * from the package name first, then from the category the app declares, and
 * Administration for the rest. The citizen may transfer a programme to another
 * ministry from its long-press menu; transfers are kept per package.
 */
enum Ministry {
    COMMUNICATION("MINISTRY OF COMMUNICATION", "MINISTERIUM FÜR KOMMUNIKATION",
            "dialer", "phone", "contacts", "mms", "messag", "mail", ".gm", "whatsapp", "telegram",
            "signal", "viber", "messenger", "teams", "slack", "discord", "zoom", "skype"),
    ENLIGHTENMENT("MINISTRY OF ENLIGHTENMENT", "MINISTERIUM FÜR AUFKLÄRUNG",
            "browser", "chrome", "firefox", "brave", "opera", "duckduckgo", "wikipedia", "news",
            "translate", "googlequicksearchbox", "reader", "books", "claude", "chatgpt"),
    LABOUR("MINISTRY OF LABOUR", "MINISTERIUM FÜR ARBEIT",
            "calendar", "clock", "calculator", "notes", "docs", "drive", "files", "filemanager",
            "fileexplorer", "keep", "office", "sheets", "slides", "recorder", "scanner", "tasks"),
    JOY("MINISTRY OF JOY", "MINISTERIUM FÜR FREUDE",
            "camera", "gallery", "photos", "music", "spotify", "netflix", "youtube", "tiktok",
            "instagram", "player", "video", "game", "radio", "podcast", "soundcloud", "tidal"),
    MOVEMENT("MINISTRY OF MOVEMENT", "MINISTERIUM FÜR BEWEGUNG",
            "maps", "waze", "uber", "bolt", "transit", "train", "rail", "flight", "airline",
            "booking", "airbnb", "weather", "fitness", "health"),
    PROSPERITY("MINISTRY OF PROSPERITY", "MINISTERIUM FÜR WOHLSTAND",
            "bank", "wallet", "pay", "revolut", "wise", "finance", "crypto", "shop", "amazon",
            "ebay", "aliexpress", "temu"),
    ADMINISTRATION("ADMINISTRATION", "VERWALTUNG");

    private static final String PREFS = "ministries";

    final String en, de;
    private final String[] keywords;

    Ministry(String en, String de, String... keywords) {
        this.en = en;
        this.de = de;
        this.keywords = keywords;
    }

    String title(boolean german) {
        return german ? de : Lang.en(en);
    }

    /** The ministry a programme answers to. */
    static Ministry of(Context c, ApplicationInfo ai) {
        String transferred = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(ai.packageName, null);
        if (transferred != null) {
            try {
                return valueOf(transferred);
            } catch (IllegalArgumentException ignored) {
                // A ministry that was dissolved in an update; fall through to the default.
            }
        }
        String pkg = ai.packageName.toLowerCase(Locale.ROOT);
        for (Ministry m : values()) {
            for (String k : m.keywords) if (pkg.contains(k)) return m;
        }
        switch (ai.category) {
            case ApplicationInfo.CATEGORY_SOCIAL: return COMMUNICATION;
            case ApplicationInfo.CATEGORY_NEWS: return ENLIGHTENMENT;
            case ApplicationInfo.CATEGORY_PRODUCTIVITY: return LABOUR;
            case ApplicationInfo.CATEGORY_GAME:
            case ApplicationInfo.CATEGORY_AUDIO:
            case ApplicationInfo.CATEGORY_VIDEO:
            case ApplicationInfo.CATEGORY_IMAGE: return JOY;
            case ApplicationInfo.CATEGORY_MAPS: return MOVEMENT;
            default: return ADMINISTRATION;
        }
    }

    /** Transfers a package to a ministry; null returns it to the one it would get by default. */
    static void transfer(Context c, String packageName, Ministry m) {
        SharedPreferences.Editor e = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit();
        if (m == null) e.remove(packageName); else e.putString(packageName, m.name());
        e.apply();
    }

    static boolean transferred(Context c, String packageName) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).contains(packageName);
    }
}
