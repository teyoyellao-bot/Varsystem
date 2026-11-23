
package varsystem.ui.auth;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import varsystem.data.DB;
import varsystem.model.Shop;
import varsystem.model.User;
import varsystem.service.NotificationService;
import varsystem.ui.components.Dialogs.DateInput;
import varsystem.ui.components.Dialogs.FileInput;
import varsystem.ui.components.Dialogs.PasswordInput;
import static varsystem.ui.components.Dialogs.err;
import static varsystem.ui.components.Dialogs.formValidated;
import static varsystem.ui.components.Dialogs.msg;

public final class RegisterDialog {

    private RegisterDialog() { }

    public static void show() {

        FileInput shopPhoto = new FileInput("Upload Shop Photo");
        FileInput idPicture = new FileInput("Upload Shop Owner ID");
        FileInput driverLicense = new FileInput(
            "Upload Driver's License"
        );

        String[] r = formValidated(
            "Create an account",

            new String[]{
                "Account type",
                "Full name",
                "Email",
                "Username",
                "Phone",
                "Location",
                "Shop name (repair shops)",
                "Shop photo",
                "Shop owner ID picture",
                "Driver's license (Optional - Vehicle Owner only)",
                "Password",
                "Confirm password",
                "Birthday"
            },

            new Object[]{
                new String[]{"Vehicle Owner", "Repair Shop"},
                "", "", "", "", "", "",
                shopPhoto,
                idPicture,
                driverLicense,
                new PasswordInput(),
                new PasswordInput(),
                new DateInput()
            },

            values -> {

                boolean shop = "Repair Shop".equals(values[0]);

                if (values[1].trim().isEmpty()
                        || values[2].trim().isEmpty()
                        || values[3].trim().isEmpty()
                        || values[10].isEmpty()) {

                    return "Please fill in name, email, username and password.";
                }

                if (!values[2].matches(
                        "[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {

                    return "Please enter a valid email address.";
                }

                if (values[12] == null || values[12].trim().isEmpty()) {
                    return "Please select your birthday.";
                }

                try {
                    java.time.LocalDate birthday =
                        java.time.LocalDate.parse(values[12]);

                    if (birthday.isAfter(java.time.LocalDate.now())) {
                        return "Birthday cannot be in the future.";
                    }
                } catch (java.time.format.DateTimeParseException ex) {
                    return "Please select a valid birthday.";
                }

                if (shop && values[6].trim().isEmpty()) {
                    return "Please enter your shop name.";
                }

                if (!shop && !values[6].trim().isEmpty()) {
                    return "Shop name is only for Repair Shop accounts.";
                }

                if (shop && values[7].trim().isEmpty()) {
                    return "Please upload your Shop photo.";
                }

                if (shop && values[8].trim().isEmpty()) {
                    return "Please upload your Shop Owner ID picture.";
                }

                int minPassword = DB.intS(
                    "Security.Minimum Password Length", 6
                );

                if (values[10].length() < minPassword) {
                    return "Password must be at least "
                        + minPassword + " characters.";
                }

                if (!values[10].equals(values[11])) {
                    return "Passwords do not match.";
                }

                String enteredName = values[1].trim()
                    .replaceAll("\\s+", " ")
                    .toLowerCase(java.util.Locale.ROOT);

                for (User u : DB.users) {

                    if (u == null) continue;

                    String existingName = u.name == null
                        ? ""
                        : u.name.trim()
                            .replaceAll("\\s+", " ")
                            .toLowerCase(java.util.Locale.ROOT);

                    if (!existingName.isEmpty()
                            && existingName.equals(enteredName)) {
                        return "This full name is already registered. "
                            + "Please use a different, correct full name.";
                    }

                    if ((u.email != null
                            && u.email.equalsIgnoreCase(values[2].trim()))
                            || (u.username != null
                            && u.username.equalsIgnoreCase(values[3].trim()))) {

                        return "Email or username is already registered.";
                    }
                }

                return null;
            }
        );

        if (r == null) return;

        boolean shop = "Repair Shop".equals(r[0]);

        String shopPhotoPath = "";
        String idPicturePath = "";
        String driverLicensePath = "";

        if (shop) {

            shopPhotoPath = saveShopImage(
                new File(r[7]),
                r[6].trim(),
                "shop-photos"
            );

            if (shopPhotoPath == null) {
                err("Unable to save Shop photo. Please try again.");
                return;
            }

            idPicturePath = saveShopImage(
                new File(r[8]),
                r[6].trim(),
                "shop-ids"
            );

            if (idPicturePath == null) {
                err("Unable to save ID picture. Please try again.");
                return;
            }

        } else {

            // Optional Driver's License for Vehicle Owner
            if (!r[9].trim().isEmpty()) {

                driverLicensePath = saveShopImage(
                    new File(r[9]),
                    r[1].trim(),
                    "driver-licenses"
                );

                if (driverLicensePath == null) {
                    err("Unable to save Driver's License. Please upload a valid JPG or PNG image.");
                    return;
                }
            }
        }

        User u = DB.mkUser(
            r[1].trim(),
            r[2].trim(),
            r[3].trim(),
            r[10],
            shop ? "Shop Manager" : "User",
            r[4].trim(),
            r[5].trim(),
            "",
            shop ? "Pending" : "Active",
            0
        );

        u.birthday = r[12];

        if (shop) {

            u.shop = r[6].trim();

            Shop newShop = DB.mkShop(
                r[6].trim(),
                r[5].trim().isEmpty()
                    ? "Balayan, Batangas"
                    : r[5].trim(),
                0, 0, 2.5,
                r[4].trim().isEmpty()
                    ? "N/A"
                    : r[4].trim(),
                "Pending",
                0.2 + Math.random() * 0.65,
                0.15 + Math.random() * 0.7,
                "General Maintenance"
            );

            newShop.owner = u.email;
            newShop.shopPhotoPath = shopPhotoPath;
            newShop.photoPath = shopPhotoPath;
            newShop.idPicturePath = idPicturePath;

            DB.addNotification(
                "New repair shop application: "
                + r[6].trim()
                + " submitted a shop application."
            );

            msg(
                "Registration submitted. Your shop is now waiting "
                + "for administrator verification. You can log in "
                + "after registration, but your shop will only "
                + "appear in searches once approved."
            );

        } else {

            if (!driverLicensePath.isEmpty()) {
                u.license = driverLicensePath;
            }

            DB.addNotification(
                "New user registration: "
                + r[1].trim()
                + " has registered as a new user."
            );

            msg("Account created. You can now log in.");
        }

        NotificationService.newRegistration(u.role, u.name, u.email);

        DB.log("Registered: " + r[3] + " (" + u.role + ")");

        DB.saveAllData();
    }

    private static String saveShopImage(
            File selectedFile,
            String shopName,
            String folderName) {

        if (selectedFile == null || !selectedFile.isFile()) {
            return null;
        }

        try {

            String fileName = selectedFile.getName().toLowerCase();

            if (!(fileName.endsWith(".jpg")
                    || fileName.endsWith(".jpeg")
                    || fileName.endsWith(".png"))) {

                return null;
            }

            Path folder = Path.of(
                System.getProperty("user.home"),
                ".vaarl",
                folderName
            );

            Files.createDirectories(folder);

            String safeName = shopName.replaceAll(
                "[^a-zA-Z0-9_-]", "_"
            );

            String extension = fileName.substring(
                fileName.lastIndexOf('.')
            );

            Path destination = folder.resolve(
                safeName + "_" + System.currentTimeMillis() + extension
            );

            Files.copy(
                selectedFile.toPath(),
                destination,
                StandardCopyOption.REPLACE_EXISTING
            );

            return destination.toAbsolutePath().toString();

        } catch (IOException ex) {

            System.err.println(
                "Unable to save image: " + ex.getMessage()
            );

            return null;
        }
    }
}

