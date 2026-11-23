
package varsystem.data;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import javax.imageio.ImageIO;

import varsystem.model.Feedback;
import varsystem.model.RecoveryRequest;
import varsystem.model.Req;
import varsystem.model.Shop;
import varsystem.model.Svc;
import varsystem.model.User;
import varsystem.util.Formats;

public final class DB {

    private DB() { }

    // =================================================
    // SYSTEM DATA
    // =================================================

    public static List<User> users = new ArrayList<>();
    public static List<Shop> shops = new ArrayList<>();
    public static List<Req> reqs = new ArrayList<>();
    public static List<Svc> svcs = new ArrayList<>();
    public static List<Feedback> feedbacks = new ArrayList<>();

    public static List<String[]> appts = new ArrayList<>();
    public static List<String[]> maint = new ArrayList<>();

    public static List<String> logs = new ArrayList<>();
    public static LinkedList<String> notifs = new LinkedList<>();

    public static Map<String, List<String>> chats = new HashMap<>();
    public static Map<String, String> settings = new HashMap<>();

    // UNREAD TRACKING
    public static Map<String, Integer> notificationReadCounts =
            new HashMap<>();

    public static Map<String, Integer> chatReadPositions =
            new HashMap<>();

    private static final Object CHAT_LOCK = new Object();

    public static int seq = 5;

    // =================================================
    // FILE PATHS
    // =================================================

    private static final Path DATA_FILE = Path.of(
            System.getProperty("user.home"),
            ".vaarl",
            "system-data.ser"
    );

    private static final Path REQUEST_FILE = Path.of(
            System.getProperty("user.home"),
            ".vaarl",
            "requests.ser"
    );

    private static final Path CHAT_FILE = Path.of(
            System.getProperty("user.home"),
            ".vaarl",
            "chats.ser"
    );

    private static final Path LOCATION_FILE = Path.of(
            System.getProperty("user.home"),
            ".vaarl",
            "locations.properties"
    );

    private static final Path SHOP_PHOTO_DIR = Path.of(
            System.getProperty("user.home"),
            ".vaarl",
            "shop-photos"
    );

    private static final Path SHOP_PHOTO_FILE = Path.of(
            System.getProperty("user.home"),
            ".vaarl",
            "shop-photos.properties"
    );

    // =================================================
    // SUPPORT CHAT SYSTEM
    // =================================================

    public static class SupportMessage implements Serializable {

        private static final long serialVersionUID = 1L;

        public String messageId;
        public String conversationId;
        public String senderName;
        public String senderEmail;
        public String senderRole;
        public String message;

        public LocalDateTime sentAt;

        public boolean fromAdmin;
        public boolean readByAdmin;
        public boolean readBySender;

        public SupportMessage(
                String conversationId,
                String senderName,
                String senderEmail,
                String senderRole,
                String message,
                boolean fromAdmin) {

            this.messageId = UUID.randomUUID().toString();
            this.conversationId = conversationId;
            this.senderName = senderName;
            this.senderEmail = senderEmail;
            this.senderRole = senderRole;
            this.message = message;
            this.sentAt = LocalDateTime.now();
            this.fromAdmin = fromAdmin;
        }
    }

    public static List<SupportMessage> supportMessages =
            new ArrayList<>();

    private static String supportConversationId(
            String email, String role) {

        return (role == null ? "" : role.trim().toLowerCase())
                + "|"
                + (email == null ? "" : email.trim().toLowerCase());
    }

    private static synchronized void ensureSupportMessageIds() {

        boolean changed = false;

        if (supportMessages == null) {
            supportMessages = new ArrayList<>();
            return;
        }

        for (SupportMessage message : supportMessages) {

            if (message == null) {
                continue;
            }

            if (message.messageId == null
                    || message.messageId.isBlank()) {

                message.messageId = UUID.randomUUID().toString();
                changed = true;
            }
        }

        if (changed) {
            saveData();
        }
    }

    public static synchronized boolean sendSupportMessage(
            String name,
            String email,
            String role,
            String message) {

        if (name == null || email == null || role == null
                || message == null || message.isBlank()) {
            return false;
        }

        String conversationId =
                supportConversationId(email, role);

        SupportMessage support = new SupportMessage(
                conversationId,
                name,
                email,
                role,
                message.trim(),
                false
        );

        supportMessages.add(support);

        addNotification(
                "New support message from " + name
                        + " (" + role + ")."
        );

        varsystem.service.NotificationService
                .newSupportRequest(email, role, name);

        log("Support message received from " + email);

        saveData();

        return true;
    }

    public static synchronized boolean replySupportMessage(
            String email,
            String role,
            String message) {

        if (email == null || email.isBlank()
                || role == null || role.isBlank()
                || message == null || message.isBlank()) {
            return false;
        }

        String conversationId =
                supportConversationId(email, role);

        boolean exists = supportMessages.stream()
                .anyMatch(m -> m != null
                        && conversationId.equals(m.conversationId));

        if (!exists) {
            return false;
        }

        SupportMessage reply = new SupportMessage(
                conversationId,
                "Admin",
                "admin",
                "Admin",
                message.trim(),
                true
        );

        supportMessages.add(reply);

        addNotification(
                "Admin replied to your support conversation."
        );

        varsystem.service.NotificationService.adminReply(
                email, role, "Admin replied to your support conversation.");

        log("Admin replied to support conversation: " + email);

        saveData();

        return true;
    }

    public static synchronized List<SupportMessage> getSupportMessages(
            String email,
            String role) {

        ensureSupportMessageIds();

        String conversationId =
                supportConversationId(email, role);

        List<SupportMessage> result = new ArrayList<>();

        for (SupportMessage message : supportMessages) {

            if (message != null
                    && conversationId.equals(message.conversationId)) {

                result.add(message);
            }
        }

        return result;
    }

    public static synchronized List<SupportMessage> getSupportConversations() {

        ensureSupportMessageIds();

        Map<String, SupportMessage> latest = new LinkedHashMap<>();

        for (SupportMessage message : supportMessages) {

            if (message == null
                    || message.conversationId == null
                    || message.conversationId.isBlank()) {
                continue;
            }

            SupportMessage previous =
                    latest.get(message.conversationId);

            if (previous == null
                    || (message.sentAt != null
                    && (previous.sentAt == null
                    || message.sentAt.isAfter(previous.sentAt)))) {

                latest.put(message.conversationId, message);
            }
        }

        List<SupportMessage> result =
                new ArrayList<>(latest.values());

        result.sort((a, b) -> {

            if (a.sentAt == null && b.sentAt == null) return 0;
            if (a.sentAt == null) return 1;
            if (b.sentAt == null) return -1;

            return b.sentAt.compareTo(a.sentAt);
        });

        return result;
    }

    public static synchronized boolean editSupportMessage(
            String messageId,
            String newMessage) {

        if (messageId == null || messageId.isBlank()
                || newMessage == null || newMessage.isBlank()) {
            return false;
        }

        ensureSupportMessageIds();

        for (SupportMessage message : supportMessages) {

            if (message != null
                    && messageId.equals(message.messageId)
                    && message.fromAdmin) {

                message.message = newMessage.trim();

                log("Admin edited a support message.");

                saveData();

                return true;
            }
        }

        return false;
    }

    public static synchronized boolean deleteSupportMessage(
            String messageId) {

        if (messageId == null || messageId.isBlank()) {
            return false;
        }

        ensureSupportMessageIds();

        Iterator<SupportMessage> iterator =
                supportMessages.iterator();

        while (iterator.hasNext()) {

            SupportMessage message = iterator.next();

            if (message != null
                    && messageId.equals(message.messageId)
                    && message.fromAdmin) {

                iterator.remove();

                log("Admin deleted a support message.");

                saveData();

                return true;
            }
        }

        return false;
    }

    public static synchronized boolean deleteSupportConversation(
            String email,
            String role) {

        if (email == null || email.isBlank()
                || role == null || role.isBlank()) {
            return false;
        }

        String conversationId =
                supportConversationId(email, role);

        Iterator<SupportMessage> iterator =
                supportMessages.iterator();

        boolean deleted = false;

        while (iterator.hasNext()) {

            SupportMessage message = iterator.next();

            if (message != null
                    && conversationId.equals(message.conversationId)) {

                iterator.remove();
                deleted = true;
            }
        }

        if (deleted) {
            log("Admin deleted support conversation: " + email);
            saveData();
        }

        return deleted;
    }

    public static synchronized void markSupportReadByAdmin(
            String email,
            String role) {

        String conversationId =
                supportConversationId(email, role);

        for (SupportMessage message : supportMessages) {

            if (message != null
                    && conversationId.equals(message.conversationId)
                    && !message.fromAdmin) {

                message.readByAdmin = true;
            }
        }

        saveData();
    }

    public static synchronized void markSupportReadBySender(
            String email,
            String role) {

        String conversationId =
                supportConversationId(email, role);

        for (SupportMessage message : supportMessages) {

            if (message != null
                    && conversationId.equals(message.conversationId)
                    && message.fromAdmin) {

                message.readBySender = true;
            }
        }

        saveData();
    }

    // =================================================
    // PERMANENT DATABASE STORAGE
    // =================================================

    private static class SystemData implements Serializable {

        private static final long serialVersionUID = 1L;

        List<User> users;
        List<Shop> shops;
        List<Req> reqs;
        List<Svc> svcs;
        List<Feedback> feedbacks;

        List<String[]> appts;
        List<String[]> maint;

        List<String> logs;
        LinkedList<String> notifs;

        Map<String, List<String>> chats;
        Map<String, String> settings;

        Map<String, Integer> notificationReadCounts;
        Map<String, Integer> chatReadPositions;

        List<SupportMessage> supportMessages;

        List<RecoveryRequest> recoveryRequests;

        int seq;

        SystemData() {

            users = DB.users;
            shops = DB.shops;
            reqs = DB.reqs;
            svcs = DB.svcs;
            feedbacks = DB.feedbacks;

            appts = DB.appts;
            maint = DB.maint;

            logs = DB.logs;
            notifs = DB.notifs;

            chats = DB.chats;
            settings = DB.settings;

            notificationReadCounts = DB.notificationReadCounts;
            chatReadPositions = DB.chatReadPositions;

            supportMessages = DB.supportMessages;

            recoveryRequests = DB.recoveryRequests;

            seq = DB.seq;
        }
    }

    // =================================================
    // INDEPENDENT REQUEST BACKUP
    // =================================================

    public static synchronized boolean saveRequests() {

        Path tempFile = REQUEST_FILE.resolveSibling("requests.tmp");

        try {

            Files.createDirectories(REQUEST_FILE.getParent());

            List<Req> requestCopy = new ArrayList<>();

            if (reqs != null) {

                for (Req request : reqs) {

                    if (request != null) {
                        requestCopy.add(request);
                    }
                }
            }

            try (OutputStream output =
                         Files.newOutputStream(tempFile);
                 ObjectOutputStream objectOutput =
                         new ObjectOutputStream(output)) {

                objectOutput.writeObject(requestCopy);
                objectOutput.flush();
            }

            try {

                Files.move(
                        tempFile,
                        REQUEST_FILE,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE
                );

            } catch (IOException ex) {

                Files.move(
                        tempFile,
                        REQUEST_FILE,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }

            System.out.println(
                    "Requests saved successfully: " + requestCopy.size()
            );

            return true;

        } catch (IOException | RuntimeException ex) {

            System.err.println(
                    "Unable to save requests: " + ex.getMessage()
            );

            return false;

        } finally {

            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException ex) {

                System.err.println(
                        "Unable to delete temporary request file: "
                                + ex.getMessage()
                );
            }
        }
    }

    public static synchronized boolean loadRequests() {

        if (!Files.isRegularFile(REQUEST_FILE)) {
            return false;
        }

        try (InputStream input =
                     Files.newInputStream(REQUEST_FILE);
             ObjectInputStream objectInput =
                     new ObjectInputStream(input)) {

            Object object = objectInput.readObject();

            if (!(object instanceof List<?>)) {
                return false;
            }

            List<?> savedRequests = (List<?>) object;

            List<Req> restoredRequests = new ArrayList<>();

            for (Object savedRequest : savedRequests) {

                if (savedRequest instanceof Req request) {
                    restoredRequests.add(request);
                }
            }

            reqs = restoredRequests;

            System.out.println(
                    "Requests loaded successfully: "
                            + restoredRequests.size()
            );

            return true;

        } catch (IOException | ClassNotFoundException
                 | RuntimeException ex) {

            System.err.println(
                    "Unable to load requests: " + ex.getMessage()
            );

            return false;
        }
    }

    // =================================================
    // INDEPENDENT CHAT BACKUP
    // =================================================

    public static synchronized boolean saveChats() {

        Path tempFile = CHAT_FILE.resolveSibling("chats.tmp");

        try {

            Files.createDirectories(CHAT_FILE.getParent());

            Map<String, List<String>> chatCopy = new HashMap<>();

            synchronized (CHAT_LOCK) {

                if (chats != null) {

                    for (Map.Entry<String, List<String>> entry
                            : chats.entrySet()) {

                        if (entry.getKey() == null
                                || entry.getValue() == null) {
                            continue;
                        }

                        chatCopy.put(
                                entry.getKey(),
                                new ArrayList<>(entry.getValue())
                        );
                    }
                }
            }

            try (OutputStream output =
                         Files.newOutputStream(tempFile);
                 ObjectOutputStream objectOutput =
                         new ObjectOutputStream(output)) {

                objectOutput.writeObject(chatCopy);
                objectOutput.flush();
            }

            try {

                Files.move(
                        tempFile,
                        CHAT_FILE,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE
                );

            } catch (IOException ex) {

                Files.move(
                        tempFile,
                        CHAT_FILE,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }

            return true;

        } catch (IOException | RuntimeException ex) {

            System.err.println(
                    "Unable to save chat messages: " + ex.getMessage()
            );

            return false;

        } finally {

            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException ex) {

                System.err.println(
                        "Unable to delete temporary chat file: "
                                + ex.getMessage()
                );
            }
        }
    }

    public static synchronized boolean loadChats() {

        if (!Files.isRegularFile(CHAT_FILE)) {
            return false;
        }

        try (InputStream input =
                     Files.newInputStream(CHAT_FILE);
             ObjectInputStream objectInput =
                     new ObjectInputStream(input)) {

            Object object = objectInput.readObject();

            if (!(object instanceof Map<?, ?>)) {
                return false;
            }

            Map<?, ?> savedChats = (Map<?, ?>) object;

            Map<String, List<String>> restoredChats =
                    new HashMap<>();

            for (Map.Entry<?, ?> entry : savedChats.entrySet()) {

                if (!(entry.getKey() instanceof String key)) {
                    continue;
                }

                if (!(entry.getValue() instanceof List<?> savedMessages)) {
                    continue;
                }

                List<String> messages = new ArrayList<>();

                for (Object savedMessage : savedMessages) {

                    if (savedMessage instanceof String message) {
                        messages.add(message);
                    }
                }

                restoredChats.put(key, messages);
            }

            chats = restoredChats;

            System.out.println(
                    "Chat messages loaded successfully."
            );

            return true;

        } catch (IOException | ClassNotFoundException
                 | RuntimeException ex) {

            System.err.println(
                    "Unable to load chat messages: " + ex.getMessage()
            );

            return false;
        }
    }

    // =================================================
    // ADMIN-ASSISTED ACCOUNT RECOVERY (MERGED - SINGLE COPY)
    // =================================================

    private static final long MAX_PROOF_BYTES = 5L * 1024 * 1024;
    private static final long APPROVAL_VALID_HOURS = 24;

    private static final Path RECOVERY_PROOF_DIR = Path.of(
            System.getProperty("user.home"),
            ".vaarl",
            "recovery-proofs"
    );

    public static List<RecoveryRequest> recoveryRequests =
            new ArrayList<>();

    private static boolean sameAccount(
            RecoveryRequest r, String email, String role) {

        return r != null
                && r.accountEmail != null
                && email != null
                && r.accountEmail.trim().equalsIgnoreCase(email.trim())
                && r.accountRole != null
                && role != null
                && r.accountRole.trim().equalsIgnoreCase(role.trim());
    }

    /** Newest recovery request for this account, or null. */
    public static synchronized RecoveryRequest latestRecovery(
            String email, String role) {

        RecoveryRequest latest = null;

        if (recoveryRequests == null) {
            recoveryRequests = new ArrayList<>();
        }

        for (RecoveryRequest r : recoveryRequests) {

            if (r == null || !sameAccount(r, email, role)) {
                continue;
            }

            if (latest == null) {
                latest = r;
                continue;
            }

            if (r.submittedAt != null
                    && (latest.submittedAt == null
                    || r.submittedAt.isAfter(latest.submittedAt))) {

                latest = r;
            }
        }

        return latest;
    }

    public static synchronized RecoveryRequest getRecovery(String id) {

        if (id == null || recoveryRequests == null) {
            return null;
        }

        for (RecoveryRequest r : recoveryRequests) {

            if (r != null && id.equals(r.id)) {
                return r;
            }
        }

        return null;
    }

    /** True when approval is valid, unused, and not expired. */
    public static synchronized boolean isRecoveryUsable(
            RecoveryRequest r) {

        return r != null
                && RecoveryRequest.APPROVED.equals(r.status)
                && !r.resetCompleted
                && r.decidedAt != null
                && r.decidedAt.plusHours(APPROVAL_VALID_HOURS)
                        .isAfter(LocalDateTime.now());
    }

    /**
     * Submit a recovery request.
     * Pending requests and active approvals cannot be duplicated.
     * Rejected, used, and expired requests may be resubmitted.
     */
    public static synchronized RecoveryRequest submitRecoveryRequest(
            User account, Path photo) {

        if (account == null
                || account.email == null
                || account.email.isBlank()
                || account.role == null
                || account.role.isBlank()
                || photo == null
                || !Files.isRegularFile(photo)) {

            return null;
        }

        if (recoveryRequests == null) {
            recoveryRequests = new ArrayList<>();
        }

        RecoveryRequest previous =
                latestRecovery(account.email, account.role);

        // Block duplicate pending requests.
        if (previous != null
                && RecoveryRequest.PENDING.equals(previous.status)) {

            return null;
        }

        // Block new requests while an approval is still usable.
        if (previous != null && isRecoveryUsable(previous)) {
            return null;
        }

        Path stored = null;
        RecoveryRequest request = null;

        try {

            if (Files.size(photo) <= 0
                    || Files.size(photo) > MAX_PROOF_BYTES
                    || ImageIO.read(photo.toFile()) == null) {

                return null;
            }

            Files.createDirectories(RECOVERY_PROOF_DIR);

            request = new RecoveryRequest();

            request.id = UUID.randomUUID().toString();
            request.accountEmail = account.email.trim();
            request.accountRole = account.role;
            request.accountName = account.name;
            request.username = account.username;
            request.phone = account.phone == null
                    ? ""
                    : account.phone;

            stored = RECOVERY_PROOF_DIR.resolve(
                    request.id + imageExtension(photo)
            );

            Files.copy(
                    photo,
                    stored,
                    StandardCopyOption.REPLACE_EXISTING
            );

            request.proofPhotoPath =
                    stored.toAbsolutePath().toString();

            recoveryRequests.add(request);

            // Admin receives a text-only notification.
            // The proof photo is stored privately.
            String notice =
                    "ACCOUNT RECOVERY REQUEST\n\n"
                    + "Account: " + account.name
                    + "\nUsername: " + account.username
                    + "\nProof photo attached (visible to Admin only)."
                    + "\nRecovery ID: " + request.id;

            if (!sendSupportMessage(
                    account.name,
                    account.email,
                    account.role,
                    notice)) {

                recoveryRequests.remove(request);

                Files.deleteIfExists(stored);

                return null;
            }

            log("Recovery request submitted for: "
                    + account.username);

            // sendSupportMessage() already saves system data.
            return request;

        } catch (IOException | RuntimeException ex) {

            System.err.println(
                    "Unable to submit recovery request: "
                            + ex.getMessage()
            );

            if (request != null) {
                recoveryRequests.remove(request);
            }

            try {

                if (stored != null) {
                    Files.deleteIfExists(stored);
                }

            } catch (IOException ignored) {
            }

            saveData();

            return null;
        }
    }

    /**
     * Admin decision.
     * Pending requests only.
     * A rejection requires a reason.
     */
    public static synchronized boolean decideRecovery(
            String id, boolean approve, String note) {

        RecoveryRequest r = getRecovery(id);

        if (r == null
                || !RecoveryRequest.PENDING.equals(r.status)) {

            return false;
        }

        String cleanNote = note == null ? "" : note.trim();

        if (!approve && cleanNote.isBlank()) {
            return false;
        }

        String oldStatus = r.status;
        String oldNote = r.adminNote;
        LocalDateTime oldDecidedAt = r.decidedAt;

        r.status = approve
                ? RecoveryRequest.APPROVED
                : RecoveryRequest.REJECTED;

        r.adminNote = cleanNote;
        r.decidedAt = LocalDateTime.now();

        if (!saveData()) {

            // Rollback if saving failed.
            r.status = oldStatus;
            r.adminNote = oldNote;
            r.decidedAt = oldDecidedAt;

            return false;
        }

        String reply = approve
                ? "Your account recovery request was APPROVED. "
                    + "Open Forgot Password and verify your account "
                    + "again to set a new password. This approval is "
                    + "valid for " + APPROVAL_VALID_HOURS
                    + " hours and can only be used once."
                : "Your account recovery request was REJECTED."
                    + "\nReason: " + cleanNote
                    + "\nYou may submit a new request with a clearer photo.";

        replySupportMessage(
                r.accountEmail,
                r.accountRole,
                reply
        );

        log("Admin " + (approve ? "approved" : "rejected")
                + " recovery request for: " + r.username);

        saveData();

        return true;
    }

    /**
     * Marks an approval as used exactly once.
     * The recovery status becomes USED and the proof photo is deleted.
     */
    public static synchronized boolean completeRecovery(String id) {

        RecoveryRequest r = getRecovery(id);

        if (!isRecoveryUsable(r)) {
            return false;
        }

        String oldStatus = r.status;
        boolean oldCompleted = r.resetCompleted;
        String oldProofPath = r.proofPhotoPath;

        // Consume approval.
        r.status = RecoveryRequest.USED;
        r.resetCompleted = true;

        if (!saveData()) {

            // Rollback if primary save fails.
            r.status = oldStatus;
            r.resetCompleted = oldCompleted;
            r.proofPhotoPath = oldProofPath;

            return false;
        }

        // The approval is already consumed and persisted.
        // Proof deletion must not undo the successful consumption.
        try {

            if (oldProofPath != null
                    && !oldProofPath.isBlank()) {

                Files.deleteIfExists(Path.of(oldProofPath));
            }

        } catch (IOException | RuntimeException ex) {

            System.err.println(
                    "Unable to delete recovery proof: "
                            + ex.getMessage()
            );
        }

        r.proofPhotoPath = "";

        // Secondary save. Failure here does not reverse consumption.
        saveData();

        return true;
    }

    /**
     * Consumes the approved recovery request and updates the user's
     * password and reset counter in one primary save operation.
     */
    public static synchronized boolean completeRecoveryWithPassword(
            String id,
            User account,
            String hashedPassword) {

        RecoveryRequest r = getRecovery(id);

        if (!isRecoveryUsable(r)
                || account == null
                || hashedPassword == null
                || hashedPassword.isBlank()
                || account.email == null
                || account.role == null
                || !sameAccount(
                        r,
                        account.email,
                        account.role
                )) {

            return false;
        }

        // Confirm that the account belongs to the database.
        boolean accountExists = false;

        if (users != null) {

            for (User savedUser : users) {

                if (savedUser == account) {
                    accountExists = true;
                    break;
                }

                if (savedUser != null
                        && savedUser.email != null
                        && savedUser.role != null
                        && savedUser.email.equalsIgnoreCase(
                                account.email
                        )
                        && savedUser.role.equalsIgnoreCase(
                                account.role
                        )) {

                    account = savedUser;
                    accountExists = true;
                    break;
                }
            }
        }

        if (!accountExists) {
            return false;
        }

        String oldPassword = account.password;
        int oldResetCount = account.passwordResetCount;

        String oldStatus = r.status;
        boolean oldCompleted = r.resetCompleted;
        String oldProofPath = r.proofPhotoPath;

        // Apply the changes.
        account.password = hashedPassword;
        account.passwordResetCount++;

        r.status = RecoveryRequest.USED;
        r.resetCompleted = true;

        // Password and approval consumption are persisted together.
        if (!saveData()) {

            // Rollback everything if the primary save fails.
            account.password = oldPassword;
            account.passwordResetCount = oldResetCount;

            r.status = oldStatus;
            r.resetCompleted = oldCompleted;
            r.proofPhotoPath = oldProofPath;

            return false;
        }

        // Password reset and approval consumption are now saved.
        try {

            if (oldProofPath != null
                    && !oldProofPath.isBlank()) {

                Files.deleteIfExists(Path.of(oldProofPath));
            }

        } catch (IOException | RuntimeException ex) {

            System.err.println(
                    "Unable to delete recovery proof: "
                            + ex.getMessage()
            );
        }

        r.proofPhotoPath = "";

        // Secondary save for the cleared proof path.
        saveData();

        log("Password recovery completed for: "
                + account.username);

        saveData();

        return true;
    }

    /** Pulls the recovery ID out of a support message, or null. */
    public static String recoveryIdFromMessage(String text) {

        if (text == null) {
            return null;
        }

        int at = text.indexOf("Recovery ID: ");

        if (at < 0) {
            return null;
        }

        String id = text.substring(at + 13).trim();

        int end = id.indexOf('\n');

        return end < 0
                ? id
                : id.substring(0, end).trim();
    }


    // =================================================
    // SAVE DATA
    // =================================================

    public static synchronized boolean saveData() {

        // Keep independent request and chat backups in sync.
        saveRequests();
        saveChats();

        Path tempFile = DATA_FILE.resolveSibling(
                "system-data.tmp"
        );

        try {

            Files.createDirectories(DATA_FILE.getParent());

            SystemData data = new SystemData();

            try (OutputStream output =
                         Files.newOutputStream(tempFile);
                 ObjectOutputStream objectOutput =
                         new ObjectOutputStream(output)) {

                objectOutput.writeObject(data);
                objectOutput.flush();
            }

            try {

                Files.move(
                        tempFile,
                        DATA_FILE,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE
                );

            } catch (IOException ex) {

                Files.move(
                        tempFile,
                        DATA_FILE,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }

            return true;

        } catch (IOException | RuntimeException ex) {

            System.err.println(
                    "Unable to save system data: " + ex.getMessage()
            );

            return false;

        } finally {

            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException ex) {
                System.err.println(
                        "Unable to delete temporary file: "
                                + ex.getMessage()
                );
            }
        }
    }

    // =================================================
    // LOAD DATA
    // =================================================

    public static synchronized boolean loadData() {

        // Restore independent backups first.
        loadRequests();
        loadChats();

        if (!Files.isRegularFile(DATA_FILE)) {
            loadLocations();
            return false;
        }

        try (InputStream input =
                     Files.newInputStream(DATA_FILE);
             ObjectInputStream objectInput =
                     new ObjectInputStream(input)) {

            Object object = objectInput.readObject();

            if (!(object instanceof SystemData)) {
                return false;
            }

            SystemData data = (SystemData) object;

            users = data.users != null
                    ? data.users : new ArrayList<>();

            shops = data.shops != null
                    ? data.shops : new ArrayList<>();

            reqs = data.reqs != null
                    ? data.reqs : new ArrayList<>();

            svcs = data.svcs != null
                    ? data.svcs : new ArrayList<>();

            feedbacks = data.feedbacks != null
                    ? data.feedbacks : new ArrayList<>();

            appts = data.appts != null
                    ? data.appts : new ArrayList<>();

            maint = data.maint != null
                    ? data.maint : new ArrayList<>();

            logs = data.logs != null
                    ? data.logs : new ArrayList<>();

            notifs = data.notifs != null
                    ? data.notifs : new LinkedList<>();

            chats = data.chats != null
                    ? data.chats : new HashMap<>();

            settings = data.settings != null
                    ? data.settings : new HashMap<>();

            notificationReadCounts =
                    data.notificationReadCounts != null
                            ? data.notificationReadCounts
                            : new HashMap<>();

            chatReadPositions =
                    data.chatReadPositions != null
                            ? data.chatReadPositions
                            : new HashMap<>();

            supportMessages = data.supportMessages != null
                    ? data.supportMessages : new ArrayList<>();

            recoveryRequests = data.recoveryRequests != null
                    ? data.recoveryRequests : new ArrayList<>();

            seq = data.seq;

            // Separate backups take priority over copies in system-data.ser.
            loadRequests();
            loadChats();

            ensureSupportMessageIds();

            loadLocations();
            loadShopPhotos();

            System.out.println(
                    "VARSystem data loaded successfully."
            );

            return true;

        } catch (IOException | ClassNotFoundException
                 | RuntimeException ex) {

            System.err.println(
                    "Unable to load system data: " + ex.getMessage()
            );

            // Preserve independently saved requests and chats.
            loadRequests();
            loadChats();
            loadLocations();

            return false;
        }
    }

    public static synchronized void saveAllData() {

        saveData();
        saveLocations();
        saveShopPhotoPaths();
    }

    // =================================================
    // GENERAL METHODS
    // =================================================

    public static String S(String key, String def) {
        return settings.getOrDefault(key, def);
    }

    public static void log(String message) {

        logs.add(
                0,
                LocalDateTime.now().format(Formats.DT)
                        + "  -  " + message
        );
    }

    // =================================================
    // NOTIFICATION SYSTEM
    // =================================================

    public static synchronized void addNotification(String message) {

        if (message == null || message.isBlank()) {
            return;
        }

        notifs.addFirst(message);
        saveData();
    }

    private static String currentAccountKey() {

        if (Session.ME == null) {
            return "unknown";
        }

        String role = Session.ME.role == null
                ? ""
                : Session.ME.role.trim().toLowerCase();

        String email = Session.ME.email == null
                ? ""
                : Session.ME.email.trim().toLowerCase();

        String shop = Session.ME.shop == null
                ? ""
                : Session.ME.shop.trim().toLowerCase();

        return role + "|" + email + "|" + shop;
    }

    public static synchronized int getUnreadNotificationCount() {

        if (notifs == null) {
            notifs = new LinkedList<>();
        }

        String key = currentAccountKey();

        if (!notificationReadCounts.containsKey(key)) {
            notificationReadCounts.put(key, notifs.size());
            saveData();
            return 0;
        }

        int readCount = notificationReadCounts.getOrDefault(key, 0);

        return Math.max(0, notifs.size() - readCount);
    }

    public static synchronized void markNotificationsRead() {

        String key = currentAccountKey();

        notificationReadCounts.put(key, notifs.size());

        saveData();
    }

    // =================================================
    // UNREAD SUPPORT COUNT
    // =================================================

    public static synchronized int getUnreadSupportCount() {

        if (Session.ME == null
                || !"Admin".equalsIgnoreCase(Session.ME.role)) {
            return 0;
        }

        int count = 0;

        for (SupportMessage message : supportMessages) {

            if (message != null
                    && !message.fromAdmin
                    && !message.readByAdmin) {
                count++;
            }
        }

        return count;
    }

    // =================================================
    // UNREAD SERVICE REQUEST COUNT
    // =================================================

    public static synchronized int getUnreadServiceRequestCount() {

        if (Session.ME == null) {
            return 0;
        }

        String role = Session.ME.role;

        if ("Admin".equalsIgnoreCase(role)) {

            return (int) reqs.stream()
                    .filter(r -> r != null
                            && "Pending".equalsIgnoreCase(r.status))
                    .count();
        }

        if ("Shop Manager".equalsIgnoreCase(role)) {

            String shopName = Session.ME.shop;

            if (shopName == null) {
                return 0;
            }

            return (int) reqs.stream()
                    .filter(r -> r != null
                            && r.shop != null
                            && r.shop.equalsIgnoreCase(shopName)
                            && "Pending".equalsIgnoreCase(r.status))
                    .count();
        }

        return 0;
    }

    // =================================================
    // UNREAD CHAT COUNT
    // =================================================

    public static int getUnreadChatCount() {

        if (Session.ME == null) {
            return 0;
        }

        int unread = 0;
        boolean changed = false;

        synchronized (CHAT_LOCK) {

            if (chats == null) {
                return 0;
            }

            if (chatReadPositions == null) {
                chatReadPositions = new HashMap<>();
            }

            String role = Session.ME.role == null
                    ? ""
                    : Session.ME.role;

            if ("Admin".equalsIgnoreCase(role)) {
                return 0;
            }

            String account = currentAccountKey();

            String currentSender =
                    "Shop Manager".equalsIgnoreCase(role)
                            ? Session.ME.shop
                            : Session.ME.name;

            if (currentSender == null) {
                currentSender = "";
            }

            for (Map.Entry<String, List<String>> entry : chats.entrySet()) {

                String conversationKey = entry.getKey();

                if (conversationKey == null
                        || !conversationKey.startsWith("chat|")) {
                    continue;
                }

                String[] parts = conversationKey.split("\\|", 3);

                if (parts.length < 3) {
                    continue;
                }

                boolean belongsToAccount;

                if ("Shop Manager".equalsIgnoreCase(role)) {

                    belongsToAccount = parts[2].equalsIgnoreCase(
                            Session.ME.shop == null ? "" : Session.ME.shop
                    );

                } else {

                    belongsToAccount = parts[1].equalsIgnoreCase(
                            Session.ME.email == null ? "" : Session.ME.email
                    );
                }

                if (!belongsToAccount) {
                    continue;
                }

                List<String> messages = entry.getValue();

                if (messages == null) {
                    continue;
                }

                String readKey = account + "|" + conversationKey;

                if (!chatReadPositions.containsKey(readKey)) {
                    chatReadPositions.put(readKey, 0);
                    changed = true;
                }

                int readPosition = Math.max(
                        0,
                        Math.min(
                                chatReadPositions.getOrDefault(readKey, 0),
                                messages.size()
                        )
                );

                for (int i = readPosition; i < messages.size(); i++) {

                    String message = messages.get(i);

                    if (message == null) {
                        continue;
                    }

                    int separator = message.indexOf("||");

                    if (separator < 0) {
                        separator = message.indexOf(": ");
                    }

                    String sender = separator >= 0
                            ? message.substring(0, separator).trim()
                            : "";

                    if (!sender.equalsIgnoreCase(currentSender)) {
                        unread++;
                    }
                }
            }
        }

        if (changed) {
            saveData();
        }

        return unread;
    }

    // =================================================
    // MARK SPECIFIC CHAT AS READ
    // =================================================

    public static void markChatRead(
            String userEmail,
            String shopName) {

        if (Session.ME == null) {
            return;
        }

        synchronized (CHAT_LOCK) {

            if (chats == null) {
                chats = new HashMap<>();
            }

            if (chatReadPositions == null) {
                chatReadPositions = new HashMap<>();
            }

            String conversationKey = "chat|"
                    + (userEmail == null ? "" : userEmail.trim().toLowerCase())
                    + "|"
                    + (shopName == null ? "" : shopName.trim().toLowerCase());

            String readKey =
                    currentAccountKey() + "|" + conversationKey;

            List<String> messages = chats.get(conversationKey);

            int position = messages == null ? 0 : messages.size();

            chatReadPositions.put(readKey, position);
        }

        saveData();
    }

    // =================================================
    // MARK ALL CHATS READ
    // =================================================

    public static void markAllChatsRead() {

        if (Session.ME == null) {
            return;
        }

        synchronized (CHAT_LOCK) {

            if (chats == null) {
                return;
            }

            if (chatReadPositions == null) {
                chatReadPositions = new HashMap<>();
            }

            String role = Session.ME.role == null
                    ? ""
                    : Session.ME.role;

            if ("Admin".equalsIgnoreCase(role)) {
                return;
            }

            String account = currentAccountKey();

            for (Map.Entry<String, List<String>> entry : chats.entrySet()) {

                String conversationKey = entry.getKey();

                if (conversationKey == null
                        || !conversationKey.startsWith("chat|")) {
                    continue;
                }

                String[] parts = conversationKey.split("\\|", 3);

                if (parts.length < 3) {
                    continue;
                }

                boolean belongsToAccount;

                if ("Shop Manager".equalsIgnoreCase(role)) {

                    belongsToAccount =
                            parts[2].equalsIgnoreCase(
                                    Session.ME.shop == null ? "" : Session.ME.shop
                            );

                } else {

                    belongsToAccount =
                            parts[1].equalsIgnoreCase(
                                    Session.ME.email == null ? "" : Session.ME.email
                            );
                }

                if (!belongsToAccount) {
                    continue;
                }

                List<String> messages = entry.getValue();

                chatReadPositions.put(
                        account + "|" + conversationKey,
                        messages == null ? 0 : messages.size()
                );
            }
        }

        saveData();
    }



    // =================================================
    // OTHER GENERAL METHODS
    // =================================================

    public static int intS(String key, int def) {

        try {

            return Integer.parseInt(
                    S(key, "" + def).replaceAll("[^0-9]", "")
            );

        } catch (NumberFormatException ex) {
            return def;
        }
    }

    public static int baseFee(String problem) {

        return switch (problem) {
            case "Flat Tire" -> 200;
            case "Battery Replacement" -> 300;
            case "Engine Repair" -> 500;
            case "Brake Service" -> 400;
            case "Oil Change" -> 250;
            case "Towing" -> 800;
            default -> 300;
        };
    }

    public static String nextId() {

        seq++;

        String id = String.format(
                "MR-%s-%03d",
                LocalDate.now().format(
                        DateTimeFormatter.ofPattern("yyyy-MMdd")
                ),
                seq
        );

        saveData();

        return id;
    }

    // =================================================
    // FACTORY METHODS
    // =================================================

    public static User mkUser(
            String n, String e, String u, String p,
            String role, String ph, String loc, String veh,
            String st, int daysAgo) {

        User user = new User();

        user.name = n;
        user.email = e;
        user.username = u;
        user.password = p;
        user.role = role;
        user.phone = ph;
        user.location = loc;
        user.vehicle = veh;
        user.status = st;
        user.joined = LocalDateTime.now().minusDays(daysAgo);

        users.add(user);

        saveData();

        return user;
    }

    public static Shop mkShop(
            String n, String loc, double rt, int rev,
            double d, String ph, String st,
            double x, double y, String... svc) {

        Shop shop = new Shop();

        shop.name = n;
        shop.location = loc;
        shop.rating = rt;
        shop.reviews = rev;
        shop.dist = d;
        shop.phone = ph;
        shop.status = st;
        shop.x = x;
        shop.y = y;

        shop.services.addAll(Arrays.asList(svc));

        shop.about = n
                + " is a trusted repair shop offering quality and reliable automotive services. "
                + "We help keep your vehicle in good condition and ready for your journey.";

        shops.add(shop);

        saveData();

        return shop;
    }

    public static Svc mkSvc(
            String n, String cat, String d, int base, boolean act) {

        Svc service = new Svc();

        service.name = n;
        service.category = cat;
        service.desc = d;
        service.base = base;
        service.active = act;

        svcs.add(service);

        saveData();

        return service;
    }

    // =================================================
    // KEY ENCODING
    // =================================================

    private static String encodeKey(String value) {

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(
                        value.getBytes(StandardCharsets.UTF_8)
                );
    }

    // =================================================
    // SHOP PHOTO SYSTEM - FOUR PHOTOS
    // =================================================

    private static String imageExtension(Path source) {

        String fileName =
                source.getFileName().toString().toLowerCase();

        int dot = fileName.lastIndexOf('.');

        if (dot < 0) {
            return ".png";
        }

        String extension = fileName.substring(dot);

        return switch (extension) {
            case ".jpg", ".jpeg", ".png", ".gif", ".bmp" ->
                    extension;
            default -> ".png";
        };
    }

    public static synchronized boolean saveShopPhoto(
            Shop shop,
            int photoNumber,
            Path source) {

        if (shop == null || shop.name == null
                || shop.name.isBlank()
                || source == null
                || !Files.isRegularFile(source)
                || photoNumber < 1 || photoNumber > 4) {

            return false;
        }

        try {

            Files.createDirectories(SHOP_PHOTO_DIR);

            String extension = imageExtension(source);

            String fileName = encodeKey(shop.name)
                    + "_photo" + photoNumber + extension;

            Path destination =
                    SHOP_PHOTO_DIR.resolve(fileName);

            Files.copy(
                    source,
                    destination,
                    StandardCopyOption.REPLACE_EXISTING
            );

            String savedPath =
                    destination.toAbsolutePath().toString();

            switch (photoNumber) {

                case 1 -> shop.photoPath = savedPath;
                case 2 -> shop.photoPath2 = savedPath;
                case 3 -> shop.photoPath3 = savedPath;
                case 4 -> shop.photoPath4 = savedPath;

                default -> {
                    return false;
                }
            }

            saveShopPhotoPaths();
            saveData();

            return true;

        } catch (IOException ex) {

            System.err.println(
                    "Unable to save shop photo: " + ex.getMessage()
            );

            return false;
        }
    }

    // =================================================
    // UNLIMITED ADDITIONAL SHOP GALLERY PHOTOS
    // =================================================

    private static Shop resolveGalleryShop(Shop shop) {

        if (shop == null || shop.name == null
                || shop.name.isBlank()) {
            return null;
        }

        for (Shop savedShop : shops) {

            if (savedShop != null
                    && savedShop.name != null
                    && savedShop.name.equalsIgnoreCase(shop.name)) {

                if (savedShop.galleryPhotos == null) {
                    savedShop.galleryPhotos = new ArrayList<>();
                }

                if (shop != savedShop && shop.galleryPhotos != null) {

                    for (String path : shop.galleryPhotos) {

                        if (path != null && !path.isBlank()
                                && !savedShop.galleryPhotos.contains(path)) {

                            savedShop.galleryPhotos.add(path);
                        }
                    }
                }

                shop.galleryPhotos = savedShop.galleryPhotos;

                return savedShop;
            }
        }

        return null;
    }

    public static synchronized boolean addShopGalleryPhoto(
            Shop shop,
            Path source) {

        if (shop == null || source == null
                || !Files.isRegularFile(source)) {
            return false;
        }

        Shop savedShop = resolveGalleryShop(shop);

        if (savedShop == null) {
            return false;
        }

        try {

            Files.createDirectories(SHOP_PHOTO_DIR);

            String extension = imageExtension(source);

            String fileName = encodeKey(savedShop.name)
                    + "_gallery_"
                    + UUID.randomUUID()
                    + extension;

            Path destination = SHOP_PHOTO_DIR.resolve(fileName);

            Files.copy(
                    source,
                    destination,
                    StandardCopyOption.REPLACE_EXISTING
            );

            String savedPath = destination.toAbsolutePath().toString();

            savedShop.galleryPhotos.add(savedPath);

            if (!saveData()) {

                savedShop.galleryPhotos.remove(savedPath);
                Files.deleteIfExists(destination);

                return false;
            }

            shop.galleryPhotos = savedShop.galleryPhotos;

            return true;

        } catch (IOException | RuntimeException ex) {

            System.err.println(
                    "Unable to save gallery photo: "
                            + ex.getMessage()
            );

            return false;
        }
    }

    public static synchronized List<String> getShopGalleryPhotos(
            Shop shop) {

        List<String> result = new ArrayList<>();

        if (shop == null) {
            return result;
        }

        Shop savedShop = resolveGalleryShop(shop);

        if (savedShop == null || savedShop.galleryPhotos == null) {
            return result;
        }

        for (String path : savedShop.galleryPhotos) {

            if (path == null || path.isBlank()) {
                continue;
            }

            try {

                if (Files.isRegularFile(Path.of(path))
                        && !result.contains(path)) {

                    result.add(path);
                }

            } catch (RuntimeException ex) {

                System.err.println(
                        "Invalid gallery photo path: " + path
                );
            }
        }

        return result;
    }

    // =================================================
    // DELETE ADDITIONAL SHOP GALLERY PHOTO
    // =================================================

    public static synchronized boolean deleteShopGalleryPhoto(
            Shop shop,
            String photoPath) {

        if (shop == null || photoPath == null
                || photoPath.isBlank()) {
            return false;
        }

        Shop savedShop = resolveGalleryShop(shop);

        if (savedShop == null || savedShop.galleryPhotos == null) {
            return false;
        }

        int index = savedShop.galleryPhotos.indexOf(photoPath);

        if (index < 0) {
            return false;
        }

        String savedPath = savedShop.galleryPhotos.remove(index);

        if (!saveData()) {

            savedShop.galleryPhotos.add(index, savedPath);
            shop.galleryPhotos = savedShop.galleryPhotos;

            return false;
        }

        shop.galleryPhotos = savedShop.galleryPhotos;

        try {

            Files.deleteIfExists(Path.of(savedPath));

        } catch (IOException | RuntimeException ex) {

            System.err.println(
                    "Unable to delete gallery photo: "
                            + ex.getMessage()
            );
        }

        return true;
    }

    private static void storeShopPhotoProperties(
            Properties properties,
            Shop shop) {

        String key = "shop." + encodeKey(shop.name);

        properties.setProperty(
                key + ".photo1",
                safePhotoPath(shop.photoPath)
        );

        properties.setProperty(
                key + ".photo2",
                safePhotoPath(shop.photoPath2)
        );

        properties.setProperty(
                key + ".photo3",
                safePhotoPath(shop.photoPath3)
        );

        properties.setProperty(
                key + ".photo4",
                safePhotoPath(shop.photoPath4)
        );
    }

    private static String safePhotoPath(String path) {
        return path == null ? "" : path;
    }

    public static synchronized void saveShopPhotoPaths() {

        Properties properties = new Properties();

        if (Files.exists(SHOP_PHOTO_FILE)) {

            try (InputStream input =
                         Files.newInputStream(SHOP_PHOTO_FILE)) {

                properties.load(input);

            } catch (IOException ex) {

                System.err.println(
                        "Unable to read existing shop photos: "
                                + ex.getMessage()
                );
            }
        }

        boolean currentShopWasStored = false;

        for (Shop shop : shops) {

            if (shop != null && shop.name != null
                    && !shop.name.isBlank()) {

                storeShopPhotoProperties(properties, shop);

                if (shop == Session.MYSHOP) {
                    currentShopWasStored = true;
                }
            }
        }

        if (!currentShopWasStored
                && Session.MYSHOP != null
                && Session.MYSHOP.name != null
                && !Session.MYSHOP.name.isBlank()) {

            storeShopPhotoProperties(
                    properties,
                    Session.MYSHOP
            );
        }

        try {

            Files.createDirectories(
                    SHOP_PHOTO_FILE.getParent()
            );

            try (OutputStream output =
                         Files.newOutputStream(SHOP_PHOTO_FILE)) {

                properties.store(
                        output,
                        "VAARL Shop Profile Photos"
                );
            }

        } catch (IOException ex) {

            System.err.println(
                    "Unable to save shop photo paths: "
                            + ex.getMessage()
            );
        }
    }

    public static synchronized void loadShopPhotos() {

        if (!Files.exists(SHOP_PHOTO_FILE)) {
            return;
        }

        Properties properties = new Properties();

        try (InputStream input =
                     Files.newInputStream(SHOP_PHOTO_FILE)) {

            properties.load(input);

            for (Shop shop : shops) {

                if (shop == null || shop.name == null
                        || shop.name.isBlank()) {
                    continue;
                }

                String key = "shop." + encodeKey(shop.name);

                shop.photoPath = validPhotoPath(
                        properties.getProperty(key + ".photo1", "")
                );

                shop.photoPath2 = validPhotoPath(
                        properties.getProperty(key + ".photo2", "")
                );

                shop.photoPath3 = validPhotoPath(
                        properties.getProperty(key + ".photo3", "")
                );

                shop.photoPath4 = validPhotoPath(
                        properties.getProperty(key + ".photo4", "")
                );
            }

        } catch (IOException | RuntimeException ex) {

            System.err.println(
                    "Unable to load shop photos: "
                            + ex.getMessage()
            );
        }
    }

    private static String validPhotoPath(String path) {

        if (path == null || path.isBlank()) {
            return "";
        }

        try {
            return Files.isRegularFile(Path.of(path)) ? path : "";
        } catch (RuntimeException ex) {
            return "";
        }
    }

    // =================================================
    // LOCATION SYSTEM
    // =================================================

    public static synchronized void loadLocations() {

        if (!Files.exists(LOCATION_FILE)) {
            return;
        }

        Properties properties = new Properties();

        try (InputStream input =
                     Files.newInputStream(LOCATION_FILE)) {

            properties.load(input);

            for (Shop shop : shops) {

                if (shop == null || shop.name == null) {
                    continue;
                }

                String key = "shop." + encodeKey(shop.name);

                String saved =
                        properties.getProperty(key + ".hasLocation");

                if (saved == null) {
                    continue;
                }

                if (!Boolean.parseBoolean(saved)) {

                    shop.latitude = Double.NaN;
                    shop.longitude = Double.NaN;

                    continue;
                }

                double lat = Double.parseDouble(
                        properties.getProperty(
                                key + ".latitude", "NaN"
                        )
                );

                double lon = Double.parseDouble(
                        properties.getProperty(
                                key + ".longitude", "NaN"
                        )
                );

                if (validCoordinates(lat, lon)) {

                    shop.latitude = lat;
                    shop.longitude = lon;
                }
            }

            for (Req request : reqs) {

                if (request == null || request.id == null) {
                    continue;
                }

                String key = "request." + encodeKey(request.id);

                String saved =
                        properties.getProperty(key + ".hasLocation");

                if (saved == null) {
                    continue;
                }

                if (!Boolean.parseBoolean(saved)) {

                    request.userLatitude = Double.NaN;
                    request.userLongitude = Double.NaN;
                    request.shareLocation = false;

                    continue;
                }

                double lat = Double.parseDouble(
                        properties.getProperty(
                                key + ".latitude", "NaN"
                        )
                );

                double lon = Double.parseDouble(
                        properties.getProperty(
                                key + ".longitude", "NaN"
                        )
                );

                if (validCoordinates(lat, lon)) {

                    request.userLatitude = lat;
                    request.userLongitude = lon;

                    request.shareLocation = Boolean.parseBoolean(
                            properties.getProperty(
                                    key + ".shareLocation",
                                    "false"
                            )
                    );
                }
            }

        } catch (IOException | NumberFormatException ex) {

            System.err.println(
                    "Unable to load saved locations: "
                            + ex.getMessage()
            );
        }
    }

    private static boolean validCoordinates(
            double lat, double lon) {

        return Double.isFinite(lat)
                && Double.isFinite(lon)
                && lat >= -90 && lat <= 90
                && lon >= -180 && lon <= 180;
    }

    public static synchronized void saveLocations() {

        Properties properties = new Properties();

        for (Shop shop : shops) {

            if (shop == null || shop.name == null) {
                continue;
            }

            String key = "shop." + encodeKey(shop.name);

            boolean hasLocation = shop.hasLocation();

            properties.setProperty(
                    key + ".hasLocation",
                    Boolean.toString(hasLocation)
            );

            if (hasLocation) {

                properties.setProperty(
                        key + ".latitude",
                        Double.toString(shop.latitude)
                );

                properties.setProperty(
                        key + ".longitude",
                        Double.toString(shop.longitude)
                );
            }
        }

        for (Req request : reqs) {

            if (request == null || request.id == null) {
                continue;
            }

            String key = "request." + encodeKey(request.id);

            boolean hasLocation = request.hasUserLocation();

            properties.setProperty(
                    key + ".hasLocation",
                    Boolean.toString(hasLocation)
            );

            if (hasLocation) {

                properties.setProperty(
                        key + ".latitude",
                        Double.toString(request.userLatitude)
                );

                properties.setProperty(
                        key + ".longitude",
                        Double.toString(request.userLongitude)
                );

                properties.setProperty(
                        key + ".shareLocation",
                        Boolean.toString(request.shareLocation)
                );
            }
        }

        try {

            Files.createDirectories(
                    LOCATION_FILE.getParent()
            );

            try (OutputStream output =
                         Files.newOutputStream(LOCATION_FILE)) {

                properties.store(
                        output,
                        "VAARL Saved Locations"
                );
            }

        } catch (IOException ex) {

            System.err.println(
                    "Unable to save locations: " + ex.getMessage()
            );
        }
    }

    // =================================================
    // FEEDBACK SYSTEM
    // =================================================

    public static synchronized boolean addFeedback(
            Feedback feedback) {

        if (feedback == null || !feedback.isValid()) {
            return false;
        }

        Req request = reqs.stream()
                .filter(r -> r != null
                        && r.id != null
                        && r.id.equalsIgnoreCase(
                                feedback.requestId
                        ))
                .findFirst()
                .orElse(null);

        if (request == null
                || !"Completed".equalsIgnoreCase(request.status)
                || request.uemail == null
                || feedback.userEmail == null
                || !request.uemail.equalsIgnoreCase(
                        feedback.userEmail
                )
                || request.shop == null
                || feedback.shop == null
                || !request.shop.equalsIgnoreCase(
                        feedback.shop
                )) {

            return false;
        }

        if (hasFeedback(feedback.requestId)) {
            return false;
        }

        feedbacks.add(feedback);

        updateShopRating(feedback.shop);

        log("Feedback submitted for " + feedback.shop);

        saveData();

        return true;
    }

    public static synchronized void updateShopRating(
            String shopName) {

        if (shopName == null) {
            return;
        }

        Shop shop = shops.stream()
                .filter(s -> s != null
                        && s.name != null
                        && s.name.equalsIgnoreCase(shopName))
                .findFirst()
                .orElse(null);

        if (shop == null) {
            return;
        }

        List<Feedback> ratings =
                getShopFeedback(shopName);

        shop.reviews = ratings.size();

        if (ratings.isEmpty()) {

            shop.rating = 0;

            return;
        }

        double total = ratings.stream()
                .mapToInt(f -> f.rating)
                .sum();

        shop.rating = total / ratings.size();
    }

    public static synchronized boolean hasFeedback(
            String requestId) {

        if (requestId == null) {
            return false;
        }

        return feedbacks.stream()
                .anyMatch(f -> f != null
                        && f.requestId != null
                        && f.requestId.equalsIgnoreCase(requestId));
    }

    public static synchronized List<Feedback> getShopFeedback(
            String shopName) {

        if (shopName == null) {
            return new ArrayList<>();
        }

        return feedbacks.stream()
                .filter(f -> f != null
                        && f.shop != null
                        && f.shop.equalsIgnoreCase(shopName))
                .toList();
    }

    // =================================================
    // SHOP STATISTICS
    // =================================================

    public static synchronized void addShopView(Shop shop) {

        if (shop != null) {

            shop.views++;

            saveData();
        }
    }

    public static synchronized int getCustomersHelped(
            String shopName) {

        if (shopName == null) {
            return 0;
        }

        return (int) reqs.stream()
                .filter(r -> r != null
                        && r.shop != null
                        && r.shop.equalsIgnoreCase(shopName)
                        && "Completed".equalsIgnoreCase(r.status))
                .count();
    }
}
