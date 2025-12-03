package com.kkllffaa.meteor_litematica_printer.Functions


import com.kkllffaa.meteor_litematica_printer.Modules.AtomicSettings.*
import meteordevelopment.meteorclient.MeteorClient.mc
import meteordevelopment.meteorclient.systems.modules.movement.GUIMove
import meteordevelopment.meteorclient.systems.modules.render.Freecam
import meteordevelopment.meteorclient.systems.modules.Modules
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.Vec3i
import net.minecraft.util.math.Direction
import net.minecraft.util.Hand
import net.minecraft.item.Item
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket
import kotlin.math.floor

val PlayerHandDistance get() = CommonSettings.PlayerHandDistance
val ClientPlayerEntity.EyeCenterPos get() = Vec3d(x, y + getEyeHeight(pose), z)


fun ClientPlayerEntity.YawDirectionBy(容差: Float = 45f): Direction? = CommonSettings.YawDirectionBy(this, 容差)

fun ClientPlayerEntity.PitchDirectionBy(容差: Float = 45f): Direction? = CommonSettings.PitchDirectionBy(this, 容差)

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


fun ClientPlayerEntity.switchItemThenDo(
    item: Item,
    action: () -> Boolean
): SwapDoResult = SwapSettings.switchItem(this, item, action)

enum class SwapDoResult {
    Success,
    没有物品,
    执行False,
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
