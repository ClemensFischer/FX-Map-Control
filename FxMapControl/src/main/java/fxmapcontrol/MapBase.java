/*
 * FX Map Control - https://github.com/ClemensFischer/FX-Map-Control
 * © 2019 Clemens Fischer
 */
package fxmapcontrol;

import java.util.List;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.Transition;
import javafx.beans.DefaultProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.css.CssMetaData;
import javafx.css.SimpleStyleableObjectProperty;
import javafx.css.Styleable;
import javafx.css.StyleableObjectProperty;
import javafx.css.StyleablePropertyFactory;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.layout.Region;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.util.Duration;

/**
 * The map control. Renders map content provided by one or more MapTileLayers. The visible map area
 * is defined by the center and zoomLevel properties. The map can be rotated by an angle that is
 * given by the heading property. MapBase can contain different child nodes, which typically
 * implement the IMapNode interface.
 */
@DefaultProperty(value = "children")
public class MapBase extends Region implements IMapNode {

    private static final StyleablePropertyFactory<MapBase> propertyFactory
            = new StyleablePropertyFactory<>(Region.getClassCssMetaData());

    private static final CssMetaData<MapBase, Duration> tileFadeDurationCssMetaData
            = propertyFactory.createDurationCssMetaData("-fx-tile-fade-duration", s -> s.tileFadeDurationProperty);

    private static final CssMetaData<MapBase, Duration> transitionDurationCssMetaData
            = propertyFactory.createDurationCssMetaData("-fx-transition-duration", s -> s.transitionDurationProperty);

    private final StyleableObjectProperty<Duration> tileFadeDurationProperty
            = new SimpleStyleableObjectProperty<>(tileFadeDurationCssMetaData, this, "tileFadeDuration", Duration.seconds(0.2));

    private final StyleableObjectProperty<Duration> transitionDurationProperty
            = new SimpleStyleableObjectProperty<>(transitionDurationCssMetaData, this, "transitionDuration", Duration.seconds(0.3));

    private final ObjectProperty<MapProjection> projectionProperty = new SimpleObjectProperty<>(this, "projection", new WebMercatorProjection());
    private final ObjectProperty<Location> projectionCenterProperty = new SimpleObjectProperty<>(this, "projectionCenter");
    private final ObjectProperty<Location> centerProperty = new SimpleObjectProperty<>(this, "center", new Location(0d, 0d));
    private final ObjectProperty<Location> targetCenterProperty = new SimpleObjectProperty<>(this, "targetCenter", new Location(0d, 0d));
    private final DoubleProperty zoomLevelProperty = new SimpleDoubleProperty(this, "zoomLevel");
    private final DoubleProperty targetZoomLevelProperty = new SimpleDoubleProperty(this, "targetZoomLevel");
    private final DoubleProperty headingProperty = new SimpleDoubleProperty(this, "heading");
    private final DoubleProperty targetHeadingProperty = new SimpleDoubleProperty(this, "targetHeading");
    private final Scale scaleTransform = new Scale();
    private final Rotate rotateTransform = new Rotate();
    private final Affine scaleRotateTransform = new Affine();
    private final CenterTransition centerTransition = new CenterTransition();
    private final ZoomLevelTransition zoomLevelTransition = new ZoomLevelTransition();
    private final HeadingTransition headingTransition = new HeadingTransition();

    private Location transformCenter = new Location(0d, 0d);
    private Point2D viewportCenter = new Point2D(0d, 0d);
    private double centerLongitude;
    private double minZoomLevel = 1d;
    private double maxZoomLevel = 19d;
    private boolean internalUpdate;

    public MapBase() {
        getStyleClass().add("map-base");
        getChildren().addListener(new MapNodeHelper.ChildrenListener(this));

        Rectangle clip = new Rectangle(getWidth(), getHeight());
        setClip(clip);

        layoutBoundsProperty().addListener((observable, oldValue, newValue) -> {
            resetTransformCenter();
            updateTransform(false, false);
            clip.setWidth(newValue.getWidth());
            clip.setHeight(newValue.getHeight());
        });

        Tile.setFadeDuration(getTileFadeDuration());

        tileFadeDurationProperty.addListener((observable, oldValue, newValue) -> {
            Tile.setFadeDuration(newValue);
        });

        transitionDurationProperty.addListener((observable, oldValue, newValue) -> {
            centerTransition.setDuration(newValue);
            zoomLevelTransition.setDuration(newValue);
            headingTransition.setDuration(newValue);
        });

        projectionProperty.addListener((observable, oldValue, newValue) -> {
            resetTransformCenter();
            updateTransform(false, true);
        });

        projectionCenterProperty.addListener((observable, oldValue, newValue) -> {
            if (getProjection().isAzimuthal()) {
                resetTransformCenter();
                updateTransform(false, false);
            }
        });

        centerProperty.addListener((observable, oldValue, newValue) -> {
            if (!internalUpdate) {
                newValue = adjustCenterProperty(centerProperty, newValue);

                if (!isCenterAnimationRunning()) {
                    internalUpdate = true;
                    setTargetCenter(newValue);
                    internalUpdate = false;
                }

                resetTransformCenter();
                updateTransform(false, false);
            }
        });

        targetCenterProperty.addListener((observable, oldValue, newValue) -> {
            if (!internalUpdate) {
                centerTransition.start(getCenter(), adjustCenterProperty(targetCenterProperty, newValue));
            }
        });

        zoomLevelProperty.addListener((observable, oldValue, newValue) -> {
            if (!internalUpdate) {
                double value = adjustZoomLevelProperty(zoomLevelProperty, newValue.doubleValue());

                if (isZoomLevelAnimationRunning()) {
                    updateTransform(false, false);
                } else {
                    internalUpdate = true;
                    setTargetZoomLevel(value);
                    internalUpdate = false;
                    updateTransform(true, false);
                }
            }
        });

        targetZoomLevelProperty.addListener((observable, oldValue, newValue) -> {
            if (!internalUpdate) {
                zoomLevelTransition.start(getZoomLevel(), adjustZoomLevelProperty(targetZoomLevelProperty, newValue.doubleValue()));
            }
        });

        headingProperty.addListener((observable, oldValue, newValue) -> {
            if (!internalUpdate) {
                double value = adjustHeadingProperty(headingProperty, newValue.doubleValue());

                if (isHeadingAnimationRunning()) {
                    updateTransform(false, false);
                } else {
                    internalUpdate = true;
                    setTargetHeading(value);
                    internalUpdate = false;
                    updateTransform(true, false);
                }
            }
        });

        targetHeadingProperty.addListener((observable, oldValue, newValue) -> {
            if (!internalUpdate) {
                headingTransition.start(getHeading(), adjustHeadingProperty(targetHeadingProperty, newValue.doubleValue()));
            }
        });
    }

    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return propertyFactory.getCssMetaData();
    }

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return getClassCssMetaData();
    }

    @Override
    public final ObservableList<Node> getChildren() {
        return super.getChildren();
    }

    @Override
    public final MapBase getMap() {
        return this;
    }

    @Override
    public final void setMap(MapBase map) {
        throw new IllegalStateException();
    }

    public final ObjectProperty<Duration> tileFadeDurationProperty() {
        return tileFadeDurationProperty;
    }

    public final Duration getTileFadeDuration() {
        return tileFadeDurationProperty.get();
    }

    public final void setTileFadeDuration(Duration tileFadeDuration) {
        tileFadeDurationProperty.set(tileFadeDuration);
    }

    public final ObjectProperty<Duration> transitionDurationProperty() {
        return transitionDurationProperty;
    }

    public final Duration getTransitionDuration() {
        return transitionDurationProperty.get();
    }

    public final void setTransitionDuration(Duration transitionDuration) {
        transitionDurationProperty.set(transitionDuration);
    }

    public final ObjectProperty<MapProjection> projectionProperty() {
        return projectionProperty;
    }

    public final MapProjection getProjection() {
        return projectionProperty.get();
    }

    public final void setProjection(MapProjection projection) {
        projectionProperty.set(projection);
    }

    public final ObjectProperty<Location> projectionCenterProperty() {
        return projectionCenterProperty;
    }

    public final Location getProjectionCenter() {
        return projectionCenterProperty.get();
    }

    public final void setProjectionCenter(Location projectionCenter) {
        projectionCenterProperty.set(projectionCenter);
    }

    public final ObjectProperty<Location> centerProperty() {
        return centerProperty;
    }

    public final Location getCenter() {
        return centerProperty.get();
    }

    public final void setCenter(Location center) {
        centerProperty.set(center);
    }

    public final ObjectProperty<Location> targetCenterProperty() {
        return targetCenterProperty;
    }

    public final Location getTargetCenter() {
        return targetCenterProperty.get();
    }

    public final void setTargetCenter(Location targetCenter) {
        targetCenterProperty.set(targetCenter);
    }

    public final double getMinZoomLevel() {
        return minZoomLevel;
    }

    public final void setMinZoomLevel(double minZoomLevel) {
        this.minZoomLevel = minZoomLevel;
    }

    public final double getMaxZoomLevel() {
        return maxZoomLevel;
    }

    public final void setMaxZoomLevel(double maxZoomLevel) {
        this.maxZoomLevel = maxZoomLevel;
    }

    public final DoubleProperty zoomLevelProperty() {
        return zoomLevelProperty;
    }

    public final double getZoomLevel() {
        return zoomLevelProperty.get();
    }

    public final void setZoomLevel(double zoomLevel) {
        zoomLevelProperty.set(zoomLevel);
    }

    public final DoubleProperty targetZoomLevelProperty() {
        return targetZoomLevelProperty;
    }

    public final double getTargetZoomLevel() {
        return targetZoomLevelProperty.get();
    }

    public final void setTargetZoomLevel(double targetZoomLevel) {
        targetZoomLevelProperty.set(targetZoomLevel);
    }

    public final DoubleProperty headingProperty() {
        return headingProperty;
    }

    public final double getHeading() {
        return headingProperty.get();
    }

    public final void setHeading(double heading) {
        headingProperty.set(heading);
    }

    public final DoubleProperty targetHeadingProperty() {
        return targetHeadingProperty;
    }

    public final double getTargetHeading() {
        return targetHeadingProperty.get();
    }

    public final void setTargetHeading(double targetHeading) {
        targetHeadingProperty.set(targetHeading);
    }

    public final Scale getScaleTransform() {
        return scaleTransform;
    }

    public final Rotate getRotateTransform() {
        return rotateTransform;
    }

    public final Affine getScaleRotateTransform() {
        return scaleRotateTransform;
    }

    public final void setTransformCenter(Point2D center) {
        transformCenter = getProjection().viewportPointToLocation(center);
        viewportCenter = center;
    }

    public final void resetTransformCenter() {
        transformCenter = null;
        viewportCenter = new Point2D(getWidth() / 2d, getHeight() / 2d);
    }

    public final void translateMap(Point2D translation) {
        if (transformCenter != null) {
            resetTransformCenter();
        }

        if (translation.getX() != 0d || translation.getY() != 0d) {
            setCenter(getProjection().viewportPointToLocation(new Point2D(
                    getWidth() / 2d - translation.getX(),
                    getHeight() / 2d - translation.getY())));
        }
    }

    public final void zoomMap(Point2D center, double zoomLevel, boolean animated) {
        zoomLevel = Math.min(Math.max(zoomLevel, getMinZoomLevel()), getMaxZoomLevel());

        if (animated && getProjection().isNormalCylindrical()) {
            if (getTargetZoomLevel() != zoomLevel) {
                setTransformCenter(center);
                setTargetZoomLevel(zoomLevel);
            }
        } else if (getZoomLevel() != zoomLevel) {
            setTransformCenter(center);
            setZoomLevel(zoomLevel);
        }
    }

    public final void rotateMap(Point2D center, double heading, boolean animated) {
        if (animated) {
            if (getTargetHeading() != heading) {
                setTransformCenter(center);
                setTargetHeading(heading);
            }
        } else if (getHeading() != heading) {
            setTransformCenter(center);
            setHeading(heading);
        }
    }

    public final void zoomToBounds(MapBoundingBox boundingBox) {
        if (boundingBox != null && boundingBox.hasValidBounds()) {
            Bounds bounds = getProjection().boundingBoxToBounds(boundingBox);
            double scale0 = 1d / getProjection().getViewportScale(0d);
            double lonScale = scale0 * getWidth() / bounds.getWidth();
            double latScale = scale0 * getHeight() / bounds.getHeight();
            double lonZoom = Math.log(lonScale) / Math.log(2d);
            double latZoom = Math.log(latScale) / Math.log(2d);

            setTargetHeading(0d);
            setTargetZoomLevel(Math.min(lonZoom, latZoom));
            setTargetCenter(getProjection().pointToLocation(new Point2D(
                    bounds.getMinX() + bounds.getWidth() / 2d,
                    bounds.getMinY() + bounds.getHeight() / 2d)));
        }
    }

    public final boolean isCenterAnimationRunning() {
        return centerTransition.getStatus() == Animation.Status.RUNNING;
    }

    public final boolean isZoomLevelAnimationRunning() {
        return zoomLevelTransition.getStatus() == Animation.Status.RUNNING;
    }

    public final boolean isHeadingAnimationRunning() {
        return headingTransition.getStatus() == Animation.Status.RUNNING;
    }

    private Location adjustCenterProperty(ObjectProperty<Location> property, Location value) {
        double maxLatitude = getProjection().maxLatitude();
        internalUpdate = true;
        if (value == null) {
            value = new Location(0d, 0d);
            property.setValue(value);
        } else if (value.getLongitude() < -180d || value.getLongitude() > 180d
                || value.getLatitude() < -maxLatitude || value.getLatitude() > maxLatitude) {
            value = new Location(
                    Math.min(Math.max(value.getLatitude(), -maxLatitude), maxLatitude),
                    Location.normalizeLongitude(value.getLongitude()));
            property.setValue(value);
        }
        internalUpdate = false;
        return value;
    }

    private double adjustZoomLevelProperty(DoubleProperty property, double value) {
        if (value < minZoomLevel || value > maxZoomLevel) {
            internalUpdate = true;
            value = Math.min(Math.max(value, minZoomLevel), maxZoomLevel);
            property.set(value);
            internalUpdate = false;
        }
        return value;
    }

    private double adjustHeadingProperty(DoubleProperty property, double value) {
        if (value < 0d || value > 360d) {
            internalUpdate = true;
            value = ((value % 360d) + 360d) % 360d;
            property.set(value);
            internalUpdate = false;
        }
        return value;
    }

    private void updateTransform(boolean resetTransformCenter, boolean projectionChanged) {
        MapProjection projection = getProjection();
        Location center = transformCenter != null ? transformCenter : getCenter();

        projection.setViewportTransform(
                getProjectionCenter() != null ? getProjectionCenter() : getCenter(),
                center, viewportCenter, getZoomLevel(), getHeading());

        if (transformCenter != null) {
            center = projection.viewportPointToLocation(new Point2D(getWidth() / 2d, getHeight() / 2d));

            double latitude = center.getLatitude();

            if (latitude < -projection.maxLatitude() || latitude > projection.maxLatitude()) {
                latitude = Math.min(Math.max(latitude, -projection.maxLatitude()), projection.maxLatitude());
                resetTransformCenter = true;
            }

            center = new Location(latitude, Location.normalizeLongitude(center.getLongitude()));

            internalUpdate = true;
            setCenter(center);
            internalUpdate = false;

            if (resetTransformCenter) {
                resetTransformCenter();
                projection.setViewportTransform(
                        getProjectionCenter() != null ? getProjectionCenter() : center,
                        center, viewportCenter, getZoomLevel(), getHeading());
            }
        }

        Point2D scale = projection.getMapScale(center);
        scaleTransform.setX(scale.getX());
        scaleTransform.setY(scale.getY());
        rotateTransform.setAngle(getHeading());
        scaleRotateTransform.setToTransform(scaleTransform.createConcatenation(rotateTransform));

        fireEvent(new ViewportChangedEvent(this, projectionChanged, getCenter().getLongitude() - centerLongitude));

        centerLongitude = getCenter().getLongitude();
    }

    private abstract class TransitionBase<T> extends Transition {

        protected T fromValue;
        protected T toValue;

        public TransitionBase() {
            setDuration(getTransitionDuration());
            setInterpolator(Interpolator.EASE_OUT);
            setOnFinished(e -> updateTransform(true, false));
        }

        public final void setDuration(Duration duration) {
            setCycleDuration(duration);
        }

        protected final void start() {
            if (getCycleDuration().greaterThan(Duration.ZERO)) {
                playFromStart();
            } else {
                interpolate(1d);
            }
        }
    }

    private class CenterTransition extends TransitionBase<Location> {

        public void start(Location from, Location to) {
            if (!from.equals(to)) {
                double longitude = Location.normalizeLongitude(to.getLongitude());
                if (longitude > from.getLongitude() + 180d) {
                    longitude -= 360d;
                } else if (longitude < from.getLongitude() - 180d) {
                    longitude += 360d;
                }
                fromValue = from;
                toValue = new Location(to.getLatitude(), longitude);
                start();
            }
        }

        @Override
        protected void interpolate(double f) {
            setCenter(new Location(
                    (1d - f) * fromValue.getLatitude() + f * toValue.getLatitude(),
                    (1d - f) * fromValue.getLongitude() + f * toValue.getLongitude()));
        }
    }

    private class ZoomLevelTransition extends TransitionBase<Double> {

        public void start(double from, double to) {
            if (from != to) {
                fromValue = from;
                toValue = to;
                start();
            }
        }

        @Override
        protected void interpolate(double f) {
            setZoomLevel((1d - f) * fromValue + f * toValue);
        }
    };

    private class HeadingTransition extends TransitionBase<Double> {

        public void start(double from, double to) {
            if (from != to) {
                if (to - from > 180d) {
                    to -= 360d;
                } else if (to - from < -180d) {
                    to += 360d;
                }
                fromValue = from;
                toValue = to;
                start();
            }
        }

        @Override
        protected void interpolate(double f) {
            setHeading((1d - f) * fromValue + f * toValue);
        }
    };
}
