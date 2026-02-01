package com.bscmod;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.net.URL;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class BscBingoHud {
    private static final List<BingoGoal> goals = new ArrayList<>();
    private static boolean isLoading = false;
    private static final String GOAL_URL = "https://raw.githubusercontent.com/IQONIQ11/bingo-goals/main/goals.txt";
    private static final ZoneId TARGET_ZONE = ZoneOffset.ofHours(1);

    private record BingoGoal(String name, String description) {}

    public static void register() {
        fetchGoals();

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            String text = message.getString();
            onChatMessage(text);
        });

        HudRenderCallback.EVENT.register((context, tickCounter) -> {
            Minecraft client = Minecraft.getInstance();
            if (client.player == null || client.options.hideGui) return;
            if (client.screen instanceof BscScreen || client.screen instanceof BscHudEditScreen) return;

            if (BscConfig.displayBingoCard) renderCard(context, client.font);
            if (BscConfig.displayBingoTimer) renderTimer(context, client.font);
        });
    }

    private static String getSessionKey() {
        ZonedDateTime now = ZonedDateTime.now(TARGET_ZONE);
        return now.getYear() + "-" + now.getMonthValue();
    }

    public static void onChatMessage(String text) {
        if (text.startsWith("BINGO GOAL COMPLETE! ")) {
            try {
                String cleanText = ChatFormatting.stripFormatting(text).replace("BINGO GOAL COMPLETE!", "").trim();
                synchronized (goals) {
                    List<BingoGoal> toRemove = new ArrayList<>();
                    for (BingoGoal goal : goals) {
                        if (cleanText.toLowerCase().contains(goal.name().toLowerCase().trim())) {
                            toRemove.add(goal);
                        }
                    }
                    for (BingoGoal goal : toRemove) {
                        removeGoal(goal);
                    }
                }
            } catch (Exception ignored) {}
        }
    }

    private static void removeGoal(BingoGoal goal) {
        synchronized (goals) {
            String cleanName = goal.name().trim();
            goals.removeIf(g -> g.name().equalsIgnoreCase(cleanName));

            if (!BscConfig.completedGoals.contains(cleanName)) {
                BscConfig.completedGoals.add(cleanName);
                BscConfig.lastBingoSession = getSessionKey();
                BscConfig.save();
            }
        }
    }

    public static void fetchGoals() {
        if (isLoading) return;
        isLoading = true;

        Util.backgroundExecutor().execute(() -> {
            try {
                String currentSession = getSessionKey();
                if (!currentSession.equals(BscConfig.lastBingoSession)) {
                    BscConfig.completedGoals.clear();
                    BscConfig.lastBingoSession = currentSession;
                    BscConfig.save();
                }

                URL url = new URL(GOAL_URL + "?t=" + System.currentTimeMillis());
                List<BingoGoal> downloadedGoals = new ArrayList<>();

                try (Scanner s = new Scanner(url.openStream())) {
                    while (s.hasNextLine()) {
                        String line = s.nextLine().trim();
                        if (line.isEmpty()) continue;

                        String[] parts = line.split("\\|", 2);
                        if(parts.length != 2) continue;
                        String name = parts[0].trim();
                        String description = parts[1].trim();

                        boolean alreadyDone = BscConfig.completedGoals.stream()
                                .anyMatch(done -> done.equalsIgnoreCase(name));

                        if (!alreadyDone) downloadedGoals.add(new BingoGoal(name, description));
                    }
                }
                synchronized (goals) {
                    goals.clear();
                    goals.addAll(downloadedGoals);
                }
            } catch (Exception e) {
                synchronized (goals) {
                    goals.clear();
                    goals.add(new BingoGoal("Error loading goals", "§cFetch Failed"));
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
                for (BingoGoal goal : goals) {
                    context.drawString(textRenderer, "§7- §f" + goal.description(), 4, offset, BscConfig.cardTextColor, true);
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
        ZonedDateTime now = ZonedDateTime.now(TARGET_ZONE);
        ZonedDateTime start = now.withDayOfMonth(1).withHour(6).withMinute(0).withSecond(0).withNano(0);
        ZonedDateTime end = start.plusDays(7);

        return (now.isAfter(start) && now.isBefore(end)) ? "Bingo Ends: " : "Next Bingo: ";
    }

    private static String getBingoTimerValue() {
        ZonedDateTime now = ZonedDateTime.now(TARGET_ZONE);
        ZonedDateTime start = now.withDayOfMonth(1).withHour(6).withMinute(0).withSecond(0).withNano(0);
        ZonedDateTime end = start.plusDays(7);

        Duration d;
        if (now.isBefore(start)) {
            d = Duration.between(now, start);
        } else if (now.isBefore(end)) {
            d = Duration.between(now, end);
        } else {
            ZonedDateTime nextMonthStart = start.plusMonths(1).withDayOfMonth(1).withHour(6).withMinute(0).withSecond(0).withNano(0);
            d = Duration.between(now, nextMonthStart);
        }

        return String.format("%dd %dh %dm", d.toDays(), d.toHoursPart(), d.toMinutesPart());
    }
}