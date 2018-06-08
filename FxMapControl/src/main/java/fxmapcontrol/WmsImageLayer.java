/*
 * FX Map Control - https://github.com/ClemensFischer/FX-Map-Control
 * © 2016 Clemens Fischer
 */
package fxmapcontrol;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Map image overlay. Fills the entire viewport with map images provided by a Web Map Service (WMS). The base request URL is specified by the serviceUrl property.
 */
public class WmsImageLayer extends MapImageLayer {

    private static final String defaultVersion = "1.3.0";

    private final StringProperty serviceUrlProperty = new SimpleStringProperty(this, "serviceUrl");
    private final StringProperty versionProperty = new SimpleStringProperty(this, "version", defaultVersion);
    private final StringProperty layersProperty = new SimpleStringProperty(this, "layers", "");
    private final StringProperty stylesProperty = new SimpleStringProperty(this, "styles", "");
    private final StringProperty formatProperty = new SimpleStringProperty(this, "format", "image/png");

    public WmsImageLayer(String serviceUrl) {
        this();
        setServiceUrl(serviceUrl);
    }

    public WmsImageLayer() {
        serviceUrlProperty.addListener((observable, oldValue, newValue) -> updateImage());
        versionProperty.addListener((observable, oldValue, newValue) -> updateImage());
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

    public final StringProperty versionProperty() {
        return versionProperty;
    }

    public final String getVersion() {
        return versionProperty.get();
    }

    public final void setVersion(String version) {
        versionProperty.set(version);
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

    public ObservableList<String> getAllLayers() {
        ObservableList<String> layers = null;

        if (getServiceUrl() != null && !getServiceUrl().isEmpty()) {
            String[] version = new String[]{getVersion()};
            String url = getRequestUrl(version) + "REQUEST=GetCapabilities";

            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(url.replace(" ", "%20")).openConnection();

                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                    Document document;

                    try (InputStream inputStream = connection.getInputStream()) {
                        document = docBuilder.parse(inputStream);
                    }

                    layers = FXCollections.observableArrayList();
                    NodeList layerNodes = document.getDocumentElement().getElementsByTagName("Layer");

                    if (layerNodes.getLength() > 0) {
                        Element rootLayer = (Element) layerNodes.item(0);
                        layerNodes = rootLayer.getElementsByTagName("Layer");

                        for (int i = 0; i < layerNodes.getLength(); i++) {
                            Node layerNode = layerNodes.item(i);

                            if (layerNode.getNodeType() == Node.ELEMENT_NODE) {
                                NodeList nameNodes = ((Element) layerNode).getElementsByTagName("Name");

                                if (nameNodes.getLength() > 0) {
                                    layers.add(((Element) nameNodes.item(0)).getTextContent());
                                }
                            }
                        }
                    }
                }
            } catch (IOException | ParserConfigurationException | SAXException | DOMException ex) {
                Logger.getLogger(WmsImageLayer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return layers;
    }

    @Override
    protected boolean updateImage(MapBoundingBox boundingBox) {
        if (getServiceUrl() == null || getServiceUrl().isEmpty()) {
            return false;
        }

        String[] version = new String[]{getVersion()};
        String url = getRequestUrl(version) + "REQUEST=GetMap&";
        String queryParameters = getMap().getProjection().wmsQueryParameters(boundingBox, version[0].startsWith("1.1."));

        if (queryParameters == null || queryParameters.isEmpty()) {
            return false;
        }

        if (!url.toUpperCase().contains("LAYERS=")) {
            url += "LAYERS=" + (getLayers() != null ? getLayers() : "") + "&";
        }

        if (!url.toUpperCase().contains("STYLES=")) {
            url += "STYLES=" + (getStyles() != null ? getStyles() : "") + "&";
        }

        if (!url.toUpperCase().contains("FORMAT=")) {
            url += "FORMAT=" + (getFormat() != null ? getFormat() : "") + "&";
        }

        url += queryParameters;
        updateImage(url.replace(" ", "%20"));
        return true;
    }

    private String getRequestUrl(String[] version) {
        if (version[0] == null) {
            version[0] = defaultVersion;
        }

        String url = getServiceUrl();

        if (!url.endsWith("?") && !url.endsWith("&")) {
            url += !url.contains("?") ? "?" : "&";
        }

        if (!url.toUpperCase().contains("SERVICE=")) {
            url += "SERVICE=WMS&";
        }

        int versionStart = url.toUpperCase().indexOf("VERSION=");
        int versionEnd;

        if (versionStart < 0) {
            url += "VERSION=" + version[0] + "&";
        } else if ((versionEnd = url.indexOf("&", versionStart + 8)) >= versionStart + 8) {
            version[0] = url.substring(versionStart, versionEnd);
        }

        return url;
    }
}
