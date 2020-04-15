/*
 * FX Map Control - https://github.com/ClemensFischer/FX-Map-Control
 * © 2020 Clemens Fischer
 */
package fxmapcontrol;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.ObservableList;
import javafx.css.CssMetaData;
import javafx.css.SimpleStyleableBooleanProperty;
import javafx.css.SimpleStyleableDoubleProperty;
import javafx.css.SimpleStyleableObjectProperty;
import javafx.css.Styleable;
import javafx.css.StyleableBooleanProperty;
import javafx.css.StyleableDoubleProperty;
import javafx.css.StyleableObjectProperty;
import javafx.css.StyleablePropertyFactory;
import javafx.geometry.BoundingBox;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.image.Image;
import javafx.util.Duration;

/**
 * Map image overlay. Fills the viewport with a single map image, e.g. from a Web Map Service (WMS).
 *
 * The image must be provided by the abstract loadImage() method.
 */
public abstract class MapImageLayer extends Parent implements IMapNode {

    private static final StyleablePropertyFactory<MapImageLayer> propertyFactory
            = new StyleablePropertyFactory<>(Parent.getClassCssMetaData());

    private static final CssMetaData<MapImageLayer, Duration> updateDelayCssMetaData
            = propertyFactory.createDurationCssMetaData("-fx-update-delay", s -> s.updateDelayProperty);

    private static final CssMetaData<MapImageLayer, Boolean> updateWhileViewportChangingCssMetaData
            = propertyFactory.createBooleanCssMetaData("-fx-update-while-viewport-changing", s -> s.updateWhileViewportChangingProperty);

    private static final CssMetaData<MapImageLayer, Number> relativeImageSizeCssMetaData
            = propertyFactory.createSizeCssMetaData("-fx-relative-image-size", s -> s.relativeImageSizeProperty);

    private final StyleableObjectProperty<Duration> updateDelayProperty
            = new SimpleStyleableObjectProperty<>(updateDelayCssMetaData, this, "updateDelay", Duration.seconds(0.2));

    private final StyleableBooleanProperty updateWhileViewportChangingProperty
            = new SimpleStyleableBooleanProperty(updateWhileViewportChangingCssMetaData, this, "updateWhileViewportChanging");

    private final StyleableDoubleProperty relativeImageSizeProperty
            = new SimpleStyleableDoubleProperty(relativeImageSizeCssMetaData, this, "relativeImageSize", 1d);

    private final DoubleProperty minLatitudeProperty = new SimpleDoubleProperty(this, "minLatitude", Double.NaN);
    private final DoubleProperty maxLatitudeProperty = new SimpleDoubleProperty(this, "maxLatitude", Double.NaN);
    private final DoubleProperty minLongitudeProperty = new SimpleDoubleProperty(this, "minLongitude", Double.NaN);
    private final DoubleProperty maxLongitudeProperty = new SimpleDoubleProperty(this, "maxLongitude", Double.NaN);
    private final DoubleProperty maxBoundingBoxWidthProperty = new SimpleDoubleProperty(this, "maxBoundingBoxWidth", Double.NaN);

    private final Timeline updateTimeline = new Timeline();
    private final MapNodeHelper mapNodeHelper = new MapNodeHelper(e -> onViewportChanged(e.getProjectionChanged(), e.getLongitudeOffset()));
    private MapBoundingBox boundingBox;
    private boolean updateInProgress;

    public MapImageLayer() {
        getStyleClass().add("map-image-layer");
        setMouseTransparent(true);

        updateTimeline.getKeyFrames().add(new KeyFrame(getUpdateDelay(), e -> updateImage()));

        updateDelayProperty.addListener((observable, oldValue, newValue)
                -> updateTimeline.getKeyFrames().set(0, new KeyFrame(getUpdateDelay(), e -> updateImage())));
    }

    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return propertyFactory.getCssMetaData();
    }

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return getClassCssMetaData();
    }

    @Override
    public final MapBase getMap() {
        return mapNodeHelper.getMap();
    }

    @Override
    public void setMap(MapBase map) {
        mapNodeHelper.setMap(map);
        getChildren().forEach(image -> ((MapImage) image).setMap(map));
        updateImage();
    }

    public final ObjectProperty<Duration> updateDelayProperty() {
        return updateDelayProperty;
    }

    public final Duration getUpdateDelay() {
        return updateDelayProperty.get();
    }

    public final void setUpdateDelay(Duration updateDelay) {
        updateDelayProperty.set(updateDelay);
    }

    public final BooleanProperty updateWhileViewportChangingProperty() {
        return updateWhileViewportChangingProperty;
    }

    public final boolean getUpdateWhileViewportChanging() {
        return updateWhileViewportChangingProperty.get();
    }

    public final void setUpdateWhileViewportChanging(boolean updateWhileViewportChanging) {
        updateWhileViewportChangingProperty.set(updateWhileViewportChanging);
    }

    public final DoubleProperty relativeImageSizeProperty() {
        return relativeImageSizeProperty;
    }

    public final double getRelativeImageSize() {
        return relativeImageSizeProperty.get();
    }

    public final void setRelativeImageSize(double relativeImageSize) {
        relativeImageSizeProperty.set(relativeImageSize);
    }

    public final DoubleProperty minLatitudeProperty() {
        return minLatitudeProperty;
    }

    public final double getMinLatitude() {
        return minLatitudeProperty.get();
    }

    public final void setMinLatitude(double minLatitude) {
        minLatitudeProperty.set(minLatitude);
    }

    public final DoubleProperty maxLatitudeProperty() {
        return maxLatitudeProperty;
    }

    public final double getMaxLatitude() {
        return maxLatitudeProperty.get();
    }

    public final void setMaxLatitude(double maxLatitude) {
        maxLatitudeProperty.set(maxLatitude);
    }

    public final DoubleProperty minLongitudeProperty() {
        return minLongitudeProperty;
    }

    public final double getMinLongitude() {
        return minLongitudeProperty.get();
    }

    public final void setMinLongitude(double minLongitude) {
        minLongitudeProperty.set(minLongitude);
    }

    public final DoubleProperty maxLongitudeProperty() {
        return maxLongitudeProperty;
    }

    public final double getMaxLongitude() {
        return maxLongitudeProperty.get();
    }

    public final void setMaxLongitude(double maxLongitude) {
        maxLongitudeProperty.set(maxLongitude);
    }

    public final DoubleProperty maxBoundingBoxWidthProperty() {
        return maxBoundingBoxWidthProperty;
    }

    public final double getMaxBoundingBoxWidth() {
        return maxBoundingBoxWidthProperty.get();
    }

    public final void setMaxBoundingBoxWidth(double maxBoundingBoxWidth) {
        maxBoundingBoxWidthProperty.set(maxBoundingBoxWidth);
    }

    public final MapBoundingBox getBoundingBox() {
        return boundingBox;
    }

    private void onViewportChanged(boolean projectionChanged, double longitudeOffset) {
        if (projectionChanged) {
            setImage(null);
            updateImage();

        } else {
            if (Math.abs(longitudeOffset) > 180d && boundingBox != null && boundingBox.hasValidBounds()) {
                double offset = 360d * Math.signum(longitudeOffset);

                boundingBox.setWest(boundingBox.getWest() + offset);
                boundingBox.setEast(boundingBox.getEast() + offset);

                getChildren().forEach(image -> {
                    MapImage mapImage = (MapImage) image;
                    MapBoundingBox bbox = mapImage.getBoundingBox();

                    if (bbox != null && bbox.hasValidBounds()) {
                        mapImage.setBoundingBox(new MapBoundingBox(
                                bbox.getSouth(), bbox.getWest() + offset,
                                bbox.getNorth(), bbox.getEast() + offset));
                    }
                });
            }

            if (getUpdateWhileViewportChanging()) {
                updateTimeline.play();
            } else {
                updateTimeline.playFromStart();
            }
        }
    }

    protected final void updateImage() {
        MapBase map = getMap();

        if (updateInProgress) {
            updateTimeline.playFromStart(); // update image on next timer tick

        } else if (map != null && map.getWidth() > 0 && map.getHeight() > 0) {
            updateInProgress = true;

            double width = map.getWidth() * getRelativeImageSize();
            double height = map.getHeight() * getRelativeImageSize();
            double x = (map.getWidth() - width) / 2d;
            double y = (map.getHeight() - height) / 2d;

            boundingBox = map.viewBoundsToBoundingBox(new BoundingBox(x, y, width, height));

            if (boundingBox != null && boundingBox.hasValidBounds()) {
                if (!Double.isNaN(getMinLatitude()) && boundingBox.getSouth() < getMinLatitude()) {
                    boundingBox.setSouth(getMinLatitude());
                }

                if (!Double.isNaN(getMinLongitude()) && boundingBox.getWest() < getMinLongitude()) {
                    boundingBox.setWest(getMinLongitude());
                }

                if (!Double.isNaN(getMaxLatitude()) && boundingBox.getNorth() > getMaxLatitude()) {
                    boundingBox.setNorth(getMaxLatitude());
                }

                if (!Double.isNaN(getMaxLongitude()) && boundingBox.getEast() > getMaxLongitude()) {
                    boundingBox.setEast(getMaxLongitude());
                }

                if (!Double.isNaN(getMaxBoundingBoxWidth()) && boundingBox.getWidth() > getMaxBoundingBoxWidth()) {
                    double d = (boundingBox.getWidth() - getMaxBoundingBoxWidth()) / 2;
                    boundingBox.setWest(boundingBox.getWest() + d);
                    boundingBox.setEast(boundingBox.getEast() - d);
                }
            }

            Image image = null;

            try {
                image = loadImage();
            } catch (Exception ex) {
                Logger.getLogger(MapImageLayer.class.getName()).log(Level.WARNING, ex.toString());
            }

            updateImage(image);
        }
    }

    /**
     * Creates a javafx.scene.image.Image for the current bounding box.
     *
     * @return Image on success, null on failure
     */
    protected abstract Image loadImage();

    private void updateImage(Image image) {
        if (image != null && image.isBackgroundLoading()) {
            image.progressProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue.doubleValue() >= 1d) {
                    setImage(image);
                }
            });

            image.errorProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue) {
                    setImage(null);
                }
            });
        } else {
            setImage(image);
        }
    }

    private void setImage(Image image) {
        MapBase map = getMap();

        if (map != null) {
            ObservableList<Node> children = getChildren();
            MapImage mapImage;

            if (children.isEmpty()) {
                mapImage = new MapImage();
                mapImage.setMap(map);
                mapImage.setOpacity(0d);
                children.add(mapImage);

                mapImage = new MapImage();
                mapImage.setMap(map);
                mapImage.setOpacity(0d);
            } else {
                mapImage = (MapImage) children.remove(0);
            }

            children.add(mapImage);

            mapImage.setImage(image);
            mapImage.setBoundingBox(boundingBox != null ? boundingBox.clone() : null);

            if (image != null) {
                FadeTransition fadeTransition = new FadeTransition(map.getTileFadeDuration(), mapImage);
                fadeTransition.setToValue(1d);
                fadeTransition.setOnFinished(e -> children.get(0).setOpacity(0d));
                fadeTransition.play();
            } else {
                children.get(0).setOpacity(0d);
                children.get(1).setOpacity(0d);
            }
        }

        updateInProgress = false;
    }
}
