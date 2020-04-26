/*
 * FX Map Control - https://github.com/ClemensFischer/FX-Map-Control
 * © 2020 Clemens Fischer
 */
package fxmapcontrol;

import javafx.beans.DefaultProperty;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Parent;

/**
 * Extendable base class for map overlays.
 */
@DefaultProperty(value = "children")
public class MapLayer extends Parent implements IMapNode {

    private final MapNodeHelper mapNodeHelper = new MapNodeHelper(e -> onViewportChanged());

    public MapLayer() {
        getStyleClass().add("map-layer");
        getChildren().addListener(new MapNodeHelper.ChildrenListener(this));
    }

    @Override
    public final ObservableList<Node> getChildren() {
        return super.getChildren();
    }

    @Override
    public final MapBase getMap() {
        return mapNodeHelper.getMap();
    }

    @Override
    public void setMap(MapBase map) {
        mapNodeHelper.setMap(map);
        getChildren().stream()
                .filter(node -> node instanceof IMapNode)
                .forEach(node -> ((IMapNode) node).setMap(map));
        onViewportChanged();
    }

    protected void onViewportChanged() {
    }
}
