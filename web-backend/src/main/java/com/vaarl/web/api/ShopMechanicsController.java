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
@RequestMapping("/api/shop/mechanics")
public class ShopMechanicsController {
    private final JdbcTemplate jdbc;
    public ShopMechanicsController(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @GetMapping
    public List<Map<String, Object>> list(Authentication authentication) {
        UUID shopId = currentShop(authentication);
        return jdbc.query("""
            SELECT id, mechanic_name, phone, availability, created_at
            FROM web_mechanics WHERE shop_id = ? ORDER BY mechanic_name
            """, (rs, row) -> Map.of(
                "id", rs.getObject("id", UUID.class).toString(),
                "name", safe(rs.getString("mechanic_name")),
                "phone", safe(rs.getString("phone")),
                "availability", safe(rs.getString("availability")),
                "createdAt", rs.getTimestamp("created_at") == null ? "" : rs.getTimestamp("created_at").toString()
            ), shopId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> add(@RequestBody MechanicBody body, Authentication authentication) {
        UUID shopId = currentShop(authentication);
        String name = body == null || body.name() == null ? "" : body.name().trim();
        if (name.isBlank() || name.length() > 120) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mechanic name is required (maximum 120 characters).");
        String phone = body.phone() == null ? "" : body.phone().trim();
        String availability = normalizeAvailability(body.availability());
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO web_mechanics (id, shop_id, mechanic_name, phone, availability) VALUES (?, ?, ?, ?, ?)", id, shopId, name, phone, availability);
        return Map.of("id", id.toString(), "name", name, "phone", phone, "availability", availability);
    }

    @PutMapping("/{mechanicId}")
    public Map<String, Object> update(@PathVariable UUID mechanicId, @RequestBody MechanicBody body, Authentication authentication) {
        UUID shopId = currentShop(authentication);
        String name = body == null || body.name() == null ? "" : body.name().trim();
        if (name.isBlank() || name.length() > 120) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mechanic name is required.");
        String phone = body.phone() == null ? "" : body.phone().trim();
        String availability = normalizeAvailability(body.availability());
        int changed = jdbc.update("UPDATE web_mechanics SET mechanic_name = ?, phone = ?, availability = ? WHERE id = ? AND shop_id = ?", name, phone, availability, mechanicId, shopId);
        if (changed == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Mechanic not found.");
        return Map.of("id", mechanicId.toString(), "name", name, "phone", phone, "availability", availability);
    }

    @DeleteMapping("/{mechanicId}")
    public Map<String, Object> remove(@PathVariable UUID mechanicId, Authentication authentication) {
        UUID shopId = currentShop(authentication);
        int changed = jdbc.update("DELETE FROM web_mechanics WHERE id = ? AND shop_id = ?", mechanicId, shopId);
        if (changed == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Mechanic not found.");
        return Map.of("message", "Mechanic removed.");
    }

    private UUID currentShop(Authentication authentication) {
        List<UUID> shops = jdbc.query("""
            SELECT s.id FROM web_repair_shops s
            JOIN web_accounts a ON a.id = s.account_id
            WHERE lower(a.username) = lower(?) AND upper(a.account_role) = 'REPAIR_SHOP'
            """, (rs, row) -> rs.getObject("id", UUID.class), authentication.getName());
        if (shops.isEmpty()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Repair shop account is not linked to a shop.");
        return shops.get(0);
    }
    private static String normalizeAvailability(String raw) {
        String v = raw == null ? "" : raw.trim().toLowerCase();
        if (v.equals("busy")) return "Busy";
        return "Available";
    }
    private static String safe(String s) { return s == null ? "" : s; }
    public record MechanicBody(String name, String phone, String availability) {}
}
