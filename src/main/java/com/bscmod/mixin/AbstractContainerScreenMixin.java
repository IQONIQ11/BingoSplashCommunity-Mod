package com.bscmod.mixin;

import com.bscmod.HowToBingoDisplay;
import com.bscmod.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {
    @Inject(method = "renderSlot", at = @At("TAIL"))
    private void renderSlot(GuiGraphics guiGraphics, Slot slot, CallbackInfo ci) {
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

    @Inject(method = "renderContents", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", shift = At.Shift.AFTER))
    private void onInventoryRender(GuiGraphics context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        if (Minecraft.getInstance().level != null && Minecraft.getInstance().player != null) {
            Screen currentScreen = Minecraft.getInstance().screen;
            if(currentScreen == null) return;

            if (!(currentScreen instanceof InventoryScreen)) return;

            HowToBingoDisplay.INSTANCE.handleGuideOverview(context, mouseX, mouseY);
        }
    }

    @Inject(method = "mouseClicked", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;mouseClicked(Lnet/minecraft/client/input/MouseButtonEvent;Z)Z", shift = At.Shift.AFTER))
    private void onInventoryMouseClick(MouseButtonEvent mouseButtonEvent, boolean bl, CallbackInfoReturnable<Boolean> ci) {
        if (Minecraft.getInstance().level != null && Minecraft.getInstance().player != null) {
            Screen currentScreen = Minecraft.getInstance().screen;
            if(currentScreen == null) return;

            if (!(currentScreen instanceof InventoryScreen)) return;

            HowToBingoDisplay.INSTANCE.handleGuideClick(mouseButtonEvent);
        }
    }
}
