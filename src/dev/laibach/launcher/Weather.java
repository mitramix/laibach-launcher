package dev.laibach.launcher;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * The state's weather, which is controlled: where the sun stands, worked out
 * from the clock and the time zone alone. The zone's standard offset gives
 * the meridian the state keeps (15° for every hour from Greenwich), and the
 * latitude is the capital's own, 46° north. No location is asked for or used.
 */
final class Weather {
    /** The capital's latitude, in degrees north. */
    static final double LATITUDE = 46.05;

    private Weather() {}

    /** The meridian the zone keeps: its standard offset, without summer time, at 15° an hour. */
    static double meridian(TimeZone zone) {
        return zone.getRawOffset() / 3_600_000.0 * 15;
    }

    /** The sun's height over the horizon at the given moment, in degrees; below zero it has set. */
    static double elevation(long millis, double latitude, double longitude) {
        double n = millis / 86_400_000.0 + 2440587.5 - 2451545.0;
        double mean = norm(280.460 + 0.9856474 * n);
        double anomaly = Math.toRadians(norm(357.528 + 0.9856003 * n));
        double ecliptic = Math.toRadians(mean + 1.915 * Math.sin(anomaly) + 0.020 * Math.sin(2 * anomaly));
        double tilt = Math.toRadians(23.439 - 0.0000004 * n);
        double ascension = Math.atan2(Math.cos(tilt) * Math.sin(ecliptic), Math.cos(ecliptic));
        double declination = Math.asin(Math.sin(tilt) * Math.sin(ecliptic));
        double sidereal = norm((18.697374558 + 24.06570982441908 * n) * 15 + longitude);
        double hour = Math.toRadians(sidereal) - ascension;
        double lat = Math.toRadians(latitude);
        return Math.toDegrees(Math.asin(Math.sin(lat) * Math.sin(declination)
                + Math.cos(lat) * Math.cos(declination) * Math.cos(hour)));
    }

    private static double norm(double degrees) {
        double d = degrees % 360;
        return d < 0 ? d + 360 : d;
    }

    /** The sun's height now, over the state's meridian for the phone's zone. */
    static double now() {
        return elevation(System.currentTimeMillis(), LATITUDE, meridian(TimeZone.getDefault()));
    }

    /**
     * The next time the sun crosses the horizon within a day, searched minute
     * by minute: a {@link Calendar} at that minute, or null in a polar day or
     * night. Rising is when the sun comes up through it.
     */
    static Calendar next(long from, double latitude, double longitude, boolean[] rising) {
        double before = elevation(from, latitude, longitude);
        for (int m = 1; m <= 24 * 60; m++) {
            long at = from + m * 60_000L;
            double e = elevation(at, latitude, longitude);
            // The sun's upper edge, bent by the air: it is up at -0.833°.
            if ((before < -0.833) != (e < -0.833)) {
                rising[0] = e > before;
                Calendar c = Calendar.getInstance();
                c.setTimeInMillis(at);
                return c;
            }
            before = e;
        }
        return null;
    }
}
