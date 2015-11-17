/*
 * FX Map Control - https://github.com/ClemensFischer/FX-Map-Control
 * © 2015 Clemens Fischer
 */
package fxmapcontrol;

import java.util.ArrayList;
import java.util.Collection;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.StrokeLineJoin;

/**
 * A Polyline with points given as geographic positions by the locations property.
 */
public class MapPolyline extends Polyline implements IMapNode {

    private final ListProperty<Location> locationsProperty = new SimpleListProperty<>(this, "locations", FXCollections.observableArrayList());
    private final MapNodeHelper mapNode = new MapNodeHelper(e -> updatePoints());

    public MapPolyline() {
        setFill(null);
        setStrokeLineJoin(StrokeLineJoin.ROUND);

        locationsProperty.addListener((observable, oldValue, newValue) -> {
            updatePoints();
        });
        locationsProperty.addListener((ListChangeListener.Change<? extends Location> change) -> {
            updatePoints();
        });
    }

    public MapPolyline(Collection<Location> locations) {
        this();
        getLocations().addAll(locations);
    }

    @Override
    public final MapBase getMap() {
        return mapNode.getMap();
    }

    @Override
    public final void setMap(MapBase map) {
        mapNode.setMap(map);
        updatePoints();
    }

    public final ListProperty<Location> locationsProperty() {
        return locationsProperty;
    }

    public final ObservableList<Location> getLocations() {
        return locationsProperty.get();
    }

    public final void setLocations(ObservableList<Location> locations) {
        locationsProperty.set(locations);
    }

    private void updatePoints() {
        if (getMap() != null && getLocations() != null && getLocations().size() > 0) {
            ArrayList<Double> locationPoints = new ArrayList<>(getLocations().size() * 2);

            getLocations().forEach(location -> {
                Point2D p = getMap().locationToViewportPoint(location);
                locationPoints.add(p.getX());
                locationPoints.add(p.getY());
            });

            getPoints().setAll(locationPoints);

        } else {
            getPoints().clear();
        }
    }
}
