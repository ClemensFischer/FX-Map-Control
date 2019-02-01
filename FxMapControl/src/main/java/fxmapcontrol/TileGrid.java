/*
 * FX Map Control - https://github.com/ClemensFischer/FX-Map-Control
 * © 2019 Clemens Fischer
 */
package fxmapcontrol;

/**
 * Defines zoom level and tile index ranges of a MapTileLayer.
 */
public class TileGrid {
    private final int zoomLevel;
    private final int xMin;
    private final int yMin;
    private final int xMax;
    private final int yMax;

    public TileGrid(int zoomLevel, int xMin, int yMin, int xMax, int yMax) {
        this.zoomLevel = zoomLevel;
        this.xMin = xMin;
        this.yMin = yMin;
        this.xMax = xMax;
        this.yMax = yMax;
    }

    public final int getZoomLevel() {
        return zoomLevel;
    }

    public final int getXMin() {
        return xMin;
    }

    public final int getYMin() {
        return yMin;
    }

    public final int getXMax() {
        return xMax;
    }

    public final int getYMax() {
        return yMax;
    }

    public final boolean equals(TileGrid tileGrid) {
        return tileGrid != null
                && zoomLevel == tileGrid.zoomLevel
                && xMin == tileGrid.xMin
                && yMin == tileGrid.yMin
                && xMax == tileGrid.xMax
                && yMax == tileGrid.yMax;
    }

    @Override
    public final boolean equals(Object obj) {
        return (obj instanceof TileGrid) && equals((TileGrid)obj);
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(zoomLevel)
                ^ Integer.hashCode(xMin)
                ^ Integer.hashCode(yMin)
                ^ Integer.hashCode(xMax)
                ^ Integer.hashCode(yMax);
    }
}
