package com.kkllffaa.meteor_litematica_printer.Functions

import net.minecraft.util.math.MathHelper.abs
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.Vec3i

fun Float.isNearIn(value: Float, halfRange: Float): Boolean =
    this in value - halfRange..value + halfRange


private fun acuteAngleSpansZero(A: Float, B: Float): Boolean = (A < 0) != (B < 0) && abs(B - A) < 180

val FloatArray.anyPairSpansZero: Boolean
    get() {
        for (i in indices) {
            for (j in i + 1..<size) {
                if (acuteAngleSpansZero(this[i], this[j])) return true
            }
        }
        return false
    }

val Vec3i.vec3d: Vec3d get() = Vec3d.of(this)

operator fun Vec3i.plus(other: Vec3i): Vec3i = add(other)

infix fun Boolean.implies(other: Boolean): Boolean = !this || other
operator fun Vec3d.plus(other: Vec3d): Vec3d = add(other)

operator fun Boolean.plus(other: Boolean): Float = (if (this) 1f else 0f) + (if (other) 1f else 0f)
operator fun Boolean.minus(other: Boolean): Float = (if (this) 1f else 0f) - (if (other) 1f else 0f)
operator fun Boolean.unaryPlus(): Float = if (this) 1f else 0f
operator fun Boolean.unaryMinus(): Float = if (this) -1f else 0f

