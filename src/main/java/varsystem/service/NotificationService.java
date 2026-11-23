
package varsystem.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import varsystem.data.DB;
import varsystem.data.DBConnection;
import varsystem.data.Session;
import varsystem.model.Req;
import varsystem.model.User;

public final class NotificationService {

    private NotificationService() {}

    public static class NotificationItem {
        public String id;
        public String role;
        public String recipient;
        public String requestId;
        public String title;
        public String message;
        public LocalDateTime createdAt;
        public boolean read;

        public NotificationItem(
                String id,
                String role,
                String recipient,
                String requestId,
                String title,
                String message,
                LocalDateTime createdAt,
                boolean read) {

            this.id = id;
            this.role = role;
            this.recipient = recipient;
            this.requestId = requestId;
            this.title = title;
            this.message = message;
            this.createdAt = createdAt;
            this.read = read;
        }
    }

    // CREATE TABLE
    private static void ensureTable(Connection c) throws SQLException {

        String sql = """
            CREATE TABLE IF NOT EXISTS public.vaarl_notifications (
                notification_id VARCHAR(64) PRIMARY KEY,
                recipient_role VARCHAR(40) NOT NULL,
                recipient_key VARCHAR(320) NOT NULL,
                request_id VARCHAR(100) DEFAULT '',
                title VARCHAR(200) NOT NULL,
                message TEXT NOT NULL,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                is_read BOOLEAN NOT NULL DEFAULT FALSE
            )
            """;

        try (Statement st = c.createStatement()) {
            st.execute(sql);

            st.execute("""
                CREATE INDEX IF NOT EXISTS
                idx_vaarl_notifications_recipient
                ON public.vaarl_notifications
                (recipient_role, recipient_key, created_at DESC)
                """);
        }
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    // NORMALIZE ROLE
    private static String normalizeRole(String role) {

        String r = clean(role);

        if (r.equalsIgnoreCase("Shop")
                || r.equalsIgnoreCase("Shop Manager")) {
            return "Shop Manager";
        }

        if (r.equalsIgnoreCase("Admin")) {
            return "Admin";
        }

        if (r.equalsIgnoreCase("Mechanic")) {
            return "Mechanic";
        }

        return "User";
    }

    // FIND ACCOUNT NOTIFICATION PREFERENCE
    private static boolean notificationsEnabled(
            String role,
            String recipient) {

        role = normalizeRole(role);
        recipient = clean(recipient);

        if (recipient.isBlank()) {
            return false;
        }

        try {
            for (User u : DB.users) {

                if (u == null) {
                    continue;
                }

                if (!normalizeRole(u.role).equalsIgnoreCase(role)) {
                    continue;
                }

                boolean matched = false;

                switch (role) {

                    case "User":
                        matched = u.email != null
                                && u.email.equalsIgnoreCase(recipient);
                        break;

                    case "Shop Manager":
                        matched = (u.shop != null
                                && u.shop.equalsIgnoreCase(recipient))
                                || (u.name != null
                                && u.name.equalsIgnoreCase(recipient));
                        break;

                    case "Mechanic":
                        matched = u.name != null
                                && u.name.equalsIgnoreCase(recipient);
                        break;

                    case "Admin":
                        matched = true;
                        break;
                }

                if (matched) {
                    return u.notif;
                }
            }

        } catch (Exception ex) {
            System.err.println(
                    "Notification preference check failed: "
                    + ex.getMessage());
        }

        // Default enabled if the recipient account cannot be found.
        return true;
    }

    // CREATE NOTIFICATION
    private static void create(
            String role,
            String recipient,
            String requestId,
            String title,
            String message) {

        role = normalizeRole(role);
        recipient = clean(recipient);

        if (recipient.isBlank()
                || title == null || title.isBlank()
                || message == null || message.isBlank()) {
            return;
        }

        // CHECK NOTIFICATION PREFERENCE
        if (!notificationsEnabled(role, recipient)) {
            return;
        }

        String sql = """
            INSERT INTO public.vaarl_notifications
            (notification_id, recipient_role, recipient_key,
             request_id, title, message, created_at, is_read)
            VALUES (?, ?, ?, ?, ?, ?, ?, FALSE)
            """;

        try (Connection c = DBConnection.getConnection()) {

            ensureTable(c);

            try (PreparedStatement ps = c.prepareStatement(sql)) {

                ps.setString(1, UUID.randomUUID().toString());
                ps.setString(2, role);
                ps.setString(3, recipient.toLowerCase());
                ps.setString(4, clean(requestId));
                ps.setString(5, title.trim());
                ps.setString(6, message.trim());
                ps.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));

                ps.executeUpdate();
            }

        } catch (SQLException ex) {
            System.err.println(
                    "Notification save failed: " + ex.getMessage());
        }
    }

    // USER NOTIFICATION
    public static void notifyUser(
            String email,
            String requestId,
            String title,
            String message) {

        create("User", email, requestId, title, message);
    }

    // SHOP NOTIFICATION
    public static void notifyShop(
            String shopName,
            String requestId,
            String title,
            String message) {

        create("Shop Manager", shopName, requestId, title, message);
    }

    // MECHANIC NOTIFICATION
    public static void notifyMechanic(
            String mechanic,
            String requestId,
            String title,
            String message) {

        create("Mechanic", mechanic, requestId, title, message);
    }

    // ADMIN NOTIFICATION
    public static void notifyAdmins(
            String requestId,
            String title,
            String message) {

        create("Admin", "admin", requestId, title, message);
    }

    // CURRENT ACCOUNT NOTIFICATION
    public static void notifyCurrentAccount(
            String requestId,
            String title,
            String message) {

        if (Session.ME == null) {
            return;
        }

        String role = normalizeRole(Session.ME.role);

        String recipient = switch (role) {
            case "Admin" -> "admin";
            case "Shop Manager" -> clean(Session.ME.shop);
            case "Mechanic" -> clean(Session.ME.name);
            default -> clean(Session.ME.email);
        };

        create(role, recipient, requestId, title, message);
    }

    // SHOP APPLICATION APPROVED / REJECTED
    public static void shopApplicationResult(
            String shopName,
            boolean approved) {

        if (shopName == null || shopName.isBlank()) {
            return;
        }

        String title = approved
                ? "Shop Application Approved"
                : "Shop Application Rejected";

        String message = approved
                ? "Your repair shop application has been approved. You can now access your Shop Manager account."
                : "Your repair shop application was rejected. Please review your application details or contact the administrator.";

        notifyShop(shopName, "", title, message);
    }

    // CURRENT ACCOUNT NOTIFICATION SETTING
    private static boolean currentNotificationsEnabled() {

        return Session.ME != null && Session.ME.notif;
    }

    // LOAD NOTIFICATIONS
    public static List<NotificationItem> getNotifications() {

        List<NotificationItem> result = new ArrayList<>();

        if (Session.ME == null || !currentNotificationsEnabled()) {
            return result;
        }

        // LEGACY NOTIFICATIONS
        try {
            List<String> legacy =
                    new ArrayList<>(DB.notifs);

            int unreadLegacy =
                    DB.getUnreadNotificationCount();

            for (int i = 0; i < legacy.size(); i++) {

                String message = legacy.get(i);

                if (message == null || message.isBlank()) {
                    continue;
                }

                result.add(new NotificationItem(
                        "legacy:" + i,
                        normalizeRole(Session.ME.role),
                        clean(Session.ME.email),
                        "",
                        "System Notification",
                        message,
                        null,
                        i < unreadLegacy
                ));
            }

        } catch (Exception ex) {
            System.err.println(
                    "Legacy notification loading failed: "
                    + ex.getMessage());
        }

        String role = normalizeRole(Session.ME.role);

        String recipient = switch (role) {
            case "Admin" -> "admin";
            case "Shop Manager" -> clean(Session.ME.shop);
            case "Mechanic" -> clean(Session.ME.name);
            default -> clean(Session.ME.email);
        };

        if (recipient.isBlank()) {
            return result;
        }

        String sql = """
            SELECT notification_id, recipient_role, recipient_key,
                   request_id, title, message, created_at, is_read
            FROM public.vaarl_notifications
            WHERE LOWER(recipient_role) = LOWER(?)
              AND LOWER(recipient_key) = LOWER(?)
            ORDER BY created_at DESC
            """;

        try (Connection c = DBConnection.getConnection()) {

            ensureTable(c);

            try (PreparedStatement ps = c.prepareStatement(sql)) {

                ps.setString(1, role);
                ps.setString(2, recipient);

                try (ResultSet rs = ps.executeQuery()) {

                    while (rs.next()) {

                        Timestamp timestamp =
                                rs.getTimestamp("created_at");

                        result.add(new NotificationItem(
                                rs.getString("notification_id"),
                                rs.getString("recipient_role"),
                                rs.getString("recipient_key"),
                                rs.getString("request_id"),
                                rs.getString("title"),
                                rs.getString("message"),
                                timestamp == null
                                        ? LocalDateTime.now()
                                        : timestamp.toLocalDateTime(),
                                rs.getBoolean("is_read")
                        ));
                    }
                }
            }

        } catch (SQLException ex) {
            System.err.println(
                    "Notification loading failed: "
                    + ex.getMessage());
        }

        return result;
    }

    // UNREAD COUNT
    public static int getUnreadCount() {

        if (Session.ME == null || !currentNotificationsEnabled()) {
            return 0;
        }

        String role = normalizeRole(Session.ME.role);

        String recipient = switch (role) {
            case "Admin" -> "admin";
            case "Shop Manager" -> clean(Session.ME.shop);
            case "Mechanic" -> clean(Session.ME.name);
            default -> clean(Session.ME.email);
        };

        String sql = """
            SELECT COUNT(*)
            FROM public.vaarl_notifications
            WHERE LOWER(recipient_role) = LOWER(?)
              AND LOWER(recipient_key) = LOWER(?)
              AND is_read = FALSE
            """;

        try (Connection c = DBConnection.getConnection()) {

            ensureTable(c);

            try (PreparedStatement ps = c.prepareStatement(sql)) {

                ps.setString(1, role);
                ps.setString(2, recipient);

                try (ResultSet rs = ps.executeQuery()) {

                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }

        } catch (SQLException ex) {
            System.err.println(
                    "Unread count failed: "
                    + ex.getMessage());
        }

        return 0;
    }

    // MARK ONE AS READ
    public static void markAsRead(String id) {

        if (id == null || id.isBlank()) {
            return;
        }

        if (id.startsWith("legacy:")) {
            DB.markNotificationsRead();
            return;
        }

        String sql = """
            UPDATE public.vaarl_notifications
            SET is_read = TRUE
            WHERE notification_id = ?
            """;

        try (Connection c = DBConnection.getConnection()) {

            ensureTable(c);

            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, id);
                ps.executeUpdate();
            }

        } catch (SQLException ex) {
            System.err.println(
                    "Mark notification read failed: "
                    + ex.getMessage());
        }
    }

    // MARK ALL AS READ
    public static void markAllAsRead() {

        if (Session.ME == null) {
            return;
        }

        DB.markNotificationsRead();

        String role = normalizeRole(Session.ME.role);

        String recipient = switch (role) {
            case "Admin" -> "admin";
            case "Shop Manager" -> clean(Session.ME.shop);
            case "Mechanic" -> clean(Session.ME.name);
            default -> clean(Session.ME.email);
        };

        String sql = """
            UPDATE public.vaarl_notifications
            SET is_read = TRUE
            WHERE LOWER(recipient_role) = LOWER(?)
              AND LOWER(recipient_key) = LOWER(?)
            """;

        try (Connection c = DBConnection.getConnection()) {

            ensureTable(c);

            try (PreparedStatement ps = c.prepareStatement(sql)) {

                ps.setString(1, role);
                ps.setString(2, recipient);
                ps.executeUpdate();
            }

        } catch (SQLException ex) {
            System.err.println(
                    "Mark all notifications failed: "
                    + ex.getMessage());
        }
    }

    // CLEAR ALL
    public static void clearAll() {

        if (Session.ME == null) {
            return;
        }

        String role = normalizeRole(Session.ME.role);

        String recipient = switch (role) {
            case "Admin" -> "admin";
            case "Shop Manager" -> clean(Session.ME.shop);
            case "Mechanic" -> clean(Session.ME.name);
            default -> clean(Session.ME.email);
        };

        String sql = """
            DELETE FROM public.vaarl_notifications
            WHERE LOWER(recipient_role) = LOWER(?)
              AND LOWER(recipient_key) = LOWER(?)
            """;

        try (Connection c = DBConnection.getConnection()) {

            ensureTable(c);

            try (PreparedStatement ps = c.prepareStatement(sql)) {

                ps.setString(1, role);
                ps.setString(2, recipient);
                ps.executeUpdate();
            }

        } catch (SQLException ex) {
            System.err.println(
                    "Clear notifications failed: "
                    + ex.getMessage());
        }
    }

    // REQUEST SUBMITTED
    public static void requestSubmitted(Req r) {

        if (r == null) {
            return;
        }

        notifyShop(
                r.shop,
                r.id,
                "New Service Request",
                r.uname + " submitted a vehicle assistance request."
        );

        notifyAdmins(
                r.id,
                "New Service Request",
                r.uname + " submitted a request for " + r.shop + "."
        );
    }

    // REQUEST ACCEPTED
    public static void requestAccepted(Req r) {

        if (r == null) {
            return;
        }

        notifyUser(
                r.uemail,
                r.id,
                "Request Accepted",
                r.shop + " accepted your vehicle assistance request."
        );

        notifyAdmins(
                r.id,
                "Request Accepted",
                r.shop + " accepted request " + r.id + "."
        );
    }

    // REQUEST REJECTED
    public static void requestRejected(Req r) {

        if (r == null) {
            return;
        }

        notifyUser(
                r.uemail,
                r.id,
                "Request Rejected",
                r.shop + " rejected your vehicle assistance request."
        );

        notifyAdmins(
                r.id,
                "Request Rejected",
                r.shop + " rejected request " + r.id + "."
        );
    }

    // MECHANIC ASSIGNED
    public static void mechanicAssigned(Req r) {

        if (r == null) {
            return;
        }

        notifyUser(
                r.uemail,
                r.id,
                "Mechanic Assigned",
                "Mechanic " + r.mechanic
                        + " has been assigned to your request."
        );

        notifyAdmins(
                r.id,
                "Mechanic Assigned",
                r.mechanic + " was assigned to request " + r.id + "."
        );

        if (r.mechanic != null && !r.mechanic.isBlank()) {
            notifyMechanic(
                    r.mechanic,
                    r.id,
                    "New Assigned Service",
                    "You have been assigned to request " + r.id
                            + " for " + r.uname + "."
            );
        }
    }

    // REPAIR STARTED
    public static void repairStarted(Req r) {

        if (r == null) {
            return;
        }

        notifyUser(
                r.uemail,
                r.id,
                "Repair in Progress",
                "The mechanic has started working on your vehicle."
        );

        notifyAdmins(
                r.id,
                "Repair in Progress",
                "Repair started for request " + r.id + "."
        );
    }

    // SERVICE COMPLETED
    public static void serviceCompleted(Req r) {

        if (r == null) {
            return;
        }

        notifyUser(
                r.uemail,
                r.id,
                "Service Completed",
                "Your vehicle repair service has been completed."
        );

        notifyAdmins(
                r.id,
                "Service Completed",
                r.shop + " completed request " + r.id + "."
        );
    }

    // REQUEST CANCELLED
    public static void requestCancelled(Req r) {

        if (r == null) {
            return;
        }

        notifyShop(
                r.shop,
                r.id,
                "Request Cancelled",
                r.uname + " cancelled request " + r.id + "."
        );

        notifyAdmins(
                r.id,
                "Request Cancelled",
                r.uname + " cancelled request " + r.id + "."
        );
    }

    // NEW FEEDBACK
    public static void newFeedback(
            String shop,
            String requestId,
            String customerName) {

        notifyShop(
                shop,
                requestId,
                "New Feedback",
                customerName + " submitted feedback for your shop."
        );

        notifyAdmins(
                requestId,
                "New Feedback",
                customerName + " submitted feedback for " + shop + "."
        );
    }

    // SUPPORT REQUEST
    public static void newSupportRequest(
            String email,
            String role,
            String name) {

        notifyAdmins(
                "",
                "New Support Request",
                name + " (" + role + ") submitted a support message."
        );
    }

    // REGISTRATION
    public static void newRegistration(
            String role,
            String name,
            String email) {

        notifyAdmins(
                "",
                "New " + role + " Registration",
                name + " registered with email " + email + "."
        );
    }

    // ADMIN REPLY
    public static void adminReply(
            String email,
            String role,
            String message) {

        create(
                role,
                email,
                "",
                "New Admin Reply",
                message
        );
    }
}
