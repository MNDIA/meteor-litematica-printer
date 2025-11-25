package com.kkllffaa.meteor_litematica_printer.Functions

import meteordevelopment.meteorclient.MeteorClient
import meteordevelopment.meteorclient.events.render.Render3DEvent
import meteordevelopment.meteorclient.renderer.ShapeMode
import meteordevelopment.meteorclient.utils.player.InvUtils
import meteordevelopment.meteorclient.utils.render.color.SettingColor
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.enums.Orientation
import net.minecraft.block.enums.RailShape
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket
import net.minecraft.state.property.Properties
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.RotationPropertyHelper
import java.util.*
import java.util.function.Predicate
import java.util.function.Supplier

object MyUtils {
    //yaw转换四方向
    private fun ConvertToDirectionFromYaw(yaw: Float): Direction {
        var yaw = yaw
        yaw = Rotation.Companion.normalizeYaw(yaw)
        if ((yaw >= 45 && yaw < 135)) {
            return Direction.WEST
        } else if ((yaw >= -45 && yaw < 45)) {
            return Direction.SOUTH
        } else if ((yaw >= -135 && yaw < -45)) {
            return Direction.EAST
        } else {
            return Direction.NORTH
        }
    }

    // 获取玩家参考系下的相对方向
    fun getLeftDirectionFromPlayer(playerDirection: Direction): Direction? {
        return when (playerDirection) {
            Direction.NORTH -> Direction.WEST
            Direction.EAST -> Direction.NORTH
            Direction.SOUTH -> Direction.EAST
            Direction.WEST -> Direction.SOUTH
            Direction.UP, Direction.DOWN -> {
                val player = MeteorClient.mc.player
                if (player == null) {
                    null
                } else {
                    val horizontal = ConvertToDirectionFromYaw(player.getYaw())
                    getLeftDirectionFromPlayer(horizontal)
                }
            }
        }
    }

    fun getUpDirectionFromPlayer(playerDirection: Direction?): Direction? {
        if (playerDirection == Direction.EAST || playerDirection == Direction.SOUTH || playerDirection == Direction.WEST || playerDirection == Direction.NORTH) {
            return Direction.UP
        }
        val player = MeteorClient.mc.player
        if (player == null) return null
        val horizontal = ConvertToDirectionFromYaw(player.getYaw())

        if (playerDirection == Direction.UP) {
            return horizontal.getOpposite()
        }
        if (playerDirection == Direction.DOWN) {
            return horizontal
        }
        throw IllegalArgumentException("Unexpected direction: " + playerDirection)
    }


    //铁轨属性转换标志方向
    private fun ConvertToDirection(shape: RailShape): Direction? {
        return when (shape) {
            RailShape.NORTH_SOUTH -> Direction.SOUTH
            RailShape.EAST_WEST -> Direction.EAST
            RailShape.ASCENDING_EAST -> Direction.EAST
            RailShape.ASCENDING_WEST -> Direction.EAST
            RailShape.ASCENDING_NORTH -> Direction.SOUTH
            RailShape.ASCENDING_SOUTH -> Direction.SOUTH
            else -> null
        }
    }

    //获取方块状态的一个可区分标志方向
    fun getATagFaceOf(state: BlockState): Direction? {
        if (state.contains(Properties.FACING)) return state.get<Direction?>(Properties.FACING)
        else if (state.contains(Properties.HOPPER_FACING)) return state.get<Direction?>(Properties.HOPPER_FACING)
        else if (state.contains(Properties.HORIZONTAL_FACING)) return state.get<Direction?>(Properties.HORIZONTAL_FACING)
        else if (state.contains(Properties.AXIS)) return Direction.from(
            state.get<Direction.Axis?>(Properties.AXIS),
            Direction.AxisDirection.POSITIVE
        )
        else if (state.contains(Properties.HORIZONTAL_AXIS)) return Direction.from(
            state.get<Direction.Axis?>(Properties.HORIZONTAL_AXIS),
            Direction.AxisDirection.POSITIVE
        )
        else if (state.contains(Properties.STRAIGHT_RAIL_SHAPE)) return ConvertToDirection(
            state.get<RailShape?>(
                Properties.STRAIGHT_RAIL_SHAPE
            )
        )
        else if (state.contains(Properties.VERTICAL_DIRECTION)) return state.get<Direction?>(Properties.VERTICAL_DIRECTION)
        else if (state.contains(Properties.ROTATION)) return RotationPropertyHelper.toDirection(
            state.get<Int?>(
                Properties.ROTATION
            )
        ).orElse(null)
        else if (state.contains(Properties.ORIENTATION)) return state.get<Orientation?>(Properties.ORIENTATION)
            .getFacing()
        else return null // Return null for blocks without directional properties
    }


    fun isBlockShapeFullCube(state: BlockState): Boolean {
        try {
            return Block.isShapeFullCube(state.getCollisionShape(null, null))
        } catch (ignored: Exception) {
            // if we can't get the collision shape, assume it's bad...
        }
        return false
    }

    fun renderPos(event: Render3DEvent, blockPos: BlockPos, shapeMode: ShapeMode?, colorScheme: ColorScheme) {
        val shape = MeteorClient.mc.world!!.getBlockState(blockPos).getOutlineShape(MeteorClient.mc.world, blockPos)
        val x1: Double
        val y1: Double
        val z1: Double
        val x2: Double
        val y2: Double
        val z2: Double
        if (!shape.isEmpty()) {
            x1 = blockPos.getX() + shape.getMin(Direction.Axis.X)
            y1 = blockPos.getY() + shape.getMin(Direction.Axis.Y)
            z1 = blockPos.getZ() + shape.getMin(Direction.Axis.Z)
            x2 = blockPos.getX() + shape.getMax(Direction.Axis.X)
            y2 = blockPos.getY() + shape.getMax(Direction.Axis.Y)
            z2 = blockPos.getZ() + shape.getMax(Direction.Axis.Z)
        } else {
            x1 = blockPos.getX().toDouble()
            y1 = blockPos.getY().toDouble()
            z1 = blockPos.getZ().toDouble()
            x2 = (blockPos.getX() + 1).toDouble()
            y2 = (blockPos.getY() + 1).toDouble()
            z2 = (blockPos.getZ() + 1).toDouble()
        }
        event.renderer.box(x1, y1, z1, x2, y2, z2, colorScheme.sideColor, colorScheme.lineColor, shapeMode, 0)
    }


    private val random = Random()
    private var usedSlot = -1
    fun switchItem(item: Item, state: BlockState?, returnHand: Boolean, action: Supplier<Boolean?>): Boolean {
        if (MeteorClient.mc.player == null) return false

        val selectedSlot = MeteorClient.mc.player!!.getInventory().getSelectedSlot()
        val isCreative = MeteorClient.mc.player!!.getAbilities().creativeMode
        val result = InvUtils.find(item)

        if (MeteorClient.mc.player!!.getMainHandStack().getItem() === item) {
            if (action.get()) {
                usedSlot = MeteorClient.mc.player!!.getInventory().getSelectedSlot()
                return true
            } else return false
        } else if (usedSlot != -1 &&
            MeteorClient.mc.player!!.getInventory().getStack(usedSlot).getItem() === item
        ) {
            InvUtils.swap(usedSlot, returnHand)
            if (action.get()) {
                return true
            } else {
                InvUtils.swap(selectedSlot, returnHand)
                return false
            }
        } else if (result.found()) {
            if (result.isHotbar()) {
                InvUtils.swap(result.slot(), returnHand)

                if (action.get()) {
                    usedSlot = MeteorClient.mc.player!!.getInventory().getSelectedSlot()
                    return true
                } else {
                    InvUtils.swap(selectedSlot, returnHand)
                    return false
                }
            } else if (result.isMain()) {
                val empty = InvUtils.findEmpty()

                if (empty.found() && empty.isHotbar()) {
                    InvUtils.move().from(result.slot()).toHotbar(empty.slot())
                    InvUtils.swap(empty.slot(), returnHand)

                    if (action.get()) {
                        usedSlot = MeteorClient.mc.player!!.getInventory().getSelectedSlot()
                        return true
                    } else {
                        InvUtils.swap(selectedSlot, returnHand)
                        return false
                    }
                } else if (usedSlot != -1) {
                    InvUtils.move().from(result.slot()).toHotbar(usedSlot)
                    InvUtils.swap(usedSlot, returnHand)

                    if (action.get()) {
                        return true
                    } else {
                        InvUtils.swap(selectedSlot, returnHand)
                        return false
                    }
                } else return false
            } else return false
        } else if (isCreative) {
            var slot = 0
            val fir = InvUtils.find(Predicate { obj: ItemStack? -> obj!!.isEmpty() }, 0, 8)
            if (fir.found()) {
                slot = fir.slot()
            }
            MeteorClient.mc.getNetworkHandler()!!
                .sendPacket(CreativeInventoryActionC2SPacket(36 + slot, item.getDefaultStack()))
            InvUtils.swap(slot, returnHand)
            if (action.get()) {
                usedSlot = MeteorClient.mc.player!!.getInventory().getSelectedSlot()
                return true
            } else {
                InvUtils.swap(selectedSlot, returnHand)
                return false
            }
        } else return false
    }


    enum class SafetyFaceMode {
        PlayerRotation,
        PlayerPosition,  // 射线方向
        None,
    }

    enum class RandomDelayMode(private val delays: IntArray) {
        None(null),
        Fast(intArrayOf(0, 0, 1)),
        Balanced(intArrayOf(0, 0, 0, 0, 1, 1, 1, 2, 2, 3)),
        Slow(intArrayOf(0, 0, 1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 4, 5, 6)),
        Variable(intArrayOf(0, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10));

        val theDelay: Int
            get() {
                if (this == RandomDelayMode.None) return 0
                return delays[random.nextInt(delays.size)]
            }
    }

    enum class DistanceMode {
        Auto,
        Max,
    }

    enum class ProtectMode {
        Off,
        ReferencePlayerY,
        ReferenceWorldY
    }

    enum class ListMode {
        Whitelist,
        Blacklist
    }

    enum class ColorScheme(side: SettingColor, line: SettingColor) {
        红(SettingColor(204, 0, 0, 10), SettingColor(204, 0, 0, 255)),
        绿(SettingColor(0, 204, 0, 10), SettingColor(0, 204, 0, 255)),
        蓝(SettingColor(0, 0, 204, 10), SettingColor(0, 0, 204, 255)),
        黄(SettingColor(204, 204, 0, 10), SettingColor(204, 204, 0, 255)),
        紫(SettingColor(204, 0, 204, 10), SettingColor(204, 0, 204, 255)),
        青(SettingColor(0, 204, 204, 10), SettingColor(0, 204, 204, 255));

        val sideColor: SettingColor?
        val lineColor: SettingColor?

        init {
            this.sideColor = side
            this.lineColor = line
        }
    }

    enum class ActionMode {
        None,
        SendPacket,
        Normal
    }

    enum class SafetyFace {
        PlayerRotation,
        PlayerPosition,
    }
}
