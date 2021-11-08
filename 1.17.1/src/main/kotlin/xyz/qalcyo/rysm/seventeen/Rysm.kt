/*
 * Rysm, a utility mod for 1.8.9.
 * Copyright (C) 2021 Rysm
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package xyz.qalcyo.rysm.seventeen

import gg.essential.api.EssentialAPI
import gg.essential.lib.kbrewster.eventbus.Subscribe
import gg.essential.universal.ChatColor
import gg.essential.universal.UDesktop
import gg.essential.universal.UResolution
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.MinecraftClient
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import org.lwjgl.glfw.GLFW
import xyz.qalcyo.rysm.core.RysmCore
import xyz.qalcyo.rysm.core.RysmInfo
import xyz.qalcyo.rysm.core.config.RysmConfig
import xyz.qalcyo.rysm.core.listener.Listener
import xyz.qalcyo.rysm.core.listener.events.BossBarResetEvent
import xyz.qalcyo.rysm.core.listener.events.ChatRefreshEvent
import xyz.qalcyo.rysm.core.listener.events.Gui
import xyz.qalcyo.rysm.core.listener.events.RenderGuiEvent
import xyz.qalcyo.rysm.core.utils.MinecraftVersions
import xyz.qalcyo.rysm.core.utils.Updater
import xyz.qalcyo.rysm.seventeen.gui.ActionBarGui
import xyz.qalcyo.rysm.seventeen.gui.BossHealthGui
import xyz.qalcyo.rysm.seventeen.gui.DownloadGui
import xyz.qalcyo.rysm.seventeen.gui.SidebarGui
import xyz.qalcyo.rysm.seventeen.mixin.gui.ChatHudAccessor
import java.io.File
import java.net.URI

object Rysm : ClientModInitializer {

    var needsToCancel = false
    private val keyBinding: KeyBinding = KeyBindingHelper.registerKeyBinding(
        KeyBinding(
            "key.rysm.keybind", // The translation key of the keybinding's name
            InputUtil.Type.KEYSYM, // The type of the keybinding, KEYSYM for keyboard, MOUSE for mouse.
            GLFW.GLFW_KEY_Z, // The keycode of the key
            "category.rysm.ok" // The translation key of the keybinding's category.
        )
    )
    var packY: Int? = null
    var packBottom: Int? = null

    /**
     * Handles the initialization of the mod.
     */
    override fun onInitializeClient() {
        RysmCore.modDir =
            File(File(File(FabricLoader.getInstance().configDir.toFile(), "Qalcyo"), "Rysm"), "1.17.1")
        if (!RysmCore.modDir.exists()) {
            RysmCore.modDir.mkdirs()
        }
        RysmCore.jarFile = File(javaClass.protectionDomain.codeSource.location.toURI())
        RysmCore.onInitialization(MinecraftVersions.SEVENTEEN)
        ClientTickEvents.END_CLIENT_TICK.register {
            while (keyBinding.wasPressed() && it.currentScreen == null) {
                EssentialAPI.getGuiUtil().openScreen(RysmConfig.gui())
                return@register
            }
        }
        Listener.color = when (RysmConfig.textColor) {
            0 -> ChatColor.BLACK.toString()
            1 -> ChatColor.DARK_BLUE.toString()
            2 -> ChatColor.DARK_GREEN.toString()
            3 -> ChatColor.DARK_AQUA.toString()
            4 -> ChatColor.DARK_RED.toString()
            5 -> ChatColor.DARK_PURPLE.toString()
            6 -> ChatColor.GOLD.toString()
            7 -> ChatColor.GRAY.toString()
            8 -> ChatColor.DARK_GRAY.toString()
            9 -> ChatColor.BLUE.toString()
            10 -> ChatColor.GREEN.toString()
            11 -> ChatColor.AQUA.toString()
            12 -> ChatColor.RED.toString()
            13 -> ChatColor.LIGHT_PURPLE.toString()
            14 -> ChatColor.YELLOW.toString()
            else -> ChatColor.WHITE.toString()
        }
        WorldRenderEvents.END.register {
            if (Updater.shouldShowNotification) {
                EssentialAPI.getNotifications()
                    .push(
                        "Mod Update",
                        "${RysmInfo.NAME} ${Updater.latestTag} is available!\nClick here to download it!",
                        5f
                    ) {
                        EssentialAPI.getGuiUtil().openScreen(DownloadGui())
                    }
                Updater.shouldShowNotification = false
            }
            if (RysmConfig.firstTime) {
                EssentialAPI.getNotifications().push(
                    "Rysm",
                    "Hello! As this is your first time using this mod, click the key Z on your keyboard to configure the many features in Wyvtils!"
                )
                RysmConfig.firstTime = false
                RysmConfig.markDirty()
                RysmConfig.writeData()
            }

        }
        RysmCore.eventBus.register(this)
    }

    @Subscribe
    fun onChatRefresh(e: ChatRefreshEvent) {
        val chat = MinecraftClient.getInstance().inGameHud.chatHud as ChatHudAccessor
        try {
            MinecraftClient.getInstance().inGameHud.chatHud.reset()
        } catch (e: Exception) {
            e.printStackTrace()
            EssentialAPI.getNotifications().push(
                RysmInfo.NAME,
                "There was a critical error while trying to refresh the chat. Please go to inv.wtf/qalcyo or click on this notification to fix this issue."
            ) {
                UDesktop.browse(URI.create("https://inv.wtf/qalcyo"))
            }
            chat.visibleMessages.clear()
            MinecraftClient.getInstance().inGameHud.chatHud.resetScroll()
            for (i in chat.messages.asReversed()) {
                chat.invokeAddMessage(i.text, i.id, i.creationTick, true)
            }
        }
    }

    @Subscribe
    fun onRenderGui(e: RenderGuiEvent) {
        when (e.gui) {
            Gui.BOSSBAR -> EssentialAPI.getGuiUtil().openScreen(BossHealthGui())
            Gui.ACTIONBAR -> EssentialAPI.getGuiUtil().openScreen(ActionBarGui())
            Gui.SIDEBAR -> EssentialAPI.getGuiUtil().openScreen(SidebarGui())
        }
    }

    @Subscribe
    fun onBossBarReset(e: BossBarResetEvent) {
        EssentialAPI.getGuiUtil().openScreen(null)
        RysmConfig.bossBarX = (UResolution.scaledWidth / 2)
        RysmConfig.bossBarY = 12
        RysmConfig.markDirty()
        RysmConfig.writeData()
        EssentialAPI.getGuiUtil().openScreen(RysmConfig.gui())
    }

}