/*
 * FX Map Control - https://github.com/ClemensFischer/FX-Map-Control
 * © 2020 Clemens Fischer
 */
package fxmapcontrol;

import javafx.geometry.Point2D;

/**
 * Spherical Orthographic Projection.
 */
public class OrthographicProjection extends AzimuthalProjection {

    public OrthographicProjection() {
        this("AUTO2:42003");
    }

    public OrthographicProjection(String crsId) {
        setCrsId(crsId);
    }

    @Override
    public Point2D locationToMap(Location location) {
        if (location.equals(getCenter())) {
            return new Point2D(0d, 0d);
        }

        double lat0 = getCenter().getLatitude() * Math.PI / 180d;
        double lat = location.getLatitude() * Math.PI / 180d;
        double dLon = (location.getLongitude() - getCenter().getLongitude()) * Math.PI / 180d;

        return new Point2D(
                WGS84_EQUATORIAL_RADIUS * Math.cos(lat) * Math.sin(dLon),
                WGS84_EQUATORIAL_RADIUS * (Math.cos(lat0) * Math.sin(lat) - Math.sin(lat0) * Math.cos(lat) * Math.cos(dLon)));
    }

    @Override
    public Location mapToLocation(Point2D point) {
        double x = point.getX();
        double y = point.getY();

        if (x == 0d && y == 0d) {
            return new Location(getCenter().getLatitude(), getCenter().getLongitude());
        }

        x /= WGS84_EQUATORIAL_RADIUS;
        y /= WGS84_EQUATORIAL_RADIUS;
        double r2 = x * x + y * y;

        if (r2 > 1d) {
            return new Location(Double.NaN, Double.NaN);
        }

        double r = Math.sqrt(r2);
        double sinC = r;
        double cosC = Math.sqrt(1 - r2);

        double lat0 = getCenter().getLatitude() * Math.PI / 180d;
        double cosLat0 = Math.cos(lat0);
        double sinLat0 = Math.sin(lat0);

        return new Location(
                180d / Math.PI * Math.asin(cosC * sinLat0 + y * sinC * cosLat0 / r),
                180d / Math.PI * Math.atan2(x * sinC, r * cosC * cosLat0 - y * sinC * sinLat0) + getCenter().getLongitude());
    }
}
