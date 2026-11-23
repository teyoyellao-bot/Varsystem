package com.vaarl.web.auth;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.security.MessageDigest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final String secret;
    private final long expiresSeconds;
    private final ObjectMapper mapper = new ObjectMapper();

    public JwtService(@Value("${vaarl.jwt.secret}") String secret,
                    @Value("${vaarl.jwt.expires-seconds:3600}") long expiresSeconds) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32)
            throw new IllegalStateException("VAARL JWT secret must be at least 32 bytes.");
        this.secret = secret;
        this.expiresSeconds = expiresSeconds;
    }

    public String createToken(String accountId, String role, String username) {
        long now = Instant.now().getEpochSecond();
        String header = encode("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
        String payload = encode("{\"sub\":\""+escape(accountId)+"\",\"role\":\""+escape(role)+"\",\"username\":\""+escape(username)+"\",\"iat\":"+now+",\"exp\":"+(now+expiresSeconds)+"}");
        String unsigned = header + "." + payload;
        return unsigned + "." + sign(unsigned);
    }

    public Claims validate(String token) {
        try {
            String[] parts = token.split("\\.", -1);
            if (parts.length != 3) return null;
            var header = mapper.readTree(decode(parts[0]));
            if (!"HS256".equals(header.path("alg").asText()) || !"JWT".equals(header.path("typ").asText())) return null;
            String unsigned = parts[0] + "." + parts[1];
            byte[] expected = sign(unsigned).getBytes(StandardCharsets.US_ASCII);
            byte[] actual = parts[2].getBytes(StandardCharsets.US_ASCII);
            if (!MessageDigest.isEqual(expected, actual)) return null;
            var payload = mapper.readTree(decode(parts[1]));
            long exp = payload.path("exp").asLong(0);
            long issuedAt = payload.path("iat").asLong(0);
            long now = Instant.now().getEpochSecond();
            if (issuedAt <= 0 || issuedAt > now + 30 || exp <= now || exp <= issuedAt || exp - issuedAt > expiresSeconds) return null;
            String sub = payload.path("sub").asText("");
            String role = payload.path("role").asText("");
            String username = payload.path("username").asText("");
            if (sub.isBlank() || username.isBlank() || !java.util.Set.of("VEHICLE_OWNER","REPAIR_SHOP","ADMIN").contains(role)) return null;
            return new Claims(sub, role, username, exp);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String sign(String input) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(input.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) { throw new IllegalStateException("Unable to sign JWT.", e); }
    }
    private static String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
    private static String decode(String value) {
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
    }
    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
    public record Claims(String subject, String role, String username, long expiresAt) {}
}
