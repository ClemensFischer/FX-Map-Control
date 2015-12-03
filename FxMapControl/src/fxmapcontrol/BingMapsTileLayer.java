/*
 * FX Map Control - https://github.com/ClemensFischer/FX-Map-Control
 * © 2015 Clemens Fischer
 */
package fxmapcontrol;

import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Displays Bing Maps tiles. The static apiKey property must be set to a Bing Maps API Key.
 */
public class BingMapsTileLayer extends MapTileLayer {

    public enum MapMode {
        Road, Aerial, AerialWithLabels
    }

    private static String apiKey;
    private final MapMode mapMode;

    public BingMapsTileLayer(MapMode mode) {
        this(new TileImageLoader(), mode);
    }

    public BingMapsTileLayer(ITileImageLoader tileImageLoader, MapMode mode) {
        super(tileImageLoader);

        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException("BingMapsTileLayer requires a Bing Maps API Key.");
        }

        mapMode = mode;

        try {
            String url = String.format("http://dev.virtualearth.net/REST/V1/Imagery/Metadata/%s?output=xml&key=%s", mapMode.toString(), apiKey);
            Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new URL(url).openStream());

            Element metadataElement = (Element) document.getElementsByTagName("ImageryMetadata").item(0);
            Element imageUrlElement = (Element) metadataElement.getElementsByTagName("ImageUrl").item(0);
            Element subdomainsElement = (Element) metadataElement.getElementsByTagName("ImageUrlSubdomains").item(0);
            Element zoomMinElement = (Element) metadataElement.getElementsByTagName("ZoomMin").item(0);
            Element zoomMaxElement = (Element) metadataElement.getElementsByTagName("ZoomMax").item(0);

            NodeList subdomainStrings = subdomainsElement.getElementsByTagName("string");
            String[] subdomains = new String[subdomainStrings.getLength()];

            for (int i = 0; i < subdomains.length; i++) {
                subdomains[i] = subdomainStrings.item(i).getTextContent();
            }

            setName("Bing Maps " + mapMode);
            setTileSource(new BingMapsTileSource(imageUrlElement.getTextContent(), subdomains));
            setMinZoomLevel(Integer.parseInt(zoomMinElement.getTextContent()));
            setMaxZoomLevel(Integer.parseInt(zoomMaxElement.getTextContent()));

        } catch (IOException | ParserConfigurationException | SAXException | DOMException ex) {
            Logger.getLogger(BingMapsTileLayer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static String getApiKey() {
        return apiKey;
    }

    public static void setApiKey(String key) {
        apiKey = key;
    }

    public final MapMode getMapMode() {
        return mapMode;
    }
}
