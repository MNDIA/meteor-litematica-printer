package com.kkllffaa.meteor_litematica_printer.Functions

import java.lang.Float
import kotlin.Boolean
import kotlin.String
import kotlin.check
import kotlin.compareTo
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class Rotation(val yaw: Float, val pitch: Float) {
    init {
        check(
            !(Float.isInfinite(yaw) || Float.isNaN(yaw) || Float.isInfinite(pitch) || Float.isNaN(
                pitch
            ))
        ) { yaw.toString() + " " + pitch }
    }

    fun add(other: Rotation): Rotation {
        return Rotation(
            this.yaw + other.yaw,
            this.pitch + other.pitch
        )
    }

    fun subtract(other: Rotation): Rotation {
        return Rotation(
            this.yaw - other.yaw,
            this.pitch - other.pitch
        )
    }

    fun clamp(): Rotation {
        return Rotation(
            this.yaw,
            clampPitch(this.pitch)
        )
    }

    fun normalize(): Rotation {
        return Rotation(
            normalizeYaw(this.yaw),
            this.pitch
        )
    }

    fun normalizeAndClamp(): Rotation {
        return Rotation(
            normalizeYaw(this.yaw),
            clampPitch(this.pitch)
        )
    }

    fun withPitch(pitch: kotlin.Float): Rotation {
        return Rotation(this.yaw, pitch)
    }

    fun isReallyCloseTo(other: Rotation): Boolean {
        return yawIsReallyClose(other) && abs(this.pitch - other.pitch) < 0.01
    }

    fun yawIsReallyClose(other: Rotation): Boolean {
        val yawDiff: kotlin.Float = abs(normalizeYaw(yaw) - normalizeYaw(other.yaw)) // you cant fool me
        return (yawDiff < 0.01 || yawDiff > 359.99)
    }

    override fun toString(): String {
        return "Yaw: " + yaw + ", Pitch: " + pitch
    }

    companion object {
        fun clampPitch(pitch: kotlin.Float): kotlin.Float {
            return max(-90f, min(90f, pitch))
        }

        fun normalizeYaw(yaw: kotlin.Float): kotlin.Float {
            var newYaw = yaw % 360f
            if (newYaw < -180f) {
                newYaw += 360f
            }
            if (newYaw > 180f) {
                newYaw -= 360f
            }
            return newYaw
        }
    }
}
