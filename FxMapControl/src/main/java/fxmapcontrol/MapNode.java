/*
 * FX Map Control - https://github.com/ClemensFischer/FX-Map-Control
 * © 2016 Clemens Fischer
 */
package fxmapcontrol;

import javafx.beans.DefaultProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Parent;

/**
 * Base class for map items and overlays. If the location property is set to a non-null value, the
 * MapNode is placed at the appropriate map viewport position, otherwise at the top left corner.
 */
@DefaultProperty(value = "children")
public class MapNode extends Parent implements IMapNode {

    private final MapNodeHelper mapNode = new MapNodeHelper(e -> viewportChanged());
    private final ObjectProperty<Location> locationProperty = new SimpleObjectProperty<>(this, "location");

    public MapNode() {
        getStyleClass().add("map-node");
        getChildren().addListener(new MapNodeHelper.ChildrenListener(this));
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
    public final ObservableList<Node> getChildren() {
        return super.getChildren();
    }

    @Override
    public final MapBase getMap() {
        return mapNode.getMap();
    }

    @Override
    public void setMap(MapBase map) {
        mapNode.setMap(map);

        getChildren().stream()
                .filter(node -> node instanceof IMapNode)
                .forEach(node -> ((IMapNode) node).setMap(map));

        viewportChanged();
    }

    protected void viewportChanged() {
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
            viewportPosition = map.getProjection().locationToViewportPoint(location);

            if (viewportPosition.getX() < 0d || viewportPosition.getX() > map.getWidth()
                    || viewportPosition.getY() < 0d || viewportPosition.getY() > map.getHeight()) {

                viewportPosition = map.getProjection().locationToViewportPoint(new Location(
                        location.getLatitude(),
                        Location.nearestLongitude(location.getLongitude(), map.getCenter().getLongitude())));
            }
        }

        viewportPositionChanged(viewportPosition);
    }
}
