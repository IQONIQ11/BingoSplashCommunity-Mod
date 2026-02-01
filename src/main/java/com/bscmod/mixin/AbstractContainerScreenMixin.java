package com.bscmod.mixin;

import com.bscmod.NetworkHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {
    @Inject(method = "renderSlot", at = @At("TAIL"))
    private void renderSlot(GuiGraphics guiGraphics, Slot slot, int i, int j, CallbackInfo ci) {
        // 1. Valid Check
        if (NetworkHandler.activeLobby == null || NetworkHandler.activeLobby.isEmpty()) return;

        // 2. 60-second Expiry
        if (System.currentTimeMillis() - NetworkHandler.lastPingTime > 60000) {
            NetworkHandler.activeLobby = "";
            return;
        }

        // 3. Identification Logic
        if (slot.hasItem()) {
            String itemName = slot.getItem().getHoverName().getString();

            // Specifically looking for "Hub #X"
            if (itemName.contains("#" + NetworkHandler.activeLobby)) {
                int x = slot.x;
                int y = slot.y;

                guiGraphics.fill(x, y, x + 16, y + 16, 0x8000FFFF);
            }
        }
    }
}
