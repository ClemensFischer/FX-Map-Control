package fxmapcontrol;

import java.util.List;
import java.util.Locale;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.css.CssMetaData;
import javafx.css.SimpleStyleableDoubleProperty;
import javafx.css.SimpleStyleableObjectProperty;
import javafx.css.Styleable;
import javafx.css.StyleableDoubleProperty;
import javafx.css.StyleableObjectProperty;
import javafx.css.StyleablePropertyFactory;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.image.Image;
import javafx.util.Duration;

/**
 * Map image overlay. Fills the entire viewport with map images provided by a web service, e.g. a
 * Web Map Service (WMS). The image request URI is specified by the uriFormat property.
 */
public class MapImageLayer extends Parent implements IMapNode {

    private static final StyleablePropertyFactory<MapImageLayer> propertyFactory
            = new StyleablePropertyFactory<>(Parent.getClassCssMetaData());

    private static final CssMetaData<MapImageLayer, Duration> updateDelayCssMetaData
            = propertyFactory.createDurationCssMetaData("-fx-update-delay", s -> s.updateDelayProperty);

    private static final CssMetaData<MapImageLayer, Number> relativeImageSizeCssMetaData
            = propertyFactory.createSizeCssMetaData("-fx-relative-image-size", s -> s.relativeImageSizeProperty);

    private final StyleableObjectProperty<Duration> updateDelayProperty
            = new SimpleStyleableObjectProperty<>(updateDelayCssMetaData, this, "updateDelay", Duration.seconds(0.5));

    private final StyleableDoubleProperty relativeImageSizeProperty
            = new SimpleStyleableDoubleProperty(relativeImageSizeCssMetaData, this, "relativeImageSize", 1d);

    private final StringProperty uriFormatProperty = new SimpleStringProperty(this, "uriFormat");

    private final Timeline updateTimeline = new Timeline();
    private final MapNodeHelper mapNode = new MapNodeHelper(e -> updateTimeline.playFromStart());
    private boolean loadingImage;

    public MapImageLayer() {
        getStyleClass().add("map-image-layer");
        setMouseTransparent(true);

        uriFormatProperty.addListener((observable, oldValue, newValue) -> updateImage());

        updateTimeline.getKeyFrames().add(new KeyFrame(getUpdateDelay(), e -> updateImage()));

        updateDelayProperty.addListener((observable, oldValue, newValue) -> {
            updateTimeline.getKeyFrames().set(0, new KeyFrame(getUpdateDelay(), e -> updateImage()));
        });
    }

    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return propertyFactory.getCssMetaData();
    }

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return getClassCssMetaData();
    }

    @Override
    public final MapBase getMap() {
        return mapNode.getMap();
    }

    @Override
    public final void setMap(MapBase map) {
        mapNode.setMap(map);

        getChildren().stream()
                .filter(node -> node instanceof IMapNode)
                .forEach(node -> ((IMapNode) node).setMap(map));

        updateImage();
    }

    public final ObjectProperty<Duration> updateDelayProperty() {
        return updateDelayProperty;
    }

    public final Duration getUpdateDelay() {
        return updateDelayProperty.get();
    }

    public final void setUpdateDelay(Duration updateDelay) {
        updateDelayProperty.set(updateDelay);
    }

    public final DoubleProperty relativeImageSizeProperty() {
        return relativeImageSizeProperty;
    }

    public final double getRelativeImageSize() {
        return relativeImageSizeProperty.get();
    }

    public final void setRelativeImageSize(double relativeImageSize) {
        relativeImageSizeProperty.set(relativeImageSize);
    }

    public final StringProperty uriFormatProperty() {
        return uriFormatProperty;
    }

    public final String getUriFormat() {
        return uriFormatProperty.get();
    }

    public final void setUriFormat(String uriFormat) {
        uriFormatProperty.set(uriFormat);
    }

    private void updateImage() {
        MapBase map = getMap();

        if (loadingImage) {
            updateTimeline.playFromStart(); // update image on next timer tick

        } else if (map != null && map.getWidth() > 0 && map.getHeight() > 0) {
            loadingImage = true;

            double relativeSize = Math.max(getRelativeImageSize(), 1d);
            double width = map.getWidth() * relativeSize;
            double height = map.getHeight() * relativeSize;
            double dx = (map.getWidth() - width) / 2d;
            double dy = (map.getHeight() - height) / 2d;

            Location loc1 = map.viewportPointToLocation(new Point2D(dx, dy));
            Location loc2 = map.viewportPointToLocation(new Point2D(dx + width, dy));
            Location loc3 = map.viewportPointToLocation(new Point2D(dx, dy + height));
            Location loc4 = map.viewportPointToLocation(new Point2D(dx + width, dy + height));

            double west = Math.min(loc1.getLongitude(), Math.min(loc2.getLongitude(), Math.min(loc3.getLongitude(), loc4.getLongitude())));
            double east = Math.max(loc1.getLongitude(), Math.max(loc2.getLongitude(), Math.max(loc3.getLongitude(), loc4.getLongitude())));
            double south = Math.min(loc1.getLatitude(), Math.min(loc2.getLatitude(), Math.min(loc3.getLatitude(), loc4.getLatitude())));
            double north = Math.max(loc1.getLatitude(), Math.max(loc2.getLatitude(), Math.max(loc3.getLatitude(), loc4.getLatitude())));

            Point2D p1 = map.getMapTransform().transform(new Location(south, west));
            Point2D p2 = map.getMapTransform().transform(new Location(north, east));

            width = Math.round((p2.getX() - p1.getX()) * map.getViewportScale());
            height = Math.round((p2.getY() - p1.getY()) * map.getViewportScale());

            updateImage(south, west, north, east, (int) width, (int) height);
        }
    }

    protected void updateImage(double south, double west, double north, double east, int width, int height) {
        String format = getUriFormat();

        if (format != null && !format.isEmpty() && width > 0 && height > 0) {
            String uri = format
                    .replace("{X}", Integer.toString(width))
                    .replace("{Y}", Integer.toString(height));

            if (uri.contains("{W}") && uri.contains("{S}") && uri.contains("{E}") && uri.contains("{N}")) {
                Point2D p1 = getMap().getMapTransform().transform(new Location(south, west));
                Point2D p2 = getMap().getMapTransform().transform(new Location(north, east));

                uri = uri
                        .replace("{W}", String.format(Locale.ROOT, "%f", TileSource.METERS_PER_DEGREE * p1.getX()))
                        .replace("{S}", String.format(Locale.ROOT, "%f", TileSource.METERS_PER_DEGREE * p1.getY()))
                        .replace("{E}", String.format(Locale.ROOT, "%f", TileSource.METERS_PER_DEGREE * p2.getX()))
                        .replace("{N}", String.format(Locale.ROOT, "%f", TileSource.METERS_PER_DEGREE * p2.getY()));
            } else {
                uri = uri
                        .replace("{w}", String.format(Locale.ROOT, "%f", west))
                        .replace("{s}", String.format(Locale.ROOT, "%f", south))
                        .replace("{e}", String.format(Locale.ROOT, "%f", east))
                        .replace("{n}", String.format(Locale.ROOT, "%f", north));
            }

            updateImage(south, west, north, east, uri);

        } else {
            updateImage(south, west, north, east, (Image) null);
        }
    }

    protected void updateImage(double south, double west, double north, double east, String uri) {
        Image image = new Image(uri, true);

        image.progressProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.doubleValue() >= 1d) {
                updateImage(south, west, north, east, image);
            }
        });

        image.errorProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                updateImage(south, west, north, east, (Image) null);
            }
        });
    }

    protected final void updateImage(double south, double west, double north, double east, Image image) {
        MapBase map = getMap();

        if (map != null) {
            final ObservableList<Node> children = getChildren();
            MapImage mapImage;

            if (children.isEmpty()) {
                mapImage = new MapImage();
                mapImage.setMap(map);
                mapImage.setOpacity(0d);
                children.add(mapImage);

                mapImage = new MapImage();
                mapImage.setMap(map);
                mapImage.setOpacity(0d);
            } else {
                mapImage = (MapImage) children.remove(0);
            }

            children.add(mapImage);

            mapImage.setImage(image);
            mapImage.setBoundingBox(south, west, north, east);

            final FadeTransition fadeTransition = new FadeTransition(map.getTileFadeDuration(), mapImage);
            fadeTransition.setToValue(1d);
            fadeTransition.setOnFinished(e -> children.get(0).setOpacity(0d));
            fadeTransition.play();
        }

        loadingImage = false;
    }
}
