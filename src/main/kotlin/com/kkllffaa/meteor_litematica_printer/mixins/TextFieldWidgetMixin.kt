package com.kkllffaa.meteor_litematica_printer.mixins


import com.kkllffaa.meteor_litematica_printer.Modules.CRUD.AtomicSettings.CommonSettings
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
import net.minecraft.client.gui.widget.TextFieldWidget
import org.spongepowered.asm.mixin.Shadow
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(TextFieldWidget::class)
class TextFieldWidgetMixin {
    @Shadow
    var maxLength: Int = 0

    @Inject(method = ["getMaxLength"], at = [At("HEAD")], cancellable = true)
    private fun getMaxLength(ci: CallbackInfoReturnable<Int>) {
        val len = CommonSettings.TextMaxTextWidth.get()
        if (len > 0) {
            this.maxLength = len
            ci.returnValue = len
        }
    }

    @Inject(method = ["setMaxLength"], at = [At("HEAD")], cancellable = true)
    private fun setMaxLength(maxLength: Int, ci: CallbackInfo) {
        val len = CommonSettings.TextMaxTextWidth.get()
        if (len > 0) {
            this.maxLength = len
            ci.cancel()
        }
    }
}
