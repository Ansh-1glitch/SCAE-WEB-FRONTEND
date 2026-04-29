// CitizenHandler.java — /api/citizens/* routes handle karta hai
// GET /api/citizens              → users.json se role=citizen filter karo (Admin only)
// GET /api/citizens/search?q=   → name ya email se search karo (HashMap — O(1) avg)
// GET /api/citizens/{id}        → single citizen + unke complaints

package handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import utils.CorsUtil;
import utils.JsonUtil;
import middleware.AuthMiddleware;
import java.io.*;
import java.util.*;

public class CitizenHandler implements HttpHandler {

    public void handle(HttpExchange exchange) throws IOException {
        CorsUtil.addCorsHeaders(exchange);
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        String path   = exchange.getRequestURI().getPath();
        String query  = exchange.getRequestURI().getRawQuery();
        String method = exchange.getRequestMethod();

        if (path.equals("/api/citizens") && method.equals("GET")) {
            handleList(exchange);

        } else if (path.equals("/api/citizens/search") && method.equals("GET")) {
            handleSearch(exchange, query);

        } else if (path.matches("/api/citizens/u\\d+") && method.equals("GET")) {
            String id = path.replace("/api/citizens/", "");
            handleGetById(exchange, id);

        } else {
            sendResponse(exchange, 404,
                "{\"success\":false,\"message\":\"Citizen route not found\"}");
        }
    }

    // GET /api/citizens — admin only, role=citizen filter karke return karo
    private void handleList(HttpExchange exchange) throws IOException {
        String payload = AuthMiddleware.verify(exchange);
        if (!AuthMiddleware.isAdmin(payload)) {
            if (payload == null) AuthMiddleware.sendUnauthorized(exchange);
            else AuthMiddleware.sendForbidden(exchange);
            return;
        }

        String usersJson = JsonUtil.readFile("users.json");
        List<String> all = parseJsonArray(usersJson);

        // role=citizen wale users nikalo — password strip karo
        List<String> citizens = new ArrayList<>();
        for (String user : all) {
            String role = extractField(user, "role");
            if ("citizen".equals(role)) {
                citizens.add(stripPassword(user));
            }
        }

        sendResponse(exchange, 200,
            "{\"success\":true,\"message\":\"Citizens fetched\",\"count\":"
            + citizens.size() + ",\"data\":" + buildArray(citizens) + "}");
    }

    // GET /api/citizens/search?q= — HashMap based search (O(1) average)
    // q matches against name ya email
    private void handleSearch(HttpExchange exchange, String query) throws IOException {
        String payload = AuthMiddleware.verify(exchange);
        if (!AuthMiddleware.isAdmin(payload)) {
            if (payload == null) AuthMiddleware.sendUnauthorized(exchange);
            else AuthMiddleware.sendForbidden(exchange);
            return;
        }

        String q = "";
        if (query != null) {
            for (String param : query.split("&")) {
                if (param.startsWith("q=")) {
                    q = param.substring(2).replace("+", " ").toLowerCase();
                    break;
                }
            }
        }
        if (q.isEmpty()) {
            sendResponse(exchange, 400,
                "{\"success\":false,\"message\":\"q parameter required\"}");
            return;
        }

        String usersJson = JsonUtil.readFile("users.json");
        List<String> all = parseJsonArray(usersJson);

        // HashMap banao: key = id/email/name (lowercase) → user object
        // O(1) lookup by id ya email; substring search for name
        Map<String, String> idMap    = new HashMap<>();
        Map<String, String> emailMap = new HashMap<>();

        for (String user : all) {
            String role = extractField(user, "role");
            if (!"citizen".equals(role)) continue;
            String id    = extractField(user, "id");
            String email = extractField(user, "email");
            if (id    != null) idMap.put(id.toLowerCase(), user);
            if (email != null) emailMap.put(email.toLowerCase(), user);
        }

        List<String> results = new ArrayList<>();

        // exact match by id ya email — O(1)
        if (idMap.containsKey(q)) {
            results.add(stripPassword(idMap.get(q)));
        } else if (emailMap.containsKey(q)) {
            String u = emailMap.get(q);
            if (!results.contains(u)) results.add(stripPassword(u));
        } else {
            // substring search on name — O(n) fallback
            for (String user : all) {
                String role = extractField(user, "role");
                if (!"citizen".equals(role)) continue;
                String name = extractField(user, "name");
                if (name != null && name.toLowerCase().contains(q)) {
                    results.add(stripPassword(user));
                }
            }
        }

        sendResponse(exchange, 200,
            "{\"success\":true,\"message\":\"Search results for: " + escapeJson(q)
            + "\",\"count\":" + results.size() + ",\"data\":" + buildArray(results) + "}");
    }

    // GET /api/citizens/{id} — single citizen + unke complaints
    private void handleGetById(HttpExchange exchange, String id) throws IOException {
        String payload = AuthMiddleware.verify(exchange);
        if (!AuthMiddleware.isAdmin(payload)) {
            if (payload == null) AuthMiddleware.sendUnauthorized(exchange);
            else AuthMiddleware.sendForbidden(exchange);
            return;
        }

        String usersJson = JsonUtil.readFile("users.json");
        List<String> all = parseJsonArray(usersJson);

        String citizen = null;
        for (String user : all) {
            if (id.equals(extractField(user, "id"))) {
                citizen = stripPassword(user);
                break;
            }
        }

        if (citizen == null) {
            sendResponse(exchange, 404,
                "{\"success\":false,\"message\":\"Citizen " + id + " not found\"}");
            return;
        }

        // citizen ke complaints bhi fetch karo
        String complaintsJson = JsonUtil.readFile("complaints.json");
        List<String> allComplaints = parseJsonArray(complaintsJson);
        List<String> citizenComplaints = new ArrayList<>();
        for (String c : allComplaints) {
            // citizen_id field match — users.json mein id hai, complaints mein citizen_id
            String cid = extractField(c, "citizen_id");
            if (id.equals(cid)) citizenComplaints.add(c);
        }

        sendResponse(exchange, 200,
            "{\"success\":true,\"message\":\"Citizen profile fetched\","
            + "\"data\":" + citizen
            + ",\"complaints\":" + buildArray(citizenComplaints) + "}");
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    // user object se password field remove karo
    private String stripPassword(String user) {
        // "password":"..." field remove karo
        String key = "\"password\":";
        int idx = user.indexOf(key);
        if (idx == -1) return user;
        int afterColon = idx + key.length();
        while (afterColon < user.length() && user.charAt(afterColon) == ' ') afterColon++;
        int valueEnd;
        if (afterColon < user.length() && user.charAt(afterColon) == '"') {
            valueEnd = user.indexOf("\"", afterColon + 1) + 1;
        } else {
            valueEnd = afterColon;
            while (valueEnd < user.length() && user.charAt(valueEnd) != ',' && user.charAt(valueEnd) != '}') valueEnd++;
        }
        // comma bhi remove karo (before ya after)
        String before = user.substring(0, idx);
        String after  = user.substring(valueEnd);
        if (before.endsWith(",")) before = before.substring(0, before.length() - 1);
        else if (after.startsWith(",")) after = after.substring(1);
        return before + after;
    }

    private List<String> parseJsonArray(String json) {
        List<String> list = new ArrayList<>();
        String trimmed = json.trim();
        int i = 0;
        while (i < trimmed.length()) {
            if (trimmed.charAt(i) == '{') {
                int depth = 0, start = i;
                while (i < trimmed.length()) {
                    char c = trimmed.charAt(i);
                    if (c == '{') depth++;
                    else if (c == '}') { depth--; if (depth == 0) { list.add(trimmed.substring(start, i + 1)); break; } }
                    i++;
                }
            }
            i++;
        }
        return list;
    }

    private String extractField(String json, String field) {
        String key = "\"" + field + "\"";
        int idx = json.indexOf(key);
        if (idx == -1) return null;
        int colon = json.indexOf(":", idx + key.length());
        if (colon == -1) return null;
        int start = colon + 1;
        while (start < json.length() && json.charAt(start) == ' ') start++;
        if (start >= json.length()) return null;
        if (json.charAt(start) == '"') {
            int end = json.indexOf("\"", start + 1);
            return end == -1 ? null : json.substring(start + 1, end);
        } else if (json.startsWith("null", start)) {
            return null;
        } else {
            int end = start;
            while (end < json.length() && "0123456789.-".indexOf(json.charAt(end)) >= 0) end++;
            return json.substring(start, end);
        }
    }

    private String buildArray(List<String> items) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(items.get(i));
        }
        sb.append("]");
        return sb.toString();
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private void sendResponse(HttpExchange exchange, int code, String body) throws IOException {
        byte[] bytes = body.getBytes("UTF-8");
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(code, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }
}
