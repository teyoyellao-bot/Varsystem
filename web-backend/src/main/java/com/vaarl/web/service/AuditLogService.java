package com.vaarl.web.service;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class AuditLogService {
    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);
    private final JdbcTemplate jdbc;

    public AuditLogService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void record(UUID accountId, String username, String eventType, String details) {
        try {
            jdbc.update("""
                INSERT INTO web_audit_logs(account_id, actor_username, event_type, details)
                VALUES (?, ?, ?, ?)
                """, accountId, safe(username, 255), safe(eventType, 80), safe(details, 500));
        } catch (RuntimeException ex) {
            // Audit failures must not interrupt login, registration, or recovery flows.
            log.warn("Could not persist audit event type {}", eventType);
        }
    }

    private static String safe(String value, int max) {
        if (value == null) return null;
        String clean = value.replaceAll("[\\r\\n\\t]", " ").trim();
        return clean.length() > max ? clean.substring(0, max) : clean;
    }
}
