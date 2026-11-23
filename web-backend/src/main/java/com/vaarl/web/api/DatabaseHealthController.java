package com.vaarl.web.api;

import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health/database")
public class DatabaseHealthController {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseHealthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public Map<String, Object> databaseHealth() {
        try {
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            boolean connected = result != null && result == 1;

            return Map.of(
                "status", connected ? "ok" : "error",
                "database", "PostgreSQL",
                "connected", connected
            );
        } catch (Exception ex) {
            return Map.of(
                "status", "error",
                "database", "PostgreSQL",
                "connected", false,
                "message", "Database connection is unavailable"
            );
        }
    }
}
