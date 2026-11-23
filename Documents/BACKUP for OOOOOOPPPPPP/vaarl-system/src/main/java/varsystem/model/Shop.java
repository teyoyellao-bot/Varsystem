
package varsystem.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Shop implements Serializable {

    private static final long serialVersionUID = 1L;

    public String name, location, phone,
            hours = "8:00 AM - 8:00 PM",
            status = "Active", about, owner = "";

    // Saved shop profile photos
    public String photoPath = "";
    public String photoPath2 = "";
    public String photoPath3 = "";
    public String photoPath4 = "";

    // Additional shop photos
    public String shopPhotoPath = "";
    public String idPicturePath = "";

    // Multiple shop gallery photos
    public List<String> galleryPhotos = new ArrayList<>();

    public double rating, dist, x, y;
    public int reviews;
    public int views = 0;

    // Real shop coordinates
    public double latitude = Double.NaN;
    public double longitude = Double.NaN;

    public boolean hasLocation() {
        return Double.isFinite(latitude)
                && Double.isFinite(longitude)
                && latitude >= -90 && latitude <= 90
                && longitude >= -180 && longitude <= 180;
    }

    public void setLocation(double lat, double lon) {
        if (!Double.isFinite(lat) || !Double.isFinite(lon)
                || lat < -90 || lat > 90
                || lon < -180 || lon > 180) {
            throw new IllegalArgumentException("Invalid shop coordinates.");
        }

        latitude = lat;
        longitude = lon;
    }

    public List<String> services = new ArrayList<>();
    public List<String[]> mechs = new ArrayList<>();
}
