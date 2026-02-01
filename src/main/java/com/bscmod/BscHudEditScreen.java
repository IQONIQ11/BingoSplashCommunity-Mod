package com.bscmod;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class BscHudEditScreen extends Screen {
    private final Screen parent;

    // Dragging & Resizing States
    private boolean draggingBingo = false, resizingBingo = false;
    private boolean draggingTimer = false, resizingTimer = false;
    private double dragOffsetX, dragOffsetY;

    public BscHudEditScreen(Screen parent) {
        super(Component.literal("HUD Editor"));
        this.parent = parent;
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        // --- Header Title ---
        String headerTitle = "§b§lHUD EDITOR MODE";
        context.drawString(this.font, headerTitle, this.width / 2, 10, 0xFFFFFFFF);
        context.fill((this.width + (font.width(headerTitle))) / 2 - 40, 20, (this.width + (font.width(headerTitle))) / 2 + 40, 21, 0xFF55FFFF);

        // --- Render HUD 1: Bingo Card ---
        if (BscConfig.displayBingoCard) {
            renderEditorElement(context, "Bingo Card", BscConfig.bingoHudX, BscConfig.bingoHudY, 130, 100,
                    BscConfig.bingoHudScale, draggingBingo, resizingBingo);
            BscBingoHud.renderCard(context, this.font);
        }

        // --- Render HUD 2: Bingo Timer ---
        if (BscConfig.displayBingoTimer) {
            renderEditorElement(context, "Bingo Timer", BscConfig.timerHudX, BscConfig.timerHudY, 100, 15,
                    BscConfig.timerHudScale, draggingTimer, resizingTimer);
            BscBingoHud.renderTimer(context, this.font);
        }

        // Footer Instructions
        context.drawCenteredString(this.font, "§7Click elements to move §8| §7Drag corners to scale", this.width / 2, this.height - 15, 0xAAAAAA);

        super.render(context, mouseX, mouseY, delta);
    }

    private void renderEditorElement(GuiGraphics context, String label, int x, int y, int baseW, int baseH, float scale, boolean isDragging, boolean isResizing) {
        int sw = (int) (baseW * scale);
        int sh = (int) (baseH * scale);

        // Guide Box & Border
        context.fill(x - 2, y - 2, x + sw + 2, y + sh + 2, (isDragging || isResizing) ? 0x4455FFFF : 0x22FFFFFF);
        context.renderOutline(x - 3, y - 3, sw + 6, sh + 6, 0xFF55FFFF);

        // Resize Handle
        int handleColor = isResizing ? 0xFFFFFF00 : 0xFFFFFFFF;
        context.fill(x + sw, y + sh, x + sw + 6, y + sh + 6, handleColor);

        // Status Label
        if (isDragging || isResizing) {
            context.drawString(this.font, "§e" + label + String.format(" (%.1fx)", scale), x, y - 12, 0xFFFFFFFF);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean bl) {
        // Check Timer First (since it's usually smaller)
        if (BscConfig.displayBingoTimer && checkInput(mouseButtonEvent.x(), mouseButtonEvent.y(), BscConfig.timerHudX, BscConfig.timerHudY, 100, 15, BscConfig.timerHudScale, "timer")) return true;

        // Check Bingo Card
        if (BscConfig.displayBingoCard && checkInput(mouseButtonEvent.x(), mouseButtonEvent.y(), BscConfig.bingoHudX, BscConfig.bingoHudY, 130, 100, BscConfig.bingoHudScale, "bingo")) return true;

        return super.mouseClicked(mouseButtonEvent, bl);
    }

    private boolean checkInput(double mx, double my, int x, int y, int baseW, int baseH, float scale, String type) {
        int sw = (int) (baseW * scale);
        int sh = (int) (baseH * scale);

        // Handle
        if (mx >= x + sw && mx <= x + sw + 8 && my >= y + sh && my <= y + sh + 8) {
            if (type.equals("bingo")) resizingBingo = true; else resizingTimer = true;
            return true;
        }
        // Body
        if (mx >= x && mx <= x + sw && my >= y && my <= y + sh) {
            if (type.equals("bingo")) draggingBingo = true; else draggingTimer = true;
            dragOffsetX = mx - x;
            dragOffsetY = my - y;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent mouseButtonEvent, double deltaX, double deltaY) {
        if (resizingBingo) {
            BscConfig.bingoHudScale = Math.max(0.5f, Math.min(3.0f, (float)((mouseButtonEvent.x() - BscConfig.bingoHudX) / 130.0)));
        } else if (resizingTimer) {
            BscConfig.timerHudScale = Math.max(0.5f, Math.min(3.0f, (float)((mouseButtonEvent.x() - BscConfig.timerHudX) / 100.0)));
        } else if (draggingBingo) {
            BscConfig.bingoHudX = (int) (mouseButtonEvent.x() - dragOffsetX);
            BscConfig.bingoHudY = (int) (mouseButtonEvent.y() - dragOffsetY);
        } else if (draggingTimer) {
            BscConfig.timerHudX = (int) (mouseButtonEvent.x() - dragOffsetX);
            BscConfig.timerHudY = (int) (mouseButtonEvent.y() - dragOffsetY);
        }
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent mouseButtonEvent) {
        draggingBingo = resizingBingo = draggingTimer = resizingTimer = false;
        BscConfig.save();
        return super.mouseReleased(mouseButtonEvent);
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        if (keyEvent.key() == GLFW.GLFW_KEY_ESCAPE) {
            if (this.minecraft != null) this.minecraft.setScreen(parent);
            return true;
        }
        return super.keyPressed(keyEvent);
    }

    @Override
    public void renderBackground(GuiGraphics context, int mouseX, int mouseY, float delta) {
    }
}