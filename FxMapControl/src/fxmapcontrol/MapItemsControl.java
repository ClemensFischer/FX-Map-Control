/*
 * FX Map Control - https://github.com/ClemensFischer/FX-Map-Control
 * © 2016 Clemens Fischer
 */
package fxmapcontrol;

import java.util.Collection;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Parent;
import javafx.scene.control.SelectionMode;
import javafx.util.Callback;

/**
 * Manages a collection of selectable items on a map. Uses MapItem as item container node class.
 *
 * @param <T> the (element) type of the {@code items}, {@code selectedItem} and
 * {@code selectedItems} properties. If T does not extend {@link MapItem}, an item generator
 * callback must be assigned to the {@code itemGenerator} property.
 */
public class MapItemsControl<T> extends Parent implements IMapNode {

    private final ListProperty<T> itemsProperty = new SimpleListProperty<>(this, "items", FXCollections.observableArrayList());
    private final ObjectProperty<SelectionMode> selectionModeProperty = new SimpleObjectProperty<>(this, "selectionMode", SelectionMode.SINGLE);
    private final ObjectProperty<T> selectedItemProperty = new SimpleObjectProperty<>(this, "selectedItem");
    private final ObservableList<T> selectedItems = FXCollections.observableArrayList();
    private final MapItemSelectedListener itemSelectedListener = new MapItemSelectedListener();
    private Callback<T, MapItem> itemGenerator;
    private MapBase map;

    public MapItemsControl() {
        itemsProperty.addListener((ListChangeListener.Change<? extends T> change) -> {
            while (change.next()) {
                if (change.wasRemoved()) {
                    removeChildren(change.getRemoved());
                }
                if (change.wasAdded()) {
                    addChildren(change.getAddedSubList());
                }
            }
        });

        selectedItemProperty.addListener((observable, oldValue, newValue) -> {
            if (oldValue != null && getSelectionMode() == SelectionMode.SINGLE) {
                setItemSelected(oldValue, false);
            }
            if (newValue != null) {
                setItemSelected(newValue, true);
            }
        });

        selectedItems.addListener((ListChangeListener.Change<? extends T> change) -> {
            while (change.next()) {
                if (change.wasRemoved()) {
                    change.getRemoved().forEach(item -> setItemSelected(item, false));
                }
                if (change.wasAdded()) {
                    change.getAddedSubList().forEach(item -> setItemSelected(item, true));
                }
            }
        });
    }

    @Override
    public final MapBase getMap() {
        return map;
    }

    @Override
    public void setMap(MapBase map) {
        this.map = map;

        getChildren().forEach(item -> ((MapItem) item).setMap(map));
    }

    /**
     * Gets the {@code Callback<T, MapItem>} that generates a MapItem if T does not extend MapItem.
     *
     * @return the {@code Callback<T, MapItem>}
     */
    public final Callback<T, MapItem> getItemGenerator() {
        return itemGenerator;
    }

    /**
     * Sets the {@code Callback<T, MapItem>} that generates a MapItem if T does not extend MapItem.
     *
     * @param itemGenerator the {@code Callback<T, MapItem>}
     */
    public final void setItemGenerator(Callback<T, MapItem> itemGenerator) {
        this.itemGenerator = itemGenerator;
    }

    public final ListProperty<T> itemsProperty() {
        return itemsProperty;
    }

    public final ObservableList<T> getItems() {
        return itemsProperty.get();
    }

    public final void setItems(ObservableList<T> items) {
        itemsProperty.set(items);
    }

    public final ObjectProperty<SelectionMode> selectionModeProperty() {
        return selectionModeProperty;
    }

    public final SelectionMode getSelectionMode() {
        return selectionModeProperty.get();
    }

    public final void setSelectionMode(SelectionMode selectionMode) {
        selectionModeProperty.set(selectionMode);
    }

    public final ObjectProperty<T> selectedItemProperty() {
        return selectedItemProperty;
    }

    public final T getSelectedItem() {
        return selectedItemProperty.get();
    }

    public final void setSelectedItem(T selectedItem) {
        selectedItemProperty.set(selectedItem);
    }

    public final ObservableList<T> getSelectedItems() {
        return selectedItems;
    }

    private MapItem getMapItem(T item) {
        return item instanceof MapItem
                ? (MapItem) item
                : (MapItem) getChildren().stream()
                .filter(node -> ((MapItem) node).getItemData() == item)
                .findFirst().orElse(null);
    }

    private void setItemSelected(T item, boolean selected) {
        MapItem mapItem = getMapItem(item);
        if (mapItem != null) {
            mapItem.setSelected(selected);
        }
    }

    private void addChildren(Collection<? extends T> items) {
        items.forEach(item -> {
            MapItem mapItem = null;
            if (item instanceof MapItem) {
                mapItem = (MapItem) item;
            } else if (itemGenerator != null) {
                mapItem = itemGenerator.call(item);
            }
            if (mapItem != null) {
                mapItem.setItemData(item);
                mapItem.setMap(map);
                mapItem.selectedProperty().addListener(itemSelectedListener);
                getChildren().add(mapItem);
            }
        });
    }

    private void removeChildren(Collection<? extends T> items) {
        items.forEach(item -> {
            MapItem mapItem = getMapItem(item);
            if (mapItem != null) {
                mapItem.selectedProperty().removeListener(itemSelectedListener);
                mapItem.setSelected(false);
                mapItem.setMap(null);
                mapItem.setItemData(null);
                getChildren().remove(mapItem);
                removeSelectedItem(item);
            }
        });
    }

    private void clearChildren() {
        getChildren().forEach(node -> {
            MapItem mapItem = (MapItem) node;
            mapItem.selectedProperty().removeListener(itemSelectedListener);
            mapItem.setSelected(false);
            mapItem.setMap(null);
            mapItem.setItemData(null);
        });

        getChildren().clear();
        selectedItems.clear();
        setSelectedItem(null);
    }

    private void addSelectedItem(T item) {
        if (!selectedItems.contains(item)) {
            if (getSelectionMode() == SelectionMode.SINGLE) {
                selectedItems.clear();
            }
            selectedItems.add(item);
            if (getSelectedItem() == null || getSelectionMode() == SelectionMode.SINGLE) {
                setSelectedItem(item);
            }
        }
    }

    private void removeSelectedItem(T item) {
        if (selectedItems.remove(item) && getSelectedItem() == item) {
            if (selectedItems.isEmpty()) {
                setSelectedItem(null);
            } else {
                setSelectedItem(selectedItems.get(0));
            }
        }
    }

    private class MapItemSelectedListener implements ChangeListener<Boolean> {

        private boolean selectionChanging;

        @Override
        public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
            if (!selectionChanging) {
                selectionChanging = true;

                ReadOnlyProperty<? extends Boolean> property = (ReadOnlyProperty<? extends Boolean>) observable;
                MapItem mapItem = (MapItem) property.getBean();
                T item = (T) mapItem.getItemData();

                getChildren().remove(mapItem);

                if (newValue) {
                    getChildren().add(mapItem); // put on top
                    addSelectedItem(item);
                } else {
                    getChildren().add(getItems().indexOf(item), mapItem);
                    removeSelectedItem(item);
                }

                selectionChanging = false;
            }
        }
    }
}
