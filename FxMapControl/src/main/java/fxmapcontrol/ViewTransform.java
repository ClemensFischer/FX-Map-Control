package fxmapcontrol;

import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.transform.Affine;
import javafx.scene.transform.NonInvertibleTransformException;

public class ViewTransform {

    public static double zoomLevelToScale(double zoomLevel) {
        return 256d * Math.pow(2d, zoomLevel) / (360d * MapProjection.Wgs84MetersPerDegree);
    }

    public static double scaleToZoomLevel(double scale) {
        return Math.log(scale * 360d * MapProjection.Wgs84MetersPerDegree / 256d) / Math.log(2d);
    }

    private double scale;
    private double rotation;
    private final Affine mapToViewTransform = new Affine();
    private final Affine viewToMapTransform = new Affine();

    public final double getScale() {
        return scale;
    }

    public final double getRotation() {
        return rotation;
    }
    
    public final Affine getMapToViewTransform() {
        return mapToViewTransform;
    }
    
    public final Affine getViewToMapTransform() {
        return viewToMapTransform;
    }

    public final Point2D mapToView(Point2D point) {
        return mapToViewTransform.transform(point);
    }

    public final Point2D viewToMap(Point2D point) {
        return viewToMapTransform.transform(point);
    }

    public final Bounds viewToMap(Bounds bounds) {
        return viewToMapTransform.transform(bounds);
    }

    public final void setTransform(Point2D mapCenter, Point2D viewCenter, double scale, double rotation) {
        this.scale = scale;
        this.rotation = rotation;

        Affine transform = new Affine();
        transform.prependTranslation(-mapCenter.getX(), -mapCenter.getY());
        transform.prependScale(scale, -scale);
        transform.prependRotation(rotation);
        transform.prependTranslation(viewCenter.getX(), viewCenter.getY());
        mapToViewTransform.setToTransform(transform);

        try {
            transform.invert();
        } catch (NonInvertibleTransformException ex) {
            throw new RuntimeException(ex); // this will never happen
        }

        viewToMapTransform.setToTransform(transform);
    }

    public final Affine getTileLayerTransform(double tileMatrixScale, Point2D tileMatrixTopLeft, Point2D tileMatrixOrigin) {
        Affine transform = new Affine();
        double transformScale = scale / tileMatrixScale;

        transform.prependScale(transformScale, transformScale);
        transform.prependRotation(rotation);

        // tile matrix origin in map coordinates
        //
        Point2D mapOrigin = new Point2D(
                tileMatrixTopLeft.getX() + tileMatrixOrigin.getX() / tileMatrixScale,
                tileMatrixTopLeft.getY() - tileMatrixOrigin.getY() / tileMatrixScale);

        // tile matrix origin in view coordinates
        //
        Point2D viewOrigin = mapToView(mapOrigin);

        transform.prependTranslation(viewOrigin.getX(), viewOrigin.getY());

        return transform;
    }

    public final Bounds getTileMatrixBounds(double tileMatrixScale, Point2D tileMatrixTopLeft, double viewWidth, double viewHeight) {
        Affine transform = new Affine();
        double transformScale = tileMatrixScale / scale;

        transform.prependScale(transformScale, transformScale);
        transform.prependRotation(-rotation);

        // view origin in map coordinates
        //
        Point2D origin = viewToMap(new Point2D(0d, 0d));

        // translate origin to tile matrix origin in pixels
        //
        transform.prependTranslation(
                tileMatrixScale * (origin.getX() - tileMatrixTopLeft.getX()),
                tileMatrixScale * (tileMatrixTopLeft.getY() - origin.getY()));

        // transform view bounds to tile pixel bounds
        //
        return transform.transform(new BoundingBox(0d, 0d, viewWidth, viewHeight));
    }
}
