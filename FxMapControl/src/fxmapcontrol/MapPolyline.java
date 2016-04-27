/*
 * FX Map Control - https://github.com/ClemensFischer/FX-Map-Control
 * © 2016 Clemens Fischer
 */
package fxmapcontrol;

import java.util.ArrayList;
import java.util.Collection;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.StrokeLineJoin;

/**
 * A Polyline with points given as geographic positions by the locations property. The optional location property helps
 * to calculate a viewport position that is nearest to the map center, in the same way as it is done for MapItems.
 */
public class MapPolyline extends Polyline implements IMapNode {

    private final ListProperty<Location> locationsProperty = new SimpleListProperty<>(this, "locations", FXCollections.observableArrayList());
    private final ObjectProperty<Location> locationProperty = new SimpleObjectProperty<>(this, "location");
    private final MapNodeHelper mapNode = new MapNodeHelper(e -> updatePoints());

    public MapPolyline() {
        getStyleClass().add("map-polyline");
        setFill(null);
        setStrokeLineJoin(StrokeLineJoin.ROUND);

        locationsProperty.addListener((ListChangeListener.Change<? extends Location> c) -> updatePoints());
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
    public void setMap(MapBase map) {
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
        MapBase map = getMap();
        ObservableList<Location> locations = getLocations();

        if (map != null && locations != null && locations.size() > 0) {
            ArrayList<Double> locationPoints = new ArrayList<>(locations.size() * 2);
            double longitudeOffset;

            if (getLocation() != null) {
                double longitude = Location.normalizeLongitude(getLocation().getLongitude());
                longitudeOffset = Location.nearestLongitude(longitude, map.getCenter().getLongitude()) - longitude;
            } else {
                longitudeOffset = 0d;
            }

            locations.forEach(location -> {
                Point2D p = map.locationToViewportPoint(
                        new Location(location.getLatitude(), location.getLongitude() + longitudeOffset));
                locationPoints.add(p.getX());
                locationPoints.add(p.getY());
            });

            getPoints().setAll(locationPoints);

        } else {
            getPoints().clear();
        }
    }
}
