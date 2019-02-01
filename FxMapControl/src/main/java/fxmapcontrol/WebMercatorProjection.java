/*
 * FX Map Control - https://github.com/ClemensFischer/FX-Map-Control
 * © 2019 Clemens Fischer
 */
package fxmapcontrol;

import javafx.geometry.Point2D;

/**
 * Transforms geographic coordinates to cartesian coordinates according to the Web Mercator
 * Projection. Longitude values are transformed linearly to X values in meters, by multiplying with
 * METERS_PER_DEGREE. Latitude values in the interval [-maxLatitude .. maxLatitude] are transformed
 * to Y values in meters in the interval [-R*pi .. R*pi], R=WGS84_EQUATORIAL_RADIUS.
 */
public class WebMercatorProjection extends MapProjection {

    public static final double MAX_LATITUDE = yToLatitude(180.);

    public WebMercatorProjection() {
        this("EPSG:3857");
    }

    public WebMercatorProjection(String crsId) {
        this.crsId = crsId;
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
    public boolean isAzimuthal() {
        return false;
    }

    @Override
    public double maxLatitude() {
        return MAX_LATITUDE;
    }

    @Override
    public Point2D getMapScale(Location location) {
        double scale = viewportScale / Math.cos(location.getLatitude() * Math.PI / 180d);

        return new Point2D(scale, scale);
    }

    @Override
    public Point2D locationToPoint(Location location) {
        return new Point2D(
                METERS_PER_DEGREE * location.getLongitude(),
                METERS_PER_DEGREE * latitudeToY(location.getLatitude()));
    }

    @Override
    public Location pointToLocation(Point2D point) {
        return new Location(
                yToLatitude(point.getY() / METERS_PER_DEGREE),
                point.getX() / METERS_PER_DEGREE);
    }

    @Override
    public double getViewportScale(double zoomLevel) {
        return super.getViewportScale(zoomLevel) / METERS_PER_DEGREE;
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
