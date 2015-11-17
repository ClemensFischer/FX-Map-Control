/*
 * FX Map Control - https://github.com/ClemensFischer/FX-Map-Control
 * © 2015 Clemens Fischer
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
    private final BooleanProperty hideOutsideViewportProperty = new SimpleBooleanProperty(this, "hideOutsideViewport");
    private final BooleanProperty selectedProperty = new SimpleBooleanProperty(this, "selected");
    private final MapNodeHelper mapNode = new MapNodeHelper(e -> viewportTransformChanged());
    private Point2D position;
    private Point2D viewportPosition;

    public MapItem() {
        getChildren().addListener(new MapNodeHelper.ChildrenListener(this));

        locationProperty.addListener((observable, oldValue, newValue) -> setPosition());

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
    public final void setMap(MapBase map) {
        mapNode.setMap(map);

        getChildren().stream()
                .filter(node -> node instanceof IMapNode)
                .forEach(node -> ((IMapNode) node).setMap(map));

        setPosition();
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

    public final BooleanProperty hideOutsideViewportProperty() {
        return hideOutsideViewportProperty;
    }

    public final boolean getHideOutsideViewport() {
        return hideOutsideViewportProperty.get();
    }

    public final void setHideOutsideViewport(boolean hideOutsideViewport) {
        hideOutsideViewportProperty.set(hideOutsideViewport);
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

    public final Point2D getViewportPosition() {
        return viewportPosition;
    }

    protected void viewportTransformChanged() {
        if (position != null) {
            final MapBase map = getMap();
            viewportPosition = map.getViewportTransform().transform(position);

            setTranslateX(viewportPosition.getX());
            setTranslateY(viewportPosition.getY());

            if (getHideOutsideViewport()) {
                setVisible(viewportPosition.getX() >= 0d
                        && viewportPosition.getY() >= 0d
                        && viewportPosition.getX() <= map.getWidth()
                        && viewportPosition.getY() <= map.getHeight());
            }
        }
    }

    private void setPosition() {
        final MapBase map = getMap();
        final Location location = getLocation();

        if (map != null && location != null) {
            position = map.getMapTransform().transform(location);
            viewportTransformChanged();

        } else if (position != null) {
            position = null;
            viewportPosition = null;

            setTranslateX(0d);
            setTranslateY(0d);

            if (getHideOutsideViewport()) {
                setVisible(true);
            }
        }
    }
}
