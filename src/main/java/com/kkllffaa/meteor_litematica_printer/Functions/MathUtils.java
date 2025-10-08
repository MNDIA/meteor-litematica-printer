package com.kkllffaa.meteor_litematica_printer.Functions;

public class MathUtils {

    public static boolean isInRangeOfValue(float subject, float value, float halfRange) {
    	return MyUtils.isInRangeBetweenValues(subject, value - halfRange, value + halfRange);
    }
    
}
