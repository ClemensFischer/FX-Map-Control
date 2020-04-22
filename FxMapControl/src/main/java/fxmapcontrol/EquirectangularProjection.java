/*
 * FX Map Control - https://github.com/ClemensFischer/FX-Map-Control
 * © 2020 Clemens Fischer
 */
package fxmapcontrol;

import java.util.Locale;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;

/**
 * Equirectangular Projection.
 *
 * Longitude and Latitude values are transformed linearly to X and Y values in meters.
 */
public class EquirectangularProjection extends MapProjection {

    public EquirectangularProjection() {
        this("EPSG:4326");
    }

    public EquirectangularProjection(String crsId) {
        setCrsId(crsId);
    }

    @Override
    public final boolean isNormalCylindrical() {
        return true;
    }

    @Override
    public Point2D getRelativeScale(Location location) {
        return new Point2D(
                1d / Math.cos(location.getLatitude() * Math.PI / 180d),
                1d);
    }

    @Override
    public Point2D locationToMap(Location location) {
        return new Point2D(
                WGS84_METERS_PER_DEGREE * location.getLongitude(),
                WGS84_METERS_PER_DEGREE * location.getLatitude());
    }

    @Override
    public Location mapToLocation(Point2D point) {
        return new Location(
                point.getY() / WGS84_METERS_PER_DEGREE,
                point.getX() / WGS84_METERS_PER_DEGREE);
    }

    @Override
    public String getBboxValue(Bounds bounds) {
        return String.format(Locale.ROOT,
                getCrsId().equals("CRS:84") ? "%1$f,%2$f,%3$f,%4$f" : "%2$f,%1$f,%4$f,%3$f",
                bounds.getMinX() / WGS84_METERS_PER_DEGREE, bounds.getMinY() / WGS84_METERS_PER_DEGREE,
                bounds.getMaxX() / WGS84_METERS_PER_DEGREE, bounds.getMaxY() / WGS84_METERS_PER_DEGREE);
    }
}
