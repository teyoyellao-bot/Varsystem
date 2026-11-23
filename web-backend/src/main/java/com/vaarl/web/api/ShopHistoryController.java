package com.vaarl.web.api;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/shop/history")
public class ShopHistoryController {
    private final JdbcTemplate jdbc;
    public ShopHistoryController(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @GetMapping
    public List<Map<String,Object>> history(Authentication auth) {
        UUID shopId = shopId(auth);
        return jdbc.query("""
            SELECT r.id, r.requester_name, r.requester_phone, r.vehicle, r.problem,
                   r.location_text, r.details, r.status, r.payment_method, r.fee, r.travel_fee,
                   r.submitted_at, r.accepted_at, r.completed_at, m.mechanic_name
            FROM web_assistance_requests r
            LEFT JOIN web_mechanics m ON m.id = r.mechanic_id
            WHERE r.shop_id = ? AND r.status IN ('Completed','Declined')
            ORDER BY COALESCE(r.completed_at, r.submitted_at) DESC
            """, (rs,row)->Map.ofEntries(
                Map.entry("id",rs.getObject("id",UUID.class).toString()),
                Map.entry("customerName",safe(rs.getString("requester_name"))),
                Map.entry("customerPhone",safe(rs.getString("requester_phone"))),
                Map.entry("vehicle",safe(rs.getString("vehicle"))),
                Map.entry("problem",safe(rs.getString("problem"))),
                Map.entry("location",safe(rs.getString("location_text"))),
                Map.entry("details",safe(rs.getString("details"))),
                Map.entry("status",safe(rs.getString("status"))),
                Map.entry("paymentMethod",safe(rs.getString("payment_method"))),
                Map.entry("fee",rs.getBigDecimal("fee")==null?java.math.BigDecimal.ZERO:rs.getBigDecimal("fee")),
                Map.entry("travelFee",rs.getBigDecimal("travel_fee")==null?java.math.BigDecimal.ZERO:rs.getBigDecimal("travel_fee")),
                Map.entry("mechanicName",safe(rs.getString("mechanic_name"))),
                Map.entry("submittedAt",rs.getTimestamp("submitted_at")==null?"":rs.getTimestamp("submitted_at").toString()),
                Map.entry("acceptedAt",rs.getTimestamp("accepted_at")==null?"":rs.getTimestamp("accepted_at").toString()),
                Map.entry("completedAt",rs.getTimestamp("completed_at")==null?"":rs.getTimestamp("completed_at").toString())
            ), shopId);
    }

    private UUID shopId(Authentication auth) {
        List<UUID> rows = jdbc.query("""
            SELECT s.id FROM web_repair_shops s JOIN web_accounts a ON a.id=s.account_id
            WHERE lower(a.username)=lower(?) AND upper(a.account_role)='REPAIR_SHOP'
            """, (rs,row)->rs.getObject("id",UUID.class), auth.getName());
        if(rows.isEmpty()) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Repair shop account is not linked to a shop.");
        return rows.get(0);
    }
    private static String safe(String value){return value==null?"":value;}
}
