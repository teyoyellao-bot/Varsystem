package com.vaarl.web.api;

import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/audit-logs")
public class AdminAuditLogsController {
    private final JdbcTemplate jdbc;

    public AdminAuditLogsController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping
    public Map<String, Object> list(Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String eventType) {
        if (authentication == null || authentication.getAuthorities().stream()
                .noneMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required.");
        }
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(200, size));
        String filter = eventType == null ? "" : eventType.trim();
        List<Map<String, Object>> rows;
        if (filter.isEmpty()) {
            rows = jdbc.queryForList("""
                SELECT id, account_id, actor_username, event_type, details, occurred_at
                FROM web_audit_logs ORDER BY occurred_at DESC, id DESC LIMIT ? OFFSET ?
                """, safeSize, (long) safePage * safeSize);
        } else {
            rows = jdbc.queryForList("""
                SELECT id, account_id, actor_username, event_type, details, occurred_at
                FROM web_audit_logs WHERE event_type = ?
                ORDER BY occurred_at DESC, id DESC LIMIT ? OFFSET ?
                """, filter, safeSize, (long) safePage * safeSize);
        }
        long total = filter.isEmpty()
            ? jdbc.queryForObject("SELECT COUNT(*) FROM web_audit_logs", Long.class)
            : jdbc.queryForObject("SELECT COUNT(*) FROM web_audit_logs WHERE event_type=?", Long.class, filter);
        return Map.of("page", safePage, "size", safeSize, "total", total, "logs", rows);
    }
}
