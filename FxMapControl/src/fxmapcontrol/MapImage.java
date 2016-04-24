/*
 * FX Map Control - https://github.com/ClemensFischer/FX-Map-Control
 * © 2016 Clemens Fischer
 */
package fxmapcontrol;

import javafx.beans.InvalidationListener;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Point2D;
import javafx.scene.image.ImageView;
import javafx.scene.transform.Transform;

/**
 * Fills a rectangular map area defined by the south, north, west and east properties with an Image.
 */
public class MapImage extends ImageView implements IMapNode {

    private final ObjectProperty<Location> southWestProperty = new SimpleObjectProperty<>(this, "southWest");
    private final ObjectProperty<Location> northEastProperty = new SimpleObjectProperty<>(this, "northEast");
    private MapBase map;

    public MapImage() {
        setMouseTransparent(true);

        InvalidationListener listener = observable -> updateLayout();
        southWestProperty.addListener(listener);
        northEastProperty.addListener(listener);
    }

    @Override
    public final MapBase getMap() {
        return map;
    }

    @Override
    public final void setMap(MapBase map) {
        this.map = map;
        updateLayout();
    }

    public final ObjectProperty<Location> southWestProperty() {
        return southWestProperty;
    }

    public final Location getSouthWest() {
        return southWestProperty.get();
    }

    public final void setSouthWest(Location southWest) {
        southWestProperty.set(southWest);
    }

    public final ObjectProperty<Location> northEastProperty() {
        return northEastProperty;
    }

    public final Location getNorthEast() {
        return northEastProperty.get();
    }

    public final void setNorthEast(Location northEast) {
        northEastProperty.set(northEast);
    }

    public final void setBoundingBox(double south, double west, double north, double east) {
        MapBase m = map;
        map = null;
        setSouthWest(new Location(south, west));
        setNorthEast(new Location(north, east));
        map = m;
        updateLayout();
    }

    private void updateLayout() {
        if (map != null) {
            getTransforms().clear();
            if (getSouthWest() != null && getNorthEast() != null) {
                Point2D p1 = map.getMapTransform().transform(getSouthWest());
                Point2D p2 = map.getMapTransform().transform(getNorthEast());
                getTransforms().add(map.getViewportTransform());
                getTransforms().add(Transform.scale(1d, -1d, 0d, p2.getY()));
                setX(p1.getX());
                setY(p2.getY());
                setFitWidth(p2.getX() - p1.getX());
                setFitHeight(p2.getY() - p1.getY());
            } else {
                setX(0);
                setY(0);
                setFitWidth(0);
                setFitHeight(0);
            }
        }
    }
}
