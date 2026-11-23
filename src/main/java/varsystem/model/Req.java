
package varsystem.model;

import java.io.Serializable;
import java.time.LocalDateTime;

import varsystem.data.DB;

/**
 * A vehicle assistance (service) request.
 * Supports saving and restoring requests after restarting the application.
 * Includes customer GPS location and shop-specific location sharing.
 */
public class Req implements Serializable {

    private static final long serialVersionUID = 1L;

    public String id, uemail, uname, uphone, vehicle, problem,
            location, details = "", shop,
            status = "Pending", mechanic = "",
            payment = "Cash / GCash";

    public int fee, travel;

    public LocalDateTime submitted = LocalDateTime.now(),
            accepted, completed;

    // Customer GPS location for this specific request
    public double userLatitude = Double.NaN;
    public double userLongitude = Double.NaN;

    // Customer permission to share location
    public boolean shareLocation = false;

    /**
     * Checks whether the request contains valid GPS coordinates.
     */
    public boolean hasUserLocation() {
        return Double.isFinite(userLatitude)
                && Double.isFinite(userLongitude)
                && userLatitude >= -90
                && userLatitude <= 90
                && userLongitude >= -180
                && userLongitude <= 180;
    }

    /**
     * Checks whether the request is still active.
     */
    public boolean isActive() {
        return status != null
                && ("Pending".equalsIgnoreCase(status)
                || "Assigned".equalsIgnoreCase(status)
                || "Accepted".equalsIgnoreCase(status)
                || "On Going".equalsIgnoreCase(status));
    }

    /**
     * Permanently removes the saved location
     * and disables sharing for this request.
     */
    public void revokeLocationSharing() {
        shareLocation = false;
        userLatitude = Double.NaN;
        userLongitude = Double.NaN;
    }

    /**
     * Automatically disables sharing for completed,
     * cancelled, declined, or inactive requests.
     */
    public void enforceLocationPrivacy() {
        if (status == null
                || "Cancelled".equalsIgnoreCase(status)
                || "Completed".equalsIgnoreCase(status)
                || "Declined".equalsIgnoreCase(status)
                || !isActive()) {
            revokeLocationSharing();
        }
    }

    /**
     * Checks the customer's account location-sharing setting.
     */
    private boolean customerLocationSharingEnabled() {
        if (uemail == null || uemail.isBlank()) {
            return false;
        }

        for (User account : DB.users) {
            if (account != null
                    && account.email != null
                    && account.email.equalsIgnoreCase(uemail)
                    && "User".equalsIgnoreCase(account.role)) {
                return account.loc;
            }
        }

        // Fail closed if permission cannot be verified.
        return false;
    }

    /**
     * Checks whether customer location can be shown
     * to the specified shop.
     */
    public boolean canShareLocationWith(String shopName) {
        enforceLocationPrivacy();

        return shareLocation
                && hasUserLocation()
                && isActive()
                && customerLocationSharingEnabled()
                && shop != null
                && shopName != null
                && !shop.isBlank()
                && !shopName.isBlank()
                && shop.trim().equalsIgnoreCase(shopName.trim());
    }
}
