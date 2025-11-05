package org.aller.cvtweaks.client.modules;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest;
import java.time.Duration;
import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;
import javax.net.ssl.TrustManager;
import java.security.cert.X509Certificate;
import java.util.UUID;

public class ScamBase {

    private static final Logger LOGGER = LoggerFactory.getLogger("CVTweaks/ScamBase");
    private static final String API_URL = "http://66.70.176.148:65514";
    private static final HttpClient httpClient = createHttpClient();

    private static HttpClient createHttpClient() {
        try {
            TrustManager[] trustAll = new TrustManager[]{
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() { return null; }
                        public void checkClientTrusted(X509Certificate[] c, String a) {}
                        public void checkServerTrusted(X509Certificate[] c, String a) {}
                    }
            };

            SSLContext ssl = SSLContext.getInstance("TLS");
            ssl.init(null, trustAll, new java.security.SecureRandom());

            return HttpClient.newBuilder()
                    .sslContext(ssl)
                    .connectTimeout(Duration.ofSeconds(6))
                    .build();

        } catch (Exception e) {
            return HttpClient.newBuilder().build();
        }
    }

    public static void init() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {

            dispatcher.register(ClientCommandManager.literal("reportscam")
                    .then(ClientCommandManager.argument("username", StringArgumentType.string())
                            .suggests((c, b) -> {
                                MinecraftClient mc = MinecraftClient.getInstance();
                                if (mc.getNetworkHandler() != null) {
                                    mc.getNetworkHandler().getPlayerList().forEach(p ->
                                            b.suggest(p.getProfile().getName()));
                                }
                                return b.buildFuture();
                            })
                            .then(ClientCommandManager.argument("reason", StringArgumentType.greedyString())
                                    .executes(ctx -> {
                                        String username = StringArgumentType.getString(ctx, "username");
                                        String reason = StringArgumentType.getString(ctx, "reason");
                                        reportScammer(username, reason);
                                        return 1;
                                    })
                            )
                    )
            );

            dispatcher.register(ClientCommandManager.literal("scamreports")
                    .then(ClientCommandManager.argument("username", StringArgumentType.string())
                            .suggests((c, b) -> {
                                MinecraftClient mc = MinecraftClient.getInstance();
                                if (mc.getNetworkHandler() != null) {
                                    mc.getNetworkHandler().getPlayerList().forEach(p ->
                                            b.suggest(p.getProfile().getName()));
                                }
                                return b.buildFuture();
                            })
                            .executes(ctx -> {
                                fetchReports(StringArgumentType.getString(ctx, "username"), 1);
                                return 1;
                            })
                            .then(ClientCommandManager.argument("page", IntegerArgumentType.integer(1))
                                    .executes(ctx -> {
                                        fetchReports(
                                                StringArgumentType.getString(ctx, "username"),
                                                IntegerArgumentType.getInteger(ctx, "page")
                                        );
                                        return 1;
                                    })
                            )
                    )
            );
        });
    }

    private static void reportScammer(String username, String reason) {
        MinecraftClient mc = MinecraftClient.getInstance();
        UUID uuid = mc.player.getUuid();

        new Thread(() -> {
            try {
                String url = API_URL + "/report_scammer?reporter_uuid=" + uuid.toString();

                JsonObject json = new JsonObject();
                json.addProperty("username", username);
                json.addProperty("reason", reason);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                        .build();

                httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                send("§a✔ Report submitted for §e" + username + "§a.");

            } catch (Exception e) {
                send("§c✘ Failed to submit scam report.");
            }
        }).start();
    }


    private static void fetchReports(String username, int page) {
        new Thread(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(API_URL + "/reports/" + username + "?page=" + page + "&size=5"))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();

                int totalPages = json.get("pages").getAsInt();
                JsonArray reports = json.getAsJsonArray("reports");

                send("§b§l[Reports for §e" + username + "§b] §7(Page " + page + "/" + totalPages + ")");

                if (reports.size() == 0) {
                    send("§7No reports found.");
                    return;
                }

                for (int i = 0; i < reports.size(); i++) {
                    JsonObject r = reports.get(i).getAsJsonObject();
                    String repUUID = r.get("reporter_uuid").getAsString();
                    String repName = resolveUsername(repUUID);

                    String reason = r.get("reason").getAsString();
                    send("§7- §e" + reason + " §7(§aby §e" + repName + "§7)");
                }

            } catch (Exception e) {
                send("§c✘ Failed to retrieve scam reports.");
            }
        }).start();
    }


    private static String resolveUsername(String uuid) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getNetworkHandler() != null) {
            PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(UUID.fromString(uuid));
            if (entry != null) return entry.getProfile().getName();
        }
        return "§8" + uuid.substring(0, 8) + "§7"; // fallback short UUID with formatting
    }


    private static void send(String msg) {
        MinecraftClient.getInstance().execute(() ->
                MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.literal(msg)));
    }
}
