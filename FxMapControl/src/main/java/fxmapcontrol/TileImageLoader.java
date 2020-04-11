/*
 * FX Map Control - https://github.com/ClemensFischer/FX-Map-Control
 * © 2020 Clemens Fischer
 */
package fxmapcontrol;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.image.Image;

/**
 * Default ITileImageLoader implementation. Optionally caches tile images in a static ITileCache instance.
 */
public class TileImageLoader implements ITileImageLoader {

    private static final int MINIMUM_EXPIRATION = 3600; // one hour
    private static final int DEFAULT_EXPIRATION = 3600 * 24; // one day

    private static ITileCache cache;

    public static void setCache(ITileCache cache) {
        TileImageLoader.cache = cache;
    }

    private final Set<LoadImageService> pendingServices = Collections.synchronizedSet(new HashSet<LoadImageService>());

    @Override
    public void loadTiles(String tileLayerName, TileSource tileSource, Iterable<Tile> tiles) {
        pendingServices.forEach(s -> ((LoadImageService) s).cancel());
        pendingServices.clear();

        for (Tile tile : tiles) {
            if (tile.isPending()) {
                LoadImageService service = new LoadImageService(tileLayerName, tileSource, tile);
                pendingServices.add(service);
                service.start();
            }
        }
    }

    private class LoadImageService extends Service<Image> {

        private final String tileLayerName;
        private final TileSource tileSource;
        private final Tile tile;

        public LoadImageService(String tileLayerName, TileSource tileSource, Tile tile) {
            this.tileLayerName = tileLayerName;
            this.tileSource = tileSource;
            this.tile = tile;
        }

        @Override
        protected void running() {
            super.running();
            pendingServices.remove(this);
        }

        @Override
        protected void succeeded() {
            super.succeeded();
            tile.setImage(getValue(), true);
        }

        @Override
        protected Task<Image> createTask() {
            return new Task<Image>() {
                @Override
                protected Image call() {
                    String tileUrl = tileSource.getUrl(tile.getXIndex(), tile.getY(), tile.getZoomLevel());

                    if (cache == null
                            || tileLayerName == null
                            || tileLayerName.isEmpty()
                            || tileUrl.startsWith("file:")) { // no caching, create Image directly from URL
                        return new Image(tileUrl);
                    }

                    long now = new Date().getTime();
                    Image image = null;
                    ITileCache.CacheItem cacheItem = cache.get(
                            tileLayerName, tile.getXIndex(), tile.getY(), tile.getZoomLevel());

                    if (cacheItem != null) {
                        try {
                            try (ByteArrayInputStream memoryStream = new ByteArrayInputStream(cacheItem.getBuffer())) {
                                image = new Image(memoryStream);
                            }

                            if (cacheItem.getExpiration() > now) { // cached image not expired
                                return image;
                            }
                        } catch (IOException ex) {
                            Logger.getLogger(TileImageLoader.class.getName()).log(Level.WARNING, ex.toString());
                        }
                    }

                    ImageStream imageStream = null;
                    int expiration = 0;

                    try {
                        HttpURLConnection connection = (HttpURLConnection) new URL(tileUrl).openConnection();

                        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                            try (InputStream inputStream = connection.getInputStream()) {
                                imageStream = new ImageStream(inputStream);
                                image = imageStream.getImage();
                            }

                            String cacheControl = connection.getHeaderField("cache-control");

                            if (cacheControl != null) {
                                String maxAge = Arrays.stream(cacheControl.split(","))
                                        .filter(s -> s.contains("max-age="))
                                        .findFirst().orElse(null);

                                if (maxAge != null) {
                                    expiration = Integer.parseInt(maxAge.trim().substring(8));
                                }
                            }
                        } else {
                            Logger.getLogger(TileImageLoader.class.getName()).log(Level.WARNING,
                                    String.format("%s: %d %s", tileUrl, connection.getResponseCode(), connection.getResponseMessage()));
                        }
                    } catch (IOException | NumberFormatException ex) {
                        Logger.getLogger(TileImageLoader.class.getName()).log(Level.WARNING,
                                String.format("%s: %s", tileUrl, ex.toString()));
                    }

                    if (image != null && imageStream != null) { // download succeeded, write image to cache
                        if (expiration <= 0) {
                            expiration = DEFAULT_EXPIRATION;
                        } else if (expiration < MINIMUM_EXPIRATION) {
                            expiration = MINIMUM_EXPIRATION;
                        }

                        cache.set(tileLayerName, tile.getXIndex(), tile.getY(), tile.getZoomLevel(),
                                imageStream.getBuffer(), now + 1000L * expiration);
                    }

                    return image;
                }
            };
        }
    }

    private static class ImageStream extends BufferedInputStream {

        public ImageStream(InputStream inputStream) {
            super(inputStream);
        }

        public int getLength() {
            return count;
        }

        public byte[] getBuffer() {
            return buf;
        }

        public Image getImage() throws IOException {
            mark(Integer.MAX_VALUE);
            Image image = new Image(this);
            reset();
            return image;
        }
    }
}
