// AuthHandler.java — login, profile routes handle karta hai
// POST /api/auth/login → JWT token deta hai
// GET  /api/auth/me    → current user ka profile deta hai

package handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import utils.*;
import middleware.AuthMiddleware;
import java.io.*;

public class AuthHandler implements HttpHandler {

    public void handle(HttpExchange exchange) throws IOException {

        // CORS headers add karo
        CorsUtil.addCorsHeaders(exchange);
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        // POST /api/auth/login → login karo
        if (path.equals("/api/auth/login") && method.equals("POST")) {
            handleLogin(exchange);

        // GET /api/auth/me → current user ka profile
        } else if (path.equals("/api/auth/me") && method.equals("GET")) {
            handleMe(exchange);

        } else {
            sendResponse(exchange, 404, "{\"success\":false,\"message\":\"Route not found\"}");
        }
    }

    private void handleLogin(HttpExchange exchange) throws IOException {

        // request body read karo
        InputStream is = exchange.getRequestBody();
        String body = new String(is.readAllBytes());

        // email aur password nikalo — simple string parsing
        String email = extractField(body, "email");
        String password = extractField(body, "password");

        if (email == null || password == null) {
            sendResponse(exchange, 400,
                "{\"success\":false,\"message\":\"Email aur password required hai\"}");
            return;
        }

        // users.json se data read karo
        String usersJson = JsonUtil.readFile("users.json");

        // user dhundo by email — users.json mein "email": "..." format hai
        if (!usersJson.contains("\"email\": \"" + email + "\"")) {
            sendResponse(exchange, 401,
                "{\"success\":false,\"message\":\"Invalid email ya password\"}");
            return;
        }

        // password check karo — abhi ke liye "scae123" hardcoded hai
        // bcrypt verify baad mein add hoga jab library milegi
        if (!"scae123".equals(password)) {
            sendResponse(exchange, 401,
                "{\"success\":false,\"message\":\"Invalid email ya password\"}");
            return;
        }

        // role aur name nikalo user block se
        String role = extractUserField(usersJson, email, "role");
        String name = extractUserField(usersJson, email, "name");

        // JWT token generate karo
        String token = JwtUtil.generateToken(email, role, name);

        // success response bhejo
        String response = "{"
            + "\"success\":true,"
            + "\"message\":\"Login successful\","
            + "\"data\":{"
            +   "\"token\":\"" + token + "\","
            +   "\"role\":\"" + role + "\","
            +   "\"name\":\"" + name + "\","
            +   "\"email\":\"" + email + "\""
            + "}}";

        sendResponse(exchange, 200, response);
    }

    private void handleMe(HttpExchange exchange) throws IOException {

        // token verify karo — null matlab unauthorized
        String payload = AuthMiddleware.verify(exchange);
        if (payload == null) {
            AuthMiddleware.sendUnauthorized(exchange);
            return;
        }

        // payload se user info nikalo
        String email = extractPayloadField(payload, "sub");
        String role  = extractPayloadField(payload, "role");
        String name  = extractPayloadField(payload, "name");

        String response = "{"
            + "\"success\":true,"
            + "\"message\":\"Profile fetched\","
            + "\"data\":{"
            +   "\"email\":\"" + email + "\","
            +   "\"role\":\"" + role + "\","
            +   "\"name\":\"" + name + "\""
            + "}}";

        sendResponse(exchange, 200, response);
    }

    // helper: JSON body se field value nikalo (double-quoted string values ke liye)
    private String extractField(String json, String field) {
        String key = "\"" + field + "\"";
        int idx = json.indexOf(key);
        if (idx == -1) return null;
        // colon ke baad opening quote dhundo
        int colonIdx = json.indexOf(":", idx + key.length());
        if (colonIdx == -1) return null;
        int start = json.indexOf("\"", colonIdx) + 1;
        int end   = json.indexOf("\"", start);
        if (start <= 0 || end <= 0) return null;
        return json.substring(start, end);
    }

    // helper: users array mein specific email wale object ka field nikalo
    // approach: email se pehle '{' dhundo — wahi us user ka object start hai
    private String extractUserField(String usersJson, String email, String field) {
        int emailIdx = usersJson.indexOf("\"email\": \"" + email + "\"");
        if (emailIdx == -1) return "unknown";
        // email se peeche jaake us user ka opening '{' dhundo
        int objStart = usersJson.lastIndexOf("{", emailIdx);
        if (objStart == -1) return "unknown";
        // us user ka closing '}' dhundo
        int objEnd = usersJson.indexOf("}", emailIdx);
        if (objEnd == -1) return "unknown";
        // sirf us user ka JSON block extract karo
        String userBlock = usersJson.substring(objStart, objEnd + 1);
        String result = extractField(userBlock, field);
        return result != null ? result : "unknown";
    }

    // helper: JWT payload se field nikalo (format: "field":"value")
    private String extractPayloadField(String payload, String field) {
        String key = "\"" + field + "\":\"";
        int idx = payload.indexOf(key);
        if (idx == -1) return "unknown";
        int start = idx + key.length();
        int end = payload.indexOf("\"", start);
        if (end == -1) return "unknown";
        return payload.substring(start, end);
    }

    // helper: HTTP response bhejo
    private void sendResponse(HttpExchange exchange, int code, String body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(code, body.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(body.getBytes());
        os.close();
    }
}
