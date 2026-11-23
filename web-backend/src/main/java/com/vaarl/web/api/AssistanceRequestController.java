package com.vaarl.web.api;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;


@RestController
@RequestMapping("/api/assistance-requests")
public class AssistanceRequestController {
    private final JdbcTemplate jdbc;
    public AssistanceRequestController(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @GetMapping
    public List<Map<String, Object>> myRequests(Authentication authentication) {
        Account account = ownerAccount(authentication);
        return jdbc.query("""
            SELECT r.id, s.shop_name, r.mechanic_id, m.mechanic_name, r.vehicle, r.problem,
                   r.location_text, r.details, r.status, r.payment_method, r.fee, r.travel_fee,
                   r.submitted_at, r.accepted_at, r.completed_at
            FROM web_assistance_requests r
            JOIN web_repair_shops s ON s.id = r.shop_id
            LEFT JOIN web_mechanics m ON m.id = r.mechanic_id
            WHERE r.requester_account_id = ?
            ORDER BY r.submitted_at DESC
            """, (rs, row) -> Map.ofEntries(
                Map.entry("id", rs.getObject("id", UUID.class).toString()),
                Map.entry("shopName", rs.getString("shop_name") == null ? "" : rs.getString("shop_name")),
                Map.entry("mechanicId", rs.getString("mechanic_id") == null ? "" : rs.getString("mechanic_id")),
                Map.entry("mechanicName", rs.getString("mechanic_name") == null ? "" : rs.getString("mechanic_name")),
                Map.entry("vehicle", rs.getString("vehicle") == null ? "" : rs.getString("vehicle")),
                Map.entry("problem", rs.getString("problem") == null ? "" : rs.getString("problem")),
                Map.entry("location", rs.getString("location_text") == null ? "" : rs.getString("location_text")),
                Map.entry("details", rs.getString("details") == null ? "" : rs.getString("details")),
                Map.entry("status", rs.getString("status") == null ? "" : rs.getString("status")),
                Map.entry("paymentMethod", rs.getString("payment_method") == null ? "" : rs.getString("payment_method")),
                Map.entry("fee", rs.getBigDecimal("fee") == null ? java.math.BigDecimal.ZERO : rs.getBigDecimal("fee")),
                Map.entry("travelFee", rs.getBigDecimal("travel_fee") == null ? java.math.BigDecimal.ZERO : rs.getBigDecimal("travel_fee")),
                Map.entry("submittedAt", rs.getTimestamp("submitted_at") == null ? "" : rs.getTimestamp("submitted_at").toString()),
                Map.entry("acceptedAt", rs.getTimestamp("accepted_at") == null ? "" : rs.getTimestamp("accepted_at").toString()),
                Map.entry("completedAt", rs.getTimestamp("completed_at") == null ? "" : rs.getTimestamp("completed_at").toString())
            ), account.id());
    }


    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public Map<String, Object> createRequest(@RequestBody RequestPayload body, Authentication authentication) {
        Account account = ownerAccount(authentication);
        if (body.shopId() == null || body.shopId().isBlank() || body.vehicle() == null || body.vehicle().isBlank()
                || body.problem() == null || body.problem().isBlank() || body.location() == null || body.location().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Shop, vehicle, problem, and location are required.");
        UUID shopId;
        try { shopId = UUID.fromString(body.shopId()); }
        catch (IllegalArgumentException ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid shop."); }
        Integer shopCount = jdbc.queryForObject("SELECT count(*) FROM web_repair_shops WHERE id = ? AND lower(status) = 'active'", Integer.class, shopId);
        if (shopCount == null || shopCount == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Active shop not found.");

        UUID mechanicId = null;
        if (body.mechanicId() != null && !body.mechanicId().isBlank()) {
            try { mechanicId = UUID.fromString(body.mechanicId()); }
            catch (IllegalArgumentException ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid mechanic."); }
            Integer mechanicCount = jdbc.queryForObject("SELECT count(*) FROM web_mechanics WHERE id = ? AND shop_id = ? AND lower(availability) = 'available'", Integer.class, mechanicId, shopId);
            if (mechanicCount == null || mechanicCount == 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected mechanic is no longer available.");
        }

        UUID requestId = UUID.randomUUID();
        jdbc.update("""
            INSERT INTO web_assistance_requests
            (id, requester_account_id, shop_id, mechanic_id, requester_name, requester_phone,
             vehicle, problem, location_text, details, status, payment_method)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'Pending', ?)
            """, requestId, account.id(), shopId, mechanicId, account.fullName(), account.phone(),
            body.vehicle().trim(), body.problem().trim(), body.location().trim(),
            body.details() == null ? "" : body.details().trim(),
            body.paymentMethod() == null || body.paymentMethod().isBlank() ? "Cash / GCash" : body.paymentMethod().trim());
        List<UUID> shopAccounts = jdbc.query("SELECT account_id FROM web_repair_shops WHERE id = ? AND account_id IS NOT NULL",
            (rs, row) -> rs.getObject("account_id", UUID.class), shopId);
        if (!shopAccounts.isEmpty()) {
            jdbc.update("""
                INSERT INTO web_notifications (id, account_id, type, title, message, related_request_id)
                VALUES (?, ?, 'NEW_REQUEST', 'New Assistance Request', ?, ?)
                """, UUID.randomUUID(), shopAccounts.get(0),
                "You received a new vehicle assistance request.", requestId);
        }
        return Map.of("message", "Assistance request submitted.", "requestId", requestId.toString(), "status", "Pending");
    }


    private Account ownerAccount(Authentication authentication) {
        List<Account> rows = jdbc.query("""
            SELECT id, full_name, username, phone, account_role FROM web_accounts
            WHERE lower(username) = lower(?) LIMIT 1
            """, (rs, row) -> new Account(rs.getObject("id", UUID.class), rs.getString("full_name"),
                rs.getString("username"), rs.getString("phone"), rs.getString("account_role")), authentication.getName());
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account not found.");
        Account account = rows.get(0);
        if (!"VEHICLE_OWNER".equals(account.role())) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only vehicle owner accounts can request assistance.");
        return account;
    }


    private record Account(UUID id, String fullName, String username, String phone, String role) {}
    public record RequestPayload(String shopId, String mechanicId, String vehicle, String problem, String location, String details, String paymentMethod) {}
}
