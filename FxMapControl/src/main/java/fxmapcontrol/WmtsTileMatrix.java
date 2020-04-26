/*
 * FX Map Control - https://github.com/ClemensFischer/FX-Map-Control
 * © 2020 Clemens Fischer
 */
package fxmapcontrol;

import javafx.geometry.Point2D;

public class WmtsTileMatrix {

    private final String identifier;
    private final double scale;
    private final Point2D topLeft;
    private final int tileWidth;
    private final int tileHeight;
    private final int matrixWidth;
    private final int matrixHeight;

    public WmtsTileMatrix(String identifier, double scaleDenominator, Point2D topLeft,
                          int tileWidth, int tileHeight, int matrixWidth, int matrixHeight) {
        this.identifier = identifier;
        this.scale = 1 / (scaleDenominator * 0.00028);
        this.topLeft = topLeft;
        this.tileWidth = tileWidth;
        this.tileHeight = tileHeight;
        this.matrixWidth = matrixWidth;
        this.matrixHeight = matrixHeight;
    }

    public final String getIdentifier() {
        return identifier;
    }

    public final double getScale() {
        return scale;
    }

    public final Point2D getTopLeft() {
        return topLeft;
    }

    public final int getTileWidth() {
        return tileWidth;
    }

    public final int getTileHeight() {
        return tileHeight;
    }

    public final int getMatrixWidth() {
        return matrixWidth;
    }

    public final int getMatrixHeight() {
        return matrixHeight;
    }
}
