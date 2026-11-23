package varsystem.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class RecoveryRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String PENDING = "Pending";
    public static final String APPROVED = "Approved";
    public static final String REJECTED = "Rejected";
    public static final String USED = "Used";

    public String id;
    public String accountEmail;
    public String accountRole;
    public String accountName;
    public String username;
    public String phone = "";
    public String proofPhotoPath = "";

    public String status = PENDING;
    public String adminNote = "";

    public LocalDateTime submittedAt = LocalDateTime.now();
    public LocalDateTime decidedAt;

    public boolean resetCompleted = false;

    public RecoveryRequest() {
    }

    public RecoveryRequest(
            String id,
            String accountEmail,
            String accountRole,
            String accountName,
            String username,
            String phone,
            String proofPhotoPath) {

        this.id = id;
        this.accountEmail = accountEmail;
        this.accountRole = accountRole;
        this.accountName = accountName;
        this.username = username;
        this.phone = phone;
        this.proofPhotoPath = proofPhotoPath;

        this.status = PENDING;
        this.adminNote = "";
        this.submittedAt = LocalDateTime.now();
        this.decidedAt = null;
        this.resetCompleted = false;
    }

    public boolean isPending() {
        return PENDING.equalsIgnoreCase(status);
    }

    public boolean isApproved() {
        return APPROVED.equalsIgnoreCase(status);
    }

    public boolean isRejected() {
        return REJECTED.equalsIgnoreCase(status);
    }

    public boolean isUsed() {
        return USED.equalsIgnoreCase(status) || resetCompleted;
    }

    public boolean isValid() {
        return id != null
                && !id.trim().isEmpty()
                && accountEmail != null
                && !accountEmail.trim().isEmpty()
                && accountRole != null
                && !accountRole.trim().isEmpty();
    }
}