/*
 * FX Map Control - https://github.com/ClemensFischer/FX-Map-Control
 * © 2020 Clemens Fischer
 */
package fxmapcontrol;

import java.util.Locale;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;

/**
 * Transforms geographic coordinates to cartesian coordinates according to the
 * Equirectangular Projection. Longitude and Latitude values are transformed
 * linearly to X and Y values in meters.
 */
public class EquirectangularProjection extends MapProjection {

    public EquirectangularProjection() {
        this("EPSG:4326");
    }

    public EquirectangularProjection(String crsId) {
        setCrsId(crsId);
    }

    @Override
    public final boolean isWebMercator() {
        return false;
    }

    @Override
    public final boolean isNormalCylindrical() {
        return true;
    }

    @Override
    public double maxLatitude() {
        return 90d;
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
                Wgs84MetersPerDegree * location.getLongitude(),
                Wgs84MetersPerDegree * location.getLatitude());
    }

    @Override
    public Location mapToLocation(Point2D point) {
        return new Location(
                point.getY() / Wgs84MetersPerDegree,
                point.getX() / Wgs84MetersPerDegree);
    }

    @Override
    public String getBboxValue(Bounds bounds) {
        return String.format(Locale.ROOT,
                getCrsId().equals("CRS:84") ? "%1$f,%2$f,%3$f,%4$f" : "%2$f,%1$f,%4$f,%3$f",
                bounds.getMinX() / Wgs84MetersPerDegree, bounds.getMinY() / Wgs84MetersPerDegree,
                bounds.getMaxX() / Wgs84MetersPerDegree, bounds.getMaxY() / Wgs84MetersPerDegree);
    }
}
