package com.vaarl.web.api;

import java.util.Map;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import com.vaarl.web.auth.LoginRequest;
import com.vaarl.web.auth.RegisterRequest;
import com.vaarl.web.auth.JwtService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthController(JdbcTemplate jdbc, PasswordEncoder encoder, JwtService jwt) {
        this.jdbc = jdbc;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        String role = request.role() == null ? "" : request.role().trim().toUpperCase();
        if (!"VEHICLE_OWNER".equals(role) && !"REPAIR_SHOP".equals(role)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid account type."));
        }
        if (!request.password().equals(request.confirmPassword())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Passwords do not match."));
        }
        if ("REPAIR_SHOP".equals(role) && (request.shopName() == null || request.shopName().isBlank())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Shop name is required."));
        }
        try {
            jdbc.update("""
                INSERT INTO web_accounts
                (id, full_name, email, username, phone, account_role, shop_name, password_hash)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                UUID accountId = UUID.randomUUID();
            jdbc.update("""
                INSERT INTO web_accounts
                (id, full_name, email, username, phone, account_role, shop_name, password_hash)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                accountId, request.fullName().trim(), request.email().trim().toLowerCase(),
                request.username().trim(), request.phone().trim(), role,
                "REPAIR_SHOP".equals(role) ? request.shopName().trim() : null,
                encoder.encode(request.password()));
            if ("REPAIR_SHOP".equals(role)) {
                jdbc.update("""
                    INSERT INTO web_repair_shops (id, account_id, shop_name, location_text, phone, status)
                    VALUES (?, ?, ?, ?, ?, 'Active')
                    """, UUID.randomUUID(), accountId, request.shopName().trim(), "", request.phone().trim());
            }
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "Registration successful."));
        } catch (DuplicateKeyException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "Email or username is already registered."));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
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
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Invalid username/email or password."));
        }
        Account a = rows.get(0);
        String token = jwt.createToken(a.id(), a.role(), a.username());
        return ResponseEntity.ok(Map.of(
            "token", token,
            "user", Map.of("id", a.id(), "username", a.username(), "email", a.email(), "role", a.role())
        ));
    }

    private record Account(String id, String username, String email, String role, String passwordHash) {}
}
