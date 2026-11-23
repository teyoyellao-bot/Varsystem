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
@RequestMapping("/api/feedback")
public class FeedbackController {
    private final JdbcTemplate jdbc;
    public FeedbackController(JdbcTemplate jdbc){this.jdbc=jdbc;}

    @GetMapping
    public List<Map<String,Object>> eligibleAndSubmitted(Authentication auth){
        UUID owner=ownerId(auth);
        return jdbc.query("""
            SELECT r.id AS request_id, s.shop_name, s.id AS shop_id, r.vehicle, r.problem,
                   r.completed_at, f.id AS feedback_id, f.rating, f.comment, f.submitted_at
            FROM web_assistance_requests r
            JOIN web_repair_shops s ON s.id=r.shop_id
            LEFT JOIN web_feedback f ON f.request_id=r.id
            WHERE r.requester_account_id=? AND r.status='Completed'
            ORDER BY r.completed_at DESC NULLS LAST, r.submitted_at DESC
            """,(rs,row)->Map.ofEntries(
            Map.entry("requestId",rs.getObject("request_id",UUID.class).toString()),
            Map.entry("shopId",rs.getObject("shop_id",UUID.class).toString()),
            Map.entry("shopName",safe(rs.getString("shop_name"))),
            Map.entry("vehicle",safe(rs.getString("vehicle"))),
            Map.entry("problem",safe(rs.getString("problem"))),
            Map.entry("completedAt",rs.getTimestamp("completed_at")==null?"":rs.getTimestamp("completed_at").toString()),
            Map.entry("feedbackId",rs.getString("feedback_id")==null?"":rs.getString("feedback_id")),
            Map.entry("rating",rs.getObject("rating")==null?0:rs.getInt("rating")),
            Map.entry("comment",safe(rs.getString("comment"))),
            Map.entry("submittedAt",rs.getTimestamp("submitted_at")==null?"":rs.getTimestamp("submitted_at").toString())
        ),owner);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String,Object> submit(@RequestBody FeedbackBody body, Authentication auth){
        UUID owner=ownerId(auth);
        if(body.requestId()==null||body.requestId().isBlank())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Request ID is required.");
        if(body.rating()<1||body.rating()>5)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Rating must be from 1 to 5.");
        String comment=body.comment()==null?"":body.comment().trim();
        if(comment.length()>2000)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Feedback must be 2000 characters or fewer.");
        UUID requestId;
        try{requestId=UUID.fromString(body.requestId());}catch(IllegalArgumentException ex){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid request ID.");}
        List<UUID> shops=jdbc.query("""
            SELECT shop_id FROM web_assistance_requests WHERE id=? AND requester_account_id=? AND status='Completed'
            """,(rs,row)->rs.getObject("shop_id",UUID.class),requestId,owner);
        if(shops.isEmpty())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Feedback is only allowed for your completed requests.");
        UUID shopId=shops.get(0);
        Integer exists=jdbc.queryForObject("SELECT count(*) FROM web_feedback WHERE request_id=?",Integer.class,requestId);
        if(exists!=null&&exists>0)throw new ResponseStatusException(HttpStatus.CONFLICT,"Feedback has already been submitted for this request.");
        jdbc.update("""
            INSERT INTO web_feedback (id,request_id,shop_id,requester_account_id,rating,comment)
            VALUES(?,?,?,?,?,?)
            """,UUID.randomUUID(),requestId,shopId,owner,body.rating(),comment);
        jdbc.update("""
            UPDATE web_repair_shops SET rating=(SELECT COALESCE(AVG(rating),0) FROM web_feedback WHERE shop_id=?),
            review_count=(SELECT COUNT(*) FROM web_feedback WHERE shop_id=?) WHERE id=?
            """,shopId,shopId,shopId);
        return Map.of("message","Feedback submitted successfully.","rating",body.rating());
    }

    private UUID ownerId(Authentication auth){
        List<UUID> ids=jdbc.query("""
            SELECT id FROM web_accounts WHERE lower(username)=lower(?) AND account_role='VEHICLE_OWNER' LIMIT 1
            """,(rs,row)->rs.getObject("id",UUID.class),auth.getName());
        if(ids.isEmpty())throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Vehicle owner account required.");
        return ids.get(0);
    }
    private static String safe(String v){return v==null?"":v;}
    public record FeedbackBody(String requestId,int rating,String comment){}
}
