package varsystem.ui.mechanic;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

public final class MechanicLocationService {

    private static HttpServer server;

    private static volatile double latitude = Double.NaN;
    private static volatile double longitude = Double.NaN;
    private static volatile long lastUpdate = 0L;
    private static volatile boolean sharing = false;

    private static final Map<String, String> params =
            new ConcurrentHashMap<>();

    private MechanicLocationService() {
    }

    // ==============================
    // START LOCATION RECEIVER
    // ==============================
    public static synchronized void start(int port) throws IOException {

        if (server != null) {
            return;
        }

        server = HttpServer.create(
                new InetSocketAddress("0.0.0.0", port), 0);

        server.createContext("/update", MechanicLocationService::handleUpdate);
        server.createContext("/location", MechanicLocationService::handleLocation);
        server.createContext("/stop", MechanicLocationService::handleStop);

        server.setExecutor(null);
        server.start();

        sharing = true;

        System.out.println("Mechanic Location Receiver started.");
        System.out.println("Port: " + port);
    }

    // ==============================
    // STOP LOCATION RECEIVER
    // ==============================
    public static synchronized void stop() {

        sharing = false;

        latitude = Double.NaN;
        longitude = Double.NaN;
        lastUpdate = 0L;

        if (server != null) {
            server.stop(0);
            server = null;
        }

        System.out.println("Mechanic location sharing stopped.");
    }

    // ==============================
    // RECEIVE COORDINATES
    // ==============================
    private static void handleUpdate(HttpExchange exchange) throws IOException {

        try {

            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                send(exchange, 405, "Method not allowed");
                return;
            }

            params.clear();
            params.putAll(parseQuery(exchange.getRequestURI().getQuery()));

            String latValue = params.get("lat");
            String lonValue = params.get("lon");

            if (latValue == null || lonValue == null) {
                send(exchange, 400, "Missing lat or lon");
                return;
            }

            double lat = Double.parseDouble(latValue);
            double lon = Double.parseDouble(lonValue);

            if (!validCoordinates(lat, lon)) {
                send(exchange, 400, "Invalid coordinates");
                return;
            }

            latitude = lat;
            longitude = lon;
            lastUpdate = System.currentTimeMillis();

            send(exchange, 200, "Location updated");

        } catch (Exception ex) {

            send(exchange, 400, "Invalid location data");
        }
    }

    // ==============================
    // RETURN CURRENT LOCATION
    // ==============================
    private static void handleLocation(HttpExchange exchange) throws IOException {

        if (!validCoordinates(latitude, longitude)) {

            send(exchange, 200,
                    "{\"available\":false}");

            return;
        }

        String json =
                "{\"available\":true,"
                + "\"latitude\":" + latitude + ","
                + "\"longitude\":" + longitude + ","
                + "\"updated\":" + lastUpdate + "}";

        send(exchange, 200, json);
    }

    // ==============================
    // STOP SHARING ENDPOINT
    // ==============================
    private static void handleStop(HttpExchange exchange) throws IOException {

        stop();

        send(exchange, 200, "Location sharing stopped");
    }

    // ==============================
    // QUERY PARSER
    // ==============================
    private static Map<String, String> parseQuery(String query) {

        Map<String, String> result = new ConcurrentHashMap<>();

        if (query == null || query.isEmpty()) {
            return result;
        }

        for (String item : query.split("&")) {

            String[] parts = item.split("=", 2);

            if (parts.length == 2) {

                String key = URLDecoder.decode(
                        parts[0], StandardCharsets.UTF_8);

                String value = URLDecoder.decode(
                        parts[1], StandardCharsets.UTF_8);

                result.put(key, value);
            }
        }

        return result;
    }

    // ==============================
    // SEND RESPONSE
    // ==============================
    private static void send(
            HttpExchange exchange,
            int status,
            String response) throws IOException {

        byte[] data = response.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set(
                "Content-Type", "text/plain; charset=UTF-8");

        exchange.sendResponseHeaders(status, data.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(data);
        }
    }

    // ==============================
    // COORDINATE VALIDATION
    // ==============================
    private static boolean validCoordinates(double lat, double lon) {

        return Double.isFinite(lat)
                && Double.isFinite(lon)
                && lat >= -90 && lat <= 90
                && lon >= -180 && lon <= 180;
    }

    // ==============================
    // GETTERS
    // ==============================
    public static double getLatitude() {
        return latitude;
    }

    public static double getLongitude() {
        return longitude;
    }

    public static long getLastUpdate() {
        return lastUpdate;
    }

    public static boolean hasLocation() {
        return validCoordinates(latitude, longitude);
    }

    public static boolean isSharing() {
        return sharing;
    }

    // ==============================
    // MANUAL LOCATION UPDATE
    // ==============================
    public static void updateLocation(double lat, double lon) {

        if (!validCoordinates(lat, lon)) {
            return;
        }

        latitude = lat;
        longitude = lon;
        lastUpdate = System.currentTimeMillis();
    }
}