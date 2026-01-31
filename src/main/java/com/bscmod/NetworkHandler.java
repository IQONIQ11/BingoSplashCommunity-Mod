package com.bscmod;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.sounds.SoundEvents;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NetworkHandler extends Thread {
    private static final String WSS_URL = "wss://api.bscmod.com/";
    private boolean running = true;
    private HttpClient client;
    private WebSocket webSocketClient;

    public static String activeLobby = "";
    public static long lastPingTime = 0;
    public static volatile String currentSplashDiscordId = "";

    private static final Pattern HUB_PATTERN = Pattern.compile("hub\\s+(\\d+)", Pattern.CASE_INSENSITIVE);

    @Override
    public void run() {
        connect();
        while (running) {
            try { Thread.sleep(1000); } catch (InterruptedException e) { break; }
        }
    }

    private void connect() {
        if (!running) return;
        client = HttpClient.newHttpClient();
        CompletableFuture<WebSocket> wsFuture = client.newWebSocketBuilder()
                .buildAsync(URI.create(WSS_URL), new WebSocketListener());
        wsFuture.whenComplete((ws, ex) -> {
            if (ex != null) {
                System.err.println("[BSC] Connection Error: " + ex.getMessage());
                ex.printStackTrace();
                scheduleReconnect();
            } else {
                webSocketClient = ws;
            }
        });
    }

    private void scheduleReconnect() {
        System.out.println("[BSC] Attempting to reconnect in 5 seconds...");
        try {
            Thread.sleep(5000);
            connect();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private class WebSocketListener implements WebSocket.Listener {
        @Override
        public void onOpen(WebSocket webSocket) {
            System.out.println("[BSC] Connected to the Websocket!");
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            String message = data.toString();
            if (message.equals("KEEPALIVE")) {
                System.out.println("[BSC] Heartbeat received from server.");
            } else {
                handleMessage(message);
            }
            webSocket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onPing(WebSocket webSocket, ByteBuffer message) {
            return WebSocket.Listener.super.onPing(webSocket, message);
        }

        @Override
        public CompletionStage<?> onPong(WebSocket webSocket, ByteBuffer message) {
            return WebSocket.Listener.super.onPong(webSocket, message);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            scheduleReconnect();
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            scheduleReconnect();
        }
    }

    private void handleMessage(String message) {
        if (!BscConfig.receivePings) return;

        String[] parts = message.split("\\|", 4);
        if (parts.length < 3) return;

        String senderName = parts[0];
        String senderDiscordId = parts[1];
        String pingType = parts[2];
        String actualContent = (parts.length == 4) ? parts[3] : "";

        String msgLower = actualContent.toLowerCase();

        if (pingType.equalsIgnoreCase("SPLASH")) {
            String detectedProfile = HypixelUtils.getProfileType();
            if (BscConfig.ironmanOnly && !detectedProfile.equalsIgnoreCase("Ironman")) return;
            if (BscConfig.bingoOnly && !detectedProfile.equalsIgnoreCase("Bingo")) return;
        } else {
            if (msgLower.contains("synthetic heart") && !BscConfig.syncHeart) return;
            if (msgLower.contains("robotron reflector") && !BscConfig.robotron) return;
            if (msgLower.contains("control switch") && !BscConfig.controlSwitch) return;
            if (msgLower.contains("superlite motor") && !BscConfig.motor) return;
            if (msgLower.contains("electron transmitter") && !BscConfig.transmitter) return;
            if (msgLower.contains("ftx 3070") && !BscConfig.ftx3070) return;
            if (msgLower.contains("armadillo egg") && !BscConfig.armadillo) return;
            if (msgLower.contains("jungle key") && !BscConfig.jungleKey) return;
            if (msgLower.contains("pickonimbus") && !BscConfig.picko) return;
            if (msgLower.contains("powder") && !msgLower.contains("2x") && !BscConfig.powder) return;
            if (msgLower.contains("goblin egg") && !BscConfig.goblinEgg) return;
            if (msgLower.contains("flawless gemstone") && !BscConfig.flawlessGem) return;

            if (msgLower.contains("2x powder") && !BscConfig.powder2x) return;
            if (msgLower.contains("goblin raid") && !BscConfig.goblinRaid) return;
            if (msgLower.contains("raffle") && !BscConfig.raffle) return;
            if (msgLower.contains("better together") && !BscConfig.betterTogether) return;
            if (msgLower.contains("gone with the wind") && !BscConfig.goneWind) return;
            if (msgLower.contains("mithril gourmand") && !BscConfig.mithrilGourmand) return;
        }

        Matcher matcher = HUB_PATTERN.matcher(actualContent);
        if (matcher.find()) {
            activeLobby = matcher.group(1);
            lastPingTime = System.currentTimeMillis();
        }

        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        client.execute(() -> {
            currentSplashDiscordId = senderDiscordId;
            String formatted = "§b§l[BSC] §e" + senderName + ": §f" + actualContent;
            client.player.displayClientMessage(Component.literal(formatted), false);

            if (BscConfig.showTitle) {
                Component titleText;
                if (pingType.equalsIgnoreCase("SPLASH")) {
                    titleText = Component.literal("Splash by " + senderName)
                            .setStyle(Style.EMPTY.withColor(BscConfig.titleColor));
                } else if (actualContent.contains("2x Powder") ||
                        actualContent.contains("Gourmand") ||
                        actualContent.contains("Raid") ||
                        actualContent.contains("Raffle") ||
                        actualContent.contains("Together") ||
                        actualContent.contains("Wind")) {
                    titleText = Component.literal("§d§lEvent Ping");
                } else {
                    titleText = Component.literal("§6§lItem Found");
                }

                client.gui.setTitle(titleText);
                client.gui.setSubtitle(Component.literal("§f" + actualContent));
                client.gui.setTimes(10, 70, 20);
            }

            if (BscConfig.playSound) {
                client.player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            }
        });
    }

    public void disconnect() { closeSocket(); }

    private void closeSocket() {
        if (webSocketClient != null) {
            webSocketClient.sendClose(1000, "User requested disconnect");
            webSocketClient = null;
        }
        if (client != null) client.close();
    }

    public void stopListener() {
        this.running = false;
        disconnect();
    }
}