package fxmapcontrol;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import javafx.geometry.Point2D;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class WmtsCapabilities {

    public final String layerIdentifier;
    public final WmtsTileSource tileSource;
    public final Collection<WmtsTileMatrixSet> tileMatrixSets;

    private WmtsCapabilities(String layerIdentifier, WmtsTileSource tileSource, Collection<WmtsTileMatrixSet> tileMatrixSets) {
        this.layerIdentifier = layerIdentifier;
        this.tileSource = tileSource;
        this.tileMatrixSets = tileMatrixSets;
    }

    public final String getLayerIdentifier() {
        return layerIdentifier;
    }

    public final Collection<WmtsTileMatrixSet> getTileMatrixSets() {
        return tileMatrixSets;
    }

    public final WmtsTileSource getTileSource() {
        return tileSource;
    }

    public static WmtsCapabilities readCapabilities(String capabilitiesUrl, String layerIdentifier)
            throws ParserConfigurationException, SAXException, IOException {

        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(capabilitiesUrl);

        Element contentsElement = getChildElement(document.getDocumentElement(), "Contents");
        if (contentsElement == null) {
            throw new IllegalArgumentException("No Contents element found.");
        }

        NodeList layerNodes = contentsElement.getElementsByTagName("Layer");
        Element layerElement = null;

        for (int i = 0; i < layerNodes.getLength(); i++) {
            Element item = (Element) layerNodes.item(i);
            String layerId = getChildElementText(item, "ows:Identifier");

            if (layerId != null
                    && !layerId.isEmpty()
                    && (layerIdentifier == null // first layer
                    || layerIdentifier.equals(layerId))) { // or matching layer

                layerIdentifier = layerId;
                layerElement = item;
                break;
            }
        }

        if (layerElement == null) {
            throw new IllegalArgumentException("No Layer element found.");
        }

        Element resourceUrlElement = getChildElement(layerElement, "ResourceURL");
        String urlTemplate;
        if (resourceUrlElement == null
                || (urlTemplate = resourceUrlElement.getAttribute("template")) == null
                || urlTemplate.isEmpty()) {
            throw new IllegalArgumentException("No valid ResourceURL element found in Layer \"" + layerIdentifier + "\".");
        }

        NodeList styleNodes = layerElement.getElementsByTagName("Style");
        Element styleElement = null;

        for (int i = 0; i < styleNodes.getLength(); i++) {
            Element item = (Element) styleNodes.item(i);

            if ("true".equals(item.getAttribute("isDefault"))) {
                styleElement = item;
                break;
            }

            if (i == 0) {
                styleElement = item;
            }
        }

        if (styleElement == null) {
            throw new IllegalArgumentException("No valid Style element found in Layer \"" + layerIdentifier + "\".");
        }

        String styleId = getChildElementText(styleElement, "ows:Identifier");
        if (styleId == null || styleId.isEmpty()) {
            throw new IllegalArgumentException("No ows:Identifier element found in default Style in Layer \"" + layerIdentifier + "\".");
        }

        NodeList tileMatrixSetLinkNodes = layerElement.getElementsByTagName("TileMatrixSetLink");
        ArrayList<String> tileMatrixSetIds = new ArrayList<>();

        for (int i = 0; i < tileMatrixSetLinkNodes.getLength(); i++) {
            String tileMatrixSetId = getChildElementText((Element) tileMatrixSetLinkNodes.item(i), "TileMatrixSet");
            if (tileMatrixSetId != null && !tileMatrixSetId.isEmpty()) {
                tileMatrixSetIds.add(tileMatrixSetId);
            }
        }

        NodeList tileMatrixSetNodes = contentsElement.getElementsByTagName("TileMatrixSet");
        Collection<WmtsTileMatrixSet> tileMatrixSets = new ArrayList<>(tileMatrixSetNodes.getLength());

        for (int i = 0; i < tileMatrixSetNodes.getLength(); i++) {
            Element item = (Element) tileMatrixSetNodes.item(i);
            String tileMatrixSetId = getChildElementText(item, "ows:Identifier");

            if (tileMatrixSetId != null && tileMatrixSetIds.contains(tileMatrixSetId)) {
                tileMatrixSets.add(readTileMatrixSet(item));
            }
        }

        WmtsTileSource tileSource = new WmtsTileSource(urlTemplate.replace("{Style}", styleId));

        return new WmtsCapabilities(layerIdentifier, tileSource, tileMatrixSets);
    }

    public static WmtsTileMatrixSet readTileMatrixSet(Element tileMatrixSetElement) {
        String identifier = getChildElementText(tileMatrixSetElement, "ows:Identifier");
        if (identifier == null || identifier.isEmpty()) {
            throw new IllegalArgumentException("No ows:Identifier element found in TileMatrixSet.");
        }

        String supportedCrs = getChildElementText(tileMatrixSetElement, "ows:SupportedCRS");
        if (supportedCrs == null || supportedCrs.isEmpty()) {
            throw new IllegalArgumentException("No ows:SupportedCRS element found in TileMatrixSet \"" + identifier + "\".");
        }

        NodeList tileMatrixNodes = tileMatrixSetElement.getElementsByTagName("TileMatrix");
        if (tileMatrixNodes.getLength() == 0) {
            throw new IllegalArgumentException("No TileMatrix elements found in TileMatrixSet \"" + identifier + "\".");
        }

        Collection<WmtsTileMatrix> tileMatrixes = new ArrayList<>(tileMatrixNodes.getLength());
        for (int i = 0; i < tileMatrixNodes.getLength(); i++) {
            tileMatrixes.add(readTileMatrix((Element) tileMatrixNodes.item(i)));
        }

        return new WmtsTileMatrixSet(identifier, supportedCrs, tileMatrixes);
    }

    public static WmtsTileMatrix readTileMatrix(Element tileMatrixElement) {
        String identifier = getChildElementText(tileMatrixElement, "ows:Identifier");
        if (identifier == null || identifier.isEmpty()) {
            throw new IllegalArgumentException("No ows:Identifier element found in TileMatrix.");
        }

        String valueString = getChildElementText(tileMatrixElement, "ScaleDenominator");
        if (valueString == null || valueString.isEmpty()) {
            throw new IllegalArgumentException("No ScaleDenominator element found in TileMatrix \"" + identifier + "\".");
        }

        double scaleDenominator = Double.parseDouble(valueString);

        valueString = getChildElementText(tileMatrixElement, "TopLeftCorner");
        String[] topLeftValues;
        if (valueString == null || valueString.isEmpty()
                || (topLeftValues = valueString.split(" ")).length != 2) {
            throw new IllegalArgumentException("No TopLeftCorner element found in TileMatrix \"" + identifier + "\".");
        }

        Point2D topLeft = new Point2D(
                Double.parseDouble(topLeftValues[0]),
                Double.parseDouble(topLeftValues[1]));

        valueString = getChildElementText(tileMatrixElement, "TileWidth");
        if (valueString == null || valueString.isEmpty()) {
            throw new IllegalArgumentException("No TileWidth element found in TileMatrix \"" + identifier + "\".");
        }

        int tileWidth = Integer.parseInt(valueString);

        valueString = getChildElementText(tileMatrixElement, "TileHeight");
        if (valueString == null || valueString.isEmpty()) {
            throw new IllegalArgumentException("No TileHeight element found in TileMatrix \"" + identifier + "\".");
        }

        int tileHeight = Integer.parseInt(valueString);

        valueString = getChildElementText(tileMatrixElement, "MatrixWidth");
        if (valueString == null || valueString.isEmpty()) {
            throw new IllegalArgumentException("No MatrixWidth element found in TileMatrix \"" + identifier + "\".");
        }

        int matrixWidth = Integer.parseInt(valueString);

        valueString = getChildElementText(tileMatrixElement, "MatrixHeight");
        if (valueString == null || valueString.isEmpty()) {
            throw new IllegalArgumentException("No MatrixHeight element found in TileMatrix \"" + identifier + "\".");
        }

        int matrixHeight = Integer.parseInt(valueString);

        return new WmtsTileMatrix(identifier, scaleDenominator, topLeft, tileWidth, tileHeight, matrixWidth, matrixHeight);
    }

    private static Element getChildElement(Element element, String tagName) {
        NodeList elements = element.getElementsByTagName(tagName);

        return elements.getLength() > 0 ? (Element) elements.item(0) : null;
    }

    private static String getChildElementText(Element element, String tagName) {
        element = getChildElement(element, tagName);

        return element != null ? element.getTextContent() : null;
    }
}
