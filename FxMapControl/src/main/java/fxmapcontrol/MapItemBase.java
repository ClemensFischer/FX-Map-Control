/*
 * FX Map Control - https://github.com/ClemensFischer/FX-Map-Control
 * © 2016 Clemens Fischer
 */
package fxmapcontrol;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Point2D;

/**
 * Extendable base class for a map item with a location.
 */
public class MapItemBase extends MapLayer {
    
    private final ObjectProperty<Location> locationProperty = new SimpleObjectProperty<>(this, "location");

    public MapItemBase() {
        getStyleClass().add("map-item-base");
        locationProperty.addListener(observable -> updateViewportPosition());
    }

    public final ObjectProperty<Location> locationProperty() {
        return locationProperty;
    }

    public final Location getLocation() {
        return locationProperty.get();
    }

    public final void setLocation(Location location) {
        locationProperty.set(location);
    }

    @Override
    public void setMap(MapBase map) {
        super.setMap(map);
        viewportChanged();
    }

    @Override
    protected void viewportChanged() {
        updateViewportPosition();
    }

    protected final void updateViewportPosition() {
        Location location = getLocation();
        Point2D viewportPosition = null;
        MapBase map;

        if (location != null && (map = getMap()) != null) {
            viewportPosition = map.getProjection().locationToViewportPoint(new Location(
                    location.getLatitude(),
                    Location.nearestLongitude(location.getLongitude(), map.getCenter().getLongitude())));
        }

        viewportPositionChanged(viewportPosition);
    }

    protected void viewportPositionChanged(Point2D viewportPosition) {
        if (viewportPosition != null) {
            setTranslateX(viewportPosition.getX());
            setTranslateY(viewportPosition.getY());
        } else {
            setTranslateX(0d);
            setTranslateY(0d);
        }
    }
}
