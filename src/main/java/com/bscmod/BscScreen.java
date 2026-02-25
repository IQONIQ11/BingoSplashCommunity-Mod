package com.bscmod;

import net.minecraft.util.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.joml.Vector2f;
import org.lwjgl.glfw.GLFW;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

@SuppressWarnings("SpellCheckingInspection")
public class BscScreen extends Screen {
    private final Screen parent;
    private static final String DISCORD_URL = "https://discord.gg/bingo";
    private static final Identifier DISCORD_ICON = Identifier.fromNamespaceAndPath("bingosplashcommunity", "textures/gui/discord.png");
    private static final Identifier MOD_LOGO = Identifier.fromNamespaceAndPath("bingosplashcommunity", "textures/gui/logo.png");
    private static final ZoneId TARGET_ZONE = ZoneOffset.UTC;

    private int x, y, windowWidth, windowHeight;
    private int currentTab = 0;
    private float scrollAmount = 0;
    private boolean isDraggingScrollbar = false;
    private boolean isDraggingSlider = false;
    private boolean waitingForKey = false;

    private final int[] PRESET_COLORS = {0xFF00FFFF, 0xFFFF5555, 0xFF55FF55, 0xFFFFFF55, 0xFFFF55FF, 0xFFFFAA00, 0xFFFFFFFF};

    private static final int SIDEBAR_WIDTH = 120;
    private static final float CONTENT_SCALE = 1.3f;
    private static final int SPACING = 35;
    private static final int SCROLLBAR_WIDTH = 4;
    private static final int SCROLLBAR_MARGIN = 4;
    private static final int LOGO_SIZE = 100;

    public BscScreen(Screen parent) {
        super(Component.literal("BSC Mod Config"));
        this.parent = parent;
        BscBingoHud.fetchGoals();
    }

    @Override
    protected void init() {
        this.windowWidth = (int) (this.width * 0.85f);
        this.windowHeight = (int) (this.height * 0.85f);
        this.x = (this.width - windowWidth) / 2;
        this.y = (this.height - windowHeight) / 2;
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0xAA000000);
        context.fill(x, y, x + windowWidth, y + windowHeight, 0xFF121212);
        context.fill(x, y, x + SIDEBAR_WIDTH, y + windowHeight, 0xFF181818);
        context.fill(x + SIDEBAR_WIDTH, y, x + SIDEBAR_WIDTH + 1, y + windowHeight, 0xFF2A2A2A);

        int logoX = x + (SIDEBAR_WIDTH - LOGO_SIZE) / 2;
        int logoY = y + 12;
        context.blit(RenderPipelines.GUI_TEXTURED, MOD_LOGO, logoX, logoY, 0.0f, 0.0f, LOGO_SIZE, LOGO_SIZE, LOGO_SIZE, LOGO_SIZE);

        drawTab(context, "General", y + 125, currentTab == 0);
        drawTab(context, "Splashes", y + 155, currentTab == 1);
        drawTab(context, "Mining", y + 185, currentTab == 2);
        drawTab(context, "Events", y + 215, currentTab == 3);
        drawTab(context, "Bingo Roles", y + 245, currentTab == 4);

        context.pose().pushMatrix();
        context.pose().translateLocal(new Vector2f(x + 140f, y + 25f));
        context.pose().scale(CONTENT_SCALE, CONTENT_SCALE);

        String headerTitle = switch (currentTab) {
            case 0 -> "General Settings";
            case 1 -> "Splash Notifications";
            case 2 -> "Mining Pings";
            case 3 -> "Event Pings";
            case 4 -> "Community Roles";
            default -> "";
        };
        drawSectionHeader(context, headerTitle);
        context.pose().popMatrix();

        int scissorX = x + 121;
        int scissorY = y + 45;
        int scissorW = windowWidth - 121;
        int scissorH = windowHeight - 75;
        context.enableScissor(scissorX, scissorY, scissorX + scissorW, scissorY + scissorH);

        double relMouseX = (mouseX - (x + 140)) / CONTENT_SCALE;
        double relMouseY = (mouseY - (y + 50)) / CONTENT_SCALE;

        context.pose().pushMatrix();
        context.pose().translateLocal(new Vector2f((float) x + 140f, (float) y + 50f));
        context.pose().scale(CONTENT_SCALE, CONTENT_SCALE);
        renderTabContent(context, relMouseX, relMouseY);
        context.pose().popMatrix();
        context.disableScissor();

        if (currentTab != 1) renderScrollbar(context, mouseX, mouseY);

        renderFooter(context);

        if (isHovering(mouseX, mouseY, logoX, logoY, LOGO_SIZE, LOGO_SIZE)) {
            context.setTooltipForNextFrame(this.font, List.of(
                    Component.literal("§b§lBSC Mod").getVisualOrderText(),
                    Component.literal("§7Logo made by May").getVisualOrderText()
            ), mouseX, mouseY);
        }

        if (isDraggingSlider && currentTab == 1) {
            int tx = (int) (((windowWidth - 190) / CONTENT_SCALE) - 22);
            float sliderWidth = 50f;
            float percent = (float) ((relMouseX - tx) / sliderWidth);
            percent = Math.max(0, Math.min(1, percent));
            BscConfig.alertDuration = 1 + Math.round(percent * 9);
        }
    }

    private void renderFooter(GuiGraphics context) {
        int footerY = y + windowHeight - 18;
        String discordText = "Discord";
        int iconSize = 10;
        int gap = 3;
        int totalDiscordWidth = iconSize + gap + this.font.width(discordText);
        int discordStartX = x + (SIDEBAR_WIDTH - totalDiscordWidth) / 2;

        context.blit(RenderPipelines.GUI_TEXTURED, DISCORD_ICON, discordStartX, footerY - 2, 0.0f, 0.0f, iconSize, iconSize, iconSize, iconSize);
        context.drawString(this.font, "§9" + discordText, discordStartX + iconSize + gap, footerY - 1, 0xFFFFFFFF, true);

        int startX = x + SIDEBAR_WIDTH + 15;
        int endX = x + windowWidth - 15;
        int totalWidth = endX - startX;

        String[] items = {
                "§7Profile: §e" + HypixelUtils.getProfileType(),
                "§bComms: §f" + getHypixelResetTimer(),
                "§dNPC: §f" + getUtcResetTimer(),
                "§aZoo: §f" + getZooTimer(),
                getBingoTimerString()
        };

        for (int i = 0; i < items.length; i++) {
            String text = items[i];
            int textWidth = this.font.width(text);
            float progress = (float) i / (items.length - 1);
            int targetX = (int) (startX + (progress * (totalWidth - textWidth)));
            context.drawString(this.font, text, targetX, footerY, 0xFFFFFFFF, true);
        }
    }

    private String getHypixelResetTimer() {
        ZonedDateTime now = ZonedDateTime.now(TARGET_ZONE);
        ZonedDateTime reset = now.toLocalDate().atTime(5, 0).atZone(TARGET_ZONE);
        if (now.isAfter(reset)) reset = reset.plusDays(1);
        Duration d = Duration.between(now, reset);
        return String.format("%02dh %02dm", d.toHours(), d.toMinutesPart());
    }

    private String getUtcResetTimer() {
        ZonedDateTime now = ZonedDateTime.now(TARGET_ZONE);
        ZonedDateTime reset = now.toLocalDate().atTime(1, 0).atZone(TARGET_ZONE);
        if (now.isAfter(reset)) reset = reset.plusDays(1);
        Duration d = Duration.between(now, reset);
        return String.format("%02dh %02dm", d.toHours(), d.toMinutesPart());
    }

    private String getZooTimer() {
        ZonedDateTime nowZoned = ZonedDateTime.now(TARGET_ZONE);
        long nowMillis = nowZoned.toInstant().toEpochMilli();
        ZonedDateTime ref = ZonedDateTime.of(2026, 2, 1, 18, 55, 0, 0, TARGET_ZONE);
        long refMillis = ref.toInstant().toEpochMilli();

        long interval = Duration.ofDays(2).plusHours(14).plusMinutes(20).toMillis();
        long dur = Duration.ofHours(1).toMillis();
        long diff = nowMillis - refMillis;

        if (diff < 0) return formatDuration(Duration.ofMillis(Math.abs(diff)));

        long pos = diff % interval;
        if (pos < dur) {
            long remaining = dur - pos;
            return "§6§lACTIVE §e(" + Duration.ofMillis(remaining).toMinutesPart() + "m)";
        }
        return formatDuration(Duration.ofMillis(interval - pos));
    }

    private String formatDuration(Duration d) {
        if (d.toDays() > 0) return String.format("%dd %dh %dm", d.toDays(), d.toHoursPart(), d.toMinutesPart());
        return String.format("%dh %dm", d.toHours(), d.toMinutesPart());
    }

    private String getBingoTimerString() {
        ZonedDateTime now = ZonedDateTime.now(TARGET_ZONE);
        ZonedDateTime start = now.withDayOfMonth(1).withHour(5).withMinute(0).withSecond(0).withNano(0);
        ZonedDateTime end = start.plusDays(7);
        Duration d;
        String prefix;

        if (now.isBefore(start)) {
            prefix = "§bNext Bingo: §f";
            d = Duration.between(now, start);
        } else if (now.isBefore(end)) {
            prefix = "§6Bingo Ends: §f";
            d = Duration.between(now, end);
        } else {
            prefix = "§bNext Bingo: §f";
            d = Duration.between(now, start.plusMonths(1).withDayOfMonth(1).withHour(5).withMinute(0).withSecond(0).withNano(0));
        }

        return prefix + String.format("%dd %dh %dm", d.toDays(), d.toHoursPart(), d.toMinutesPart());
    }

    private void renderTabContent(GuiGraphics context, double relMouseX, double relMouseY) {
        int currY = -(int) scrollAmount;

        if (currentTab == 0) {
            drawSubHeader(context, "Mod Settings", currY);
            currY += 20;
            drawSetting(context, "Mod Toggle", "Toggles On/Off for the entire mod.", currY, BscConfig.receivePings, relMouseX, relMouseY);
            currY += SPACING;
            drawActionSetting(context, currY, relMouseX, relMouseY);
            currY += SPACING;
            drawKeybindSetting(context, waitingForKey ? "§e???" : "§b" + BingoSplashCommunity.getSettingsKeyName(), currY, relMouseX, relMouseY);
            currY += (SPACING + 10);
            drawSubHeader(context, "Bingo Card", currY);
            currY += 20;
            drawSetting(context, "Display Bingo Card", "HUD displaying remaining Bingo goals.", currY, BscConfig.displayBingoCard, relMouseX, relMouseY);
            currY += SPACING;
            drawSetting(context, "Bingo Profile Only", "Only show card on Bingo profile.", currY, BscConfig.bingoCardBingoProfileOnly, relMouseX, relMouseY);
            currY += SPACING;
            drawColorSetting(context, "Card Title", BscConfig.cardTitleColor, currY, relMouseX, relMouseY);
            currY += SPACING;
            drawColorSetting(context, "Card Text", BscConfig.cardTextColor, currY, relMouseX, relMouseY);
            currY += (SPACING + 10);
            drawSubHeader(context, "Bingo Timer", currY);
            currY += 20;
            drawSetting(context, "Display Bingo Timer", "Show the event timer on your HUD.", currY, BscConfig.displayBingoTimer, relMouseX, relMouseY);
            currY += SPACING;
            drawColorSetting(context, "Timer Title", BscConfig.timerTitleColor, currY, relMouseX, relMouseY);
            currY += SPACING;
            drawColorSetting(context, "Timer Text", BscConfig.timerTextColor, currY, relMouseX, relMouseY);
            currY += (SPACING + 20);
            boolean hReset = relMouseX >= 0 && relMouseX <= 80 && relMouseY >= currY && relMouseY <= currY + 15;
            context.fill(0, currY, 80, currY + 15, hReset ? 0xFFFF5555 : 0xFF882222);
            context.drawCenteredString(this.font, "Reset Defaults", 40, currY + 3, 0xFFFFFFFF);
        } else if (currentTab == 1) {
            drawSetting(context, "Show Screen Alerts", "Display a large title in the center of your screen.", 10, BscConfig.showTitle, relMouseX, relMouseY);
            drawSliderSetting(context, "Alert Duration", BscConfig.alertDuration, 40, relMouseX, relMouseY);
            drawColorSetting(context, "Alert Color", BscConfig.titleColor, 75, relMouseX, relMouseY);
            drawSetting(context, "Play Sound", "Play a notification sound when a splash is detected.", 110, BscConfig.playSound, relMouseX, relMouseY);
            drawSetting(context, "Ironman Only", "Only show splash notifications while on an Ironman profile.", 145, BscConfig.ironmanOnly, relMouseX, relMouseY);
            drawSetting(context, "Bingo Only", "Only show splash notifications while on a Bingo profile.", 180, BscConfig.bingoOnly, relMouseX, relMouseY);
            drawSetting(context, "Hub Warp Button", "Adds a [WARP] button to splash notifications.", 215, BscConfig.showHubWarp, relMouseX, relMouseY);
        } else if (currentTab == 4) {
            String lastCat = "";
            for (BingoRolesRenderer.RoleData role : BingoRolesRenderer.ROLES) {
                if (!role.category.equals(lastCat)) {
                    lastCat = role.category;
                    context.fill(0, currY + 10, (int) ((windowWidth - 160) / CONTENT_SCALE), currY + 11, 0xFF333333);
                    context.drawString(this.font, "§6§l" + lastCat, 0, currY + 14, 0xFFFFAA00, true);
                    currY += 28;
                }
                drawBadge(context, role.name, role.color, currY);
                context.drawString(this.font, "§8" + role.desc, 4, currY + 14, 0xFFAAAAAA, false);
                currY += 32;
            }
        } else {
            int count = (currentTab == 2) ? 12 : 6;
            for (int i = 0; i < count; i++) {
                String subText = (currentTab == 2) ? "Receive a notification when this item is found." : "Receive an alert when this event begins.";
                drawSetting(context, (currentTab == 2) ? getMiningTitle(i) : getEventTitle(i), subText, currY + (SPACING * i), (currentTab == 2) ? getMiningValue(i) : getEventValue(i), relMouseX, relMouseY);
            }
        }
    }

    private void drawSubHeader(GuiGraphics context, String title, int yPos) {
        context.drawString(this.font, "§6§l" + title, 0, yPos, 0xFFFFFFFF, true);
        context.fill(0, yPos + 10, (int) ((windowWidth - 160) / CONTENT_SCALE), yPos + 11, 0xFF2A2A2A);
    }

    private void drawSetting(GuiGraphics context, String title, String desc, int yPos, boolean enabled, double relMouseX, double relMouseY) {
        int tx = (int) ((windowWidth - 190) / CONTENT_SCALE);
        if (isHovering(relMouseX, relMouseY, 0, yPos - 2, tx + 35, 20)) context.fill(-2, yPos - 2, tx + 35, yPos + 22, 0x22FFFFFF);
        context.drawString(this.font, title, 0, yPos, 0xFFFFFFFF, true);
        context.drawString(this.font, "§8" + desc, 0, yPos + 10, 0xFF888888, true);
        context.fill(tx, yPos + 2, tx + 28, yPos + 14, enabled ? 0xFF3574F0 : 0xFF444444);
        int kX = enabled ? tx + 16 : tx + 2;
        context.fill(kX, yPos + 4, kX + 10, yPos + 12, 0xFFFFFFFF);
    }

    private void drawSliderSetting(GuiGraphics context, String label, long value, int yPos, double relX, double relY) {
        int tx = (int) (((windowWidth - 190) / CONTENT_SCALE) - 22);
        if (isHovering(relX, relY, 0, yPos - 2, tx + 55, 20)) context.fill(-2, yPos - 2, tx + 55, yPos + 22, 0x22FFFFFF);
        context.drawString(this.font, label, 0, yPos, 0xFFFFFFFF, true);
        context.drawString(this.font, "§8Duration: §e" + value + "s", 0, yPos + 10, 0xFF888888, true);
        int sliderWidth = 50;
        context.fill(tx, yPos + 6, tx + sliderWidth, yPos + 10, 0xFF444444);
        float progress = (value - 1) / 9f;
        int knobX = tx + (int) (progress * sliderWidth);
        context.fill(knobX - 2, yPos + 4, knobX + 2, yPos + 12, 0xFF3574F0);
    }

    private void drawActionSetting(GuiGraphics context, int yPos, double relMouseX, double relMouseY) {
        int tx = (int) ((windowWidth - 190) / CONTENT_SCALE);
        if (isHovering(relMouseX, relMouseY, 0, yPos - 2, tx + 35, 20)) context.fill(-2, yPos - 2, tx + 35, yPos + 22, 0x22FFFFFF);
        context.drawString(this.font, "Edit HUD Position", 0, yPos, 0xFFFFFFFF, true);
        context.drawString(this.font, "§8Click to drag and move/resize HUD.", 0, yPos + 10, 0xFF888888, true);
        context.fill(tx - 10, yPos + 2, tx + 28, yPos + 14, 0xFF5555FF);
        context.drawCenteredString(this.font, "EDIT", tx + 9, yPos + 4, 0xFFFFFFFF);
    }

    private void drawKeybindSetting(GuiGraphics context, String key, int yPos, double relX, double relY) {
        int tx = (int) ((windowWidth - 190) / CONTENT_SCALE);
        if (isHovering(relX, relY, 0, yPos - 2, tx + 35, 20)) context.fill(-2, yPos - 2, tx + 35, yPos + 22, 0x22FFFFFF);
        context.drawString(this.font, "Menu Keybind", 0, yPos, 0xFFFFFFFF, true);
        context.drawString(this.font, "§8Key to open this menu.", 0, yPos + 10, 0xFF888888, true);
        context.fill(tx - 20, yPos + 2, tx + 28, yPos + 14, 0xFF2A2A2A);
        context.drawCenteredString(this.font, key, tx + 4, yPos + 4, 0xFFFFFFFF);
    }

    private void drawColorSetting(GuiGraphics context, String label, int color, int yPos, double relX, double relY) {
        int tx = (int) ((windowWidth - 190) / CONTENT_SCALE);
        if (isHovering(relX, relY, 0, yPos - 2, tx + 35, 20)) context.fill(-2, yPos - 2, tx + 35, yPos + 22, 0x22FFFFFF);
        context.drawString(this.font, label, 0, yPos, 0xFFFFFFFF, true);
        context.drawString(this.font, "§8Click the box to cycle color.", 0, yPos + 10, 0xFF888888, true);
        context.fill(tx, yPos + 2, tx + 28, yPos + 14, 0xFFFFFFFF);
        context.fill(tx + 2, yPos + 4, tx + 26, yPos + 12, 0xFF000000 | color);
    }

    private void drawBadge(GuiGraphics context, String text, int color, int yPos) {
        int tw = this.font.width(text);
        context.fill(0, yPos, tw + 12, yPos + 12, 0x33000000);
        context.fill(0, yPos, 2, yPos + 12, 0xFF000000 | color);
        context.drawString(this.font, text, 6, yPos + 2, 0xFF000000 | color, false);
    }

    private void drawTab(GuiGraphics context, String name, int yPos, boolean sel) {
        if (sel) context.fill(x + 5, yPos - 5, x + 115, yPos + 12, 0x445555FF);
        context.drawString(this.font, (sel ? "§f" : "§7") + name, x + 15, yPos, 0xFFFFFFFF, true);
    }

    private void drawSectionHeader(GuiGraphics context, String title) {
        context.drawString(this.font, "§b" + title, 0, 0, 0xFFFFFFFF, true);
        context.fill(0, 12, (int) ((windowWidth - 160) / CONTENT_SCALE), 13, 0xFF2A2A2A);
    }

    private void renderScrollbar(GuiGraphics context, int mx, int my) {
        float max = getMaxScroll();
        if (max <= 0) return;
        int tH = windowHeight - 80;
        int sH = Math.max(20, (int) ((tH / (tH + max)) * tH));
        int sX = x + windowWidth - SCROLLBAR_WIDTH - SCROLLBAR_MARGIN;
        if (this.isDraggingScrollbar) {
            float p = (float) (my - (y + 50)) / tH;
            scrollAmount = Math.max(0, Math.min(max, p * max));
        }
        int sY = y + 50 + (int) ((scrollAmount / max) * (tH - sH));
        boolean h = mx >= sX && mx <= sX + SCROLLBAR_WIDTH && my >= sY && my <= sY + sH;
        context.fill(sX, y + 50, sX + SCROLLBAR_WIDTH, y + 50 + tH, 0xFF222222);
        context.fill(sX, sY, sX + SCROLLBAR_WIDTH, sY + sH, (h || isDraggingScrollbar) ? 0xFF7777FF : 0xFF5555FF);
    }

    private float getMaxScroll() {
        return switch (currentTab) {
            case 0 -> 280;
            case 1 -> 50;
            case 2 -> 250;
            case 4 -> (BingoRolesRenderer.ROLES.size() * 40);
            default -> 0;
        };
    }

    private String getMiningTitle(int i) { return switch (i) { case 0 -> "Synthetic Heart"; case 1 -> "Robotron Reflector"; case 2 -> "Control Switch"; case 3 -> "Superlite Motor"; case 4 -> "Electron Transmitter"; case 5 -> "FTX 3070"; case 6 -> "Armadillo Egg"; case 7 -> "Jungle Key"; case 8 -> "Pickonimbus 2000"; case 9 -> "Powder"; case 10 -> "Goblin Egg"; case 11 -> "Flawless Gemstone"; default -> ""; }; }
    private boolean getMiningValue(int i) { return switch (i) { case 0 -> BscConfig.syncHeart; case 1 -> BscConfig.robotron; case 2 -> BscConfig.controlSwitch; case 3 -> BscConfig.motor; case 4 -> BscConfig.transmitter; case 5 -> BscConfig.ftx3070; case 6 -> BscConfig.armadillo; case 7 -> BscConfig.jungleKey; case 8 -> BscConfig.picko; case 9 -> BscConfig.powder; case 10 -> BscConfig.goblinEgg; case 11 -> BscConfig.flawlessGem; default -> false; }; }
    private String getEventTitle(int i) { return switch (i) { case 0 -> "2x Powder"; case 1 -> "Goblin Raid"; case 2 -> "Raffle"; case 3 -> "Better Together"; case 4 -> "Gone with the Wind"; case 5 -> "Mithril Gourmand"; default -> ""; }; }
    private boolean getEventValue(int i) { return switch (i) { case 0 -> BscConfig.powder2x; case 1 -> BscConfig.goblinRaid; case 2 -> BscConfig.raffle; case 3 -> BscConfig.betterTogether; case 4 -> BscConfig.goneWind; case 5 -> BscConfig.mithrilGourmand; default -> false; }; }
    private void toggleMining(int i) { switch (i) { case 0 -> BscConfig.syncHeart = !BscConfig.syncHeart; case 1 -> BscConfig.robotron = !BscConfig.robotron; case 2 -> BscConfig.controlSwitch = !BscConfig.controlSwitch; case 3 -> BscConfig.motor = !BscConfig.motor; case 4 -> BscConfig.transmitter = !BscConfig.transmitter; case 5 -> BscConfig.ftx3070 = !BscConfig.ftx3070; case 6 -> BscConfig.armadillo = !BscConfig.armadillo; case 7 -> BscConfig.jungleKey = !BscConfig.jungleKey; case 8 -> BscConfig.picko = !BscConfig.picko; case 9 -> BscConfig.powder = !BscConfig.powder; case 10 -> BscConfig.goblinEgg = !BscConfig.goblinEgg; case 11 -> BscConfig.flawlessGem = !BscConfig.flawlessGem; } }
    private void toggleEvent(int i) { switch (i) { case 0 -> BscConfig.powder2x = !BscConfig.powder2x; case 1 -> BscConfig.goblinRaid = !BscConfig.goblinRaid; case 2 -> BscConfig.raffle = !BscConfig.raffle; case 3 -> BscConfig.betterTogether = !BscConfig.betterTogether; case 4 -> BscConfig.goneWind = !BscConfig.goneWind; case 5 -> BscConfig.mithrilGourmand = !BscConfig.mithrilGourmand; } }

    private void resetAllToDefaults() {
        BscConfig.receivePings = true; BscConfig.showTitle = true; BscConfig.playSound = true; BscConfig.titleColor = 0xFF00FFFF;
        BscConfig.displayBingoCard = false; BscConfig.displayBingoTimer = false;
        BscConfig.bingoCardBingoProfileOnly = false;
        BscConfig.cardTitleColor = 0xFF00FFFF; BscConfig.cardTextColor = 0xFFFFFFFF;
        BscConfig.timerTitleColor = 0xFF00FFFF; BscConfig.timerTextColor = 0xFFFFFFFF;
        BscConfig.alertDuration = 1;
        BscConfig.showHubWarp = true;
        BingoSplashCommunity.resetKeybind(); BscConfig.save();
    }

    private int getNextColor(int c) {
        int idx = 0;
        for (int i = 0; i < PRESET_COLORS.length; i++) if (PRESET_COLORS[i] == c) idx = i;
        return PRESET_COLORS[(idx + 1) % PRESET_COLORS.length];
    }

    private boolean isHovering(double mx, double my, double x, double y, double w, double h) { return mx >= x && mx <= x + w && my >= y && my <= y + h; }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        this.isDraggingScrollbar = false;
        if (this.isDraggingSlider) {
            this.isDraggingSlider = false;
            BscConfig.save();
        }
        return super.mouseReleased(event);
    }

    @Override public boolean mouseScrolled(double mx, double my, double h, double v) { scrollAmount = Math.max(0, Math.min(getMaxScroll(), scrollAmount - (float) (v * 20))); return true; }

    @Override public boolean keyPressed(KeyEvent keyEvent) {
        if (waitingForKey) {
            if (keyEvent.key() == GLFW.GLFW_KEY_ESCAPE) BingoSplashCommunity.updateKeybind(null);
            else BingoSplashCommunity.updateKeybind(keyEvent.key());
            waitingForKey = false; BscConfig.save(); return true;
        }
        if (keyEvent.key() == GLFW.GLFW_KEY_ESCAPE) { if (this.minecraft != null) this.minecraft.setScreen(parent); return true; }
        return super.keyPressed(keyEvent);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean bl) {
        if (waitingForKey) return false;
        int sX = x + windowWidth - SCROLLBAR_WIDTH - SCROLLBAR_MARGIN;
        if (mouseButtonEvent.x() >= sX && mouseButtonEvent.x() <= sX + SCROLLBAR_WIDTH && mouseButtonEvent.y() >= y + 50) { this.isDraggingScrollbar = true; return true; }

        if (isHovering(mouseButtonEvent.x(), mouseButtonEvent.y(), x + 5, y + 120, 110, 20)) { currentTab = 0; scrollAmount = 0; return true; }
        if (isHovering(mouseButtonEvent.x(), mouseButtonEvent.y(), x + 5, y + 150, 110, 20)) { currentTab = 1; scrollAmount = 0; return true; }
        if (isHovering(mouseButtonEvent.x(), mouseButtonEvent.y(), x + 5, y + 180, 110, 20)) { currentTab = 2; scrollAmount = 0; return true; }
        if (isHovering(mouseButtonEvent.x(), mouseButtonEvent.y(), x + 5, y + 210, 110, 20)) { currentTab = 3; scrollAmount = 0; return true; }
        if (isHovering(mouseButtonEvent.x(), mouseButtonEvent.y(), x + 5, y + 240, 110, 20)) { currentTab = 4; scrollAmount = 0; return true; }

        double relX = (mouseButtonEvent.x() - (x + 140)) / CONTENT_SCALE;
        double relY = (mouseButtonEvent.y() - (y + 50)) / CONTENT_SCALE;
        int tx = (int) ((windowWidth - 190) / CONTENT_SCALE);

        if (currentTab == 0) {
            int cY = -(int) scrollAmount + 20;
            if (isHovering(relX, relY, tx, cY, 28, 12)) { BscConfig.receivePings = !BscConfig.receivePings; BscConfig.save(); return true; }
            cY += SPACING;
            if (isHovering(relX, relY, tx - 10, cY, 38, 12)) { if (this.minecraft != null) this.minecraft.setScreen(new BscHudEditScreen(this)); return true; }
            cY += SPACING;
            if (isHovering(relX, relY, tx - 20, cY, 48, 12)) { waitingForKey = true; return true; }
            cY += (SPACING + 30);
            if (isHovering(relX, relY, tx, cY, 28, 12)) { BscConfig.displayBingoCard = !BscConfig.displayBingoCard; BscConfig.save(); return true; }
            cY += SPACING;
            if (isHovering(relX, relY, tx, cY, 28, 12)) { BscConfig.bingoCardBingoProfileOnly = !BscConfig.bingoCardBingoProfileOnly; BscConfig.save(); return true; }
            cY += SPACING;
            if (isHovering(relX, relY, tx, cY, 28, 12)) { BscConfig.cardTitleColor = getNextColor(BscConfig.cardTitleColor); BscConfig.save(); return true; }
            cY += SPACING;
            if (isHovering(relX, relY, tx, cY, 28, 12)) { BscConfig.cardTextColor = getNextColor(BscConfig.cardTextColor); BscConfig.save(); return true; }
            cY += (SPACING + 30);
            if (isHovering(relX, relY, tx, cY, 28, 12)) { BscConfig.displayBingoTimer = !BscConfig.displayBingoTimer; BscConfig.save(); return true; }
            cY += SPACING;
            if (isHovering(relX, relY, tx, cY, 28, 12)) { BscConfig.timerTitleColor = getNextColor(BscConfig.timerTitleColor); BscConfig.save(); return true; }
            cY += SPACING;
            if (isHovering(relX, relY, tx, cY, 28, 12)) { BscConfig.timerTextColor = getNextColor(BscConfig.timerTextColor); BscConfig.save(); return true; }
            cY += (SPACING + 20);
            if (isHovering(relX, relY, 0, cY, 80, 15)) { resetAllToDefaults(); return true; }
        } else if (currentTab == 1) {
            int cY = -(int) scrollAmount;
            if (isHovering(relX, relY, tx, cY + 10, 28, 12)) { BscConfig.showTitle = !BscConfig.showTitle; BscConfig.save(); return true; }
            if (isHovering(relX, relY, tx - 22, cY + 40, 50, 20)) { this.isDraggingSlider = true; return true; }
            if (isHovering(relX, relY, tx, cY + 75, 28, 12)) { BscConfig.titleColor = getNextColor(BscConfig.titleColor); BscConfig.save(); return true; }
            if (isHovering(relX, relY, tx, cY + 110, 28, 12)) { BscConfig.playSound = !BscConfig.playSound; BscConfig.save(); return true; }
            if (isHovering(relX, relY, tx, cY + 145, 28, 12)) { BscConfig.ironmanOnly = !BscConfig.ironmanOnly; BscConfig.save(); return true; }
            if (isHovering(relX, relY, tx, cY + 180, 28, 12)) { BscConfig.bingoOnly = !BscConfig.bingoOnly; BscConfig.save(); return true; }
            if (isHovering(relX, relY, tx, cY + 215, 28, 12)) { BscConfig.showHubWarp = !BscConfig.showHubWarp; BscConfig.save(); return true; }
        } else {
            int sY = -(int) scrollAmount;
            for (int i = 0; i < (currentTab == 2 ? 12 : 6); i++) {
                if (isHovering(relX, relY, tx, sY + (SPACING * i), 28, 12)) {
                    if (currentTab == 2) toggleMining(i); else toggleEvent(i);
                    BscConfig.save(); return true;
                }
            }
        }
        if (isHovering(mouseButtonEvent.x(), mouseButtonEvent.y(), x + 10, y + windowHeight - 20, 80, 15)) { Util.getPlatform().openUri(DISCORD_URL); return true; }
        return false;
    }
}