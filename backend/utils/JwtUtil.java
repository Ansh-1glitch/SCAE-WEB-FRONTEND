// JwtUtil.java — JWT token generate aur verify karne ka helper
// JWT library nahi hai toh manual HS256 implementation karenge

package utils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.Date;

public class JwtUtil {

    // JWT secret .env se aata hai
    private static final String SECRET =
        System.getenv().getOrDefault("JWT_SECRET", "scae-default-secret-key-32chars!!");

    // token 8 ghante ke liye valid hai
    private static final long EXPIRY_MS = 8 * 60 * 60 * 1000;

    // JWT token generate karo — login ke baad milta hai
    public static String generateToken(String email, String role, String name) {
        try {
            // header banao
            String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes());

            // payload banao — role aur name include karo
            long exp = System.currentTimeMillis() + EXPIRY_MS;
            String payloadJson = "{"
                + "\"sub\":\"" + email + "\","
                + "\"role\":\"" + role + "\","
                + "\"name\":\"" + name + "\","
                + "\"exp\":" + exp
                + "}";
            String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payloadJson.getBytes());

            // signature banao — HS256 use hoga
            String dataToSign = header + "." + payload;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SECRET.getBytes(), "HmacSHA256"));
            String signature = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(mac.doFinal(dataToSign.getBytes()));

            // final token return karo
            return header + "." + payload + "." + signature;
        } catch (Exception e) {
            throw new RuntimeException("Token generate nahi ho saka: " + e.getMessage());
        }
    }

    // token verify karo — middleware mein use hota hai
    // returns payload JSON string ya null if invalid
    public static String verifyToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) return null;

            // signature verify karo
            String dataToSign = parts[0] + "." + parts[1];
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SECRET.getBytes(), "HmacSHA256"));
            String expectedSig = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(mac.doFinal(dataToSign.getBytes()));

            if (!expectedSig.equals(parts[2])) return null; // signature match nahi hua

            // payload decode karo
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));

            // expiry check karo — exp last field ho sakta hai (no comma after)
            int expIdx = payload.indexOf("\"exp\":");
            if (expIdx != -1) {
                int numStart = expIdx + 6;
                // number end kahan hai — comma ya closing brace, jo pehle mile
                int commaIdx  = payload.indexOf(",", numStart);
                int braceIdx  = payload.indexOf("}", numStart);
                int numEnd;
                if (commaIdx == -1 && braceIdx == -1) numEnd = payload.length();
                else if (commaIdx == -1) numEnd = braceIdx;
                else if (braceIdx == -1) numEnd = commaIdx;
                else numEnd = Math.min(commaIdx, braceIdx);
                long exp = Long.parseLong(payload.substring(numStart, numEnd).trim());
                if (System.currentTimeMillis() > exp) return null; // token expire ho gaya
            }

            return payload;
        } catch (Exception e) {
            return null; // invalid token
        }
    }
}
