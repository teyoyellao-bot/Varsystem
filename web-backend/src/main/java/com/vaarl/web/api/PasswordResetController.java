package com.vaarl.web.api;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Base64;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaarl.web.service.AuditLogService;

@RestController
@RequestMapping("/api/auth")
public class PasswordResetController {
    private static final Logger log = LoggerFactory.getLogger(PasswordResetController.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    private final ObjectMapper objectMapper;
    private final String resetUrl;
    private final String resendApiKey;
    private final String fromAddress;
    private final AuditLogService audit;

    public PasswordResetController(JdbcTemplate jdbc, PasswordEncoder encoder, ObjectMapper objectMapper,
            @Value("${vaarl.password-reset-url:http://localhost:5173/reset-password}") String resetUrl,
            @Value("${vaarl.resend-api-key:}") String resendApiKey,
            @Value("${vaarl.email-from:onboarding@resend.dev}") String fromAddress,
            AuditLogService audit) {
        this.jdbc = jdbc;
        this.encoder = encoder;
        this.objectMapper = objectMapper;
        this.resetUrl = resetUrl;
        this.resendApiKey = resendApiKey;
        this.fromAddress = fromAddress;
        this.audit = audit;
    }

    @PostMapping("/forgot-password")
    public Map<String,String> requestReset(@RequestBody ForgotPasswordBody body) {
        String email = body == null || body.email() == null ? "" : body.email().trim().toLowerCase();
        if (StringUtils.hasText(email) && email.length() <= 255) {
            List<UUID> accounts = jdbc.query("""
                SELECT id FROM web_accounts WHERE lower(email)=lower(?) LIMIT 1
                """, (rs,row)->rs.getObject("id",UUID.class), email);
            if (!accounts.isEmpty()) {
                UUID accountId = accounts.get(0);
                byte[] randomBytes = new byte[32];
                RANDOM.nextBytes(randomBytes);
                String token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
                String tokenHash = sha256(token);
                jdbc.update("UPDATE web_password_reset_tokens SET used_at=CURRENT_TIMESTAMP WHERE account_id=? AND used_at IS NULL",accountId);
                jdbc.update("""
                    INSERT INTO web_password_reset_tokens(id,account_id,token_hash,expires_at)
                    VALUES(?,?,?,?)
                    """,UUID.randomUUID(),accountId,tokenHash,LocalDateTime.now().plusMinutes(30));
                audit.record(accountId, null, "PASSWORD_RESET_REQUESTED", "Password reset request received.");
                sendResetEmail(email, resetUrl + (resetUrl.contains("?") ? "&" : "?") + "token=" + token);
            }
        }
        return Map.of("message","If an account matches that email, a password reset link will be sent.");
    }

    private void sendResetEmail(String recipient, String resetLink) {
        if (!StringUtils.hasText(resendApiKey) || !StringUtils.hasText(fromAddress)) {
            log.warn("Password reset email not sent: Resend API key or sender address is not configured.");
            return;
        }
        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                "from", fromAddress,
                "to", recipient,
                "subject", "VAARL Password Reset",
                "html", "<p>We received a request to reset your VAARL password.</p>"
                    + "<p>This link expires in 30 minutes:</p><p><a href=\"" + resetLink
                    + "\">Reset Password</a></p><p>If you did not request this, you can ignore this email.</p>"
            ));
            HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.resend.com/emails"))
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "Bearer " + resendApiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                .build();
            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("Resend accepted password reset email (HTTP {}).", response.statusCode());
            } else {
                log.warn("Resend rejected password reset email (HTTP {}): {}", response.statusCode(), response.body());
            }
        } catch (Exception ex) {
            log.warn("Password reset email request failed: {}", ex.toString());
        }
    }

    @PostMapping("/reset-password")
    @Transactional
    public Map<String,String> resetPassword(@RequestBody ResetPasswordBody body) {
        if (body == null || !StringUtils.hasText(body.token()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Reset token is required.");
        if (body.password()==null || body.password().length()<8 || body.password().length()>72)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Password must be between 8 and 72 characters.");
        if (!body.password().equals(body.confirmPassword()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Passwords do not match.");
        String hash=sha256(body.token().trim());
        List<ResetToken> tokens=jdbc.query("""
            SELECT id,account_id FROM web_password_reset_tokens
            WHERE token_hash=? AND used_at IS NULL AND expires_at>CURRENT_TIMESTAMP
            FOR UPDATE
            """,(rs,row)->new ResetToken(rs.getObject("id",UUID.class),rs.getObject("account_id",UUID.class)),hash);
        if(tokens.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Reset link is invalid or expired.");
        ResetToken token=tokens.get(0);
        jdbc.update("UPDATE web_accounts SET password_hash=? WHERE id=?",encoder.encode(body.password()),token.accountId());
        jdbc.update("UPDATE web_password_reset_tokens SET used_at=CURRENT_TIMESTAMP WHERE id=?",token.id());
        jdbc.update("UPDATE web_password_reset_tokens SET used_at=CURRENT_TIMESTAMP WHERE account_id=? AND used_at IS NULL",token.accountId());
        audit.record(token.accountId(), null, "PASSWORD_RESET_COMPLETED", "Account password was reset successfully.");
        return Map.of("message","Password reset successfully. You may now log in.");
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to hash reset token.",ex);
        }
    }
    private record ResetToken(UUID id,UUID accountId){}
    public record ForgotPasswordBody(String email){}
    public record ResetPasswordBody(String token,String password,String confirmPassword){}
}
