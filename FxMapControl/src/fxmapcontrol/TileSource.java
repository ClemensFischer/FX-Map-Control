/*
 * FX Map Control - https://github.com/ClemensFischer/FX-Map-Control
 * © 2015 Clemens Fischer
 */
package fxmapcontrol;

import java.util.Locale;
import javafx.geometry.Point2D;

/**
 * Provides the URI of a map tile.
 */
public class TileSource {

    private interface UriFormatter {

        String getUri(int x, int y, int z);
    }

    public static final int TILE_SIZE = 256;
    public static final double METERS_PER_DEGREE = 6378137d * Math.PI / 180d; // WGS 84 semi major axis

    private UriFormatter uriFormatter;
    private String uriFormat;

    public TileSource() {
    }

    public TileSource(String uriFormat) {
        setUriFormat(uriFormat);
    }

    public static TileSource valueOf(String uriFormat) {
        return new TileSource(uriFormat);
    }

    public final String getUriFormat() {
        return uriFormat;
    }

    public final void setUriFormat(String uriFormat) {
        if (uriFormat == null || uriFormat.isEmpty()) {
            throw new IllegalArgumentException("uriFormat must not be null or empty");
        }

        if (uriFormat.contains("{x}") && uriFormat.contains("{y}") && uriFormat.contains("{z}")) {
            if (uriFormat.contains("{c}")) {
                uriFormatter = (x, y, z) -> getOpenStreetMapUri(x, y, z);
            } else if (uriFormat.contains("{n}")) {
                uriFormatter = (x, y, z) -> getMapQuestUri(x, y, z);
            } else {
                uriFormatter = (x, y, z) -> getDefaultUri(x, y, z);
            }
        } else if (uriFormat.contains("{q}")) {
            uriFormatter = (x, y, z) -> getQuadKeyUri(x, y, z);

        } else if (uriFormat.contains("{W}") && uriFormat.contains("{S}")
                && uriFormat.contains("{E}") && uriFormat.contains("{N}")) {
            uriFormatter = (x, y, z) -> getBoundingBoxUri(x, y, z);

        } else if (uriFormat.contains("{w}") && uriFormat.contains("{s}")
                && uriFormat.contains("{e}") && uriFormat.contains("{n}")) {
            uriFormatter = (x, y, z) -> getLatLonBoundingBoxUri(x, y, z);

        } else {
            throw new IllegalArgumentException("Invalid uriFormat: " + uriFormat);
        }

        this.uriFormat = uriFormat;
    }

    public String getUri(int x, int y, int zoomLevel) {
        return uriFormatter != null
                ? uriFormatter.getUri(x, y, zoomLevel)
                : null;
    }

    private String getDefaultUri(int x, int y, int zoomLevel) {
        return uriFormat
                .replace("{x}", Integer.toString(x))
                .replace("{y}", Integer.toString(y))
                .replace("{z}", Integer.toString(zoomLevel));
    }

    private String getOpenStreetMapUri(int x, int y, int zoomLevel) {
        int hostIndex = (x + y) % 3;
        return uriFormat
                .replace("{c}", "abc".substring(hostIndex, hostIndex + 1))
                .replace("{x}", Integer.toString(x))
                .replace("{y}", Integer.toString(y))
                .replace("{z}", Integer.toString(zoomLevel));
    }

    private String getMapQuestUri(int x, int y, int zoomLevel) {
        int hostIndex = (x + y) % 4 + 1;
        return uriFormat
                .replace("{n}", Integer.toString(hostIndex))
                .replace("{x}", Integer.toString(x))
                .replace("{y}", Integer.toString(y))
                .replace("{z}", Integer.toString(zoomLevel));
    }

    private String getQuadKeyUri(int x, int y, int zoomLevel) {
        if (zoomLevel < 1) {
            return null;
        }

        char[] quadkey = new char[zoomLevel];

        for (int z = zoomLevel - 1; z >= 0; z--, x /= 2, y /= 2) {
            quadkey[z] = (char) ('0' + 2 * (y % 2) + (x % 2));
        }

        return uriFormat
                .replace("{i}", new String(quadkey, zoomLevel - 1, 1))
                .replace("{q}", new String(quadkey));
    }

    private String getBoundingBoxUri(int x, int y, int zoomLevel) {
        double n = (double) (1 << zoomLevel);
        double x1 = METERS_PER_DEGREE * ((double) x * 360d / n - 180d);
        double x2 = METERS_PER_DEGREE * ((double) (x + 1) * 360d / n - 180d);
        double y1 = METERS_PER_DEGREE * (180d - (double) (y + 1) * 360d / n);
        double y2 = METERS_PER_DEGREE * (180d - (double) y * 360d / n);
        return uriFormat
                .replace("{W}", String.format(Locale.ROOT, "%f", x1))
                .replace("{S}", String.format(Locale.ROOT, "%f", y1))
                .replace("{E}", String.format(Locale.ROOT, "%f", x2))
                .replace("{N}", String.format(Locale.ROOT, "%f", y2))
                .replace("{X}", Integer.toString(TILE_SIZE))
                .replace("{Y}", Integer.toString(TILE_SIZE));
    }

    private String getLatLonBoundingBoxUri(int x, int y, int zoomLevel) {
        double n = (double) (1 << zoomLevel);
        double x1 = (double) x * 360d / n - 180d;
        double x2 = (double) (x + 1) * 360d / n - 180d;
        double y1 = 180d - (double) (y + 1) * 360d / n;
        double y2 = 180d - (double) y * 360d / n;
        MercatorTransform t = new MercatorTransform();
        Location p1 = t.transform(new Point2D(x1, y1));
        Location p2 = t.transform(new Point2D(x2, y2));
        return uriFormat
                .replace("{w}", String.format(Locale.ROOT, "%f", p1.getLongitude()))
                .replace("{s}", String.format(Locale.ROOT, "%f", p1.getLatitude()))
                .replace("{e}", String.format(Locale.ROOT, "%f", p2.getLongitude()))
                .replace("{n}", String.format(Locale.ROOT, "%f", p2.getLatitude()))
                .replace("{X}", Integer.toString(TILE_SIZE))
                .replace("{Y}", Integer.toString(TILE_SIZE));
    }
}
