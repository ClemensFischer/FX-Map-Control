/*
 * FX Map Control - https://github.com/ClemensFischer/FX-Map-Control
 * © 2020 Clemens Fischer
 */
package fxmapcontrol;

import java.util.Collection;
import java.util.List;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.StrokeLineJoin;

/**
 * A Polygon with points given as geographic positions by the locations property. The optional location property helps
 * to calculate a viewport position that is nearest to the map center, in the same way as it is done for MapItems.
 */
public class MapPolygon extends Polygon implements IMapNode {

    private final ListProperty<Location> locationsProperty = new SimpleListProperty<>(this, "locations", FXCollections.observableArrayList());
    private final ObjectProperty<Location> locationProperty = new SimpleObjectProperty<>(this, "location");
    private final MapNodeHelper mapNodeHelper = new MapNodeHelper(e -> updatePoints());

    public MapPolygon() {
        getStyleClass().add("map-polygon");
        setFill(null);
        setStrokeLineJoin(StrokeLineJoin.ROUND);
        locationsProperty.addListener((ListChangeListener.Change<? extends Location> c) -> updatePoints());
    }

    public MapPolygon(Collection<Location> locations) {
        this();
        getLocations().addAll(locations);
    }

    @Override
    public final MapBase getMap() {
        return mapNodeHelper.getMap();
    }

    @Override
    public void setMap(MapBase map) {
        mapNodeHelper.setMap(map);
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

    public final ObjectProperty<Location> locationProperty() {
        return locationProperty;
    }

    public final Location getLocation() {
        return locationProperty.get();
    }

    public final void setLocation(Location location) {
        locationProperty.set(location);
    }

    private void updatePoints() {
        List<Double> points = MapPolyline.updatePoints(getMap(), getLocation(), getLocations());
        
        if (points != null) {
            getPoints().setAll(points);
        } else {
            getPoints().setAll(new Double[] { 0d, 0d }); // clear() or empty collection is ignored
        }
    }
}
