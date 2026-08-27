import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class DashboardServer {
    private static final List<Transaction> transactions = new ArrayList<>();
    private static final List<Budget> budgets = new ArrayList<>();
    private static final AtomicInteger ids = new AtomicInteger(1000);
    private static final Path FRONTEND = Path.of("dashboard");

    public static void main(String[] args) throws IOException {
        seedData();
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/api", DashboardServer::api);
        server.createContext("/", DashboardServer::staticFile);
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();
        System.out.println("ExpenseFlow is running at http://localhost:8080");
    }

    private static synchronized void api(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        if (path.equals("/api/transactions")) {
            switch (method) {
                case "GET" -> respond(exchange, 200, transactionsJson());
                case "POST" -> {
                    Map<String, String> data = bodyFields(exchange);
                    if (data.getOrDefault("merchant", "").isBlank() || data.getOrDefault("amount", "").isBlank() || number(data.get("amount"), 0) <= 0) { respond(exchange, 400, "{\"error\":\"Merchant and a positive amount are required\"}"); return; }
                    Transaction tx = Transaction.from(data, ids.incrementAndGet()); transactions.add(0, tx);
                    respond(exchange, 201, tx.json());
                }
                default -> respond(exchange, 405, "{\"error\":\"Method not allowed\"}");
            }
            return;
        }
        if (path.startsWith("/api/transactions/")) {
            int id = Integer.parseInt(path.substring(path.lastIndexOf('/') + 1));
            int index = find(id);
            if (index < 0) { respond(exchange, 404, "{\"error\":\"Transaction not found\"}"); return; }
            switch (method) {
                case "DELETE" -> { transactions.remove(index); respond(exchange, 204, ""); }
                case "PUT" -> { transactions.set(index, Transaction.from(bodyFields(exchange), id)); respond(exchange, 200, transactions.get(index).json()); }
                default -> respond(exchange, 405, "{\"error\":\"Method not allowed\"}");
            }
            return;
        }
        switch (path) {
            case "/api/dashboard" -> respond(exchange, 200, dashboardJson());
            case "/api/analytics" -> respond(exchange, 200, analyticsJson());
            case "/api/categories" -> respond(exchange, 200, categoriesJson());
            case "/api/services" -> respond(exchange, 200, servicesJson());
            case "/api/budgets" -> {
                if (method.equals("POST")) { Map<String, String> d = bodyFields(exchange); budgets.add(new Budget(d.getOrDefault("name", "New budget"), number(d.get("limit"), 5000))); }
                respond(exchange, 200, budgetsJson());
            }
            case "/api/subscriptions" -> respond(exchange, 200, subscriptionsJson());
            case "/api/reports" -> respond(exchange, 200, reportJson());
            default -> respond(exchange, 404, "{\"error\":\"Unknown endpoint\"}");
        }
    }

    private static void staticFile(HttpExchange exchange) throws IOException {
        String requested = exchange.getRequestURI().getPath();
        if (requested.equals("/")) requested = "/index.html";
        Path file = FRONTEND.resolve(requested.substring(1)).normalize();
        if (!file.startsWith(FRONTEND) || !Files.exists(file) || Files.isDirectory(file)) { respond(exchange, 404, "Not found"); return; }
        String type = requested.endsWith(".css") ? "text/css" : requested.endsWith(".js") ? "application/javascript" : "text/html";
        byte[] content = Files.readAllBytes(file);
        exchange.getResponseHeaders().set("Content-Type", type + "; charset=utf-8"); exchange.sendResponseHeaders(200, content.length);
        try (OutputStream out = exchange.getResponseBody()) { out.write(content); }
    }

    private static String dashboardJson() {
        double total = transactions.stream().mapToDouble(t -> t.amount).sum();
        double today = transactions.stream().filter(t -> t.date.startsWith("2026-08")).mapToDouble(t -> t.amount).sum();
        return "{\"total\":%s,\"month\":%s,\"daily\":%s,\"subscriptions\":5,\"budgetRemaining\":%s,\"count\":%d,\"change\":12.4}".formatted(total, today, total / 30, 60000 - total, transactions.size());
    }
    private static String analyticsJson() { return "{\"monthly\":[6100,7420,6890,8240,9110,10380,8750,11240,9680,12100,10840,12450],\"daily\":[320,540,275,810,460,620,390,920,510,740,430,650,380,710],\"categories\":%s,\"websites\":%s}".formatted(categoriesJson(), servicesJson()); }
    private static String categoriesJson() { Map<String, Double> map = new LinkedHashMap<>(); transactions.forEach(t -> map.merge(t.category, t.amount, Double::sum)); return mapJson(map); }
    private static String servicesJson() { Map<String, Double> map = new LinkedHashMap<>(); transactions.forEach(t -> map.merge(t.website, t.amount, Double::sum)); return mapJson(map); }
    private static String subscriptionsJson() { return "[{\"service\":\"Netflix\",\"amount\":649,\"cycle\":\"Monthly\",\"next\":\"Sep 03, 2026\",\"annual\":7788},{\"service\":\"JioHotstar\",\"amount\":149,\"cycle\":\"Monthly\",\"next\":\"Sep 08, 2026\",\"annual\":1788},{\"service\":\"Spotify\",\"amount\":119,\"cycle\":\"Monthly\",\"next\":\"Sep 12, 2026\",\"annual\":1428},{\"service\":\"Apple\",\"amount\":99,\"cycle\":\"Monthly\",\"next\":\"Sep 15, 2026\",\"annual\":1188},{\"service\":\"YouTube\",\"amount\":149,\"cycle\":\"Monthly\",\"next\":\"Sep 22, 2026\",\"annual\":1788}]"; }
    private static String reportJson() { return "{\"total\":%s,\"transactions\":%d,\"generated\":\"Aug 27, 2026\"}".formatted(transactions.stream().mapToDouble(t -> t.amount).sum(), transactions.size()); }
    private static String budgetsJson() { List<String> out = new ArrayList<>(); for (Budget b : budgets) { double spent = transactions.stream().filter(t -> t.category.equalsIgnoreCase(b.name)).mapToDouble(t -> t.amount).sum(); out.add(b.json(spent)); } return "[" + String.join(",", out) + "]"; }
    private static String transactionsJson() { return "[" + String.join(",", transactions.stream().map(Transaction::json).toList()) + "]"; }
    private static String mapJson(Map<String, Double> map) { return "{" + String.join(",", map.entrySet().stream().map(e -> quote(e.getKey()) + ":" + String.format("%.2f", e.getValue())).toList()) + "}"; }
    private static int find(int id) { for (int i = 0; i < transactions.size(); i++) if (transactions.get(i).id == id) return i; return -1; }
    private static double number(String value, double fallback) { try { return Double.parseDouble(value); } catch (NumberFormatException ignored) { return fallback; } }
    private static Map<String, String> bodyFields(HttpExchange exchange) throws IOException { String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8); Map<String, String> map = new LinkedHashMap<>(); for (String part : body.split("&")) { String[] pair = part.split("=", 2); if (pair.length == 2) map.put(URLDecoder.decode(pair[0], StandardCharsets.UTF_8), URLDecoder.decode(pair[1], StandardCharsets.UTF_8)); } return map; }
    private static String quote(String value) { return "\"" + value.replace("\"", "\\\"") + "\""; }
    private static void respond(HttpExchange exchange, int status, String body) throws IOException { byte[] bytes = body.getBytes(StandardCharsets.UTF_8); exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*"); exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8"); exchange.sendResponseHeaders(status, bytes.length); try (OutputStream out = exchange.getResponseBody()) { out.write(bytes); } }

    private static void seedData() { String[][] rows = { {"2026-08-26","Amazon","Shopping","1299","UPI"},{"2026-08-25","Zomato","Food","420","UPI"},{"2026-08-24","Uber","Transport","340","Card"},{"2026-08-23","Netflix","Subscriptions","649","Card"},{"2026-08-22","Flipkart","Shopping","2499","UPI"},{"2026-08-21","Swiggy","Food","680","UPI"},{"2026-08-20","Apple","Technology","1999","Card"},{"2026-08-19","Spotify","Subscriptions","119","Card"},{"2026-08-18","Myntra","Shopping","1890","UPI"},{"2026-08-17","JioHotstar","Subscriptions","149","Card"},{"2026-08-16","Ola","Transport","280","UPI"},{"2026-08-15","Ajio","Shopping","2200","UPI"},{"2026-08-14","YouTube","Subscriptions","149","Card"},{"2026-08-13","Google","Technology","799","UPI"},{"2026-08-12","Microsoft","Technology","899","Card"},{"2026-08-11","Amazon","Shopping","899","UPI"},{"2026-08-10","Zomato","Food","560","UPI"},{"2026-08-09","Swiggy","Food","380","Card"},{"2026-08-08","Netflix","Subscriptions","649","Card"},{"2026-08-07","Uber","Transport","510","UPI"},{"2026-08-06","Amazon","Shopping","2450","Card"},{"2026-08-05","Flipkart","Shopping","1299","UPI"},{"2026-08-04","Spotify","Subscriptions","119","Card"},{"2026-08-03","Apple","Technology","499","UPI"},{"2026-08-02","Ola","Transport","330","UPI"},{"2026-08-01","Zomato","Food","740","Card"},{"2026-07-30","Amazon","Shopping","3499","UPI"},{"2026-07-28","Swiggy","Food","620","UPI"},{"2026-07-25","Myntra","Shopping","1599","Card"},{"2026-07-20","JioHotstar","Subscriptions","149","Card"},{"2026-07-18","Uber","Transport","420","UPI"},{"2026-07-12","Google","Technology","1299","UPI"},{"2026-07-09","Amazon","Shopping","799","Card"},{"2026-07-04","Zomato","Food","455","UPI"},{"2026-06-28","Netflix","Subscriptions","649","Card"},{"2026-06-20","Ajio","Shopping","2399","UPI"},{"2026-06-14","Spotify","Subscriptions","119","Card"},{"2026-06-05","Apple","Technology","999","UPI"} }; for (String[] r : rows) transactions.add(new Transaction(ids.incrementAndGet(), r[0], r[1], r[1], r[2], Double.parseDouble(r[3]), "INR", r[4], "Demo transaction", "Completed", "Demo seed")); for (String c : List.of("Food","Shopping","Entertainment","Transport","Subscriptions","Technology")) budgets.add(new Budget(c, c.equals("Food") ? 6000 : 10000)); }

    private record Transaction(int id, String date, String merchant, String website, String category, double amount, String currency, String paymentMethod, String description, String status, String source) {
        static Transaction from(Map<String, String> d, int id) { return new Transaction(id, d.getOrDefault("date", LocalDate.now().toString()), d.getOrDefault("merchant", "Custom"), d.getOrDefault("website", d.getOrDefault("merchant", "Custom")), d.getOrDefault("category", "Other"), number(d.get("amount"), 0), "INR", d.getOrDefault("paymentMethod", "UPI"), d.getOrDefault("description", ""), "Completed", "Manual entry"); }
        String json() { return "{\"id\":%d,\"date\":%s,\"merchant\":%s,\"website\":%s,\"category\":%s,\"amount\":%.2f,\"currency\":\"INR\",\"paymentMethod\":%s,\"description\":%s,\"status\":\"%s\",\"source\":\"%s\"}".formatted(id, quote(date), quote(merchant), quote(website), quote(category), amount, quote(paymentMethod), quote(description), status, source); }
    }
    private record Budget(String name, double limit) { String json(double spent) { return "{\"name\":%s,\"limit\":%.2f,\"spent\":%.2f,\"remaining\":%.2f,\"used\":%.1f}".formatted(quote(name), limit, spent, limit - spent, limit == 0 ? 0 : spent / limit * 100); } }
}
