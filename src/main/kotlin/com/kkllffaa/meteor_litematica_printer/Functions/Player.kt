package com.kkllffaa.meteor_litematica_printer.Functions


import com.kkllffaa.meteor_litematica_printer.Modules.CRUD.AtomicSettings.CommonSettings
import meteordevelopment.meteorclient.MeteorClient.mc
import meteordevelopment.meteorclient.utils.player.InvUtils
import meteordevelopment.meteorclient.systems.modules.movement.GUIMove
import meteordevelopment.meteorclient.systems.modules.render.Freecam
import meteordevelopment.meteorclient.systems.modules.Modules
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.Vec3i
import net.minecraft.util.math.Direction
import net.minecraft.util.Hand
import net.minecraft.item.Item
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket
import kotlin.math.floor

val PlayerHandDistance get() = CommonSettings.PlayerHandDistance
val ClientPlayerEntity.EyeCenterPos get() = Vec3d(x, y + getEyeHeight(pose), z)
fun ClientPlayerEntity.YawDirectionBy(容差: Float = 45f): Direction? {
    val yaw = yaw.normalizeAsYaw
    if (yaw.isNearIn(90f, 容差)) {
        return Direction.WEST
    } else if (yaw.isNearIn(0f, 容差)) {
        return Direction.SOUTH
    } else if (yaw.isNearIn(-90f, 容差)) {
        return Direction.EAST
    } else if (yaw.isNearIn(180f, 容差) || yaw.isNearIn(-180f, 容差)) {
        return Direction.NORTH
    }
    return null
}

fun ClientPlayerEntity.PitchDirectionBy(容差: Float = 45f): Direction? {
    val pitch = pitch.clampAsPitch
    if (pitch.isNearIn(90f, 容差)) {
        return Direction.DOWN
    } else if (pitch.isNearIn(-90f, 容差)) {
        return Direction.UP
    } else if (pitch.isNearIn(0f, 容差)) {
        return Direction.NORTH
    }
    return null
}

const val 十六分之周 = 22.50f
fun ClientPlayerEntity.YawInt16By(容差: Float = 11.25f): Int? {
    val yaw = ((yaw % 360.00f) + 360.00f) % 360.00f
    val 周期 = floor(yaw / 十六分之周).toInt()
    val 余数 = yaw - 周期 * 十六分之周

    val result = if (余数.isNearIn(0f, 容差)) {
        周期
    } else if (余数.isNearIn(十六分之周, 容差)) {
        周期 + 1
    } else {
        return null
    }
    // 规范化到 0-15
    return ((result % 16) + 16) % 16
}
//region 玩家角度对准方块的面

//region 方块锚点到方块面(四顶点)的偏移常量Vec3i[4]
private val FACE_OFFSETS_UP = arrayOf(
    Vec3i(0, 1, 0),
    Vec3i(1, 1, 0),
    Vec3i(1, 1, 1),
    Vec3i(0, 1, 1)
)

private val FACE_OFFSETS_DOWN = arrayOf(
    Vec3i(0, 0, 0),
    Vec3i(1, 0, 0),
    Vec3i(1, 0, 1),
    Vec3i(0, 0, 1)
)

private val FACE_OFFSETS_NORTH = arrayOf(
    Vec3i(0, 0, 0),
    Vec3i(1, 0, 0),
    Vec3i(1, 1, 0),
    Vec3i(0, 1, 0)
)

private val FACE_OFFSETS_SOUTH = arrayOf(
    Vec3i(0, 0, 1),
    Vec3i(1, 0, 1),
    Vec3i(1, 1, 1),
    Vec3i(0, 1, 1)
)

private val FACE_OFFSETS_EAST = arrayOf(
    Vec3i(1, 0, 0),
    Vec3i(1, 0, 1),
    Vec3i(1, 1, 1),
    Vec3i(1, 1, 0)
)

private val FACE_OFFSETS_WEST = arrayOf(
    Vec3i(0, 0, 0),
    Vec3i(0, 0, 1),
    Vec3i(0, 1, 1),
    Vec3i(0, 1, 0)
)

//endregion
fun ClientPlayerEntity.RotationInTheFaceOfBlock(blockPos: Vec3i, face: Direction): Boolean {
    val eye = EyeCenterPos
    val playerYaw = yaw
    val playerPitch = pitch
    val offsets = when (face) {
        Direction.UP -> FACE_OFFSETS_UP
        Direction.DOWN -> FACE_OFFSETS_DOWN
        Direction.NORTH -> FACE_OFFSETS_NORTH
        Direction.SOUTH -> FACE_OFFSETS_SOUTH
        Direction.EAST -> FACE_OFFSETS_EAST
        Direction.WEST -> FACE_OFFSETS_WEST
    }
    val pointsYawRelative = FloatArray(4)
    val pointsPitchRelative = FloatArray(4)
    for (i in offsets.indices) {
        val offset = offsets[i]
        val point = (blockPos + offset).vec3d
        val pointRot = (eye to point).Rotation
        pointsYawRelative[i] = (pointRot.yaw - playerYaw).normalizeAsYaw
        pointsPitchRelative[i] = pointRot.pitch - playerPitch
    }
    return pointsYawRelative.anyPairSpansZero && pointsPitchRelative.anyPairSpansZero
}


// endregion


private var usedSlot = 8
fun ClientPlayerEntity.switchItem(
    item: Item,
    returnHand: Boolean,
    action: () -> Boolean
): Boolean {
    val selectedSlot = inventory.selectedSlot
    val isCreative = abilities.creativeMode
    val result = InvUtils.find(item)

    // 执行操作并处理槽位记录
    fun tryAction(updateUsedSlot: Boolean = true): Boolean {
        return if (action()) {
            if (updateUsedSlot) usedSlot = inventory.selectedSlot
            true
        } else false
    }

    // 切换槽位，执行操作，失败则切回
    fun swapAndTry(slot: Int, updateUsedSlot: Boolean = true): Boolean {
        InvUtils.swap(slot, returnHand)
        return if (tryAction(updateUsedSlot)) {
            true
        } else {
            InvUtils.swap(selectedSlot, returnHand)
            false
        }
    }

    return when {
        // 情况1：主手已持有目标物品
        mainHandStack.item === item -> tryAction()

        // 情况2：之前使用的槽位仍有目标物品
        inventory.getStack(usedSlot).item === item -> swapAndTry(usedSlot, updateUsedSlot = false)

        // 情况3：在背包中找到目标物品
        result.found() -> when {
            result.isHotbar -> swapAndTry(result.slot())
            result.isMain -> {
                // 物品在主背包，需要先移到快捷栏
                val empty = InvUtils.find({ it.isEmpty }, 0, 8)
                when {
                    empty.found() -> {
                        InvUtils.move().from(result.slot).toHotbar(empty.slot)
                        swapAndTry(empty.slot)
                    }

                    else -> {
                        InvUtils.move().from(result.slot).toHotbar(usedSlot)
                        swapAndTry(usedSlot, updateUsedSlot = false)
                    }
                }
            }

            else -> false
        }

        // 情况4：创造模式，直接生成物品
        isCreative -> {
            val slot = InvUtils.find({ it.isEmpty }, 0, 8)
                .takeIf { it.found() }?.slot() ?: 0
            mc.networkHandler?.sendPacket(
                CreativeInventoryActionC2SPacket(36 + slot, item.defaultStack)
            )
            swapAndTry(slot)
        }

        else -> false
    }
}


fun ClientPlayerEntity.swing(hand: Hand = Hand.MAIN_HAND, swingMode: ActionMode = ActionMode.Normal) {
    when (swingMode) {
        ActionMode.None -> {}
        ActionMode.SendPacket -> mc.networkHandler?.sendPacket(HandSwingC2SPacket(hand))
        ActionMode.Normal -> swingHand(hand)
    }
}

val isPlayerInControl: Boolean
    get() = (mc.currentScreen == null
            || Modules.get().get(GUIMove::class.java)?.skip() == false)
            && !Modules.get().isActive(Freecam::class.java)
