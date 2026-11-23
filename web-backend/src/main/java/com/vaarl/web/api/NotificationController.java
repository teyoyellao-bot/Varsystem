package com.vaarl.web.api;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final JdbcTemplate jdbc;
    public NotificationController(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @GetMapping
    public List<Map<String,Object>> list(Authentication auth) {
        UUID accountId = accountId(auth);
        return jdbc.query("""
            SELECT id, type, title, message, related_request_id, is_read, created_at
            FROM web_notifications WHERE account_id=?
            ORDER BY created_at DESC LIMIT 100
            """, (rs,row)->Map.of(
            "id",rs.getObject("id",UUID.class).toString(),
            "type",safe(rs.getString("type")),
            "title",safe(rs.getString("title")),
            "message",safe(rs.getString("message")),
            "requestId",rs.getString("related_request_id")==null?"":rs.getString("related_request_id"),
            "read",rs.getBoolean("is_read"),
            "createdAt",rs.getTimestamp("created_at")==null?"":rs.getTimestamp("created_at").toString()
        ), accountId);
    }

    @GetMapping("/unread-count")
    public Map<String,Object> unreadCount(Authentication auth) {
        UUID accountId = accountId(auth);
        Integer count=jdbc.queryForObject("SELECT count(*) FROM web_notifications WHERE account_id=? AND is_read=false",Integer.class,accountId);
        return Map.of("count",count==null?0:count);
    }

    @PatchMapping("/{notificationId}/read")
    public Map<String,Object> markRead(@PathVariable UUID notificationId, Authentication auth) {
        UUID accountId=accountId(auth);
        int updated=jdbc.update("UPDATE web_notifications SET is_read=true WHERE id=? AND account_id=?",notificationId,accountId);
        if(updated==0) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Notification not found.");
        return Map.of("message","Notification marked as read.");
    }

    @PatchMapping("/read-all")
    public Map<String,Object> markAllRead(Authentication auth) {
        UUID accountId=accountId(auth);
        int updated=jdbc.update("UPDATE web_notifications SET is_read=true WHERE account_id=? AND is_read=false",accountId);
        return Map.of("message","Notifications marked as read.","updated",updated);
    }

    private UUID accountId(Authentication auth) {
        if(auth==null || auth.getName()==null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Authentication required.");
        List<UUID> ids=jdbc.query("SELECT id FROM web_accounts WHERE lower(username)=lower(?)",
            (rs,row)->rs.getObject("id",UUID.class),auth.getName());
        if(ids.isEmpty()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Account not found.");
        return ids.get(0);
    }
    private static String safe(String v){return v==null?"":v;}
}
