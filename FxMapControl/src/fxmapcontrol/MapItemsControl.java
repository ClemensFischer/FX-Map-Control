/*
 * FX Map Control - https://github.com/ClemensFischer/FX-Map-Control
 * © 2015 Clemens Fischer
 */
package fxmapcontrol;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Parent;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.SelectionMode;

/**
 * Manages a collection of selectable items on a map. Uses MapItem as item container node class.
 */
public class MapItemsControl extends Parent implements IMapNode {

    private final ObjectProperty<ObservableList<MapItem>> itemsProperty
            = new SimpleObjectProperty<>(this, "items", FXCollections.<MapItem>observableArrayList());

    private final SelectionModel selectionModel = new SelectionModel();
    private final MapItemSelectedChangeListener mapItemSelectedChangeListener = new MapItemSelectedChangeListener();
    private MapBase map;

    public MapItemsControl() {
        ListChangeListener<MapItem> itemsChangeListener = c -> {
            while (c.next()) {
                if (c.wasRemoved()) {
                    removeChildren(c.getRemoved());
                }
                if (c.wasAdded()) {
                    addChildren(c.getAddedSubList());
                }
            }
        };

        getItems().addListener(itemsChangeListener);

        itemsProperty.addListener((observable, oldValue, newValue) -> {
            if (oldValue != null) {
                oldValue.removeListener(itemsChangeListener);
                clearChildren();
            }
            if (newValue != null) {
                addChildren(newValue);
                newValue.addListener(itemsChangeListener);
            }
        });
    }

    @Override
    public MapBase getMap() {
        return map;
    }

    @Override
    public void setMap(MapBase map) {
        this.map = map;

        getChildren().stream().forEach(item -> ((MapItem) item).setMap(map));
    }

    public final ObjectProperty<ObservableList<MapItem>> itemsProperty() {
        return itemsProperty;
    }

    public final ObservableList<MapItem> getItems() {
        return itemsProperty.get();
    }

    public final void setItems(ObservableList<MapItem> items) {
        itemsProperty.set(items);
    }

    public final MultipleSelectionModel<MapItem> getSelectionModel() {
        return selectionModel;
    }

    private void addChildren(Collection<? extends MapItem> items) {
        items.stream().forEach(item -> {
            if (item.isSelected()) {
                item.setSelected(false);
            }
            item.selectedProperty().addListener(mapItemSelectedChangeListener);
            item.setMap(map);
        });

        getChildren().addAll(items);
        selectionModel.updateIndices();
    }

    private void removeChildren(Collection<? extends MapItem> items) {
        items.stream().forEach(item -> {
            item.selectedProperty().removeListener(mapItemSelectedChangeListener);
            item.setMap(null);
        });

        getChildren().removeAll(items);
        selectionModel.removeItems(items);
    }

    private void clearChildren() {
        getChildren().stream().forEach(item -> {
            ((MapItem) item).selectedProperty().removeListener(mapItemSelectedChangeListener);
            ((MapItem) item).setMap(null);
        });

        getChildren().clear();
        selectionModel.clearItems();
    }

    private class MapItemSelectedChangeListener implements ChangeListener<Boolean> {

        @Override
        public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
            ReadOnlyProperty<? extends Boolean> property = (ReadOnlyProperty<? extends Boolean>) observable;
            MapItem mapItem = (MapItem) property.getBean();
            int index = getItems().indexOf(mapItem);

            if (newValue) {
                selectionModel.select(index);
                index = getChildren().size() - 1; // to front
            } else {
                selectionModel.clearSelection(index);
            }

            getChildren().remove(mapItem);
            getChildren().add(index, mapItem);
        }
    }

    private class SelectionModel extends MultipleSelectionModel<MapItem> {

        private final ObservableList<Integer> selectedIndices = FXCollections.<Integer>observableArrayList();
        private final ObservableList<MapItem> selectedItems = FXCollections.<MapItem>observableArrayList();
        private boolean selectionChanging;

        void updateIndices() {
            if (selectedItems.isEmpty()) {
                selectedIndices.clear();
                setSelectedIndex(-1);
                setSelectedItem(null);
            } else {
                selectedIndices.setAll(selectedItems.stream().map(item -> getItems().indexOf(item)).collect(Collectors.toList()));
                setSelectedIndex(selectedIndices.get(0));
                setSelectedItem(selectedItems.get(0));
            }
        }

        void removeItems(Collection<? extends MapItem> items) {
            selectedItems.removeAll(items);
            updateIndices();
        }

        void clearItems() {
            selectedIndices.clear();
            selectedItems.clear();
            setSelectedIndex(-1);
            setSelectedItem(null);
        }

        @Override
        public ObservableList<Integer> getSelectedIndices() {
            return selectedIndices;
        }

        @Override
        public ObservableList<MapItem> getSelectedItems() {
            return selectedItems;
        }

        @Override
        public boolean isEmpty() {
            return selectedIndices.isEmpty();
        }

        @Override
        public boolean isSelected(int index) {
            return selectedIndices.contains(index);
        }

        @Override
        public void clearSelection() {
            if (!selectionChanging) {
                selectionChanging = true;

                selectedIndices.clear();
                selectedItems.stream().forEach(item -> item.setSelected(false));
                selectedItems.clear();
                setSelectedIndex(-1);
                setSelectedItem(null);

                selectionChanging = false;
            }
        }

        @Override
        public void clearSelection(int index) {
            final int i;

            if (!selectionChanging
                    && index >= 0
                    && index < getItems().size()
                    && (i = selectedIndices.indexOf(index)) >= 0) {
                selectionChanging = true;

                final MapItem item = getItems().get(index);
                item.setSelected(false);

                selectedIndices.remove(i);
                selectedItems.remove(i);

                if (i == 0) {
                    if (selectedIndices.size() > 0) {
                        setSelectedIndex(selectedIndices.get(0));
                        setSelectedItem(selectedItems.get(0));
                    } else {
                        setSelectedIndex(-1);
                        setSelectedItem(null);
                    }
                }

                selectionChanging = false;
            }
        }

        @Override
        public void clearAndSelect(int index) {
            if (!selectionChanging) {
                selectionChanging = true;

                selectedIndices.clear();
                selectedItems.stream().forEach(item -> item.setSelected(false));
                selectedItems.clear();

                if (index >= 0 && index < getItems().size()) {
                    final MapItem item = getItems().get(index);
                    if (!item.isSelected()) {
                        item.setSelected(true);
                    }

                    selectedIndices.add(index);
                    selectedItems.add(item);
                    setSelectedIndex(index);
                    setSelectedItem(item);
                } else {
                    setSelectedIndex(-1);
                    setSelectedItem(null);
                }

                selectionChanging = false;
            }
        }

        @Override
        public void select(int index) {
            int i;

            if (!selectionChanging
                    && index >= 0
                    && index < getItems().size()
                    && (i = -Collections.binarySearch(selectedIndices, index) - 1) >= 0) { // not in selectedIndices
                selectionChanging = true;

                if (getSelectionMode() == SelectionMode.SINGLE) {
                    selectedIndices.clear();
                    selectedItems.stream().forEach(item -> item.setSelected(false));
                    selectedItems.clear();
                    i = 0;
                }

                final MapItem item = getItems().get(index);
                if (!item.isSelected()) {
                    item.setSelected(true);
                }

                selectedIndices.add(i, index);
                selectedItems.add(i, item);
                setSelectedIndex(selectedIndices.get(0));
                setSelectedItem(selectedItems.get(0));

                selectionChanging = false;
            }
        }

        @Override
        public void selectIndices(int index, int... indices) {
            select(index);
            for (int i : indices) {
                select(i);
            }
        }

        @Override
        public void select(MapItem item) {
            select(getItems().indexOf(item));
        }

        @Override
        public void selectAll() {
        }

        @Override
        public void selectFirst() {
            select(0);
        }

        @Override
        public void selectLast() {
            select(getItems().size() - 1);
        }

        @Override
        public void selectPrevious() {
        }

        @Override
        public void selectNext() {
        }
    }
}
