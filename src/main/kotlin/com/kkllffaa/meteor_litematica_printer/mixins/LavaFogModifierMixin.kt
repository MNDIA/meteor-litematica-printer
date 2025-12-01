package com.kkllffaa.meteor_litematica_printer.mixins

import net.minecraft.block.enums.CameraSubmersionType
import net.minecraft.client.render.Camera
import net.minecraft.client.render.RenderTickCounter
import net.minecraft.client.render.fog.FogData
import net.minecraft.client.render.fog.FogModifier
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

import net.minecraft.client.render.fog.LavaFogModifier
import net.minecraft.client.world.ClientWorld
import net.minecraft.entity.Entity
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(LavaFogModifier::class)
class LavaFogModifierMixin {

    @Inject(method = ["shouldApply"], at = [At("RETURN")])
    private fun shouldApply(submersionType: CameraSubmersionType?, cameraEntity: Entity, ci: CallbackInfoReturnable<Boolean>){
        ci.returnValue = false
    }

}
