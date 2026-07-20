package com.bscmod

import kotlinx.atomicfu.locks.synchronized
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents.Game
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.util.Util
import org.lwjgl.glfw.GLFW
import java.net.URI

object HowToBingoDisplay {
    const val url = "https://raw.githubusercontent.com/IQONIQ11/bingo-goals/refs/heads/main/guide.txt"
    var activeGuide: String? = null
    var hoveringGuide: String? = null
    var isLoading: Boolean = false
    var guides = mutableListOf<BingoGuide>()

    data class BingoGuide(val name: String, val description: String, val explanation: String)

    fun register() {
        fetchGuides()

        ClientReceiveMessageEvents.GAME.register(Game { message: Component, _: Boolean ->
            onChatMessage(message.string)
        })

        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("bingosplashcommunity", "bingo_guide")) { context, _ ->
            val client = Minecraft.getInstance()
            if (client.player == null || client.options.hideGui) return@addLast
            if (client.screen is BscScreen || client.screen is BscHudEditScreen) return@addLast

            val currentProfile = HypixelUtils.getProfileType()
            val isBingoProfile = currentProfile.equals("Bingo", ignoreCase = true)

            if (isBingoProfile && BscConfig.displayBingoGuide) { // TODO find a way to debug the guide feature
                renderGuide(context, client.font)
            }
        }
    }

    fun handleGuideOverview(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        val currentProfile = HypixelUtils.getProfileType()
        val isBingoProfile = currentProfile.equals("Bingo", ignoreCase = true)

        if (!isBingoProfile || !BscConfig.displayBingoGuide) return // TODO find a way to debug the guide feature

        context.pose().pushMatrix()

        val availableGuides = guides.filterNot { BscConfig.completedGoals.contains(it.name) }

        if (availableGuides.isEmpty()) return

        val font = Minecraft.getInstance().font

        val guideDisplayHeight = Minecraft.getInstance().window.guiScaledHeight / 2
        val guideElementHeight = font.lineHeight + 2
        val totalGuideDisplayHeight = guideElementHeight * (availableGuides.size + 1)

        context.pose().translateLocal(10.toFloat(), guideDisplayHeight.toFloat())

        val firstElementY = -(totalGuideDisplayHeight / 2)
        context.text(font, "Available Bingo Guides:", 0, firstElementY, 0xFFFFFFFF.toInt(), true)

        var hoveredGuide: String? = null

        for ((index, guide) in availableGuides.withIndex()) {
            val text = "${
                if (activeGuide == guide.name) {
                    "★"
                } else {
                    "⭕"
                }
            } ${guide.name}: ${guide.description}"

            val elementY = (index + 1) * guideElementHeight - (totalGuideDisplayHeight / 2)

            context.text(font, text, 0, elementY, 0xFFFFFFFF.toInt(), true)

            val listStartOnScreen = guideDisplayHeight - (totalGuideDisplayHeight / 2)

            val absoluteTop = listStartOnScreen + ((index + 1) * guideElementHeight)
            val absoluteBottom = absoluteTop + font.lineHeight

            if (mouseX in 10..font.width(text) + 10 && mouseY in absoluteTop - 1..<absoluteBottom) {
                hoveredGuide = guide.name
            }
        }

        hoveringGuide = hoveredGuide

        context.pose().popMatrix()
    }

    fun handleGuideClick(mouseButtonEvent: MouseButtonEvent) {
        if (hoveringGuide == null) return

        if (mouseButtonEvent.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            activeGuide = hoveringGuide
        } else if (mouseButtonEvent.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            activeGuide = null
        }
    }

    fun renderGuide(context: GuiGraphicsExtractor, textRenderer: Font) {
        val x = BscConfig.bingoGuideX
        val y = BscConfig.bingoGuideY
        val width = BscConfig.bingoGuideWidth
        val height = 250

        context.pose().pushMatrix()
        // Don't translate - we'll use relative coordinates instead

        // Draw the semi-transparent background box with border (like other HUD elements)
        val bgColor = 0x77121212  // Semi-transparent dark gray background
        val borderColor = 0x77242424  // Use configurable border color

        // Render the guide text with word wrapping
        synchronized(guides) {
            val guide = guides.firstOrNull { it.name == activeGuide }

            if(activeGuide == null || guide == null) return@synchronized

            val guideText = "Guide: ${guide.explanation}"

            val wrappedText = wrapText(
                textRenderer,
                guideText,
                width - 4
            )

            val totalHeight = wrappedText.size * (textRenderer.lineHeight + 2) + 2

            context.fill(x - 2, y - 2, x + width, y + totalHeight, bgColor)
            context.outline(
                x - 2,
                y - 2,
                width + 2,
                totalHeight + 2,
                borderColor
            )

            val lineHeight = textRenderer.lineHeight + 2
            val maxHeight = height - 4

            for ((index, line) in wrappedText.withIndex()) {
                val yOffset = (index * lineHeight).coerceAtMost(maxHeight - lineHeight)
                context.text(textRenderer, line.trim(), x + 2, y + 2 + yOffset, BscConfig.bingoGuideColor, true)
            }
        }

        context.pose().popMatrix()
    }

    fun isActive(): Boolean {
        return activeGuide != null && guides.any { it.name == activeGuide }
    }

    fun calculateTextHeight(textRenderer: Font): Int {
        val width = BscConfig.bingoGuideWidth

        synchronized(guides) {
            val guide = guides.firstOrNull { it.name == activeGuide }

            if(activeGuide == null || guide == null) return 0

            val guideText = "Guide: ${guide.explanation}"

            val wrappedText = wrapText(
                textRenderer,
                guideText,
                width - 4
            )

            return wrappedText.size * (textRenderer.lineHeight + 2) + 2
        }
    }

    private fun wrapText(
        font: Font,
        fullText: String,
        maxWidth: Int,  // Maximum pixel width for each line
    ): List<String> {
        if (fullText.isEmpty()) return listOf()

        // Split the full text into wrapped lines at word boundaries
        val wrappedLines = mutableListOf<String>()
        val remainingWords = fullText.trimEnd().split(" ").filter { it.isNotEmpty() }

        if (remainingWords.isEmpty()) return listOf()

        val currentLine = StringBuilder()
        for (word in remainingWords) {
            // Check if adding this word would exceed maxWidth
            val nextWidth = font.width("$currentLine $word")

            if (nextWidth <= maxWidth) {
                // Word fits on current line - add it with a space separator
                currentLine.append(" ")
                currentLine.append(word)
            } else {
                // Word doesn't fit - save the current line and start new one
                wrappedLines.add(currentLine.toString())
                currentLine.clear()
                currentLine.append(word)
            }
        }

        // Don't forget the last line (if there's any content left)
        if (currentLine.isNotEmpty()) {
            wrappedLines.add(currentLine.trim().toString())
        }

        return wrappedLines
    }

    fun onChatMessage(text: String) {
        if (text.startsWith("BINGO GOAL COMPLETE! ")) {
            try {
                val cleanText = ChatFormatting.stripFormatting(text)!!.replace("BINGO GOAL COMPLETE!", "").trim()

                if (cleanText.equals(activeGuide, ignoreCase = true)) {
                    activeGuide = null
                }
            } catch (_: Exception) { }
        }
    }

    fun fetchGuides() {
        if (isLoading) return
        isLoading = true

        Util.backgroundExecutor().execute {
            try {
                val url = URI.create("$url?t=${System.currentTimeMillis()}").toURL()

                val downloadedGuides = mutableListOf<BingoGuide>()

                url.openStream().bufferedReader().useLines { lines ->
                    lines.forEach { line ->
                        val (name, description, explanation) = line.trim()
                            .takeIf { it.isNotEmpty() }
                            ?.split("|", limit = 3)
                            ?.takeIf { it.size == 3 }
                            ?.map { it.trim() }
                            ?: return@forEach

                        downloadedGuides.add(BingoGuide(name, description, explanation))
                    }
                }

                synchronized(guides) {
                    guides.clear()
                    guides.addAll(downloadedGuides)
                }
            } catch (_: Exception) {
                synchronized(guides) {
                    guides.clear()
                    guides.add(BingoGuide("Error loading guides", "§cFetch Failed", "§cFetch Failed"))
                }
            } finally {
                isLoading = false
            }
        }
    }
}