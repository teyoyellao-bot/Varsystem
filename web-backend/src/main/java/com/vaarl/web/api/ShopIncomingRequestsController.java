package com.vaarl.web.api;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/shop/requests")
public class ShopIncomingRequestsController {
    private final JdbcTemplate jdbc;
    public ShopIncomingRequestsController(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @GetMapping
    public List<Map<String, Object>> incoming(Authentication authentication) {
        UUID shopId = currentShop(authentication);
        return jdbc.query("""
            SELECT r.id, r.requester_name, r.requester_phone, r.vehicle, r.problem,
                   r.location_text, r.details, r.status, r.payment_method, r.fee, r.travel_fee,
                   r.submitted_at, r.accepted_at, r.completed_at,
                   m.mechanic_name
            FROM web_assistance_requests r
            LEFT JOIN web_mechanics m ON m.id = r.mechanic_id
            WHERE r.shop_id = ?
            ORDER BY r.submitted_at DESC
            """, (rs, row) -> Map.ofEntries(
                Map.entry("id", rs.getObject("id", UUID.class).toString()),
                Map.entry("customerName", safe(rs.getString("requester_name"))),
                Map.entry("customerPhone", safe(rs.getString("requester_phone"))),
                Map.entry("vehicle", safe(rs.getString("vehicle"))),
                Map.entry("problem", safe(rs.getString("problem"))),
                Map.entry("location", safe(rs.getString("location_text"))),
                Map.entry("details", safe(rs.getString("details"))),
                Map.entry("status", safe(rs.getString("status"))),
                Map.entry("paymentMethod", safe(rs.getString("payment_method"))),
                Map.entry("fee", rs.getBigDecimal("fee") == null ? java.math.BigDecimal.ZERO : rs.getBigDecimal("fee")),
                Map.entry("travelFee", rs.getBigDecimal("travel_fee") == null ? java.math.BigDecimal.ZERO : rs.getBigDecimal("travel_fee")),
                Map.entry("mechanicName", safe(rs.getString("mechanic_name"))),
                Map.entry("submittedAt", rs.getTimestamp("submitted_at") == null ? "" : rs.getTimestamp("submitted_at").toString()),
                Map.entry("acceptedAt", rs.getTimestamp("accepted_at") == null ? "" : rs.getTimestamp("accepted_at").toString()),
                Map.entry("completedAt", rs.getTimestamp("completed_at") == null ? "" : rs.getTimestamp("completed_at").toString())
            ), shopId);
    }

    @PatchMapping("/{requestId}/decision")
    public Map<String, Object> decide(@PathVariable String requestId, @RequestBody Decision body, Authentication authentication) {
        UUID shopId = currentShop(authentication);
        UUID id;
        try { id = UUID.fromString(requestId); }
        catch (IllegalArgumentException ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid request ID."); }
        String decision = body == null || body.decision() == null ? "" : body.decision().trim().toLowerCase();
        if (!decision.equals("accept") && !decision.equals("decline"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Decision must be Accept or Decline.");
        String nextStatus = decision.equals("accept") ? "Accepted" : "Declined";
        int changed = decision.equals("accept")
            ? jdbc.update("UPDATE web_assistance_requests SET status = 'Accepted', accepted_at = CURRENT_TIMESTAMP WHERE id = ? AND shop_id = ? AND status = 'Pending'", id, shopId)
            : jdbc.update("UPDATE web_assistance_requests SET status = 'Declined' WHERE id = ? AND shop_id = ? AND status = 'Pending'", id, shopId);
        if (changed == 0) throw new ResponseStatusException(HttpStatus.CONFLICT, "Request not found, or it is no longer Pending.");
        return Map.of("message", "Request " + nextStatus.toLowerCase() + ".", "status", nextStatus);
    }

    private UUID currentShop(Authentication authentication) {
        List<UUID> shops = jdbc.query("""
            SELECT id FROM web_repair_shops WHERE account_id = ?
            """, (rs, row) -> rs.getObject("id", UUID.class), currentAccountId(authentication));
        if (shops.isEmpty()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This account is not linked to a repair shop.");
        return shops.get(0);
    }

    private UUID currentAccountId(Authentication authentication) {
        List<UUID> accounts = jdbc.query("""
            SELECT id FROM web_accounts WHERE lower(username) = lower(?) AND upper(account_role) = 'REPAIR_SHOP'
            """, (rs, row) -> rs.getObject("id", UUID.class), authentication.getName());
        if (accounts.isEmpty()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Repair shop account not found.");
        return accounts.get(0);
    }

    private static String safe(String value) { return value == null ? "" : value; }
    public record Decision(String decision) {}
}
