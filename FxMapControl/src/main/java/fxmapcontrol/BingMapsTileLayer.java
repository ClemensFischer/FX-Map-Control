/*
 * FX Map Control - https://github.com/ClemensFischer/FX-Map-Control
 * © 2020 Clemens Fischer
 */
package fxmapcontrol;

import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Displays Bing Maps tiles. The static apiKey property must be set to a Bing Maps API Key.
 */
public class BingMapsTileLayer extends MapTileLayer {

    public enum MapMode {
        Road, Aerial, AerialWithLabels
    }

    private static final String metadataUrlFormat = "http://dev.virtualearth.net/REST/V1/Imagery/Metadata/%s?output=xml&key=%s";

    private static String apiKey;

    public static String getApiKey() {
        return apiKey;
    }

    public static void setApiKey(String key) {
        apiKey = key;
    }

    private final MapMode mapMode;
    private String metadataUrl;

    public BingMapsTileLayer(ITileImageLoader tileImageLoader, MapMode mode) {
        super(tileImageLoader);
        getStyleClass().add("bing-maps-tile-layer");
        mapMode = mode;
        setName("Bing Maps " + mapMode);
    }

    public BingMapsTileLayer(MapMode mode) {
        this(new TileImageLoader(), mode);
    }

    public final MapMode getMapMode() {
        return mapMode;
    }

    @Override
    public void setMap(MapBase map) {
        super.setMap(map);

        if (metadataUrl == null) {
            if (apiKey != null && !apiKey.isEmpty()) {
                metadataUrl = String.format(metadataUrlFormat, mapMode.toString(), apiKey);
                new MetadataReader().start();
            } else {
                metadataUrl = "";
                Logger.getLogger(BingMapsTileLayer.class.getName()).log(
                        Level.WARNING, "BingMapsTileLayer requires a Bing Maps API Key.");
            }
        }
    }

    private class MetadataReader extends Service<Boolean> {

        public int minZoomLevel;
        public int maxZoomLevel;
        public TileSource tileSource;

        @Override
        protected void succeeded() {
            if (getValue()) {
                setMinZoomLevel(minZoomLevel);
                setMaxZoomLevel(maxZoomLevel);
                setTileSource(tileSource); // -> update tiles
            }
        }

        @Override
        protected Task<Boolean> createTask() {
            return new Task<Boolean>() {
                @Override
                protected Boolean call() {
                    return readMetadata();
                }
            };
        }

        private Boolean readMetadata() {
            try {
                Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(metadataUrl);

                Element metadataElement = (Element) document.getDocumentElement().getElementsByTagName("ImageryMetadata").item(0);
                Element subdomainsElement = (Element) metadataElement.getElementsByTagName("ImageUrlSubdomains").item(0);
                NodeList subdomainNodes = subdomainsElement.getElementsByTagName("string");
                String[] subdomains = new String[subdomainNodes.getLength()];

                for (int i = 0; i < subdomains.length; i++) {
                    subdomains[i] = subdomainNodes.item(i).getTextContent();
                }

                minZoomLevel = Integer.parseInt(metadataElement.getElementsByTagName("ImageUrl").item(0).getTextContent());
                maxZoomLevel = Integer.parseInt(metadataElement.getElementsByTagName("ZoomMax").item(0).getTextContent());

                tileSource = new BingMapsTileSource(
                        metadataElement.getElementsByTagName("ImageUrl").item(0).getTextContent(), subdomains);

            } catch (Exception ex) {
                Logger.getLogger(BingMapsTileLayer.class.getName()).log(
                        Level.WARNING, "{0}: {1}", new Object[]{metadataUrl, ex});
            }

            return tileSource != null;
        }
    }
}
