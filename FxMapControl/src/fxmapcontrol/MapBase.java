/*
 * FX Map Control - https://github.com/ClemensFischer/FX-Map-Control
 * © 2015 Clemens Fischer
 */
package fxmapcontrol;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
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
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.css.CssMetaData;
import javafx.css.SimpleStyleableObjectProperty;
import javafx.css.Styleable;
import javafx.css.StyleableObjectProperty;
import javafx.css.StyleablePropertyFactory;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.layout.Region;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Affine;
import javafx.scene.transform.NonInvertibleTransformException;
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
            = new SimpleStyleableObjectProperty<>(tileFadeDurationCssMetaData, this, "tileFadeDuration", Duration.seconds(0.3));

    private final StyleableObjectProperty<Duration> transitionDurationProperty
            = new SimpleStyleableObjectProperty<>(transitionDurationCssMetaData, this, "transitionDuration", Duration.seconds(0.3));

    private final ObjectProperty<Location> centerProperty = new SimpleObjectProperty<>(this, "center", new Location(0d, 0d));
    private final ObjectProperty<Location> targetCenterProperty = new SimpleObjectProperty<>(this, "targetCenter", new Location(0d, 0d));
    private final DoubleProperty zoomLevelProperty = new SimpleDoubleProperty(this, "zoomLevel");
    private final DoubleProperty targetZoomLevelProperty = new SimpleDoubleProperty(this, "targetZoomLevel");
    private final DoubleProperty headingProperty = new SimpleDoubleProperty(this, "heading");
    private final DoubleProperty targetHeadingProperty = new SimpleDoubleProperty(this, "targetHeading");
    private final ReadOnlyDoubleWrapper centerScaleProperty = new ReadOnlyDoubleWrapper(this, "centerScale");

    private final CenterTransition centerTransition = new CenterTransition();
    private final ZoomLevelTransition zoomLevelTransition = new ZoomLevelTransition();
    private final HeadingTransition headingTransition = new HeadingTransition();
    private final MapTransform mapTransform = new MercatorTransform();
    private final Affine viewportTransform = new Affine();

    private double minZoomLevel = 1d;
    private double maxZoomLevel = 19d;
    private boolean internalUpdate;
    private Location transformOrigin;
    private Point2D mapOrigin = new Point2D(0d, 0d);
    private Point2D viewportOrigin = new Point2D(0d, 0d);
    private double viewportScale;

    public MapBase() {
        getStyleClass().add("map-base");
        getChildren().addListener(new MapNodeHelper.ChildrenListener(this));

        final Rectangle clip = new Rectangle(getWidth(), getHeight());
        setClip(clip);

        layoutBoundsProperty().addListener((observable, oldValue, newValue) -> {
            resetTransformOrigin();
            updateTransform(false);
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

        centerProperty.addListener(new CenterChangeListener());
        targetCenterProperty.addListener(new TargetCenterChangeListener());
        zoomLevelProperty.addListener(new ZoomLevelChangeListener());
        targetZoomLevelProperty.addListener(new TargetZoomLevelChangeListener());
        headingProperty.addListener(new HeadingChangeListener());
        targetHeadingProperty.addListener(new TargetHeadingChangeListener());
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

    public final ReadOnlyDoubleProperty centerScaleProperty() {
        return centerScaleProperty.getReadOnlyProperty();
    }

    public final double getCenterScale() {
        return centerScaleProperty.get();
    }

    public final double getViewportScale() {
        return viewportScale;
    }

    public final MapTransform getMapTransform() {
        return mapTransform;
    }

    public final Affine getViewportTransform() {
        return viewportTransform;
    }

    public final Point2D getMapOrigin() {
        return mapOrigin;
    }

    public final Point2D getViewportOrigin() {
        return viewportOrigin;
    }

    public final Point2D locationToViewportPoint(Location location) {
        return viewportTransform.transform(mapTransform.transform(location));
    }

    public final Location viewportPointToLocation(Point2D point) {
        try {
            return mapTransform.transform(viewportTransform.inverseTransform(point));
        } catch (NonInvertibleTransformException ex) {
            Logger.getLogger(MapBase.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    public final void translateMap(Point2D translation) {
        if (transformOrigin != null) {
            resetTransformOrigin();
        }

        if (translation.getX() != 0d || translation.getY() != 0d) {
            setCenter(viewportPointToLocation(new Point2D(
                    viewportOrigin.getX() - translation.getX(),
                    viewportOrigin.getY() - translation.getY())));
        }
    }

    public final void zoomMap(Point2D origin, double zoomLevel, boolean animated) {
        setTransformOrigin(origin);
        zoomLevel = Math.min(Math.max(zoomLevel, getMinZoomLevel()), getMaxZoomLevel());
        if (animated) {
            setTargetZoomLevel(zoomLevel);
        } else {
            setZoomLevel(zoomLevel);
        }
    }

    public final void rotateMap(Point2D origin, double heading, boolean animated) {
        setTransformOrigin(origin);
        if (animated) {
            setTargetHeading(heading);
        } else {
            setHeading(heading);
        }
    }

    public final void setTransformOrigin(Location origin) {
        transformOrigin = origin;
        viewportOrigin = locationToViewportPoint(origin);
    }

    public final void setTransformOrigin(Point2D origin) {
        viewportOrigin = new Point2D(
                Math.min(Math.max(origin.getX(), 0d), getWidth()),
                Math.min(Math.max(origin.getY(), 0d), getHeight()));
        transformOrigin = viewportPointToLocation(viewportOrigin);
    }

    public final void resetTransformOrigin() {
        transformOrigin = null;
        viewportOrigin = new Point2D(getWidth() / 2d, getHeight() / 2d);
    }

    private void setViewportTransform(Location origin) {
        mapOrigin = mapTransform.transform(origin);
        viewportScale = Math.pow(2d, getZoomLevel()) * (double) TileSource.TILE_SIZE / 360d;

        final Affine transform = new Affine();
        transform.prependTranslation(-mapOrigin.getX(), -mapOrigin.getY());
        transform.prependScale(viewportScale, -viewportScale);
        transform.prependRotation(getHeading());
        transform.prependTranslation(viewportOrigin.getX(), viewportOrigin.getY());
        viewportTransform.setToTransform(transform);
    }

    private void updateTransform(boolean resetTransformOrigin) {
        Location centerLocation = transformOrigin != null ? transformOrigin : getCenter();

        setViewportTransform(centerLocation);

        if (transformOrigin != null) {
            centerLocation = viewportPointToLocation(new Point2D(getWidth() / 2d, getHeight() / 2d));

            double latitude = centerLocation.getLatitude();

            if (latitude < -mapTransform.maxLatitude() || latitude > mapTransform.maxLatitude()) {
                latitude = Math.min(Math.max(latitude, -mapTransform.maxLatitude()), mapTransform.maxLatitude());
                resetTransformOrigin = true;
            }

            centerLocation = new Location(latitude, Location.normalizeLongitude(centerLocation.getLongitude()));

            internalUpdate = true;
            setCenter(centerLocation);
            internalUpdate = false;

            if (resetTransformOrigin) {
                resetTransformOrigin();
                setViewportTransform(centerLocation);
            }
        }

        // pixels per meter at center latitude
        centerScaleProperty.set(viewportScale * mapTransform.relativeScale(centerLocation) / TileSource.METERS_PER_DEGREE);
    }

    private Location adjustCenterProperty(ObjectProperty<Location> property, Location value) {
        internalUpdate = true;
        if (value == null) {
            value = new Location(0d, 0d);
            property.setValue(value);
        } else if (value.getLongitude() < -180d || value.getLongitude() > 180d
                || value.getLatitude() < -mapTransform.maxLatitude() || value.getLatitude() > mapTransform.maxLatitude()) {
            value = new Location(
                    Math.min(Math.max(value.getLatitude(), -mapTransform.maxLatitude()), mapTransform.maxLatitude()),
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

    private class CenterChangeListener implements ChangeListener<Location> {

        @Override
        public void changed(ObservableValue<? extends Location> observable, Location oldValue, Location newValue) {
            if (!internalUpdate) {
                newValue = adjustCenterProperty(centerProperty, newValue);

                if (centerTransition.getStatus() != Animation.Status.RUNNING) {
                    internalUpdate = true;
                    setTargetCenter(newValue);
                    internalUpdate = false;
                }

                resetTransformOrigin();
                updateTransform(false);
            }
        }
    }

    private class TargetCenterChangeListener implements ChangeListener<Location> {

        @Override
        public void changed(ObservableValue<? extends Location> observable, Location oldValue, Location newValue) {
            if (!internalUpdate) {
                centerTransition.start(getCenter(), adjustCenterProperty(targetCenterProperty, newValue));
            }
        }
    }

    private class ZoomLevelChangeListener implements ChangeListener<Number> {

        @Override
        public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
            if (!internalUpdate) {
                final double value = adjustZoomLevelProperty(zoomLevelProperty, newValue.doubleValue());

                if (zoomLevelTransition.getStatus() == Animation.Status.RUNNING) {
                    updateTransform(false);
                } else {
                    internalUpdate = true;
                    setTargetZoomLevel(value);
                    internalUpdate = false;
                    updateTransform(true);
                }
            }
        }
    }

    private class TargetZoomLevelChangeListener implements ChangeListener<Number> {

        @Override
        public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
            if (!internalUpdate) {
                zoomLevelTransition.start(getZoomLevel(), adjustZoomLevelProperty(targetZoomLevelProperty, newValue.doubleValue()));
            }
        }
    }

    private class HeadingChangeListener implements ChangeListener<Number> {

        @Override
        public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
            if (!internalUpdate) {
                final double value = adjustHeadingProperty(headingProperty, newValue.doubleValue());

                if (headingTransition.getStatus() == Animation.Status.RUNNING) {
                    updateTransform(false);
                } else {
                    internalUpdate = true;
                    setTargetHeading(value);
                    internalUpdate = false;
                    updateTransform(true);
                }
            }
        }
    }

    private class TargetHeadingChangeListener implements ChangeListener<Number> {

        @Override
        public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
            if (!internalUpdate) {
                headingTransition.start(getHeading(), adjustHeadingProperty(targetHeadingProperty, newValue.doubleValue()));
            }
        }
    }

    private abstract class TransitionBase<T> extends Transition {

        protected T fromValue;
        protected T toValue;

        public TransitionBase() {
            setDuration(getTransitionDuration());
            setInterpolator(Interpolator.EASE_OUT);
            setOnFinished(e -> updateTransform(true));
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
