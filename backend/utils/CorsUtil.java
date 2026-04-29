// CorsUtil.java — CORS headers add karne ka helper
// frontend Netlify pe hai — usse allow karna zaroori hai

package utils;

import com.sun.net.httpserver.HttpExchange;

public class CorsUtil {

    // frontend ka URL — ye Netlify pe hai
    private static final String FRONTEND_URL =
        System.getenv().getOrDefault("FRONTEND_URL",
        "https://bucolic-biscotti-dfb4d5.netlify.app");

    public static void addCorsHeaders(HttpExchange exchange) {

        // local testing ke liye har origin ko allow karo
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");

        // ye methods allow hai
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods",
            "GET, POST, PUT, PATCH, DELETE, OPTIONS");

        // ye headers allow hai — Authorization JWT ke liye chahiye
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers",
            "Content-Type, Authorization");

        // response cache ho sakta hai 1 ghante ke liye
        exchange.getResponseHeaders().set("Access-Control-Max-Age", "3600");
    }
}
