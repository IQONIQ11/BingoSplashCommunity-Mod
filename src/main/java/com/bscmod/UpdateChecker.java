package com.bscmod;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.ChatFormatting;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.concurrent.CompletableFuture;

public class UpdateChecker {

    private static final String MOD_ID = "bingosplashcommunity";
    private static final String GITHUB_REPO = "IQONIQ11/BingoSplashCommunity-Mod";
    private static final String CURRENT_VERSION = FabricLoader.getInstance()
            .getModContainer(MOD_ID)
            .map(container -> container.getMetadata().getVersion().getFriendlyString())
            .orElse("1.0.0");

    private static int state = 0;
    private static String latestVersionTag = "";

    public static void tick() {
        if (state == 0) {
            state = 1;
            CompletableFuture.runAsync(UpdateChecker::fetchLatestVersion);
        } else if (state == 2) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                sendUpdateMessage(mc);
                state = 3;
            }
        }
    }

    private static void fetchLatestVersion() {
        try {
            URL url = URI.create("https://api.github.com/repos/" + GITHUB_REPO + "/releases/latest").toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "BscMod-UpdateChecker");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            if (connection.getResponseCode() == 200) {
                InputStreamReader reader = new InputStreamReader(connection.getInputStream());
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                latestVersionTag = json.get("tag_name").getAsString().replace("v", "");

                if (isNewerVersion(CURRENT_VERSION, latestVersionTag)) {
                    state = 2;
                } else {
                    state = 3;
                }
            } else {
                state = 3;
            }
        } catch (Exception e) {
            state = 3;
        }
    }

    private static boolean isNewerVersion(String current, String latest) {
        String[] currentParts = current.split("\\.");
        String[] latestParts = latest.split("\\.");
        int length = Math.max(currentParts.length, latestParts.length);

        for (int i = 0; i < length; i++) {
            int v1 = i < currentParts.length ? Integer.parseInt(currentParts[i].replaceAll("[^0-9]", "")) : 0;
            int v2 = i < latestParts.length ? Integer.parseInt(latestParts[i].replaceAll("[^0-9]", "")) : 0;
            if (v2 > v1) return true;
            if (v1 > v2) return false;
        }
        return false;
    }

    private static void sendUpdateMessage(Minecraft mc) {
        String downloadPage = "https://modrinth.com/mod/bingosplashcommunity/versions";

        Component downloadComponent = Component.literal("[DOWNLOAD]").withStyle(style ->
                style.withClickEvent(new ClickEvent.OpenUrl(URI.create(downloadPage)))
                        .withHoverEvent(new HoverEvent.ShowText(
                                Component.literal("§7Click to open download page")
                        ))
                        .withColor(ChatFormatting.GREEN)
                        .withBold(true)
        );

        Component message = Component.literal("§b[BSC] §6§lNew update available: §f" + latestVersionTag + " ")
                .append(downloadComponent);

        mc.player.displayClientMessage(message, false);
    }
}