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
 * Defines a map projection between geographic coordinates and cartesian map coordinates and
 * viewport coordinates, i.e. pixels.
 */
public abstract class MapProjection {

    public static final double WGS84_EQUATORIAL_RADIUS = 6378137d;
    public static final double WGS84_FLATTENING = 1d / 298.257223563;

    public static final double METERS_PER_DEGREE = WGS84_EQUATORIAL_RADIUS * Math.PI / 180d;

    protected final Affine viewportTransform = new Affine();
    protected final Affine inverseViewportTransform = new Affine();
    protected double viewportScale;
    protected String crsId;

    /**
     * Gets the WMS 1.3.0 CRS Identifier.
     */
    public String getCrsId() {
        return crsId;
    }

    /**
     * Sets the WMS 1.3.0 CRS Identifier.
     */
    public void setCrsId(String crsId) {
        this.crsId = crsId;
    }

    /**
     * Gets the transformation from cartesian map coordinates to viewport coordinates.
     */
    public Affine getViewportTransform() {
        return viewportTransform;
    }

    /**
     * Gets the transformation from viewport coordinates to cartesian map coordinates.
     */
    public Affine getInverseViewportTransform() {
        return inverseViewportTransform;
    }
    
    /**
     * Indicates if this is a web mercator projection, i.e. compatible with TileLayer.
     */
    public abstract boolean isWebMercator();
    
    /**
     * Indicates if this is a normal cylindrical projection, i.e. compatible with MapGraticule.
     */
    public abstract boolean isNormalCylindrical();
    
    /**
     * Indicates if this is an azimuthal projection.
     */
    public abstract boolean isAzimuthal();

    /**
     * Gets the absolute value of the minimum and maximum latitude that can be transformed.
     */
    public abstract double maxLatitude();

    /**
     * Gets the map scale at the specified Location as viewport coordinate units per meter (px/m).
     */
    public abstract Point2D getMapScale(Location location);

    /**
     * Transforms a Location in geographic coordinates to a Point2D in cartesian map coordinates.
     */
    public abstract Point2D locationToPoint(Location location);

    /**
     * Transforms a Point2D in cartesian map coordinates to a Location in geographic coordinates.
     */
    public abstract Location pointToLocation(Point2D point);

    /**
     * Transforms a MapBoundingBox in geographic coordinates to Bounds in cartesian map coordinates.
     */
    public Bounds boundingBoxToBounds(MapBoundingBox boundingBox) {
        Point2D sw = locationToPoint(new Location(boundingBox.getSouth(), boundingBox.getWest()));
        Point2D ne = locationToPoint(new Location(boundingBox.getNorth(), boundingBox.getEast()));

        return new BoundingBox(sw.getX(), sw.getY(), ne.getX() - sw.getX(), ne.getY() - sw.getY());
    }

    /**
     * Transforms Bounds in cartesian map coordinates to a BoundingBox in geographic coordinates.
     */
    public MapBoundingBox boundsToBoundingBox(Bounds bounds) {
        Location sw = pointToLocation(new Point2D(bounds.getMinX(), bounds.getMinY()));
        Location ne = pointToLocation(new Point2D(bounds.getMaxX(), bounds.getMaxY()));

        return new MapBoundingBox(sw.getLatitude(), sw.getLongitude(), ne.getLatitude(), ne.getLongitude());
    }

    public Point2D locationToViewportPoint(Location location) {
        return viewportTransform.transform(locationToPoint(location));
    }

    public Location viewportPointToLocation(Point2D point) {
        return pointToLocation(inverseViewportTransform.transform(point));
    }

    public MapBoundingBox viewportBoundsToBoundingBox(Bounds bounds) {
        return boundsToBoundingBox(inverseViewportTransform.transform(bounds));
    }

    public double getViewportScale(double zoomLevel) {
        return Math.pow(2d, zoomLevel) * TileSource.TILE_SIZE / 360d;
    }

    public void setViewportTransform(Location projectionCenter, Location mapCenter, Point2D viewportCenter, double zoomLevel, double heading) {
        viewportScale = getViewportScale(zoomLevel);

        Point2D center = locationToPoint(mapCenter);

        Affine transform = new Affine();
        transform.prependTranslation(-center.getX(), -center.getY());
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

    public String wmsQueryParameters(MapBoundingBox boundingBox, boolean useSrs) {
        if (crsId == null || crsId.isEmpty()) {
            return null;
        }

        String format;

        if (useSrs) {
            format = "SRS=%s&BBOX=%f,%f,%f,%f&WIDTH=%d&HEIGHT=%d";
        } else if (crsId.equals("EPSG:4326")) {
            format = "CRS=%1$s&BBOX=%3$f,%2$f,%5$f,%4$f&WIDTH=%6$d&HEIGHT=%7$d";
        } else {
            format = "CRS=%s&BBOX=%f,%f,%f,%f&WIDTH=%d&HEIGHT=%d";
        }

        Bounds bounds = boundingBoxToBounds(boundingBox);

        return String.format(Locale.ROOT, format, crsId,
                bounds.getMinX(), bounds.getMinY(), bounds.getMaxX(), bounds.getMaxY(),
                (int) Math.round(viewportScale * bounds.getWidth()),
                (int) Math.round(viewportScale * bounds.getHeight()));
    }
}
