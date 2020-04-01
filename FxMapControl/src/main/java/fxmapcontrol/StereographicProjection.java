/*
 * FX Map Control - https://github.com/ClemensFischer/FX-Map-Control
 * © 2020 Clemens Fischer
 */
package fxmapcontrol;

import javafx.geometry.Point2D;

/**
 * Transforms geographic coordinates to cartesian coordinates according to the Stereographic Projection.
 */
public class StereographicProjection extends AzimuthalProjection {

    public StereographicProjection() {
        this("AUTO2:97002"); // GeoServer non-standard CRS ID
    }

    public StereographicProjection(String crsId) {
        setCrsId(crsId);
    }

    @Override
    public Point2D locationToMap(Location location) {
        if (location.equals(getCenter())) {
            return new Point2D(0d, 0d);
        }

        double[] azimuthDistance = getAzimuthDistance(getCenter(), location);
        double azimuth = azimuthDistance[0];
        double distance = azimuthDistance[1];
        double mapDistance = 2d * Wgs84EquatorialRadius * Math.tan(distance / 2d);

        return new Point2D(mapDistance * Math.sin(azimuth), mapDistance * Math.cos(azimuth));
    }

    @Override
    public Location mapToLocation(Point2D point) {
        double x = point.getX();
        double y = point.getY();

        if (x == 0d && y == 0d) {
            return getCenter();
        }

        double azimuth = Math.atan2(x, y);
        double mapDistance = Math.sqrt(x * x + y * y);
        double distance = 2d * Math.atan(mapDistance / (2d * Wgs84EquatorialRadius));

        return getLocation(getCenter(), azimuth, distance);
    }

}
