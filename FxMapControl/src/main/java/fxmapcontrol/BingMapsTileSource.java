/*
 * FX Map Control - https://github.com/ClemensFischer/FX-Map-Control
 * © 2019 Clemens Fischer
 */
package fxmapcontrol;

class BingMapsTileSource extends TileSource {

    private final String[] subdomains;

    public BingMapsTileSource(String urlFormat, String[] subdomains) {
        super(urlFormat);
        this.subdomains = subdomains;
    }

    @Override
    public String getUrl(int x, int y, int zoomLevel) {
        if (zoomLevel < 1) {
            return null;
        }

        String subdomain = subdomains[(x + y) % subdomains.length];
        char[] quadkey = new char[zoomLevel];

        for (int z = zoomLevel - 1; z >= 0; z--, x /= 2, y /= 2) {
            quadkey[z] = (char) ('0' + 2 * (y % 2) + (x % 2));
        }

        return getUrlFormat()
                .replace("{subdomain}", subdomain)
                .replace("{quadkey}", new String(quadkey));
    }
}
