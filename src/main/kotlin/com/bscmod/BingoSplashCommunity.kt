package com.bscmod

import com.mojang.blaze3d.platform.InputConstants
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.commands.CommandBuildContext
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation

class BingoSplashCommunity : ClientModInitializer {
    override fun onInitializeClient() {
        BscConfig.load()
        BscBingoHud.register()
        BingoRolesRenderer.fetchLatestRoles()

        settingsKey = KeyBindingHelper.registerKeyBinding(
            KeyMapping(
                "key.bsc.settings",
                InputConstants.Type.KEYSYM,
                InputConstants.UNKNOWN.value,
                KeyMapping.Category.register(ResourceLocation.fromNamespaceAndPath("bsc", "main"))
            )
        )

        ClientCommandRegistrationCallback.EVENT.register(ClientCommandRegistrationCallback { dispatcher: CommandDispatcher<FabricClientCommandSource?>?, _: CommandBuildContext? ->
            dispatcher!!.register(
                ClientCommandManager.literal("bsc")
                    .executes { _: CommandContext<FabricClientCommandSource> ->
                        scrollQueueOpen = true
                        1
                    }
            )

            dispatcher.register(
                ClientCommandManager.literal("bingocard")
                    .then(
                        ClientCommandManager.argument("player", StringArgumentType.word())
                            .executes { context: CommandContext<FabricClientCommandSource> ->
                                val target = StringArgumentType.getString(context, "player")
                                networkHandler.sendMessage("REQUEST_CARD|$target")
                                context.source.sendFeedback(Component.literal("§e[BSC] Fetching bingo card for $target..."))
                                1
                            }
                    )
            )
        })

        ClientTickEvents.END_CLIENT_TICK.register(ClientTickEvents.EndTick { client: Minecraft? ->
            UpdateChecker.tick()

            if (client!!.player == null) return@EndTick
            if (scrollQueueOpen) {
                client.setScreen(BscScreen(null))
                scrollQueueOpen = false
            }
            if (settingsKey != null) {
                while (settingsKey!!.consumeClick()) {
                    client.setScreen(BscScreen(client.screen))
                }
            }
        })

        networkHandler = NetworkHandler()
        networkHandler.start()
    }

    companion object {
        lateinit var networkHandler: NetworkHandler
        var settingsKey: KeyMapping? = null
        var scrollQueueOpen: Boolean = false

        @JvmStatic
        fun resetKeybind() {
            if (settingsKey != null) {
                settingsKey!!.setKey(InputConstants.UNKNOWN)
                Minecraft.getInstance().options.save()
            }
        }

        @JvmStatic
        fun getSettingsKeyName(): String {
            if (settingsKey == null || settingsKey!!.isUnbound) return "NONE"
            return settingsKey!!.saveString().uppercase()
                .replace("KEY.KEYBOARD.", "")
                .replace("KEY.MOUSE.", "MOUSE ")
        }

        @JvmStatic
        fun updateKeybind(keyCode: Int?) {
            if (settingsKey != null) {
                if (keyCode == null) {
                    settingsKey!!.setKey(InputConstants.UNKNOWN)
                } else {
                    settingsKey!!.setKey(InputConstants.Type.KEYSYM.getOrCreate(keyCode))
                }
                KeyMapping.resetMapping()
                Minecraft.getInstance().options.save()
            }
        }
    }
}