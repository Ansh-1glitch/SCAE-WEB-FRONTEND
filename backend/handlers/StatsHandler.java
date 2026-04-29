// StatsHandler.java — /api/stats/* routes handle karta hai
// GET /api/stats/summary      → Public — homepage dashboard counters
// GET /api/stats/complaints   → Admin — complaints breakdown by zone + category
// GET /api/stats/work-orders  → Admin — work orders breakdown

package handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import utils.CorsUtil;
import utils.JsonUtil;
import middleware.AuthMiddleware;
import java.io.*;
import java.util.*;

public class StatsHandler implements HttpHandler {

    public void handle(HttpExchange exchange) throws IOException {
        CorsUtil.addCorsHeaders(exchange);
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        String path   = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        if (path.equals("/api/stats/summary") && method.equals("GET")) {
            handleSummary(exchange);

        } else if (path.equals("/api/stats/complaints") && method.equals("GET")) {
            handleComplaintsStats(exchange);

        } else if (path.equals("/api/stats/work-orders") && method.equals("GET")) {
            handleWorkOrdersStats(exchange);

        } else {
            sendResponse(exchange, 404,
                "{\"success\":false,\"message\":\"Stats route not found\"}");
        }
    }

    // GET /api/stats/summary — public, no auth required
    private void handleSummary(HttpExchange exchange) throws IOException {
        // complaints.json se Resolved count karo
        String complaintsJson = JsonUtil.readFile("complaints.json");
        List<String> complaints = parseJsonArray(complaintsJson);
        int resolved = 0;
        for (String c : complaints) {
            if ("Resolved".equals(extractField(c, "status"))) resolved++;
        }

        // nodes.json se zones count karo
        String nodesJson = JsonUtil.readFile("nodes.json");
        int zonesCovered = countEntries(nodesJson); // 13 nodes = 13 zones covered

        // work_orders.json se active (non-Completed) count karo
        String workOrdersJson = JsonUtil.readFile("work_orders.json");
        List<String> workOrders = parseJsonArray(workOrdersJson);
        int activeWorkOrders = 0;
        for (String wo : workOrders) {
            if (!"Completed".equals(extractField(wo, "status"))) activeWorkOrders++;
        }

        // users.json se citizen count karo
        String usersJson = JsonUtil.readFile("users.json");
        List<String> users = parseJsonArray(usersJson);
        int citizens = 0;
        for (String u : users) {
            if ("citizen".equals(extractField(u, "role"))) citizens++;
        }

        String response = "{\"success\":true,\"message\":\"Stats summary fetched\",\"data\":{"
            + "\"complaints_resolved\":" + resolved
            + ",\"zones_covered\":" + zonesCovered
            + ",\"active_work_orders\":" + activeWorkOrders
            + ",\"citizens_registered\":" + citizens
            + "}}";
        sendResponse(exchange, 200, response);
    }

    // GET /api/stats/complaints — admin only, breakdown by zone + category
    private void handleComplaintsStats(HttpExchange exchange) throws IOException {
        String payload = AuthMiddleware.verify(exchange);
        if (!AuthMiddleware.isAdmin(payload)) {
            if (payload == null) AuthMiddleware.sendUnauthorized(exchange);
            else AuthMiddleware.sendForbidden(exchange);
            return;
        }

        String complaintsJson = JsonUtil.readFile("complaints.json");
        List<String> complaints = parseJsonArray(complaintsJson);

        // zone breakdown
        Map<String, Integer> byZone     = new LinkedHashMap<>();
        Map<String, Integer> byCategory = new LinkedHashMap<>();
        Map<String, Integer> byStatus   = new LinkedHashMap<>();
        int total = complaints.size();

        for (String c : complaints) {
            String zone     = extractField(c, "zone");
            String category = extractField(c, "category");
            String status   = extractField(c, "status");

            if (zone     != null) byZone.merge(zone, 1, Integer::sum);
            if (category != null) byCategory.merge(category, 1, Integer::sum);
            if (status   != null) byStatus.merge(status, 1, Integer::sum);
        }

        String response = "{\"success\":true,\"message\":\"Complaints stats fetched\",\"data\":{"
            + "\"total\":" + total
            + ",\"by_zone\":" + mapToJson(byZone)
            + ",\"by_category\":" + mapToJson(byCategory)
            + ",\"by_status\":" + mapToJson(byStatus)
            + "}}";
        sendResponse(exchange, 200, response);
    }

    // GET /api/stats/work-orders — admin only
    private void handleWorkOrdersStats(HttpExchange exchange) throws IOException {
        String payload = AuthMiddleware.verify(exchange);
        if (!AuthMiddleware.isAdmin(payload)) {
            if (payload == null) AuthMiddleware.sendUnauthorized(exchange);
            else AuthMiddleware.sendForbidden(exchange);
            return;
        }

        String woJson = JsonUtil.readFile("work_orders.json");
        List<String> orders = parseJsonArray(woJson);

        Map<String, Integer> byStatus = new LinkedHashMap<>();
        Map<String, Integer> byZone   = new LinkedHashMap<>();
        int total = orders.size();

        for (String wo : orders) {
            String status = extractField(wo, "status");
            String zone   = extractField(wo, "zone");
            if (status != null) byStatus.merge(status, 1, Integer::sum);
            if (zone   != null) byZone.merge(zone, 1, Integer::sum);
        }

        String response = "{\"success\":true,\"message\":\"Work orders stats fetched\",\"data\":{"
            + "\"total\":" + total
            + ",\"by_status\":" + mapToJson(byStatus)
            + ",\"by_zone\":" + mapToJson(byZone)
            + "}}";
        sendResponse(exchange, 200, response);
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    // Map<String,Integer> ko JSON object string mein convert karo
    private String mapToJson(Map<String, Integer> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Integer> e : map.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(escapeJson(e.getKey())).append("\":").append(e.getValue());
            first = false;
        }
        sb.append("}");
        return sb.toString();
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

    private int countEntries(String json) {
        int count = 0;
        for (int i = 0; i < json.length() - 1; i++) if (json.charAt(i) == '{') count++;
        return count;
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
