package com.bscmod;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class BingoRolesRenderer {

    // Replace with your RAW GitHub link (click 'Raw' on GitHub to get it)
    private static final String GITHUB_URL = "https://raw.githubusercontent.com/IQONIQ11/bingo-goals/refs/heads/main/roles.json";

    public static class RoleData {
        public String name, desc, category;
        public int color;

        public RoleData(String category, String name, String desc, int color) {
            this.category = category;
            this.name = name;
            this.desc = desc;
            this.color = color;
        }
    }

    // This List starts with your current hardcoded roles as defaults
    public static volatile List<RoleData> ROLES = new ArrayList<>(Arrays.asList(
            new RoleData("Bingo Ranks", "Bingo Rank I-IV", "Bingo Rank (Color based).", 0xFF55FF55),
            new RoleData("RNG Based", "RNG Carried", "Got an RNG Drop. Exp Share Core doesn’t count.", 0xFFFF55FF),
            new RoleData("RNG Based", "Lucky Gamer", "Got 3 RNG Drops (All Bingos).", 0xFFFF55FF),
            new RoleData("Community Based", "Top 1", "Player who has gotten #1 in a community goal.", 0xFFFFAA00),
            new RoleData("Community Based", "Community Grinder", "Top #100 in every community goal.", 0xFFFFAA00),
            new RoleData("Sweat or Speedrunner?", "Sweat", "10 hours or less in playtime. (Bingo #7 doesn't count)", 0xFF55FFFF),
            new RoleData("Sweat or Speedrunner?", "Speedrunner", "Finishes bingo in 2 days and 12 hours or under.", 0xFF55FFFF),
            new RoleData("Achieving Based", "OverGrinder", "Overgrinds on something; common is high skill level.", 0xFFFFFF55),
            new RoleData("Achieving Based", "Overachiever+", "Grinded something outside of the Bingo Card (Crazy).", 0xFFFFFF55),
            new RoleData("Achieving Based", "Overachiever", "Grinded something outside of the Bingo Card.", 0xFFFFFF55),
            new RoleData("Achieving Based", "Just Achieving+", "Grinded something outside of the Bingo Card (Harder).", 0xFFFFFF55),
            new RoleData("Achieving Based", "Just Achieving", "Grinded something outside of the Bingo Card.", 0xFFFFFF55),
            new RoleData("Grinder or No", "NoGrind", "Wants to avoid skill grinding; skill average below 10.", 0xFFAAAAAA),
            new RoleData("Grinder or No", "YesGrind", "Wants to grind skill average of 20 on bingo.", 0xFF55FF55),
            new RoleData("Bingos doing Combat?", "Bloom's Puppet", "Player who has done F7 completion or above.", 0xFFFF5555),
            new RoleData("Bingos doing Combat?", "Slayer Grinder", "Grinds all tiers (Rev 6, Tara 7, Sven 6, Emen 5, Blaze 3).", 0xFFFF5555),
            new RoleData("Bingos doing Combat?", "Slayer", "Grinded Slayer levels (Can pick one and grind it).", 0xFFFF5555),
            new RoleData("The Random", "The Bank", "Grinded 10M Coins or Above.", 0xFFFFFF55),
            new RoleData("The Random", "Deathless", "Completed bingo without dying.", 0xFFFFFFFF),
            new RoleData("The Random", "Builder", "Doesn't care about Bingo but builds on Bingo.", 0xFFFFAA00),
            new RoleData("Collection Based", "Collector", "Unlocks all collections (No Chili/Rift).", 0xFF55FFFF),
            new RoleData("Collection Based", "Max Collector", "Maxes 10 Collections.", 0xFF55FFFF),
            new RoleData("Collection Based", "Committed Collector", "Acquires 66 unlocked collections total.", 0xFF55FFFF),
            new RoleData("Rift Roles", "Rift Citizen", "Completes 4 timecharms in the rift.", 0xFFAA00AA),
            new RoleData("Rift Roles", "Rift Completionist", "Completes all 8 timecharms in the rift.", 0xFFAA00AA),
            new RoleData("Completed Bingo!", "Cracked", "Completed at least 10 Bingos", 0xFF00AAAA),
            new RoleData("Completed Bingo!", "Nooby Streak", "6 Bingos in a Row", 0xFF00AAAA),
            new RoleData("Completed Bingo!", "Sweaty Streak", "8 Bingos in a Row", 0xFF00AAAA),
            new RoleData("Completed Bingo!", "Cracked Streak", "12 Bingos in a Row", 0xFF00AAAA),
            new RoleData("Completed Bingo!", "Extreme Streak", "16 Bingos in a Row", 0xFF00AAAA),
            new RoleData("Completed Bingo!", "Omega Streak", "20 Bingos in a Row", 0xFF00AAAA),
            new RoleData("Completed Bingo!", "Godly Streak", "24 Bingos in a Row", 0xFF00AAAA),
            new RoleData("Completed Bingo!", "Flawless Streak", "28 Bingos in a Row", 0xFF00AAAA),
            new RoleData("Completed Bingo!", "Perfect Streak", "32 Bingos in a Row", 0xFF00AAAA),
            new RoleData("Completed Bingo!", "Mythic Streak", "36 Bingos in a Row.", 0xFF00AAAA),
            new RoleData("Completed Bingo!", "Divine Streak", "42 Bingos in a Row.", 0xFF00AAAA),
            new RoleData("Completed Bingo!", "Celestial Streak", "46 Bingos in a Row.", 0xFF00AAAA),
            new RoleData("Completed Bingo!", "Dedicated Streak", "50 Bingos in a Row.", 0xFF00AAAA),
            new RoleData("Extreme Bingo Roles", "Sweat+", "2 days (48 hours) or less in playtime.", 0xFFFF5555),
            new RoleData("Extreme Bingo Roles", "Speedrunner+", "Finishes bingo in 6 days or under.", 0xFFFF5555),
            new RoleData("Extreme Bingo Roles", "Max Collector+", "Maxes 15 Collections.", 0xFFFF5555),
            new RoleData("Extreme Bingo Roles", "The Bank+", "Grinded 25M Coins or Above.", 0xFFFFFF55),
            new RoleData("Secret Bingo Roles", "Secret Credit", "Credited for a goal in bingo-guides.", 0xFFFFFFFF),
            new RoleData("Secret Bingo Roles", "#5+ to #100+", "Top placements for goals in Secret Bingo.", 0xFFFFFFFF),
            new RoleData("Secret Bingo Roles", "Snowy Farmer", "Apply hot potato book to Snow Suit armor.", 0xFFFFFFFF),
            new RoleData("Secret Bingo Roles", "The Secret Bank", "Get 35m on Bingo.", 0xFFFFFF55),
            new RoleData("Secret Bingo Roles", "The Secret Grind", "Get 300k Cobblestone Collection.", 0xFF55FF55)
    ));

    public static void fetchLatestRoles() {
        CompletableFuture.runAsync(() -> {
            try {
                URL url = new URL(GITHUB_URL);
                InputStreamReader reader = new InputStreamReader(url.openStream());
                List<RoleData> remoteRoles = new Gson().fromJson(reader, new TypeToken<List<RoleData>>(){}.getType());

                if (remoteRoles != null && !remoteRoles.isEmpty()) {
                    ROLES = remoteRoles;
                    System.out.println("[BSC] Successfully synced community roles from GitHub.");
                }
                reader.close();
            } catch (Exception e) {
                System.err.println("[BSC] Could not fetch roles: " + e.getMessage());
            }
        });
    }
}