/*
 * FX Map Control - https://github.com/ClemensFischer/FX-Map-Control
 * © 2020 Clemens Fischer
 */
package fxmapcontrol;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
 * A Polyline with points given as geographic positions by the locations property. The optional
 * location property helps to calculate a viewport position that is nearest to the map center, in
 * the same way as it is done for MapItems.
 */
public class MapPolyline extends Polyline implements IMapNode {

    private final ListProperty<Location> locationsProperty = new SimpleListProperty<>(this, "locations", FXCollections.observableArrayList());
    private final ObjectProperty<Location> locationProperty = new SimpleObjectProperty<>(this, "location");
    private final MapNodeHelper mapNodeHelper = new MapNodeHelper(e -> updatePoints());

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
        List<Double> points = updatePoints(getMap(), getLocation(), getLocations());
        
        if (points != null) {
            getPoints().setAll(points);
        } else {
            getPoints().setAll(new Double[] { 0d, 0d }); // clear() or empty collection is ignored
        }
    }

    static List<Double> updatePoints(MapBase map, Location location, ObservableList<Location> locations) {
        ArrayList<Double> points = null;
        
        if (map != null && locations != null && !locations.isEmpty()) {
            double longitudeOffset = 0d;

            if (location != null && map.getProjection().isNormalCylindrical()) {
                Point2D viewportPosition = map.locationToView(location);

                if (viewportPosition.getX() < 0d || viewportPosition.getX() > map.getWidth()
                        || viewportPosition.getY() < 0d || viewportPosition.getY() > map.getHeight()) {

                    double nearestLongitude = Location.nearestLongitude(location.getLongitude(), map.getCenter().getLongitude());
                    longitudeOffset = nearestLongitude - location.getLongitude();
                }
            }

            points = new ArrayList<>(locations.size() * 2);

            for (Location loc : locations) {
                Point2D p = map.locationToView(
                        new Location(loc.getLatitude(), loc.getLongitude() + longitudeOffset));

                if (Double.isInfinite(p.getX()) || Double.isInfinite(p.getY())) {
                    points = null;
                    break;
                }

                points.add(p.getX());
                points.add(p.getY());
            }
        }
        
        return points;
    }
}
