package com.bscmod

import kotlinx.atomicfu.locks.synchronized
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents.Game
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
import net.minecraft.ChatFormatting
import net.minecraft.Util
import net.minecraft.client.DeltaTracker
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW
import java.net.URI

object HowToBingoDisplay {
    const val url = "https://raw.githubusercontent.com/IQONIQ11/bingo-goals/refs/heads/main/guide.txt"
    var activeGuide: String? = null
    var hoveringGuide: String? = null
    var isLoading: Boolean = false
    var guides = mutableListOf<BingoGuide>()

    data class BingoGuide(val name: String, val explanation: String)

    fun register() {
        fetchGuides()

        ClientReceiveMessageEvents.GAME.register(Game { message: Component, _: Boolean ->
            onChatMessage(message.string)
        })

        HudRenderCallback.EVENT.register(HudRenderCallback { context: GuiGraphics, _: DeltaTracker ->
            val client = Minecraft.getInstance()
            if (client.player == null || client.options.hideGui) return@HudRenderCallback
            if (client.screen is BscScreen || client.screen is BscHudEditScreen) return@HudRenderCallback

            if (BscConfig.displayBingoGuide) {
                renderCard(context, client.font)
            }
        })
    }

    fun handleGuideOverview(context: GuiGraphics, mouseX: Int, mouseY: Int) {
        context.pose().pushMatrix()

        val availableGuides = guides.filterNot { BscConfig.completedGoals.contains(it.name) }

        if(availableGuides.isEmpty()) return

        val font = Minecraft.getInstance().font

        val guideDisplayHeight = Minecraft.getInstance().window.guiScaledHeight / 2
        val guideElementHeight = font.lineHeight + 2
        val totalGuideDisplayHeight = guideElementHeight * (availableGuides.size + 1)

        context.pose().translateLocal(10.toFloat(), guideDisplayHeight.toFloat())

        val firstElementY = -(totalGuideDisplayHeight / 2)
        context.drawString(font, "Available Bingo Guides:", 0, firstElementY, 0xFFFFFFFF.toInt(), true)

        var hoveredGuide: String? = null

        for((index, guide) in availableGuides.withIndex()) {
            val text = "${
                if(activeGuide == guide.name) {
                    "★"
                } else {
                    "⭕"
                }
            } ${guide.name}"

            val elementY = (index + 1) * guideElementHeight - (totalGuideDisplayHeight / 2)

            context.drawString(font, text, 0, elementY, 0xFFFFFFFF.toInt(), true)

            val listStartOnScreen = guideDisplayHeight - (totalGuideDisplayHeight / 2)

            val absoluteTop = listStartOnScreen + ((index + 1) * guideElementHeight)
            val absoluteBottom = absoluteTop + font.lineHeight

            if (mouseX in 10..font.width(text) + 10 && mouseY in absoluteTop - 1..< absoluteBottom) {
                hoveredGuide = guide.name
            }
        }

        hoveringGuide = hoveredGuide

        context.pose().popMatrix()
    }

    fun handleGuideClick(mouseButtonEvent: MouseButtonEvent) {
        if(hoveringGuide == null) return

        if(mouseButtonEvent.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            activeGuide = hoveringGuide
        } else if(mouseButtonEvent.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            activeGuide = null
        }
    }

    fun renderCard(context: GuiGraphics, textRenderer: Font) {
        context.pose().pushMatrix()
        context.pose().translateLocal(BscConfig.bingoGuideX.toFloat(), BscConfig.bingoGuideY.toFloat())
        val scale = BscConfig.bingoHudScale
        context.pose().scale(scale, scale)

        synchronized(guides) {
            val guide = guides.firstOrNull { it.name == activeGuide }

            if(activeGuide != null && guide != null) {
                context.drawString(textRenderer, "Guide: ${guide.explanation}", 0, 0, BscConfig.bingoGuideColor, true)
            }
        }
        context.pose().popMatrix()
    }

    fun onChatMessage(text: String) {
        if(text.startsWith("BINGO GOAL COMPLETE! ")) {
            try {
                val cleanText = ChatFormatting.stripFormatting(text)!!.replace("BINGO GOAL COMPLETE!", "").trim()

                if(cleanText.equals(activeGuide, ignoreCase = true)) {
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
                        val (name, description) = line.trim()
                            .takeIf { it.isNotEmpty() }
                            ?.split("|", limit = 2)
                            ?.takeIf { it.size == 2 }
                            ?.map { it.trim() }
                            ?: return@forEach

                        downloadedGuides.add(BingoGuide(name, description))
                    }
                }

                synchronized(guides) {
                    guides.clear()
                    guides.addAll(downloadedGuides)
                }
            } catch (_: Exception) {
                synchronized(guides) {
                    guides.clear()
                    guides.add(BingoGuide("Error loading guides", "§cFetch Failed"))
                }
            } finally {
                isLoading = false
            }
        }
    }
}