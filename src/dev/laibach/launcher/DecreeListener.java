package dev.laibach.launcher;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Turns the waiting notifications into decrees. Only which app posted them is
 * read, and how many there are: never their title or text. Runs only after the
 * owner grants notification access in Settings.
 */
public class DecreeListener extends NotificationListenerService {
    private static DecreeListener connected;
    private static Runnable onChange;
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    static boolean granted(Context c) {
        String enabled = Settings.Secure.getString(c.getContentResolver(), "enabled_notification_listeners");
        return enabled != null && enabled.contains(c.getPackageName() + "/");
    }

    static Intent settings() {
        return new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
    }

    /** Called on the main thread whenever the waiting notifications change. */
    static void setOnChange(Runnable r) {
        onChange = r;
    }

    /** Forgets r, unless another listener has taken its place since. */
    static void clearOnChange(Runnable r) {
        if (onChange == r) onChange = null;
    }

    /** Number of waiting notifications per package; empty when not connected. */
    static Map<String, Integer> waiting(Context c) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        DecreeListener l = connected;
        if (l == null) return counts;
        StatusBarNotification[] active;
        try {
            active = l.getActiveNotifications();
        } catch (RuntimeException e) {
            return counts;
        }
        if (active == null) return counts;
        for (StatusBarNotification n : active) {
            if (n.isOngoing() || !n.isClearable() || n.getPackageName().equals(c.getPackageName())) continue;
            if ((n.getNotification().flags & Notification.FLAG_GROUP_SUMMARY) != 0) continue;
            counts.merge(n.getPackageName(), 1, Integer::sum);
        }
        return counts;
    }

    private static void changed() {
        MAIN.post(() -> {
            if (onChange != null) onChange.run();
        });
    }

    @Override public void onListenerConnected() { connected = this; changed(); }
    @Override public void onListenerDisconnected() { connected = null; changed(); }
    @Override public void onNotificationPosted(StatusBarNotification sbn) { changed(); }
    @Override public void onNotificationRemoved(StatusBarNotification sbn) { changed(); }
}
