package varsystem.ui.auth;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;

import varsystem.data.DB;
import varsystem.model.RecoveryRequest;
import varsystem.model.User;
import varsystem.service.PasswordSecurity;
import varsystem.ui.components.Dialogs.FileInput;
import varsystem.ui.components.Dialogs.PasswordInput;

import static varsystem.ui.components.Dialogs.ask;
import static varsystem.ui.components.Dialogs.err;
import static varsystem.ui.components.Dialogs.form;
import static varsystem.ui.components.Dialogs.msg;

public final class ForgotPasswordDialog {

    private ForgotPasswordDialog() {
    }

    // =================================================
    // FORGOT PASSWORD
    // =================================================

    public static void show() {

        // STEP 1: ACCOUNT VERIFICATION
        String[] details = form(
                "Forgot Password - Verify Your Account",
                new String[]{
                    "Registered Email",
                    "Account Type",
                    "Full Name",
                    "Username",
                    "Birthday (YYYY-MM-DD)"
                },
                new Object[]{
                    "",
                    new String[]{
                        "Vehicle Owner / User",
                        "Repair Shop"
                    },
                    "", "", ""
                }
        );

        if (details == null) {
            return;
        }

        String email = clean(details[0]);
        String accountType = clean(details[1]);
        String fullName = normalize(details[2]);
        String username = clean(details[3]);
        String birthdayText = clean(details[4]);

        if (email.isEmpty()
                || accountType.isEmpty()
                || fullName.isEmpty()
                || username.isEmpty()
                || birthdayText.isEmpty()) {

            err("Please complete all account verification fields.");
            return;
        }

        // STEP 2: VALIDATE BIRTHDAY
        LocalDate birthday;

        try {
            birthday = LocalDate.parse(birthdayText);
        } catch (DateTimeParseException ex) {
            err("Please enter your birthday using YYYY-MM-DD.");
            return;
        }

        if (birthday.isAfter(LocalDate.now())) {
            err("Birthday cannot be in the future.");
            return;
        }

        // STEP 3: FIND ACCOUNT
        User account = DB.users.stream()
                .filter(u -> u != null
                        && u.email != null
                        && u.email.equalsIgnoreCase(email))
                .findFirst()
                .orElse(null);

        boolean selectedShop =
                "Repair Shop".equalsIgnoreCase(accountType);

        boolean actualShop = account != null
                && ("Shop Manager".equalsIgnoreCase(account.role)
                    || "Shop".equalsIgnoreCase(account.role));

        boolean hasSavedBirthday = account != null
                && account.birthday != null
                && !account.birthday.isBlank();

        boolean birthdayMatches = account != null
                && (!hasSavedBirthday
                    || account.birthday.equals(birthday.toString()));

        boolean accountDetailsMatch = account != null
                && account.name != null
                && normalize(account.name).equals(fullName)
                && account.username != null
                && account.username.equalsIgnoreCase(username)
                && birthdayMatches
                && selectedShop == actualShop;

        if (!accountDetailsMatch) {

            err("We could not verify the account details. "
                    + "Check your information or use Admin-Assisted Recovery.");

            return;
        }

        // STEP 4: CHECK LATEST RECOVERY REQUEST
        RecoveryRequest existing =
                DB.latestRecovery(account.email, account.role);

        if (existing != null) {

            // APPROVED BUT UNUSED
            if (DB.isRecoveryUsable(existing)) {

                msg("Your recovery request has already been approved.\n\n"
                        + "You may use this approval to reset your password "
                        + "once. It cannot be reused.");

                resetPassword(account, existing.id);
                return;
            }

            // STILL WAITING FOR ADMIN
            if (RecoveryRequest.PENDING.equalsIgnoreCase(
                    existing.status)) {

                msg("Your recovery request is still Pending Admin review.\n\n"
                        + "Please wait until the Admin approves or rejects "
                        + "your request. Password changes are not allowed "
                        + "while it is pending.");

                return;
            }

            // REJECTED REQUEST
            if (RecoveryRequest.REJECTED.equalsIgnoreCase(
                    existing.status)) {

                String note = existing.adminNote == null
                        ? ""
                        : existing.adminNote.trim();

                msg("Your previous recovery request was Rejected."
                        + (note.isEmpty()
                            ? ""
                            : "\nAdmin's reason: " + note)
                        + "\n\nYou may submit a NEW recovery request "
                        + "with a new proof photo.");
            }

            // USED REQUEST
            if (RecoveryRequest.USED.equalsIgnoreCase(existing.status)
                    || existing.resetCompleted) {

                msg("Your previous Admin approval has already been USED.\n\n"
                        + "You cannot reuse that authorization. "
                        + "You must submit a NEW recovery request "
                        + "and proof photo for Admin review.");
            }
        }

        // STEP 5: ALL PASSWORD RESETS REQUIRE ADMIN APPROVAL
        submitAdminRecovery(account);
    }

    // =================================================
    // PASSWORD RESET
    // =================================================

    private static void resetPassword(
            User account,
            String recoveryId) {

        if (account == null || recoveryId == null
                || recoveryId.isBlank()) {

            err("Invalid recovery authorization.");
            return;
        }

        // CHECK APPROVAL BEFORE DISPLAYING THE PASSWORD FORM
        RecoveryRequest recovery = DB.getRecovery(recoveryId);

        if (!DB.isRecoveryUsable(recovery)) {

            err("This recovery approval is Pending, Rejected, Used, "
                    + "or expired. Password reset is not allowed.");

            return;
        }

        if (recovery.accountEmail == null
                || account.email == null
                || !recovery.accountEmail.equalsIgnoreCase(
                        account.email)
                || recovery.accountRole == null
                || account.role == null
                || !recovery.accountRole.equalsIgnoreCase(
                        account.role)) {

            err("Recovery approval does not match this account.");
            return;
        }

        // STEP 1: PASSWORD FORM
        String[] passwords = form(
                "Create a New Password",
                new String[]{
                    "New Password",
                    "Confirm New Password"
                },
                new Object[]{
                    new PasswordInput(),
                    new PasswordInput()
                }
        );

        if (passwords == null) {
            return;
        }

        String newPassword = passwords[0];
        String confirmPassword = passwords[1];

        if (newPassword == null
                || confirmPassword == null
                || newPassword.isEmpty()
                || confirmPassword.isEmpty()) {

            err("Please complete both password fields.");
            return;
        }

        // STEP 2: PASSWORD REQUIREMENTS
        int minimumLength = DB.intS(
                "Security.Minimum Password Length",
                8
        );

        if (newPassword.length() < minimumLength) {

            err("Password must contain at least "
                    + minimumLength + " characters.");

            return;
        }

        if (!newPassword.equals(confirmPassword)) {

            err("Passwords do not match.");
            return;
        }

        if (PasswordSecurity.verify(
                newPassword,
                account.password)) {

            err("Your new password must be different "
                    + "from your current password.");

            return;
        }

        // STEP 3: RECHECK APPROVAL BEFORE COMMITTING
        recovery = DB.getRecovery(recoveryId);

        if (!DB.isRecoveryUsable(recovery)) {

            err("This recovery approval is no longer valid "
                    + "or has already been used.");

            return;
        }

        if (recovery.accountEmail == null
                || recovery.accountRole == null
                || account.email == null
                || account.role == null
                || !recovery.accountEmail.equalsIgnoreCase(
                        account.email)
                || !recovery.accountRole.equalsIgnoreCase(
                        account.role)) {

            err("Recovery authorization does not match the account.");
            return;
        }

        // STEP 4: HASH PASSWORD
        String hashedPassword;

        try {
            hashedPassword = PasswordSecurity.hash(newPassword);
        } catch (RuntimeException ex) {

            err("Unable to secure the new password. Please try again.");
            return;
        }

        // STEP 5: ATOMIC DB UPDATE
        // DB checks approval again, changes password, increments
        // reset count, marks request USED, and saves the changes.
        boolean completed = DB.completeRecoveryWithPassword(
                recoveryId,
                account,
                hashedPassword
        );

        if (!completed) {

            err("Password reset was not completed.\n\n"
                    + "This recovery approval may already be used, "
                    + "expired, or no longer approved. "
                    + "Please submit a new recovery request if necessary.");

            return;
        }

        msg("Your password has been reset successfully.\n\n"
                + "Your recovery approval is now USED and cannot "
                + "be reused. You may log in using your new password.");
    }

    // =================================================
    // ADMIN-ASSISTED RECOVERY
    // =================================================

    private static void submitAdminRecovery(User account) {

        msg("ADMIN-ASSISTED RECOVERY\n\n"
                + "You must upload a clear photo for Admin verification.\n\n"
                + "Examples:\n"
                + "- Vehicle Owner: selfie holding a valid ID\n"
                + "- Repair Shop: business permit or shop verification photo\n\n"
                + "Your proof photo is stored privately for Admin review "
                + "and is not displayed in the support chat.");

        // STEP 1: UPLOAD PROOF
        String[] upload = form(
                "Admin-Assisted Recovery - Upload Proof",
                new String[]{
                    "Proof Photo (JPG or PNG, max 5 MB)"
                },
                new Object[]{
                    new FileInput("Upload Proof Photo")
                }
        );

        if (upload == null) {
            return;
        }

        String photoPath = clean(upload[0]);

        if (photoPath.isEmpty()) {

            err("Please upload a proof photo to continue.");
            return;
        }

        // STEP 2: CONFIRM SUBMISSION
        if (!ask("Submit a NEW recovery request to Admin?\n\n"
                + "Account: " + account.name
                + "\nUsername: " + account.username
                + "\n\nAdmin approval is required before "
                + "you can change your password.")) {

            return;
        }

        // STEP 3: SUBMIT
        RecoveryRequest request =
                DB.submitRecoveryRequest(
                        account,
                        Path.of(photoPath)
                );

        if (request != null) {

            msg("Your NEW Admin-Assisted Recovery request "
                    + "has been submitted successfully.\n\n"
                    + "Request Status: Pending\n\n"
                    + "The Admin must review and approve your request "
                    + "before you can change your password.");

        } else {

            err("Unable to submit your recovery request.\n\n"
                    + "A pending request may already exist, or the "
                    + "photo may be invalid. Please check that the photo "
                    + "is JPG or PNG and does not exceed 5 MB.");
        }
    }

    // =================================================
    // HELPERS
    // =================================================

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static String normalize(String value) {

        return clean(value)
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
    }
}