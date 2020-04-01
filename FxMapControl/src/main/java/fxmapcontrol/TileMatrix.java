/*
 * FX Map Control - https://github.com/ClemensFischer/FX-Map-Control
 * © 2020 Clemens Fischer
 */
package fxmapcontrol;

/**
 * Defines zoom level and tile index ranges of a MapTileLayer.
 */
public class TileMatrix {
    private final int zoomLevel;
    private final int xMin;
    private final int yMin;
    private final int xMax;
    private final int yMax;

    public TileMatrix(int zoomLevel, int xMin, int yMin, int xMax, int yMax) {
        System.out.println("" + zoomLevel + ": " + xMin + ".." + xMax + ", " + yMin + ".." + yMax);
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
}
