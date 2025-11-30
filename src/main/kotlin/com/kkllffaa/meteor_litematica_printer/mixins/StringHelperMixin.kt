package com.kkllffaa.meteor_litematica_printer.mixins

import com.kkllffaa.meteor_litematica_printer.Modules.CRUD.AtomicSettings.CommonSettings
import net.minecraft.util.StringHelper
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
import meteordevelopment.meteorclient.MeteorClient.mc
import org.spongepowered.asm.mixin.Overwrite


@Mixin(StringHelper::class)
class StringHelperMixin {
    @Overwrite
    fun isValidChar(c: Int): Boolean {
        return true
    }
}
