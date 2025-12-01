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


    @Inject(method = ["setNewItemName"], at = [At("HEAD")], cancellable = true)
    private fun setNewItemName(newItemName: String, ci: CallbackInfoReturnable<Boolean>) {
      
            ci.returnValue = true;
        // } else {
        //     cir.returnValue = false;
        // }
        // cir.cancel();
    }

}
