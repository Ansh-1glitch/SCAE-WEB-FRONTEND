// HealthHandler.java — server ka health check endpoint
// GET /api/health → server chalra hai ya nahi check karta hai

package handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import utils.CorsUtil;
import java.io.*;

public class HealthHandler implements HttpHandler {

    public void handle(HttpExchange exchange) throws IOException {

        // CORS headers add karo — frontend se request aayegi
        CorsUtil.addCorsHeaders(exchange);

        // OPTIONS request handle karo (preflight)
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        // health response banao
        String response = "{"
            + "\"success\": true,"
            + "\"message\": \"SCAE Server is running\","
            + "\"data\": {"
            +   "\"status\": \"ok\","
            +   "\"server\": \"SCAE Java Backend\","
            +   "\"algorithms\": \"C via JNI\","
            +   "\"storage\": \"JSON files\","
            +   "\"timestamp\": \"" + new java.util.Date() + "\""
            + "}"
            + "}";

        // response bhejo
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }
}
