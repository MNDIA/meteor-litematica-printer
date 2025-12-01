package com.kkllffaa.meteor_litematica_printer.mixins

import com.kkllffaa.meteor_litematica_printer.Modules.CRUD.AtomicSettings.CommonSettings
import org.spongepowered.asm.mixin.Shadow
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
import meteordevelopment.meteorclient.MeteorClient.mc
import net.minecraft.block.entity.SignBlockEntity
import net.minecraft.block.entity.HangingSignBlockEntity
import net.minecraft.client.gui.screen.ingame.AbstractSignEditScreen
import net.minecraft.client.gui.widget.TextFieldWidget
import net.minecraft.screen.AnvilScreenHandler

@Mixin(SignBlockEntity::class, HangingSignBlockEntity::class)
class SignBlockEntityMixin {
    @Inject(method = ["getMaxTextWidth"], at = [At("RETURN")], cancellable = true)
    private fun modifyMaxTextWidth(ci: CallbackInfoReturnable<Int>) {
        val customWidth = CommonSettings.MaxTextWidth.get()
        if (customWidth > 0 && mc.currentScreen is AbstractSignEditScreen) {
            ci.returnValue = customWidth
        }
    }
}


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


@Mixin(TextFieldWidget::class)
class TextFieldWidgetMixin {
    @Shadow
    private var maxLength: Int = 0

    @Inject(method = ["getMaxLength"], at = [At("HEAD")], cancellable = true)
    private fun getMaxLength(ci: CallbackInfoReturnable<Int>) {
        val len = CommonSettings.MaxTextWidth.get()
        if (len > 0) {
            this.maxLength = len
            ci.returnValue = len
        }
    }

    @Inject(method = ["setMaxLength"], at = [At("HEAD")], cancellable = true)
    private fun setMaxLength(maxLength: Int, ci: CallbackInfo) {
        val len = CommonSettings.MaxTextWidth.get()
        if (len > 0) {
            this.maxLength = len
            ci.cancel()
        }
    }
}


