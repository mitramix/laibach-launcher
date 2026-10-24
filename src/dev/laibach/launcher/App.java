package dev.laibach.launcher;

import android.content.pm.ApplicationInfo;
import android.content.pm.LauncherActivityInfo;

import java.text.Normalizer;
import java.util.Locale;

/** A programme on the register: what it is called, who it answers to, and whether it shows. */
final class App {
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

    /** Lower-case without accents, so "c" finds "č". */
    static String fold(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }
}
