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
@RequestMapping("/api/support-requests")
public class SupportRequestsController {
    private final JdbcTemplate jdbc;
    public SupportRequestsController(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String,Object> submit(@RequestBody Payload body, Authentication auth) {
        UUID accountId = accountId(auth);
        if (body.subject()==null || body.subject().isBlank() || body.message()==null || body.message().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Subject and message are required.");
        Map<String,Object> account = jdbc.queryForMap("SELECT full_name,email FROM web_accounts WHERE id=?", accountId);
        UUID id = UUID.randomUUID();
        jdbc.update("""
            INSERT INTO web_support_requests(id,account_id,requester_name,email,subject,message,status)
            VALUES(?,?,?,?,?,?, 'Pending')
            """, id, accountId, safe(body.name(), (String)account.get("full_name")),
                safe(body.email(), (String)account.get("email")), body.subject().trim(), body.message().trim());
        return Map.of("id",id.toString(),"message","Support request submitted.");
    }

    @GetMapping
    public List<Map<String,Object>> list(Authentication auth) {
        requireAdmin(auth);
        return jdbc.query("""
            SELECT id, requester_name, email, subject, message, status, submitted_at, resolved_at
            FROM web_support_requests ORDER BY submitted_at DESC
            """, (rs,row)->Map.of(
            "id",rs.getObject("id",UUID.class).toString(),
            "name",safe(rs.getString("requester_name")),
            "email",safe(rs.getString("email")),
            "subject",safe(rs.getString("subject")),
            "message",safe(rs.getString("message")),
            "status",safe(rs.getString("status")),
            "submittedAt",rs.getTimestamp("submitted_at")==null?"":rs.getTimestamp("submitted_at").toString(),
            "resolvedAt",rs.getTimestamp("resolved_at")==null?"":rs.getTimestamp("resolved_at").toString()
        ));
    }

    @PatchMapping("/{id}/status")
    public Map<String,Object> updateStatus(@PathVariable UUID id, @RequestBody StatusPayload body, Authentication auth) {
        requireAdmin(auth);
        String status = body.status()==null ? "" : body.status().trim();
        if (!List.of("Pending","In Progress","Resolved").contains(status))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid support status.");
        int changed=jdbc.update("""
            UPDATE web_support_requests SET status=?, resolved_at=CASE WHEN ?='Resolved' THEN CURRENT_TIMESTAMP ELSE NULL END
            WHERE id=?
            """, status, status, id);
        if(changed==0) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Support request not found.");
        return Map.of("message","Support request updated.","status",status);
    }

    private UUID accountId(Authentication auth) {
        if(auth==null || auth.getName()==null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Authentication required.");
        List<UUID> ids=jdbc.query("SELECT id FROM web_accounts WHERE lower(username)=lower(?)",
            (rs,row)->rs.getObject("id",UUID.class),auth.getName());
        if(ids.isEmpty()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Account not found.");
        return ids.get(0);
    }
    private void requireAdmin(Authentication auth) {
        if(auth==null || auth.getAuthorities().stream().noneMatch(a -> "ROLE_ADMIN".equals(a.getAuthority())))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Admin access required.");
    }
    private static String safe(String v){return v==null?"":v;}
    private static String safe(String v,String fallback){return v==null||v.isBlank()?fallback:v.trim();}
    public record Payload(String name,String email,String subject,String message){}
    public record StatusPayload(String status){}
}