/*
 * FX Map Control - https://github.com/ClemensFischer/FX-Map-Control
 * © 2020 Clemens Fischer
 */
package fxmapcontrol;

import java.util.List;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.Transition;
import javafx.beans.DefaultProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;
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
import javafx.util.Duration;

/**
 * The map control. Renders map content provided by one or more MapTileLayers. The visible map area is defined
 * by the center and zoomLevel properties. The map can be rotated by an angle that is given by the heading
 * property. MapBase can contain different child nodes, which typically implement the IMapNode interface.
 */
@DefaultProperty(value = "children")
public class MapBase extends Region implements IMapNode {

    private static final StyleablePropertyFactory<MapBase> propertyFactory
            = new StyleablePropertyFactory<>(Region.getClassCssMetaData());

    private static final CssMetaData<MapBase, Duration> imageFadeDurationCssMetaData
            = propertyFactory.createDurationCssMetaData("-fx-image-fade-duration", s -> MapBase.imageFadeDurationProperty);

    private static final CssMetaData<MapBase, Duration> transitionDurationCssMetaData
            = propertyFactory.createDurationCssMetaData("-fx-transition-duration", s -> MapBase.transitionDurationProperty);

    private static final StyleableObjectProperty<Duration> imageFadeDurationProperty
            = new SimpleStyleableObjectProperty<>(imageFadeDurationCssMetaData, null, "tileFadeDuration", Duration.seconds(0.1));

    private static final StyleableObjectProperty<Duration> transitionDurationProperty
            = new SimpleStyleableObjectProperty<>(transitionDurationCssMetaData, null, "transitionDuration", Duration.seconds(0.2));

    private final ObjectProperty<MapProjection> projectionProperty = new SimpleObjectProperty<>(this, "projection", new WebMercatorProjection());
    private final ObjectProperty<Location> projectionCenterProperty = new SimpleObjectProperty<>(this, "projectionCenter");
    private final ObjectProperty<Location> centerProperty = new SimpleObjectProperty<>(this, "center", new Location(0d, 0d));
    private final ObjectProperty<Location> targetCenterProperty = new SimpleObjectProperty<>(this, "targetCenter", new Location(0d, 0d));
    private final DoubleProperty zoomLevelProperty = new SimpleDoubleProperty(this, "zoomLevel");
    private final DoubleProperty targetZoomLevelProperty = new SimpleDoubleProperty(this, "targetZoomLevel");
    private final DoubleProperty headingProperty = new SimpleDoubleProperty(this, "heading");
    private final DoubleProperty targetHeadingProperty = new SimpleDoubleProperty(this, "targetHeading");
    private final ReadOnlyDoubleWrapper viewScaleProperty = new ReadOnlyDoubleWrapper(this, "viewScale");
    private final ViewTransform viewTransform = new ViewTransform();
    private final CenterTransition centerTransition = new CenterTransition();
    private final ZoomLevelTransition zoomLevelTransition = new ZoomLevelTransition();
    private final HeadingTransition headingTransition = new HeadingTransition();

    private Location transformCenter = new Location(0d, 0d);
    private Point2D viewCenter = new Point2D(0d, 0d);
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
            resetTransformCenter();
            updateTransform(false, false);
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

    public static final ObjectProperty<Duration> imageFadeDurationProperty() {
        return imageFadeDurationProperty;
    }

    public static final Duration getImageFadeDuration() {
        return imageFadeDurationProperty.get();
    }

    public static final void setImageFadeDuration(Duration imageFadeDuration) {
        imageFadeDurationProperty.set(imageFadeDuration);
    }

    public static final ObjectProperty<Duration> transitionDurationProperty() {
        return transitionDurationProperty;
    }

    public static final Duration getTransitionDuration() {
        return transitionDurationProperty.get();
    }

    public static final void setTransitionDuration(Duration transitionDuration) {
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

    public final ReadOnlyDoubleProperty viewScaleProperty() {
        return viewScaleProperty.getReadOnlyProperty();
    }

    public final double getViewScale() {
        return viewScaleProperty.get();
    }

    public final ViewTransform getViewTransform() {
        return viewTransform;
    }

    public final Point2D getScale(Location location) {
        return getProjection().getRelativeScale(location).multiply(viewTransform.getScale());
    }

    public final Point2D locationToView(Location location) {
        return viewTransform.mapToView(getProjection().locationToMap(location));
    }

    public final Location viewToLocation(Point2D point) {
        return getProjection().mapToLocation(viewTransform.viewToMap(point));
    }

    public MapBoundingBox viewBoundsToBoundingBox(Bounds bounds) {
        return getProjection().boundsToBoundingBox(viewTransform.viewToMap(bounds));
    }

    public final void setTransformCenter(Point2D center) {
        transformCenter = viewToLocation(center);
        viewCenter = center;
    }

    public final void resetTransformCenter() {
        transformCenter = null;
        viewCenter = new Point2D(getWidth() / 2d, getHeight() / 2d);
    }

    public final void translateMap(Point2D translation) {
        if (transformCenter != null) {
            resetTransformCenter();
        }

        if (translation.getX() != 0d || translation.getY() != 0d) {
            setCenter(viewToLocation(new Point2D(
                    getWidth() / 2d - translation.getX(),
                    getHeight() / 2d - translation.getY())));
        }
    }

    public final void zoomMap(Point2D center, double zoomLevel, boolean animated) {
        zoomLevel = Math.min(Math.max(zoomLevel, getMinZoomLevel()), getMaxZoomLevel());

        if (animated) {
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
            Point2D center = new Point2D(
                    bounds.getMinX() + bounds.getWidth() / 2d,
                    bounds.getMinY() + bounds.getHeight() / 2d);
            double scale = Math.min(getWidth() / bounds.getWidth(), getHeight() / bounds.getHeight());

            setTargetZoomLevel(ViewTransform.scaleToZoomLevel(scale));
            setTargetCenter(getProjection().mapToLocation(center));
            setTargetHeading(0d);
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

        double viewScale = ViewTransform.zoomLevelToScale(getZoomLevel());
        Location center = transformCenter != null ? transformCenter : getCenter();
        MapProjection projection = getProjection();

        projection.setCenter(getProjectionCenter() != null ? getProjectionCenter() : getCenter());

        viewTransform.setTransform(projection.locationToMap(center), viewCenter, viewScale, getHeading());

        if (transformCenter != null) {
            center = viewToLocation(new Point2D(getWidth() / 2d, getHeight() / 2d));

            double latitude = center.getLatitude();

            if (latitude < -projection.maxLatitude() || latitude > projection.maxLatitude()) {
                latitude = Math.min(Math.max(latitude, -projection.maxLatitude()), projection.maxLatitude());
                resetTransformCenter = true;
            }

            center = new Location(latitude, Location.normalizeLongitude(center.getLongitude()));

            internalUpdate = true;
            setCenter(center);
            if (!isCenterAnimationRunning()) {
                setTargetCenter(center);
            }
            internalUpdate = false;

            if (resetTransformCenter) {
                resetTransformCenter();

                projection.setCenter(getProjectionCenter() != null ? getProjectionCenter() : center);

                viewTransform.setTransform(projection.locationToMap(center), viewCenter, viewScale, getHeading());
            }
        }

        viewScaleProperty.set(viewScale);

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
