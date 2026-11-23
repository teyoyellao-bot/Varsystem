package com.vaarl.web.api;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.Locale;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import org.springframework.util.StringUtils;
import com.vaarl.web.auth.LoginRequest;
import com.vaarl.web.auth.RegisterRequest;
import com.vaarl.web.auth.JwtService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private static final int MAX_LOGIN_FAILURES = 5;
    private static final long LOCK_DURATION_MS = 15 * 60 * 1000L;
    private final ConcurrentMap<String, LoginAttempt> loginAttempts = new ConcurrentHashMap<>();

    public AuthController(JdbcTemplate jdbc, PasswordEncoder encoder, JwtService jwt) {
        this.jdbc = jdbc;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    @PostMapping("/register")
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
                (id, full_name, email, username, phone, account_role, shop_name, password_hash)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
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
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "Registration successful."));
        } catch (DuplicateKeyException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "Email or username is already registered."));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        String loginKey = request.usernameOrEmail().trim().toLowerCase(Locale.ROOT);
        LoginAttempt attempt = loginAttempts.computeIfAbsent(loginKey, key -> new LoginAttempt());
        synchronized (attempt) {
            long now = System.currentTimeMillis();
            if (attempt.lockedUntil > now) {
                long remainingSeconds = Math.max(1, (attempt.lockedUntil - now + 999) / 1000);
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("message", "Too many failed login attempts. Please try again later.", "retryAfterSeconds", remainingSeconds));
            }
            if (attempt.lockedUntil != 0 && attempt.lockedUntil <= now) {
                attempt.failures = 0;
                attempt.lockedUntil = 0;
            }
        }
        var rows = jdbc.query("""
            SELECT id, username, email, account_role, password_hash
            FROM web_accounts
            WHERE lower(username) = lower(?) OR lower(email) = lower(?)
            LIMIT 1
            """,
            (rs, rowNum) -> new Account(
                rs.getString("id"), rs.getString("username"), rs.getString("email"),
                rs.getString("account_role"), rs.getString("password_hash")),
            request.usernameOrEmail().trim(), request.usernameOrEmail().trim());
        if (rows.isEmpty() || !encoder.matches(request.password(), rows.get(0).passwordHash())) {
            synchronized (attempt) {
                attempt.failures++;
                if (attempt.failures >= MAX_LOGIN_FAILURES) {
                    attempt.lockedUntil = System.currentTimeMillis() + LOCK_DURATION_MS;
                    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(Map.of("message", "Too many failed login attempts. Please try again in 15 minutes."));
                }
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Invalid username/email or password."));
        }
        synchronized (attempt) {
            attempt.failures = 0;
            attempt.lockedUntil = 0;
        }
        loginAttempts.remove(loginKey, attempt);
        Account a = rows.get(0);
        String token = jwt.createToken(a.id(), a.role(), a.username());
        return ResponseEntity.ok(Map.of(
            "token", token,
            "user", Map.of("id", a.id(), "username", a.username(), "email", a.email(), "role", a.role())
        ));
    }

    private static final class LoginAttempt {
        private int failures;
        private long lockedUntil;
    }

    private record Account(String id, String username, String email, String role, String passwordHash) {}
}
