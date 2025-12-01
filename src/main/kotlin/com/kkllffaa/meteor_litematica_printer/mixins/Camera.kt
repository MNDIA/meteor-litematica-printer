package com.kkllffaa.meteor_litematica_printer.mixins

import meteordevelopment.meteorclient.MeteorClient
import meteordevelopment.meteorclient.mixininterface.ICamera
import meteordevelopment.meteorclient.systems.modules.Modules
import meteordevelopment.meteorclient.systems.modules.render.FreeLook
import meteordevelopment.meteorclient.systems.modules.render.Freecam
import meteordevelopment.meteorclient.systems.modules.world.HighwayBuilder
import net.minecraft.client.render.Camera
import net.minecraft.entity.Entity
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Shadow
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.ModifyArgs
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import org.spongepowered.asm.mixin.injection.invoke.arg.Args
import com.llamalad7.mixinextras.sugar.Local
import kotlin.math.abs

@Mixin(Entity::class, priority = 1001)
abstract class EntityMixin {

    @Inject(method = ["changeLookDirection"], at = [At("HEAD")], cancellable = true)
    private fun updateChangeLookDirection(cursorDeltaX: Double, cursorDeltaY: Double, ci: CallbackInfo) {
        if (this as Any !== MeteorClient.mc.player) return

        cameraYaw += (cursorDeltaX / 8F).toFloat()
        cameraPitch += (cursorDeltaY / 8F).toFloat()

        if (abs(cameraPitch) > 90.0f) cameraPitch =
            if (cameraPitch > 0.0f) 90.0f else -90.0f
        ci.cancel()

    }
}


@Mixin(Camera::class, priority = 1001)
abstract class CameraMixin : ICamera {
    // @Shadow
    // private var yaw: Float = 0F

    // @Shadow
    // private var pitch: Float = 0F

    @ModifyArgs(
        method = ["update"],
        at = At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setRotation(FF)V")
    )
    private fun onUpdateSetRotationArgs(args: Args, @Local(argsOnly = true) tickDelta: Float) {
        args.set<Float?>(0, cameraYaw)
        args.set<Float?>(1, cameraPitch)
    }
}

var cameraYaw: Float = 0F
var cameraPitch: Float = 0F
