package com.vaarl.web.api;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/shop/jobs")
public class ShopServiceJobsController {
    private final JdbcTemplate jdbc;
    public ShopServiceJobsController(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @GetMapping
    public List<Map<String,Object>> list(Authentication auth) {
        UUID shopId = shopId(auth);
        return jdbc.query("""
            SELECT r.id, r.requester_name, r.requester_phone, r.vehicle, r.problem,
                   r.location_text, r.details, r.status, r.payment_method, r.fee, r.travel_fee,
                   r.submitted_at, r.accepted_at, r.completed_at, m.id mechanic_id, m.mechanic_name
            FROM web_assistance_requests r LEFT JOIN web_mechanics m ON m.id = r.mechanic_id
            WHERE r.shop_id = ? AND r.status IN ('Accepted','On Going')
            ORDER BY r.accepted_at DESC NULLS LAST, r.submitted_at DESC
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
                Map.entry("mechanicId",rs.getString("mechanic_id")==null?"":rs.getString("mechanic_id")),
                Map.entry("mechanicName",safe(rs.getString("mechanic_name"))),
                Map.entry("submittedAt",rs.getTimestamp("submitted_at")==null?"":rs.getTimestamp("submitted_at").toString()),
                Map.entry("acceptedAt",rs.getTimestamp("accepted_at")==null?"":rs.getTimestamp("accepted_at").toString()),
                Map.entry("completedAt",rs.getTimestamp("completed_at")==null?"":rs.getTimestamp("completed_at").toString())
            ),shopId);
    }

    @PatchMapping("/{requestId}/status")
    @Transactional
    public Map<String,Object> status(@PathVariable UUID requestId,@RequestBody StatusBody body,Authentication auth) {
        UUID shopId=shopId(auth);
        String target=body==null||body.status()==null?"":body.status().trim();
        if(!target.equals("On Going")&&!target.equals("Completed"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Status must be On Going or Completed.");
        List<Map<String,Object>> current=jdbc.query("SELECT status, mechanic_id FROM web_assistance_requests WHERE id=? AND shop_id=? FOR UPDATE",
            (rs,row)->Map.of("status",safe(rs.getString("status")),"mechanicId",rs.getObject("mechanic_id")==null?"":rs.getObject("mechanic_id").toString()),requestId,shopId);
        if(current.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Service job not found.");
        String old=(String)current.get(0).get("status");
        String mechanicId=(String)current.get(0).get("mechanicId");
        int updated;
        if(target.equals("On Going")&&old.equals("Accepted")){
            if(!mechanicId.isBlank()){
                int claimed=jdbc.update("UPDATE web_mechanics SET availability='Busy' WHERE id=? AND shop_id=? AND lower(availability)='available'",
                    UUID.fromString(mechanicId),shopId);
                if(claimed==0)throw new ResponseStatusException(HttpStatus.CONFLICT,"Assigned mechanic is no longer available.");
            }
            updated=jdbc.update("UPDATE web_assistance_requests SET status='On Going' WHERE id=? AND shop_id=? AND status='Accepted'",requestId,shopId);
        } else if(target.equals("Completed")&&old.equals("On Going")){
            updated=jdbc.update("UPDATE web_assistance_requests SET status='Completed', completed_at=CURRENT_TIMESTAMP WHERE id=? AND shop_id=? AND status='On Going'",requestId,shopId);
            if(updated==1&&!mechanicId.isBlank()){
                jdbc.update("UPDATE web_mechanics SET availability='Available' WHERE id=? AND shop_id=? AND lower(availability)='busy'",
                    UUID.fromString(mechanicId),shopId);
            }
        } else {
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Invalid status transition. Accepted jobs can start; ongoing jobs can be completed.");
        }
        if(updated==0)throw new ResponseStatusException(HttpStatus.CONFLICT,"Job status was not updated.");
        List<UUID> ownerIds=jdbc.query("SELECT requester_account_id FROM web_assistance_requests WHERE id=? AND requester_account_id IS NOT NULL",
            (rs,row)->rs.getObject("requester_account_id",UUID.class),requestId);
        if(!ownerIds.isEmpty()){
            String title=target.equals("On Going")?"Service Started":"Service Completed";
            String message=target.equals("On Going")?"The repair shop has started your assistance service.":"Your assistance service has been completed.";
            jdbc.update("INSERT INTO web_notifications (id,account_id,type,title,message,related_request_id) VALUES (?, ?, ?, ?, ?, ?)",
                UUID.randomUUID(),ownerIds.get(0),target.equals("On Going")?"SERVICE_STARTED":"SERVICE_COMPLETED",title,message,requestId);
        }
        return Map.of("message","Job updated to "+target+".","status",target);
    }

    private UUID shopId(Authentication auth){
        List<UUID> rows=jdbc.query("""
            SELECT s.id FROM web_repair_shops s JOIN web_accounts a ON a.id=s.account_id
            WHERE lower(a.username)=lower(?) AND upper(a.account_role)='REPAIR_SHOP'
            """,(rs,row)->rs.getObject("id",UUID.class),auth.getName());
        if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Repair shop account is not linked to a shop.");
        return rows.get(0);
    }
    private static String safe(String v){return v==null?"":v;}
    public record StatusBody(String status){}
}
