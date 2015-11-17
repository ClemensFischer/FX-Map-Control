/*
 * FX Map Control - https://github.com/ClemensFischer/FX-Map-Control
 * © 2015 Clemens Fischer
 */
package fxmapcontrol;

import javafx.geometry.Point2D;

/**
 * Defines a normal cylindrical projection. Latitude and longitude values in degrees are transformed
 * to cartesian coordinates with origin at latitude = 0 and longitude = 0. Longitude values are
 * transformed identically to x values in the interval [-180 .. 180]. Latitude values in the
 * interval [-maxLatitude .. maxLatitude] are transformed to y values in the interval [-180 .. 180]
 * according to the actual projection, e.g. the Mercator projection.
 */
public abstract class MapTransform {

    public abstract double maxLatitude();

    public abstract double relativeScale(Location location);

    public abstract Location transform(Point2D point);

    public abstract Point2D transform(Location location);

    public Point2D transform(Location location, double referenceLongitude) {
        Point2D p = transform(location);
        double x = Location.normalizeLongitude(p.getX());

        if (x > referenceLongitude + 180d) {
            x -= 360d;
        } else if (x < referenceLongitude - 180d) {
            x += 360d;
        }

        return new Point2D(x, p.getY());
    }
}
