/*
 * FX Map Control - https://github.com/ClemensFischer/FX-Map-Control
 * © 2017 Clemens Fischer
 */
package fxmapcontrol;

import javafx.geometry.Point2D;

/**
 * Transforms geographic coordinates to cartesian coordinates according to the Equirectangular
 * Projection. Longitude and Latitude values are transformed identically to X and Y.
 */
public class EquirectangularProjection extends MapProjection {

    public EquirectangularProjection() {
        this("EPSG:4326");
    }

    public EquirectangularProjection(String crsId) {
        this.crsId = crsId;
    }

    @Override
    public boolean isWebMercator() {
        return false;
    }

    @Override
    public boolean isNormalCylindrical() {
        return true;
    }

    @Override
    public boolean isAzimuthal() {
        return false;
    }

    @Override
    public double maxLatitude() {
        return 90d;
    }

    @Override
    public Point2D getMapScale(Location location) {
        return new Point2D(
            viewportScale / (METERS_PER_DEGREE * Math.cos(location.getLatitude() * Math.PI / 180d)),
            viewportScale / METERS_PER_DEGREE);
    }

    @Override
    public Point2D locationToPoint(Location location) {
        return new Point2D(location.getLongitude(), location.getLatitude());
    }

    @Override
    public Location pointToLocation(Point2D point) {
        return new Location(point.getY(), point.getX());
    }
}
