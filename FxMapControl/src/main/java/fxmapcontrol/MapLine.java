/*
 * FX Map Control - https://github.com/ClemensFischer/FX-Map-Control
 * © 2019 Clemens Fischer
 */
package fxmapcontrol;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Point2D;
import javafx.scene.shape.Line;

/**
 * A Line with start and end points given as geographic positions.
 */
public class MapLine extends Line implements IMapNode {

    private final ObjectProperty<Location> startLocationProperty = new SimpleObjectProperty<>(this, "startLocation");
    private final ObjectProperty<Location> endLocationProperty = new SimpleObjectProperty<>(this, "endLocation");
    private final MapNodeHelper mapNode = new MapNodeHelper(e -> updatePoints());
    
    public MapLine() {
        getStyleClass().add("map-line");

        startLocationProperty.addListener((observable, oldValue, newValue) -> updateStartPoint());
        endLocationProperty.addListener((observable, oldValue, newValue) -> updateEndPoint());
    }
    
    public MapLine(Location start, Location end) {
        this();
        setStartLocation(start);
        setEndLocation(end);
    }

    @Override
    public MapBase getMap() {
        return mapNode.getMap();
    }

    @Override
    public void setMap(MapBase map) {
        mapNode.setMap(map);
        updatePoints();
    }

    public final ObjectProperty<Location> startLocationProperty() {
        return startLocationProperty;
    }

    public final Location getStartLocation() {
        return startLocationProperty.get();
    }

    public final void setStartLocation(Location location) {
        startLocationProperty.set(location);
    }

    public final ObjectProperty<Location> endLocationProperty() {
        return endLocationProperty;
    }

    public final Location getEndLocation() {
        return endLocationProperty.get();
    }

    public final void setEndLocation(Location location) {
        endLocationProperty.set(location);
    }

    private void updatePoints() {
        updateStartPoint();
        updateEndPoint();
    }

    private void updateStartPoint() {
        MapBase map = getMap();
        Location start = getStartLocation();
        if (map != null && start != null) {
            Point2D p = map.getProjection().locationToViewportPoint(start);
            setStartX(p.getX());
            setStartY(p.getY());
            setVisible(getEndLocation() != null);
        } else {
            setVisible(false);
            setStartX(0);
            setStartY(0);
        }
    }

    private void updateEndPoint() {
        MapBase map = getMap();
        Location end = getEndLocation();
        if (map != null && end != null) {
            Point2D p = map.getProjection().locationToViewportPoint(end);
            setEndX(p.getX());
            setEndY(p.getY());
            setVisible(getStartLocation() != null);
        } else {
            setVisible(false);
            setEndX(0);
            setEndY(0);
        }
    }
}
