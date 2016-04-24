/*
 * FX Map Control - https://github.com/ClemensFischer/FX-Map-Control
 * © 2016 Clemens Fischer
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
public interface IMapTransform {

    double maxLatitude();
    double relativeScale(Location location);
    Location transform(Point2D point);
    Point2D transform(Location location);
}
