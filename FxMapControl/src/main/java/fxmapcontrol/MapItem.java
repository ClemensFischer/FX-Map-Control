/*
 * FX Map Control - https://github.com/ClemensFischer/FX-Map-Control
 * © 2016 Clemens Fischer
 */
package fxmapcontrol;

import javafx.beans.DefaultProperty;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

/**
 * Container node of an item in a MapItemsControl.
 *
 * @param <T> the item type of the parent MapItemsControl.
 */
public class MapItem<T> extends MapNode {

    private final BooleanProperty selectedProperty = new SimpleBooleanProperty(this, "selected");
    private final T item;

    public MapItem() {
        this(null, true);
    }

    public MapItem(T item) {
        this(item, true);
    }

    public MapItem(T item, boolean defaultClickBehavior) {
        this.item = item;

        getStyleClass().add("map-item");

        if (defaultClickBehavior) {
            addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
                if (e.getButton() == MouseButton.PRIMARY) {
                    setSelected(!isSelected());
                    e.consume();
                }
            });
        }
    }

    public final T getItem() {
        return item;
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
}
