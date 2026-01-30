package com.bscmod;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.inventory.Slot;

public class InventoryUtils {
    public static void renderHighlight(GuiGraphics context, Slot slot) {
        // 1. Check if we have a stored lobby
        if (NetworkHandler.activeLobby.isEmpty()) return;

        // 2. Check if 1 minute has passed (60,000 ms)
        if (System.currentTimeMillis() - NetworkHandler.lastPingTime > 60000) {
            NetworkHandler.activeLobby = "";
            return;
        }

        // 3. Check if the slot contains the right Hub number
        // Hub Selector items usually have the hub number in the name: "Hub #10"
        String itemName = slot.getItem().getHoverName().getString();
        if (itemName.contains("#" + NetworkHandler.activeLobby)) {
            // Draw a semi-transparent Cyan box over the slot
            context.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, 0x8800FFFF);
        }
    }
}