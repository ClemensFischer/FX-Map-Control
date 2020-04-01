/*
 * FX Map Control - https://github.com/ClemensFischer/FX-Map-Control
 * © 2020 Clemens Fischer
 */
package fxmapcontrol;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.css.CssMetaData;
import javafx.css.SimpleStyleableBooleanProperty;
import javafx.css.SimpleStyleableIntegerProperty;
import javafx.css.SimpleStyleableObjectProperty;
import javafx.css.Styleable;
import javafx.css.StyleableBooleanProperty;
import javafx.css.StyleableIntegerProperty;
import javafx.css.StyleableObjectProperty;
import javafx.css.StyleablePropertyFactory;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Parent;
import javafx.scene.image.ImageView;
import javafx.scene.transform.Affine;
import javafx.util.Duration;

/**
 * Fills the map viewport with map tiles from a TileSource.
 */
public class MapTileLayer extends Parent implements IMapNode {

    public static final int TileSize = 256;

    public static final Point2D TileMatrixTopLeft = new Point2D(
            -180d * MapProjection.Wgs84MetersPerDegree, 180d * MapProjection.Wgs84MetersPerDegree);

    private static final StyleablePropertyFactory<MapTileLayer> propertyFactory
            = new StyleablePropertyFactory<>(Parent.getClassCssMetaData());

    private static final CssMetaData<MapTileLayer, Number> maxDownloadThreadsCssMetaData
            = propertyFactory.createSizeCssMetaData("-fx-max-download-threads", s -> s.maxDownloadThreadsProperty);

    private static final CssMetaData<MapTileLayer, Duration> updateDelayCssMetaData
            = propertyFactory.createDurationCssMetaData("-fx-update-delay", s -> s.updateDelayProperty);

    private static final CssMetaData<MapTileLayer, Boolean> updateWhileViewportChangingCssMetaData
            = propertyFactory.createBooleanCssMetaData("-fx-update-while-viewport-changing", s -> s.updateWhileViewportChangingProperty);

    private final StyleableIntegerProperty maxDownloadThreadsProperty
            = new SimpleStyleableIntegerProperty(maxDownloadThreadsCssMetaData, this, "maxDownloadThreads", 4);

    private final StyleableObjectProperty<Duration> updateDelayProperty
            = new SimpleStyleableObjectProperty<>(updateDelayCssMetaData, this, "updateDelay", Duration.seconds(0.2));

    private final StyleableBooleanProperty updateWhileViewportChangingProperty
            = new SimpleStyleableBooleanProperty(updateWhileViewportChangingCssMetaData, this, "updateWhileViewportChanging", true);

    private final ObjectProperty<TileSource> tileSourceProperty = new SimpleObjectProperty<>(this, "tileSource");

    private final ITileImageLoader tileImageLoader;
    private final Timeline updateTimeline = new Timeline();
    private final MapNodeHelper mapNodeHelper = new MapNodeHelper(e -> onViewportChanged(e.getProjectionChanged(), e.getLongitudeOffset()));
    private ArrayList<Tile> tiles = new ArrayList<>();
    private TileMatrix tileMatrix;
    private int minZoomLevel;
    private int maxZoomLevel = 18;
    private int maxBackgroundLevels = 8;
    private String name;

    public static MapTileLayer getOpenStreetMapLayer() {
        return new MapTileLayer("OpenStreetMap", "http://{c}.tile.openstreetmap.org/{z}/{x}/{y}.png", 0, 19);
    }

    public MapTileLayer() {
        this(new TileImageLoader());
    }

    public MapTileLayer(String name, String tileUrlFormat, int minZoomLevel, int maxZoomLevel) {
        this(name, tileUrlFormat, minZoomLevel, maxZoomLevel, 8);
    }

    public MapTileLayer(String name, String tileUrlFormat, int minZoomLevel, int maxZoomLevel, int maxBackgroundLevels) {
        this(new TileImageLoader(), name, tileUrlFormat, minZoomLevel, maxZoomLevel, maxBackgroundLevels);
    }

    public MapTileLayer(ITileImageLoader tileImageLoader, String name, String tileUrlFormat, int minZoomLevel, int maxZoomLevel, int maxBackgroundLevels) {
        this(tileImageLoader);
        this.name = name;
        this.minZoomLevel = minZoomLevel;
        this.maxZoomLevel = maxZoomLevel;
        this.maxBackgroundLevels = maxBackgroundLevels;
        setTileSource(new TileSource(tileUrlFormat));
    }

    public MapTileLayer(ITileImageLoader tileImageLoader) {
        getStyleClass().add("map-tile-layer");
        getTransforms().add(new Affine());
        setMouseTransparent(true);

        this.tileImageLoader = tileImageLoader;

        tileSourceProperty.addListener((observable, oldValue, newValue) -> updateTiles(true));

        updateTimeline.getKeyFrames().add(new KeyFrame(getUpdateDelay(), e -> updateTileGrid()));

        updateDelayProperty.addListener((observable, oldValue, newValue)
                -> updateTimeline.getKeyFrames().set(0, new KeyFrame(getUpdateDelay(), e -> updateTileGrid())));
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
        updateTileGrid();
    }

    public final IntegerProperty maxDownloadThreadsProperty() {
        return maxDownloadThreadsProperty;
    }

    public final int getMaxDownloadThreads() {
        return maxDownloadThreadsProperty.get();
    }

    public final void setMaxDownloadThreads(int maxDownloadThreads) {
        maxDownloadThreadsProperty.set(maxDownloadThreads);
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

    public final int getMinZoomLevel() {
        return minZoomLevel;
    }

    public final void setMinZoomLevel(int minZoomLevel) {
        this.minZoomLevel = minZoomLevel;
    }

    public final int getMaxZoomLevel() {
        return maxZoomLevel;
    }

    public final void setMaxZoomLevel(int maxZoomLevel) {
        this.maxZoomLevel = maxZoomLevel;
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

    public ITileImageLoader getTileImageLoader() {
        return tileImageLoader;
    }

    protected void updateTileGrid() {
        updateTimeline.stop();

        if (getMap() == null || !getMap().getProjection().isWebMercator()) {
            tileMatrix = null;
            updateTiles(true);
        } else if (setTileGrid()) {
            setTransform();
            updateTiles(false);
        }
    }

    private void onViewportChanged(boolean projectionChanged, double longitudeOffset) {
        if (tileMatrix == null || projectionChanged || Math.abs(longitudeOffset) > 180d) {
            // update immediately when map projection has changed or map center has moved across 180° longitude
            updateTileGrid();

        } else {
            setTransform();

            if (getUpdateWhileViewportChanging()) {
                updateTimeline.play();
            } else {
                updateTimeline.playFromStart();
            }
        }
    }

    private Point2D getTileCenter(double tileScale) {
        Location center = getMap().getCenter();

        return new Point2D(
                tileScale * (0.5 + center.getLongitude() / 360d),
                tileScale * (0.5 - WebMercatorProjection.latitudeToY(center.getLatitude()) / 360d));
    }

    private void setTransform() {
        // tile matrix origin in pixels
        //
        Point2D tileMatrixOrigin = new Point2D(TileSize * tileMatrix.getXMin(), TileSize * tileMatrix.getYMin());

        double tileMatrixScale = ViewTransform.zoomLevelToScale(tileMatrix.getZoomLevel());

        getTransforms().set(0,
                getMap().getViewTransform().getTileLayerTransform(tileMatrixScale, TileMatrixTopLeft, tileMatrixOrigin));
    }

    private boolean setTileGrid() {
        MapBase map = getMap();

        int tileMatrixZoomLevel = (int) Math.floor(map.getZoomLevel() + 0.001); // avoid rounding issues

        double tileMatrixScale = ViewTransform.zoomLevelToScale(tileMatrixZoomLevel);

        // bounds in tile pixels from view size
        //
        Bounds tileBounds = map.getViewTransform().getTileMatrixBounds(
                tileMatrixScale, TileMatrixTopLeft, map.getWidth(), map.getHeight());

        // tile column and row index bounds
        //
        int xMin = (int) Math.floor(tileBounds.getMinX() / TileSize);
        int yMin = (int) Math.floor(tileBounds.getMinY() / TileSize);
        int xMax = (int) Math.floor(tileBounds.getMaxX() / TileSize);
        int yMax = (int) Math.floor(tileBounds.getMaxY() / TileSize);

        if (tileMatrix != null
                && tileMatrix.getZoomLevel() == tileMatrixZoomLevel
                && tileMatrix.getXMin() == xMin
                && tileMatrix.getYMin() == yMin
                && tileMatrix.getXMax() == xMax
                && tileMatrix.getYMax() == yMax) {
            return false;
        }

        tileMatrix = new TileMatrix(tileMatrixZoomLevel, xMin, yMin, xMax, yMax);
        return true;
    }

    private void updateTiles(boolean clearTiles) {
        if (!tiles.isEmpty()) {
            tileImageLoader.cancelLoadTiles(this);
        }

        if (clearTiles) {
            tiles.clear();
        }

        MapBase map = getMap();
        ArrayList<Tile> newTiles = new ArrayList<>();

        if (map != null && tileMatrix != null && getTileSource() != null) {
            int maxZoom = Math.min(tileMatrix.getZoomLevel(), maxZoomLevel);

            if (maxZoom >= minZoomLevel) {
                int minZoom = maxZoom;

                if (this == map.getChildrenUnmodifiable().stream().findFirst().orElse(null)) {
                    // load background tiles
                    minZoom = Math.max(tileMatrix.getZoomLevel() - maxBackgroundLevels, minZoomLevel);
                }

                for (int tz = minZoom; tz <= maxZoom; tz++) {
                    int tileSize = 1 << (tileMatrix.getZoomLevel() - tz);
                    int x1 = (int) Math.floor((double) tileMatrix.getXMin() / tileSize); // may be negative
                    int x2 = tileMatrix.getXMax() / tileSize;
                    int y1 = Math.max(tileMatrix.getYMin() / tileSize, 0);
                    int y2 = Math.min(tileMatrix.getYMax() / tileSize, (1 << tz) - 1);

                    for (int ty = y1; ty <= y2; ty++) {
                        for (int tx = x1; tx <= x2; tx++) {
                            int z = tz;
                            int x = tx;
                            int y = ty;
                            Tile tile = tiles.stream()
                                    .filter(t -> t.getZoomLevel() == z && t.getX() == x && t.getY() == y)
                                    .findAny().orElse(null);

                            if (tile == null) {
                                tile = new Tile(z, x, y);
                                int xIndex = tile.getXIndex();

                                Tile equivalentTile = tiles.stream()
                                        .filter(t -> t.getZoomLevel() == z && t.getXIndex() == xIndex && t.getY() == y && t.getImage() != null)
                                        .findAny().orElse(null);

                                if (equivalentTile != null) {
                                    tile.setImage(equivalentTile.getImage(), false);
                                }
                            }

                            newTiles.add(tile);
                        }
                    }
                }
            }
        }

        tiles = newTiles;

        if (tiles.isEmpty()) {
            getChildren().clear();

        } else {
            getChildren().setAll(tiles.stream()
                    .map(tile -> {
                        ImageView imageView = tile.getImageView();
                        int tileSize = TileSize << (tileMatrix.getZoomLevel() - tile.getZoomLevel());
                        imageView.setX(tileSize * tile.getX() - TileSize * tileMatrix.getXMin());
                        imageView.setY(tileSize * tile.getY() - TileSize * tileMatrix.getYMin());
                        imageView.setFitWidth(tileSize);
                        imageView.setFitHeight(tileSize);
                        return imageView;
                    })
                    .collect(Collectors.toList()));

            tileImageLoader.beginLoadTiles(this, tiles.stream()
                    .filter(tile -> tile.isPending())
                    .collect(Collectors.toList()));
        }
    }
}
