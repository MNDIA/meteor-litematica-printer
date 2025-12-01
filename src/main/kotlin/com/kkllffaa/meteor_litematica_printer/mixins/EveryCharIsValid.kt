package com.kkllffaa.meteor_litematica_printer.mixins


import com.kkllffaa.meteor_litematica_printer.Modules.CRUD.AtomicSettings.CommonSettings
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
import net.minecraft.util.StringHelper
import net.minecraft.util.Formatting

@Mixin(StringHelper::class)
class StringHelperMixin {

    private companion object {
        @Inject(method = ["isBlank"], at = [At("HEAD")], cancellable = true)
        @JvmStatic
        private fun isBlank(string: String?, ci: CallbackInfoReturnable<Boolean>) {
            // if (CommonSettings.WhitespaceIsNOTBlank.get()) {
            ci.returnValue = string == null || string.isEmpty()
            // }
        }

        @Inject(method = ["isValidChar"], at = [At("HEAD")], cancellable = true)
        @JvmStatic
        private fun isValidChar(c: Int, ci: CallbackInfoReturnable<Boolean>) {
            if (CommonSettings.EveryCharIsValid.get()) ci.returnValue = true
        }

        @Inject(method = ["stripTextFormat"], at = [At("HEAD")], cancellable = true)
        @JvmStatic
        private fun stripTextFormat(string: String?, ci: CallbackInfoReturnable<String?>) {
            if (CommonSettings.EveryCharIsValid.get()) ci.returnValue = string
        }
    }

}


@Mixin(Formatting::class)
class FormattingMixin {
    private companion object {
        @Inject(method = ["strip"], at = [At("HEAD")], cancellable = true)
        @JvmStatic
        private fun strip(string: String?, ci: CallbackInfoReturnable<String?>) {
            if (CommonSettings.EveryCharIsValid.get()) ci.returnValue = string
        }
    }

}