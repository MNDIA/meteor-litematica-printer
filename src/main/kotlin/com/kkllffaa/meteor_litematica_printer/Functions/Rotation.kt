package com.kkllffaa.meteor_litematica_printer.Functions

import net.minecraft.util.math.MathHelper.*
import net.minecraft.util.math.Position
import net.minecraft.util.math.Vec3d
import kotlin.math.sqrt
import kotlin.math.abs

import meteordevelopment.meteorclient.utils.player.Rotations
import net.minecraft.util.math.BlockPos

data class Rotation(val yaw: Float, val pitch: Float) {
    init {
        check(
            !(yaw.isInfinite() || yaw.isNaN() || pitch.isInfinite() || pitch.isNaN())
        ) { "$yaw $pitch" }
    }
}

infix fun Rotation.add(other: Rotation): Rotation = Rotation(
    this.yaw + other.yaw,
    this.pitch + other.pitch
)

infix fun Rotation.subtract(other: Rotation): Rotation = Rotation(
    this.yaw - other.yaw,
    this.pitch - other.pitch
)

infix fun Rotation.isReallyCloseTo(other: Rotation): Boolean =
    yawIsReallyClose(other) && abs(this.pitch - other.pitch) < 0.01

infix fun Rotation.yawIsReallyClose(other: Rotation): Boolean {
    val yawDiff: Float = abs(yaw.normalizeAsYaw - other.yaw.normalizeAsYaw)
    return (yawDiff !in 0.01..359.99)
}

val Rotation.clamp
    get() = Rotation(
        this.yaw,
        this.pitch.clampAsPitch
    )

val Rotation.normalize
    get() = Rotation(
        this.yaw.normalizeAsYaw,
        this.pitch
    )

val Rotation.normalizeAndClamp
    get() = Rotation(
        this.yaw.normalizeAsYaw,
        this.pitch.clampAsPitch
    )

val Float.clampAsPitch: Float get() = clamp(this, -90f, 90f)
val Float.normalizeAsYaw get() = wrapDegrees(this)

const val DEG_TO_RAD: Double = Math.PI / 180.0
const val DEG_TO_RAD_F: Float = DEG_TO_RAD.toFloat()
const val RAD_TO_DEG: Double = 180.0 / Math.PI
const val RAD_TO_DEG_F: Float = RAD_TO_DEG.toFloat()

val <T : Position> Pair<T, T>.Rotation: Rotation
    get() {
        val delta = doubleArrayOf(first.x - second.x, first.y - second.y, first.z - second.z)
        val yaw = atan2(delta[0], -delta[2])
        val dist = sqrt(delta[0] * delta[0] + delta[2] * delta[2])
        val pitch = atan2(delta[1], dist)
        return Rotation(
            (yaw * RAD_TO_DEG).toFloat(),
            (pitch * RAD_TO_DEG).toFloat()
        )
    }

val Rotation.方向向量: Vec3d
    get() {
        val flatZ = cos((-yaw * DEG_TO_RAD_F) - Math.PI.toFloat())
        val flatX = sin((-yaw * DEG_TO_RAD_F) - Math.PI.toFloat())
        val pitchBase = -cos(-pitch * DEG_TO_RAD_F)
        val pitchHeight = sin(-pitch * DEG_TO_RAD_F)
        return Vec3d((flatX * pitchBase).toDouble(), pitchHeight.toDouble(), (flatZ * pitchBase).toDouble())
    }


inline fun RotateAndDo(
    rotationToPos: BlockPos,
    rotationMode: ActionMode,
    crossinline action: () -> Unit
) {
    when (rotationMode) {
        ActionMode.None -> action()
        ActionMode.SendPacket -> Rotations.rotate(
            Rotations.getYaw(rotationToPos),
            Rotations.getPitch(rotationToPos),
            50,
            false
        ) { action() }

        ActionMode.Normal -> Rotations.rotate(
            Rotations.getYaw(rotationToPos),
            Rotations.getPitch(rotationToPos),
            50,
            true
        ) { action() }
    }
}
