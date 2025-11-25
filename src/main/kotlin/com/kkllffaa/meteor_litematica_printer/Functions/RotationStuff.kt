package com.kkllffaa.meteor_litematica_printer.Functions

import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import net.minecraft.world.RaycastContext
import kotlin.math.sqrt

object RotationStuff {
    val DEG_TO_RAD: Double = Math.PI / 180.0
    val DEG_TO_RAD_F: Float = DEG_TO_RAD.toFloat()
    val RAD_TO_DEG: Double = 180.0 / Math.PI
    val RAD_TO_DEG_F: Float = RAD_TO_DEG.toFloat()
    fun calcRotationFromVec3d(orig: Vec3d, dest: Vec3d, current: Rotation): Rotation? {
        return wrapAnglesToRelative(current, calcRotationFromVec3d(orig, dest))
    }

    fun calcRotationFromVec3d(orig: Vec3d, dest: Vec3d): Rotation {
        val delta = doubleArrayOf(orig.x - dest.x, orig.y - dest.y, orig.z - dest.z)
        val yaw = MathHelper.atan2(delta[0], -delta[2])
        val dist = sqrt(delta[0] * delta[0] + delta[2] * delta[2])
        val pitch = MathHelper.atan2(delta[1], dist)
        return Rotation(
            (yaw * RAD_TO_DEG).toFloat(),
            (pitch * RAD_TO_DEG).toFloat()
        )
    }

    fun wrapAnglesToRelative(current: Rotation, target: Rotation): Rotation? {
        if (current.yawIsReallyClose(target)) {
            return Rotation(current.getYaw(), target.getPitch())
        }
        return target.subtract(current).normalize().add(current)
    }

    fun calcLookDirectionFromRotation(rotation: Rotation): Vec3d {
        val flatZ = MathHelper.cos((-rotation.getYaw() * DEG_TO_RAD_F) - Math.PI.toFloat())
        val flatX = MathHelper.sin((-rotation.getYaw() * DEG_TO_RAD_F) - Math.PI.toFloat())
        val pitchBase = -MathHelper.cos(-rotation.getPitch() * DEG_TO_RAD_F)
        val pitchHeight = MathHelper.sin(-rotation.getPitch() * DEG_TO_RAD_F)
        return Vec3d((flatX * pitchBase).toDouble(), pitchHeight.toDouble(), (flatZ * pitchBase).toDouble())
    }


    fun rayTraceTowards(entity: ClientPlayerEntity, rotation: Rotation, blockReachDistance: Double): HitResult? {
        val start = entity.getCameraPosVec(1.0f)


        val direction = calcLookDirectionFromRotation(rotation)
        val end = start.add(
            direction.x * blockReachDistance,
            direction.y * blockReachDistance,
            direction.z * blockReachDistance
        )
        return entity.getEntityWorld().raycast(
            RaycastContext(
                start,
                end,
                RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.NONE,
                entity
            )
        )
    }
}
