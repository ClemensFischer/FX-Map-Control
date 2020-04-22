/*
 * FX Map Control - https://github.com/ClemensFischer/FX-Map-Control
 * © 2020 Clemens Fischer
 */
package fxmapcontrol;

/**
 * Provides methods for caching tile image buffers.
 */
public interface ITileCache {

    public static class CacheItem {
        private final byte[] buffer;
        private final long expiration; // milliseconds since 1970/01/01 00:00:00 UTC

        public CacheItem(byte[] buffer, long expiration) {
            this.buffer = buffer;
            this.expiration = expiration;
        }

        public final byte[] getBuffer() {
            return buffer;
        }

        public final long getExpiration() {
            return expiration;
        }
    }

    CacheItem get(String key);

    void set(String key, byte[] buffer, long expiration);
}
