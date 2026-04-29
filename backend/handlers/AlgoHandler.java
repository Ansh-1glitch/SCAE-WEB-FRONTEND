// AlgoHandler.java — /api/algo/* algorithm endpoints handle karta hai
// ye handler C algorithms ko JNI ke zariye call karta hai
// macOS: libscae.dylib | Linux: libscae.so

package handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import utils.*;
import middleware.AuthMiddleware;
import java.io.*;

public class AlgoHandler implements HttpHandler {

    // JNI library load karega — libscae.dylib (mac) ya libscae.so (linux)
    // java.library.path se dhundha jata hai — run command mein set karo
    private static boolean jniLoaded = false;
    static {
        try {
            System.loadLibrary("scae");
            jniLoaded = true;
            System.out.println("✅ C Algorithm Library loaded via JNI");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("⚠️  JNI Library load nahi hua: " + e.getMessage());
            System.err.println("   Run with: java -Djava.library.path=. ScaeServer");
        }
    }

    // native methods — C mein implement hain ScaeJNI.c mein
    private static native int[] runDijkstra(int[] graph, int V, int src, int dst, int skipU, int skipV);
    private static native int[] runBFS(int[] graph, int V, int src, int maxHops);
    private static native int[] runBellmanFord(int[] edgesU, int[] edgesV, int[] edgesW,
                                                int E, int V, int src, int dst, int skipU, int skipV);
    private static native int[] runFloydWarshall(int[] graph, int V);
    private static native int[] runKruskal(int[] edgesU, int[] edgesV, int[] edgesW, int E, int V);
    private static native int[] runPrim(int[] graph, int V);
    private static native int[] runDFSComponents(int[] graph, int V, int[] disabled, int disabledCount);
    // Prompt 5 native methods
    private static native int[]  runKnapsack01(int[] costs, int[] benefits, int n, int W);
    private static native int[]  runHuffman(String input);
    private static native int[]  runKMP(String text, String pattern);
    private static native int[]  runJobScheduler(int[] deadlines, int[] profits, int n);
    private static native long[] runBenchmark(int n);

    public void handle(HttpExchange exchange) throws IOException {

        CorsUtil.addCorsHeaders(exchange);
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        String path   = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        // POST /api/algo/dijkstra — shortest path
        if (path.equals("/api/algo/dijkstra") && method.equals("POST")) {
            handleDijkstra(exchange);

        // POST /api/algo/bfs — zone reachability
        } else if (path.equals("/api/algo/bfs") && method.equals("POST")) {
            handleBFS(exchange);

        // POST /api/algo/bellman-ford — road closure alternate route
        } else if (path.equals("/api/algo/bellman-ford") && method.equals("POST")) {
            handleBellmanFord(exchange);

        // GET /api/algo/floyd-warshall — all pairs matrix (Admin only)
        } else if (path.equals("/api/algo/floyd-warshall") && method.equals("GET")) {
            String payload = AuthMiddleware.verify(exchange);
            if (!AuthMiddleware.isAdmin(payload)) { AuthMiddleware.sendForbidden(exchange); return; }
            handleFloydWarshall(exchange);

        // GET /api/algo/mst/kruskal — MST (Admin only)
        } else if (path.equals("/api/algo/mst/kruskal") && method.equals("GET")) {
            String payload = AuthMiddleware.verify(exchange);
            if (!AuthMiddleware.isAdmin(payload)) { AuthMiddleware.sendForbidden(exchange); return; }
            handleMST(exchange, "kruskal");

        // GET /api/algo/mst/prim — MST alternate (Admin only)
        } else if (path.equals("/api/algo/mst/prim") && method.equals("GET")) {
            String payload = AuthMiddleware.verify(exchange);
            if (!AuthMiddleware.isAdmin(payload)) { AuthMiddleware.sendForbidden(exchange); return; }
            handleMST(exchange, "prim");

        // POST /api/algo/dfs/components — disaster zone isolation (Admin only)
        } else if (path.equals("/api/algo/dfs/components") && method.equals("POST")) {
            String payload = AuthMiddleware.verify(exchange);
            if (!AuthMiddleware.isAdmin(payload)) { AuthMiddleware.sendForbidden(exchange); return; }
            handleDFSComponents(exchange);

        // POST /api/algo/knapsack — budget project selection (DP)
        } else if (path.equals("/api/algo/knapsack") && method.equals("POST")) {
            handleKnapsack(exchange);

        // POST /api/algo/huffman — sensor data compression (Greedy)
        } else if (path.equals("/api/algo/huffman") && method.equals("POST")) {
            handleHuffman(exchange);

        // POST /api/algo/kmp — complaint text search (String Matching)
        } else if (path.equals("/api/algo/kmp") && method.equals("POST")) {
            handleKMP(exchange);

        // POST /api/algo/job-scheduler — emergency dispatch scheduling (Greedy)
        } else if (path.equals("/api/algo/job-scheduler") && method.equals("POST")) {
            handleJobScheduler(exchange);

        // GET /api/algo/benchmark — sorting algorithm comparison (Admin)
        } else if (path.equals("/api/algo/benchmark") && method.equals("GET")) {
            String payload = AuthMiddleware.verify(exchange);
            if (!AuthMiddleware.isAdmin(payload)) { AuthMiddleware.sendForbidden(exchange); return; }
            handleBenchmark(exchange);

        } else {
            sendResponse(exchange, 404,
                "{\"success\":false,\"message\":\"Algorithm endpoint not found\"}");
        }
    }

    // =============================================
    // DIJKSTRA — POST /api/algo/dijkstra
    // Body: { "source": 0, "destination": 9, "skip_edge_u": -1, "skip_edge_v": -1 }
    // =============================================
    private void handleDijkstra(HttpExchange exchange) throws IOException {
        if (!jniLoaded) { sendJniError(exchange); return; }
        String body = new String(exchange.getRequestBody().readAllBytes());
        int src   = extractInt(body, "source");
        int dst   = extractInt(body, "destination");
        int skipU = extractIntOr(body, "skip_edge_u", -1);
        int skipV = extractIntOr(body, "skip_edge_v", -1);

        // edges.json se adjacency matrix banao
        int V = getNodeCount();
        int[] graph = buildGraphMatrix(V);

        // C function call karo
        int[] result = runDijkstra(graph, V, src, dst, skipU, skipV);
        int distance = result[0];
        int pathLen  = result[1];

        // path array JSON mein convert karo
        StringBuilder pathArr = new StringBuilder("[");
        for (int i = 0; i < pathLen; i++) {
            if (i > 0) pathArr.append(",");
            pathArr.append(result[2 + i]);
        }
        pathArr.append("]");

        // node labels bhi attach karo
        String[] labels = getNodeLabels(V);
        StringBuilder pathLabels = new StringBuilder("[");
        for (int i = 0; i < pathLen; i++) {
            if (i > 0) pathLabels.append(",");
            int nodeId = result[2 + i];
            pathLabels.append("\"").append(nodeId < labels.length ? labels[nodeId] : "Node " + nodeId).append("\"");
        }
        pathLabels.append("]");

        String response = "{\"success\":true,\"message\":\"Dijkstra completed\","
            + "\"data\":{\"path\":" + pathArr
            + ",\"path_labels\":" + pathLabels
            + ",\"distance\":" + distance
            + ",\"source\":" + src
            + ",\"destination\":" + dst
            + ",\"algorithm\":\"Dijkstra\""
            + ",\"complexity\":\"O((V+E) log V)\"}}";
        sendResponse(exchange, 200, response);
    }

    // =============================================
    // BFS — POST /api/algo/bfs
    // Body: { "source": 0, "max_hops": 2 }
    // =============================================
    private void handleBFS(HttpExchange exchange) throws IOException {
        if (!jniLoaded) { sendJniError(exchange); return; }
        String body = new String(exchange.getRequestBody().readAllBytes());
        int src     = extractInt(body, "source");
        int maxHops = extractIntOr(body, "max_hops", 2);

        int V = getNodeCount();
        int[] graph = buildGraphMatrix(V);

        int[] result = runBFS(graph, V, src, maxHops);
        int count = result[0];

        // reachable nodes JSON mein
        StringBuilder nodesArr = new StringBuilder("[");
        String[] labels = getNodeLabels(V);
        for (int i = 0; i < count; i++) {
            if (i > 0) nodesArr.append(",");
            int nodeId = result[1 + i];
            nodesArr.append("{\"id\":").append(nodeId)
                    .append(",\"label\":\"").append(nodeId < labels.length ? labels[nodeId] : "Node " + nodeId)
                    .append("\"}");
        }
        nodesArr.append("]");

        String response = "{\"success\":true,\"message\":\"BFS completed\","
            + "\"data\":{\"reachable_nodes\":" + nodesArr
            + ",\"count\":" + count
            + ",\"source\":" + src
            + ",\"max_hops\":" + maxHops
            + ",\"algorithm\":\"BFS\""
            + ",\"complexity\":\"O(V+E)\"}}";
        sendResponse(exchange, 200, response);
    }

    // =============================================
    // BELLMAN-FORD — POST /api/algo/bellman-ford
    // Body: { "source": 0, "destination": 9, "skip_edge_u": 0, "skip_edge_v": 1 }
    // =============================================
    private void handleBellmanFord(HttpExchange exchange) throws IOException {
        if (!jniLoaded) { sendJniError(exchange); return; }
        String body = new String(exchange.getRequestBody().readAllBytes());
        int src   = extractInt(body, "source");
        int dst   = extractInt(body, "destination");
        int skipU = extractIntOr(body, "skip_edge_u", -1);
        int skipV = extractIntOr(body, "skip_edge_v", -1);

        int V = getNodeCount();
        int[][] edgeArrays = buildEdgeArrays();
        int[] eu = edgeArrays[0], ev = edgeArrays[1], ew = edgeArrays[2];
        int E = eu.length;

        int[] result = runBellmanFord(eu, ev, ew, E, V, src, dst, skipU, skipV);
        int distance = result[0];
        int pathLen  = result[1];

        StringBuilder pathArr = new StringBuilder("[");
        for (int i = 0; i < pathLen; i++) {
            if (i > 0) pathArr.append(",");
            pathArr.append(result[2 + i]);
        }
        pathArr.append("]");

        String response = "{\"success\":true,\"message\":\"Bellman-Ford completed\","
            + "\"data\":{\"path\":" + pathArr
            + ",\"distance\":" + distance
            + ",\"skipped_edge\":{\"u\":" + skipU + ",\"v\":" + skipV + "}"
            + ",\"algorithm\":\"Bellman-Ford\""
            + ",\"complexity\":\"O(VE)\"}}";
        sendResponse(exchange, 200, response);
    }

    // =============================================
    // FLOYD-WARSHALL — GET /api/algo/floyd-warshall (Admin)
    // =============================================
    private void handleFloydWarshall(HttpExchange exchange) throws IOException {
        if (!jniLoaded) { sendJniError(exchange); return; }
        int V = getNodeCount();
        int[] graph = buildGraphMatrix(V);
        int[] matrix = runFloydWarshall(graph, V);

        // V×V matrix ko nested array JSON mein convert karo
        StringBuilder matrixJson = new StringBuilder("[");
        for (int i = 0; i < V; i++) {
            if (i > 0) matrixJson.append(",");
            matrixJson.append("[");
            for (int j = 0; j < V; j++) {
                if (j > 0) matrixJson.append(",");
                int val = matrix[i * V + j];
                // INF (99999) ko null se replace karo
                matrixJson.append(val >= 99999 ? "null" : val);
            }
            matrixJson.append("]");
        }
        matrixJson.append("]");

        String[] labels = getNodeLabels(V);
        StringBuilder labelsJson = new StringBuilder("[");
        for (int i = 0; i < V; i++) {
            if (i > 0) labelsJson.append(",");
            labelsJson.append("\"").append(labels[i]).append("\"");
        }
        labelsJson.append("]");

        String response = "{\"success\":true,\"message\":\"Floyd-Warshall completed\","
            + "\"data\":{\"matrix\":" + matrixJson
            + ",\"labels\":" + labelsJson
            + ",\"nodes\":" + V
            + ",\"algorithm\":\"Floyd-Warshall\""
            + ",\"complexity\":\"O(V^3)\"}}";
        sendResponse(exchange, 200, response);
    }

    // =============================================
    // MST — GET /api/algo/mst/kruskal | /api/algo/mst/prim (Admin)
    // =============================================
    private void handleMST(HttpExchange exchange, String algo) throws IOException {
        if (!jniLoaded) { sendJniError(exchange); return; }

        int[] result;
        int V = getNodeCount();

        if ("kruskal".equals(algo)) {
            int[][] edgeArrays = buildEdgeArrays();
            int[] eu = edgeArrays[0], ev = edgeArrays[1], ew = edgeArrays[2];
            result = runKruskal(eu, ev, ew, eu.length, V);
        } else {
            int[] graph = buildGraphMatrix(V);
            result = runPrim(graph, V);
        }

        int totalCost = result[0];
        int edgeCount = result[1];

        String[] labels = getNodeLabels(V);
        StringBuilder edgesJson = new StringBuilder("[");
        for (int i = 0; i < edgeCount; i++) {
            if (i > 0) edgesJson.append(",");
            int u = result[2 + i*3];
            int v = result[2 + i*3 + 1];
            int w = result[2 + i*3 + 2];
            String uLabel = u < labels.length ? labels[u] : "Node " + u;
            String vLabel = v < labels.length ? labels[v] : "Node " + v;
            edgesJson.append("{\"u\":").append(u)
                     .append(",\"v\":").append(v)
                     .append(",\"weight\":").append(w)
                     .append(",\"u_label\":\"").append(uLabel).append("\"")
                     .append(",\"v_label\":\"").append(vLabel).append("\"")
                     .append("}");
        }
        edgesJson.append("]");

        String algoName = "kruskal".equals(algo) ? "Kruskal" : "Prim";
        String response = "{\"success\":true,\"message\":\"MST (" + algoName + ") completed\","
            + "\"data\":{\"mst_edges\":" + edgesJson
            + ",\"total_cost\":" + totalCost
            + ",\"edge_count\":" + edgeCount
            + ",\"algorithm\":\"" + algoName + "\""
            + ",\"complexity\":\"" + ("kruskal".equals(algo) ? "O(E log E)" : "O((V+E) log V)") + "\"}}";
        sendResponse(exchange, 200, response);
    }

    // =============================================
    // DFS COMPONENTS — POST /api/algo/dfs/components (Admin)
    // Body: { "disabled_nodes": [3, 7] }
    // =============================================
    private void handleDFSComponents(HttpExchange exchange) throws IOException {
        if (!jniLoaded) { sendJniError(exchange); return; }
        String body = new String(exchange.getRequestBody().readAllBytes());

        // disabled_nodes array nikalo
        int[] disabled = extractIntArray(body, "disabled_nodes");
        int V = getNodeCount();
        int[] graph = buildGraphMatrix(V);

        int[] result = runDFSComponents(graph, V, disabled, disabled.length);
        int compCount = result[0];

        String[] labels = getNodeLabels(V);
        StringBuilder compsJson = new StringBuilder("[");
        int pos = 1;
        for (int c = 0; c < compCount; c++) {
            if (c > 0) compsJson.append(",");
            int sz = result[pos++];
            compsJson.append("{\"size\":").append(sz).append(",\"nodes\":[");
            for (int i = 0; i < sz; i++) {
                if (i > 0) compsJson.append(",");
                int nodeId = result[pos++];
                String lbl = nodeId < labels.length ? labels[nodeId] : "Node " + nodeId;
                compsJson.append("{\"id\":").append(nodeId)
                         .append(",\"label\":\"").append(lbl).append("\"}");
            }
            compsJson.append("]}");
        }
        compsJson.append("]");

        StringBuilder disabledJson = new StringBuilder("[");
        for (int i = 0; i < disabled.length; i++) {
            if (i > 0) disabledJson.append(",");
            disabledJson.append(disabled[i]);
        }
        disabledJson.append("]");

        String response = "{\"success\":true,\"message\":\"DFS Components completed\","
            + "\"data\":{\"components\":" + compsJson
            + ",\"component_count\":" + compCount
            + ",\"disabled_nodes\":" + disabledJson
            + ",\"algorithm\":\"DFS\""
            + ",\"complexity\":\"O(V+E)\"}}";
        sendResponse(exchange, 200, response);
    }

    // =============================================
    // HELPERS — graph data build karne ke liye
    // =============================================

    // nodes.json se node count nikalo
    private int getNodeCount() throws IOException {
        String nodesJson = JsonUtil.readFile("nodes.json");
        int count = 0;
        for (int i = 0; i < nodesJson.length() - 1; i++) {
            if (nodesJson.charAt(i) == '{') count++;
        }
        return count;
    }

    // nodes.json se labels array banao
    private String[] getNodeLabels(int V) throws IOException {
        String nodesJson = JsonUtil.readFile("nodes.json");
        String[] labels = new String[V];
        for (int i = 0; i < V; i++) labels[i] = "Node " + i;
        // "label": "..." pattern dhundo
        int searchFrom = 0;
        int nodeIdx = 0;
        while (nodeIdx < V) {
            int labelIdx = nodesJson.indexOf("\"label\":", searchFrom);
            if (labelIdx == -1) break;
            int start = nodesJson.indexOf("\"", labelIdx + 8) + 1;
            int end   = nodesJson.indexOf("\"", start);
            if (start > 0 && end > start && nodeIdx < V) {
                labels[nodeIdx++] = nodesJson.substring(start, end);
            }
            searchFrom = end + 1;
        }
        return labels;
    }

    // edges.json se adjacency matrix banao (open edges only)
    private int[] buildGraphMatrix(int V) throws IOException {
        String edgesJson = JsonUtil.readFile("edges.json");
        int[] matrix = new int[V * V];

        int searchFrom = 0;
        while (true) {
            // ek edge object dhundo
            int objStart = edgesJson.indexOf("{", searchFrom);
            if (objStart == -1) break;
            int objEnd = edgesJson.indexOf("}", objStart);
            if (objEnd == -1) break;
            String edge = edgesJson.substring(objStart, objEnd + 1);

            // is_closed check karo — band edge matrix mein nahi jayegi
            boolean isClosed = edge.contains("\"is_closed\": true") || edge.contains("\"is_closed\":true");

            if (!isClosed) {
                // node_u, node_v, weight nikalo
                int u = extractIntFromStr(edge, "node_u");
                int v = extractIntFromStr(edge, "node_v");
                int w = extractIntFromStr(edge, "weight");
                if (u >= 0 && v >= 0 && u < V && v < V && w > 0) {
                    // undirected graph — dono directions set karo
                    matrix[u * V + v] = w;
                    matrix[v * V + u] = w;
                }
            }
            searchFrom = objEnd + 1;
        }
        return matrix;
    }

    // edges.json se edge arrays banao (Bellman-Ford / Kruskal ke liye)
    private int[][] buildEdgeArrays() throws IOException {
        String edgesJson = JsonUtil.readFile("edges.json");
        int[] tempU = new int[200], tempV = new int[200], tempW = new int[200];
        int count = 0;

        int searchFrom = 0;
        while (true) {
            int objStart = edgesJson.indexOf("{", searchFrom);
            if (objStart == -1) break;
            int objEnd = edgesJson.indexOf("}", objStart);
            if (objEnd == -1) break;
            String edge = edgesJson.substring(objStart, objEnd + 1);

            boolean isClosed = edge.contains("\"is_closed\": true") || edge.contains("\"is_closed\":true");
            if (!isClosed) {
                tempU[count] = extractIntFromStr(edge, "node_u");
                tempV[count] = extractIntFromStr(edge, "node_v");
                tempW[count] = extractIntFromStr(edge, "weight");
                if (tempW[count] > 0) count++;
            }
            searchFrom = objEnd + 1;
        }

        int[] eu = new int[count], ev = new int[count], ew = new int[count];
        System.arraycopy(tempU, 0, eu, 0, count);
        System.arraycopy(tempV, 0, ev, 0, count);
        System.arraycopy(tempW, 0, ew, 0, count);
        return new int[][]{eu, ev, ew};
    }

    // helper: JSON string mein ek edge object se int field nikalo
    private int extractIntFromStr(String edgeJson, String field) {
        String key = "\"" + field + "\":";
        int idx = edgeJson.indexOf(key);
        if (idx == -1) {
            // space ke saath bhi try karo
            key = "\"" + field + "\": ";
            idx = edgeJson.indexOf(key);
            if (idx == -1) return -1;
        }
        int start = idx + key.length();
        while (start < edgeJson.length() && edgeJson.charAt(start) == ' ') start++;
        int end = start;
        while (end < edgeJson.length() && (Character.isDigit(edgeJson.charAt(end)) || edgeJson.charAt(end) == '-')) end++;
        try { return Integer.parseInt(edgeJson.substring(start, end)); } catch (Exception e) { return -1; }
    }

    // helper: JSON body se int field nikalo
    private int extractInt(String json, String field) {
        String key = "\"" + field + "\"";
        int idx = json.indexOf(key);
        if (idx == -1) return 0;
        int colon = json.indexOf(":", idx + key.length());
        if (colon == -1) return 0;
        int start = colon + 1;
        while (start < json.length() && json.charAt(start) == ' ') start++;
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) end++;
        try { return Integer.parseInt(json.substring(start, end)); } catch (Exception e) { return 0; }
    }

    private int extractIntOr(String json, String field, int def) {
        String key = "\"" + field + "\"";
        if (!json.contains(key)) return def;
        return extractInt(json, field);
    }

    // helper: JSON array field nikalo (e.g. "disabled_nodes": [3, 7])
    private int[] extractIntArray(String json, String field) {
        String key = "\"" + field + "\"";
        int idx = json.indexOf(key);
        if (idx == -1) return new int[0];
        int arrStart = json.indexOf("[", idx);
        int arrEnd   = json.indexOf("]", arrStart);
        if (arrStart == -1 || arrEnd == -1) return new int[0];
        String inner = json.substring(arrStart + 1, arrEnd).trim();
        if (inner.isEmpty()) return new int[0];
        String[] parts = inner.split(",");
        int[] result = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try { result[i] = Integer.parseInt(parts[i].trim()); } catch (Exception e) { result[i] = 0; }
        }
        return result;
    }

    // helper: JNI load fail hone pe error response
    // =============================================
    // KNAPSACK — POST /api/algo/knapsack
    // Body: { "budget": 300, "project_ids": ["p001","p002",...] }
    //   OR  { "budget": 300 }  (uses all active projects from projects.json)
    // =============================================
    private void handleKnapsack(HttpExchange exchange) throws IOException {
        if (!jniLoaded) { sendJniError(exchange); return; }
        String body   = new String(exchange.getRequestBody().readAllBytes());
        int budget    = extractIntOr(body, "budget", 300);

        // projects.json se saare active projects read karo
        String projectsJson = JsonUtil.readFile("projects.json");

        // project ids, costs, benefits parse karo
        String[] ids = new String[50];
        String[] names = new String[50];
        int[] costs = new int[50], benefits = new int[50];
        int n = parseProjects(projectsJson, ids, names, costs, benefits);

        if (n == 0) { sendResponse(exchange, 400, "{\"success\":false,\"message\":\"No projects found\"}"); return; }

        // C Knapsack call karo
        int[] result = runKnapsack01(costs, benefits, n, budget);
        int maxBenefit = result[0];

        // selected projects JSON banao
        StringBuilder selectedArr = new StringBuilder("[");
        int totalCost = 0;
        boolean first = true;
        for (int i = 0; i < n; i++) {
            if (result[2 + i] == 1) {
                if (!first) selectedArr.append(",");
                selectedArr.append("{\"id\":\"").append(ids[i]).append("\"")
                           .append(",\"name\":\"").append(names[i]).append("\"")
                           .append(",\"cost\":").append(costs[i])
                           .append(",\"benefit\":").append(benefits[i]).append("}");
                totalCost += costs[i];
                first = false;
            }
        }
        selectedArr.append("]");

        String response = "{\"success\":true,\"message\":\"Knapsack 0/1 completed\","
            + "\"data\":{\"selected_projects\":" + selectedArr
            + ",\"max_benefit\":" + maxBenefit
            + ",\"total_cost\":" + totalCost
            + ",\"budget\":" + budget
            + ",\"algorithm\":\"0/1 Knapsack DP\""
            + ",\"complexity\":\"O(nW)\"}}";
        sendResponse(exchange, 200, response);
    }

    // helper: projects.json parse karo
    private int parseProjects(String json, String[] ids, String[] names, int[] costs, int[] benefits) {
        int count = 0;
        int searchFrom = 0;
        while (count < 50) {
            int objStart = json.indexOf("{", searchFrom);
            if (objStart == -1) break;
            int objEnd = json.indexOf("}", objStart);
            if (objEnd == -1) break;
            String obj = json.substring(objStart, objEnd + 1);
            if (obj.contains("\"active\": false") || obj.contains("\"active\":false")) {
                searchFrom = objEnd + 1; continue;
            }
            ids[count]      = extractStrFromObj(obj, "id");
            names[count]    = extractStrFromObj(obj, "name");
            costs[count]    = extractIntFromStr(obj, "cost");
            benefits[count] = extractIntFromStr(obj, "benefit");
            if (costs[count] > 0) count++;
            searchFrom = objEnd + 1;
        }
        return count;
    }

    // helper: object string se string field nikalo
    private String extractStrFromObj(String obj, String field) {
        String key = "\"" + field + "\":";
        int idx = obj.indexOf(key);
        if (idx == -1) { key = "\"" + field + "\": "; idx = obj.indexOf(key); }
        if (idx == -1) return "";
        int start = idx + key.length();
        while (start < obj.length() && (obj.charAt(start) == ' ' || obj.charAt(start) == '"')) {
            if (obj.charAt(start) == '"') { start++; break; }
            start++;
        }
        int end = obj.indexOf("\"", start);
        return end == -1 ? "" : obj.substring(start, end);
    }

    // =============================================
    // HUFFMAN — POST /api/algo/huffman
    // Body: { "input": "aabbbcccdddeeeffgg" }
    //   OR  { "sensor_id": 1 }  (uses sensor_logs.json)
    // =============================================
    private void handleHuffman(HttpExchange exchange) throws IOException {
        if (!jniLoaded) { sendJniError(exchange); return; }
        String body = new String(exchange.getRequestBody().readAllBytes());

        // input string nikalo — body se ya sensor_logs.json se
        String input = extractStrFromBody(body, "input");
        if (input == null || input.isEmpty()) {
            // sensor_logs.json se pehla entry ka raw_data lo
            String logsJson = JsonUtil.readFile("sensor_logs.json");
            input = extractStrFromObj(logsJson, "raw_data");
        }
        if (input == null || input.isEmpty()) input = "aabbbcccdddeeeffgg"; // default

        // C Huffman call karo
        int[] result = runHuffman(input);
        int originalBits    = result[0];
        int compressedBits  = result[1];
        int uniqueCount     = result[2];

        // character table JSON banao
        StringBuilder tableJson = new StringBuilder("[");
        for (int i = 0; i < uniqueCount; i++) {
            if (i > 0) tableJson.append(",");
            int freq    = result[3 + i*3];
            int charVal = result[3 + i*3 + 1];
            int codeLen = result[3 + i*3 + 2];
            tableJson.append("{\"char\":\"").append((char)charVal).append("\"")
                     .append(",\"freq\":").append(freq)
                     .append(",\"code_length\":").append(codeLen).append("}");
        }
        tableJson.append("]");

        double ratio = originalBits > 0 ? (double)compressedBits / originalBits * 100 : 100;
        String response = "{\"success\":true,\"message\":\"Huffman encoding completed\","
            + "\"data\":{\"input\":\"" + input + "\""
            + ",\"original_bits\":" + originalBits
            + ",\"compressed_bits\":" + compressedBits
            + ",\"compression_ratio\":\"" + String.format("%.1f", ratio) + "%\""
            + ",\"space_saved\":\"" + String.format("%.1f", 100 - ratio) + "%\""
            + ",\"unique_chars\":" + uniqueCount
            + ",\"char_table\":" + tableJson
            + ",\"algorithm\":\"Huffman Coding\""
            + ",\"complexity\":\"O(n log n)\"}}";
        sendResponse(exchange, 200, response);
    }

    // helper: JSON body se string field nikalo (quotes ke saath)
    private String extractStrFromBody(String json, String field) {
        String key = "\"" + field + "\"";
        int idx = json.indexOf(key);
        if (idx == -1) return null;
        int colon = json.indexOf(":", idx + key.length());
        if (colon == -1) return null;
        int start = colon + 1;
        while (start < json.length() && json.charAt(start) != '"') start++;
        if (start >= json.length()) return null;
        start++; // opening quote skip karo
        int end = json.indexOf("\"", start);
        return end == -1 ? null : json.substring(start, end);
    }

    // =============================================
    // KMP — POST /api/algo/kmp
    // Body: { "text": "pothole near north gate", "pattern": "north" }
    //   OR  { "pattern": "water", "search_in": "complaints" }
    // =============================================
    private void handleKMP(HttpExchange exchange) throws IOException {
        if (!jniLoaded) { sendJniError(exchange); return; }
        String body    = new String(exchange.getRequestBody().readAllBytes());
        String pattern = extractStrFromBody(body, "pattern");
        String text    = extractStrFromBody(body, "text");

        // agar text nahi diya toh complaints.json se search karo
        if (text == null || text.isEmpty()) {
            String complaintsJson = JsonUtil.readFile("complaints.json");
            // saare descriptions combine karo
            StringBuilder combined = new StringBuilder();
            int from = 0;
            while (true) {
                int s = complaintsJson.indexOf("{", from); if (s == -1) break;
                int e = complaintsJson.indexOf("}", s);    if (e == -1) break;
                String obj = complaintsJson.substring(s, e + 1);
                String desc = extractStrFromObj(obj, "description");
                if (!desc.isEmpty()) combined.append(desc).append(" ");
                from = e + 1;
            }
            text = combined.toString().trim();
        }
        if (pattern == null || pattern.isEmpty()) {
            sendResponse(exchange, 400, "{\"success\":false,\"message\":\"pattern required hai\"}"); return;
        }
        if (text.isEmpty()) text = "no text available";

        // C KMP call karo
        int[] result   = runKMP(text, pattern);
        int matchCount = result[0];
        int comparisons = result[1];

        StringBuilder posArr = new StringBuilder("[");
        for (int i = 0; i < matchCount; i++) {
            if (i > 0) posArr.append(",");
            posArr.append(result[2 + i]);
        }
        posArr.append("]");

        String response = "{\"success\":true,\"message\":\"KMP search completed\","
            + "\"data\":{\"pattern\":\"" + pattern + "\""
            + ",\"text_length\":" + text.length()
            + ",\"match_count\":" + matchCount
            + ",\"positions\":" + posArr
            + ",\"comparisons\":" + comparisons
            + ",\"algorithm\":\"KMP\""
            + ",\"complexity\":\"O(n+m)\"}}";
        sendResponse(exchange, 200, response);
    }

    // =============================================
    // JOB SCHEDULER — POST /api/algo/job-scheduler
    // Body: { "jobs": [{"id":"WO-001","deadline":3,"profit":8}, ...] }
    //   OR  {}  (uses work_orders.json)
    // =============================================
    private void handleJobScheduler(HttpExchange exchange) throws IOException {
        if (!jniLoaded) { sendJniError(exchange); return; }
        String body = new String(exchange.getRequestBody().readAllBytes());

        // work_orders.json se jobs read karo
        String woJson = JsonUtil.readFile("work_orders.json");
        String[] woIds = new String[50];
        String[] woTitles = new String[50];
        int[] deadlines = new int[50], profits = new int[50];
        int n = parseWorkOrders(woJson, woIds, woTitles, deadlines, profits);

        if (n == 0) { sendResponse(exchange, 400, "{\"success\":false,\"message\":\"No work orders found\"}"); return; }

        // C Job Scheduler call karo
        int[] result = runJobScheduler(deadlines, profits, n);
        int scheduledCount = result[0];
        int totalProfit    = result[1];

        StringBuilder ganttJson = new StringBuilder("[");
        for (int i = 0; i < scheduledCount; i++) {
            if (i > 0) ganttJson.append(",");
            int jobIdx = result[2 + i];
            ganttJson.append("{\"slot\":").append(i + 1)
                     .append(",\"job_id\":\"").append(jobIdx < woIds.length ? woIds[jobIdx] : "J" + jobIdx).append("\"")
                     .append(",\"title\":\"").append(jobIdx < woTitles.length ? woTitles[jobIdx] : "Job " + jobIdx).append("\"")
                     .append(",\"deadline\":").append(jobIdx < n ? deadlines[jobIdx] : 0)
                     .append(",\"urgency\":").append(jobIdx < n ? profits[jobIdx] : 0)
                     .append("}");
        }
        ganttJson.append("]");

        String response = "{\"success\":true,\"message\":\"Job Scheduling completed\","
            + "\"data\":{\"gantt_chart\":" + ganttJson
            + ",\"scheduled_count\":" + scheduledCount
            + ",\"total_urgency_score\":" + totalProfit
            + ",\"total_jobs\":" + n
            + ",\"algorithm\":\"Job Sequencing (Greedy)\""
            + ",\"complexity\":\"O(n^2)\"}}";
        sendResponse(exchange, 200, response);
    }

    // helper: work_orders.json parse karo
    private int parseWorkOrders(String json, String[] ids, String[] titles, int[] deadlines, int[] profits) {
        int count = 0;
        int from = 0;
        while (count < 50) {
            int s = json.indexOf("{", from); if (s == -1) break;
            int e = json.indexOf("}", s);   if (e == -1) break;
            String obj = json.substring(s, e + 1);
            ids[count]       = extractStrFromObj(obj, "id");
            titles[count]    = extractStrFromObj(obj, "title");
            profits[count]   = extractIntFromStr(obj, "urgency");
            // deadline = urgency level ko deadline convert karo (1-5 range)
            int urgency      = profits[count];
            deadlines[count] = urgency >= 9 ? 1 : urgency >= 7 ? 2 : urgency >= 5 ? 3 : 4;
            if (profits[count] > 0) count++;
            from = e + 1;
        }
        return count;
    }

    // =============================================
    // BENCHMARK — GET /api/algo/benchmark?n=1000 (Admin only)
    // =============================================
    private void handleBenchmark(HttpExchange exchange) throws IOException {
        if (!jniLoaded) { sendJniError(exchange); return; }

        // query string se n nikalo
        String query = exchange.getRequestURI().getQuery();
        int n = 1000; // default
        if (query != null && query.contains("n=")) {
            try { n = Integer.parseInt(query.split("n=")[1].split("&")[0].trim()); }
            catch (Exception e) { n = 1000; }
        }
        if (n > 50000) n = 50000; // cap karo — bahut bada nahi

        // C benchmark call karo
        long[] result = runBenchmark(n);
        long actualN = result[0];

        String[] algoNames = {"Merge Sort", "Quick Sort", "Heap Sort", "Shell Sort",
                              "Insertion Sort", "Selection Sort", "Counting Sort"};
        String[] complexities = {"O(n log n)", "O(n log n) avg", "O(n log n)",
                                 "O(n log^2 n)", "O(n^2)", "O(n^2)", "O(n+k)"};

        StringBuilder timesJson = new StringBuilder("[");
        for (int i = 0; i < 7; i++) {
            if (i > 0) timesJson.append(",");
            timesJson.append("{\"algorithm\":\"").append(algoNames[i]).append("\"")
                     .append(",\"time_us\":").append(result[i + 1])
                     .append(",\"complexity\":\"").append(complexities[i]).append("\"")
                     .append("}");
        }
        timesJson.append("]");

        String response = "{\"success\":true,\"message\":\"Benchmark completed\","
            + "\"data\":{\"n\":" + actualN
            + ",\"results\":" + timesJson
            + ",\"unit\":\"microseconds\"}}";
        sendResponse(exchange, 200, response);
    }

    private void sendJniError(HttpExchange exchange) throws IOException {
        sendResponse(exchange, 503,
            "{\"success\":false,\"message\":\"C library load nahi hua — run with -Djava.library.path=.\"}");
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
