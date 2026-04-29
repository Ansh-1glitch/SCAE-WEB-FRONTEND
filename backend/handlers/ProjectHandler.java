// ProjectHandler.java — /api/projects/* routes handle karta hai
// GET    /api/projects        → projects.json return karo (Admin only)
// POST   /api/projects        → naya project add karo (Admin only)
// PUT    /api/projects/{id}   → project fields update karo (Admin only)
// DELETE /api/projects/{id}   → soft delete — active = false karo (Admin only)

package handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import utils.CorsUtil;
import utils.JsonUtil;
import middleware.AuthMiddleware;
import java.io.*;
import java.util.*;

public class ProjectHandler implements HttpHandler {

    public void handle(HttpExchange exchange) throws IOException {
        CorsUtil.addCorsHeaders(exchange);
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        String path   = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        if (path.equals("/api/projects") && method.equals("GET")) {
            handleList(exchange);

        } else if (path.equals("/api/projects") && method.equals("POST")) {
            handleCreate(exchange);

        } else if (path.matches("/api/projects/p\\d+") && method.equals("PUT")) {
            String id = path.replace("/api/projects/", "");
            handleUpdate(exchange, id);

        } else if (path.matches("/api/projects/p\\d+") && method.equals("DELETE")) {
            String id = path.replace("/api/projects/", "");
            handleSoftDelete(exchange, id);

        } else {
            sendResponse(exchange, 404,
                "{\"success\":false,\"message\":\"Project route not found\"}");
        }
    }

    // GET /api/projects — admin only
    private void handleList(HttpExchange exchange) throws IOException {
        String payload = AuthMiddleware.verify(exchange);
        if (!AuthMiddleware.isAdmin(payload)) {
            if (payload == null) AuthMiddleware.sendUnauthorized(exchange);
            else AuthMiddleware.sendForbidden(exchange);
            return;
        }
        String json = JsonUtil.readFile("projects.json");
        sendResponse(exchange, 200,
            "{\"success\":true,\"message\":\"Projects fetched\",\"data\":" + json + "}");
    }

    // POST /api/projects — admin only
    private void handleCreate(HttpExchange exchange) throws IOException {
        String payload = AuthMiddleware.verify(exchange);
        if (!AuthMiddleware.isAdmin(payload)) {
            if (payload == null) AuthMiddleware.sendUnauthorized(exchange);
            else AuthMiddleware.sendForbidden(exchange);
            return;
        }

        String body     = new String(exchange.getRequestBody().readAllBytes());
        String name     = extractField(body, "name");
        String costStr  = extractField(body, "cost");
        String benStr   = extractField(body, "benefit");
        String category = extractField(body, "category");

        if (name == null || costStr == null || benStr == null) {
            sendResponse(exchange, 400,
                "{\"success\":false,\"message\":\"name, cost, benefit required hai\"}");
            return;
        }

        String existing = JsonUtil.readFile("projects.json");
        int count = countEntries(existing);
        String newId = "p" + String.format("%03d", count + 1);

        String newProject = "{\"id\":\"" + newId
            + "\",\"name\":\"" + escapeJson(name)
            + "\",\"cost\":" + parseInt(costStr)
            + ",\"benefit\":" + parseInt(benStr)
            + ",\"category\":\"" + (category != null ? category : "General")
            + "\",\"active\":true}";

        String updated = appendToArray(existing, newProject);
        JsonUtil.writeFile("projects.json", updated);

        sendResponse(exchange, 201,
            "{\"success\":true,\"message\":\"Project created\",\"data\":" + newProject + "}");
    }

    // PUT /api/projects/{id} — admin only, field-by-field update
    private void handleUpdate(HttpExchange exchange, String id) throws IOException {
        String payload = AuthMiddleware.verify(exchange);
        if (!AuthMiddleware.isAdmin(payload)) {
            if (payload == null) AuthMiddleware.sendUnauthorized(exchange);
            else AuthMiddleware.sendForbidden(exchange);
            return;
        }

        String body     = new String(exchange.getRequestBody().readAllBytes());
        String name     = extractField(body, "name");
        String costStr  = extractField(body, "cost");
        String benStr   = extractField(body, "benefit");
        String category = extractField(body, "category");

        String existing = JsonUtil.readFile("projects.json");
        List<String> all = parseJsonArray(existing);
        boolean found = false;
        List<String> result = new ArrayList<>();

        for (String obj : all) {
            if (id.equals(extractField(obj, "id"))) {
                found = true;
                if (name     != null) obj = replaceStringField(obj,  "name",     name);
                if (costStr  != null) obj = replaceNumericField(obj, "cost",     parseInt(costStr));
                if (benStr   != null) obj = replaceNumericField(obj, "benefit",  parseInt(benStr));
                if (category != null) obj = replaceStringField(obj,  "category", category);
            }
            result.add(obj);
        }

        if (!found) {
            sendResponse(exchange, 404,
                "{\"success\":false,\"message\":\"Project " + id + " not found\"}");
            return;
        }

        JsonUtil.writeFile("projects.json", buildArray(result));
        sendResponse(exchange, 200,
            "{\"success\":true,\"message\":\"Project " + id + " updated\"}");
    }

    // DELETE /api/projects/{id} — soft delete: active = false
    private void handleSoftDelete(HttpExchange exchange, String id) throws IOException {
        String payload = AuthMiddleware.verify(exchange);
        if (!AuthMiddleware.isAdmin(payload)) {
            if (payload == null) AuthMiddleware.sendUnauthorized(exchange);
            else AuthMiddleware.sendForbidden(exchange);
            return;
        }

        String existing = JsonUtil.readFile("projects.json");
        List<String> all = parseJsonArray(existing);
        boolean found = false;
        List<String> result = new ArrayList<>();

        for (String obj : all) {
            if (id.equals(extractField(obj, "id"))) {
                found = true;
                obj = replaceBoolField(obj, "active", false);
            }
            result.add(obj);
        }

        if (!found) {
            sendResponse(exchange, 404,
                "{\"success\":false,\"message\":\"Project " + id + " not found\"}");
            return;
        }

        JsonUtil.writeFile("projects.json", buildArray(result));
        sendResponse(exchange, 200,
            "{\"success\":true,\"message\":\"Project " + id + " deactivated (soft delete)\"}");
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

    private String replaceStringField(String obj, String field, String newValue) {
        String key = "\"" + field + "\":";
        int idx = obj.indexOf(key);
        if (idx == -1) return obj;
        int afterColon = idx + key.length();
        while (afterColon < obj.length() && obj.charAt(afterColon) == ' ') afterColon++;
        int valueEnd;
        if (obj.charAt(afterColon) == '"') valueEnd = obj.indexOf("\"", afterColon + 1) + 1;
        else { valueEnd = afterColon; while (valueEnd < obj.length() && obj.charAt(valueEnd) != ',' && obj.charAt(valueEnd) != '}') valueEnd++; }
        return obj.substring(0, idx + key.length()) + "\"" + escapeJson(newValue) + "\"" + obj.substring(valueEnd);
    }

    private String replaceNumericField(String obj, String field, int newValue) {
        String key = "\"" + field + "\":";
        int idx = obj.indexOf(key);
        if (idx == -1) return obj;
        int afterColon = idx + key.length();
        while (afterColon < obj.length() && obj.charAt(afterColon) == ' ') afterColon++;
        int valueEnd = afterColon;
        while (valueEnd < obj.length() && obj.charAt(valueEnd) != ',' && obj.charAt(valueEnd) != '}') valueEnd++;
        return obj.substring(0, idx + key.length()) + newValue + obj.substring(valueEnd);
    }

    private String replaceBoolField(String obj, String field, boolean newValue) {
        String key = "\"" + field + "\":";
        int idx = obj.indexOf(key);
        if (idx == -1) return obj;
        int afterColon = idx + key.length();
        while (afterColon < obj.length() && obj.charAt(afterColon) == ' ') afterColon++;
        int valueEnd = afterColon;
        while (valueEnd < obj.length() && obj.charAt(valueEnd) != ',' && obj.charAt(valueEnd) != '}') valueEnd++;
        return obj.substring(0, idx + key.length()) + newValue + obj.substring(valueEnd);
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
