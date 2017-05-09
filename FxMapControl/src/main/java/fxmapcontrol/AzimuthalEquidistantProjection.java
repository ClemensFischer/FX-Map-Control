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
        this("AUTO2:99999");
    }

    public AzimuthalEquidistantProjection(String crsId) {
        this.crsId = crsId;
    }

    @Override
    public Point2D locationToPoint(Location location) {
        double[] azimuthDistance = getAzimuthDistance(centerLocation, location);
        double azimuth = azimuthDistance[0];
        double distance = centerRadius * azimuthDistance[1];

        return new Point2D(distance * Math.sin(azimuth), distance * Math.cos(azimuth));
    }

    @Override
    public Location pointToLocation(Point2D point) {
        double x = point.getX();
        double y = point.getY();

        if (x == 0d && y == 0d) {
            return centerLocation;
        }

        double azimuth = Math.atan2(x, y);
        double distance = Math.sqrt(x * x + y * y) / centerRadius;

        return getLocation(centerLocation, azimuth, distance);
    }
}
