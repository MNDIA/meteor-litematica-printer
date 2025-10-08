package com.kkllffaa.meteor_litematica_printer.Functions;

public class MathUtils {

    public static boolean isNearValue(float subject, float value, float halfRange) {
    	return MathUtils.isInOpenRangeValues(subject, value - halfRange, value + halfRange);
    }

    public static boolean isInOpenRangeValues(float subject, float min, float max) {
    	return subject > min && subject < max;
    }

    public static boolean acuteAngleSpansZero(float yaw1, float yaw2) {
    	return (Float.floatToIntBits(yaw1) ^ Float.floatToIntBits(yaw2)) < 0 && Math.abs(yaw2 - yaw1) < 180;
    }
    
}
