/*
 * FX Map Control - https://github.com/ClemensFischer/FX-Map-Control
 * © 2019 Clemens Fischer
 */
package fxmapcontrol;

import javafx.collections.ListChangeListener;
import javafx.event.EventHandler;
import javafx.scene.Node;

/**
 * Helper class for implementing the IMapNode interface.
 */
public final class MapNodeHelper implements IMapNode {

    private final EventHandler<ViewportChangedEvent> viewportChangedHandler;
    private MapBase map;

    public MapNodeHelper(EventHandler<ViewportChangedEvent> viewportChangedHandler) {
        this.viewportChangedHandler = viewportChangedHandler;
    }

    @Override
    public final MapBase getMap() {
        return map;
    }

    @Override
    public final void setMap(MapBase map) {
        if (this.map != null) {
            this.map.removeEventHandler(ViewportChangedEvent.VIEWPORT_CHANGED, viewportChangedHandler);
        }

        this.map = map;

        if (this.map != null) {
            this.map.addEventHandler(ViewportChangedEvent.VIEWPORT_CHANGED, viewportChangedHandler);
        }
    }

    public static class ChildrenListener implements ListChangeListener<Node> {

        private final IMapNode mapNode;

        public ChildrenListener(IMapNode parentNode) {
            mapNode = parentNode;
        }

        @Override
        public void onChanged(ListChangeListener.Change<? extends Node> change) {
            while (change.next()) {
                if (change.wasRemoved()) {
                    change.getRemoved().stream()
                            .filter(node -> node instanceof IMapNode)
                            .forEach(node -> ((IMapNode) node).setMap(null));
                }
                if (change.wasAdded()) {
                    change.getAddedSubList().stream()
                            .filter(node -> node instanceof IMapNode)
                            .forEach(node -> ((IMapNode) node).setMap(mapNode.getMap()));

                }
            }
        }
    }
}
