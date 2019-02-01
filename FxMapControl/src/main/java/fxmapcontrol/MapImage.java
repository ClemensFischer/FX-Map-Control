/*
 * FX Map Control - https://github.com/ClemensFischer/FX-Map-Control
 * © 2019 Clemens Fischer
 */
package fxmapcontrol;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Bounds;
import javafx.scene.image.ImageView;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Transform;

/**
 * Fills a rectangular map area defined by the boundingBox property with an Image.
 */
public class MapImage extends ImageView implements IMapNode {

    private final ObjectProperty<MapBoundingBox> boundingBoxProperty = new SimpleObjectProperty<>(this, "boundingBox");
    private final MapNodeHelper mapNode = new MapNodeHelper(e -> updateLayout());

    public MapImage() {
        setMouseTransparent(true);
        boundingBoxProperty.addListener((observable, oldValue, newValue) -> updateLayout());
    }

    @Override
    public final MapBase getMap() {
        return mapNode.getMap();
    }

    @Override
    public void setMap(MapBase map) {
        mapNode.setMap(map);
        updateLayout();
    }

    public final ObjectProperty<MapBoundingBox> boundingBoxProperty() {
        return boundingBoxProperty;
    }

    public final MapBoundingBox getBoundingBox() {
        return boundingBoxProperty.get();
    }

    public final void setBoundingBox(MapBoundingBox boundingBox) {
        boundingBoxProperty.set(boundingBox);
    }

    private void updateLayout() {
        Affine viewportTransform = null;
        Bounds bounds = null;
        MapBase map = getMap();
        MapBoundingBox boundingBox = getBoundingBox();

        if (map != null && boundingBox != null) {
            bounds = map.getProjection().boundingBoxToBounds(boundingBox);
            viewportTransform = map.getProjection().getViewportTransform();
        }

        if (bounds != null) {
            getTransforms().setAll(viewportTransform, Transform.scale(1d, -1d, 0d, bounds.getMaxY()));
            setX(bounds.getMinX());
            setY(bounds.getMaxY());
            setFitWidth(bounds.getWidth());
            setFitHeight(bounds.getHeight());
        } else {
            getTransforms().clear();
            setX(0);
            setY(0);
            setFitWidth(0);
            setFitHeight(0);
        }
    }
}
