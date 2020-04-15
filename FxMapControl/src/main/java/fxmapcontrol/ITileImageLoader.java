/*
 * FX Map Control - https://github.com/ClemensFischer/FX-Map-Control
 * © 2020 Clemens Fischer
 */
package fxmapcontrol;

import java.util.Collection;

/**
 * Provides a method to asynchronously load map tile images.
 */
public interface ITileImageLoader {

    void loadTiles(Collection<Tile> tiles, TileSource tileSource, String tileSourceName);
}
