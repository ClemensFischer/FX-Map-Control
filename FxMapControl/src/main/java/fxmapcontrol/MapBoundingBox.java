/*
 * FX Map Control - https://github.com/ClemensFischer/FX-Map-Control
 * © 2020 Clemens Fischer
 */
package fxmapcontrol;

/**
 * A bounding box with south and north latitude and west and east longitude values in degrees.
 */
public class MapBoundingBox {

    private double south;
    private double west;
    private double north;
    private double east;

    public MapBoundingBox() {
    }

    public MapBoundingBox(double south, double west, double north, double east) {
        this.south = Math.min(Math.max(south, -90d), 90d);
        this.west = west;
        this.north = Math.min(Math.max(north, -90d), 90d);
        this.east = east;
    }

    public double getSouth() {
        return south;
    }

    public void setSouth(double south) {
        this.south = Math.min(Math.max(south, -90d), 90d);
    }

    public double getWest() {
        return west;
    }

    public void setWest(double west) {
        this.west = west;
    }

    public double getNorth() {
        return north;
    }

    public void setNorth(double north) {
        this.north = Math.min(Math.max(north, -90d), 90d);
    }

    public double getEast() {
        return east;
    }

    public void setEast(double east) {
        this.east = east;
    }

    public double getWidth() {
        return east - west;
    }

    public double getHeight() {
        return north - south;
    }

    public boolean hasValidBounds() {
        return south < north && west < east;
    }

    @Override
    public MapBoundingBox clone() {
        return new MapBoundingBox(south, west, north, east);
    }
}
