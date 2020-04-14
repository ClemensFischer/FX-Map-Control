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
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Displays Bing Maps tiles. The static apiKey property must be set to a Bing Maps API Key.
 */
public class BingMapsTileLayer extends MapTileLayer {

    public enum MapMode {
        Road, Aerial, AerialWithLabels
    }

    private static String apiKey;

    public static String getApiKey() {
        return apiKey;
    }

    public static void setApiKey(String key) {
        apiKey = key;
    }

    private final MapMode mapMode;
    private String metadataUrl;

    public BingMapsTileLayer(MapMode mode) {
        this(new TileImageLoader(), mode);
    }

    public BingMapsTileLayer(ITileImageLoader tileImageLoader, MapMode mode) {
        super(tileImageLoader);
        mapMode = mode;
        setName("Bing Maps " + mapMode);
    }

    public final MapMode getMapMode() {
        return mapMode;
    }

    @Override
    public void setMap(MapBase map) {
        super.setMap(map);

        if (metadataUrl == null) {
            if (apiKey != null && !apiKey.isEmpty()) {
                metadataUrl = String.format(
                        "http://dev.virtualearth.net/REST/V1/Imagery/Metadata/%s?output=xml&key=%s",
                        mapMode.toString(), apiKey);
                new MetadataReader().start();
            } else {
                metadataUrl = "";
                Logger.getLogger(BingMapsTileLayer.class.getName()).log(Level.WARNING,
                        "BingMapsTileLayer requires a Bing Maps API Key.");
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

                Element metadata = (Element) document.getDocumentElement().getElementsByTagName("ImageryMetadata").item(0);

                Node imageUrl = metadata.getElementsByTagName("ImageUrl").item(0);
                Node zoomMin = metadata.getElementsByTagName("ZoomMin").item(0);
                Node zoomMax = metadata.getElementsByTagName("ZoomMax").item(0);

                Element urlSubdomains = (Element) metadata.getElementsByTagName("ImageUrlSubdomains").item(0);
                NodeList subdomainStrings = urlSubdomains.getElementsByTagName("string");
                String[] subdomains = new String[subdomainStrings.getLength()];

                for (int i = 0; i < subdomains.length; i++) {
                    subdomains[i] = subdomainStrings.item(i).getTextContent();
                }

                minZoomLevel = Integer.parseInt(zoomMin.getTextContent());
                maxZoomLevel = Integer.parseInt(zoomMax.getTextContent());
                tileSource = new BingMapsTileSource(imageUrl.getTextContent(), subdomains);

            } catch (Exception ex) {
                Logger.getLogger(BingMapsTileLayer.class.getName()).log(Level.WARNING,
                        String.format("%s: %s", metadataUrl, ex.toString()));
            }

            return tileSource != null;
        }
    }
}
