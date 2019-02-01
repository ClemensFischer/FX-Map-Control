/*
 * FX Map Control - https://github.com/ClemensFischer/FX-Map-Control
 * © 2019 Clemens Fischer
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

    public static double nearestLongitude(double longitude, double referenceLongitude) {
        longitude = normalizeLongitude(longitude);
        if (longitude > referenceLongitude + 180d) {
            longitude -= 360d;
        } else if (longitude < referenceLongitude - 180d) {
            longitude += 360d;
        }
        return longitude;
    }

    public static double greatCircleDistance(Location location1, Location location2) {
        double lat1 = location1.getLatitude() * Math.PI / 180d;
        double lon1 = location1.getLongitude() * Math.PI / 180d;
        double lat2 = location2.getLatitude() * Math.PI / 180d;
        double lon2 = location2.getLongitude() * Math.PI / 180d;
        double sinLat1 = Math.sin(lat1);
        double cosLat1 = Math.cos(lat1);
        double sinLat2 = Math.sin(lat2);
        double cosLat2 = Math.cos(lat2);
        double cosLon12 = Math.cos(lon2 - lon1);
        double cosS12 = sinLat1 * sinLat2 + cosLat1 * cosLat2 * cosLon12;
        double s12 = 0d;

        if (Math.abs(cosS12) < 0.99999999) {
            s12 = Math.acos(Math.min(Math.max(cosS12, -1d), 1d));
        } else {
            double sinLon12 = Math.sin(lon2 - lon1);
            double a = cosLat1 * sinLat2 - sinLat1 * cosLat2 * cosLon12;
            double b = cosLat2 * sinLon12;
            s12 = Math.atan2(Math.sqrt(a * a + b * b), cosS12);
        }

        return MapProjection.WGS84_EQUATORIAL_RADIUS * s12;
    }

    public static Location greatCircleLocation(Location location, double azimuth, double distance) {
        double s12 = distance / MapProjection.WGS84_EQUATORIAL_RADIUS;
        double az1 = azimuth * Math.PI / 180d;
        double lat1 = location.getLatitude() * Math.PI / 180d;
        double lon1 = location.getLongitude() * Math.PI / 180d;
        double sinS12 = Math.sin(s12);
        double cosS12 = Math.cos(s12);
        double sinAz1 = Math.sin(az1);
        double cosAz1 = Math.cos(az1);
        double sinLat1 = Math.sin(lat1);
        double cosLat1 = Math.cos(lat1);
        double lat2 = Math.asin(sinLat1 * cosS12 + cosLat1 * sinS12 * cosAz1);
        double lon2 = lon1 + Math.atan2(sinS12 * sinAz1, (cosLat1 * cosS12 - sinLat1 * sinS12 * cosAz1));

        return new Location(lat2 / Math.PI * 180d, lon2 / Math.PI * 180d);
    }
}
