/*
 * FX Map Control - https://github.com/ClemensFischer/FX-Map-Control
 * © 2017 Clemens Fischer
 */
package fxmapcontrol;

import static fxmapcontrol.AzimuthalProjection.getAzimuthDistance;
import static fxmapcontrol.AzimuthalProjection.getlLocation;
import javafx.geometry.Point2D;

/**
 * Transforms geographic coordinates to cartesian coordinates according to the Stereographic
 * Projection.
 */
public class StereographicProjection extends AzimuthalProjection {

    public StereographicProjection() {
        this("AUTO2:97002");
    }

    public StereographicProjection(String crsId) {
        this.crsId = crsId;
    }

    @Override
    public Point2D locationToPoint(Location location) {
        double[] azimuthDistance = getAzimuthDistance(centerLocation, location);
        double azimuth = azimuthDistance[0];
        double distance = azimuthDistance[1];
        double mapDistance = 2d * centerRadius * Math.tan(distance / 2d);

        return new Point2D(mapDistance * Math.sin(azimuth), mapDistance * Math.cos(azimuth));
    }

    @Override
    public Location pointToLocation(Point2D point) {
        double x = point.getX();
        double y = point.getY();

        if (x == 0d && y == 0d) {
            return centerLocation;
        }

        double azimuth = Math.atan2(x, y);
        double mapDistance = Math.sqrt(x * x + y * y);
        double distance = 2d * Math.atan(mapDistance / (2d * centerRadius));

        return getlLocation(centerLocation, azimuth, distance);
    }

}
