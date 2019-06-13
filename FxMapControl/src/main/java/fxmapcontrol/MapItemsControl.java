/*
 * FX Map Control - https://github.com/ClemensFischer/FX-Map-Control
 * © 2019 Clemens Fischer
 */
package fxmapcontrol;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Parent;
import javafx.scene.control.SelectionMode;
import javafx.util.Callback;

/**
 * Manages a collection of selectable items on a map. Uses MapItem as item container node class.
 *
 * @param <T> the (element) type of the {@code items}, {@code selectedItem} and {@code selectedItems} properties.
 * If T does not extend {@link MapItem}, an item generator callback must be assigned to the {@code itemGenerator} property.
 */
public class MapItemsControl<T> extends Parent implements IMapNode {

    private final ListProperty<T> itemsProperty = new SimpleListProperty<>(this, "items", FXCollections.observableArrayList());
    private final ObjectProperty<SelectionMode> selectionModeProperty = new SimpleObjectProperty<>(this, "selectionMode", SelectionMode.SINGLE);
    private final ObjectProperty<T> selectedItemProperty = new SimpleObjectProperty<>(this, "selectedItem");
    private final ObservableList<T> selectedItems = FXCollections.observableArrayList();
    private Callback<T, MapItem<T>> itemGenerator;
    private MapBase map;
    private boolean selectionChanging;

    private final ChangeListener<Boolean> itemSelectedListener = (observable, oldValue, newValue) -> {
        if (!selectionChanging) {
            selectionChanging = true;

            MapItem<T> mapItem = (MapItem<T>) ((ReadOnlyProperty) observable).getBean();
            T item = mapItem.getItem();

            if (item == null) {
                item = (T) mapItem;
            }

            if (newValue) {
                addSelectedItem(item);
            } else {
                removeSelectedItem(item);
            }

            selectionChanging = false;
        }
    };

    private final ChangeListener itemZIndexListener = (observable, oldValue, newValue) -> {
        MapItem mapItem = (MapItem) ((ReadOnlyProperty) observable).getBean();
        int oldIndex = getChildren().indexOf(mapItem);
        int newIndex = getItemIndex(mapItem);

        if (newIndex != oldIndex) {
            getChildren().remove(oldIndex);
            getChildren().add(newIndex, mapItem);
        }
    };

    public MapItemsControl() {
        getStyleClass().add("map-items-control");

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
    public final Callback<T, MapItem<T>> getItemGenerator() {
        return itemGenerator;
    }

    /**
     * Sets the {@code Callback<T, MapItem>} that generates a MapItem if T does not extend MapItem.
     *
     * @param itemGenerator the {@code Callback<T, MapItem>}
     */
    public final void setItemGenerator(Callback<T, MapItem<T>> itemGenerator) {
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
        selectedItemProperty.set(getItems().contains(selectedItem) ? selectedItem : null);
    }

    public final ObservableList<T> getSelectedItems() {
        return selectedItems;
    }

    public final MapItem getMapItem(T item) {
        return item instanceof MapItem
                ? (MapItem) item
                : (MapItem) getChildren().stream()
                .filter(node -> ((MapItem) node).getItem() == item)
                .findFirst().orElse(null);
    }

    private MapItem createMapItem(T item) {
        return item instanceof MapItem
                ? (MapItem) item
                : (itemGenerator != null ? itemGenerator.call(item) : null);
    }

    private void setItemSelected(T item, boolean selected) {
        if (getItems().contains(item)) {
            MapItem mapItem = getMapItem(item);
            if (mapItem != null) {
                mapItem.setSelected(selected);
            }
        }
    }

    private int getItemIndex(MapItem mapItem) {
        List<Integer> zIndexes = getChildren().stream()
                .filter(i -> i != mapItem)
                .map(i -> ((MapItem) i).getZIndex())
                .collect(Collectors.toList());

        int index = Collections.binarySearch(zIndexes, mapItem.getZIndex(), (value1, value2) -> value1 - value2);

        return index >= 0 ? index : (-index - 1);
    }

    private void addChildren(Collection<? extends T> items) {
        items.forEach((item) -> {
            MapItem mapItem = createMapItem(item);
            if (mapItem != null) {
                mapItem.setMap(map);

                if (mapItem.isSelected()) {
                    addSelectedItem(item);
                }

                mapItem.zIndexProperty().addListener(itemZIndexListener);
                mapItem.selectedProperty().addListener(itemSelectedListener);
                getChildren().add(getItemIndex(mapItem), mapItem);
            }
        });
    }

    private void removeChildren(Collection<? extends T> items) {
        items.stream().forEach((item) -> {
            MapItem mapItem = getMapItem(item);
            if (mapItem != null) {
                mapItem.setMap(null);
                mapItem.zIndexProperty().removeListener(itemZIndexListener);
                mapItem.selectedProperty().removeListener(itemSelectedListener);
                getChildren().remove(mapItem);
            }
            removeSelectedItem(item);
        });
    }

    private void addSelectedItem(T item) {
        if (getItems().contains(item) && !selectedItems.contains(item)) {
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
            setSelectedItem(selectedItems.isEmpty() ? null : selectedItems.get(0));
        }
    }
}
