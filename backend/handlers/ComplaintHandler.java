// ComplaintHandler.java — /api/complaints/* routes handle karta hai
// GET  /api/complaints              → list (admin: all, citizen: apne)
// POST /api/complaints              → naya complaint create (citizen/admin)
// GET  /api/complaints/sorted       → urgency se sorted (heap sort via JNI)
// GET  /api/complaints/search       → KMP search on description
// GET  /api/complaints/{id}         → single complaint
// PUT  /api/complaints/{id}/status  → status update (admin/dept)
// DELETE /api/complaints/{id}       → delete (admin only)

package handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import utils.CorsUtil;
import utils.JsonUtil;
import middleware.AuthMiddleware;
import java.io.*;
import java.time.LocalDate;
import java.util.*;

public class ComplaintHandler implements HttpHandler {

    public void handle(HttpExchange exchange) throws IOException {
        CorsUtil.addCorsHeaders(exchange);
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        String path   = exchange.getRequestURI().getPath();
        String query  = exchange.getRequestURI().getRawQuery(); // query params
        String method = exchange.getRequestMethod();

        if (path.equals("/api/complaints") && method.equals("GET")) {
            handleList(exchange);

        } else if (path.equals("/api/complaints") && method.equals("POST")) {
            handleCreate(exchange);

        } else if (path.equals("/api/complaints/sorted") && method.equals("GET")) {
            handleSorted(exchange);

        } else if (path.equals("/api/complaints/search") && method.equals("GET")) {
            handleSearch(exchange, query);

        } else if (path.matches("/api/complaints/CMP-\\d+") && method.equals("GET")) {
            String id = path.replace("/api/complaints/", "");
            handleGetById(exchange, id);

        } else if (path.matches("/api/complaints/CMP-\\d+/status") && method.equals("PUT")) {
            String id = path.replace("/api/complaints/", "").replace("/status", "");
            handleUpdateStatus(exchange, id);

        } else if (path.matches("/api/complaints/CMP-\\d+") && method.equals("DELETE")) {
            String id = path.replace("/api/complaints/", "");
            handleDelete(exchange, id);

        } else {
            sendResponse(exchange, 404,
                "{\"success\":false,\"message\":\"Complaint route not found\"}");
        }
    }

    // GET /api/complaints — auth required, role-filtered
    private void handleList(HttpExchange exchange) throws IOException {
        String payload = AuthMiddleware.verify(exchange);
        if (payload == null) { AuthMiddleware.sendUnauthorized(exchange); return; }

        String allJson = JsonUtil.readFile("complaints.json");

        // admin/dept → saare complaints; citizen → sirf apne
        if (AuthMiddleware.isAdmin(payload) || AuthMiddleware.isDeptOrAdmin(payload)) {
            sendResponse(exchange, 200,
                "{\"success\":true,\"message\":\"Complaints fetched\",\"data\":" + allJson + "}");
        } else {
            // citizen ka sub (email) nikalo aur citizen_id match karo
            String email = extractPayloadField(payload, "sub");
            // complaints filter karo jahan citizen_id == email
            String filtered = filterByField(allJson, "citizen_id", email);
            sendResponse(exchange, 200,
                "{\"success\":true,\"message\":\"Your complaints fetched\",\"data\":" + filtered + "}");
        }
    }

    // POST /api/complaints — create new complaint
    private void handleCreate(HttpExchange exchange) throws IOException {
        String payload = AuthMiddleware.verify(exchange);
        if (payload == null) { AuthMiddleware.sendUnauthorized(exchange); return; }

        String body      = new String(exchange.getRequestBody().readAllBytes());
        String category  = extractField(body, "category");
        String desc      = extractField(body, "description");
        String zone      = extractField(body, "zone");
        String urgencyStr= extractField(body, "urgency");

        if (category == null || desc == null || zone == null) {
            sendResponse(exchange, 400,
                "{\"success\":false,\"message\":\"category, description, zone required hai\"}");
            return;
        }

        int urgency = urgencyStr != null ? parseInt(urgencyStr) : 5;
        String citizenId = extractPayloadField(payload, "sub");
        String date = LocalDate.now().toString();

        // existing complaints read karo → new id generate karo
        String existing = JsonUtil.readFile("complaints.json");
        int count = countEntries(existing);
        String newId = "CMP-" + String.format("%04d", count + 1);

        String newComplaint = "{\"id\":\"" + newId
            + "\",\"citizen_id\":\"" + citizenId
            + "\",\"category\":\"" + category
            + "\",\"description\":\"" + escapeJson(desc)
            + "\",\"urgency\":" + urgency
            + ",\"zone\":\"" + zone
            + "\",\"status\":\"Pending\""
            + ",\"date_filed\":\"" + date
            + "\",\"assigned_to\":null,\"notes\":\"\"}";

        String updated = appendToArray(existing, newComplaint);
        JsonUtil.writeFile("complaints.json", updated);

        sendResponse(exchange, 201,
            "{\"success\":true,\"message\":\"Complaint filed\",\"data\":" + newComplaint + "}");
    }

    // GET /api/complaints/sorted — heap sort by urgency (descending)
    private void handleSorted(HttpExchange exchange) throws IOException {
        String payload = AuthMiddleware.verify(exchange);
        if (payload == null) { AuthMiddleware.sendUnauthorized(exchange); return; }

        String allJson = JsonUtil.readFile("complaints.json");
        List<String> objects = parseJsonArray(allJson);

        // urgency values extract karke heap sort karo (manual — descending)
        int n = objects.size();
        int[] urgencies = new int[n];
        for (int i = 0; i < n; i++) {
            String u = extractField(objects.get(i), "urgency");
            urgencies[i] = u != null ? parseInt(u) : 0;
        }

        // heap sort descending — indices sort karo urgency by
        Integer[] idx = new Integer[n];
        for (int i = 0; i < n; i++) idx[i] = i;
        heapSortDesc(urgencies, idx, n);

        // sorted array banao
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < n; i++) {
            if (i > 0) sb.append(",");
            sb.append(objects.get(idx[i]));
        }
        sb.append("]");

        sendResponse(exchange, 200,
            "{\"success\":true,\"message\":\"Complaints sorted by urgency\",\"data\":"
            + sb + "}");
    }

    // GET /api/complaints/search?q=pothole — KMP on description
    private void handleSearch(HttpExchange exchange, String query) throws IOException {
        String payload = AuthMiddleware.verify(exchange);
        if (payload == null) { AuthMiddleware.sendUnauthorized(exchange); return; }

        String q = "";
        if (query != null) {
            for (String param : query.split("&")) {
                if (param.startsWith("q=")) {
                    q = param.substring(2).replace("+", " ");
                    break;
                }
            }
        }

        if (q.isEmpty()) {
            sendResponse(exchange, 400,
                "{\"success\":false,\"message\":\"q parameter required\"}");
            return;
        }

        String allJson  = JsonUtil.readFile("complaints.json");
        List<String> all = parseJsonArray(allJson);
        String pattern  = q.toLowerCase();

        // KMP search — description mein pattern dhundo
        List<String> matched = new ArrayList<>();
        for (String obj : all) {
            String desc = extractField(obj, "description");
            if (desc != null && kmpContains(desc.toLowerCase(), pattern)) {
                matched.add(obj);
            }
        }

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < matched.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(matched.get(i));
        }
        sb.append("]");

        sendResponse(exchange, 200,
            "{\"success\":true,\"message\":\"Search results for: " + escapeJson(q)
            + "\",\"count\":" + matched.size() + ",\"data\":" + sb + "}");
    }

    // GET /api/complaints/{id}
    private void handleGetById(HttpExchange exchange, String id) throws IOException {
        String payload = AuthMiddleware.verify(exchange);
        if (payload == null) { AuthMiddleware.sendUnauthorized(exchange); return; }

        String allJson = JsonUtil.readFile("complaints.json");
        String found   = findById(allJson, "id", id);

        if (found == null) {
            sendResponse(exchange, 404,
                "{\"success\":false,\"message\":\"Complaint " + id + " not found\"}");
            return;
        }

        sendResponse(exchange, 200,
            "{\"success\":true,\"message\":\"Complaint fetched\",\"data\":" + found + "}");
    }

    // PUT /api/complaints/{id}/status — admin/dept only
    private void handleUpdateStatus(HttpExchange exchange, String id) throws IOException {
        String payload = AuthMiddleware.verify(exchange);
        if (!AuthMiddleware.isDeptOrAdmin(payload)) {
            AuthMiddleware.sendForbidden(exchange); return;
        }

        String body   = new String(exchange.getRequestBody().readAllBytes());
        String status = extractField(body, "status");
        String notes  = extractField(body, "notes");
        String assignedTo = extractField(body, "assigned_to");

        if (status == null) {
            sendResponse(exchange, 400,
                "{\"success\":false,\"message\":\"status required hai\"}");
            return;
        }

        String allJson = JsonUtil.readFile("complaints.json");
        String updated = updateComplaintFields(allJson, id, status,
            notes != null ? notes : "", assignedTo);

        if (updated == null) {
            sendResponse(exchange, 404,
                "{\"success\":false,\"message\":\"Complaint " + id + " not found\"}");
            return;
        }

        JsonUtil.writeFile("complaints.json", updated);
        sendResponse(exchange, 200,
            "{\"success\":true,\"message\":\"Complaint " + id + " status updated to " + status + "\"}");
    }

    // DELETE /api/complaints/{id} — admin only
    private void handleDelete(HttpExchange exchange, String id) throws IOException {
        String payload = AuthMiddleware.verify(exchange);
        if (!AuthMiddleware.isAdmin(payload)) {
            AuthMiddleware.sendForbidden(exchange); return;
        }

        String allJson = JsonUtil.readFile("complaints.json");
        List<String> all = parseJsonArray(allJson);

        boolean removed = false;
        List<String> remaining = new ArrayList<>();
        for (String obj : all) {
            String objId = extractField(obj, "id");
            if (id.equals(objId)) { removed = true; }
            else { remaining.add(obj); }
        }

        if (!removed) {
            sendResponse(exchange, 404,
                "{\"success\":false,\"message\":\"Complaint " + id + " not found\"}");
            return;
        }

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < remaining.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(remaining.get(i));
        }
        sb.append("]");
        JsonUtil.writeFile("complaints.json", sb.toString());

        sendResponse(exchange, 200,
            "{\"success\":true,\"message\":\"Complaint " + id + " deleted\"}");
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    // JSON array string ko List<String> mein parse karo (top-level objects)
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

    // field value se filter karo (string field match)
    private String filterByField(String json, String field, String value) {
        List<String> all = parseJsonArray(json);
        List<String> matched = new ArrayList<>();
        for (String obj : all) {
            String v = extractField(obj, field);
            if (value != null && value.equals(v)) matched.add(obj);
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < matched.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(matched.get(i));
        }
        sb.append("]");
        return sb.toString();
    }

    // id field se single object dhundo
    private String findById(String json, String field, String value) {
        List<String> all = parseJsonArray(json);
        for (String obj : all) {
            String v = extractField(obj, field);
            if (value.equals(v)) return obj;
        }
        return null;
    }

    // complaint ka status, notes, assigned_to update karo
    private String updateComplaintFields(String json, String id, String status,
                                          String notes, String assignedTo) {
        List<String> all = parseJsonArray(json);
        boolean found = false;
        List<String> result = new ArrayList<>();
        for (String obj : all) {
            String objId = extractField(obj, "id");
            if (id.equals(objId)) {
                found = true;
                // status replace karo
                obj = replaceStringField(obj, "status", status);
                // notes replace karo
                obj = replaceStringField(obj, "notes", notes);
                // assigned_to handle karo (null ya string)
                if (assignedTo != null) {
                    obj = replaceStringField(obj, "assigned_to", assignedTo);
                }
            }
            result.add(obj);
        }
        if (!found) return null;
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < result.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(result.get(i));
        }
        sb.append("]");
        return sb.toString();
    }

    // JSON object mein string field ki value replace karo
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

    // payload JSON se field nikalo (simple, no nesting)
    private String extractPayloadField(String payload, String field) {
        return extractField(payload, field);
    }

    // JSON body/object se field nikalo (string ya number)
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

    // top-level JSON objects count karo
    private int countEntries(String json) {
        int count = 0;
        for (int i = 0; i < json.length() - 1; i++) {
            if (json.charAt(i) == '{') count++;
        }
        return count;
    }

    // JSON array mein element append karo
    private String appendToArray(String arrayJson, String newElement) {
        String trimmed = arrayJson.trim();
        if (trimmed.equals("[]")) return "[" + newElement + "]";
        int lastBracket = trimmed.lastIndexOf("]");
        return trimmed.substring(0, lastBracket) + "," + newElement + "]";
    }

    // string to int safely
    private int parseInt(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 0; }
    }

    // JSON string mein special chars escape karo
    private String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    // Heap sort descending — idx array ko urgencies by sort karo
    private void heapSortDesc(int[] vals, Integer[] idx, int n) {
        // max-heap banao
        for (int i = n / 2 - 1; i >= 0; i--) heapify(vals, idx, n, i);
        // elements extract karo
        for (int i = n - 1; i > 0; i--) {
            Integer tmp = idx[0]; idx[0] = idx[i]; idx[i] = tmp;
            heapify(vals, idx, i, 0);
        }
        // max-heap se sorted ascending milta hai → reverse for descending
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

    // KMP: text mein pattern hai ya nahi
    private boolean kmpContains(String text, String pattern) {
        if (pattern.isEmpty()) return true;
        int n = text.length(), m = pattern.length();
        int[] lps = buildLPS(pattern);
        int i = 0, j = 0;
        while (i < n) {
            if (text.charAt(i) == pattern.charAt(j)) { i++; j++; }
            if (j == m) return true;
            else if (i < n && text.charAt(i) != pattern.charAt(j)) {
                if (j != 0) j = lps[j - 1]; else i++;
            }
        }
        return false;
    }

    private int[] buildLPS(String pattern) {
        int m = pattern.length();
        int[] lps = new int[m];
        int len = 0, i = 1;
        while (i < m) {
            if (pattern.charAt(i) == pattern.charAt(len)) { lps[i++] = ++len; }
            else if (len != 0) { len = lps[len - 1]; }
            else { lps[i++] = 0; }
        }
        return lps;
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
