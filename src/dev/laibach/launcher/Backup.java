package dev.laibach.launcher;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The settings as a file in Download/Laibach that can be read back: every
 * preference the citizen chose, the record and the lists. Only backups this
 * launcher wrote are visible to it.
 */
final class Backup {
    private static final String SETTINGS_PREFIX = "laibach-settings-";
    /** The preference files a backup carries; caches and chosen pictures stay behind. */
    private static final String[] BACKED_UP = {Variant.PREFS, "citizen", "record", "ministries", "tv", "stamps", "visas",
            "drills", "passport", "comrades"};

    private Backup() {}

    /** Writes the settings, the record and the lists to Download/Laibach as JSON. */
    static boolean backup(Context c) {
        JSONObject root = new JSONObject();
        try {
            root.put("app", c.getPackageName());
            root.put("version", 1);
            root.put("written", System.currentTimeMillis());
            JSONObject files = new JSONObject();
            for (String name : BACKED_UP) {
                JSONObject prefs = new JSONObject();
                for (Map.Entry<String, ?> e : c.getSharedPreferences(name, Context.MODE_PRIVATE).getAll().entrySet()) {
                    // The dressed lock screen belongs to this phone's wallpaper; it is not carried over.
                    if (e.getKey().startsWith("lock_wallpaper")) continue;
                    Object value = e.getValue();
                    JSONObject typed = new JSONObject();
                    if (value instanceof Boolean) typed.put("b", value);
                    else if (value instanceof Integer) typed.put("i", value);
                    else if (value instanceof Long) typed.put("l", value);
                    else if (value instanceof Float) typed.put("f", ((Float) value).doubleValue());
                    else if (value instanceof String) typed.put("s", value);
                    else if (value instanceof Set) typed.put("set", new JSONArray((Set<?>) value));
                    else continue;
                    prefs.put(e.getKey(), typed);
                }
                files.put(name, prefs);
            }
            root.put("prefs", files);
        } catch (org.json.JSONException e) {
            return false;
        }
        byte[] bytes = root.toString().getBytes(StandardCharsets.UTF_8);
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, SETTINGS_PREFIX + Exports.stamp() + ".json");
        values.put(MediaStore.MediaColumns.MIME_TYPE, "application/json");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/" + Exports.FOLDER);
        return Exports.write(c, MediaStore.Downloads.EXTERNAL_CONTENT_URI, values, out -> {
            try {
                out.write(bytes);
                return true;
            } catch (java.io.IOException e) {
                return false;
            }
        });
    }

    /** The name of the newest backup this launcher wrote, or null. */
    static String latestBackup(Context c) {
        Uri uri = newestBackup(c);
        if (uri == null) return null;
        try (Cursor cur = c.getContentResolver().query(uri,
                new String[]{MediaStore.MediaColumns.DISPLAY_NAME}, null, null, null)) {
            return cur != null && cur.moveToFirst() ? cur.getString(0) : null;
        }
    }

    /**
     * Reads back the newest backup this launcher wrote (only its own files are
     * visible to it) and replaces the settings with it. Returns false if there
     * is none or it cannot be read.
     */
    static boolean restore(Context c) {
        Uri uri = newestBackup(c);
        if (uri == null) return false;
        JSONObject files;
        try (InputStream in = c.getContentResolver().openInputStream(uri)) {
            if (in == null) return false;
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            byte[] chunk = new byte[16 * 1024];
            for (int n; (n = in.read(chunk)) > 0; ) {
                buf.write(chunk, 0, n);
                if (buf.size() > 2_000_000) return false;
            }
            JSONObject root = new JSONObject(buf.toString(StandardCharsets.UTF_8.name()));
            if (!c.getPackageName().equals(root.optString("app"))) return false;
            files = root.getJSONObject("prefs");
        } catch (Exception e) {
            return false;
        }
        for (String name : BACKED_UP) {
            JSONObject prefs = files.optJSONObject(name);
            if (prefs == null) continue;
            SharedPreferences p = c.getSharedPreferences(name, Context.MODE_PRIVATE);
            SharedPreferences.Editor e = p.edit();
            for (String key : p.getAll().keySet()) if (!key.startsWith("lock_wallpaper")) e.remove(key);
            for (java.util.Iterator<String> keys = prefs.keys(); keys.hasNext(); ) {
                String key = keys.next();
                if (key.startsWith("lock_wallpaper")) continue;
                JSONObject typed = prefs.optJSONObject(key);
                if (typed == null) continue;
                if (typed.has("b")) e.putBoolean(key, typed.optBoolean("b"));
                else if (typed.has("i")) e.putInt(key, typed.optInt("i"));
                else if (typed.has("l")) e.putLong(key, typed.optLong("l"));
                else if (typed.has("f")) e.putFloat(key, (float) typed.optDouble("f"));
                else if (typed.has("s")) e.putString(key, typed.optString("s"));
                else if (typed.has("set")) {
                    JSONArray a = typed.optJSONArray("set");
                    Set<String> set = new HashSet<>();
                    for (int i = 0; a != null && i < a.length(); i++) set.add(a.optString(i));
                    e.putStringSet(key, set);
                }
            }
            e.commit();
        }
        Lang.reset();
        return true;
    }

    private static Uri newestBackup(Context c) {
        Uri collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI;
        try (Cursor cur = c.getContentResolver().query(collection, new String[]{MediaStore.MediaColumns._ID},
                MediaStore.MediaColumns.DISPLAY_NAME + " LIKE ? AND " + MediaStore.MediaColumns.OWNER_PACKAGE_NAME + " = ?",
                new String[]{SETTINGS_PREFIX + "%", c.getPackageName()},
                MediaStore.MediaColumns.DATE_ADDED + " DESC")) {
            if (cur == null || !cur.moveToFirst()) return null;
            return android.content.ContentUris.withAppendedId(collection, cur.getLong(0));
        } catch (RuntimeException e) {
            return null;
        }
    }
}
