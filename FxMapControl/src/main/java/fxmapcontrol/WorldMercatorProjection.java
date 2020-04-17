/*
 * FX Map Control - https://github.com/ClemensFischer/FX-Map-Control
 * © 2020 Clemens Fischer
 */
package fxmapcontrol;

import javafx.geometry.Point2D;

/**
 * Elliptical Mercator Projection.
 *
 * See "Map Projections - A Working Manual" (https://pubs.usgs.gov/pp/1395/report.pdf), p.44-45.
 */
public class WorldMercatorProjection extends MapProjection {

    private static final double convergenceTolerance = 1e-6;
    private static final int maxIterations = 10;

    public static final double MAX_LATITUDE = yToLatitude(180d);

    public WorldMercatorProjection() {
        this("EPSG:3395");
    }

    public WorldMercatorProjection(String crsId) {
        setCrsId(crsId);
    }

    @Override
    public final boolean isNormalCylindrical() {
        return true;
    }

    @Override
    public final double maxLatitude() {
        return MAX_LATITUDE;
    }

    @Override
    public Point2D getRelativeScale(Location location) {
        double lat = location.getLatitude() * Math.PI / 180d;
        double eSinLat = WGS84_ECCENTRICITY * Math.sin(lat);
        double k = Math.sqrt(1d - eSinLat * eSinLat) / Math.cos(lat); // p.44 (7-8)

        return new Point2D(k, k);
    }

    @Override
    public Point2D locationToMap(Location location) {
        return new Point2D(
                WGS84_METERS_PER_DEGREE * location.getLongitude(),
                WGS84_METERS_PER_DEGREE * latitudeToY(location.getLatitude()));
    }

    @Override
    public Location mapToLocation(Point2D point) {
        return new Location(
                yToLatitude(point.getY() / WGS84_METERS_PER_DEGREE),
                point.getX() / WGS84_METERS_PER_DEGREE);
    }

    public static double latitudeToY(double latitude) {
        if (latitude <= -90d) {
            return Double.NEGATIVE_INFINITY;
        }

        if (latitude >= 90d) {
            return Double.POSITIVE_INFINITY;
        }

        double lat = latitude * Math.PI / 180d;

        return Math.log(Math.tan(lat / 2d + Math.PI / 4d) * conformalFactor(lat)) * 180d / Math.PI; // p.44 (7-7)
    }

    public static double yToLatitude(double y) {
        double t = Math.exp(-y * Math.PI / 180d); // p.44 (7-10)
        double lat = Math.PI / 2d - 2d * Math.atan(t); // p.44 (7-11)
        double relChange = 1d;

        for (int i = 0; i < maxIterations && relChange > convergenceTolerance; i++) {
            double newLat = Math.PI / 2d - 2d * Math.atan(t * conformalFactor(lat)); // p.44 (7-9)
            relChange = Math.abs(1d - newLat / lat);
            lat = newLat;
        }

        return lat * 180d / Math.PI;
    }

    private static double conformalFactor(double lat) {
        double eSinLat = WGS84_ECCENTRICITY * Math.sin(lat);

        return Math.pow((1d - eSinLat) / (1d + eSinLat), WGS84_ECCENTRICITY / 2d);
    }
}
