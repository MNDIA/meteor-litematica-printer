package com.kkllffaa.meteor_litematica_printer.Functions

import java.lang.Float
import kotlin.Boolean
import kotlin.compareTo
import kotlin.math.abs

object MathUtils {
    fun isNearValue(subject: Float, value: Float, halfRange: Float): Boolean {
        return isInOpenRangeValues(subject, value - halfRange, value + halfRange)
    }

    fun isInOpenRangeValues(subject: Float, min: Float, max: Float): Boolean {
        return subject > min && subject < max
    }

    fun acuteAngleSpansZero(yaw1: Float, yaw2: Float): Boolean {
        return (Float.floatToIntBits(yaw1) xor Float.floatToIntBits(yaw2)) < 0 && abs(yaw2 - yaw1) < 180
    }
}
