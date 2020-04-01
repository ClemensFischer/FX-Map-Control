/*
 * FX Map Control - https://github.com/ClemensFischer/FX-Map-Control
 * © 2020 Clemens Fischer
 */
package fxmapcontrol;

/**
 * Represents a Node in the visual tree of a MapBase instance.
 */
public interface IMapNode {

    MapBase getMap();

    void setMap(MapBase map);
}
