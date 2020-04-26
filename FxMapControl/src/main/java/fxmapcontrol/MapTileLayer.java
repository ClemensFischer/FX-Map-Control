/*
 * FX Map Control - https://github.com/ClemensFischer/FX-Map-Control
 * © 2020 Clemens Fischer
 */
package fxmapcontrol;

import java.util.ArrayList;
import java.util.stream.Collectors;

import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.image.ImageView;

/**
 * Displays web mercator map tiles.
 */
public class MapTileLayer extends MapTileLayerBase {

    public static final int TILE_SIZE = 256;

    public static final Point2D MAP_TOP_LEFT = new Point2D(
            -180d * MapProjection.WGS84_METERS_PER_DEGREE, 180d * MapProjection.WGS84_METERS_PER_DEGREE);

    private int minZoomLevel;
    private int maxZoomLevel = 18;
    private TileMatrix tileMatrix;
    private ArrayList<Tile> tiles = new ArrayList<>();

    public static MapTileLayer getOpenStreetMapLayer() {
        return new MapTileLayer("OpenStreetMap", "http://tile.openstreetmap.org/{z}/{x}/{y}.png", 0, 19);
    }

    public MapTileLayer(ITileImageLoader tileImageLoader) {
        super(tileImageLoader);
        getStyleClass().add("map-tile-layer");
        tileSourceProperty().addListener((observable, oldValue, newValue) -> updateTiles(true));
    }

    public MapTileLayer() {
        this(new TileImageLoader());
    }

    public MapTileLayer(String name, String tileUrlFormat) {
        this();
        setName(name);
        setTileSource(new TileSource(tileUrlFormat));
    }

    public MapTileLayer(String name, String tileUrlFormat, int minZoomLevel, int maxZoomLevel) {
        this(name, tileUrlFormat);
        setMinZoomLevel(minZoomLevel);
        setMaxZoomLevel(maxZoomLevel);
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

    @Override
    protected void updateTileLayer() {
        getUpdateTimeline().stop();

        if (getMap() == null || !getMap().getProjection().isWebMercator()) {
            tileMatrix = null;
            updateTiles(true);
        } else if (setTileMatrix()) {
            setTransform();
            updateTiles(false);
        }
    }

    @Override
    protected void setTransform() {
        // tile matrix origin in pixels
        //
        Point2D tileMatrixOrigin = new Point2D(TILE_SIZE * tileMatrix.getXMin(), TILE_SIZE * tileMatrix.getYMin());

        double tileMatrixScale = ViewTransform.zoomLevelToScale(tileMatrix.getZoomLevel());

        getTransforms().set(0,
                getMap().getViewTransform().getTileLayerTransform(tileMatrixScale, MAP_TOP_LEFT, tileMatrixOrigin));
    }

    private boolean setTileMatrix() {
        MapBase map = getMap();

        int tileMatrixZoomLevel = (int) Math.floor(map.getZoomLevel() + 0.001); // avoid rounding issues
        double tileMatrixScale = ViewTransform.zoomLevelToScale(tileMatrixZoomLevel);

        // bounds in tile pixels from view size
        //
        Bounds tileBounds = map.getViewTransform().getTileMatrixBounds(tileMatrixScale, MAP_TOP_LEFT, map.getWidth(), map.getHeight());

        // tile column and row index bounds
        //
        int xMin = (int) Math.floor(tileBounds.getMinX() / TILE_SIZE);
        int yMin = (int) Math.floor(tileBounds.getMinY() / TILE_SIZE);
        int xMax = (int) Math.floor(tileBounds.getMaxX() / TILE_SIZE);
        int yMax = (int) Math.floor(tileBounds.getMaxY() / TILE_SIZE);

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
                    minZoom = Math.max(tileMatrix.getZoomLevel() - getMaxBackgroundLevels(), minZoomLevel);
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
                        int tileSize = TILE_SIZE << (tileMatrix.getZoomLevel() - tile.getZoomLevel());
                        imageView.setX(tileSize * tile.getX() - TILE_SIZE * tileMatrix.getXMin());
                        imageView.setY(tileSize * tile.getY() - TILE_SIZE * tileMatrix.getYMin());
                        imageView.setFitWidth(tileSize);
                        imageView.setFitHeight(tileSize);
                        return imageView;
                    })
                    .collect(Collectors.toList()));
        }

        getTileImageLoader().loadTiles(tiles, getTileSource(), getName());
    }
}
