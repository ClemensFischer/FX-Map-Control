/*
 * FX Map Control - https://github.com/ClemensFischer/FX-Map-Control
 * © 2015 Clemens Fischer
 */
package fxmapcontrol;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
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
 * Default ITileImageLoader implementation. Caches tile image files in a folder given by the
 * cacheRootFolderPath property.
 */
public class TileImageLoader implements ITileImageLoader {

    private static final ThreadFactory threadFactory = runnable -> {
        Thread thread = new Thread(runnable, TileImageLoader.class.getSimpleName() + " Thread");
        thread.setDaemon(true);
        return thread;
    };

    private static Path cacheRootFolderPath;

    static {
        String programData = System.getenv("ProgramData");

        if (programData != null) {
            cacheRootFolderPath = Paths.get(programData, "MapControl", "TileCache");
        }
    }

    public static Path getCacheRootFolderPath() {
        return cacheRootFolderPath;
    }

    public static void setCacheRootFolderPath(Path cacheRootFolderPath) {
        TileImageLoader.cacheRootFolderPath = cacheRootFolderPath;
    }

    private final Set<LoadImageService> pendingServices = Collections.synchronizedSet(new HashSet<LoadImageService>());
    private int threadPoolSize;
    private ExecutorService serviceExecutor;

    @Override
    public void beginLoadTiles(MapTileLayer tileLayer, Iterable<Tile> tiles) {
        if (threadPoolSize != tileLayer.getMaxDownloadThreads()) {
            threadPoolSize = tileLayer.getMaxDownloadThreads();
            if (serviceExecutor != null) {
                serviceExecutor.shutdown();
            }
            serviceExecutor = Executors.newFixedThreadPool(threadPoolSize, threadFactory);
        }

        for (Tile tile : tiles) {
            LoadImageService service = new LoadImageService(tileLayer, tile);
            pendingServices.add(service);
            service.start();
        }
    }

    @Override
    public void cancelLoadTiles(MapTileLayer tileLayer) {
        pendingServices.forEach(s -> ((LoadImageService) s).cancel());
        pendingServices.clear();
    }

    private class LoadImageService extends Service<Image> {

        private final MapTileLayer tileLayer;
        private final Tile tile;

        public LoadImageService(MapTileLayer tileLayer, Tile tile) {
            this.tileLayer = tileLayer;
            this.tile = tile;
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
            return new Task<Image>() {
                @Override
                protected Image call() throws Exception {
                    String tileUri = tileLayer.getTileSource().getUri(tile.getXIndex(), tile.getY(), tile.getZoomLevel());
                    String tileLayerName = tileLayer.getName();

                    if (cacheRootFolderPath == null
                            || tileLayerName == null
                            || tileLayerName.isEmpty()
                            || tileUri.startsWith("file://")) {
                        // no caching, create Image directly from URI
                        return new Image(tileUri);
                    }

                    Path cacheFilePath = cacheRootFolderPath.resolve(Paths.get(
                            tileLayerName, Integer.toString(tile.getZoomLevel()), Integer.toString(tile.getXIndex()),
                            String.format("%d.%s", tile.getY(), tileLayer.getTileSource().getImageType())));
                    File cacheFile = cacheFilePath.toFile();

                    if (cacheFile.exists() && cacheFile.lastModified() > new Date().getTime()) {
                        // cached image not expired
                        try (FileInputStream fileStream = new FileInputStream(cacheFile)) {
                            return new Image(fileStream);
                        }
                    }

                    Image image = null;
                    ImageStream imageStream = null;
                    long expires = 0;

                    try {
                        URL url = new URL(tileUri);
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        int responseCode = connection.getResponseCode();

                        if (responseCode == HttpURLConnection.HTTP_OK) {
                            try (InputStream inputStream = connection.getInputStream()) {
                                imageStream = new ImageStream(inputStream);
                                image = imageStream.getImage();

                                expires = connection.getExpiration();
                                if (expires <= 0) {
                                    expires = new Date().getTime() + 7 * 24 * 60 * 60 * 1000;
                                }
                            }
                        }

                        Logger.getLogger(TileImageLoader.class.getName()).log(Level.INFO,
                                String.format("%s: %d %s", tileUri, responseCode, connection.getResponseMessage()));

                    } catch (Exception ex) {
                        Logger.getLogger(TileImageLoader.class.getName()).log(Level.WARNING, null, ex);
                    }

                    if (image != null && imageStream != null) {
                        // download succeeded, write image to cache
                        try {
                            cacheFile.getParentFile().mkdirs();

                            try (FileOutputStream fileStream = new FileOutputStream(cacheFile)) {
                                fileStream.write(imageStream.getBuffer(), 0, imageStream.getLength());
                            }

                            cacheFile.setLastModified(expires);
                        } catch (Exception ex) {
                            Logger.getLogger(TileImageLoader.class.getName()).log(Level.WARNING, null, ex);
                        }

                    } else if (cacheFile.exists()) {
                        // download failed, use expired cached image if available
                        try (FileInputStream fileStream = new FileInputStream(cacheFile)) {
                            image = new Image(fileStream);
                        }
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
