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

package xyz.qalcyo.rysm.eight.mixin.gui;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.GuiIngameForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.qalcyo.rysm.core.RysmCore;
import xyz.qalcyo.rysm.core.listener.events.TitleEvent;

/**
 * This mixin sends and handles the TitleEvent which is used
 * in the core submodule to modify rendered titles and
 * subtitles ingame.
 */
@SuppressWarnings("DefaultAnnotationParam")
@Mixin(value = GuiIngameForge.class, remap = false)
public class GuiIngameForgeMixin {
    private TitleEvent titleEvent;

    /**
     * Invokes the TitleEvent and cancels the title from rendering if asked.
     */
    @Inject(method = "renderTitle", at = @At("HEAD"), cancellable = true)
    private void invokeAndCancelEvent(int width, int height, float partialTicks, CallbackInfo ci) {
        titleEvent = new TitleEvent(false, 1.0F, 1.0F, true);
        RysmCore.INSTANCE.getEventBus().post(titleEvent);
        if (titleEvent.getCancelled()) ci.cancel();
    }

    /**
     * Modifies the title scale based on the invoked event.
     */
    @Redirect(method = "renderTitle", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;scale(FFF)V"), remap = true)
    private void modifyTitleScale(float x, float y, float z) {
        if (x == 4.0F) {
            //Title
            GlStateManager.scale(x * titleEvent.getTitleScale(), y * titleEvent.getTitleScale(), z * titleEvent.getTitleScale());
            return;
        }
        if (x == 2.0F) {
            //Subtitle
            GlStateManager.scale(x * titleEvent.getSubtitleScale(), y * titleEvent.getSubtitleScale(), z * titleEvent.getSubtitleScale());
        }
    }

    /**
     * Sets the shadow of the title and subtitle based on the invoked event.
     */
    @ModifyArg(method = "renderTitle", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;drawString(Ljava/lang/String;FFIZ)I"), index = 4, remap = true)
    private boolean setShadow(boolean shadow) {
        return titleEvent.getShadow();
    }
}