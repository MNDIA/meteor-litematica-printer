package com.kkllffaa.meteor_litematica_printer.mixins

import com.llamalad7.mixinextras.injector.ModifyExpressionValue
import com.llamalad7.mixinextras.injector.ModifyReturnValue
import meteordevelopment.meteorclient.MeteorClient
import meteordevelopment.meteorclient.events.entity.LivingEntityMoveEvent
import meteordevelopment.meteorclient.events.entity.player.JumpVelocityMultiplierEvent
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent
import meteordevelopment.meteorclient.mixininterface.ICamera
import meteordevelopment.meteorclient.systems.modules.Modules
import meteordevelopment.meteorclient.systems.modules.combat.Hitboxes
import meteordevelopment.meteorclient.systems.modules.movement.*
import meteordevelopment.meteorclient.systems.modules.movement.elytrafly.ElytraFly
import meteordevelopment.meteorclient.systems.modules.render.ESP
import meteordevelopment.meteorclient.systems.modules.render.FreeLook
import meteordevelopment.meteorclient.systems.modules.render.Freecam
import meteordevelopment.meteorclient.systems.modules.render.NoRender
import meteordevelopment.meteorclient.systems.modules.world.HighwayBuilder
import meteordevelopment.meteorclient.utils.Utils
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerEntity
import net.minecraft.block.Block
import net.minecraft.block.Blocks
import net.minecraft.client.render.Camera
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityPose
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.MovementType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.math.Vec3d
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.ModifyArgs
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
import org.spongepowered.asm.mixin.injection.invoke.arg.Args
import com.llamalad7.mixinextras.sugar.Local

import meteordevelopment.meteorclient.systems.modules.render.CameraTweaks

import net.minecraft.block.enums.CameraSubmersionType

import net.minecraft.util.math.MathHelper
import net.minecraft.world.BlockView

import org.spongepowered.asm.mixin.Shadow
import org.spongepowered.asm.mixin.Unique

import meteordevelopment.meteorclient.MeteorClient.mc
import kotlin.math.abs

@Mixin(Entity::class, priority = 1001)
abstract class EntityMixin {

    @Inject(method = ["changeLookDirection"], at = [At("HEAD")], cancellable = true)
    private fun updateChangeLookDirection(cursorDeltaX: Double, cursorDeltaY: Double, ci: CallbackInfo) {
        if (this as Any !== MeteorClient.mc.player) return

        val freecam = Modules.get().get<Freecam?>(Freecam::class.java)
        val freeLook = Modules.get().get<FreeLook?>(FreeLook::class.java)

        if (freecam!!.isActive()) {
            freecam.changeLookDirection(cursorDeltaX * 0.15, cursorDeltaY * 0.15)
            ci.cancel()
        } else if (Modules.get().isActive(HighwayBuilder::class.java)) {
            val camera = MeteorClient.mc.gameRenderer.getCamera()
            (camera as ICamera).`meteor$setRot`(
                camera.getYaw() + cursorDeltaX * 0.15,
                camera.getPitch() + cursorDeltaY * 0.15
            )
            ci.cancel()
        } else if (freeLook!!.cameraMode()) {
            freeLook.cameraYaw += (cursorDeltaX / freeLook.sensitivity.get().toFloat()).toFloat()
            freeLook.cameraPitch += (cursorDeltaY / freeLook.sensitivity.get().toFloat()).toFloat()

            if (abs(freeLook.cameraPitch) > 90.0f) freeLook.cameraPitch =
                if (freeLook.cameraPitch > 0.0f) 90.0f else -90.0f
            ci.cancel()
        }
    }
}


@Mixin(Camera::class)
abstract class CameraMixin : ICamera {

    @ModifyArgs(
        method = ["update"],
        at = At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setRotation(FF)V")
    )
    private fun onUpdateSetRotationArgs(args: Args, @Local(argsOnly = true) tickDelta: Float) {
        val freecam = Modules.get().get<Freecam?>(Freecam::class.java)
        val freeLook = Modules.get().get<FreeLook?>(FreeLook::class.java)

        if (freecam!!.isActive()) {
            args.set<Float?>(0, freecam.getYaw(tickDelta).toFloat())
            args.set<Float?>(1, freecam.getPitch(tickDelta).toFloat())
        } else if (Modules.get().isActive(HighwayBuilder::class.java)) {
            args.set<Float?>(0, yaw)
            args.set<Float?>(1, pitch)
        } else if (freeLook!!.isActive()) {
            args.set<Float?>(0, freeLook.cameraYaw)
            args.set<Float?>(1, freeLook.cameraPitch)
        }
    }
}
