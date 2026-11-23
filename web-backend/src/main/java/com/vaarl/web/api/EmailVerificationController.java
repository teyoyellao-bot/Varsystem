package com.vaarl.web.api;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.nio.charset.StandardCharsets;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import com.vaarl.web.service.AuditLogService;

@RestController
@RequestMapping("/api/auth/email")
public class EmailVerificationController {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Logger log = LoggerFactory.getLogger(EmailVerificationController.class);
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final String resendApiKey;
    private final String emailFrom;
    private final String verifyUrl;
    private final AuditLogService audit;

    public EmailVerificationController(JdbcTemplate jdbc, ObjectMapper objectMapper,
            @Value("${vaarl.resend-api-key:}") String resendApiKey,
            @Value("${vaarl.email-from:onboarding@resend.dev}") String emailFrom,
            @Value("${vaarl.email-verification-url}") String verifyUrl, AuditLogService audit) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
        this.resendApiKey = resendApiKey;
        this.emailFrom = emailFrom;
        this.verifyUrl = verifyUrl;
        this.audit = audit;
    }

    private static String token() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private static String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }

    private void sendToken(String accountId, String email) {
        String raw = token();
        jdbc.update("""
            INSERT INTO web_email_verification_tokens(id, account_id, token_hash, expires_at)
            VALUES (?, ?, ?, ?)
            """, UUID.randomUUID(), accountId, hash(raw), Instant.now().plusSeconds(24 * 60 * 60));
        if (!StringUtils.hasText(resendApiKey) || !StringUtils.hasText(emailFrom)) {
            throw new IllegalStateException("Resend email settings are missing.");
        }
        String link = verifyUrl + "?token=" + raw;
        String payload = objectMapper.writeValueAsString(Map.of(
            "from", emailFrom,
            "to", email,
            "subject", "Verify your VAARL email",
            "html", "<p>Verify your email using the link below. This link expires in 24 hours.</p><p><a href=\"" + link + "\\">Verify Email</a></p>"
        ));
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.resend.com/emails"))
            .timeout(Duration.ofSeconds(20))
            .header("Authorization", "Bearer " + resendApiKey)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
            .build();
        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            log.warn("Resend rejected verification email (HTTP {}): {}", response.statusCode(), response.body());
            throw new IllegalStateException("Resend API rejected the email request.");
        }
        log.info("Resend accepted verification email (HTTP {}).", response.statusCode());
    }

    @GetMapping("/verify")
    @Transactional
    public ResponseEntity<?> verify(@RequestParam String token) {
        if (token == null || token.length() != 64) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid or expired verification link."));
        }
        var rows = jdbc.query("""
            SELECT id, account_id FROM web_email_verification_tokens
            WHERE token_hash=? AND expires_at > CURRENT_TIMESTAMP AND used_at IS NULL
            FOR UPDATE
            """, (rs, n) -> new Token(rs.getString("id"), rs.getString("account_id")), hash(token));
        if (rows.isEmpty()) return ResponseEntity.badRequest().body(Map.of("message", "Invalid or expired verification link."));
        Token t = rows.get(0);
        jdbc.update("UPDATE web_accounts SET email_verified=true WHERE id=?", t.accountId());
        jdbc.update("UPDATE web_email_verification_tokens SET used_at=CURRENT_TIMESTAMP WHERE id=?", t.id());
        jdbc.update("UPDATE web_email_verification_tokens SET used_at=CURRENT_TIMESTAMP WHERE account_id=? AND used_at IS NULL", t.accountId());
        audit.record(UUID.fromString(t.accountId()), null, "EMAIL_VERIFIED", "Account email verification completed.");
        return ResponseEntity.ok(Map.of("message", "Email verified. You may sign in."));
    }

    @PostMapping("/resend")
    public ResponseEntity<?> resend(@RequestBody Map<String,String> body) {
        String email = body == null || body.get("email") == null ? "" : body.get("email").trim().toLowerCase();
        String generic = "If the account exists and needs verification, a verification email will be sent.";
        if (!StringUtils.hasText(email)) return ResponseEntity.ok(Map.of("message", generic));
        List<Account> accounts = jdbc.query("""
            SELECT id, email FROM web_accounts WHERE lower(email)=lower(?) AND email_verified=false LIMIT 1
            """, (rs, n) -> new Account(rs.getString("id"), rs.getString("email")), email);
        if (!accounts.isEmpty()) {
            try { sendToken(accounts.get(0).id(), accounts.get(0).email()); audit.record(UUID.fromString(accounts.get(0).id()), null, "VERIFICATION_EMAIL_RESENT", "Verification email resent."); }
            catch (Exception ex) { log.warn("Verification resend failed: {}", ex.toString()); }
        }
        return ResponseEntity.ok(Map.of("message", generic));
    }

    private record Token(String id, String accountId) {}
    private record Account(String id, String email) {}
}
