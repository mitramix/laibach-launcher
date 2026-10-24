package dev.laibach.launcher;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Insets;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.animation.DecelerateInterpolator;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Volkskunst: the owner's own pictures, framed and shown in greyscale, one at a
 * time, with a "TITLE — FROM: NAME" (or "TITEL — VON: NAME") caption. Pictures
 * come from the system photo picker (no storage permission needed) and are
 * copied, scaled down, into the app's private folder, so the originals are
 * never touched.
 */
public class VolkskunstActivity extends StateActivity {
    private static final int PICK = 1;
    private static final int MAX_SIDE = 2048;
    private static final String KEY_WORKS = "works";
    private static final String KEY_LAST_AUTHOR = "lastAuthor";
    // Stored for an empty title or author (kept from 1.2) and shown in the chosen language.
    private static final String UNTITLED = "OHNE TITEL";
    private static final String UNKNOWN = "UNBEKANNT";

    /** One picture in the collection. */
    private static final class Work {
        final String file;
        String title;
        String author;

        Work(String file, String title, String author) {
            this.file = file;
            this.title = title;
            this.author = author;
        }
    }

    private SharedPreferences prefs;
    private File dir;
    private final List<Work> works = new ArrayList<>();
    private int index;
    private Wall wall;
    private TextView counter;
    private TextView caption;
    private View actions;
    private View footRule;
    private View empty;
    private boolean importing;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("volkskunst", MODE_PRIVATE);
        dir = new File(getFilesDir(), "volkskunst");
        dir.mkdirs();
        load();

        wear(buildUi());
        show();
    }

    // ---- Layout -------------------------------------------------------------

    private View buildUi() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(v.bg);
        wall = new Wall(this);
        root.addView(wall);

        LinearLayout chrome = new LinearLayout(this);
        chrome.setOrientation(LinearLayout.VERTICAL);
        root.addView(chrome);

        // Head: title, counter, rule.
        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = type(26, v.accent, 25, 1000);
        back.setText("◀");
        back.setPadding(0, 0, dp(12), 0);
        back.setOnClickListener(view -> finish());
        head.addView(back);
        TextView title = type(30, v.ink, 25, 1000);
        title.setText("VOLKSKUNST");
        title.setLetterSpacing(0.14f);
        head.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        counter = type(20, v.accent, 25, 1000);
        counter.setLetterSpacing(0.08f);
        head.addView(counter);
        chrome.addView(head);
        View rule = new View(this);
        rule.setBackgroundColor(v.accent);
        LinearLayout.LayoutParams ruleLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(5));
        ruleLp.topMargin = dp(6);
        chrome.addView(rule, ruleLp);

        // Empty state, centred in the space between head and foot.
        LinearLayout none = new LinearLayout(this);
        none.setOrientation(LinearLayout.VERTICAL);
        none.setGravity(Gravity.CENTER);
        TextView noneTitle = type(34, v.ink, 25, 1000);
        noneTitle.setText(t("NO WORKS YET.", "NOCH KEINE WERKE."));
        noneTitle.setGravity(Gravity.CENTER);
        none.addView(noneTitle);
        TextView noneText = new TextView(this);
        noneText.setTypeface(Typeface.create("serif", Typeface.BOLD));
        noneText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        noneText.setTextColor(v.muted);
        noneText.setGravity(Gravity.CENTER);
        noneText.setLetterSpacing(0.04f);
        noneText.setText(t("THE PEOPLE'S ART AWAITS YOUR CONTRIBUTION.\nFRAMED. IN BLACK AND WHITE.\nTHE ORIGINALS STAY UNTOUCHED.",
                "DIE VOLKSKUNST ERWARTET IHREN BEITRAG.\nGERAHMT. IN SCHWARZWEISS.\nDIE ORIGINALE BLEIBEN UNBERÜHRT."));
        noneText.setPadding(0, dp(12), 0, dp(24));
        none.addView(noneText);
        TextView noneButton = button(t("■ SUBMIT PICTURES", "■ BILDER EINREICHEN"), v.accent, view -> pick());
        noneButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        noneButton.setGravity(Gravity.CENTER);
        none.addView(noneButton, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        empty = none;
        chrome.addView(none, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        // Foot: caption and actions.
        caption = type(17, v.ink, 45, 800);
        caption.setLetterSpacing(0.06f);
        caption.setMaxLines(2);
        caption.setEllipsize(TextUtils.TruncateAt.END);
        caption.setPadding(0, 0, 0, dp(10));
        caption.setOnClickListener(view -> describe());
        chrome.addView(caption);
        footRule = new View(this);
        footRule.setBackgroundColor(v.accent);
        chrome.addView(footRule, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(2)));
        LinearLayout bar = new LinearLayout(this);
        bar.addView(button(t("+ SUBMIT", "+ EINREICHEN"), v.accent, view -> pick()), weight());
        TextView label = button(t("LABEL", "BESCHRIFTEN"), v.ink, view -> describe());
        label.setGravity(Gravity.CENTER_HORIZONTAL);
        bar.addView(label, weight());
        TextView remove = button(t("REMOVE", "ENTFERNEN"), v.muted, view -> remove());
        remove.setGravity(Gravity.END);
        bar.addView(remove, weight());
        actions = bar;
        chrome.addView(bar);

        root.setOnApplyWindowInsetsListener((view, insets) -> {
            Insets b = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            chrome.setPadding(b.left + dp(20), b.top + dp(6), b.right + dp(20), b.bottom + dp(4));
            // The pictures hang between the head and the foot.
            wall.setBounds(b.top + dp(78), b.bottom + dp(124));
            return WindowInsets.CONSUMED;
        });
        return root;
    }

    private LinearLayout.LayoutParams weight() {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
    }

    private TextView button(String text, int color, View.OnClickListener click) {
        TextView b = type(13, color, 50, 850);
        b.setText(text);
        b.setLetterSpacing(0.08f);
        b.setPadding(0, dp(14), 0, dp(14));
        b.setOnClickListener(click);
        return b;
    }

    /** Brings counter, caption and buttons in line with the current picture. */
    private void show() {
        boolean any = !works.isEmpty();
        index = Math.max(0, Math.min(index, works.size() - 1));
        empty.setVisibility(any ? View.INVISIBLE : View.VISIBLE);
        caption.setVisibility(any ? View.VISIBLE : View.INVISIBLE);
        actions.setVisibility(any ? View.VISIBLE : View.INVISIBLE);
        footRule.setVisibility(any ? View.VISIBLE : View.INVISIBLE);
        if (importing) {
            counter.setText(t("INSPECTING …", "WIRD GEPRÜFT …"));
        } else {
            counter.setText(any ? String.format(locale, "%02d / %02d", index + 1, works.size()) : "");
        }
        if (any) {
            Work w = works.get(index);
            SpannableStringBuilder s = new SpannableStringBuilder("■ ");
            s.setSpan(new ForegroundColorSpan(v.accent), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            s.append(shown(w.title, UNTITLED, "UNTITLED").toUpperCase(locale)).append(" — ");
            int from = s.length();
            s.append(t("FROM: ", "VON: ")).append(shown(w.author, UNKNOWN, "UNKNOWN").toUpperCase(locale));
            s.setSpan(new ForegroundColorSpan(v.muted), from, s.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            caption.setText(s);
        }
        wall.invalidate();
    }

    // ---- Collection ---------------------------------------------------------

    private void load() {
        works.clear();
        try {
            JSONArray a = new JSONArray(prefs.getString(KEY_WORKS, "[]"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                Work w = new Work(o.getString("file"), o.optString("title", UNTITLED), o.optString("author", UNKNOWN));
                if (new File(dir, w.file).isFile()) works.add(w);
            }
        } catch (JSONException e) {
            // A broken list starts over; the files stay where they are.
        }
    }

    private void save() {
        JSONArray a = new JSONArray();
        try {
            for (Work w : works) {
                a.put(new JSONObject().put("file", w.file).put("title", w.title).put("author", w.author));
            }
        } catch (JSONException e) {
            throw new IllegalStateException(e);
        }
        prefs.edit().putString(KEY_WORKS, a.toString()).apply();
    }

    private void pick() {
        if (importing) return;
        Intent pick = new Intent(MediaStore.ACTION_PICK_IMAGES);
        pick.setType("image/*");
        pick.putExtra(MediaStore.EXTRA_PICK_IMAGES_MAX, MediaStore.getPickImagesMaxLimit());
        try {
            startActivityForResult(pick, PICK);
        } catch (ActivityNotFoundException e) {
            Intent open = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            open.addCategory(Intent.CATEGORY_OPENABLE);
            open.setType("image/*");
            open.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            try {
                startActivityForResult(open, PICK);
            } catch (ActivityNotFoundException e2) {
                Toast.makeText(this, t("No picture picker on this phone", "Keine Bildauswahl auf diesem Telefon"), Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request != PICK || result != RESULT_OK || data == null) return;
        List<Uri> uris = new ArrayList<>();
        ClipData clip = data.getClipData();
        if (clip != null) {
            for (int i = 0; i < clip.getItemCount(); i++) uris.add(clip.getItemAt(i).getUri());
        } else if (data.getData() != null) {
            uris.add(data.getData());
        }
        if (uris.isEmpty()) return;

        importing = true;
        show();
        String author = prefs.getString(KEY_LAST_AUTHOR, UNKNOWN);
        new Thread(() -> {
            List<Work> added = new ArrayList<>();
            int failed = 0;
            long stamp = System.currentTimeMillis();
            for (int i = 0; i < uris.size(); i++) {
                String name = "w" + stamp + "_" + i + ".jpg";
                if (copy(uris.get(i), new File(dir, name))) {
                    added.add(new Work(name, UNTITLED, author));
                } else {
                    failed++;
                }
            }
            int failures = failed;
            runOnUiThread(() -> {
                importing = false;
                // Newest first, as on the Volkskunst site.
                works.addAll(0, added);
                save();
                if (isDestroyed()) return;
                index = 0;
                wall.forget();
                show();
                if (failures > 0) {
                    Toast.makeText(this, failures + t(" picture(s) could not be read", " Bild(er) ließen sich nicht lesen"), Toast.LENGTH_SHORT).show();
                }
                // A single new picture gets its caption right away.
                if (added.size() == 1) describe();
            });
        }, "volkskunst-import").start();
    }

    /** Copies a picked picture: upright, at most MAX_SIDE pixels on its long side, as JPEG. */
    private boolean copy(Uri uri, File out) {
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            try (InputStream in = getContentResolver().openInputStream(uri)) {
                BitmapFactory.decodeStream(in, null, bounds);
            }
            if (bounds.outWidth <= 0) return false;
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = 1;
            while (Math.max(bounds.outWidth, bounds.outHeight) / (opts.inSampleSize * 2) >= MAX_SIDE) {
                opts.inSampleSize *= 2;
            }
            Bitmap raw;
            try (InputStream in = getContentResolver().openInputStream(uri)) {
                raw = BitmapFactory.decodeStream(in, null, opts);
            }
            if (raw == null) return false;
            int rotation = 0;
            try (InputStream in = getContentResolver().openInputStream(uri)) {
                int o = new ExifInterface(in).getAttributeInt(ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL);
                rotation = o == ExifInterface.ORIENTATION_ROTATE_90 ? 90
                        : o == ExifInterface.ORIENTATION_ROTATE_180 ? 180
                        : o == ExifInterface.ORIENTATION_ROTATE_270 ? 270 : 0;
            } catch (IOException | RuntimeException e) {
                // No usable EXIF: keep it as decoded.
            }
            float scale = Math.min(1f, (float) MAX_SIDE / Math.max(raw.getWidth(), raw.getHeight()));
            Matrix m = new Matrix();
            m.postScale(scale, scale);
            m.postRotate(rotation);
            Bitmap upright = scale == 1f && rotation == 0 ? raw
                    : Bitmap.createBitmap(raw, 0, 0, raw.getWidth(), raw.getHeight(), m, true);
            try (OutputStream o = new FileOutputStream(out)) {
                upright.compress(Bitmap.CompressFormat.JPEG, 90, o);
            }
            if (upright != raw) upright.recycle();
            raw.recycle();
            return true;
        } catch (IOException | RuntimeException | OutOfMemoryError e) {
            out.delete();
            return false;
        }
    }

    private void describe() {
        if (works.isEmpty()) return;
        Work w = works.get(index);
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(22), dp(8), dp(22), 0);
        EditText title = field(t("Title", "Titel"), UNTITLED.equals(w.title) ? "" : w.title);
        EditText author = field(t("From", "Von"), UNKNOWN.equals(w.author) ? "" : w.author);
        form.addView(title);
        form.addView(author);
        StateMenu.dress(new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle(t("Label the work", "Beschriften"))
                .setView(form)
                .setPositiveButton(t("Save", "Speichern"), (d, which) -> {
                    String t = title.getText().toString().trim();
                    String a = author.getText().toString().trim();
                    w.title = t.isEmpty() ? UNTITLED : t;
                    w.author = a.isEmpty() ? UNKNOWN : a;
                    if (!a.isEmpty()) prefs.edit().putString(KEY_LAST_AUTHOR, a).apply();
                    save();
                    show();
                })
                .setNegativeButton(t("Cancel", "Abbrechen"), null)
                .show(), v);
    }

    /** A stored title or author; the stored placeholder shows in the chosen language. */
    private String shown(String value, String placeholder, String english) {
        return value.isEmpty() || placeholder.equals(value) ? t(english, placeholder) : value;
    }

    private EditText field(String hint, String value) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setText(value);
        e.setSingleLine(true);
        e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        return e;
    }

    private void remove() {
        if (works.isEmpty()) return;
        Work w = works.get(index);
        StateMenu.dress(new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle(t("Remove this work?", "Werk entfernen?"))
                .setMessage(t("Only the copy in Laibach is deleted. The photo stays in the phone's gallery.",
                        "Nur die Kopie in Laibach wird gelöscht. Das Foto in der Galerie des Telefons bleibt."))
                .setPositiveButton(t("Remove", "Entfernen"), (d, which) -> {
                    works.remove(index);
                    new File(dir, w.file).delete();
                    save();
                    wall.forget();
                    show();
                })
                .setNegativeButton(t("Cancel", "Abbrechen"), null)
                .show(), v);
    }

    // ---- The wall -----------------------------------------------------------

    /**
     * Draws the current picture and its neighbours side by side, each in a frame,
     * and follows the finger: swipe for the next or previous one, long-press to
     * write a caption.
     */
    private final class Wall extends View {
        private final Map<String, Bitmap> cache = new HashMap<>();
        private final Paint picture = new Paint(Paint.FILTER_BITMAP_FLAG);
        private final Paint fill = new Paint();
        private final RectF rect = new RectF();
        private final GestureDetector gestures;
        private final float frame = dp(10), gap = dp(3), margin = dp(26);
        private VelocityTracker velocity;
        private ValueAnimator settle;
        private float downX, offset;
        private int top, bottom;

        Wall(Context context) {
            super(context);
            ColorMatrix grey = new ColorMatrix();
            grey.setSaturation(0f);
            // A little extra contrast, like a photocopy.
            float c = 1.18f, t = (1 - c) * 127.5f;
            grey.postConcat(new ColorMatrix(new float[]{
                    c, 0, 0, 0, t,
                    0, c, 0, 0, t,
                    0, 0, c, 0, t,
                    0, 0, 0, 1, 0}));
            picture.setColorFilter(new ColorMatrixColorFilter(grey));
            gestures = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
                @Override
                public void onLongPress(MotionEvent e) {
                    if (Math.abs(offset) < dp(8)) describe();
                }
            });
        }

        void setBounds(int top, int bottom) {
            this.top = top;
            this.bottom = bottom;
            invalidate();
        }

        /** Drops cached pictures after the collection changed. */
        void forget() {
            for (Bitmap b : cache.values()) b.recycle();
            cache.clear();
            offset = 0;
        }

        private Bitmap bitmap(int i) {
            String file = works.get(i).file;
            Bitmap b = cache.get(file);
            if (b == null) {
                b = BitmapFactory.decodeFile(new File(dir, file).getPath());
                if (b != null) cache.put(file, b);
            }
            return b;
        }

        /** Keeps only the current picture and its two neighbours in memory. */
        private void trim() {
            List<String> keep = new ArrayList<>();
            for (int i = index - 1; i <= index + 1; i++) {
                if (i >= 0 && i < works.size()) keep.add(works.get(i).file);
            }
            cache.entrySet().removeIf(e -> {
                if (keep.contains(e.getKey())) return false;
                e.getValue().recycle();
                return true;
            });
        }

        @Override
        protected void onDraw(Canvas c) {
            if (works.isEmpty()) return;
            int w = getWidth();
            for (int i = index - 1; i <= index + 1; i++) {
                if (i < 0 || i >= works.size()) continue;
                float left = (i - index) * w + offset;
                if (left >= w || left <= -w) continue;
                Bitmap b = bitmap(i);
                if (b != null) drawFramed(c, b, left, w);
            }
        }

        private void drawFramed(Canvas c, Bitmap b, float left, int w) {
            float border = frame + gap;
            float availW = w - 2 * (margin + border);
            float availH = getHeight() - top - bottom - 2 * border;
            if (availW <= 0 || availH <= 0) return;
            float s = Math.min(availW / b.getWidth(), availH / b.getHeight());
            float pw = b.getWidth() * s, ph = b.getHeight() * s;
            float x = left + (w - pw) / 2f, y = top + (getHeight() - top - bottom - ph) / 2f;
            // Frame in ink, a thin line of background, then the picture.
            fill.setColor(v.ink);
            rect.set(x - border, y - border, x + pw + border, y + ph + border);
            c.drawRect(rect, fill);
            fill.setColor(v.bg);
            rect.set(x - gap, y - gap, x + pw + gap, y + ph + gap);
            c.drawRect(rect, fill);
            rect.set(x, y, x + pw, y + ph);
            c.drawBitmap(b, null, rect, picture);
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            if (works.isEmpty()) return false;
            gestures.onTouchEvent(e);
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    if (settle != null) settle.cancel();
                    velocity = VelocityTracker.obtain();
                    velocity.addMovement(e);
                    downX = e.getX() - offset;
                    return true;
                case MotionEvent.ACTION_MOVE: {
                    velocity.addMovement(e);
                    float dx = e.getX() - downX;
                    boolean edge = (dx > 0 && index == 0) || (dx < 0 && index == works.size() - 1);
                    offset = edge ? dx / 3 : dx;
                    invalidate();
                    return true;
                }
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL: {
                    velocity.addMovement(e);
                    velocity.computeCurrentVelocity(1000);
                    float vx = velocity.getXVelocity();
                    velocity.recycle();
                    velocity = null;
                    int w = getWidth();
                    boolean fling = Math.abs(vx) > dp(600);
                    int step = 0;
                    if ((offset < -w / 5f || (fling && vx < 0)) && index < works.size() - 1) step = 1;
                    if ((offset > w / 5f || (fling && vx > 0)) && index > 0) step = -1;
                    slide(step);
                    return true;
                }
                default:
                    return false;
            }
        }

        /** Animates to the next (1), previous (-1) or the same (0) picture. */
        private void slide(int step) {
            float target = -step * getWidth();
            settle = ValueAnimator.ofFloat(offset, target);
            settle.setDuration(220);
            settle.setInterpolator(new DecelerateInterpolator());
            settle.addUpdateListener(a -> {
                offset = (float) a.getAnimatedValue();
                invalidate();
            });
            settle.addListener(new AnimatorListenerAdapter() {
                private boolean cancelled;

                @Override
                public void onAnimationCancel(Animator animation) {
                    cancelled = true;
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    if (cancelled || step == 0) return;
                    index += step;
                    offset = 0;
                    trim();
                    show();
                }
            });
            settle.start();
        }

        @Override
        protected void onDetachedFromWindow() {
            if (settle != null) settle.cancel();
            forget();
            super.onDetachedFromWindow();
        }
    }
}
