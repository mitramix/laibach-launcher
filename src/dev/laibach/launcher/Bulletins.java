package dev.laibach.launcher;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * News from the state, for the ticker and the home screen: mobilisation orders
 * (the band's future concerts, from laibach.org's own events feed) and the
 * latest transmission on Laibach TV. Received at most every three hours and
 * kept, so the ticker has news without the network. Only public listings are
 * read; nothing is sent but the requests themselves.
 */
final class Bulletins {
    private static final String PREFS = "bulletins";
    private static final String TOUR = "https://www.laibach.org/?json=1&count=100&post_type=wp_events_manager&future=true";
    private static final long FRESH = 3 * 60 * 60_000L;
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static boolean fetching;
    private static Bitmap still;
    private static String stillId;

    private Bulletins() {}

    /** One call-up: a concert, as the state sees it. */
    static final class Order {
        LocalDate date;
        String city, country, place, link;
        boolean soldOut;
    }

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** Receives the news if it is older than three hours; done (if any) runs on the main thread after. */
    static void refresh(Context context, Runnable done) {
        Context c = context.getApplicationContext();
        SharedPreferences p = prefs(c);
        long now = System.currentTimeMillis();
        boolean tour = now - p.getLong("tourAt", 0) > FRESH, tv = now - p.getLong("tvAt", 0) > FRESH;
        if (fetching || (!tour && !tv && (latestTv(c) == null || still != null))) return;
        fetching = true;
        new Thread(() -> {
            SharedPreferences.Editor e = p.edit();
            if (tour) {
                try {
                    String json = new String(BroadcastActivity.bytes(TOUR, 1_500_000), StandardCharsets.UTF_8);
                    e.putString("tour", compact(json)).putLong("tourAt", now);
                } catch (Exception ignored) {
                    // The old orders stand.
                }
            }
            if (tv) {
                try {
                    List<BroadcastActivity.Video> videos = BroadcastActivity.parse(new String(BroadcastActivity.bytes(
                            "https://www.youtube.com/feeds/videos.xml?channel_id=" + BroadcastActivity.CHANNEL, 2_000_000),
                            StandardCharsets.UTF_8));
                    if (!videos.isEmpty()) {
                        e.putString("tv", videos.get(0).id + "\t" + videos.get(0).title).putLong("tvAt", now);
                    }
                } catch (Exception ignored) {
                    // The old transmission stands.
                }
            }
            e.commit();
            String[] latest = latestTv(c);
            if (latest != null && !latest[0].equals(stillId)) {
                try {
                    byte[] b = BroadcastActivity.bytes("https://i.ytimg.com/vi/" + latest[0] + "/mqdefault.jpg", 500_000);
                    Bitmap bmp = BitmapFactory.decodeByteArray(b, 0, b.length);
                    if (bmp != null) {
                        still = bmp;
                        stillId = latest[0];
                    }
                } catch (Exception ignored) {
                    // No still; the strip shows the title alone.
                }
            }
            MAIN.post(() -> {
                fetching = false;
                if (done != null) done.run();
            });
        }, "bulletins").start();
    }

    /** Only the fields the orders need, so the kept copy stays small. */
    private static String compact(String json) throws Exception {
        JSONArray posts = new JSONObject(json).getJSONArray("posts");
        JSONArray out = new JSONArray();
        for (int i = 0; i < posts.length(); i++) {
            JSONObject f = posts.getJSONObject(i).optJSONObject("custom_fields");
            if (f == null) continue;
            JSONObject o = new JSONObject();
            o.put("date", first(f, "event_start_date"));
            o.put("city", first(f, "event_city"));
            String country = first(f, "event_conutry");
            o.put("country", country.isEmpty() ? first(f, "event_country") : country);
            o.put("place", first(f, "event_place"));
            o.put("link", first(f, "event_tickets_link"));
            o.put("soldOut", "1".equals(first(f, "sold_out")));
            out.put(o);
        }
        return out.toString();
    }

    private static String first(JSONObject f, String key) {
        JSONArray a = f.optJSONArray(key);
        return a == null || a.length() == 0 ? "" : a.optString(0, "").trim();
    }

    /** Orders from today on, soonest first. */
    static List<Order> orders(Context c) {
        List<Order> out = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(prefs(c).getString("tour", "[]"));
            LocalDate today = LocalDate.now();
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                Order order = new Order();
                try {
                    order.date = LocalDate.parse(o.getString("date"), DateTimeFormatter.BASIC_ISO_DATE);
                } catch (Exception e) {
                    continue;
                }
                if (order.date.isBefore(today)) continue;
                order.city = o.optString("city").toUpperCase(Locale.ROOT);
                order.country = o.optString("country").toUpperCase(Locale.ROOT);
                order.place = o.optString("place");
                order.link = o.optString("link");
                order.soldOut = o.optBoolean("soldOut");
                out.add(order);
            }
        } catch (Exception ignored) {
            // No orders.
        }
        out.sort((x, y) -> x.date.compareTo(y.date));
        return out;
    }

    /** The newest video on the channel as {id, title}, or null. */
    static String[] latestTv(Context c) {
        String[] f = prefs(c).getString("tv", "").split("\t", 2);
        return f.length == 2 ? f : null;
    }

    /** The newest video's still, once received in this process. */
    static Bitmap still(String id) {
        return id != null && id.equals(stillId) ? still : null;
    }
}
