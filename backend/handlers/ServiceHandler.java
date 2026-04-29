// ServiceHandler.java — /api/services/* routes handle karta hai
// GET /api/services               → Public — saari city services
// GET /api/services/search?q=     → binary search by name (sorted assumption)
// GET /api/services/zone/{zone}   → zone se filter karo (Public)
// POST /api/services              → naya service add karo (Admin only)
// DELETE /api/services/{id}       → service delete karo (Admin only)

package handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import utils.CorsUtil;
import utils.JsonUtil;
import middleware.AuthMiddleware;
import java.io.*;
import java.util.*;

public class ServiceHandler implements HttpHandler {

    public void handle(HttpExchange exchange) throws IOException {
        CorsUtil.addCorsHeaders(exchange);
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        String path   = exchange.getRequestURI().getPath();
        String query  = exchange.getRequestURI().getRawQuery();
        String method = exchange.getRequestMethod();

        if (path.equals("/api/services") && method.equals("GET")) {
            handleList(exchange);

        } else if (path.equals("/api/services/search") && method.equals("GET")) {
            handleSearch(exchange, query);

        } else if (path.startsWith("/api/services/zone/") && method.equals("GET")) {
            String zone = path.replace("/api/services/zone/", "")
                              .replace("%20", " ").replace("+", " ");
            handleByZone(exchange, zone);

        } else if (path.equals("/api/services") && method.equals("POST")) {
            handleCreate(exchange);

        } else if (path.matches("/api/services/s\\d+") && method.equals("DELETE")) {
            String id = path.replace("/api/services/", "");
            handleDelete(exchange, id);

        } else {
            sendResponse(exchange, 404,
                "{\"success\":false,\"message\":\"Service route not found\"}");
        }
    }

    // GET /api/services — public, no auth
    private void handleList(HttpExchange exchange) throws IOException {
        String json = JsonUtil.readFile("services.json");
        sendResponse(exchange, 200,
            "{\"success\":true,\"message\":\"Services fetched\",\"data\":" + json + "}");
    }

    // GET /api/services/search?q=hospital — binary search on sorted name list
    private void handleSearch(HttpExchange exchange, String query) throws IOException {
        String q = "";
        if (query != null) {
            for (String param : query.split("&")) {
                if (param.startsWith("q=")) {
                    q = param.substring(2).replace("+", " ").replace("%20", " ").toLowerCase();
                    break;
                }
            }
        }
        if (q.isEmpty()) {
            sendResponse(exchange, 400,
                "{\"success\":false,\"message\":\"q parameter required\"}");
            return;
        }

        String json = JsonUtil.readFile("services.json");
        List<String> all = parseJsonArray(json);
        int n = all.size();

        // name array banao sorting ke liye
        String[] names = new String[n];
        for (int i = 0; i < n; i++) {
            String name = extractField(all.get(i), "name");
            names[i] = name != null ? name.toLowerCase() : "";
        }

        // sort by name — binary search ke liye
        Integer[] idx = new Integer[n];
        for (int i = 0; i < n; i++) idx[i] = i;
        Arrays.sort(idx, (a, b) -> names[a].compareTo(names[b]));

        // binary search — exact match dhundo
        List<String> results = new ArrayList<>();
        int lo = 0, hi = n - 1;
        while (lo <= hi) {
            int mid = (lo + hi) / 2;
            String midName = names[idx[mid]];
            if (midName.equals(q)) {
                results.add(all.get(idx[mid]));
                int left = mid - 1;
                while (left >= 0 && names[idx[left]].equals(q)) results.add(all.get(idx[left--]));
                int right = mid + 1;
                while (right < n && names[idx[right]].equals(q)) results.add(all.get(idx[right++]));
                break;
            } else if (midName.compareTo(q) < 0) lo = mid + 1;
            else hi = mid - 1;
        }

        // exact match nahi mila → substring fallback
        if (results.isEmpty()) {
            for (String obj : all) {
                String name = extractField(obj, "name");
                if (name != null && name.toLowerCase().contains(q)) results.add(obj);
            }
        }

        sendResponse(exchange, 200,
            "{\"success\":true,\"message\":\"Search results for: " + escapeJson(q)
            + "\",\"count\":" + results.size() + ",\"data\":" + buildArray(results) + "}");
    }

    // GET /api/services/zone/{zone} — zone se filter karo
    private void handleByZone(HttpExchange exchange, String zone) throws IOException {
        String json = JsonUtil.readFile("services.json");
        List<String> all = parseJsonArray(json);
        List<String> filtered = new ArrayList<>();

        for (String obj : all) {
            String z = extractField(obj, "zone");
            if (zone.equalsIgnoreCase(z)) filtered.add(obj);
        }

        sendResponse(exchange, 200,
            "{\"success\":true,\"message\":\"Services in zone: " + escapeJson(zone)
            + "\",\"count\":" + filtered.size() + ",\"data\":" + buildArray(filtered) + "}");
    }

    // POST /api/services — admin only
    private void handleCreate(HttpExchange exchange) throws IOException {
        String payload = AuthMiddleware.verify(exchange);
        if (!AuthMiddleware.isAdmin(payload)) {
            if (payload == null) AuthMiddleware.sendUnauthorized(exchange);
            else AuthMiddleware.sendForbidden(exchange);
            return;
        }

        String body    = new String(exchange.getRequestBody().readAllBytes());
        String name    = extractField(body, "name");
        String zone    = extractField(body, "zone");
        String type    = extractField(body, "type");
        String phone   = extractField(body, "phone");
        String nodeStr = extractField(body, "node_id");

        if (name == null || zone == null || type == null) {
            sendResponse(exchange, 400,
                "{\"success\":false,\"message\":\"name, zone, type required hai\"}");
            return;
        }

        String existing = JsonUtil.readFile("services.json");
        int count = countEntries(existing);
        String newId = "s" + String.format("%03d", count + 1);
        int nodeId = nodeStr != null ? parseInt(nodeStr) : 0;

        String newService = "{\"id\":\"" + newId
            + "\",\"name\":\"" + escapeJson(name)
            + "\",\"zone\":\"" + zone
            + "\",\"type\":\"" + type
            + "\",\"node_id\":" + nodeId
            + ",\"phone\":\"" + (phone != null ? phone : "") + "\"}";

        String updated = appendToArray(existing, newService);
        JsonUtil.writeFile("services.json", updated);

        sendResponse(exchange, 201,
            "{\"success\":true,\"message\":\"Service added\",\"data\":" + newService + "}");
    }

    // DELETE /api/services/{id} — admin only
    private void handleDelete(HttpExchange exchange, String id) throws IOException {
        String payload = AuthMiddleware.verify(exchange);
        if (!AuthMiddleware.isAdmin(payload)) {
            if (payload == null) AuthMiddleware.sendUnauthorized(exchange);
            else AuthMiddleware.sendForbidden(exchange);
            return;
        }

        String existing = JsonUtil.readFile("services.json");
        List<String> all = parseJsonArray(existing);
        boolean removed = false;
        List<String> remaining = new ArrayList<>();

        for (String obj : all) {
            if (id.equals(extractField(obj, "id"))) removed = true;
            else remaining.add(obj);
        }

        if (!removed) {
            sendResponse(exchange, 404,
                "{\"success\":false,\"message\":\"Service " + id + " not found\"}");
            return;
        }

        JsonUtil.writeFile("services.json", buildArray(remaining));
        sendResponse(exchange, 200,
            "{\"success\":true,\"message\":\"Service " + id + " deleted\"}");
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

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

    private int countEntries(String json) {
        int count = 0;
        for (int i = 0; i < json.length() - 1; i++) if (json.charAt(i) == '{') count++;
        return count;
    }

    private String appendToArray(String arrayJson, String newElement) {
        String trimmed = arrayJson.trim();
        if (trimmed.equals("[]")) return "[" + newElement + "]";
        return trimmed.substring(0, trimmed.lastIndexOf("]")) + "," + newElement + "]";
    }

    private String buildArray(List<String> items) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < items.size(); i++) { if (i > 0) sb.append(","); sb.append(items.get(i)); }
        sb.append("]"); return sb.toString();
    }

    private int parseInt(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 0; }
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
