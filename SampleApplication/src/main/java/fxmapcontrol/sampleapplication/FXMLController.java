package fxmapcontrol.sampleapplication;

import fxmapcontrol.AzimuthalEquidistantProjection;
import fxmapcontrol.BingMapsTileLayer;
import fxmapcontrol.EquirectangularProjection;
import fxmapcontrol.GnomonicProjection;
import fxmapcontrol.ImageFileCache;
import fxmapcontrol.Location;
import fxmapcontrol.MapBase;
import fxmapcontrol.MapGraticule;
import fxmapcontrol.MapNode;
import fxmapcontrol.MapPolygon;
import fxmapcontrol.MapProjection;
import fxmapcontrol.StereographicProjection;
import fxmapcontrol.MapTileLayer;
import fxmapcontrol.TileImageLoader;
import fxmapcontrol.WebMercatorProjection;
import fxmapcontrol.WmsImageLayer;
import java.net.URL;
import java.util.HashMap;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;

public class FXMLController implements Initializable {

    @FXML
    private MapBase map;
    @FXML
    private WmsImageLayer wmsLayer;
    @FXML
    private MapGraticule mapGraticule;
    @FXML
    private Slider zoomSlider;
    @FXML
    private Slider headingSlider;
    @FXML
    private CheckBox seamarksCheckBox;
    @FXML
    private ComboBox mapLayerComboBox;
    @FXML
    private ComboBox projectionComboBox;

    @FXML
    private void handlePressed(MouseEvent event) {
        if (event.getTarget() == map && event.getClickCount() == 2) {
            map.setTargetCenter(map.getProjection().viewportPointToLocation(new Point2D(event.getX(), event.getY())));
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        TileImageLoader.setCache(new ImageFileCache());

        map.targetZoomLevelProperty().bindBidirectional(zoomSlider.valueProperty());
        map.targetHeadingProperty().bindBidirectional(headingSlider.valueProperty());

        map.getChildren().remove(wmsLayer);

        HashMap<String, Node> mapLayers = new HashMap<>();
        mapLayers.put("OpenStreetMap", MapTileLayer.getOpenStreetMapLayer());
        //mapLayers.put("Bing Maps Aerial", new BingMapsTileLayer(BingMapsTileLayer.MapMode.Aerial));
        mapLayers.put("Seamarks", new MapTileLayer("Seamarks", "http://tiles.openseamap.org/seamark/{z}/{x}/{y}.png", 9, 18));
        mapLayers.put("OpenStreetMap WMS", new WmsImageLayer("http://ows.terrestris.de/osm/service?LAYERS=OSM-WMS"));

        mapLayerComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                Node mapLayer = mapLayers.get((String) newValue);
                
                if (mapLayers.containsValue(map.getChildren().get(0))) {
                    map.getChildren().set(0, mapLayer);
                } else {
                    map.getChildren().add(0, mapLayer);
                }

                if (mapLayer instanceof BingMapsTileLayer
                        && ((BingMapsTileLayer) mapLayer).getMapMode() != BingMapsTileLayer.MapMode.Road) {
                    map.setBackground(new Background(new BackgroundFill(Color.BLACK, CornerRadii.EMPTY, Insets.EMPTY)));
                    mapGraticule.setStroke(Color.WHITE);
                    mapGraticule.setTextFill(Color.WHITE);
                } else {
                    map.setBackground(new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)));
                    mapGraticule.setStroke(Color.BLACK);
                    mapGraticule.setTextFill(Color.BLACK);
                }
            }
        });
        mapLayerComboBox.getSelectionModel().select(0);

        seamarksCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            Node mapLayer = mapLayers.get("Seamarks");
            if (newValue) {
                map.getChildren().add(1, mapLayer);
            } else {
                map.getChildren().remove(mapLayer);
            }
        });

        MapProjection[] projections = new MapProjection[]{
            new WebMercatorProjection(),
            new EquirectangularProjection(),
            new GnomonicProjection(),
            new StereographicProjection()
        };

        projectionComboBox.getSelectionModel().select(0);
        projectionComboBox.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                int index = newValue.intValue();
                map.setProjection(projections[index]);
            }
        });

        for (double lon = -180d; lon < 180d; lon += 15) {
            MapPolygon polygon = new MapPolygon();
            polygon.getLocations().add(new Location(0d, lon - 5d));
            polygon.getLocations().add(new Location(-5d, lon));
            polygon.getLocations().add(new Location(0d, lon + 5d));
            polygon.getLocations().add(new Location(5d, lon));
            polygon.setLocation(new Location(0d, lon));
            polygon.setStroke(Color.RED);

            map.getChildren().add(polygon);
        }

        for (double lon = -180d; lon < 180d; lon += 15) {
            String text = lon < 0d ? String.format(" W %.0f ", -lon) : String.format(" E %.0f ", lon);

            Label label = new Label(text);
            label.setBackground(new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)));
            label.setTranslateY(-20);

            MapNode pushpin = new MapNode();
            pushpin.setLocation(new Location(0, lon));
            pushpin.getChildren().add(label);

            map.getChildren().add(pushpin);
        }
    }
}
