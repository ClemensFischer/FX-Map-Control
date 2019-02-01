/*
 * FX Map Control - https://github.com/ClemensFischer/FX-Map-Control
 * © 2019 Clemens Fischer
 */
package fxmapcontrol;

import javafx.beans.DefaultProperty;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Parent;

/**
 * Extendable base class for map overlays.
 */
@DefaultProperty(value="children")
public class MapLayer extends Parent implements IMapNode {

    private final MapNodeHelper mapNode = new MapNodeHelper(e -> viewportChanged());

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
    }
}
