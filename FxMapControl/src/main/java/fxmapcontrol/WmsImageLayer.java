/*
 * FX Map Control - https://github.com/ClemensFischer/FX-Map-Control
 * © 2020 Clemens Fischer
 */
package fxmapcontrol;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.geometry.Bounds;
import javafx.scene.image.Image;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Displays a single map image from a Web Map Service (WMS).
 * <p>
 * The base request URL is specified by the serviceUrl property.
 */
public class WmsImageLayer extends MapImageLayer {

    private final StringProperty serviceUrlProperty = new SimpleStringProperty(this, "serviceUrl");
    private final StringProperty layersProperty = new SimpleStringProperty(this, "layers");
    private final StringProperty stylesProperty = new SimpleStringProperty(this, "styles", "");
    private final StringProperty formatProperty = new SimpleStringProperty(this, "format", "image/png");

    public WmsImageLayer() {
        ChangeListener changeListener = (observable, oldValue, newValue) -> updateImage();
        serviceUrlProperty.addListener(changeListener);
        layersProperty.addListener(changeListener);
        stylesProperty.addListener(changeListener);
        formatProperty.addListener(changeListener);
    }

    public WmsImageLayer(String serviceUrl) {
        this();
        setServiceUrl(serviceUrl);
    }

    public final StringProperty serviceUrlProperty() {
        return serviceUrlProperty;
    }

    public final String getServiceUrl() {
        return serviceUrlProperty.get();
    }

    public final void setServiceUrl(String serviceUrl) {
        serviceUrlProperty.set(serviceUrl);
    }

    public final StringProperty layersProperty() {
        return layersProperty;
    }

    public final String getLayers() {
        return layersProperty.get();
    }

    public final void setLayers(String layers) {
        layersProperty.set(layers);
    }

    public final StringProperty stylesProperty() {
        return stylesProperty;
    }

    public final String getStyles() {
        return stylesProperty.get();
    }

    public final void setStyles(String styles) {
        stylesProperty.set(styles);
    }

    public final StringProperty formatProperty() {
        return formatProperty;
    }

    public final String getFormat() {
        return formatProperty.get();
    }

    public final void setFormat(String format) {
        formatProperty.set(format);
    }

    public List<String> getAllLayers() {
        List<String> layerNames = null;

        if (getServiceUrl() != null && !getServiceUrl().isEmpty()) {
            String url = getRequestUrl("GetCapabilities").replace(" ", "%20");

            try {
                Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(url);
                NodeList layerNodes = document.getDocumentElement().getElementsByTagName("Layer");

                layerNames = new ArrayList<>();

                if (layerNodes.getLength() > 0) {
                    layerNodes = ((Element) layerNodes.item(0)).getElementsByTagName("Layer");

                    for (int i = 0; i < layerNodes.getLength(); i++) {
                        NodeList nameNodes = ((Element) layerNodes.item(i)).getElementsByTagName("Name");

                        if (nameNodes.getLength() > 0) {
                            layerNames.add(nameNodes.item(0).getTextContent());
                        }
                    }
                }
            } catch (Exception ex) {
                Logger.getLogger(WmsImageLayer.class.getName()).log(
                        Level.WARNING, "{0}: {1}", new Object[]{url, ex});
            }
        }

        return layerNames;
    }

    @Override
    public void setMap(MapBase map) {
        super.setMap(map);
    }

    @Override
    protected Image loadImage() {
        Image image = null;

        if (getServiceUrl() != null && !getServiceUrl().isEmpty()) {
            if (getLayers() == null && !getServiceUrl().toUpperCase().contains("LAYERS=")) {

                new DefaultLayerService().start(); // get first Layer from Capabilities
            } else {
                String url = getImageUrl();

                if (url != null && !url.isEmpty()) {
                    image = new Image(url, true);
                }
            }
        }

        return image;
    }

    protected String getImageUrl() {
        MapProjection projection = getMap().getProjection();
        Bounds bounds = projection.boundingBoxToBounds(getBoundingBox());
        double viewScale = getMap().getViewTransform().getScale();
        String url = getRequestUrl("GetMap");
        String urlUpperCase = url.toUpperCase();

        if (!urlUpperCase.contains("LAYERS=") && getLayers() != null) {
            url += "&LAYERS=" + getLayers();
        }

        if (!urlUpperCase.contains("STYLES=") && getStyles() != null) {
            url += "&STYLES=" + getStyles();
        }

        if (!urlUpperCase.contains("FORMAT=") && getFormat() != null) {
            url += "&FORMAT=" + getFormat();
        }

        url += "&CRS=" + projection.getCrsValue();
        url += "&BBOX=" + projection.getBboxValue(bounds);
        url += "&WIDTH=" + (int) Math.round(viewScale * bounds.getWidth());
        url += "&HEIGHT=" + (int) Math.round(viewScale * bounds.getHeight());

        return url.replace(" ", "%20");
    }

    private String getRequestUrl(String request) {
        String url = getServiceUrl();

        if (!url.endsWith("?") && !url.endsWith("&")) {
            url += !url.contains("?") ? "?" : "&";
        }

        if (!url.toUpperCase().contains("SERVICE=")) {
            url += "SERVICE=WMS&";
        }

        if (!url.toUpperCase().contains("VERSION=")) {
            url += "VERSION=1.3.0&";
        }

        return url + "REQUEST=" + request;
    }

    private class DefaultLayerService extends Service<String> {
        @Override
        protected void succeeded() {
            setLayers(getValue());
        }

        @Override
        protected void failed() {
            setLayers("");
        }

        @Override
        protected Task<String> createTask() {
            return new Task<String>() {
                @Override
                protected String call() {
                    List<String> layers = getAllLayers();
                    return layers != null && !layers.isEmpty() ? layers.get(0) : "";
                }
            };
        }
    }
}
