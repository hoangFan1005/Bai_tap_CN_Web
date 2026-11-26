package vn.rally;

import static org.junit.jupiter.api.Assertions.*;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.catalina.startup.Tomcat;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class StoreIT {
    private static final String CONTEXT = "/RallySport";
    private static final String SCHEMA = "rally_qa_" + UUID.randomUUID().toString().replace("-", "");
    private static final String DB_SERVER = setting("RALLY_TEST_DB_SERVER", "jdbc:mysql://127.0.0.1:3306/");
    private static final String DB_USER = setting("RALLY_TEST_DB_USER", "root");
    private static final String DB_PASSWORD = setting("RALLY_TEST_DB_PASSWORD", "");
    private static final String OPTIONS = "?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8";
    private static final String FIXTURE_PASSWORD = UUID.randomUUID().toString();
    private static final Map<String, String> ORIGINAL_PROPERTIES = new LinkedHashMap<>();
    private static Tomcat tomcat;
    private static String origin;

    @BeforeAll
    static void start() throws Exception {
        Path root = Path.of("").toAbsolutePath();
        Path war = root.resolve("target/RallySport.war");
        assertTrue(Files.isRegularFile(war), "Build the WAR before running this suite");
        var script = Files.readString(root.resolve("database/setup.sql"), StandardCharsets.UTF_8);
        try (var connection = connect(""); var statement = connection.createStatement()) {
            for (String sql : script.replace("\uFEFF", "").replace("rally_sport", SCHEMA).lines()
                    .filter(line -> !line.stripLeading().startsWith("--"))
                    .collect(Collectors.joining("\n")).split(";")) {
                if (!sql.isBlank()) {
                    statement.execute(sql);
                }
            }
        }
        setApp("rally.db.url", DB_SERVER + SCHEMA + OPTIONS);
        setApp("rally.db.user", DB_USER);
        setApp("rally.db.password", DB_PASSWORD);
        tomcat = new Tomcat();
        Path serverBase = Files.createTempDirectory(root.resolve("target"), "tomcat-test-");
        Files.createDirectories(serverBase.resolve("webapps"));
        tomcat.setBaseDir(serverBase.toString());
        tomcat.setPort(0);
        tomcat.getConnector().setProperty("address", "127.0.0.1");
        tomcat.addWebapp(CONTEXT, war.toString());
        tomcat.start();
        origin = "http://127.0.0.1:" + tomcat.getConnector().getLocalPort();
    }

    @AfterAll
    static void stop() throws Exception {
        try {
            if (tomcat != null) {
                try {
                    tomcat.stop();
                } finally {
                    tomcat.destroy();
                }
            }
        } finally {
            try (var connection = connect(""); var statement = connection.createStatement()) {
                statement.execute("DROP DATABASE IF EXISTS " + SCHEMA);
            } finally {
                ORIGINAL_PROPERTIES.forEach((key, value) -> {
                    if (value == null) {
                        System.clearProperty(key);
                    } else {
                        System.setProperty(key, value);
                    }
                });
            }
        }
    }

    @Test
    void guestsBrowseAndSearchButCannotOpenCheckoutOrAdministration() throws Exception {
        Browser guest = new Browser();
        var home = guest.get("/home");
        assertEquals(200, home.statusCode(), home.body());
        assertEquals(200, guest.get("/").statusCode());
        assertTrue(home.body().contains("Rally"), home.body());
        assertTrue(home.body().contains("Sẵn sàng"), home.body());
        var search = guest.get("/products?q=" + encode("cuốn cán"));
        assertEquals(200, search.statusCode(), search.body());
        assertTrue(search.body().contains("Cuốn Cán Vải Apavi Ag-110"), search.body());
        assertTrue(search.body().contains("Sản phẩm cầu lông"), search.body());
        assertFalse(search.body().contains("href=\"" + CONTEXT + "/product?id=1\""), search.body());
        assertLocation("/login", guest.get("/checkout"));
        assertLocation("/login", guest.get("/admin"));
        assertLocation("/login", guest.get("/admin/products"));
        assertEquals(404, guest.get("/WEB-INF/views/home.jsp").statusCode());
    }

    @Test
    void registrationCreatesCustomerAndPostRequiresToken() throws Exception {
        Browser user = new Browser();
        assertEquals(403, user.post("/register", Map.of("email", "invalid@example.test")).statusCode());
        user.register("customer@example.test");
        assertEquals("USER", scalar("SELECT role FROM users WHERE email='customer@example.test'"));
        user.login("customer@example.test");
        assertEquals(403, user.get("/admin").statusCode());
        assertEquals(403, user.get("/admin/products").statusCode());
        assertEquals(200, user.get("/cart").statusCode());
        assertLocation("/home", user.postForm("/logout", "/home", Map.of()));
        assertLocation("/login", user.get("/orders"));
    }

    @Test
    void administratorCreatesSearchesEditsAndDeletesAProduct() throws Exception {
        Browser admin = admin("catalog-admin@example.test");
        assertLocation("/admin/products", admin.get("/admin"));
        var product = new LinkedHashMap<>(Map.of(
                "name", "Vợt kiểm thử Alpha", "brand", "Rally", "categoryId", "1",
                "price", "222000", "description", "Dòng vợt thử", "imagePath", "racket.webp",
                "active", "true", "variantId", "", "sku", "QA-ALPHA", "sizeLabel", ""));
        product.put("stock", "4");
        product.put("originalStock", "");
        var create = admin.postForm("/admin/products/save", "/admin/products/new", product);
        assertEquals(302, create.statusCode(), create.body());
        String id = scalar("SELECT id FROM products WHERE name='Vợt kiểm thử Alpha'");
        var adminSearch = admin.get("/admin/products?q=" + encode("Alpha"));
        assertEquals(200, adminSearch.statusCode(), adminSearch.body());
        assertTrue(adminSearch.body().contains("Vợt kiểm thử Alpha"), adminSearch.body());
        assertFalse(adminSearch.body().contains("Cuốn Cán Vải Apavi Ag-110"), adminSearch.body());
        assertTrue(adminSearch.body().contains("href=\"" + CONTEXT + "/admin/orders\""), adminSearch.body());
        var categoryFilter = admin.get("/admin/products?category=5");
        assertEquals(200, categoryFilter.statusCode(), categoryFilter.body());
        assertTrue(categoryFilter.body().contains("Cuốn Cán Vải Apavi Ag-110"), categoryFilter.body());
        assertFalse(categoryFilter.body().contains("Vợt kiểm thử Alpha"), categoryFilter.body());
        var categoriesPage = admin.get("/admin/categories");
        assertEquals(200, categoriesPage.statusCode(), categoriesPage.body());
        assertTrue(categoriesPage.body().contains("href=\"" + CONTEXT + "/admin/orders\""), categoriesPage.body());

        product.put("id", id);
        product.put("variantId", scalar("SELECT id FROM product_variants WHERE sku='QA-ALPHA'"));
        product.put("originalStock", "4");
        product.put("name", "Vợt kiểm thử Beta");
        product.put("price", "333000");
        var update = admin.postForm("/admin/products/save", "/admin/products/edit?id=" + id, product);
        assertEquals(302, update.statusCode(), update.body());
        assertEquals("333000", scalar("SELECT price FROM products WHERE id=" + id));
        var detail = new Browser().get("/product?id=" + id);
        assertEquals(200, detail.statusCode(), detail.body());
        assertTrue(detail.body().contains("Vợt kiểm thử Beta"));
        var remove = admin.postForm("/admin/products/delete", "/admin/products", Map.of("id", id));
        assertEquals(302, remove.statusCode(), remove.body());
        assertEquals("0", scalar("SELECT COUNT(*) FROM products WHERE id=" + id));
        assertEquals(404, new Browser().get("/product?id=" + id).statusCode());
    }

    @Test
    void checkoutIsIdempotentAndKeepsOrderHistoryAfterArchive() throws Exception {
        Browser user = customer("buyer@example.test");
        var added = user.postForm("/cart", "/product?id=19",
                Map.of("action", "add", "variantId", "58", "quantity", "2"));
        assertLocation("/cart", added);
        var cart = user.get("/cart");
        assertEquals(200, cart.statusCode(), cart.body());
        assertTrue(cart.body().contains("Áo cầu lông Lining 9002B"), cart.body());
        Map<String, String> order = checkoutForm(user);
        var placed = user.post("/checkout", order);
        assertLocation("/order", placed);
        String orderUrl = URI.create(placed.headers().firstValue("Location").orElseThrow())
                .getRawQuery();
        String id = scalar("SELECT id FROM orders WHERE checkout_key='" + order.get("checkoutKey") + "'");

        var duplicate = user.post("/checkout", order);
        assertLocation("/order", duplicate);
        assertEquals(orderUrl, URI.create(duplicate.headers().firstValue("Location").orElseThrow()).getRawQuery());
        assertEquals("360000", scalar("SELECT total FROM orders WHERE id=" + id));
        assertEquals("16", scalar("SELECT stock FROM product_variants WHERE id=58"));
        assertEquals("HỒNG / L", scalar("SELECT size_label FROM order_items WHERE order_id=" + id));
        assertEquals(200, user.get("/order?id=" + id).statusCode());
        assertEquals(200, user.get("/orders").statusCode());
        assertEquals(404, customer("another@example.test").get("/order?id=" + id).statusCode());

        Browser admin = admin("orders-admin@example.test");
        execute("UPDATE products SET name='Áo cập nhật', price=475000 WHERE id=19");
        var archived = admin.postForm("/admin/products/delete", "/admin/products", Map.of("id", "19"));
        assertEquals(302, archived.statusCode(), archived.body());
        assertEquals("0", scalar("SELECT active FROM products WHERE id=19"));
        assertEquals(404, new Browser().get("/product?id=19").statusCode());
        var history = user.get("/order?id=" + id);
        assertTrue(history.body().contains("Áo cầu lông Lining 9002B"), history.body());
        assertFalse(history.body().contains("Áo cập nhật"), history.body());

        var cancelled = admin.postForm("/admin/orders", "/admin/orders?id=" + id,
                Map.of("id", id, "status", "CANCELLED"));
        assertLocation("/admin/orders", cancelled);
        assertEquals("CANCELLED", scalar("SELECT status FROM orders WHERE id=" + id));
        assertEquals("18", scalar("SELECT stock FROM product_variants WHERE id=58"));
        var invalidTransition = admin.postForm("/admin/orders", "/admin/orders",
                Map.of("id", id, "status", "CANCELLED"));
        assertEquals(409, invalidTransition.statusCode(), invalidTransition.body());
        assertEquals("18", scalar("SELECT stock FROM product_variants WHERE id=58"));
    }

    @Test
    void orderWithInsufficientStockDoesNotPartiallyReduceInventory() throws Exception {
        Browser user = customer("stock@example.test");
        assertLocation("/cart", user.postForm("/cart", "/product?id=2",
                Map.of("action", "add", "variantId", "2", "quantity", "2")));
        assertLocation("/cart", user.postForm("/cart", "/product?id=33",
                Map.of("action", "add", "variantId", "105", "quantity", "3")));
        Map<String, String> order = checkoutForm(user);
        execute("UPDATE product_variants SET stock=2 WHERE id=105");
        try {
            var response = user.post("/checkout", order);
            assertEquals(409, response.statusCode(), response.body());
            assertEquals("393", scalar("SELECT stock FROM product_variants WHERE id=2"));
            assertEquals("2", scalar("SELECT stock FROM product_variants WHERE id=105"));
            assertEquals("0", scalar("SELECT COUNT(*) FROM orders WHERE checkout_key='" + order.get("checkoutKey") + "'"));
        } finally {
            execute("UPDATE product_variants SET stock=40 WHERE id=105");
        }
    }

    @Test
    void twoBuyersCannotBothPlaceAnOrderForTheLastUnits() throws Exception {
        Browser first = customer("race-first@example.test");
        Browser second = customer("race-second@example.test");
        Map<String, String> items = Map.of("action", "add", "variantId", "24", "quantity", "12");
        assertLocation("/cart", first.postForm("/cart", "/product?id=13", items));
        assertLocation("/cart", second.postForm("/cart", "/product?id=13", items));
        var firstOrder = checkoutForm(first);
        var secondOrder = checkoutForm(second);

        var requests = Executors.newFixedThreadPool(2);
        try {
            var a = requests.submit(() -> first.post("/checkout", firstOrder).statusCode());
            var b = requests.submit(() -> second.post("/checkout", secondOrder).statusCode());
            int codeA = a.get(25, TimeUnit.SECONDS);
            int codeB = b.get(25, TimeUnit.SECONDS);
            assertTrue((codeA == 302 && codeB == 409) || (codeA == 409 && codeB == 302),
                    "Expected exactly one successful checkout but got " + codeA + " and " + codeB);
            assertEquals("0", scalar("SELECT stock FROM product_variants WHERE id=24"));
            assertEquals("1", scalar("SELECT COUNT(*) FROM order_items WHERE variant_id=24"));
        } finally {
            requests.shutdownNow();
        }
    }

    @Test
    void customerCancelsPendingOrderAndRestoresStockButCannotCancelConfirmedOrForeignOrder() throws Exception {
        Browser buyer = customer("cancel-buyer@example.test");
        assertLocation("/cart", buyer.postForm("/cart", "/product?id=21",
                Map.of("action", "add", "variantId", "68", "quantity", "2")));
        Map<String, String> firstOrder = checkoutForm(buyer);
        assertLocation("/order", buyer.post("/checkout", firstOrder));
        String firstId = scalar("SELECT id FROM orders WHERE checkout_key='" + firstOrder.get("checkoutKey") + "'");
        assertEquals("9", scalar("SELECT stock FROM product_variants WHERE id=68"));
        var pendingPage = buyer.get("/order?id=" + firstId);
        assertEquals(200, pendingPage.statusCode(), pendingPage.body());
        assertTrue(pendingPage.body().contains("Hủy đơn hàng"), pendingPage.body());

        Browser stranger = customer("cancel-stranger@example.test");
        var foreignCancel = stranger.postForm("/order", "/orders", Map.of("action", "cancel", "id", firstId));
        assertEquals(404, foreignCancel.statusCode(), foreignCancel.body());
        assertEquals("PENDING", scalar("SELECT status FROM orders WHERE id=" + firstId));
        assertEquals("9", scalar("SELECT stock FROM product_variants WHERE id=68"));

        var cancelled = buyer.postForm("/order", "/order?id=" + firstId, Map.of("action", "cancel", "id", firstId));
        assertLocation("/order", cancelled);
        assertEquals("CANCELLED", scalar("SELECT status FROM orders WHERE id=" + firstId));
        assertEquals("11", scalar("SELECT stock FROM product_variants WHERE id=68"));

        var duplicateCancel = buyer.postForm("/order", "/orders", Map.of("action", "cancel", "id", firstId));
        assertEquals(409, duplicateCancel.statusCode(), duplicateCancel.body());
        assertEquals("11", scalar("SELECT stock FROM product_variants WHERE id=68"));

        assertLocation("/cart", buyer.postForm("/cart", "/product?id=37",
                Map.of("action", "add", "variantId", "122", "quantity", "1")));
        Map<String, String> secondOrder = checkoutForm(buyer);
        assertLocation("/order", buyer.post("/checkout", secondOrder));
        String secondId = scalar("SELECT id FROM orders WHERE checkout_key='" + secondOrder.get("checkoutKey") + "'");
        assertEquals("23", scalar("SELECT stock FROM product_variants WHERE id=122"));

        Browser admin = admin("cancel-admin@example.test");
        assertLocation("/admin/orders", admin.postForm("/admin/orders", "/admin/orders?id=" + secondId,
                Map.of("id", secondId, "status", "CONFIRMED")));

        var confirmedPage = buyer.get("/order?id=" + secondId);
        assertEquals(200, confirmedPage.statusCode(), confirmedPage.body());
        assertFalse(confirmedPage.body().contains("Hủy đơn hàng"), confirmedPage.body());
        var lateCancel = buyer.postForm("/order", "/orders", Map.of("action", "cancel", "id", secondId));
        assertEquals(409, lateCancel.statusCode(), lateCancel.body());
        assertEquals("CONFIRMED", scalar("SELECT status FROM orders WHERE id=" + secondId));
        assertEquals("23", scalar("SELECT stock FROM product_variants WHERE id=122"));
    }

    private static Map<String, String> checkoutForm(Browser browser) throws Exception {
        var checkout = browser.get("/checkout");
        assertEquals(200, checkout.statusCode(), checkout.body());
        return Map.of("_csrf", field(checkout.body(), "_csrf"), "checkoutKey", field(checkout.body(), "checkoutKey"),
                "recipientName", "Người nhận thử", "phone", "0900000010", "address", "Đà Nẵng");
    }

    private static Browser customer(String email) throws Exception {
        Browser browser = new Browser();
        browser.register(email);
        browser.login(email);
        return browser;
    }

    private static Browser admin(String email) throws Exception {
        Browser browser = new Browser();
        browser.register(email);
        try (Connection connection = connect(SCHEMA);
                var query = connection.prepareStatement("UPDATE users SET role='ADMIN' WHERE email=?")) {
            query.setString(1, email);
            assertEquals(1, query.executeUpdate());
        }
        browser.login(email);
        return browser;
    }

    private static Connection connect(String schema) throws Exception {
        return DriverManager.getConnection(DB_SERVER + schema + OPTIONS, DB_USER, DB_PASSWORD);
    }

    private static String scalar(String sql) throws Exception {
        try (Connection connection = connect(SCHEMA); Statement statement = connection.createStatement();
                var rows = statement.executeQuery(sql)) {
            assertTrue(rows.next(), sql);
            return rows.getString(1);
        }
    }

    private static void execute(String sql) throws Exception {
        try (Connection connection = connect(SCHEMA); Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    private static void assertLocation(String expected, HttpResponse<String> response) {
        assertEquals(302, response.statusCode(), response.body());
        assertEquals(CONTEXT + expected, URI.create(response.headers().firstValue("Location").orElseThrow()).getPath());
    }

    private static String setting(String name, String fallback) {
        String value = System.getenv(name);
        return value == null ? fallback : value;
    }

    private static void setApp(String key, String value) {
        ORIGINAL_PROPERTIES.put(key, System.getProperty(key));
        System.setProperty(key, value);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String field(String html, String name) {
        var match = Pattern.compile("name=\"" + Pattern.quote(name) + "\"\\s+value=\"([^\"]+)\"").matcher(html);
        assertTrue(match.find(), "Missing field " + name + " in " + html);
        return match.group(1);
    }

    private static final class Browser {
        private final HttpClient client = HttpClient.newBuilder()
                .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
                .connectTimeout(Duration.ofSeconds(8)).build();

        HttpResponse<String> get(String path) throws Exception {
            return send(path, null);
        }

        HttpResponse<String> post(String path, Map<String, String> fields) throws Exception {
            String data = fields.entrySet().stream().map(item -> encode(item.getKey()) + "=" + encode(item.getValue()))
                    .collect(Collectors.joining("&"));
            return send(path, data);
        }

        HttpResponse<String> postForm(String path, String source, Map<String, String> fields) throws Exception {
            var page = get(source);
            assertEquals(200, page.statusCode(), page.body());
            var body = new LinkedHashMap<>(fields);
            body.put("_csrf", field(page.body(), "_csrf"));
            return post(path, body);
        }

        void register(String email) throws Exception {
            assertLocation("/login", postForm("/register", "/register",
                    Map.of("name", "Khách thử", "email", email, "password", FIXTURE_PASSWORD)));
        }

        void login(String email) throws Exception {
            var response = postForm("/login", "/login", Map.of("email", email, "password", FIXTURE_PASSWORD));
            assertEquals(302, response.statusCode(), response.body());
        }

        private HttpResponse<String> send(String path, String form) throws Exception {
            var request = HttpRequest.newBuilder(URI.create(origin + CONTEXT + path)).timeout(Duration.ofSeconds(20));
            if (form != null) {
                request.header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                        .POST(HttpRequest.BodyPublishers.ofString(form));
            }
            return client.send(request.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        }
    }
}
