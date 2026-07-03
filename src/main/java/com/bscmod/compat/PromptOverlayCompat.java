package com.bscmod.compat;

import net.dungeonhub.promptoverlay.feature.OverlayFeature;
import net.dungeonhub.promptoverlay.render.AcceptableOverlay;
import net.dungeonhub.promptoverlay.render.OneActionOverlay;
import net.dungeonhub.promptoverlay.service.KeyMappingService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.jspecify.annotations.NonNull;

import java.awt.*;

public class PromptOverlayCompat {
    public static void sendSplashPartyOverlay(String sender, String command) {
        OverlayFeature.INSTANCE.setOverlay(new SplashPartyOverlay(sender, command));
    }

    public static class SplashPartyOverlay implements AcceptableOverlay, OneActionOverlay {
        private final String sender;
        private final String command;

        public SplashPartyOverlay(String sender, String command) {
            this.sender = sender;
            this.command = command;
        }

        @Override
        public void accept() {
            Minecraft.getInstance().execute(() -> {
                LocalPlayer player = Minecraft.getInstance().player;
                if(player == null) return;
                player.connection.sendCommand(command);
            });
        }

        @Override
        public @NonNull String getFirstText() {
            String acceptKeyName = KeyMappingService.INSTANCE.getAcceptKey().getTranslatedKeyMessage().getString();

            return "[" + acceptKeyName + "] Accept";
        }

        @Override
        public @NonNull Color getBorderColor() {
            return new Color(0xFF55FF);
        }

        @Override
        public @NonNull String getMessage() {
            return sender + " invited you to a splash.";
        }
    }
}