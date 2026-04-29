// ScaeServer.java — SCAE ka main entry point
// ye file server start karti hai aur saare routes mount karti hai

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;

public class ScaeServer {

    public static void main(String[] args) throws Exception {

        // PORT environment variable se port read karo — Render ke liye zaroori hai
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8000"));

        // server create kiya gaya hai given port pe
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        // health check endpoint — sabse pehle test hota hai
        server.createContext("/api/health", new handlers.HealthHandler());

        // auth routes — login, profile
        server.createContext("/api/auth", new handlers.AuthHandler());

        // graph routes — nodes, edges
        server.createContext("/api/graph", new handlers.GraphHandler());

        // algorithm routes — dijkstra, knapsack, etc.
        server.createContext("/api/algo", new handlers.AlgoHandler());

        // complaints routes
        server.createContext("/api/complaints", new handlers.ComplaintHandler());

        // work orders routes
        server.createContext("/api/work-orders", new handlers.WorkOrderHandler());

        // emergencies routes
        server.createContext("/api/emergencies", new handlers.EmergencyHandler());

        // citizens routes — admin only
        server.createContext("/api/citizens", new handlers.CitizenHandler());

        // projects routes — budget knapsack ke liye
        server.createContext("/api/projects", new handlers.ProjectHandler());

        // notices routes — public announcements
        server.createContext("/api/notices", new handlers.NoticeHandler());

        // services routes — city services
        server.createContext("/api/services", new handlers.ServiceHandler());

        // stats routes — dashboard counters
        server.createContext("/api/stats", new handlers.StatsHandler());

        // default executor use ho raha hai
        server.setExecutor(null);

        // server start kar diya
        server.start();
        System.out.println("✅ SCAE Server started at http://localhost:" + port);
        System.out.println("📊 Algorithm Engine: C algorithms via JNI");
        System.out.println("💾 Storage: JSON files in data/ folder");
    }
}
