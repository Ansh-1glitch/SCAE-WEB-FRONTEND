// GraphHandler.java — /api/graph/* routes handle karta hai
// GET  /api/graph/nodes            → saare nodes return karo
// GET  /api/graph/edges            → saare edges return karo
// POST /api/graph/nodes            → naya node add karo (Admin only)
// POST /api/graph/edges            → naya edge add karo (Admin only)
// PUT  /api/graph/edges/{id}/close → edge band karo (Admin only)
// PUT  /api/graph/edges/{id}/open  → edge kholo (Admin only)

package handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import utils.CorsUtil;
import utils.JsonUtil;
import middleware.AuthMiddleware;
import java.io.*;

public class GraphHandler implements HttpHandler {

    public void handle(HttpExchange exchange) throws IOException {

        // CORS headers add karo
        CorsUtil.addCorsHeaders(exchange);
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        String path   = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        // GET /api/graph/nodes → saare city nodes return karo
        if (path.equals("/api/graph/nodes") && method.equals("GET")) {
            handleGetNodes(exchange);

        // GET /api/graph/edges → saare city edges return karo
        } else if (path.equals("/api/graph/edges") && method.equals("GET")) {
            handleGetEdges(exchange);

        // POST /api/graph/nodes → naya node add karo
        } else if (path.equals("/api/graph/nodes") && method.equals("POST")) {
            handleAddNode(exchange);

        // POST /api/graph/edges → naya edge add karo
        } else if (path.equals("/api/graph/edges") && method.equals("POST")) {
            handleAddEdge(exchange);

        // PUT /api/graph/edges/{id}/close → edge band karo
        } else if (path.matches("/api/graph/edges/\\d+/close") && method.equals("PUT")) {
            int edgeId = parseEdgeId(path, "/close");
            handleToggleEdge(exchange, edgeId, true);

        // PUT /api/graph/edges/{id}/open → edge kholo
        } else if (path.matches("/api/graph/edges/\\d+/open") && method.equals("PUT")) {
            int edgeId = parseEdgeId(path, "/open");
            handleToggleEdge(exchange, edgeId, false);

        } else {
            sendResponse(exchange, 404, "{\"success\":false,\"message\":\"Graph route not found\"}");
        }
    }

    // GET /api/graph/nodes — nodes.json padhke return karo
    private void handleGetNodes(HttpExchange exchange) throws IOException {
        String nodes = JsonUtil.readFile("nodes.json");
        String response = "{\"success\":true,\"message\":\"Nodes fetched\",\"data\":" + nodes + "}";
        sendResponse(exchange, 200, response);
    }

    // GET /api/graph/edges — edges.json padhke return karo
    private void handleGetEdges(HttpExchange exchange) throws IOException {
        String edges = JsonUtil.readFile("edges.json");
        String response = "{\"success\":true,\"message\":\"Edges fetched\",\"data\":" + edges + "}";
        sendResponse(exchange, 200, response);
    }

    // POST /api/graph/nodes — naya node add karo (Admin only)
    private void handleAddNode(HttpExchange exchange) throws IOException {

        // admin check karo
        String payload = AuthMiddleware.verify(exchange);
        if (!AuthMiddleware.isAdmin(payload)) {
            AuthMiddleware.sendForbidden(exchange);
            return;
        }

        // request body read karo
        String body = new String(exchange.getRequestBody().readAllBytes());
        String label = extractField(body, "label");
        String zone  = extractField(body, "zone");

        if (label == null || zone == null) {
            sendResponse(exchange, 400,
                "{\"success\":false,\"message\":\"label aur zone required hai\"}");
            return;
        }

        // nodes.json read karo aur naya node append karo
        String existing = JsonUtil.readFile("nodes.json");
        // nayi id = existing nodes ki count
        int newId = countEntries(existing);

        // default x,y values — frontend baad mein update kar sakta hai
        String xStr = extractField(body, "x");
        String yStr = extractField(body, "y");
        int x = xStr != null ? parseInt(xStr) : 300;
        int y = yStr != null ? parseInt(yStr) : 300;

        // naya node JSON banao
        String newNode = "{\"id\":" + newId + ",\"label\":\"" + label
            + "\",\"x\":" + x + ",\"y\":" + y + ",\"zone\":\"" + zone + "\"}";

        // existing array mein append karo
        String updated = appendToArray(existing, newNode);
        JsonUtil.writeFile("nodes.json", updated);

        String response = "{\"success\":true,\"message\":\"Node added\",\"data\":" + newNode + "}";
        sendResponse(exchange, 201, response);
    }

    // POST /api/graph/edges — naya edge add karo (Admin only)
    private void handleAddEdge(HttpExchange exchange) throws IOException {

        // admin check karo
        String payload = AuthMiddleware.verify(exchange);
        if (!AuthMiddleware.isAdmin(payload)) {
            AuthMiddleware.sendForbidden(exchange);
            return;
        }

        String body = new String(exchange.getRequestBody().readAllBytes());
        String nodeU   = extractField(body, "node_u");
        String nodeV   = extractField(body, "node_v");
        String weight  = extractField(body, "weight");

        if (nodeU == null || nodeV == null || weight == null) {
            sendResponse(exchange, 400,
                "{\"success\":false,\"message\":\"node_u, node_v, weight required hai\"}");
            return;
        }

        String existing = JsonUtil.readFile("edges.json");
        int newId = countEntries(existing);

        String newEdge = "{\"id\":" + newId + ",\"node_u\":" + parseInt(nodeU)
            + ",\"node_v\":" + parseInt(nodeV)
            + ",\"weight\":" + parseInt(weight) + ",\"is_closed\":false}";

        String updated = appendToArray(existing, newEdge);
        JsonUtil.writeFile("edges.json", updated);

        String response = "{\"success\":true,\"message\":\"Edge added\",\"data\":" + newEdge + "}";
        sendResponse(exchange, 201, response);
    }

    // PUT /api/graph/edges/{id}/close|open — edge ka is_closed toggle karo (Admin only)
    private void handleToggleEdge(HttpExchange exchange, int edgeId, boolean close) throws IOException {

        // admin check karo
        String payload = AuthMiddleware.verify(exchange);
        if (!AuthMiddleware.isAdmin(payload)) {
            AuthMiddleware.sendForbidden(exchange);
            return;
        }

        // edges.json read karo
        String edgesJson = JsonUtil.readFile("edges.json");

        // edge {id}: <edgeId> ka is_closed field update karo
        // format: "id": <N>, ... "is_closed": <bool>
        String updated = updateEdgeClosed(edgesJson, edgeId, close);
        if (updated == null) {
            sendResponse(exchange, 404,
                "{\"success\":false,\"message\":\"Edge " + edgeId + " not found\"}");
            return;
        }

        JsonUtil.writeFile("edges.json", updated);

        String action = close ? "closed" : "opened";
        String response = "{\"success\":true,\"message\":\"Edge " + edgeId + " " + action
            + "\",\"data\":{\"id\":" + edgeId + ",\"is_closed\":" + close + "}}";
        sendResponse(exchange, 200, response);
    }

    // helper: edges JSON mein specific id wale edge ka is_closed update karo
    private String updateEdgeClosed(String json, int id, boolean closed) {
        // "id" field dhundo — space ke saath ya bina dono handle karo
        String idKey = "\"id\":";
        int searchFrom = 0;
        while (searchFrom < json.length()) {
            int idIdx = json.indexOf(idKey, searchFrom);
            if (idIdx == -1) return null;

            // colon ke baad whitespace skip karo
            int numStart = idIdx + idKey.length();
            while (numStart < json.length() && json.charAt(numStart) == ' ') numStart++;

            // id number read karo
            int numEnd = numStart;
            while (numEnd < json.length() && Character.isDigit(json.charAt(numEnd))) numEnd++;

            // id match check karo
            try {
                int foundId = Integer.parseInt(json.substring(numStart, numEnd));
                if (foundId == id) {
                    // is_closed field is entry mein dhundo aur update karo
                    int closedIdx = json.indexOf("\"is_closed\":", idIdx);
                    if (closedIdx == -1) return null;
                    int valueStart = closedIdx + 12;
                    // whitespace skip karo
                    while (valueStart < json.length() && json.charAt(valueStart) == ' ') valueStart++;
                    // value end dhundo (comma ya closing brace)
                    int valueEnd = valueStart;
                    while (valueEnd < json.length() &&
                           json.charAt(valueEnd) != ',' && json.charAt(valueEnd) != '}') valueEnd++;
                    return json.substring(0, valueStart) + closed + json.substring(valueEnd);
                }
            } catch (NumberFormatException e) { /* skip */ }

            searchFrom = numEnd; // agle entry ki taraf jao
        }
        return null; // edge nahi mila
    }

    // helper: path se edge id nikalo — /api/graph/edges/3/close → 3
    private int parseEdgeId(String path, String suffix) {
        String trimmed = path.replace("/api/graph/edges/", "").replace(suffix, "");
        try { return Integer.parseInt(trimmed.trim()); } catch (Exception e) { return -1; }
    }

    // helper: JSON array mein entries count karo (top-level objects)
    private int countEntries(String json) {
        int count = 0;
        for (int i = 0; i < json.length() - 1; i++) {
            if (json.charAt(i) == '{') count++;
        }
        return count;
    }

    // helper: JSON array ke end mein naya element append karo
    private String appendToArray(String arrayJson, String newElement) {
        String trimmed = arrayJson.trim();
        if (trimmed.equals("[]")) return "[" + newElement + "]";
        // last ']' se pehle comma aur naya element add karo
        int lastBracket = trimmed.lastIndexOf("]");
        return trimmed.substring(0, lastBracket) + "," + newElement + "]";
    }

    // helper: JSON body se field nikalo
    private String extractField(String json, String field) {
        String key = "\"" + field + "\"";
        int idx = json.indexOf(key);
        if (idx == -1) return null;
        int colon = json.indexOf(":", idx + key.length());
        if (colon == -1) return null;
        // number ya string value dono handle karo
        int start = colon + 1;
        while (start < json.length() && json.charAt(start) == ' ') start++;
        if (start >= json.length()) return null;
        if (json.charAt(start) == '"') {
            int end = json.indexOf("\"", start + 1);
            return end == -1 ? null : json.substring(start + 1, end);
        } else {
            // number value
            int end = start;
            while (end < json.length() && "0123456789.-".indexOf(json.charAt(end)) >= 0) end++;
            return json.substring(start, end);
        }
    }

    // helper: string ko int mein convert karo safely
    private int parseInt(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 0; }
    }

    // helper: HTTP response bhejo
    private void sendResponse(HttpExchange exchange, int code, String body) throws IOException {
        byte[] bytes = body.getBytes("UTF-8");
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(code, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }
}
