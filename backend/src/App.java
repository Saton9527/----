import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class App {
    public static void main(String[] args) throws IOException {
        int port = 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/api/auth/login", App::handleLogin);
        server.createContext("/api/tasks", jsonHandler("""
            [
              {"id":1,"title":"基础动态规划热身","description":"完成 5 道 1200-1400 rating 的 DP 题目。","deadline":"2026-03-12 23:59","status":"PUBLISHED","totalProblems":5,"completedProblems":2},
              {"id":2,"title":"图论专题训练","description":"完成最短路与并查集专题，共 6 题。","deadline":"2026-03-15 20:00","status":"OVERDUE","totalProblems":6,"completedProblems":4},
              {"id":3,"title":"字符串专题","description":"KMP 与哈希基础练习。","deadline":"2026-03-18 20:00","status":"DONE","totalProblems":4,"completedProblems":4}
            ]
            """));
        server.createContext("/api/rankings/overall", jsonHandler("""
            [
              {"rankNo":1,"userName":"张三","totalPoints":248,"solvedCount":61,"streakDays":9},
              {"rankNo":2,"userName":"李四","totalPoints":221,"solvedCount":55,"streakDays":7},
              {"rankNo":3,"userName":"王五","totalPoints":198,"solvedCount":49,"streakDays":5},
              {"rankNo":4,"userName":"赵六","totalPoints":186,"solvedCount":46,"streakDays":6}
            ]
            """));
        server.createContext("/api/points/me/logs", jsonHandler("""
            [
              {"id":1,"sourceType":"TASK","reason":"完成图论专题任务","points":24,"createdAt":"2026-03-06 21:10"},
              {"id":2,"sourceType":"DAILY_AC","reason":"完成 3 道 1400 rating 题目","points":12,"createdAt":"2026-03-05 22:34"},
              {"id":3,"sourceType":"CONTEST","reason":"周赛排名奖励","points":18,"createdAt":"2026-03-04 22:00"}
            ]
            """));
        server.createContext("/api/dashboard/me/trend", jsonHandler("""
            [
              {"date":"03-01","solved":3},
              {"date":"03-02","solved":5},
              {"date":"03-03","solved":2},
              {"date":"03-04","solved":6},
              {"date":"03-05","solved":4},
              {"date":"03-06","solved":3},
              {"date":"03-07","solved":4}
            ]
            """));
        server.createContext("/api/recommendations/me", jsonHandler("""
            [
              {"id":1,"level":"WARMUP","problemCode":"CF 1607A","title":"Linear Keyboard"},
              {"id":2,"level":"CORE","problemCode":"CF 1851C","title":"Tiles Comeback"},
              {"id":3,"level":"CHALLENGE","problemCode":"CF 1899D","title":"Yarik and Musical Notes"}
            ]
            """));
        server.createContext("/api/alerts", jsonHandler("""
            [
              {"id":1,"userName":"李四","ruleCode":"RULE_1","riskLevel":"HIGH","hitTime":"2026-03-08 21:15","status":"OPEN"},
              {"id":2,"userName":"王五","ruleCode":"RULE_4","riskLevel":"MEDIUM","hitTime":"2026-03-08 20:00","status":"OPEN"}
            ]
            """));
        server.createContext("/api/students", jsonHandler("""
            [
              {"id":1,"realName":"张三","grade":"2023","major":"计算机科学与技术","handle":"zhangsan_cf","totalPoints":248},
              {"id":2,"realName":"李四","grade":"2023","major":"软件工程","handle":"lisi_cf","totalPoints":221},
              {"id":3,"realName":"王五","grade":"2024","major":"数据科学与大数据技术","handle":"wangwu_cf","totalPoints":198}
            ]
            """));

        server.setExecutor(null);
        server.start();
        System.out.println("Backend running on http://localhost:" + port);
    }

    private static HttpHandler jsonHandler(String body) {
        return exchange -> {
            if (isOptions(exchange)) {
                send(exchange, 204, "");
                return;
            }
            send(exchange, 200, body.trim());
        };
    }

    private static void handleLogin(HttpExchange exchange) throws IOException {
        if (isOptions(exchange)) {
            send(exchange, 204, "");
            return;
        }

        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            send(exchange, 405, "{\"message\":\"Method Not Allowed\"}");
            return;
        }

        String body = readBody(exchange);
        String username = extractField(body, "username");
        if (username == null || username.isBlank()) {
            username = "student";
        }

        String role = username.toLowerCase().contains("coach") ? "coach" : "student";
        int id = "coach".equals(role) ? 1 : 2;
        String realName = "coach".equals(role) ? "演示教练" : "演示学生";

        String resp = "{" +
                "\"token\":\"demo-token\"," +
                "\"user\":{" +
                "\"id\":" + id + "," +
                "\"username\":\"" + escapeJson(username) + "\"," +
                "\"realName\":\"" + realName + "\"," +
                "\"role\":\"" + role + "\"}" +
                "}";

        send(exchange, 200, resp);
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        InputStream in = exchange.getRequestBody();
        return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }

    private static String extractField(String json, String field) {
        Pattern p = Pattern.compile("\"" + Pattern.quote(field) + "\"\\s*:\\s*\"([^\"]*)\"");
        Matcher m = p.matcher(json == null ? "" : json);
        return m.find() ? m.group(1) : null;
    }

    private static boolean isOptions(HttpExchange exchange) {
        return "OPTIONS".equalsIgnoreCase(exchange.getRequestMethod());
    }

    private static void send(HttpExchange exchange, int statusCode, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Type", "application/json; charset=utf-8");
        headers.set("Access-Control-Allow-Origin", "*");
        headers.set("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
        headers.set("Access-Control-Allow-Headers", "Content-Type,Authorization");

        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
