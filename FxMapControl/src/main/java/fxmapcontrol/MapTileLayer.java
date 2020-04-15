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
 * Fills the map viewport with map tiles from a TileSource.
 */
public class MapTileLayer extends MapTileLayerBase {

    public static final int TileSize = 256;

    public static final Point2D TileMatrixTopLeft = new Point2D(
            -180d * MapProjection.Wgs84MetersPerDegree, 180d * MapProjection.Wgs84MetersPerDegree);

    private ArrayList<Tile> tiles = new ArrayList<>();
    private TileMatrix tileMatrix;
    private int minZoomLevel;
    private int maxZoomLevel = 18;

    public static MapTileLayer getOpenStreetMapLayer() {
        return new MapTileLayer("OpenStreetMap", "http://tile.openstreetmap.org/{z}/{x}/{y}.png", 0, 19);
    }

    public MapTileLayer(String name, String tileUrlFormat) {
        this(name, tileUrlFormat, 0, 18);
    }

    public MapTileLayer(String name, String tileUrlFormat, int minZoomLevel, int maxZoomLevel) {
        this();
        setName(name);
        setTileSource(new TileSource(tileUrlFormat));
        this.minZoomLevel = minZoomLevel;
        this.maxZoomLevel = maxZoomLevel;
    }

    public MapTileLayer() {
        this(new TileImageLoader());
    }

    public MapTileLayer(ITileImageLoader tileImageLoader) {
        super(tileImageLoader);
        getStyleClass().add("map-tile-layer");
        tileSourceProperty().addListener((observable, oldValue, newValue) -> updateTiles(true));
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
        Point2D tileMatrixOrigin = new Point2D(TileSize * tileMatrix.getXMin(), TileSize * tileMatrix.getYMin());

        double tileMatrixScale = ViewTransform.zoomLevelToScale(tileMatrix.getZoomLevel());

        getTransforms().set(0,
                getMap().getViewTransform().getTileLayerTransform(tileMatrixScale, TileMatrixTopLeft, tileMatrixOrigin));
    }

    private boolean setTileMatrix() {
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
                        int tileSize = TileSize << (tileMatrix.getZoomLevel() - tile.getZoomLevel());
                        imageView.setX(tileSize * tile.getX() - TileSize * tileMatrix.getXMin());
                        imageView.setY(tileSize * tile.getY() - TileSize * tileMatrix.getYMin());
                        imageView.setFitWidth(tileSize);
                        imageView.setFitHeight(tileSize);
                        return imageView;
                    })
                    .collect(Collectors.toList()));
        }

        getTileImageLoader().loadTiles(tiles, getTileSource(), getName());
    }
}
