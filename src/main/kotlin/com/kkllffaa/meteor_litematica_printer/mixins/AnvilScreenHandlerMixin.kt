package com.kkllffaa.meteor_litematica_printer.mixins

import net.minecraft.screen.AnvilScreenHandler
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Overwrite
import org.spongepowered.asm.mixin.Shadow
import net.minecraft.component.DataComponentTypes
import net.minecraft.text.Text
import net.minecraft.util.StringHelper
import net.minecraft.screen.slot.Slot

@Mixin(AnvilScreenHandler::class)
class AnvilScreenHandlerMixin {
    @Shadow
    private var newItemName: String? = null

    @Shadow
    fun getSlot(index: Int): Slot? = null

    @Shadow
    fun updateResult() = Unit

    @Overwrite
    fun setNewItemName(newItemName: String): Boolean {
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
            return true;
        } else {
            return false;
        }
    }

}
