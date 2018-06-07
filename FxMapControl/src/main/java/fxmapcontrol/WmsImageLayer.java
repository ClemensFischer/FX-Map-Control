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

    private final StringProperty serviceUrlProperty = new SimpleStringProperty(this, "serviceUrl");
    private final StringProperty versionProperty = new SimpleStringProperty(this, "version", "1.3.0");

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

    public ObservableList<String> getAllLayers() {
        ObservableList<String> layers = FXCollections.observableArrayList();
        String url = getServiceUrl();

        if (url != null && !url.isEmpty()) {
            try {
                if (!url.endsWith("?") && !url.endsWith("&")) {
                    url += !url.contains("?") ? "?" : "&";
                }

                url += "SERVICE=WMS"
                        + "&VERSION=" + (getVersion() != null ? getVersion() : "1.3.0")
                        + "&REQUEST=GetCapabilities";

                HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();

                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                    Document document;

                    try (InputStream inputStream = connection.getInputStream()) {
                        document = docBuilder.parse(inputStream);
                    }

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
        String url = getServiceUrl();

        if (url == null || url.isEmpty()) {
            return false;
        }

        String version = getVersion() != null ? getVersion() : "1.3.0";
        String queryParameters = getMap().getProjection().wmsQueryParameters(boundingBox, version);

        if (queryParameters == null || queryParameters.isEmpty()) {
            return false;
        }

        if (!url.endsWith("?") && !url.endsWith("&")) {
            url += !url.contains("?") ? "?" : "&";
        }

        url += "SERVICE=WMS"
                + "&VERSION=" + version
                + "&REQUEST=GetMap"
                + "&" + queryParameters
                + "&FORMAT=image/png";
        url = url.replace(" ", "%20");

        updateImage(url);
        return true;
    }
}
