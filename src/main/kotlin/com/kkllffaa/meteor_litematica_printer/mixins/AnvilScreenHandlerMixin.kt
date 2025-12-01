package com.kkllffaa.meteor_litematica_printer.mixins

import net.minecraft.screen.AnvilScreenHandler
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Shadow
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
import net.minecraft.component.DataComponentTypes
import net.minecraft.text.Text
import net.minecraft.util.StringHelper
import net.minecraft.screen.slot.Slot
import net.minecraft.client.gui.screen.ingame.AnvilScreen

@Mixin(AnvilScreenHandler::class)
class AnvilScreenHandlerMixin {

    private companion object {
        @Inject(method = ["sanitize"], at = [At("HEAD")], cancellable = true)
        @JvmStatic
        private fun sanitize(name: String, ci: CallbackInfoReturnable<String?>) {
            ci.returnValue = name
        }
    }

}
