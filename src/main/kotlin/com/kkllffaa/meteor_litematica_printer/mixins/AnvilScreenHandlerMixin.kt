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

@Mixin(AnvilScreenHandler::class)
class AnvilScreenHandlerMixin {
    @Shadow
    private var newItemName: String = ""

    @Shadow
    fun getSlot(index: Int): Slot = Slot(null, 0, 0, 0)

    @Shadow
    fun updateResult() = Unit

    @Inject(method = ["setNewItemName"], at = [At("HEAD")], cancellable = true)
    fun setNewItemName(newItemName: String, cir: CallbackInfoReturnable<Boolean>) {
        if (newItemName != this.newItemName) {
            this.newItemName = newItemName;
            if (this.getSlot(2)!!.hasStack()) {
                val itemStack = this.getSlot(2)!!.getStack();
                if (StringHelper.isBlank(newItemName)) {
                    itemStack.remove(DataComponentTypes.CUSTOM_NAME);
                } else {
                    itemStack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(newItemName));
                }
            }

            this.updateResult();
            cir.returnValue = true;
        } else {
            cir.returnValue = false;
        }
        cir.cancel();
    }

}
