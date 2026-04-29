// EmergencyHandler.java — /api/emergencies/* routes handle karta hai
// GET  /api/emergencies              → list all
// POST /api/emergencies              → create new (admin/dept)
// GET  /api/emergencies/sorted       → urgency descending (heap sort)
// POST /api/emergencies/generate     → 10 random emergencies add karo (admin)
// GET  /api/emergencies/dispatch-next → highest urgency Queued wala dispatch karo
// GET  /api/emergencies/{id}         → single by id
// PUT  /api/emergencies/{id}/resolve → mark Resolved (admin/dept)
// DELETE /api/emergencies/{id}       → delete (admin only)

package handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import utils.CorsUtil;
import utils.JsonUtil;
import middleware.AuthMiddleware;
import java.io.*;
import java.util.*;

public class EmergencyHandler implements HttpHandler {

    private static final String[] TYPES = {
        "Road Accident", "Fire Outbreak", "Power Failure", "Flood Alert",
        "Water Burst", "Gas Leak", "Building Collapse", "Medical Emergency",
        "Traffic Jam", "Landslide"
    };
    private static final String[] ZONES = {"North", "South", "East", "West", "Central"};
    private static final String[] CREWS = {
        "Crew Alpha", "Crew Beta", "Crew Gamma", "Crew Delta", "Crew Echo"
    };

    public void handle(HttpExchange exchange) throws IOException {
        CorsUtil.addCorsHeaders(exchange);
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        String path   = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        if (path.equals("/api/emergencies") && method.equals("GET")) {
            handleList(exchange);

        } else if (path.equals("/api/emergencies") && method.equals("POST")) {
            handleCreate(exchange);

        } else if (path.equals("/api/emergencies/sorted") && method.equals("GET")) {
            handleSorted(exchange);

        } else if (path.equals("/api/emergencies/generate") && method.equals("POST")) {
            handleGenerate(exchange);

        } else if (path.equals("/api/emergencies/dispatch-next") && method.equals("GET")) {
            handleDispatchNext(exchange);

        } else if (path.matches("/api/emergencies/\\d+") && method.equals("GET")) {
            int id = parseId(path.replace("/api/emergencies/", ""));
            handleGetById(exchange, id);

        } else if (path.matches("/api/emergencies/\\d+/resolve") && method.equals("PUT")) {
            int id = parseId(path.replace("/api/emergencies/", "").replace("/resolve", ""));
            handleResolve(exchange, id);

        } else if (path.matches("/api/emergencies/\\d+") && method.equals("DELETE")) {
            int id = parseId(path.replace("/api/emergencies/", ""));
            handleDelete(exchange, id);

        } else {
            sendResponse(exchange, 404,
                "{\"success\":false,\"message\":\"Emergency route not found\"}");
        }
    }

    // GET /api/emergencies — auth required
    private void handleList(HttpExchange exchange) throws IOException {
        String payload = AuthMiddleware.verify(exchange);
        if (payload == null) { AuthMiddleware.sendUnauthorized(exchange); return; }

        String allJson = JsonUtil.readFile("emergencies.json");
        sendResponse(exchange, 200,
            "{\"success\":true,\"message\":\"Emergencies fetched\",\"data\":" + allJson + "}");
    }

    // POST /api/emergencies — admin/dept
    private void handleCreate(HttpExchange exchange) throws IOException {
        String payload = AuthMiddleware.verify(exchange);
        if (!AuthMiddleware.isDeptOrAdmin(payload)) {
            if (payload == null) AuthMiddleware.sendUnauthorized(exchange);
            else AuthMiddleware.sendForbidden(exchange);
            return;
        }

        String body    = new String(exchange.getRequestBody().readAllBytes());
        String type    = extractField(body, "type");
        String zone    = extractField(body, "zone");
        String urgStr  = extractField(body, "urgency");
        String time    = extractField(body, "time");
        String crew    = extractField(body, "crew");

        if (type == null || zone == null) {
            sendResponse(exchange, 400,
                "{\"success\":false,\"message\":\"type aur zone required hai\"}");
            return;
        }

        int urgency = urgStr != null ? parseInt(urgStr) : 5;

        String existing = JsonUtil.readFile("emergencies.json");
        int newId = countEntries(existing) + 1;

        String timeVal = time != null ? time : currentTime();
        String newEmergency = "{\"id\":" + newId
            + ",\"type\":\"" + escapeJson(type)
            + "\",\"zone\":\"" + zone
            + "\",\"urgency\":" + urgency
            + ",\"time\":\"" + timeVal
            + "\",\"crew\":" + (crew != null ? "\"" + escapeJson(crew) + "\"" : "null")
            + ",\"status\":\"Queued\"}";

        String updated = appendToArray(existing, newEmergency);
        JsonUtil.writeFile("emergencies.json", updated);

        sendResponse(exchange, 201,
            "{\"success\":true,\"message\":\"Emergency created\",\"data\":" + newEmergency + "}");
    }

    // GET /api/emergencies/sorted — heap sort by urgency descending
    private void handleSorted(HttpExchange exchange) throws IOException {
        String payload = AuthMiddleware.verify(exchange);
        if (payload == null) { AuthMiddleware.sendUnauthorized(exchange); return; }

        String allJson = JsonUtil.readFile("emergencies.json");
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
            "{\"success\":true,\"message\":\"Emergencies sorted by urgency\",\"data\":" + sb + "}");
    }

    // POST /api/emergencies/generate — 10 random emergencies add karo (admin)
    private void handleGenerate(HttpExchange exchange) throws IOException {
        String payload = AuthMiddleware.verify(exchange);
        if (!AuthMiddleware.isAdmin(payload)) {
            if (payload == null) AuthMiddleware.sendUnauthorized(exchange);
            else AuthMiddleware.sendForbidden(exchange);
            return;
        }

        String existing = JsonUtil.readFile("emergencies.json");
        int startId = countEntries(existing) + 1;
        Random rnd = new Random();

        List<String> generated = new ArrayList<>();
        StringBuilder sb = new StringBuilder(existing.trim());

        for (int i = 0; i < 10; i++) {
            int id      = startId + i;
            String type = TYPES[rnd.nextInt(TYPES.length)];
            String zone = ZONES[rnd.nextInt(ZONES.length)];
            int urgency = 1 + rnd.nextInt(10); // 1-10
            String time = String.format("%02d:%02d", rnd.nextInt(24), rnd.nextInt(60));
            String crew = rnd.nextBoolean() ? CREWS[rnd.nextInt(CREWS.length)] : null;

            String obj = "{\"id\":" + id
                + ",\"type\":\"" + type
                + "\",\"zone\":\"" + zone
                + "\",\"urgency\":" + urgency
                + ",\"time\":\"" + time
                + "\",\"crew\":" + (crew != null ? "\"" + crew + "\"" : "null")
                + ",\"status\":\"Queued\"}";
            generated.add(obj);
        }

        // existing array mein sab append karo
        String current = existing.trim();
        for (String obj : generated) {
            current = appendToArray(current, obj);
        }
        JsonUtil.writeFile("emergencies.json", current);

        StringBuilder result = new StringBuilder("[");
        for (int i = 0; i < generated.size(); i++) {
            if (i > 0) result.append(",");
            result.append(generated.get(i));
        }
        result.append("]");

        sendResponse(exchange, 201,
            "{\"success\":true,\"message\":\"10 random emergencies generated\",\"data\":"
            + result + "}");
    }

    // GET /api/emergencies/dispatch-next — highest urgency Queued emergency dispatch karo
    private void handleDispatchNext(HttpExchange exchange) throws IOException {
        String payload = AuthMiddleware.verify(exchange);
        if (!AuthMiddleware.isDeptOrAdmin(payload)) {
            if (payload == null) AuthMiddleware.sendUnauthorized(exchange);
            else AuthMiddleware.sendForbidden(exchange);
            return;
        }

        String allJson = JsonUtil.readFile("emergencies.json");
        List<String> all = parseJsonArray(allJson);

        // Queued mein se highest urgency
        String topObj  = null;
        int topId      = -1;
        int maxUrgency = -1;

        for (String obj : all) {
            String status = extractField(obj, "status");
            if (!"Queued".equals(status)) continue;
            String u = extractField(obj, "urgency");
            int urgency = u != null ? parseInt(u) : 0;
            if (urgency > maxUrgency) {
                maxUrgency = urgency;
                topId = parseId(extractIntField(obj, "id"));
                topObj = obj;
            }
        }

        if (topObj == null) {
            sendResponse(exchange, 200,
                "{\"success\":true,\"message\":\"No queued emergencies\",\"data\":null}");
            return;
        }

        // status → Dispatched update karo + random crew assign karo if null
        String crew = extractField(topObj, "crew");
        if (crew == null) {
            crew = CREWS[new Random().nextInt(CREWS.length)];
        }

        List<String> updated = new ArrayList<>();
        for (String obj : all) {
            String idStr = extractIntField(obj, "id");
            if (topId == parseId(idStr)) {
                obj = replaceField(obj, "status", "Dispatched", false);
                obj = replaceField(obj, "crew", crew, true);
            }
            updated.add(obj);
        }
        JsonUtil.writeFile("emergencies.json", buildArray(updated));

        // return updated object
        String updatedTop = replaceField(replaceField(topObj, "status", "Dispatched", false),
            "crew", crew, true);
        sendResponse(exchange, 200,
            "{\"success\":true,\"message\":\"Emergency dispatched\",\"urgency\":"
            + maxUrgency + ",\"data\":" + updatedTop + "}");
    }

    // GET /api/emergencies/{id}
    private void handleGetById(HttpExchange exchange, int id) throws IOException {
        String payload = AuthMiddleware.verify(exchange);
        if (payload == null) { AuthMiddleware.sendUnauthorized(exchange); return; }

        String allJson = JsonUtil.readFile("emergencies.json");
        List<String> all = parseJsonArray(allJson);

        for (String obj : all) {
            if (id == parseId(extractIntField(obj, "id"))) {
                sendResponse(exchange, 200,
                    "{\"success\":true,\"message\":\"Emergency fetched\",\"data\":" + obj + "}");
                return;
            }
        }
        sendResponse(exchange, 404,
            "{\"success\":false,\"message\":\"Emergency " + id + " not found\"}");
    }

    // PUT /api/emergencies/{id}/resolve — admin/dept
    private void handleResolve(HttpExchange exchange, int id) throws IOException {
        String payload = AuthMiddleware.verify(exchange);
        if (!AuthMiddleware.isDeptOrAdmin(payload)) {
            if (payload == null) AuthMiddleware.sendUnauthorized(exchange);
            else AuthMiddleware.sendForbidden(exchange);
            return;
        }

        String allJson = JsonUtil.readFile("emergencies.json");
        List<String> all = parseJsonArray(allJson);
        boolean found = false;
        List<String> result = new ArrayList<>();

        for (String obj : all) {
            if (id == parseId(extractIntField(obj, "id"))) {
                found = true;
                obj = replaceField(obj, "status", "Resolved", false);
            }
            result.add(obj);
        }

        if (!found) {
            sendResponse(exchange, 404,
                "{\"success\":false,\"message\":\"Emergency " + id + " not found\"}");
            return;
        }

        JsonUtil.writeFile("emergencies.json", buildArray(result));
        sendResponse(exchange, 200,
            "{\"success\":true,\"message\":\"Emergency " + id + " resolved\"}");
    }

    // DELETE /api/emergencies/{id} — admin only
    private void handleDelete(HttpExchange exchange, int id) throws IOException {
        String payload = AuthMiddleware.verify(exchange);
        if (!AuthMiddleware.isAdmin(payload)) {
            if (payload == null) AuthMiddleware.sendUnauthorized(exchange);
            else AuthMiddleware.sendForbidden(exchange);
            return;
        }

        String allJson = JsonUtil.readFile("emergencies.json");
        List<String> all = parseJsonArray(allJson);
        boolean removed = false;
        List<String> remaining = new ArrayList<>();

        for (String obj : all) {
            if (id == parseId(extractIntField(obj, "id"))) removed = true;
            else remaining.add(obj);
        }

        if (!removed) {
            sendResponse(exchange, 404,
                "{\"success\":false,\"message\":\"Emergency " + id + " not found\"}");
            return;
        }

        JsonUtil.writeFile("emergencies.json", buildArray(remaining));
        sendResponse(exchange, 200,
            "{\"success\":true,\"message\":\"Emergency " + id + " deleted\"}");
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

    // numeric id field extract karo (int stored as number, not string)
    private String extractIntField(String obj, String field) {
        String key = "\"" + field + "\":";
        int idx = obj.indexOf(key);
        if (idx == -1) return "0";
        int start = idx + key.length();
        while (start < obj.length() && obj.charAt(start) == ' ') start++;
        int end = start;
        while (end < obj.length() && Character.isDigit(obj.charAt(end))) end++;
        return obj.substring(start, end);
    }

    // string field extract karo (value quoted ya null)
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

    // field value replace karo — isString=true hone par quotes lagao
    private String replaceField(String obj, String field, String newValue, boolean isString) {
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
        String replacement = isString ? "\"" + escapeJson(newValue) + "\"" : newValue;
        return obj.substring(0, idx + key.length()) + replacement + obj.substring(valueEnd);
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

    private int countEntries(String json) {
        int count = 0;
        for (int i = 0; i < json.length() - 1; i++) {
            if (json.charAt(i) == '{') count++;
        }
        return count;
    }

    private int parseId(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return -1; }
    }

    private int parseInt(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 0; }
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String currentTime() {
        java.time.LocalTime t = java.time.LocalTime.now();
        return String.format("%02d:%02d", t.getHour(), t.getMinute());
    }

    private void heapSortDesc(int[] vals, Integer[] idx, int n) {
        for (int i = n / 2 - 1; i >= 0; i--) heapify(vals, idx, n, i);
        for (int i = n - 1; i > 0; i--) {
            Integer tmp = idx[0]; idx[0] = idx[i]; idx[i] = tmp;
            heapify(vals, idx, i, 0);
        }
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
