/*
 * FX Map Control - https://github.com/ClemensFischer/FX-Map-Control
 * © 2020 Clemens Fischer
 */
package fxmapcontrol;

import javafx.beans.DefaultProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Point2D;

/**
 * Base class for map items and overlays. If the location property is set to a non-null value, the
 * MapNode is placed at the appropriate map viewport position, otherwise at the top left corner.
 */
@DefaultProperty(value = "children")
public class MapNode extends MapLayer {

    private final ObjectProperty<Location> locationProperty = new SimpleObjectProperty<>(this, "location");

    public MapNode() {
        getStyleClass().add("map-node");
        getChildren().addListener(new MapNodeHelper.ChildrenListener(this));
        locationProperty.addListener((observable, oldValue, newValue) -> updateViewportPosition());
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
    protected void onViewportChanged() {
        updateViewportPosition();
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

    private void updateViewportPosition() {
        Location location = getLocation();
        Point2D viewportPosition = null;
        MapBase map;

        if (location != null && (map = getMap()) != null) {
            viewportPosition = map.locationToView(location);

            if (viewportPosition.getX() < 0d || viewportPosition.getX() > map.getWidth()
                    || viewportPosition.getY() < 0d || viewportPosition.getY() > map.getHeight()) {

                viewportPosition = map.locationToView(new Location(
                        location.getLatitude(),
                        Location.nearestLongitude(location.getLongitude(), map.getCenter().getLongitude())));
            }
        }

        viewportPositionChanged(viewportPosition);
    }
}
