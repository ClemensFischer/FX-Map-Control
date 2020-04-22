/*
 * FX Map Control - https://github.com/ClemensFischer/FX-Map-Control
 * © 2020 Clemens Fischer
 */
package fxmapcontrol;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Default ITileCache implementation. Caches tile image files in a directory given by the rootDirectory
 * property.
 */
public class ImageFileCache implements ITileCache {

    // For compatibility with XAML Map Control ImageFileCache, expiration dates are stored as .NET DateTime ticks,
    // i.e. 100-nanosecond intervals since 0001/01/01 00:00:00 UTC. The datetimeOffset and datetimeFactor constants
    // are used to convert to and from java.util.Date milliseconds since 1970/01/01 00:00:00 UTC.
    //
    private static final long datetimeOffset = 62135596800000L;
    private static final long datetimeFactor = 10000L;
    private static final ByteBuffer expirationMarker = ByteBuffer.wrap("EXPIRES:".getBytes(StandardCharsets.US_ASCII));

    private final Path rootDirectory;

    public ImageFileCache(Path rootDirectory) {
        this.rootDirectory = rootDirectory;
        //System.out.println(rootDirectory.toAbsolutePath());
    }

    public ImageFileCache() {
        this(getDefaultRootDirectory());
    }

    public static final Path getDefaultRootDirectory() {
        String osName = System.getProperty("os.name").toLowerCase();

        if (osName.contains("windows")) {
            String programData = System.getenv("ProgramData");

            if (programData != null) {
                // use default XAML Map Control cache directory
                return Paths.get(programData, "MapControl", "TileCache");
            }
        } else {//if (osName.contains("linux")) {
            return Paths.get("/var", "tmp", "FxMapControl-Cache");
        }

        return null;
    }

    public final Path getRootDirectory() {
        return rootDirectory;
    }

    @Override
    public CacheItem get(String key) {
        try {
            File cacheFile = getFile(key);

            if (cacheFile.isFile()) {
                //System.out.println("Reading " + cacheFile.getPath());
                byte[] buffer = new byte[(int) cacheFile.length()];
                long expiration = 0;

                try (FileInputStream fileStream = new FileInputStream(cacheFile)) {
                    fileStream.read(buffer);
                }

                if (buffer.length >= 16 && ByteBuffer.wrap(buffer, buffer.length - 16, 8).equals(expirationMarker)) {
                    expiration = ByteBuffer.wrap(buffer, buffer.length - 8, 8).order(ByteOrder.LITTLE_ENDIAN)
                            .getLong() / datetimeFactor - datetimeOffset;
                }

                return new CacheItem(buffer, expiration);
            }

        } catch (IOException ex) {
            Logger.getLogger(ImageFileCache.class.getName()).log(Level.WARNING, ex.toString());
        }

        return null;
    }

    @Override
    public void set(String key, byte[] buffer, long expiration) {
        try {
            File cacheFile = getFile(key);
            //System.out.println("Writing " + cacheFile.getPath() + ", Expires " + new java.util.Date(expiration));
            cacheFile.getParentFile().mkdirs();

            try (FileOutputStream fileStream = new FileOutputStream(cacheFile)) {
                fileStream.write(buffer, 0, buffer.length);
                fileStream.write(expirationMarker.array());
                fileStream.write(ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
                        .putLong((expiration + datetimeOffset) * datetimeFactor).array());
            }

            cacheFile.setReadable(true, false);
            cacheFile.setWritable(true, false);
        } catch (IOException ex) {
            Logger.getLogger(ImageFileCache.class.getName()).log(Level.WARNING, ex.toString());
        }
    }

    private File getFile(String key) {
        key = key.replace(",", "/").replace(":", "/").replace(";", "/");

        return rootDirectory.resolve(key).toFile();
    }
}
