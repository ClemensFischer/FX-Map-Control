/*
 * FX Map Control - https://github.com/ClemensFischer/FX-Map-Control
 * © 2020 Clemens Fischer
 */
package fxmapcontrol;

import fxmapcontrol.ITileCache.CacheItem;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.image.Image;

/**
 * Default ITileImageLoader implementation. Optionally caches tile images in a static ITileCache instance.
 */
public class TileImageLoader implements ITileImageLoader {

    private static final int defaultMaxTasks = 4;
    private static final int defaultHttpTimeout = 10; // seconds
    private static final int defaultCacheExpiration = 3600 * 24; // one day

    private static final ExecutorService serviceExecutor = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable);
        thread.setDaemon(true);
        return thread;
    });

    private static ITileCache tileCache;

    public static void setCache(ITileCache cache) {
        tileCache = cache;
    }

    private final ConcurrentLinkedQueue<Tile> tileQueue = new ConcurrentLinkedQueue<>();
    private final HashSet<LoadImageService> services = new HashSet<>();
    private final int maxLoadTasks;
    private final int httpTimeout;

    public TileImageLoader() {
        this(defaultMaxTasks, defaultHttpTimeout);
    }

    public TileImageLoader(int maxLoadTasks, int httpTimeout) {
        this.maxLoadTasks = maxLoadTasks;
        this.httpTimeout = httpTimeout * 1000;
    }

    @Override
    public void loadTiles(Collection<Tile> tiles, TileSource tileSource, String tileSourceName) {
        tiles = tiles.stream().filter(tile -> tile.isPending()).collect(Collectors.toList());
        tileQueue.clear();

        if (tileSource != null && !tiles.isEmpty()) {
            tileQueue.addAll(tiles);

            int numServices = Math.min(tiles.size(), maxLoadTasks);

            while (services.size() < numServices) {
                services.add(new LoadImageService(tileSource, tileSourceName));
            }
        }
    }

    private class LoadImageService extends Service<Image> {

        private final TileSource tileSource;
        private final String tileSourceName;
        private Tile tile;

        public LoadImageService(TileSource tileSource, String tileSourceName) {
            this.tileSource = tileSource;
            this.tileSourceName = tileSourceName;
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
                    return loadImage();
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

        private Image loadImage() throws Exception {
            Image image;

            if (tileCache == null
                    || tileSourceName == null
                    || tileSourceName.isEmpty()
                    || !tileSource.getUrlFormat().startsWith("http")) {

                image = tileSource.getImage(tile.getXIndex(), tile.getY(), tile.getZoomLevel(), false);
            } else {
                image = loadCachedImage();
            }

            return image;
        }

        private Image loadCachedImage() throws Exception {
            Image image = null;
            CacheItem cacheItem = tileCache.get(tileSourceName, tile.getXIndex(), tile.getY(), tile.getZoomLevel());

            if (cacheItem != null) {
                try {
                    try (ByteArrayInputStream memoryStream = new ByteArrayInputStream(cacheItem.getBuffer())) {
                        image = new Image(memoryStream);
                    }
                } catch (Exception ex) {
                    Logger.getLogger(TileImageLoader.class.getName()).log(Level.WARNING, ex.toString());
                }
            }

            if (image == null
                    || cacheItem == null
                    || cacheItem.getExpiration() < new Date().getTime()) { // no cached image or cache expired

                String tileUrl = tileSource.getUrl(tile.getXIndex(), tile.getY(), tile.getZoomLevel());

                try {
                    HttpURLConnection connection = (HttpURLConnection) new URL(tileUrl).openConnection();
                    connection.setConnectTimeout(httpTimeout);
                    connection.setReadTimeout(httpTimeout);
                    connection.connect();

                    if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                        Logger.getLogger(TileImageLoader.class.getName()).log(Level.WARNING, "{0}: {1} {2}",
                                new Object[]{tileUrl, connection.getResponseCode(), connection.getResponseMessage()});

                    } else if (isTileAvailable(connection)) { // check headers
                        try (ImageStream imageStream = new ImageStream(connection.getInputStream())) {
                            image = imageStream.getImage();

                            tileCache.set(tileSourceName,
                                    tile.getXIndex(), tile.getY(), tile.getZoomLevel(),
                                    imageStream.getBuffer(),
                                    getCacheExpiration(connection));
                        }
                    }
                } catch (Exception ex) {
                    Logger.getLogger(TileImageLoader.class.getName()).log(Level.WARNING, "{0}: {1}", new Object[]{tileUrl, ex});

                    throw ex; // do not call tile.setImage(), i.e. keep tile pending
                }
            }

            return image;
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

    private static boolean isTileAvailable(HttpURLConnection connection) {
        String tileInfo = connection.getHeaderField("X-VE-Tile-Info");

        return tileInfo == null || !tileInfo.contains("no-tile");
    }

    private static long getCacheExpiration(HttpURLConnection connection) {
        int expiration = defaultCacheExpiration;
        String cacheControl = connection.getHeaderField("cache-control");

        if (cacheControl != null) {
            String maxAge = Arrays.stream(cacheControl.split(","))
                    .filter(s -> s.contains("max-age="))
                    .findFirst().orElse(null);

            if (maxAge != null) {
                try {
                    expiration = Math.max(Integer.parseInt(maxAge.trim().substring(8)), 0);
                } catch (NumberFormatException ex) {
                }
            }
        }

        return new Date().getTime() + 1000L * expiration;
    }
}
