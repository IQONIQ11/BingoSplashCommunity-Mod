package com.bscmod.overlays;

import net.dungeonhub.promptoverlay.api.render.AcceptableOverlay;
import net.dungeonhub.promptoverlay.api.render.OneActionOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.jspecify.annotations.NonNull;

import java.awt.*;
import java.util.Objects;

public class SplashPartyOverlay implements AcceptableOverlay, OneActionOverlay {
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
        return "[" + acceptKey() + "] " + (Objects.equals(command, "warp hub") ? "Warp to hub" : "Accept");
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