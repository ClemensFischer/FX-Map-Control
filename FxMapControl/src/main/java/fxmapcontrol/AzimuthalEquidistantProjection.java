/*
 * FX Map Control - https://github.com/ClemensFischer/FX-Map-Control
 * © 2017 Clemens Fischer
 */
package fxmapcontrol;

import javafx.geometry.Point2D;

/**
 * Transforms geographic coordinates to cartesian coordinates according to the Azimuthal Equidistant
 * Projection.
 */
public class AzimuthalEquidistantProjection extends AzimuthalProjection {

    public AzimuthalEquidistantProjection() {
        // No known standard or de-facto standard CRS ID
    }

    public AzimuthalEquidistantProjection(String crsId) {
        this.crsId = crsId;
    }

    @Override
    public Point2D locationToPoint(Location location) {
        if (location.equals(projectionCenter)) {
            return new Point2D(0d, 0d);
        }

        double[] azimuthDistance = getAzimuthDistance(projectionCenter, location);
        double azimuth = azimuthDistance[0];
        double distance = WGS84_EQUATORIAL_RADIUS * azimuthDistance[1];

        return new Point2D(distance * Math.sin(azimuth), distance * Math.cos(azimuth));
    }

    @Override
    public Location pointToLocation(Point2D point) {
        double x = point.getX();
        double y = point.getY();

        if (x == 0d && y == 0d) {
            return projectionCenter;
        }

        double azimuth = Math.atan2(x, y);
        double distance = Math.sqrt(x * x + y * y) / WGS84_EQUATORIAL_RADIUS;

        return getLocation(projectionCenter, azimuth, distance);
    }
}
