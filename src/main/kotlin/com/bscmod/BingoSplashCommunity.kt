package com.bscmod

import com.mojang.blaze3d.platform.InputConstants
import com.mojang.brigadier.Command
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.context.CommandContext
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.client.input.KeyEvent
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.CommandSelection
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation


class BingoSplashCommunity : ClientModInitializer {
	override fun onInitializeClient() {
		BscConfig.load()
		BscBingoHud.register()
		BingoRolesRenderer.fetchLatestRoles()
        Registry.register(BuiltInRegistries.SOUND_EVENT, NetworkHandler.SPLASH_SOUND_ID, NetworkHandler.SPLASH_SOUND_EVENT)

		// 1. Keybind Registration
		settingsKey = KeyBindingHelper.registerKeyBinding(
			KeyMapping(
				"key.bsc.settings",
				InputConstants.Type.KEYSYM,
				InputConstants.UNKNOWN.value,
				KeyMapping.Category.register(ResourceLocation.fromNamespaceAndPath("bsc", "main"))
			)
		)

		// 3. Command Registration (Only main config command remains)
		ClientCommandRegistrationCallback.EVENT.register(ClientCommandRegistrationCallback { dispatcher: CommandDispatcher<FabricClientCommandSource?>?, registryAccess: CommandBuildContext? ->
			dispatcher!!.register(
				ClientCommandManager.literal("bsc")
					.executes(Command { context: CommandContext<FabricClientCommandSource?>? ->
						scrollQueueOpen = true
						1
					})
			)

            // NUOVO: Comando di test /bsctest
            dispatcher.register(
                ClientCommandManager.literal("bsctest")
                    .executes { context ->
                        // Simuliamo il formato del server: "Nome|DiscordID|Tipo|Messaggio"
                        val testMessage = "TestUser|123456789|SPLASH|Hub 42 - Alchemy Splash at spawn!"

                        // Chiamiamo il metodo del NetworkHandler
                        NetworkHandler.handleMessage(testMessage)

                        // Invia un feedback nel log/chat di chi esegue il comando
                        context.source.sendFeedback(net.minecraft.network.chat.Component.literal("§a[BSC] Test splash inviato!"))
                        1
                    }
            )
		})

		// 4. Tick Handling
		ClientTickEvents.END_CLIENT_TICK.register(ClientTickEvents.EndTick { client: Minecraft? ->
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
		var settingsKey: KeyMapping? = null
		lateinit var networkHandler: NetworkHandler
		var scrollQueueOpen: Boolean = false

		@JvmStatic
		fun resetKeybind() {
			if (settingsKey != null) {
				settingsKey!!.setKey(InputConstants.UNKNOWN)
				Minecraft.getInstance().options.save()
			}
		}

		@JvmStatic
		fun getSettingsKeyName(): String? {
			if (settingsKey == null || settingsKey!!.isUnbound) return "NONE"
			return settingsKey!!.saveString().uppercase()
				.replace("KEY.KEYBOARD.", "")
				.replace("KEY.MOUSE.", "MOUSE ")
		}

		@JvmStatic
		fun updateKeybind(keyCode: Int?) {
			if (settingsKey != null) {
				if(keyCode == null) {
					settingsKey!!.setKey(InputConstants.UNKNOWN)
				} else {
					settingsKey!!.setKey(InputConstants.getKey(KeyEvent(keyCode, 0, 0)))
				}
				KeyMapping.resetMapping()
				Minecraft.getInstance().options.save()
			}
		}
	}
}