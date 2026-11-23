package com.vaarl.web.service;

import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Optional, environment-controlled bootstrap for the first web Admin account. */
@Component
public class AdminBootstrapService implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapService.class);
    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    private final String fullName, email, username, password;

    public AdminBootstrapService(JdbcTemplate jdbc, PasswordEncoder encoder,
            @Value("${VAARL_ADMIN_FULL_NAME:}") String fullName,
            @Value("${VAARL_ADMIN_EMAIL:}") String email,
            @Value("${VAARL_ADMIN_USERNAME:}") String username,
            @Value("${VAARL_ADMIN_PASSWORD:}") String password) {
        this.jdbc = jdbc;
        this.encoder = encoder;
        this.fullName = fullName == null ? "" : fullName.trim();
        this.email = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        this.username = username == null ? "" : username.trim();
        this.password = password == null ? "" : password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (fullName.isBlank() || email.isBlank() || username.isBlank() || password.length() < 12) {
            log.info("Admin bootstrap skipped. Set VAARL_ADMIN_FULL_NAME, VAARL_ADMIN_EMAIL, VAARL_ADMIN_USERNAME and a 12+ character VAARL_ADMIN_PASSWORD to enable.");
            return;
        }
        List<AccountRow> matches = jdbc.query("""
            SELECT id, username, email FROM web_accounts
            WHERE lower(email)=lower(?) OR lower(username)=lower(?)
            """, (rs, row) -> new AccountRow(rs.getString("id"), rs.getString("username"), rs.getString("email")), email, username);
        if (matches.size() > 1) {
            log.error("Admin bootstrap stopped: configured email and username match different accounts. Resolve this conflict manually.");
            return;
        }
        String hash = encoder.encode(password);
        if (matches.isEmpty()) {
            jdbc.update("""
                INSERT INTO web_accounts (id, full_name, email, username, phone, account_role, shop_name, password_hash, email_verified)
                VALUES (gen_random_uuid(), ?, ?, ?, '', 'ADMIN', NULL, ?, true)
                """, fullName, email, username, hash);
            log.info("Bootstrap Admin account created.");
        } else {
            AccountRow row = matches.get(0);
            if (!row.email().equalsIgnoreCase(email) || !row.username().equalsIgnoreCase(username)) {
                log.error("Admin bootstrap stopped: matched account email/username differs from configured pair. No changes made.");
                return;
            }
            jdbc.update("UPDATE web_accounts SET account_role='ADMIN', email_verified=true, password_hash=? WHERE id=?", hash, row.id());
            log.info("Configured existing account as Admin and refreshed bootstrap password.");
        }
        jdbc.update("DELETE FROM web_login_attempts WHERE lower(login_key)=lower(?) OR lower(login_key)=lower(?)", email, username);
    }

    private record AccountRow(String id, String username, String email) {}
}
