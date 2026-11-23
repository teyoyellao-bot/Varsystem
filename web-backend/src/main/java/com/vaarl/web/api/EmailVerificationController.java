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
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/email")
public class EmailVerificationController {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final JdbcTemplate jdbc;
    private final JavaMailSender mail;
    private final String verifyUrl;

    public EmailVerificationController(JdbcTemplate jdbc, JavaMailSender mail,
            @Value("${vaarl.email-verification-url}") String verifyUrl) {
        this.jdbc = jdbc;
        this.mail = mail;
        this.verifyUrl = verifyUrl;
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
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Verify your VAARL email");
        message.setText("Verify your email using this link (expires in 24 hours):\n\n"
            + verifyUrl + "?token=" + raw);
        mail.send(message);
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
            try { sendToken(accounts.get(0).id(), accounts.get(0).email()); }
            catch (MailException ignored) { /* keep response generic */ }
        }
        return ResponseEntity.ok(Map.of("message", generic));
    }

    private record Token(String id, String accountId) {}
    private record Account(String id, String email) {}
}
