/*
 * FX Map Control - https://github.com/ClemensFischer/FX-Map-Control
 * © 2016 Clemens Fischer
 */
package fxmapcontrol;

/**
 * A geographic location with latitude and longitude values in degrees.
 */
public class Location {

    private final double latitude;
    private final double longitude;

    public Location(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public final double getLatitude() {
        return latitude;
    }

    public final double getLongitude() {
        return longitude;
    }

    public final boolean equals(Location location) {
        return this == location
                || (latitude == location.latitude && longitude == location.longitude);
    }

    @Override
    public final boolean equals(Object obj) {
        return (obj instanceof Location) && equals((Location) obj);
    }

    @Override
    public int hashCode() {
        return Double.hashCode(latitude) ^ Double.hashCode(longitude);
    }

    public static Location valueOf(String locationString) {
        String[] pair = locationString.split(",");
        if (pair.length != 2) {
            throw new IllegalArgumentException(
                    "Location string must be a comma-separated pair of double values");
        }
        return new Location(
                Double.parseDouble(pair[0]),
                Double.parseDouble(pair[1]));
    }

    public static double normalizeLongitude(double longitude) {
        if (longitude < -180.) {
            longitude = ((longitude + 180.) % 360.) + 180.;
        } else if (longitude > 180.) {
            longitude = ((longitude - 180.) % 360.) - 180.;
        }
        return longitude;
    }

    static double nearestLongitude(double longitude, double referenceLongitude) {
        longitude = normalizeLongitude(longitude);
        if (longitude > referenceLongitude + 180d) {
            longitude -= 360d;
        } else if (longitude < referenceLongitude - 180d) {
            longitude += 360d;
        }
        return longitude;
    }
}
