/*
 * FX Map Control - https://github.com/ClemensFischer/FX-Map-Control
 * © 2017 Clemens Fischer
 */
package fxmapcontrol;

import java.util.Locale;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.transform.Affine;
import javafx.scene.transform.NonInvertibleTransformException;

/**
 * Base class for azimuthal map projections.
 */
public abstract class AzimuthalProjection extends MapProjection {

    protected Location centerLocation = new Location(0d, 0d);
    protected double centerRadius = WGS84_EQUATORIAL_RADIUS;

    @Override
    public boolean isWebMercator() {
        return false;
    }

    @Override
    public boolean isNormalCylindrical() {
        return false;
    }

    @Override
    public double maxLatitude() {
        return 90d;
    }

    @Override
    public Point2D getMapScale(Location location) {
        return new Point2D(viewportScale, viewportScale);
    }

    @Override
    public Bounds boundingBoxToBounds(MapBoundingBox boundingBox) {
        if (!(boundingBox instanceof CenteredBoundingBox)) {
            return super.boundingBoxToBounds(boundingBox);
        }

        CenteredBoundingBox cbbox = (CenteredBoundingBox) boundingBox;
        Point2D center = locationToPoint(cbbox.getCenter());
        double width = cbbox.getWidth();
        double height = cbbox.getHeight();

        return new BoundingBox(center.getX() - width / 2d, center.getY() - height / 2d, width, height);
    }

    @Override
    public MapBoundingBox boundsToBoundingBox(Bounds bounds) {
        Location center = pointToLocation(new Point2D(
                bounds.getMinX() + bounds.getWidth() / 2d,
                bounds.getMinY() + bounds.getHeight() / 2d));

        return new CenteredBoundingBox(center, bounds.getWidth(), bounds.getHeight()); // width and height in meters
    }

    @Override
    public double getViewportScale(double zoomLevel) {
        return super.getViewportScale(zoomLevel) / METERS_PER_DEGREE;
    }

    @Override
    public void setViewportTransform(Location center, Point2D viewportCenter, double zoomLevel, double heading) {
        centerLocation = center;
        centerRadius = geocentricRadius(center.getLatitude());
        viewportScale = getViewportScale(zoomLevel);

        Affine transform = new Affine();
        transform.prependScale(viewportScale, -viewportScale);
        transform.prependRotation(heading);
        transform.prependTranslation(viewportCenter.getX(), viewportCenter.getY());
        viewportTransform.setToTransform(transform);

        try {
            transform.invert();
        } catch (NonInvertibleTransformException ex) {
            throw new RuntimeException(ex); // this will never happen
        }

        inverseViewportTransform.setToTransform(transform);
    }

    @Override
    public String wmsQueryParameters(MapBoundingBox boundingBox, String version) {
        Bounds bounds = boundingBoxToBounds(boundingBox);
        String crs = version.startsWith("1.1.") ? "SRS" : "CRS";

        return String.format(Locale.ROOT,
                "%s=%s,1,%f,%f&BBOX=%f,%f,%f,%f&WIDTH=%d&HEIGHT=%d",
                crs, crsId, centerLocation.getLongitude(), centerLocation.getLatitude(),
                bounds.getMinX(), bounds.getMinY(), bounds.getMaxX(), bounds.getMaxY(),
                (int) Math.round(viewportScale * bounds.getWidth()),
                (int) Math.round(viewportScale * bounds.getHeight()));
    }

    /**
     * Calculates the geocentric earth radius at the specified latitude, based on the specified
     * ellipsoid equatorial radius and flattening values.
     */
    public static double geocentricRadius(double latitude, double equatorialRadius, double flattening) {
        double a = equatorialRadius;
        double b = a * (1d - flattening);
        double aCosLat = a * Math.cos(latitude * Math.PI / 180d);
        double bSinLat = b * Math.sin(latitude * Math.PI / 180d);
        double aCosLat2 = aCosLat * aCosLat;
        double bSinLat2 = bSinLat * bSinLat;
        return Math.sqrt((a * a * aCosLat2 + b * b * bSinLat2) / (aCosLat2 + bSinLat2));
    }

    /**
     * Calculates the geocentric earth radius at the specified latitude, based on WGS84 values.
     */
    public static double geocentricRadius(double latitude) {
        return geocentricRadius(latitude, WGS84_EQUATORIAL_RADIUS, WGS84_FLATTENING);
    }

    /**
     * Calculates azimuth and distance in radians from location1 to location2.
     */
    public static double[] getAzimuthDistance(Location location1, Location location2) {
        double lat1 = location1.getLatitude() * Math.PI / 180d;
        double lon1 = location1.getLongitude() * Math.PI / 180d;
        double lat2 = location2.getLatitude() * Math.PI / 180d;
        double lon2 = location2.getLongitude() * Math.PI / 180d;
        double cosLat1 = Math.cos(lat1);
        double sinLat1 = Math.sin(lat1);
        double cosLat2 = Math.cos(lat2);
        double sinLat2 = Math.sin(lat2);
        double cosLon12 = Math.cos(lon2 - lon1);
        double sinLon12 = Math.sin(lon2 - lon1);
        double cosDistance = sinLat1 * sinLat2 + cosLat1 * cosLat2 * cosLon12;
        double azimuth = Math.atan2(sinLon12, cosLat1 * sinLat2 / cosLat2 - sinLat1 * cosLon12);
        double distance = Math.acos(Math.max(Math.min(cosDistance, 1d), -1d));

        return new double[]{azimuth, distance};
    }

    /**
     * Calculates the Location of the point given by azimuth and distance in radians from location.
     */
    public static Location getLocation(Location location, double azimuth, double distance) {
        double lat = location.getLatitude() * Math.PI / 180d;
        double sinDistance = Math.sin(distance);
        double cosDistance = Math.cos(distance);
        double cosAzimuth = Math.cos(azimuth);
        double sinAzimuth = Math.sin(azimuth);
        double cosLat1 = Math.cos(lat);
        double sinLat1 = Math.sin(lat);
        double sinLat2 = sinLat1 * cosDistance + cosLat1 * sinDistance * cosAzimuth;
        double lat2 = Math.asin(Math.max(Math.min(sinLat2, 1d), -1d));
        double dLon = Math.atan2(sinDistance * sinAzimuth, cosLat1 * cosDistance - sinLat1 * sinDistance * cosAzimuth);

        return new Location(180d / Math.PI * lat2, location.getLongitude() + 180d / Math.PI * dLon);
    }
}
