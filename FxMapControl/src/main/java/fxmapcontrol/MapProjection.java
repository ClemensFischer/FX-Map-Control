/*
 * FX Map Control - https://github.com/ClemensFischer/FX-Map-Control
 * © 2020 Clemens Fischer
 */
package fxmapcontrol;

import java.util.Locale;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;

/**
 * Defines a map projection between geographic coordinates and cartesian map coordinates.
 */
public abstract class MapProjection {

    public static final double WGS84_EQUATORIAL_RADIUS = 6378137d;
    public static final double WGS84_FLATTENING = 1d / 298.257223563;
    public static final double WGS84_METERS_PER_DEGREE = WGS84_EQUATORIAL_RADIUS * Math.PI / 180d;
    public static final double WGS84_ECCENTRICITY = Math.sqrt((2d - WGS84_FLATTENING) * WGS84_FLATTENING);

    private String crsId = "";
    private Location center = new Location(0d, 0d);

    /**
     * Gets the WMS 1.3.0 CRS Identifier.
     */
    public final String getCrsId() {
        return crsId;
    }

    /**
     * Sets the WMS 1.3.0 CRS Identifier.
     */
    public final void setCrsId(String crsId) {
        this.crsId = crsId;
    }

    /**
     * Get the projection center for azimuthal projections.
     */
    public final Location getCenter() {
        return center;
    }

    /**
     * Set the projection center for azimuthal projections.
     */
    public final void setCenter(Location center) {
        this.center = center;
    }

    /**
     * Indicates if this is a normal cylindrical projection, i.e. compatible with MapGraticule.
     */
    public boolean isNormalCylindrical() {
        return false;
    }

    /**
     * Indicates if this is a web mercator projection, i.e. compatible with TileLayer.
     */
    public boolean isWebMercator() {
        return false;
    }

    /**
     * Gets the absolute value of the minimum and maximum latitude that can be transformed.
     */
    public double maxLatitude() {
        return 90d;
    }

    /**
     * Transforms a Location in geographic coordinates to a Point2D in cartesian map coordinates.
     */
    public abstract Point2D locationToMap(Location location);

    /**
     * Transforms a Point2D in cartesian map coordinates to a Location in geographic coordinates.
     */
    public abstract Location mapToLocation(Point2D point);

    /**
     * Transforms a MapBoundingBox in geographic coordinates to Bounds in cartesian map coordinates.
     */
    public Bounds boundingBoxToBounds(MapBoundingBox boundingBox) {
        Point2D sw = locationToMap(new Location(boundingBox.getSouth(), boundingBox.getWest()));
        Point2D ne = locationToMap(new Location(boundingBox.getNorth(), boundingBox.getEast()));

        return new BoundingBox(sw.getX(), sw.getY(), ne.getX() - sw.getX(), ne.getY() - sw.getY());
    }

    /**
     * Transforms Bounds in cartesian map coordinates to a BoundingBox in geographic coordinates.
     */
    public MapBoundingBox boundsToBoundingBox(Bounds bounds) {
        Location sw = mapToLocation(new Point2D(bounds.getMinX(), bounds.getMinY()));
        Location ne = mapToLocation(new Point2D(bounds.getMaxX(), bounds.getMaxY()));

        return new MapBoundingBox(sw.getLatitude(), sw.getLongitude(), ne.getLatitude(), ne.getLongitude());
    }

    /**
     * Gets the relative map scale at the specified Location.
     */
    public Point2D getRelativeScale(Location location) {
        return new Point2D(1d, 1d);
    }

    /**
     * Gets the CRS parameter value for a WMS GetMap request.
     */
    public String getCrsValue() {
        return crsId.startsWith("AUTO:") || crsId.startsWith("AUTO2:")
                ? String.format(Locale.ROOT, "%s,1,%f,%f", crsId, center.getLongitude(), center.getLatitude())
                : crsId;
    }

    /**
     * Gets the BBOX parameter value for a WMS GetMap request.
     */
    public String getBboxValue(Bounds bounds) {
        return String.format(Locale.ROOT,
                "%f,%f,%f,%f",
                bounds.getMinX(), bounds.getMinY(), bounds.getMaxX(), bounds.getMaxY());
    }
}
