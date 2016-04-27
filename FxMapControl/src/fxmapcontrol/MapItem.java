/*
 * FX Map Control - https://github.com/ClemensFischer/FX-Map-Control
 * © 2016 Clemens Fischer
 */
package fxmapcontrol;

import javafx.beans.DefaultProperty;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.input.MouseEvent;

/**
 * Container node of an item in a MapItemsControl. The location property specifies the geographic
 * position on the containing MapBase instance.
 */
@DefaultProperty(value = "children")
public class MapItem extends Parent implements IMapNode {

    private final ObjectProperty<Location> locationProperty = new SimpleObjectProperty<>(this, "location");
    private final BooleanProperty selectedProperty = new SimpleBooleanProperty(this, "selected");
    private final MapNodeHelper mapNode = new MapNodeHelper(e -> updateViewportPosition());
    private Object itemData;

    public MapItem() {
        getStyleClass().add("map-item");
        getChildren().addListener(new MapNodeHelper.ChildrenListener(this));

        locationProperty.addListener(observable -> updateViewportPosition());

        addEventHandler(MouseEvent.MOUSE_CLICKED, e -> setSelected(!isSelected()));
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

        updateViewportPosition();
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

    public final BooleanProperty selectedProperty() {
        return selectedProperty;
    }

    public final boolean isSelected() {
        return selectedProperty.get();
    }

    public final void setSelected(boolean selected) {
        selectedProperty.set(selected);
    }

    protected final Object getItemData() {
        return itemData;
    }

    protected final void setItemData(Object itemData) {
        this.itemData = itemData;
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
            viewportPosition = map.locationToViewportPoint(new Location(
                    location.getLatitude(),
                    Location.nearestLongitude(location.getLongitude(), map.getCenter().getLongitude())));
        }
        
        viewportPositionChanged(viewportPosition);
    }
}
