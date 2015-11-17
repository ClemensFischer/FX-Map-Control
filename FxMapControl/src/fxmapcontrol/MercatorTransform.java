/*
 * FX Map Control - https://github.com/ClemensFischer/FX-Map-Control
 * © 2015 Clemens Fischer
 */
package fxmapcontrol;

import javafx.geometry.Point2D;

/**
 * Transforms latitude and longitude values in degrees to cartesian coordinates according to the
 * Mercator projection.
 */
public class MercatorTransform extends MapTransform {

    private static final double maxLatitude = Math.atan(Math.sinh(Math.PI)) / Math.PI * 180.;

    @Override
    public double maxLatitude() {
        return maxLatitude;
    }

    @Override
    public double relativeScale(Location location) {
        if (location.getLatitude() <= -90.) {
            return Double.NEGATIVE_INFINITY;
        }
        if (location.getLatitude() >= 90.) {
            return Double.POSITIVE_INFINITY;
        }

        return 1d / Math.cos(location.getLatitude() * Math.PI / 180d);
    }

    @Override
    public Point2D transform(Location location) {
        double latitude;

        if (location.getLatitude() <= -90.) {
            latitude = Double.NEGATIVE_INFINITY;
        } else if (location.getLatitude() >= 90.) {
            latitude = Double.POSITIVE_INFINITY;
        } else {
            latitude = location.getLatitude() * Math.PI / 180.;
            latitude = Math.log(Math.tan(latitude) + 1d / Math.cos(latitude)) / Math.PI * 180.;
        }

        return new Point2D(location.getLongitude(), latitude);
    }

    @Override
    public Location transform(Point2D point) {
        double latitude = Math.atan(Math.sinh(point.getY() * Math.PI / 180.)) / Math.PI * 180.;

        return new Location(latitude, point.getX());
    }
}
