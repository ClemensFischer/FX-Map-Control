/*
 * FX Map Control - https://github.com/ClemensFischer/FX-Map-Control
 * © 2020 Clemens Fischer
 */
package fxmapcontrol;

import javafx.geometry.Point2D;

/**
 * Transforms geographic coordinates to cartesian coordinates according to the Web Mercator Projection.
 *
 * Longitude values are transformed linearly to X values in meters, by multiplying with Wgs84MetersPerDegree.
 * Latitude values in the interval [-maxLatitude .. maxLatitude] are transformed to Y values in meters in the
 * interval [-R*pi .. R*pi], R=Wgs84EquatorialRadius.
 *
 * See "Map Projections - A Working Manual" (https://pubs.usgs.gov/pp/1395/report.pdf), p.41-44.
 */
public class WebMercatorProjection extends MapProjection {

    public static final double MaxLatitude = yToLatitude(180.);

    public WebMercatorProjection() {
        this("EPSG:3857");
    }

    public WebMercatorProjection(String crsId) {
        setCrsId(crsId);
    }

    @Override
    public boolean isWebMercator() {
        return true;
    }

    @Override
    public boolean isNormalCylindrical() {
        return true;
    }

    @Override
    public double maxLatitude() {
        return MaxLatitude;
    }

    @Override
    public Point2D getRelativeScale(Location location) {
        double k = 1d / Math.cos(location.getLatitude() * Math.PI / 180d); // p.44 (7-3)

        return new Point2D(k, k);
    }

    @Override
    public Point2D locationToMap(Location location) {
        return new Point2D(
                Wgs84MetersPerDegree * location.getLongitude(),
                Wgs84MetersPerDegree * latitudeToY(location.getLatitude()));
    }

    @Override
    public Location mapToLocation(Point2D point) {
        return new Location(
                yToLatitude(point.getY() / Wgs84MetersPerDegree),
                point.getX() / Wgs84MetersPerDegree);
    }

    public static double latitudeToY(double latitude) {
        return latitude <= -90 ? Double.NEGATIVE_INFINITY
                : latitude >= 90 ? Double.POSITIVE_INFINITY
                        : Math.log(Math.tan(latitude * Math.PI / 360 + Math.PI / 4)) / Math.PI * 180;
    }

    public static double yToLatitude(double y) {
        return Math.atan(Math.sinh(y * Math.PI / 180)) / Math.PI * 180;
    }
}
