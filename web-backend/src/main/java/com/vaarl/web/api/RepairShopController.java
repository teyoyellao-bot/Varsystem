package com.vaarl.web.api;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shops")
public class RepairShopController {
    private final JdbcTemplate jdbc;

    public RepairShopController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping
    public List<Map<String, Object>> listShops(@RequestParam(required = false, name = "q") String query) {
        String search = query == null ? "" : query.trim().toLowerCase();
        List<Map<String, Object>> shops = jdbc.query("""
            SELECT id, shop_name, location_text, phone, hours_text, about_text,
                   status, rating, review_count
            FROM web_repair_shops
            WHERE lower(status) = 'active'
              AND (? = '' OR lower(shop_name) LIKE ? OR lower(coalesce(location_text,'')) LIKE ?)
            ORDER BY shop_name
            """,
            (rs, row) -> {
                UUID id = rs.getObject("id", UUID.class);
                List<String> services = jdbc.query(
                    "SELECT service_name FROM web_shop_services WHERE shop_id = ? ORDER BY service_name",
                    (serviceRs, serviceRow) -> serviceRs.getString("service_name"), id);
                return Map.of(
                    "id", id.toString(),
                    "shopName", rs.getString("shop_name"),
                    "location", rs.getString("location_text") == null ? "" : rs.getString("location_text"),
                    "phone", rs.getString("phone") == null ? "" : rs.getString("phone"),
                    "hours", rs.getString("hours_text") == null ? "" : rs.getString("hours_text"),
                    "about", rs.getString("about_text") == null ? "" : rs.getString("about_text"),
                    "rating", rs.getBigDecimal("rating") == null ? 0 : rs.getBigDecimal("rating"),
                    "reviewCount", rs.getInt("review_count"),
                    "services", services
                );
            }, search, "%" + search + "%", "%" + search + "%");
        return shops;
    }

    @GetMapping("/{shopId}")
    public Map<String, Object> shopDetails(@PathVariable UUID shopId) {
        List<Map<String, Object>> found = jdbc.query("""
            SELECT id, shop_name, location_text, phone, hours_text, about_text,
                   status, rating, review_count
            FROM web_repair_shops
            WHERE id = ? AND lower(status) = 'active'
            """,
            (rs, row) -> {
                List<String> services = jdbc.query(
                    "SELECT service_name FROM web_shop_services WHERE shop_id = ? ORDER BY service_name",
                    (serviceRs, serviceRow) -> serviceRs.getString("service_name"), shopId);
                List<Map<String, Object>> mechanics = jdbc.query("""
                    SELECT id, mechanic_name, phone, availability
                    FROM web_mechanics
                    WHERE shop_id = ? AND lower(availability) = 'available'
                    ORDER BY mechanic_name
                    """,
                    (mechanicRs, mechanicRow) -> Map.of(
                        "id", mechanicRs.getObject("id", UUID.class).toString(),
                        "name", mechanicRs.getString("mechanic_name"),
                        "phone", mechanicRs.getString("phone") == null ? "" : mechanicRs.getString("phone"),
                        "availability", mechanicRs.getString("availability")), shopId);
                return Map.of(
                    "id", rs.getObject("id", UUID.class).toString(),
                    "shopName", rs.getString("shop_name"),
                    "location", rs.getString("location_text") == null ? "" : rs.getString("location_text"),
                    "phone", rs.getString("phone") == null ? "" : rs.getString("phone"),
                    "hours", rs.getString("hours_text") == null ? "" : rs.getString("hours_text"),
                    "about", rs.getString("about_text") == null ? "" : rs.getString("about_text"),
                    "rating", rs.getBigDecimal("rating") == null ? 0 : rs.getBigDecimal("rating"),
                    "reviewCount", rs.getInt("review_count"),
                    "services", services,
                    "availableMechanics", mechanics
                );
            }, shopId);
        if (found.isEmpty()) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND, "Repair shop not found.");
        }
        return found.get(0);
    }

}
