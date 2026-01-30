package com.bscmod;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.net.URL;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class BscBingoHud {
    private static final List<String> goals = new ArrayList<>();
    private static boolean isLoading = false;
    private static final String GOAL_URL = "https://raw.githubusercontent.com/IQONIQ11/bingo-goals/main/goals.txt";

    public static void register() {
        fetchGoals();

        HudRenderCallback.EVENT.register((context, tickCounter) -> {
            Minecraft client = Minecraft.getInstance();
            if (client.player == null || client.options.hideGui) return;
            if (client.screen instanceof BscScreen || client.screen instanceof BscHudEditScreen) return;

            if (BscConfig.displayBingoCard) renderCard(context, client.font);
            if (BscConfig.displayBingoTimer) renderTimer(context, client.font);
        });
    }

    // Helper to identify the current month's event
    private static String getSessionKey() {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        return now.getYear() + "-" + now.getMonthValue();
    }

    public static void onChatMessage(String text) {
        if (text.contains("»") && text.contains("Bingo Goal Completed:")) {
            try {
                String[] parts = text.split("Completed:");
                if (parts.length > 1) {
                    String completedGoal = parts[1].replaceAll("§[0-9a-fk-or]", "").trim();
                    removeGoal(completedGoal);
                }
            } catch (Exception ignored) {}
        }
    }

    private static void removeGoal(String goalName) {
        synchronized (goals) {
            String cleanName = goalName.toLowerCase().trim();
            // 1. Remove from the active RAM list
            boolean removed = goals.removeIf(goal -> cleanName.contains(goal.toLowerCase().trim()));

            // 2. If successfully removed, save to config so it stays gone
            if (removed) {
                if (!BscConfig.completedGoals.contains(cleanName)) {
                    BscConfig.completedGoals.add(cleanName);
                    BscConfig.lastBingoSession = getSessionKey();
                    BscConfig.save(); // Writes to your existing bsc-config.json
                }
            }
        }
    }

    public static void fetchGoals() {
        if (isLoading) return;
        isLoading = true;

        Util.backgroundExecutor().execute(() -> {
            try {
                // AUTO-RESET: Wipe the "completed" list if the month has changed
                String currentSession = getSessionKey();
                if (!currentSession.equals(BscConfig.lastBingoSession)) {
                    BscConfig.completedGoals.clear();
                    BscConfig.lastBingoSession = currentSession;
                    BscConfig.save();
                }

                URL url = new URL(GOAL_URL + "?t=" + System.currentTimeMillis());
                List<String> downloadedGoals = new ArrayList<>();

                try (Scanner s = new Scanner(url.openStream())) {
                    while (s.hasNextLine()) {
                        String line = s.nextLine().trim();
                        if (line.isEmpty()) continue;

                        // Only add to the visible HUD if we haven't finished it this month
                        String cleanLine = line.toLowerCase();
                        boolean alreadyDone = BscConfig.completedGoals.stream()
                                .anyMatch(done -> done.contains(cleanLine));

                        if (!alreadyDone) downloadedGoals.add(line);
                    }
                }
                synchronized (goals) {
                    goals.clear();
                    goals.addAll(downloadedGoals);
                }
            } catch (Exception e) {
                synchronized (goals) {
                    goals.clear();
                    goals.add("§cFetch Failed");
                }
            } finally {
                isLoading = false;
            }
        });
    }

    public static void renderCard(GuiGraphics context, Font textRenderer) {
        context.pose().pushMatrix();
        context.pose().translateLocal(BscConfig.bingoHudX, BscConfig.bingoHudY);
        float scale = BscConfig.bingoHudScale;
        context.pose().scale(scale, scale);

        synchronized (goals) {
            if (goals.isEmpty()) {
                if (!isLoading) context.drawString(textRenderer, "§aAll Goals Done!", 0, 0, BscConfig.cardTitleColor, true);
            } else {
                context.drawString(textRenderer, "Bingo Goals:", 0, 0, BscConfig.cardTitleColor, true);
                int offset = 12;
                for (String goal : goals) {
                    context.drawString(textRenderer, "§7- §f" + goal, 4, offset, BscConfig.cardTextColor, true);
                    offset += 10;
                }
            }
        }
        context.pose().popMatrix();
    }

    public static void renderTimer(GuiGraphics context, Font textRenderer) {
        context.pose().pushMatrix();
        context.pose().translateLocal(BscConfig.timerHudX, BscConfig.timerHudY);
        float scale = BscConfig.timerHudScale;
        context.pose().scale(scale, scale);
        String label = getBingoTimerLabel();
        String time = getBingoTimerValue();
        context.drawString(textRenderer, label, 0, 0, BscConfig.timerTitleColor, true);
        context.drawString(textRenderer, time, textRenderer.width(label), 0, BscConfig.timerTextColor, true);
        context.pose().popMatrix();
    }

    private static String getBingoTimerLabel() {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime end = now.withDayOfMonth(1).toLocalDate().atStartOfDay(ZoneOffset.UTC).plusDays(7);
        return now.isBefore(end) ? "Bingo Ends: " : "Next Bingo: ";
    }

    private static String getBingoTimerValue() {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime end = now.withDayOfMonth(1).toLocalDate().atStartOfDay(ZoneOffset.UTC).plusDays(7);
        Duration d = now.isBefore(end) ? Duration.between(now, end) : Duration.between(now, now.withDayOfMonth(1).plusMonths(1).toLocalDate().atStartOfDay(ZoneOffset.UTC));
        return String.format("%dd %dh", d.toDays(), d.toHoursPart());
    }
}