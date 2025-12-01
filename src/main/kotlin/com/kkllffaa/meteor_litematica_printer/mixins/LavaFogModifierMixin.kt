package com.kkllffaa.meteor_litematica_printer.mixins


import net.minecraft.client.render.RenderTickCounter
import net.minecraft.client.render.fog.FogData
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject

import com.kkllffaa.meteor_litematica_printer.Modules.CRUD.AtomicSettings.CommonSettings
import net.minecraft.client.render.fog.LavaFogModifier
import net.minecraft.client.world.ClientWorld
import net.minecraft.entity.Entity
import net.minecraft.util.math.BlockPos
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(LavaFogModifier::class)
class LavaFogModifierMixin {

    @Inject(method = ["applyStartEndModifier"], at = [At("RETURN")])
    private fun applyStartEndModifier(
        data: FogData,
        cameraEntity: Entity,
        cameraPos: BlockPos,
        world: ClientWorld,
        viewDistance: Float,
        tickCounter: RenderTickCounter,
        ci: CallbackInfo
    ) {
        if (CommonSettings.NoFogInLava.get()) {
            data.environmentalStart = -8.0F
            data.environmentalEnd = viewDistance * 0.5F

            data.skyEnd = data.environmentalEnd
            data.cloudEnd = data.environmentalEnd
        }
    }

}
