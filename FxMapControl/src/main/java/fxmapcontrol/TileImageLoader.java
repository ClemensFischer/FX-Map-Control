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
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.image.Image;

/**
 * Default ITileImageLoader implementation. Optionally caches tile images in a static ITileCache instance.
 */
public class TileImageLoader implements ITileImageLoader {

    private static final int DEFAULT_MAX_TASKS = 4;
    private static final int DEFAULT_HTTP_TIMEOUT = 10; // seconds
    private static final int DEFAULT_CACHE_EXPIRATION = 3600 * 24; // one day

    private static final ExecutorService serviceExecutor = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable);
        thread.setDaemon(true);
        return thread;
    });

    private static ITileCache tileCache;

    public static void setCache(ITileCache cache) {
        tileCache = cache;
    }

    private final Set<LoadImageService> services = Collections.synchronizedSet(new HashSet<LoadImageService>());
    private final ConcurrentLinkedQueue<Tile> tileQueue = new ConcurrentLinkedQueue<>();
    private final int maxLoadTasks;
    private final int httpTimeout;

    public TileImageLoader() {
        this(DEFAULT_MAX_TASKS, DEFAULT_HTTP_TIMEOUT);
    }

    public TileImageLoader(int maxLoadTasks, int httpTimeout) {
        this.maxLoadTasks = maxLoadTasks;
        this.httpTimeout = httpTimeout * 1000;
    }

    @Override
    public void loadTiles(String tileLayerName, TileSource tileSource, Collection<Tile> tiles) {
        tileQueue.clear();

        Stream<Tile> pendingTiles = tiles.stream().filter(tile -> tile.isPending());

        if (tileCache == null
                || tileLayerName == null
                || tileLayerName.isEmpty()
                || !tileSource.getUrlFormat().startsWith("http")) {

            pendingTiles.forEach(tile -> {
                Image image = tileSource.getImage(tile.getXIndex(), tile.getY(), tile.getZoomLevel());

                tile.setImage(image, true);
            });

        } else {
            tiles = pendingTiles.collect(Collectors.toList());
            tileQueue.addAll(tiles);

            int numServices = Math.min(tiles.size(), maxLoadTasks);

            while (services.size() < numServices) {
                services.add(new LoadImageService(tileLayerName, tileSource));
            }
        }
    }

    private class LoadImageService extends Service<Image> {

        private final String tileLayerName;
        private final TileSource tileSource;
        private Tile tile;

        public LoadImageService(String tileLayerName, TileSource tileSource) {
            this.tileLayerName = tileLayerName;
            this.tileSource = tileSource;
            setExecutor(serviceExecutor);
            nextTile();
        }

        @Override
        protected void failed() {
            nextTile();
        }

        @Override
        protected void succeeded() {
            tile.setImage(getValue(), true);
            nextTile();
        }

        @Override
        protected Task<Image> createTask() {
            return new Task<Image>() {
                @Override
                protected Image call() throws Exception {
                    return loadImage(tileLayerName, tileSource, tile);
                }
            };
        }

        private void nextTile() {
            tile = tileQueue.poll();
            if (tile != null) {
                restart();
            } else {
                services.remove(this);
            }
        }

        private Image loadImage(String tileLayerName, TileSource tileSource, Tile tile) throws Exception {
            Image image = null;
            long now = new Date().getTime();
            ITileCache.CacheItem cacheItem = tileCache.get(tileLayerName, tile.getXIndex(), tile.getY(), tile.getZoomLevel());

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

                String tileUrl = tileSource.getUrl(tile.getXIndex(), tile.getY(), tile.getZoomLevel());

                try {
                    HttpURLConnection connection = (HttpURLConnection) new URL(tileUrl).openConnection();
                    connection.setConnectTimeout(httpTimeout);
                    connection.setReadTimeout(httpTimeout);
                    connection.connect();

                    if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                        Logger.getLogger(TileImageLoader.class.getName()).log(Level.WARNING,
                                String.format("%s: %d %s", tileUrl, connection.getResponseCode(), connection.getResponseMessage()));

                    } else if (isTileAvailable(connection)) { // may have X-VE-Tile-Info header

                        try (ImageStream imageStream = new ImageStream(connection.getInputStream())) {
                            image = imageStream.getImage();

                            int expiration = getCacheExpiration(connection);

                            if (expiration > 0) { // write image to cache
                                tileCache.set(tileLayerName,
                                        tile.getXIndex(), tile.getY(), tile.getZoomLevel(),
                                        imageStream.getBuffer(), now + 1000L * expiration);
                            }
                        }
                    }
                } catch (Exception ex) {
                    Logger.getLogger(TileImageLoader.class.getName()).log(Level.WARNING,
                            String.format("%s: %s", tileUrl, ex.toString()));

                    throw ex; // do not call tile.setImage(), i.e. keep tile pending
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
