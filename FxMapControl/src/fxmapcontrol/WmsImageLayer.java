/*
 * FX Map Control - https://github.com/ClemensFischer/FX-Map-Control
 * © 2016 Clemens Fischer
 */
package fxmapcontrol;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
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
 * Map image overlay. Fills the entire viewport with map images provided by a Web Map Service (WMS).
 * The base request URI is specified by the serverUri property.
 */
public class WmsImageLayer extends MapImageLayer {

    private final StringProperty serverUriProperty = new SimpleStringProperty(this, "serverUri");
    private final StringProperty layersProperty = new SimpleStringProperty(this, "layers");
    private final StringProperty parametersProperty = new SimpleStringProperty(this, "parameters");
    private final BooleanProperty transparentProperty = new SimpleBooleanProperty(this, "transparent");
    private String allLayers;

    public WmsImageLayer() {
        serverUriProperty.addListener(observable -> {
            allLayers = null;
            updateUriFormat();
        });

        layersProperty.addListener(observable -> updateUriFormat());
        transparentProperty.addListener(observable -> updateUriFormat());
    }

    public final StringProperty serverUriProperty() {
        return serverUriProperty;
    }

    public final String getServerUri() {
        return serverUriProperty.get();
    }

    public final void setServerUri(String serverUri) {
        serverUriProperty.set(serverUri);
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

    public final StringProperty parametersProperty() {
        return parametersProperty;
    }

    public final String getParameters() {
        return parametersProperty.get();
    }

    public final void setParameters(String parameters) {
        parametersProperty.set(parameters);
    }

    public final BooleanProperty transparentProperty() {
        return transparentProperty;
    }

    public final boolean getTransparent() {
        return transparentProperty.get();
    }

    public final void setTransparent(boolean transparent) {
        transparentProperty.set(transparent);
    }

    public List<String> getAllLayers() {
        List<String> layers = new ArrayList<>();
        String uri = getServerUri();

        if (uri != null && !uri.isEmpty()) {
            try {
                uri += "?SERVICE=WMS&VERSION=1.3.0&REQUEST=GetCapabilities";
                HttpURLConnection connection = (HttpURLConnection) new URL(uri).openConnection();

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

    private void updateUriFormat() {
        String serverUri = getServerUri();
        String layers = getLayers();
        String uriFormat = null;

        if (serverUri != null && !serverUri.isEmpty() && layers != null && !layers.isEmpty()) {

            if (layers.equals("*")) {
                if (allLayers == null) {
                    allLayers = String.join(",", getAllLayers());
                }
                layers = allLayers;
            }

            uriFormat = serverUri + "?SERVICE=WMS"
                    + "&VERSION=1.3.0"
                    + "&REQUEST=GetMap"
                    + "&LAYERS=" + layers.replaceAll(" ", "%20")
                    + "&STYLES="
                    + "&CRS=EPSG:3857"
                    + "&BBOX={W},{S},{E},{N}"
                    + "&WIDTH={X}"
                    + "&HEIGHT={Y}"
                    + "&FORMAT=image/png"
                    + "&TRANSPARENT=" + (getTransparent() ? "TRUE" : "FALSE");

            String parameters = getParameters();
            if (parameters != null && !parameters.isEmpty()) {
                uriFormat += "&" + parameters;
            }
        }

        setUriFormat(uriFormat);
    }
}
