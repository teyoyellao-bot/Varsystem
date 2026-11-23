
package varsystem.service;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import varsystem.data.DB;
import varsystem.model.User;

/** Password hashing and legacy plain-text password upgrade support. */
public final class PasswordSecurity {

    private static final int ITERATIONS = 210_000;
    private static final int KEY_LENGTH = 256;
    private static final int SALT_LENGTH = 16;
    private static final String PREFIX = "PBKDF2$";
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordSecurity() { }

    public static String hash(String password) {
        if (password == null) {
            throw new IllegalArgumentException("Password is required.");
        }

        byte[] salt = new byte[SALT_LENGTH];
        RANDOM.nextBytes(salt);
        byte[] derived = derive(password.toCharArray(), salt, ITERATIONS);

        return PREFIX + ITERATIONS + "$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(derived);
    }

    public static boolean verify(String password, String stored) {
        if (password == null || stored == null) {
            return false;
        }

        if (!stored.startsWith(PREFIX)) {
            return stored.equals(password);
        }

        try {
            String[] parts = stored.split("\\$", -1);
            if (parts.length != 4) return false;

            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            byte[] actual = derive(password.toCharArray(), salt, iterations);

            return MessageDigest.isEqual(expected, actual);

        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    /** Verifies a login and upgrades legacy plain-text passwords on success. */
    public static boolean verifyAndUpgrade(User user, String password) {
        if (user == null || !verify(password, user.password)) {
            return false;
        }

        if (user.password == null || !user.password.startsWith(PREFIX)) {
            user.password = hash(password);
            DB.saveAllData();
        }

        return true;
    }

    private static byte[] derive(
            char[] password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(
                password, salt, iterations, KEY_LENGTH);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException(
                    "Password hashing is unavailable.", ex);
        } finally {
            spec.clearPassword();
            java.util.Arrays.fill(password, '\0');
        }
    }
}

