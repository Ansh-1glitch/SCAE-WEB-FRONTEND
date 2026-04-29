// WorkOrderHandler.java — /api/work-orders/* routes handle karta hai
// GET  /api/work-orders              → list (admin/dept: all)
// POST /api/work-orders              → create (admin/dept)
// GET  /api/work-orders/sorted       → urgency sorted (heap sort)
// GET  /api/work-orders/assign-top   → highest urgency Pending order assign (job seq logic)
// GET  /api/work-orders/{id}         → single
// PUT  /api/work-orders/{id}/status  → status update (admin/dept)
// DELETE /api/work-orders/{id}       → delete (admin only)

package handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import utils.CorsUtil;
import utils.JsonUtil;
import middleware.AuthMiddleware;
import java.io.*;
import java.util.*;

public class WorkOrderHandler implements HttpHandler {

    public void handle(HttpExchange exchange) throws IOException {
        CorsUtil.addCorsHeaders(exchange);
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        String path   = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        if (path.equals("/api/work-orders") && method.equals("GET")) {
            handleList(exchange);

        } else if (path.equals("/api/work-orders") && method.equals("POST")) {
            handleCreate(exchange);

        } else if (path.equals("/api/work-orders/sorted") && method.equals("GET")) {
            handleSorted(exchange);

        } else if (path.equals("/api/work-orders/assign-top") && method.equals("GET")) {
            handleAssignTop(exchange);

        } else if (path.matches("/api/work-orders/WO-\\d+") && method.equals("GET")) {
            String id = path.replace("/api/work-orders/", "");
            handleGetById(exchange, id);

        } else if (path.matches("/api/work-orders/WO-\\d+/status") && method.equals("PUT")) {
            String id = path.replace("/api/work-orders/", "").replace("/status", "");
            handleUpdateStatus(exchange, id);

        } else if (path.matches("/api/work-orders/WO-\\d+") && method.equals("DELETE")) {
            String id = path.replace("/api/work-orders/", "");
            handleDelete(exchange, id);

        } else {
            sendResponse(exchange, 404,
                "{\"success\":false,\"message\":\"Work order route not found\"}");
        }
    }

    // GET /api/work-orders — admin/dept only
    private void handleList(HttpExchange exchange) throws IOException {
        String payload = AuthMiddleware.verify(exchange);
        if (!AuthMiddleware.isDeptOrAdmin(payload)) {
            if (payload == null) AuthMiddleware.sendUnauthorized(exchange);
            else AuthMiddleware.sendForbidden(exchange);
            return;
        }
        String allJson = JsonUtil.readFile("work_orders.json");
        sendResponse(exchange, 200,
            "{\"success\":true,\"message\":\"Work orders fetched\",\"data\":" + allJson + "}");
    }

    // POST /api/work-orders — admin/dept only
    private void handleCreate(HttpExchange exchange) throws IOException {
        String payload = AuthMiddleware.verify(exchange);
        if (!AuthMiddleware.isDeptOrAdmin(payload)) {
            if (payload == null) AuthMiddleware.sendUnauthorized(exchange);
            else AuthMiddleware.sendForbidden(exchange);
            return;
        }

        String body     = new String(exchange.getRequestBody().readAllBytes());
        String title    = extractField(body, "title");
        String zone     = extractField(body, "zone");
        String urgStr   = extractField(body, "urgency");
        String deadline = extractField(body, "deadline");
        String crew     = extractField(body, "crew");
        String desc     = extractField(body, "description");

        if (title == null || zone == null) {
            sendResponse(exchange, 400,
                "{\"success\":false,\"message\":\"title aur zone required hai\"}");
            return;
        }

        int urgency = urgStr != null ? parseInt(urgStr) : 5;
        String existing = JsonUtil.readFile("work_orders.json");
        int count = countEntries(existing);
        String newId = "WO-" + String.format("%03d", count + 1);

        String newOrder = "{\"id\":\"" + newId
            + "\",\"title\":\"" + escapeJson(title)
            + "\",\"zone\":\"" + zone
            + "\",\"urgency\":" + urgency
            + ",\"deadline\":\"" + (deadline != null ? deadline : "")
            + "\",\"crew\":" + (crew != null ? "\"" + escapeJson(crew) + "\"" : "null")
            + ",\"status\":\"Pending\""
            + ",\"description\":\"" + (desc != null ? escapeJson(desc) : "") + "\"}";

        String updated = appendToArray(existing, newOrder);
        JsonUtil.writeFile("work_orders.json", updated);

        sendResponse(exchange, 201,
            "{\"success\":true,\"message\":\"Work order created\",\"data\":" + newOrder + "}");
    }

    // GET /api/work-orders/sorted — urgency descending (heap sort)
    private void handleSorted(HttpExchange exchange) throws IOException {
        String payload = AuthMiddleware.verify(exchange);
        if (!AuthMiddleware.isDeptOrAdmin(payload)) {
            if (payload == null) AuthMiddleware.sendUnauthorized(exchange);
            else AuthMiddleware.sendForbidden(exchange);
            return;
        }

        String allJson = JsonUtil.readFile("work_orders.json");
        List<String> objects = parseJsonArray(allJson);
        int n = objects.size();

        int[] urgencies = new int[n];
        for (int i = 0; i < n; i++) {
            String u = extractField(objects.get(i), "urgency");
            urgencies[i] = u != null ? parseInt(u) : 0;
        }

        Integer[] idx = new Integer[n];
        for (int i = 0; i < n; i++) idx[i] = i;
        heapSortDesc(urgencies, idx, n);

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < n; i++) {
            if (i > 0) sb.append(",");
            sb.append(objects.get(idx[i]));
        }
        sb.append("]");

        sendResponse(exchange, 200,
            "{\"success\":true,\"message\":\"Work orders sorted by urgency\",\"data\":" + sb + "}");
    }

    // GET /api/work-orders/assign-top — highest urgency Pending order dhundo
    private void handleAssignTop(HttpExchange exchange) throws IOException {
        String payload = AuthMiddleware.verify(exchange);
        if (!AuthMiddleware.isDeptOrAdmin(payload)) {
            if (payload == null) AuthMiddleware.sendUnauthorized(exchange);
            else AuthMiddleware.sendForbidden(exchange);
            return;
        }

        String allJson = JsonUtil.readFile("work_orders.json");
        List<String> all = parseJsonArray(allJson);

        // Pending orders mein se highest urgency wala nikalo
        String top = null;
        int maxUrgency = -1;
        for (String obj : all) {
            String status = extractField(obj, "status");
            if (!"Pending".equals(status)) continue;
            String u = extractField(obj, "urgency");
            int urgency = u != null ? parseInt(u) : 0;
            if (urgency > maxUrgency) { maxUrgency = urgency; top = obj; }
        }

        if (top == null) {
            sendResponse(exchange, 200,
                "{\"success\":true,\"message\":\"No pending work orders\",\"data\":null}");
            return;
        }

        sendResponse(exchange, 200,
            "{\"success\":true,\"message\":\"Top priority work order\",\"urgency\":"
            + maxUrgency + ",\"data\":" + top + "}");
    }

    // GET /api/work-orders/{id}
    private void handleGetById(HttpExchange exchange, String id) throws IOException {
        String payload = AuthMiddleware.verify(exchange);
        if (!AuthMiddleware.isDeptOrAdmin(payload)) {
            if (payload == null) AuthMiddleware.sendUnauthorized(exchange);
            else AuthMiddleware.sendForbidden(exchange);
            return;
        }

        String allJson = JsonUtil.readFile("work_orders.json");
        String found = findById(allJson, "id", id);

        if (found == null) {
            sendResponse(exchange, 404,
                "{\"success\":false,\"message\":\"Work order " + id + " not found\"}");
            return;
        }
        sendResponse(exchange, 200,
            "{\"success\":true,\"message\":\"Work order fetched\",\"data\":" + found + "}");
    }

    // PUT /api/work-orders/{id}/status — admin/dept
    private void handleUpdateStatus(HttpExchange exchange, String id) throws IOException {
        String payload = AuthMiddleware.verify(exchange);
        if (!AuthMiddleware.isDeptOrAdmin(payload)) {
            if (payload == null) AuthMiddleware.sendUnauthorized(exchange);
            else AuthMiddleware.sendForbidden(exchange);
            return;
        }

        String body   = new String(exchange.getRequestBody().readAllBytes());
        String status = extractField(body, "status");
        String crew   = extractField(body, "crew");

        if (status == null) {
            sendResponse(exchange, 400,
                "{\"success\":false,\"message\":\"status required hai\"}");
            return;
        }

        String allJson = JsonUtil.readFile("work_orders.json");
        List<String> all = parseJsonArray(allJson);
        boolean found = false;
        List<String> result = new ArrayList<>();

        for (String obj : all) {
            String objId = extractField(obj, "id");
            if (id.equals(objId)) {
                found = true;
                obj = replaceStringField(obj, "status", status);
                if (crew != null) obj = replaceStringField(obj, "crew", crew);
            }
            result.add(obj);
        }

        if (!found) {
            sendResponse(exchange, 404,
                "{\"success\":false,\"message\":\"Work order " + id + " not found\"}");
            return;
        }

        String updated = buildArray(result);
        JsonUtil.writeFile("work_orders.json", updated);

        sendResponse(exchange, 200,
            "{\"success\":true,\"message\":\"Work order " + id + " status updated to " + status + "\"}");
    }

    // DELETE /api/work-orders/{id} — admin only
    private void handleDelete(HttpExchange exchange, String id) throws IOException {
        String payload = AuthMiddleware.verify(exchange);
        if (!AuthMiddleware.isAdmin(payload)) {
            if (payload == null) AuthMiddleware.sendUnauthorized(exchange);
            else AuthMiddleware.sendForbidden(exchange);
            return;
        }

        String allJson = JsonUtil.readFile("work_orders.json");
        List<String> all = parseJsonArray(allJson);
        boolean removed = false;
        List<String> remaining = new ArrayList<>();

        for (String obj : all) {
            if (id.equals(extractField(obj, "id"))) removed = true;
            else remaining.add(obj);
        }

        if (!removed) {
            sendResponse(exchange, 404,
                "{\"success\":false,\"message\":\"Work order " + id + " not found\"}");
            return;
        }

        JsonUtil.writeFile("work_orders.json", buildArray(remaining));
        sendResponse(exchange, 200,
            "{\"success\":true,\"message\":\"Work order " + id + " deleted\"}");
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

    private String findById(String json, String field, String value) {
        List<String> all = parseJsonArray(json);
        for (String obj : all) {
            if (value.equals(extractField(obj, field))) return obj;
        }
        return null;
    }

    private String replaceStringField(String obj, String field, String newValue) {
        String key = "\"" + field + "\":";
        int idx = obj.indexOf(key);
        if (idx == -1) return obj;
        int afterColon = idx + key.length();
        while (afterColon < obj.length() && obj.charAt(afterColon) == ' ') afterColon++;
        if (afterColon >= obj.length()) return obj;
        int valueEnd;
        if (obj.charAt(afterColon) == '"') {
            valueEnd = obj.indexOf("\"", afterColon + 1) + 1;
        } else if (obj.startsWith("null", afterColon)) {
            valueEnd = afterColon + 4;
        } else {
            valueEnd = afterColon;
            while (valueEnd < obj.length() && obj.charAt(valueEnd) != ',' && obj.charAt(valueEnd) != '}') valueEnd++;
        }
        return obj.substring(0, idx + key.length()) + "\"" + escapeJson(newValue) + "\"" + obj.substring(valueEnd);
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
        for (int i = 0; i < json.length() - 1; i++) {
            if (json.charAt(i) == '{') count++;
        }
        return count;
    }

    private String appendToArray(String arrayJson, String newElement) {
        String trimmed = arrayJson.trim();
        if (trimmed.equals("[]")) return "[" + newElement + "]";
        int lastBracket = trimmed.lastIndexOf("]");
        return trimmed.substring(0, lastBracket) + "," + newElement + "]";
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

    private int parseInt(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 0; }
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private void heapSortDesc(int[] vals, Integer[] idx, int n) {
        for (int i = n / 2 - 1; i >= 0; i--) heapify(vals, idx, n, i);
        for (int i = n - 1; i > 0; i--) {
            Integer tmp = idx[0]; idx[0] = idx[i]; idx[i] = tmp;
            heapify(vals, idx, i, 0);
        }
        // reverse for descending
        for (int i = 0, j = n - 1; i < j; i++, j--) {
            Integer tmp = idx[i]; idx[i] = idx[j]; idx[j] = tmp;
        }
    }

    private void heapify(int[] vals, Integer[] idx, int n, int root) {
        int largest = root, l = 2 * root + 1, r = 2 * root + 2;
        if (l < n && vals[idx[l]] > vals[idx[largest]]) largest = l;
        if (r < n && vals[idx[r]] > vals[idx[largest]]) largest = r;
        if (largest != root) {
            Integer tmp = idx[root]; idx[root] = idx[largest]; idx[largest] = tmp;
            heapify(vals, idx, n, largest);
        }
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
