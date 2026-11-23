package com.vaarl.web.api;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class PasswordResetController {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    private final JavaMailSender mailSender;
    private final String resetUrl;
    private final String fromAddress;

    public PasswordResetController(JdbcTemplate jdbc, PasswordEncoder encoder, JavaMailSender mailSender,
            @Value("${vaarl.password-reset-url:http://localhost:5173/reset-password}") String resetUrl,
            @Value("${spring.mail.username:}") String fromAddress) {
        this.jdbc = jdbc;
        this.encoder = encoder;
        this.mailSender = mailSender;
        this.resetUrl = resetUrl;
        this.fromAddress = fromAddress;
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
                try {
                    SimpleMailMessage mail = new SimpleMailMessage();
                    mail.setTo(email);
                    if (StringUtils.hasText(fromAddress)) mail.setFrom(fromAddress);
                    mail.setSubject("VAARL Password Reset");
                    mail.setText("We received a request to reset your VAARL password.\n\nUse this link within 30 minutes:\n"
                        + resetUrl + (resetUrl.contains("?") ? "&" : "?") + "token=" + token
                        + "\n\nIf you did not request this, you can ignore this email.");
                    mailSender.send(mail);
                } catch (MailException ex) {
                    // Keep the response generic to avoid exposing account existence.
                }
            }
        }
        return Map.of("message","If an account matches that email, a password reset link will be sent.");
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
