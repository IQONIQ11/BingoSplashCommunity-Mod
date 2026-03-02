package com.bscmod;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.util.Collection;

public class HypixelUtils {

    public static String getProfileType() {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) return "Unknown";

        Scoreboard scoreboard = client.level.getScoreboard();
        Objective objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);

        if (objective == null) return "Unknown";

        Collection<PlayerTeam> teams = scoreboard.getPlayerTeams();
        for (PlayerTeam team : teams) {
            if(team.getPlayers().size() != 1) continue;

            String playerName = client.player.getGameProfile().name();
            if(!team.getPlayers().iterator().next().equals(playerName)) continue;

            String suffix = ChatFormatting.stripFormatting(team.getPlayerSuffix().getString());

            if (suffix.contains("♲")) return "Ironman";
            if (suffix.contains("Ⓑ")) return "Bingo";
        }

        return "Normal";
    }
}