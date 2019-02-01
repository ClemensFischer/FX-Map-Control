/*
 * FX Map Control - https://github.com/ClemensFischer/FX-Map-Control
 * © 2019 Clemens Fischer
 */
package fxmapcontrol;

import static fxmapcontrol.AzimuthalProjection.getAzimuthDistance;
import javafx.geometry.Point2D;
import static fxmapcontrol.AzimuthalProjection.getLocation;

/**
 * Transforms geographic coordinates to cartesian coordinates according to the Gnomonic Projection.
 */
public class GnomonicProjection extends AzimuthalProjection {

    public GnomonicProjection() {
        this("AUTO2:97001"); // GeoServer non-standard CRS ID
    }

    public GnomonicProjection(String crsId) {
        this.crsId = crsId;
    }

    @Override
    public Point2D locationToPoint(Location location) {
        if (location.equals(projectionCenter)) {
            return new Point2D(0d, 0d);
        }

        double[] azimuthDistance = getAzimuthDistance(projectionCenter, location);
        double azimuth = azimuthDistance[0];
        double distance = azimuthDistance[1];
        double mapDistance = distance < Math.PI / 2d ? WGS84_EQUATORIAL_RADIUS * Math.tan(distance) : Double.POSITIVE_INFINITY;

        return new Point2D(mapDistance * Math.sin(azimuth), mapDistance * Math.cos(azimuth));
    }

    @Override
    public Location pointToLocation(Point2D point) {
        double x = point.getX();
        double y = point.getY();

        if (x == 0d && y == 0d) {
            return projectionCenter;
        }

        double azimuth = Math.atan2(x, y);
        double mapDistance = Math.sqrt(x * x + y * y);
        double distance = Math.atan(mapDistance / WGS84_EQUATORIAL_RADIUS);

        return getLocation(projectionCenter, azimuth, distance);
    }

}
