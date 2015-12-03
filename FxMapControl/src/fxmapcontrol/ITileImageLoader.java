/*
 * FX Map Control - https://github.com/ClemensFischer/FX-Map-Control
 * © 2015 Clemens Fischer
 */
package fxmapcontrol;

/**
 * Provides methods to begin and cancel loading of map tile images for a specific MapTileLayer.
 */
public interface ITileImageLoader {

    void beginLoadTiles(MapTileLayer tileLayer, Iterable<Tile> tiles);

    void cancelLoadTiles(MapTileLayer tileLayer);
}
