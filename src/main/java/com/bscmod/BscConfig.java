package com.bscmod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BscConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("bsc");

    public static boolean receivePings = true;
    public static boolean showTitle = true;
    public static long alertDuration = 4;
    public static int titleColor = 0xFF00FFFF;
    public static boolean displayBingoCard = false;
    public static boolean playSound = true;
    public static boolean ironmanOnly = false;
    public static boolean bingoOnly = false;
    public static boolean showHubWarp = false;

    public static boolean bingoCardBingoProfileOnly = false;

    public static int bingoHudX = 10;
    public static int bingoHudY = 60;
    public static float bingoHudScale = 1.0f;
    public static int cardTitleColor = 0xFF00FFFF;
    public static int cardTextColor = 0xFFFFFFFF;

    public static List<String> completedGoals = new ArrayList<>();
    public static String lastBingoSession = "";

    public static boolean displayBingoTimer = false;
    public static int timerHudX = 10;
    public static int timerHudY = 150;
    public static float timerHudScale = 1.0f;
    public static int timerTitleColor = 0xFF00FFFF;
    public static int timerTextColor = 0xFFFFFFFF;

    public static boolean syncHeart = false, robotron = false, controlSwitch = false, motor = false, transmitter = false, ftx3070 = false;
    public static boolean armadillo = false, jungleKey = false, picko = false, powder = false, goblinEgg = false, flawlessGem = false;
    public static boolean powder2x = false, goblinRaid = false, raffle = false, betterTogether = false, goneWind = false, mithrilGourmand = false;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("bsc-config.json").toFile();

    public static void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            ConfigData data = new ConfigData();
            data.receivePings = receivePings;
            data.showTitle = showTitle;
            data.alertDuration = alertDuration;
            data.titleColor = titleColor;
            data.displayBingoCard = displayBingoCard;
            data.playSound = playSound;
            data.ironmanOnly = ironmanOnly;
            data.bingoOnly = bingoOnly;
            data.showHubWarp = showHubWarp;
            data.bingoCardBingoProfileOnly = bingoCardBingoProfileOnly;
            data.completedGoals = completedGoals;
            data.lastBingoSession = lastBingoSession;
            data.bingoHudX = bingoHudX;
            data.bingoHudY = bingoHudY;
            data.bingoHudScale = bingoHudScale;
            data.cardTitleColor = cardTitleColor;
            data.cardTextColor = cardTextColor;
            data.displayBingoTimer = displayBingoTimer;
            data.timerHudX = timerHudX;
            data.timerHudY = timerHudY;
            data.timerHudScale = timerHudScale;
            data.timerTitleColor = timerTitleColor;
            data.timerTextColor = timerTextColor;
            data.syncHeart = syncHeart;
            data.robotron = robotron;
            data.controlSwitch = controlSwitch;
            data.motor = motor;
            data.transmitter = transmitter;
            data.ftx3070 = ftx3070;
            data.armadillo = armadillo;
            data.jungleKey = jungleKey;
            data.picko = picko;
            data.powder = powder;
            data.goblinEgg = goblinEgg;
            data.flawlessGem = flawlessGem;
            data.powder2x = powder2x;
            data.goblinRaid = goblinRaid;
            data.raffle = raffle;
            data.betterTogether = betterTogether;
            data.goneWind = goneWind;
            data.mithrilGourmand = mithrilGourmand;

            GSON.toJson(data, writer);
        } catch (IOException e) {
            LOGGER.error("Failed to save Bingo Splash Mod configuration!", e);
        }
    }

    public static void load() {
        if (!CONFIG_FILE.exists()) {
            save();
            return;
        }
        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            ConfigData data = GSON.fromJson(reader, ConfigData.class);
            if (data != null) {
                receivePings = data.receivePings;
                showTitle = data.showTitle;
                alertDuration = data.alertDuration;
                titleColor = data.titleColor;
                displayBingoCard = data.displayBingoCard;
                playSound = data.playSound;
                ironmanOnly = data.ironmanOnly;
                bingoOnly = data.bingoOnly;
                showHubWarp = data.showHubWarp;
                bingoCardBingoProfileOnly = data.bingoCardBingoProfileOnly;
                completedGoals = data.completedGoals != null ? data.completedGoals : new ArrayList<>();
                lastBingoSession = data.lastBingoSession != null ? data.lastBingoSession : "";
                bingoHudX = data.bingoHudX;
                bingoHudY = data.bingoHudY;
                bingoHudScale = data.bingoHudScale;
                cardTitleColor = data.cardTitleColor;
                cardTextColor = data.cardTextColor;
                displayBingoTimer = data.displayBingoTimer;
                timerHudX = data.timerHudX;
                timerHudY = data.timerHudY;
                timerHudScale = data.timerHudScale;
                timerTitleColor = data.timerTitleColor;
                timerTextColor = data.timerTextColor;
                syncHeart = data.syncHeart;
                robotron = data.robotron;
                controlSwitch = data.controlSwitch;
                motor = data.motor;
                transmitter = data.transmitter;
                ftx3070 = data.ftx3070;
                armadillo = data.armadillo;
                jungleKey = data.jungleKey;
                picko = data.picko;
                powder = data.powder;
                goblinEgg = data.goblinEgg;
                flawlessGem = data.flawlessGem;
                powder2x = data.powder2x;
                goblinRaid = data.goblinRaid;
                raffle = data.raffle;
                betterTogether = data.betterTogether;
                goneWind = data.goneWind;
                mithrilGourmand = data.mithrilGourmand;
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load Bingo Splash Mod configuration!", e);
        }
    }

    private static class ConfigData {
        boolean receivePings = true, showTitle = true, displayBingoCard = false, playSound = true, ironmanOnly = false, bingoOnly = false, showHubWarp = false;
        boolean bingoCardBingoProfileOnly = false;
        long alertDuration = 4;
        int titleColor = 0xFF00FFFF;
        List<String> completedGoals = new ArrayList<>();
        String lastBingoSession = "";
        int bingoHudX = 10, bingoHudY = 60, cardTitleColor = 0xFF00FFFF, cardTextColor = 0xFFFFFFFF;
        float bingoHudScale = 1.0f;
        boolean displayBingoTimer = false;
        int timerHudX = 10, timerHudY = 150, timerTitleColor = 0xFF00FFFF, timerTextColor = 0xFFFFFFFF;
        float timerHudScale = 1.0f;
        boolean syncHeart = false, robotron = false, controlSwitch = false, motor = false, transmitter = false, ftx3070 = false;
        boolean armadillo = false, jungleKey = false, picko = false, powder = false, goblinEgg = false, flawlessGem = false;
        boolean powder2x = false, goblinRaid = false, raffle = false, betterTogether = false, goneWind = false, mithrilGourmand = false;
    }
}