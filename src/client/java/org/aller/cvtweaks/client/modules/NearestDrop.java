package org.aller.cvtweaks.client.modules;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.FileReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Paths;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Registers the /nearestdrop command and queries a FastAPI server over HTTP.
 */
public class NearestDrop {

    private static final MinecraftClient client = MinecraftClient.getInstance();
    private static Map<String, double[]> drops = new HashMap<>();
    private static final String API_URL = "http://66.70.176.148:65514/nearestdrop";
    private static final Gson gson = new Gson();

    private static final HttpClient httpClient = createHttpClient();

    private static void sendHudMessage(String msg) {
        if (client != null && client.inGameHud != null) {
            client.inGameHud.getChatHud().addMessage(Text.literal(msg));
        }
    }

    /** Creates an HTTP client that allows insecure HTTP/HTTPS connections */
    private static HttpClient createHttpClient() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() { return null; }
                        public void checkClientTrusted(X509Certificate[] certs, String authType) { }
                        public void checkServerTrusted(X509Certificate[] certs, String authType) { }
                    }
            };
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

            return HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .sslContext(sslContext)
                    .build();
        } catch (Exception e) {
            sendHudMessage("§cFailed to create HTTP client, using default: " + e.getMessage());
            return HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();
        }
    }

    private static double distance(double x1, double y1, double z1, double x2, double y2, double z2) {
        return Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2) + Math.pow(z1 - z2, 2));
    }

    private static double distance(double x1, double z1, double x2, double z2) {
        return Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(z1 - z2, 2));
    }

    /** Load drops from local drops.json */
    private static void loadDrops() {
        if (!drops.isEmpty()) return;
        try (Reader reader = new FileReader(Paths.get("drops.json").toFile())) {
            Type type = new TypeToken<Map<String, double[]>>() {}.getType();
            drops = gson.fromJson(reader, type);
        } catch (Exception e) {
            sendHudMessage("§cFailed to load drops.json: " + e.getMessage());
        }
    }

    /** Query server for nearest drop */
    private static void queryServer(double x, double y, double z, boolean hasY) {
        new Thread(() -> {
            try {
                // Round coordinates to integers
                int xInt = (int) Math.round(x);
                int zInt = (int) Math.round(z);
                int yInt = (int) Math.round(y);

                String url = API_URL + "?x=" + xInt + "&z=" + zInt;
                if (hasY) url += "&y=" + yInt;

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(5))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    // Parse JSON response
                    JsonObject jsonResponse = gson.fromJson(response.body(), JsonObject.class);
                    String drop = jsonResponse.get("drop").getAsString();
                    int distance = jsonResponse.get("distance").getAsInt();

                    sendHudMessage("§eNearest drop: §a" + drop + " §7(" + distance + " blocks)");
                }
            } catch (Exception e) {
                sendHudMessage("§cError querying server: " + e.getMessage());
            }
        }).start();
    }

    public static void init() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            // /nearestdrop (no args)
            dispatcher.register(ClientCommandManager.literal("nearestdrop")
                    .executes(ctx -> {
                        if (!PremiumChecker.isPremium()) {
                            sendHudMessage("§cThe nearest drop feature requires premium!");
                            return 0;
                        }

                        double x = client.player.getX();
                        double y = client.player.getY();
                        double z = client.player.getZ();
                        queryServer(x, y, z, true);
                        return 1;
                    })
            );

            // /nearestdrop <x> <z>
            dispatcher.register(ClientCommandManager.literal("nearestdrop")
                    .then(ClientCommandManager.argument("x", DoubleArgumentType.doubleArg())
                            .then(ClientCommandManager.argument("z", DoubleArgumentType.doubleArg())
                                    .executes(ctx -> {
                                        if (!PremiumChecker.isPremium()) {
                                            sendHudMessage("§cThe nearest drop feature requires premium!");
                                            return 0;
                                        }

                                        double x = DoubleArgumentType.getDouble(ctx, "x");
                                        double z = DoubleArgumentType.getDouble(ctx, "z");
                                        double y = client.player != null ? client.player.getY() : 0;
                                        queryServer(x, y, z, false);
                                        return 1;
                                    })
                            )
                    )
            );

            // /nearestdrop <x> <z> <y>
            dispatcher.register(ClientCommandManager.literal("nearestdrop")
                    .then(ClientCommandManager.argument("x", DoubleArgumentType.doubleArg())
                            .then(ClientCommandManager.argument("z", DoubleArgumentType.doubleArg())
                                    .then(ClientCommandManager.argument("y", DoubleArgumentType.doubleArg())
                                            .executes(ctx -> {
                                                if (!PremiumChecker.isPremium()) {
                                                    sendHudMessage("§cThe nearest drop feature requires premium!");
                                                    return 0;
                                                }

                                                double x = DoubleArgumentType.getDouble(ctx, "x");
                                                double z = DoubleArgumentType.getDouble(ctx, "z");
                                                double y = DoubleArgumentType.getDouble(ctx, "y");
                                                queryServer(x, y, z, true);
                                                return 1;
                                            })
                                    )
                            )
                    )
            );
        });
    }
}