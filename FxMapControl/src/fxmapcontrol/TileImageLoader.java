/*
 * FX Map Control - https://github.com/ClemensFischer/FX-Map-Control
 * © 2015 Clemens Fischer
 */
package fxmapcontrol;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
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

    // conversion of java.util.Date to .NET System.DateTime, i.e. milliseconds since 1970-01-01
    // to 100 nanoseconds intervals since 0001-01-01 00:00:00 UTC.
    private static final long DATETIME_OFFSET = 62135596800000L;
    private static final long DATETIME_FACTOR = 10000L;

    private static final ByteBuffer expirationMarker = ByteBuffer.wrap("EXPIRES:".getBytes(StandardCharsets.US_ASCII));

    private static final ThreadFactory threadFactory = runnable -> {
        final Thread thread = new Thread(runnable, TileImageLoader.class.getSimpleName() + " Thread");
        thread.setDaemon(true);
        return thread;
    };

    private static Path cacheRootFolderPath;

    static {
        final String osName = System.getProperty("os.name").toLowerCase();

        if (osName.contains("windows")) {
            final String programData = System.getenv("ProgramData");

            if (programData != null) {
                // use XAML Map Control cache folder
                cacheRootFolderPath = Paths.get(programData, "MapControl", "TileCache");
            }
        } else if (osName.contains("linux")) {
            cacheRootFolderPath = Paths.get("/var", "tmp", "FxMapControl-Cache");
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
                    final String tileUri = tileLayer.getTileSource().getUri(tile.getXIndex(), tile.getY(), tile.getZoomLevel());
                    final String tileLayerName = tileLayer.getName();

                    if (cacheRootFolderPath == null
                            || tileLayerName == null
                            || tileLayerName.isEmpty()
                            || tileUri.startsWith("file://")) {
                        // no caching, create Image directly from URI
                        return new Image(tileUri);
                    }

                    final Path cacheFilePath = cacheRootFolderPath.resolve(Paths.get(
                            tileLayerName, Integer.toString(tile.getZoomLevel()), Integer.toString(tile.getXIndex()),
                            String.format("%d.%s", tile.getY(), tileLayer.getTileSource().getImageType())));
                    final File cacheFile = cacheFilePath.toFile();
                    Image image = null;

                    if (cacheFile.exists()) {
                        final byte[] buffer;

                        try (FileInputStream fileStream = new FileInputStream(cacheFile)) {
                            buffer = new byte[(int) cacheFile.length()];
                            fileStream.read(buffer);
                        }

                        try (ByteArrayInputStream memoryStream = new ByteArrayInputStream(buffer)) {
                            image = new Image(memoryStream);
                        }

                        if (buffer.length >= 16 && ByteBuffer.wrap(buffer, buffer.length - 16, 8).equals(expirationMarker)) {

                            Date expires = new Date(ByteBuffer.wrap(buffer, buffer.length - 8, 8)
                                    .order(ByteOrder.LITTLE_ENDIAN).getLong() / DATETIME_FACTOR - DATETIME_OFFSET);

                            if (expires.after(new Date())) {
                                // cached image not expired
                                return image;
                            }
                        }
                    }

                    ImageStream imageStream = null;
                    long expires = 0;

                    try {
                        final HttpURLConnection connection = (HttpURLConnection) new URL(tileUri).openConnection();

                        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                            try (InputStream inputStream = connection.getInputStream()) {
                                imageStream = new ImageStream(inputStream);
                                image = imageStream.getImage();
                            }

                            expires = connection.getExpiration();
                            if (expires <= 0) {
                                expires = new Date().getTime() + 24 * 60 * 60 * 1000;
                            }
                            // convert to 100 nanoseconds intervals since 0001-01-01 00:00:00 UTC
                            expires = (expires + DATETIME_OFFSET) * DATETIME_FACTOR;
                        }

                        Logger.getLogger(TileImageLoader.class.getName()).log(Level.INFO,
                                String.format("%s: %d %s", tileUri, connection.getResponseCode(), connection.getResponseMessage()));

                    } catch (Exception ex) {
                        Logger.getLogger(TileImageLoader.class.getName()).log(Level.WARNING, ex.toString());
                    }

                    if (image != null && imageStream != null) {
                        // download succeeded, write image to cache
                        try {
                            cacheFile.getParentFile().mkdirs();
                            
                            try (FileOutputStream fileStream = new FileOutputStream(cacheFile)) {
                                fileStream.write(imageStream.getBuffer(), 0, imageStream.getLength());
                                fileStream.write(expirationMarker.array());
                                fileStream.write(ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(expires).array());
                            }

                            cacheFile.setReadable(true, false);
                            cacheFile.setWritable(true, false);
                        } catch (Exception ex) {
                            Logger.getLogger(TileImageLoader.class.getName()).log(Level.WARNING, ex.toString());
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
            final Image image = new Image(this);
            reset();
            return image;
        }
    }
}
