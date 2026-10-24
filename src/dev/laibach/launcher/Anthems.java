package dev.laibach.launcher;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

/**
 * The state's sounds written as WAV files where the phone's sound settings
 * offer them. The owner chooses them there; the launcher never sets one.
 */
final class Anthems {
    private Anthems() {}

    /** The uniforms' names as they read on a sound's title. */
    private static String titled(String uniform) {
        StringBuilder out = new StringBuilder();
        for (String word : uniform.toLowerCase(Locale.ROOT).split(" ")) {
            if (out.length() > 0) out.append(' ');
            out.append(word.equals("of") ? word : word.equals("wat") ? "WAT"
                    : Character.toUpperCase(word.charAt(0)) + word.substring(1));
        }
        return out.toString();
    }

    /**
     * Writes the state's sounds as WAV files to Ringtones/Laibach,
     * Notifications/Laibach and Alarms/Laibach, where the phone's sound
     * settings offer them. Earlier copies written by the launcher are replaced.
     * The owner chooses them there; the launcher never sets one itself.
     * Returns how many were written.
     */
    static int sounds(Context c) {
        Map<String, short[]> ringtones = new java.util.LinkedHashMap<>();
        Map<String, short[]> notifications = new java.util.LinkedHashMap<>();
        Map<String, short[]> alarms = new java.util.LinkedHashMap<>();
        ringtones.put("Bell", Tones.repeat(Tones.bell(2.6), 3, 0.4));
        ringtones.put("Anthem", Tones.repeat(Tones.anthem(), 2, 0.8));
        notifications.put("Decree", Tones.decree());
        notifications.put("Stamp", Tones.stamp());
        notifications.put("Charging", Tones.charging());
        for (Variant v : Variant.ALL) {
            short[] call = Tones.signature(v.name);
            ringtones.put(titled(v.name), Tones.repeat(call, 3, 0.7));
            notifications.put(titled(v.name), call);
        }
        alarms.put("Reveille Bell", Tones.repeat(Tones.bell(2.6), 4, 0.2));
        alarms.put("Reveille Anthem", Tones.repeat(Tones.anthem(), 3, 0.6));

        int written = 0;
        written += writeSounds(c, ringtones, Environment.DIRECTORY_RINGTONES, MediaStore.Audio.Media.IS_RINGTONE);
        written += writeSounds(c, notifications, Environment.DIRECTORY_NOTIFICATIONS, MediaStore.Audio.Media.IS_NOTIFICATION);
        written += writeSounds(c, alarms, Environment.DIRECTORY_ALARMS, MediaStore.Audio.Media.IS_ALARM);
        return written;
    }

    private static int writeSounds(Context c, Map<String, short[]> sounds, String folder, String kind) {
        Uri audio = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
        String path = folder + "/" + Exports.FOLDER + "/";
        int written = 0;
        for (Map.Entry<String, short[]> e : sounds.entrySet()) {
            String name = "Laibach " + e.getKey() + ".wav";
            // Only the launcher's own earlier copy can be seen here, and only that one is removed.
            try {
                c.getContentResolver().delete(audio, MediaStore.MediaColumns.RELATIVE_PATH + "=? AND "
                        + MediaStore.MediaColumns.DISPLAY_NAME + "=?", new String[]{path, name});
            } catch (RuntimeException ignored) {
                // Nothing to replace.
            }
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
            values.put(MediaStore.MediaColumns.TITLE, "Laibach · " + e.getKey());
            values.put(MediaStore.MediaColumns.MIME_TYPE, "audio/x-wav");
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, path);
            values.put(kind, 1);
            byte[] wav = Tones.wav(e.getValue());
            if (Exports.write(c, audio, values, out -> {
                try {
                    out.write(wav);
                    return true;
                } catch (java.io.IOException x) {
                    return false;
                }
            })) written++;
        }
        return written;
    }
}
