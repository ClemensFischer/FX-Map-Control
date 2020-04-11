/*
 * FX Map Control - https://github.com/ClemensFischer/FX-Map-Control
 * © 2020 Clemens Fischer
 */
package fxmapcontrol;

import java.util.List;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.css.CssMetaData;
import javafx.css.SimpleStyleableBooleanProperty;
import javafx.css.SimpleStyleableObjectProperty;
import javafx.css.Styleable;
import javafx.css.StyleableBooleanProperty;
import javafx.css.StyleableObjectProperty;
import javafx.css.StyleablePropertyFactory;
import javafx.scene.Parent;
import javafx.scene.transform.Affine;
import javafx.util.Duration;

public abstract class MapTileLayerBase extends Parent implements IMapNode {

    private static final StyleablePropertyFactory<MapTileLayerBase> propertyFactory
            = new StyleablePropertyFactory<>(Parent.getClassCssMetaData());

    private static final CssMetaData<MapTileLayerBase, Duration> updateDelayCssMetaData
            = propertyFactory.createDurationCssMetaData("-fx-update-delay", s -> s.updateDelayProperty);

    private static final CssMetaData<MapTileLayerBase, Boolean> updateWhileViewportChangingCssMetaData
            = propertyFactory.createBooleanCssMetaData("-fx-update-while-viewport-changing", s -> s.updateWhileViewportChangingProperty);

    private final StyleableObjectProperty<Duration> updateDelayProperty
            = new SimpleStyleableObjectProperty<>(updateDelayCssMetaData, this, "updateDelay", Duration.seconds(0.2));

    private final StyleableBooleanProperty updateWhileViewportChangingProperty
            = new SimpleStyleableBooleanProperty(updateWhileViewportChangingCssMetaData, this, "updateWhileViewportChanging", true);

    private final ObjectProperty<TileSource> tileSourceProperty = new SimpleObjectProperty<>(this, "tileSource");

    private final ITileImageLoader tileImageLoader;
    private final Timeline updateTimeline = new Timeline();
    private final MapNodeHelper mapNodeHelper = new MapNodeHelper(e -> onViewportChanged(e.getProjectionChanged(), e.getLongitudeOffset()));

    private int maxBackgroundLevels = 8;
    private String name;

    protected MapTileLayerBase(ITileImageLoader tileImageLoader) {
        getStyleClass().add("map-tile-layer-base");
        getTransforms().add(new Affine());
        setMouseTransparent(true);

        this.tileImageLoader = tileImageLoader;

        updateTimeline.getKeyFrames().add(new KeyFrame(getUpdateDelay(), e -> updateTileLayer()));

        updateDelayProperty.addListener((observable, oldValue, newValue)
                -> updateTimeline.getKeyFrames().set(0, new KeyFrame(getUpdateDelay(), e -> updateTileLayer())));
    }

    @Override
    public final MapBase getMap() {
        return mapNodeHelper.getMap();
    }

    @Override
    public void setMap(MapBase map) {
        mapNodeHelper.setMap(map);
        updateTileLayer();
    }

    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return propertyFactory.getCssMetaData();
    }

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return getClassCssMetaData();
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

    public final ObjectProperty<TileSource> tileSourceProperty() {
        return tileSourceProperty;
    }

    public final TileSource getTileSource() {
        return tileSourceProperty.get();
    }

    public final void setTileSource(TileSource tileSource) {
        tileSourceProperty.set(tileSource);
    }

    public ITileImageLoader getTileImageLoader() {
        return tileImageLoader;
    }
    
    public Timeline getUpdateTimeline() {
        return updateTimeline;
    }

    public final int getMaxBackgroundLevels() {
        return maxBackgroundLevels;
    }

    public final void setMaxBackgroundLevels(int maxBackgroundLevels) {
        this.maxBackgroundLevels = maxBackgroundLevels;
    }

    public final String getName() {
        return name;
    }

    public final void setName(String name) {
        this.name = name;
    }

    private void onViewportChanged(boolean projectionChanged, double longitudeOffset) {
        if (getChildren().isEmpty() || projectionChanged || Math.abs(longitudeOffset) > 180d) {
            // update immediately when map projection has changed or map center has moved across 180° longitude
            updateTileLayer();

        } else {
            setTransform();

            if (getUpdateWhileViewportChanging()) {
                updateTimeline.play();
            } else {
                updateTimeline.playFromStart();
            }
        }
    }

    protected abstract void updateTileLayer();
    
    protected abstract void setTransform();
}
