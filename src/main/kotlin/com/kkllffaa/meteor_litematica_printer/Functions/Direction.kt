package com.kkllffaa.meteor_litematica_printer.Functions

import net.minecraft.util.math.Direction
import net.minecraft.block.enums.RailShape
import net.minecraft.block.BlockState
import net.minecraft.state.property.Properties

import net.minecraft.block.Block
import net.minecraft.block.*
import net.minecraft.block.enums.*
import net.minecraft.util.math.RotationPropertyHelper

val Float.AsYawToDirectionNSWE: Direction
    get() = when (val yaw = this.normalizeAsYaw) {
        in 45f..<135f -> Direction.WEST
        in -45f..<45f -> Direction.SOUTH
        in -135f..<-45f -> Direction.EAST
        else -> Direction.NORTH
    }

val Float.AsPitchToDirectionUD: Direction?
    get() = when {
        this > 45f -> Direction.UP
        this < -45f -> Direction.DOWN
        else -> null
    }

val Direction.Left: Direction
    get() = when (this) {
        Direction.NORTH -> Direction.WEST
        Direction.SOUTH -> Direction.EAST
        Direction.WEST -> Direction.SOUTH
        Direction.EAST -> Direction.NORTH
        else -> throw IllegalArgumentException("No LR for UP or DOWN")
    }
val Direction.Right: Direction get() = Left.opposite

val Int.opposite: Int get() = (this + 8) % 16
val Int.Left: Int get() = (this + 12) % 16
val Int.Right: Int get() = (this + 4) % 16

val RailShape.Dir: Direction?
    get() = when (this) {
        RailShape.NORTH_SOUTH -> Direction.SOUTH
        RailShape.EAST_WEST -> Direction.EAST
        else -> null
    }

val BlockState.ATagFaceOf6: Direction?
    get() = when {
        Properties.FACING in this -> get(Properties.FACING)
        Properties.HOPPER_FACING in this -> get(Properties.HOPPER_FACING)
        Properties.HORIZONTAL_FACING in this -> get(Properties.HORIZONTAL_FACING)
        Properties.AXIS in this -> Direction.from(get(Properties.AXIS), Direction.AxisDirection.POSITIVE)
        Properties.HORIZONTAL_AXIS in this -> Direction.from(get(Properties.HORIZONTAL_AXIS), Direction.AxisDirection.POSITIVE)
        Properties.STRAIGHT_RAIL_SHAPE in this -> get(Properties.STRAIGHT_RAIL_SHAPE).Dir
        Properties.VERTICAL_DIRECTION in this -> get(Properties.VERTICAL_DIRECTION)
        Properties.ROTATION in this -> RotationPropertyHelper.toDirection(get(Properties.ROTATION)).orElse(null)
        Properties.ORIENTATION in this -> get(Properties.ORIENTATION).facing
        else -> null
    }

