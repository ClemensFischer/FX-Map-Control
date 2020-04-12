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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.image.Image;

/**
 * Default ITileImageLoader implementation. Optionally caches tile images in a static ITileCache instance.
 */
public class TileImageLoader implements ITileImageLoader {

    private static final int DEFAULT_THREADPOOL_SIZE = 8;
    private static final int DEFAULT_HTTP_TIMEOUT = 10; // seconds
    private static final int DEFAULT_CACHE_EXPIRATION = 3600 * 24; // one day

    private static final ThreadFactory threadFactory = runnable -> {
        Thread thread = new Thread(runnable);
        thread.setDaemon(true);
        return thread;
    };

    private static ITileCache cache;

    public static void setCache(ITileCache cache) {
        TileImageLoader.cache = cache;
    }

    private final Set<LoadImageService> pendingServices = Collections.synchronizedSet(new HashSet<LoadImageService>());
    private final ExecutorService serviceExecutor;
    private final int httpTimeout;

    public TileImageLoader() {
        this(DEFAULT_THREADPOOL_SIZE, DEFAULT_HTTP_TIMEOUT);
    }

    public TileImageLoader(int threadPoolSize, int httpTimeout) {
        serviceExecutor = Executors.newFixedThreadPool(threadPoolSize, threadFactory);
        this.httpTimeout = httpTimeout * 1000;
    }

    @Override
    public void loadTiles(String tileLayerName, TileSource tileSource, Iterable<Tile> tiles) {
        pendingServices.forEach(s -> ((LoadImageService) s).cancel());
        pendingServices.clear();

        for (Tile tile : tiles) {
            if (tile.isPending()) {
                LoadImageService service = new LoadImageService(tile, tileSource, tileLayerName);
                pendingServices.add(service);
                service.start();
            }
        }
    }

    private class LoadImageService extends Service<Image> {

        private final Tile tile;
        private final TileSource tileSource;
        private final String tileLayerName;

        public LoadImageService(Tile tile, TileSource tileSource, String tileLayerName) {
            this.tile = tile;
            this.tileSource = tileSource;
            this.tileLayerName = tileLayerName;
            setExecutor(serviceExecutor);
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
            return new LoadImageTask(tile, tileSource, tileLayerName);
        }
    }

    private class LoadImageTask extends Task<Image> {

        private final Tile tile;
        private final TileSource tileSource;
        private final String tileLayerName;

        public LoadImageTask(Tile tile, TileSource tileSource, String tileLayerName) {
            this.tile = tile;
            this.tileSource = tileSource;
            this.tileLayerName = tileLayerName;
        }

        @Override
        protected Image call() throws Exception {
            Image image;

            if (cache == null
                    || tileLayerName == null
                    || tileLayerName.isEmpty()
                    || !tileSource.getUrlFormat().startsWith("http")) {

                // no caching, get Image directly from TileSource
                image = tileSource.getImage(tile.getXIndex(), tile.getY(), tile.getZoomLevel());
            } else {
                image = loadCachedImage(tileSource.getUrl(tile.getXIndex(), tile.getY(), tile.getZoomLevel()));
            }

            return image;
        }

        private Image loadCachedImage(String tileUrl) throws Exception {
            Image image = null;
            long now = new Date().getTime();
            ITileCache.CacheItem cacheItem = cache.get(tileLayerName, tile.getXIndex(), tile.getY(), tile.getZoomLevel());

            if (cacheItem != null) {
                try {
                    try (ByteArrayInputStream memoryStream = new ByteArrayInputStream(cacheItem.getBuffer())) {
                        image = new Image(memoryStream);
                    }
                } catch (IOException ex) {
                    Logger.getLogger(TileImageLoader.class.getName()).log(Level.WARNING, ex.toString());
                }
            }

            if (image == null || cacheItem == null || cacheItem.getExpiration() < now) { // no cached image or cache expired
                ImageStream imageStream = null;
                int expiration = 0;

                try {
                    HttpURLConnection connection = (HttpURLConnection) new URL(tileUrl).openConnection();
                    connection.setConnectTimeout(httpTimeout);
                    connection.setReadTimeout(httpTimeout);
                    connection.connect();

                    if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                        Logger.getLogger(TileImageLoader.class.getName()).log(Level.WARNING,
                                String.format("%s: %d %s", tileUrl, connection.getResponseCode(), connection.getResponseMessage()));

                    } else if (isTileAvailable(connection)) {
                        try (InputStream inputStream = connection.getInputStream()) {
                            imageStream = new ImageStream(inputStream);
                            image = imageStream.getImage();
                        }

                        expiration = getCacheExpiration(connection);
                    }
                } catch (Exception ex) {
                    Logger.getLogger(TileImageLoader.class.getName()).log(Level.WARNING,
                            String.format("%s: %s", tileUrl, ex.toString()));

                    throw ex; // invoke LoadImageService.failed(), i.e. don't call tile.setImage()
                }

                if (image != null && imageStream != null && expiration > 0) { // download succeeded, write image to cache
                    cache.set(tileLayerName,
                            tile.getXIndex(), tile.getY(), tile.getZoomLevel(),
                            imageStream.getBuffer(), now + 1000L * expiration);
                }
            }

            return image;
        }

        private boolean isTileAvailable(HttpURLConnection connection) {
            String tileInfo = connection.getHeaderField("X-VE-Tile-Info");

            return tileInfo == null || !tileInfo.contains("no-tile");
        }

        private int getCacheExpiration(HttpURLConnection connection) {
            int expiration = DEFAULT_CACHE_EXPIRATION;
            String cacheControl = connection.getHeaderField("cache-control");

            if (cacheControl != null) {
                String maxAge = Arrays.stream(cacheControl.split(","))
                        .filter(s -> s.contains("max-age="))
                        .findFirst().orElse(null);

                if (maxAge != null) {
                    try {
                        expiration = Integer.parseInt(maxAge.trim().substring(8));
                    } catch (NumberFormatException ex) {
                    }
                }
            }

            return expiration;
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
