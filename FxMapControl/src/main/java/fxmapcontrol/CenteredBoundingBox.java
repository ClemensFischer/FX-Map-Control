/*
 * FX Map Control - https://github.com/ClemensFischer/FX-Map-Control
 * © 2017 Clemens Fischer
 */
package fxmapcontrol;

/**
 */
public class CenteredBoundingBox extends MapBoundingBox {

    private final Location center;
    private final double width;
    private final double height;

    public CenteredBoundingBox(Location center, double width, double height) {
        this.center = center;
        this.width = width;
        this.height = height;
    }

    public Location getCenter() {
        return center;
    }

    @Override
    public double getWidth() {
        return width;
    }

    @Override
    public double getHeight() {
        return height;
    }

    @Override
    public MapBoundingBox clone() {
        return new CenteredBoundingBox(center, width, height);
    }
}
