/*
 * FX Map Control - https://github.com/ClemensFischer/FX-Map-Control
 * © 2015 Clemens Fischer
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
 * Default ITileCache implementation. Caches tile image files in a directory given by the rootDirectory property.
 */
public class ImageFileCache implements ITileCache {

    // For compatibility with XAML Map Control ImageFileCache, expiration dates are stored as .NET DateTime ticks,
    // i.e. 100-nanosecond intervals since 0001/01/01 00:00:00 UTC. The DATETIME_OFFSET and DATETIME_FACTOR constants
    // are used to convert to and from java.util.Date milliseconds since 1970/01/01 00:00:00 UTC.
    private static final long DATETIME_OFFSET = 62135596800000L;
    private static final long DATETIME_FACTOR = 10000L;
    private static final ByteBuffer EXPIRATION_MARKER = ByteBuffer.wrap("EXPIRES:".getBytes(StandardCharsets.US_ASCII));

    private final Path rootDirectory;

    public ImageFileCache(Path rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    public ImageFileCache() {
        this(getDefaultRootDirectory());
    }

    public static final Path getDefaultRootDirectory() {
        final String osName = System.getProperty("os.name").toLowerCase();

        if (osName.contains("windows")) {
            final String programData = System.getenv("ProgramData");

            if (programData != null) {
                // use default XAML Map Control cache directory
                return Paths.get(programData, "MapControl", "TileCache");
            }
        } else if (osName.contains("linux")) {
            return Paths.get("/var", "tmp", "FxMapControl-Cache");
        }

        return null;
    }

    public final Path getRootDirectory() {
        return rootDirectory;
    }

    @Override
    public CacheItem get(String tileLayerName, int x, int y, int zoomLevel) {
        final File cacheDir = rootDirectory
                .resolve(tileLayerName)
                .resolve(Integer.toString(zoomLevel))
                .resolve(Integer.toString(x)).toFile();
        final String fileNameFilter = Integer.toString(y) + ".";

        if (cacheDir.isDirectory()) {
            File[] cacheFiles = cacheDir.listFiles((dir, name) -> name.startsWith(fileNameFilter));

            if (cacheFiles.length > 0) {
                //System.out.println("Reading " + cacheFiles[0].getPath());
                try {
                    final byte[] buffer = new byte[(int) cacheFiles[0].length()];
                    long expiration = 0;

                    try (FileInputStream fileStream = new FileInputStream(cacheFiles[0])) {
                        fileStream.read(buffer);
                    }

                    if (buffer.length >= 16 && ByteBuffer.wrap(buffer, buffer.length - 16, 8).equals(EXPIRATION_MARKER)) {
                        expiration = ByteBuffer.wrap(buffer, buffer.length - 8, 8).order(ByteOrder.LITTLE_ENDIAN)
                                .getLong() / DATETIME_FACTOR - DATETIME_OFFSET;
                    }

                    return new CacheItem(buffer, expiration);

                } catch (IOException ex) {
                    Logger.getLogger(ImageFileCache.class.getName()).log(Level.WARNING, ex.toString());
                }
            }
        }

        return null;
    }

    @Override
    public void set(String tileLayerName, int x, int y, int zoomLevel, byte[] buffer, long expiration) {
        final File cacheFile = rootDirectory
                .resolve(tileLayerName)
                .resolve(Integer.toString(zoomLevel))
                .resolve(Integer.toString(x))
                .resolve(String.format("%d%s", y, getFileExtension(buffer))).toFile();

        //System.out.println("Writing " + cacheFile.getPath() + ", Expires " + new Date(expiration));
        try {
            cacheFile.getParentFile().mkdirs();

            try (FileOutputStream fileStream = new FileOutputStream(cacheFile)) {
                fileStream.write(buffer, 0, buffer.length);
                fileStream.write(EXPIRATION_MARKER.array());
                fileStream.write(ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
                        .putLong((expiration + DATETIME_OFFSET) * DATETIME_FACTOR).array());
            }

            cacheFile.setReadable(true, false);
            cacheFile.setWritable(true, false);
        } catch (IOException ex) {
            Logger.getLogger(ImageFileCache.class.getName()).log(Level.WARNING, ex.toString());
        }
    }

    private static String getFileExtension(byte[] buffer) {
        if (buffer.length >= 8
                && buffer[0] == (byte) 0x89
                && buffer[1] == (byte) 0x50
                && buffer[2] == (byte) 0x4E
                && buffer[3] == (byte) 0x47
                && buffer[4] == (byte) 0x0D
                && buffer[5] == (byte) 0x0A
                && buffer[6] == (byte) 0x1A
                && buffer[7] == (byte) 0x0A) {
            return ".png";
        }

        if (buffer.length >= 3
                && buffer[0] == (byte) 0xFF
                && buffer[1] == (byte) 0xD8
                && buffer[2] == (byte) 0xFF) {
            return ".jpg";
        }

        if (buffer.length >= 3
                && buffer[0] == (byte) 0x47
                && buffer[1] == (byte) 0x49
                && buffer[2] == (byte) 0x46) {
            return ".gif";
        }

        if (buffer.length >= 2
                && buffer[0] == (byte) 0x42
                && buffer[1] == (byte) 0x4D) {
            return ".bmp";
        }

        if (buffer.length >= 4
                && buffer[0] == (byte) 0x49
                && buffer[1] == (byte) 0x49
                && buffer[2] == (byte) 0x2A
                && buffer[3] == (byte) 0x00) {
            return ".tif";
        }

        if (buffer.length >= 4
                && buffer[0] == (byte) 0x4D
                && buffer[1] == (byte) 0x4D
                && buffer[2] == (byte) 0x00
                && buffer[3] == (byte) 0x2A) {
            return ".tif";
        }

        return ".bin";
    }
}
