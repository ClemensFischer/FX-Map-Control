/*
 * FX Map Control - https://github.com/ClemensFischer/FX-Map-Control
 * © 2020 Clemens Fischer
 */
package fxmapcontrol;

import javafx.geometry.Point2D;

/**
 * Web Mercator Projection.
 *
 * Longitude values are transformed to X values in meters by multiplying with WGS84_METERS_PER_DEGREE.
 * Latitude values in the interval [-MAX_LATITUDE .. MAX_LATITUDE] are transformed to Y values in meters in
 * the interval [-R*pi .. R*pi], R = WGS84_EQUATORIAL_RADIUS.
 *
 * See "Map Projections - A Working Manual" (https://pubs.usgs.gov/pp/1395/report.pdf), p.41-44.
 */
public class WebMercatorProjection extends MapProjection {

    public static final double MAX_LATITUDE = yToLatitude(180d);

    public WebMercatorProjection() {
        this("EPSG:3857");
    }

    public WebMercatorProjection(String crsId) {
        setCrsId(crsId);
    }

    @Override
    public final boolean isNormalCylindrical() {
        return true;
    }

    @Override
    public final boolean isWebMercator() {
        return true;
    }

    @Override
    public final double maxLatitude() {
        return MAX_LATITUDE;
    }

    @Override
    public Point2D getRelativeScale(Location location) {
        double k = 1d / Math.cos(location.getLatitude() * Math.PI / 180d); // p.44 (7-3)

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

        return Math.log(Math.tan(latitude * Math.PI / 360d + Math.PI / 4d)) / Math.PI * 180d;
    }

    public static double yToLatitude(double y) {
        return Math.atan(Math.sinh(y * Math.PI / 180d)) / Math.PI * 180d;
    }
}
