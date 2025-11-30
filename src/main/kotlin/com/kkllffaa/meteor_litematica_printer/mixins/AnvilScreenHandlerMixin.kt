package com.kkllffaa.meteor_litematica_printer.mixins

import com.kkllffaa.meteor_litematica_printer.Modules.CRUD.AtomicSettings.CommonSettings
import net.minecraft.screen.AnvilScreenHandler
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Overwrite
import org.spongepowered.asm.mixin.Shadow
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
import meteordevelopment.meteorclient.MeteorClient.mc
import net.minecraft.component.DataComponentTypes
import net.minecraft.item.ItemStack
import net.minecraft.text.Text
import net.minecraft.util.StringHelper
import org.spongepowered.asm.mixin.Final
import net.minecraft.screen.slot.Slot

@Mixin(AnvilScreenHandler::class)
class AnvilScreenHandlerMixin {
    companion object {
        @Shadow
        @JvmStatic
        private fun sanitize(name: String): String? = null

    }

    @Shadow
    private var newItemName: String? = null

    @Shadow
    fun getSlot(index: Int): Slot? = null

    @Shadow
    fun updateResult() = Unit

    @Overwrite
    fun setNewItemName(newItemName: String): Boolean {
        val string = sanitize(newItemName);
        if (string != null && !string.equals(this.newItemName)) {
            this.newItemName = string;
            if (this.getSlot(2)!!.hasStack()) {
                val itemStack = this.getSlot(2)!!.getStack();
                if (StringHelper.isBlank(string)) {
                    itemStack.remove(DataComponentTypes.CUSTOM_NAME);
                } else {
                    itemStack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(string));
                }
            }

            this.updateResult();
            return true;
        } else {
            return false;
        }
    }

}