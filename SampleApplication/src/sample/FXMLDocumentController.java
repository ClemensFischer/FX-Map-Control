package sample;

import com.sun.javafx.tk.Toolkit;
import fxmapcontrol.Location;
import fxmapcontrol.MapBase;
import fxmapcontrol.MapGraticule;
import fxmapcontrol.MapImageLayer;
import fxmapcontrol.MapItem;
import fxmapcontrol.MapItemsControl;
import fxmapcontrol.MapPolygon;
import fxmapcontrol.MapTileLayer;
import fxmapcontrol.TileSource;
import java.net.URL;
import java.util.ArrayList;
import java.util.Random;
import java.util.ResourceBundle;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Point2D;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Ellipse;

public class FXMLDocumentController implements Initializable {
    
    private final Random random = new Random();
    private int itemNumber = 1;

    @FXML
    private MapBase map;
    @FXML
    private MapGraticule mapGraticule;
    @FXML
    private MapItemsControl itemsControl;
    @FXML
    private Slider zoomSlider;
    @FXML
    private Slider headingSlider;
    @FXML
    private CheckBox wmsCheckBox;
    @FXML
    private CheckBox seamarksCheckBox;
    @FXML
    private ComboBox tileLayerComboBox;

    @FXML
    private void handlePressed(MouseEvent event) {
        if (event.getTarget() == map && event.getClickCount() == 2) {
            map.setTargetCenter(map.viewportPointToLocation(new Point2D(event.getX(), event.getY())));
        }
    }

    class MapCircle extends MapItem {

        @Override
        public String toString() {
            return getUserData().toString();
        }
    }

    @FXML
    private void addItem(ActionEvent event) {
        MapItem item = new MapCircle();
        item.setUserData(itemNumber);

        if (itemNumber % 10 == 0) {            
            final ArrayList<Location> locations = new ArrayList<>();
            locations.add(new Location(53d + random.nextDouble(), 8d + 2d * random.nextDouble()));
            locations.add(new Location(53d + random.nextDouble(), 8d + 2d * random.nextDouble()));
            locations.add(new Location(53d + random.nextDouble(), 8d + 2d * random.nextDouble()));
            
            final MapPolygon selectedPolygon = new MapPolygon(locations);
            selectedPolygon.visibleProperty().bind(item.selectedProperty());
            selectedPolygon.setStroke(new Color(1d, 1d, 1d, 0.75));
            selectedPolygon.setStrokeWidth(7d);
            item.getChildren().add(selectedPolygon);
            
            final MapPolygon polygon = new MapPolygon(locations);
            polygon.setStroke(Color.MAGENTA);
            polygon.setStrokeWidth(3d);
            item.getChildren().add(polygon);

        } else {
            item.setLocation(new Location(53d + random.nextDouble(), 8d + 2d * random.nextDouble()));
            item.setHideOutsideViewport(true);

            final Ellipse selectedCircle = new Ellipse(25, 25);
            selectedCircle.setMouseTransparent(true);
            selectedCircle.setFill(new Color(1d, 1d, 1d, 0.75));
            selectedCircle.visibleProperty().bind(item.selectedProperty());

            final Ellipse circle = new Ellipse(15, 15);
            circle.setFill(Color.MAGENTA);

            final Label label = new Label(Integer.toString(itemNumber));
            label.setTextFill(Color.WHITE);

            double width = Toolkit.getToolkit().getFontLoader().computeStringWidth(label.getText(), label.getFont());
            label.relocate(-width / 2, -10);

            item.getChildren().add(selectedCircle);
            item.getChildren().add(circle);
            item.getChildren().add(label);
        }

        itemsControl.getItems().add(item);
        itemNumber++;
    }

    @FXML
    private void removeItem(ActionEvent event) {
        MapItem item = itemsControl.getSelectionModel().getSelectedItem();

        if (item != null) {
            itemsControl.getItems().remove(item);
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        map.targetZoomLevelProperty().bindBidirectional(zoomSlider.valueProperty());
        map.targetHeadingProperty().bindBidirectional(headingSlider.valueProperty());

//        BingMapsTileLayer.setApiKey("...");

        MapTileLayer[] tileLayers = new MapTileLayer[]{
            (MapTileLayer) map.getChildren().get(0),
            new MapTileLayer(),
            new MapTileLayer(),
//            new BingMapsTileLayer(BingMapsTileLayer.MapMode.Road),
//            new BingMapsTileLayer(BingMapsTileLayer.MapMode.Aerial),
//            new BingMapsTileLayer(BingMapsTileLayer.MapMode.AerialWithLabels)
        };

        tileLayers[1].setName("Thunderforest OpenCycleMap");
        tileLayers[1].setTileSource(new TileSource("http://{c}.tile.thunderforest.com/cycle/{z}/{x}/{y}.png"));
        tileLayers[2].setName("MapQuest OpenStreetMap");
        tileLayers[2].setTileSource(new TileSource("http://otile{n}.mqcdn.com/tiles/1.0.0/osm/{z}/{x}/{y}.jpg"));

        MapTileLayer seamarksLayer = new MapTileLayer();
        seamarksLayer.setName("Seamarks");
        seamarksLayer.setTileSource(new TileSource("http://tiles.openseamap.org/seamark/{z}/{x}/{y}.png"));
        seamarksLayer.setMinZoomLevel(9);

        MapImageLayer wmsLayer = new MapImageLayer();
        wmsLayer.setUriFormat("http://129.206.228.72/cached/osm?SERVICE=WMS&VERSION=1.1.1&REQUEST=GetMap&LAYERS=osm_auto:all&STYLES=&SRS=EPSG:900913&BBOX={W},{S},{E},{N}&WIDTH={X}&HEIGHT={Y}&FORMAT=image/png");
        wmsLayer.setRelativeImageSize(1.5);

        tileLayerComboBox.getSelectionModel().select(0);
        tileLayerComboBox.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                int index = newValue.intValue();
                map.getChildren().set(0, tileLayers[newValue.intValue()]);

                if (index < 4) {
                    mapGraticule.setStroke(Color.BLACK);
                    mapGraticule.setTextFill(Color.BLACK);
                } else {
                    mapGraticule.setStroke(Color.WHITE);
                    mapGraticule.setTextFill(Color.WHITE);
                }
            }
        });

        wmsCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                map.getChildren().add(1, wmsLayer);
            } else {
                map.getChildren().remove(wmsLayer);
            }
        });

        seamarksCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                map.getChildren().add(wmsCheckBox.isSelected() ? 2 : 1, seamarksLayer);
            } else {
                map.getChildren().remove(seamarksLayer);
            }
        });

//        itemsControl.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

itemsControl.getSelectionModel().getSelectedIndices().addListener((ListChangeListener.Change<? extends Integer> c) -> {
            System.out.println("------------------------------");
            while (c.next()) {
                if (c.wasRemoved()) {
                    c.getRemoved().stream().forEach(index -> {
                        System.out.println("Deselected Index " + index);
                    });
                }
                if (c.wasAdded()) {
                    c.getAddedSubList().stream().forEach(index -> {
                        System.out.println("Selected Index " + index);
                    });
                }
            }
            System.out.println("Selected Indices: " + c.getList());
        });

        itemsControl.getSelectionModel().getSelectedItems().addListener((ListChangeListener.Change<? extends MapItem> c) -> {
            while (c.next()) {
                if (c.wasRemoved()) {
                    c.getRemoved().stream().forEach(item -> {
                        System.out.println("Deselected Item " + item.getUserData());
                    });
                }
                if (c.wasAdded()) {
                    c.getAddedSubList().stream().forEach(item -> {
                        System.out.println("Selected Item " + item.getUserData());
                    });
                }
            }
            System.out.println("Selected Items: " + c.getList());
        });

        itemsControl.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
            System.out.println("Selected Index: " + newValue);
        });

        itemsControl.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                System.out.println("Selected Item: " + newValue.getUserData());
            } else {
                System.out.println("Selected Item: null");
            }
        });
    }
}
