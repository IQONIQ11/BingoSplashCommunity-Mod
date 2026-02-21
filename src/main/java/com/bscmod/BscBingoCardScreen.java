package com.bscmod;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.util.FormattedCharSequence;
import java.util.ArrayList;
import java.util.List;

public class BscBingoCardScreen extends Screen {
    private final String playerName;
    private final String month;
    private final List<String> goalsRaw;

    public BscBingoCardScreen(String playerName, String month, List<String> goals) {
        super(Component.literal("Bingo Card"));
        this.playerName = playerName;
        this.month = month;
        this.goalsRaw = goals;
    }

    private boolean isCommunityGoal(int index) {
        return (index == 0 || index == 6 || index == 12 || index == 18 || index == 24);
    }

    private void drawBorder(GuiGraphics guiGraphics, int x, int y, int width, int height, int color) {
        guiGraphics.fill(x, y, x + width, y + 1, color);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, color);
        guiGraphics.fill(x, y, x + 1, y + height, color);
        guiGraphics.fill(x + width - 1, y, x + width, y + height, color);
    }

    private void renderCustomTooltip(GuiGraphics guiGraphics, List<FormattedCharSequence> lines, int mouseX, int mouseY) {
        if (lines.isEmpty()) return;
        int width = 0;
        for (FormattedCharSequence line : lines) {
            int lineWidth = this.font.width(line);
            if (lineWidth > width) width = lineWidth;
        }
        int x = mouseX + 12;
        int y = mouseY - 12;
        int height = 8 + (lines.size() > 1 ? (lines.size() - 1) * 10 : 0);
        if (x + width > this.width) x -= 28 + width;
        if (y + height > this.height) y = Math.max(4, Math.min(this.height - height - 4, y));

        guiGraphics.fill(x - 3, y - 4, x + width + 3, y + height + 4, 0xF0100010);
        drawBorder(guiGraphics, x - 3, y - 4, width + 6, height + 8, 0x505000FF);

        int textY = y;
        for (FormattedCharSequence line : lines) {
            guiGraphics.drawString(this.font, line, x, textY, -1, true);
            textY += 10;
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(0, 0, this.width, this.height, 0x90000000);

        int slotSize = 54;
        int padding = 4;
        int totalGridSize = (slotSize + padding) * 5;

        int startX = (this.width - totalGridSize) / 2;
        int startY = (this.height - totalGridSize) / 2 + 35;

        int boxTop = startY - 50;

        guiGraphics.fill(startX - 15, boxTop, startX + totalGridSize + 15, startY + totalGridSize + 15, 0xFF181818);
        drawBorder(guiGraphics, startX - 15, boxTop, totalGridSize + 30, totalGridSize + 65, 0xFF555555);

        String headerText = playerName + "'s " + month + " Bingo Card";
        int headerWidth = this.font.width(headerText);
        int headerX = (this.width - headerWidth) / 2;
        int headerY = boxTop + 16;

        guiGraphics.drawString(this.font, headerText, headerX, headerY, 0xFFFFFFFF, true);

        List<FormattedCharSequence> activeTooltipLines = null;

        for (int i = 0; i < 25; i++) {
            if (i >= goalsRaw.size()) break;
            String[] data = goalsRaw.get(i).split("::", 3);

            String name = data[0];
            String lore = data.length > 1 ? data[1] : "No description available";
            boolean isCompleted = data.length > 2 && data[2].equalsIgnoreCase("true");

            int x = startX + ((i % 5) * (slotSize + padding));
            int y = startY + ((i / 5) * (slotSize + padding));

            boolean community = isCommunityGoal(i);
            int bgColor = isCompleted ? 0x8022AA22 : (community ? 0x80AA8822 : 0x80333333);
            int borderColor = isCompleted ? 0xFF55FF55 : (community ? 0xFFFFAA00 : 0xFF555555);

            if (mouseX >= x && mouseX <= x + slotSize && mouseY >= y && mouseY <= y + slotSize) {
                bgColor = isCompleted ? 0xAA22AA22 : (community ? 0xAAAA8822 : 0xAA555555);
                activeTooltipLines = new ArrayList<>();
                activeTooltipLines.add(Component.literal(name).withStyle(community ? ChatFormatting.GOLD : ChatFormatting.YELLOW).getVisualOrderText());
                activeTooltipLines.addAll(this.font.split(Component.literal(lore).withStyle(ChatFormatting.GRAY), 180));
                activeTooltipLines.add(Component.literal("").getVisualOrderText());
                activeTooltipLines.add(Component.literal(isCompleted ? "✔ COMPLETED" : "✖ INCOMPLETE").withStyle(isCompleted ? ChatFormatting.GREEN : ChatFormatting.RED).getVisualOrderText());
                if (community) activeTooltipLines.add(Component.literal("Community Goal").withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC).getVisualOrderText());
            }

            guiGraphics.fill(x, y, x + slotSize, y + slotSize, bgColor);
            drawBorder(guiGraphics, x, y, slotSize, slotSize, borderColor);

            List<FormattedCharSequence> nameLines = this.font.split(Component.literal(name).withStyle(ChatFormatting.WHITE), slotSize - 6);
            int textY = y + (slotSize / 2) - (nameLines.size() * 4);
            for (FormattedCharSequence line : nameLines) {
                guiGraphics.drawCenteredString(this.font, line, x + (slotSize / 2), textY, 0xFFFFFFFF);
                textY += 9;
            }
        }

        if (activeTooltipLines != null) renderCustomTooltip(guiGraphics, activeTooltipLines, mouseX, mouseY);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}