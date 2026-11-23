package com.vaarl.web.api;

import java.util.Map;
import java.util.UUID;
import java.util.Locale;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.transaction.annotation.Transactional;
import jakarta.validation.Valid;
import org.springframework.util.StringUtils;
import com.vaarl.web.auth.LoginRequest;
import com.vaarl.web.auth.RegisterRequest;
import com.vaarl.web.auth.JwtService;
import com.vaarl.web.service.AuditLogService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final JavaMailSender mail;
    private final String verifyUrl;
    private final AuditLogService audit;
    private static final SecureRandom RANDOM = new SecureRandom();

    public AuthController(JdbcTemplate jdbc, PasswordEncoder encoder, JwtService jwt, JavaMailSender mail,
            @Value("${vaarl.email-verification-url}") String verifyUrl, AuditLogService audit) {
        this.jdbc = jdbc;
        this.encoder = encoder;
        this.jwt = jwt;
        this.mail = mail;
        this.verifyUrl = verifyUrl;
        this.audit = audit;
    }

    @PostMapping("/register")
    @Transactional
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        String role = request.role() == null ? "" : request.role().trim().toUpperCase();
        String fullName = request.fullName().trim();
        String email = request.email().trim().toLowerCase();
        String username = request.username().trim();
        String phone = request.phone().trim();
        String shopName = request.shopName() == null ? "" : request.shopName().trim();
        if (!"VEHICLE_OWNER".equals(role) && !"REPAIR_SHOP".equals(role)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid account type."));
        }
        if (!request.password().equals(request.confirmPassword())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Passwords do not match."));
        }
        if ("REPAIR_SHOP".equals(role) && !StringUtils.hasText(shopName)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Shop name is required."));
        }
        try {
            UUID accountId = UUID.randomUUID();
            jdbc.update("""
                INSERT INTO web_accounts
                (id, full_name, email, username, phone, account_role, shop_name, password_hash, email_verified)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, false)
                """,
                accountId, fullName, email,
                username, phone, role,
                "REPAIR_SHOP".equals(role) ? shopName : null,
                encoder.encode(request.password()));
            if ("REPAIR_SHOP".equals(role)) {
                jdbc.update("""
                    INSERT INTO web_repair_shops (id, account_id, shop_name, location_text, phone, status)
                    VALUES (?, ?, ?, ?, ?, 'Active')
                    """, UUID.randomUUID(), accountId, shopName, "", phone);
            }
            byte[] tokenBytes = new byte[32];
            RANDOM.nextBytes(tokenBytes);
            String rawToken = HexFormat.of().formatHex(tokenBytes);
            String tokenHash = sha256(rawToken);
            jdbc.update("INSERT INTO web_email_verification_tokens(id, account_id, token_hash, expires_at) VALUES (?, ?, ?, ?)",
                UUID.randomUUID(), accountId, tokenHash, Instant.now().plusSeconds(24 * 60 * 60));
            SimpleMailMessage verification = new SimpleMailMessage();
            verification.setTo(email);
            verification.setSubject("Verify your VAARL email");
            verification.setText("Verify your email using this link (expires in 24 hours):\\n\\n" + verifyUrl + "?token=" + rawToken);
            mail.send(verification);
            audit.record(accountId, username, "ACCOUNT_REGISTERED", "Account registration completed; email verification pending.");
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "Registration successful. Please check your email to verify your account."));
        } catch (DuplicateKeyException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "Email or username is already registered."));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        String loginKey = request.usernameOrEmail().trim().toLowerCase(Locale.ROOT);
        var lockRows = jdbc.query("""
            SELECT failures, locked_until FROM web_login_attempts WHERE login_key=?
            """, (rs, rowNum) -> new LoginLock(rs.getInt("failures"), rs.getTimestamp("locked_until") == null ? null : rs.getTimestamp("locked_until").toInstant()), loginKey);
        if (!lockRows.isEmpty() && lockRows.get(0).lockedUntil() != null && lockRows.get(0).lockedUntil().isAfter(Instant.now())) {
            audit.record(null, loginKey, "LOGIN_LOCKED", "Login attempt rejected while account identifier is locked.");
            long remaining = Math.max(1, java.time.Duration.between(Instant.now(), lockRows.get(0).lockedUntil()).toSeconds());
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(Map.of("message", "Too many failed login attempts. Please try again later.", "retryAfterSeconds", remaining));
        }
        var rows = jdbc.query("""
            SELECT id, username, email, account_role, password_hash, email_verified
            FROM web_accounts
            WHERE lower(username) = lower(?) OR lower(email) = lower(?)
            LIMIT 1
            """,
            (rs, rowNum) -> new Account(
                rs.getString("id"), rs.getString("username"), rs.getString("email"),
                rs.getString("account_role"), rs.getString("password_hash"), rs.getBoolean("email_verified")),
            request.usernameOrEmail().trim(), request.usernameOrEmail().trim());
        if (rows.isEmpty() || !encoder.matches(request.password(), rows.get(0).passwordHash())) {
            audit.record(rows.isEmpty() ? null : UUID.fromString(rows.get(0).id()), loginKey, "LOGIN_FAILED", "Invalid username/email or password.");
            jdbc.update("""
                INSERT INTO web_login_attempts(login_key, failures, locked_until, updated_at)
                VALUES (?, 1, NULL, CURRENT_TIMESTAMP)
                ON CONFLICT (login_key) DO UPDATE SET
                  failures = CASE WHEN web_login_attempts.locked_until IS NOT NULL
                                      AND web_login_attempts.locked_until <= CURRENT_TIMESTAMP
                                THEN 1 ELSE web_login_attempts.failures + 1 END,
                  locked_until = CASE WHEN (CASE WHEN web_login_attempts.locked_until IS NOT NULL
                                                   AND web_login_attempts.locked_until <= CURRENT_TIMESTAMP
                                             THEN 1 ELSE web_login_attempts.failures + 1 END) >= 5
                                  THEN CURRENT_TIMESTAMP + INTERVAL '15 minutes'
                                  ELSE NULL END,
                  updated_at = CURRENT_TIMESTAMP
                """, loginKey);
            var failureRows = jdbc.query("SELECT failures, locked_until FROM web_login_attempts WHERE login_key=?",
                (rs, n) -> new LoginLock(rs.getInt("failures"), rs.getTimestamp("locked_until") == null ? null : rs.getTimestamp("locked_until").toInstant()), loginKey);
            if (!failureRows.isEmpty() && failureRows.get(0).lockedUntil() != null) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("message", "Too many failed login attempts. Please try again in 15 minutes."));
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Invalid username/email or password."));
        }
        jdbc.update("DELETE FROM web_login_attempts WHERE login_key=?", loginKey);
        Account a = rows.get(0);
        if (!a.emailVerified()) {
            audit.record(UUID.fromString(a.id()), a.username(), "LOGIN_BLOCKED_UNVERIFIED", "Login denied because email verification is incomplete.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Please verify your email before signing in."));
        }
        audit.record(UUID.fromString(a.id()), a.username(), "LOGIN_SUCCESS", "User signed in successfully.");
        String token = jwt.createToken(a.id(), a.role(), a.username());
        return ResponseEntity.ok(Map.of(
            "token", token,
            "user", Map.of("id", a.id(), "username", a.username(), "email", a.email(), "role", a.role())
        ));
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) { throw new IllegalStateException("SHA-256 unavailable", ex); }
    }

    private record LoginLock(int failures, Instant lockedUntil) {}

    private static final class LoginAttempt {
        private int failures;
        private long lockedUntil;
    }

    private record Account(String id, String username, String email, String role, String passwordHash, boolean emailVerified) {}
}
