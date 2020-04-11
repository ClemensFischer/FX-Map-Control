/*
 * FX Map Control - https://github.com/ClemensFischer/FX-Map-Control
 * © 2020 Clemens Fischer
 */
package fxmapcontrol;

/**
 * Provides a method to asynchronously load map tile images.
 */
public interface ITileImageLoader {

    void loadTiles(String tileLayerName, TileSource tileSource, Iterable<Tile> tiles);
}
