// AuthMiddleware.java — JWT check karta hai
// ye class verify karti hai ki request authorized hai ya nahi

package middleware;

import utils.JwtUtil;
import com.sun.net.httpserver.HttpExchange;

public class AuthMiddleware {

    // token verify karo aur payload return karo
    // null return matlab unauthorized
    public static String verify(HttpExchange exchange) {

        // Authorization header read karo
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");

        // header nahi hai
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }

        // "Bearer " remove karo — sirf token chahiye
        String token = authHeader.substring(7);

        // token verify karo
        return JwtUtil.verifyToken(token);
    }

    // role check karo — admin only routes ke liye
    public static boolean isAdmin(String payload) {
        return payload != null && payload.contains("\"role\":\"admin\"");
    }

    // dept ya admin — work orders ke liye
    public static boolean isDeptOrAdmin(String payload) {
        return payload != null &&
            (payload.contains("\"role\":\"admin\"") || payload.contains("\"role\":\"dept\""));
    }

    // error response bhejo — unauthorized
    public static void sendUnauthorized(HttpExchange exchange) throws java.io.IOException {
        String body = "{\"success\":false,\"message\":\"Unauthorized — token required\"}";
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(401, body.getBytes().length);
        java.io.OutputStream os = exchange.getResponseBody();
        os.write(body.getBytes());
        os.close();
    }

    // error response bhejo — forbidden (role nahi hai)
    public static void sendForbidden(HttpExchange exchange) throws java.io.IOException {
        String body = "{\"success\":false,\"message\":\"Forbidden — admin role required\"}";
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(403, body.getBytes().length);
        java.io.OutputStream os = exchange.getResponseBody();
        os.write(body.getBytes());
        os.close();
    }
}
