/*
 * FX Map Control - https://github.com/ClemensFischer/FX-Map-Control
 * © 2020 Clemens Fischer
 */
package fxmapcontrol;

import javafx.geometry.Point2D;

/**
 * Transforms geographic coordinates to cartesian coordinates according to the Gnomonic Projection.
 */
public class GnomonicProjection extends AzimuthalProjection {

    public GnomonicProjection() {
        this("AUTO2:97001"); // GeoServer non-standard CRS ID
    }

    public GnomonicProjection(String crsId) {
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
        double mapDistance = distance < Math.PI / 2d ? Wgs84EquatorialRadius * Math.tan(distance) : Double.POSITIVE_INFINITY;

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
        double distance = Math.atan(mapDistance / Wgs84EquatorialRadius);

        return getLocation(getCenter(), azimuth, distance);
    }

}
