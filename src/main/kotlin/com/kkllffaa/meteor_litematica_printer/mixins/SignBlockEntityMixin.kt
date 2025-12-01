package com.kkllffaa.meteor_litematica_printer.mixins

import com.kkllffaa.meteor_litematica_printer.Modules.CRUD.AtomicSettings.CommonSettings
import net.minecraft.block.entity.SignBlockEntity
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
import meteordevelopment.meteorclient.MeteorClient.mc
import net.minecraft.client.gui.screen.ingame.AbstractSignEditScreen
@Mixin(SignBlockEntity::class)
class SignBlockEntityMixin {
    @Inject(method = ["getMaxTextWidth"], at = [At("RETURN")], cancellable = true)
    private fun modifyMaxTextWidth(ci: CallbackInfoReturnable<Int>) {
        val customWidth = CommonSettings.SignMaxTextWidth.get()
        if (customWidth > 0 && mc.currentScreen is AbstractSignEditScreen) {
            ci.returnValue = customWidth
        }
    }
}
