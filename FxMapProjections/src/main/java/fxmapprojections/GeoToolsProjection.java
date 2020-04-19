package fxmapprojections;

import fxmapcontrol.Location;
import fxmapcontrol.MapProjection;
import java.text.ParseException;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.operation.DefaultCoordinateOperationFactory;
import org.geotools.referencing.wkt.Parser;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.parameter.ParameterNotFoundException;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.opengis.referencing.operation.Projection;
import org.opengis.referencing.operation.TransformException;

public class GeoToolsProjection extends MapProjection {

    private CoordinateReferenceSystem coordinateReferenceSystem;
    private MathTransform locationToMapTransform;
    private MathTransform mapToLocationTransform;
    private boolean isNormalCylindrical;
    private boolean isWebMercator;
    private double scaleFactor;
    private String bboxFormat;

    @Override
    public boolean isNormalCylindrical() {
        return isNormalCylindrical;
    }

    @Override
    public boolean isWebMercator() {
        return isWebMercator;
    }

    @Override
    public double maxLatitude() {
        return 90d;
    }

    public CoordinateReferenceSystem getCRS() {
        return coordinateReferenceSystem;
    }

    public void setCRS(CoordinateReferenceSystem crs) {
        try {
            locationToMapTransform = new DefaultCoordinateOperationFactory()
                    .createOperation(DefaultGeographicCRS.WGS84, crs)
                    .getMathTransform();

            mapToLocationTransform = locationToMapTransform.inverse();

            String crsId = crs.getIdentifiers().stream()
                    .map(id -> id.toString())
                    .findFirst().orElse("");

            if (crs instanceof ProjectedCRS) {
                ProjectedCRS projCrs = (ProjectedCRS) crs;
                Projection proj = projCrs.getConversionFromBase();
                ParameterValueGroup parameters = proj.getParameterValues();

                double centralMeridian = Double.NaN;
                double centralParallel = Double.NaN;
                double falseEasting = Double.NaN;
                double falseNorthing = Double.NaN;

                try {
                    centralMeridian = parameters.parameter("central_meridian").doubleValue();
                } catch (ParameterNotFoundException ex) {
                    try {
                        centralMeridian = parameters.parameter("longitude_of_origin").doubleValue();
                    } catch (ParameterNotFoundException exc) {
                    }
                }

                try {
                    centralParallel = parameters.parameter("central_parallel").doubleValue();
                } catch (ParameterNotFoundException ex) {
                    try {
                        centralParallel = parameters.parameter("latitude_of_origin").doubleValue();
                    } catch (ParameterNotFoundException exc) {
                    }
                }

                try {
                    falseEasting = parameters.parameter("false_easting").doubleValue();
                } catch (ParameterNotFoundException ex) {
                }

                try {
                    falseNorthing = parameters.parameter("false_northing").doubleValue();
                } catch (ParameterNotFoundException ex) {
                }

                isNormalCylindrical = centralMeridian == 0d && centralParallel == 0d
                        && falseEasting == 0d && falseNorthing == 0d;
                isWebMercator = "EPSG:3857".equals(crsId) || "EPSG:900913".equals(crsId);
                scaleFactor = 1d;
                bboxFormat = "{0},{1},{2},{3}";

            } else {
                isNormalCylindrical = true;
                isWebMercator = false;
                scaleFactor = WGS84_METERS_PER_DEGREE;
                bboxFormat = "{1},{0},{3},{2}";
            }

            coordinateReferenceSystem = crs;
            setCrsId(crsId);

        } catch (FactoryException | NoninvertibleTransformException ex) {
            Logger.getLogger(GeoToolsProjection.class.getName()).log(Level.SEVERE, ex.getMessage());
        }
    }

    public String getWKT() {
        if (coordinateReferenceSystem == null) {
            throw new IllegalStateException("The CRS property is not set.");
        }

        return coordinateReferenceSystem.toWKT();
    }

    public void setWKT(String wkt) {
        try {
            setCRS(new Parser().parseCoordinateReferenceSystem(wkt));
        } catch (ParseException ex) {
            Logger.getLogger(GeoToolsProjection.class.getName()).log(Level.SEVERE, ex.getMessage());
        }
    }

    @Override
    public Point2D locationToMap(Location location) {
        if (locationToMapTransform == null) {
            throw new IllegalStateException("The CRS property is not set.");
        }

        try {
            DirectPosition2D pos = new DirectPosition2D();
            DirectPosition2D loc = new DirectPosition2D(
                    location.getLongitude(), location.getLatitude());

            locationToMapTransform.transform(loc, pos);

            return new Point2D(pos.x * scaleFactor, pos.y * scaleFactor);

        } catch (MismatchedDimensionException | TransformException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Override
    public Location mapToLocation(Point2D point) {
        if (mapToLocationTransform == null) {
            throw new IllegalStateException("The CRS property is not set.");
        }

        try {
            DirectPosition2D loc = new DirectPosition2D();
            DirectPosition2D pos = new DirectPosition2D(
                    point.getX() / scaleFactor, point.getY() / scaleFactor);

            mapToLocationTransform.transform(pos, loc);

            return new Location(loc.y, loc.x);

        } catch (MismatchedDimensionException | TransformException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Override
    public String getBboxValue(Bounds bounds) {
        return String.format(Locale.ROOT, bboxFormat,
                bounds.getMinX() / scaleFactor, bounds.getMinY() / scaleFactor,
                bounds.getMaxX() / scaleFactor, bounds.getMaxY() / scaleFactor);
    }
}
