package dev.laibach.launcher;

import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

/**
 * Quick-settings tiles for the state's switches, so they can be thrown from
 * the notification shade: the curfew, the pacified display and the dressed
 * lock screen. Each one is added by the owner from the shade's edit view.
 */
public final class Tiles {
    private Tiles() {}

    private static void show(TileService s, boolean on, String subtitle) {
        Tile tile = s.getQsTile();
        if (tile == null) return;
        tile.setState(on ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.setSubtitle(subtitle);
        tile.updateTile();
    }

    private static String t(TileService s, String en, String de) {
        return Lang.german(s) ? de : Lang.en(en);
    }

    /** The curfew, 23:00 to 06:00. */
    public static final class Curfew extends TileService {
        @Override
        public void onStartListening() {
            boolean on = Citizen.curfewOn(this);
            show(this, on, on ? "23:00–06:00" : t(this, "SUSPENDED", "AUSGESETZT"));
        }

        @Override
        public void onClick() {
            boolean on = !Citizen.curfewOn(this);
            Citizen.setCurfewOn(this, on);
            Record.note(this, on ? "CURFEW REINSTATED FROM THE SHADE." : "CURFEW SUSPENDED FROM THE SHADE.",
                    on ? "AUSGANGSSPERRE AUS DER LEISTE WIEDER EINGESETZT." : "AUSGANGSSPERRE AUS DER LEISTE AUSGESETZT.");
            onStartListening();
        }
    }

    /** The display without motion. */
    public static final class Pacify extends TileService {
        @Override
        public void onStartListening() {
            boolean on = Fx.pacified(this);
            show(this, on, on ? t(this, "STILL", "STILL") : t(this, "IN MOTION", "IN BEWEGUNG"));
        }

        @Override
        public void onClick() {
            Fx.setPacified(this, !Fx.pacified(this));
            onStartListening();
        }
    }

    /** The lock screen in the uniform. */
    public static final class Dress extends TileService {
        @Override
        public void onStartListening() {
            boolean on = LockWallpaper.dressed(this);
            show(this, on, on ? Variant.current(this).name : t(this, "PLAIN", "SCHLICHT"));
        }

        @Override
        public void onClick() {
            if (LockWallpaper.dressed(this)) {
                LockWallpaper.undress(this);
                onStartListening();
            } else {
                Tile tile = getQsTile();
                if (tile != null) {
                    tile.setSubtitle(t(this, "DRESSING ...", "WIRD EINGEKLEIDET ..."));
                    tile.updateTile();
                }
                LockWallpaper.dress(this, Variant.current(this), Lang.german(this), ok -> onStartListening());
            }
        }
    }
}
