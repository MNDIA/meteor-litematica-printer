package com.kkllffaa.meteor_litematica_printer.Functions

import meteordevelopment.meteorclient.MeteorClient
import meteordevelopment.meteorclient.utils.Utils
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.Vec3i
import net.minecraft.world.LightType
import net.minecraft.world.RaycastContext
import kotlin.math.abs

object BlockPosUtils {
    fun getLightLevel(pos: BlockPos?): Int {
        if (MeteorClient.mc.world == null) return 15
        return MeteorClient.mc.world!!.getLightLevel(LightType.BLOCK, pos)
    }

    //region 距离计算
    fun getManhattanDistance(pos1: Vec3i, pos2: Vec3i): Int {
        return abs(pos1.getX() - pos2.getX()) + abs(pos1.getY() - pos2.getY()) + abs(pos1.getZ() - pos2.getZ())
    }

    fun getDistanceFromPosCenterToPlayerEyes(pos: Vec3i): Double {
        if (MeteorClient.mc.player == null) return Double.Companion.MAX_VALUE
        // 等价MyUtils.getPlayerEye(player).distanceTo(Vec3d.ofCenter(pos));
        return Utils.distance(
            MeteorClient.mc.player!!.getX(),
            MeteorClient.mc.player!!.getY() + MeteorClient.mc.player!!.getEyeHeight(MeteorClient.mc.player!!.getPose()),
            MeteorClient.mc.player!!.getZ(),
            pos.getX() + 0.5,
            pos.getY() + 0.5,
            pos.getZ() + 0.5
        )
    }

    // endregion
    //region 玩家角度对准方块的面
    //region 方块锚点到方块面(四顶点)的偏移常量Vec3d[4]
    private val FACE_OFFSETS_UP = arrayOf<Vec3d?>(
        Vec3d(0.0, 1.0, 0.0),
        Vec3d(1.0, 1.0, 0.0),
        Vec3d(1.0, 1.0, 1.0),
        Vec3d(0.0, 1.0, 1.0)
    )

    private val FACE_OFFSETS_DOWN = arrayOf<Vec3d?>(
        Vec3d(0.0, 0.0, 0.0),
        Vec3d(1.0, 0.0, 0.0),
        Vec3d(1.0, 0.0, 1.0),
        Vec3d(0.0, 0.0, 1.0)
    )

    private val FACE_OFFSETS_NORTH = arrayOf<Vec3d?>(
        Vec3d(0.0, 0.0, 0.0),
        Vec3d(1.0, 0.0, 0.0),
        Vec3d(1.0, 1.0, 0.0),
        Vec3d(0.0, 1.0, 0.0)
    )

    private val FACE_OFFSETS_SOUTH = arrayOf<Vec3d?>(
        Vec3d(0.0, 0.0, 1.0),
        Vec3d(1.0, 0.0, 1.0),
        Vec3d(1.0, 1.0, 1.0),
        Vec3d(0.0, 1.0, 1.0)
    )

    private val FACE_OFFSETS_EAST = arrayOf<Vec3d?>(
        Vec3d(1.0, 0.0, 0.0),
        Vec3d(1.0, 0.0, 1.0),
        Vec3d(1.0, 1.0, 1.0),
        Vec3d(1.0, 1.0, 0.0)
    )

    private val FACE_OFFSETS_WEST = arrayOf<Vec3d?>(
        Vec3d(0.0, 0.0, 0.0),
        Vec3d(0.0, 0.0, 1.0),
        Vec3d(0.0, 1.0, 1.0),
        Vec3d(0.0, 1.0, 0.0)
    )

    //endregion
    fun isPlayerYawPitchInTheFaceOfBlock(blockPos: Vec3i, direction: Direction?): Boolean {
        if (MeteorClient.mc.player == null) return false

        val playerEye = Vec3d(
            MeteorClient.mc.player!!.getX(),
            MeteorClient.mc.player!!.getY() + MeteorClient.mc.player!!.getEyeHeight(MeteorClient.mc.player!!.getPose()),
            MeteorClient.mc.player!!.getZ()
        )
        val playerYaw: Float = Rotation.Companion.normalizeYaw(MeteorClient.mc.player!!.getYaw())
        val playerPitch = MeteorClient.mc.player!!.getPitch()
        val offsets = when (direction) {
            Direction.UP -> FACE_OFFSETS_UP
            Direction.DOWN -> FACE_OFFSETS_DOWN
            Direction.NORTH -> FACE_OFFSETS_NORTH
            Direction.SOUTH -> FACE_OFFSETS_SOUTH
            Direction.EAST -> FACE_OFFSETS_EAST
            Direction.WEST -> FACE_OFFSETS_WEST
        }
        val pointsYawRelative: FloatArray? = FloatArray(4)
        val pointsPitchRelative: FloatArray? = FloatArray(4)
        for (i in offsets.indices) {
            val offset = offsets[i]
            val point = Vec3d(blockPos.getX() + offset.x, blockPos.getY() + offset.y, blockPos.getZ() + offset.z)
            val pointRot = RotationStuff.calcRotationFromVec3d(playerEye, point)
            pointsYawRelative!![i] = Rotation.Companion.normalizeYaw(pointRot.getYaw() - playerYaw)
            pointsPitchRelative!![i] = pointRot.getPitch() - playerPitch
        }
        val pitchInRange = MathUtils.acuteAngleSpansZero(pointsPitchRelative!![0], pointsPitchRelative[1]) ||
                MathUtils.acuteAngleSpansZero(pointsPitchRelative[0], pointsPitchRelative[2]) ||
                MathUtils.acuteAngleSpansZero(pointsPitchRelative[0], pointsPitchRelative[3]) ||
                MathUtils.acuteAngleSpansZero(pointsPitchRelative[1], pointsPitchRelative[2]) ||
                MathUtils.acuteAngleSpansZero(pointsPitchRelative[1], pointsPitchRelative[3]) ||
                MathUtils.acuteAngleSpansZero(pointsPitchRelative[2], pointsPitchRelative[3])
        val yawInRange = MathUtils.acuteAngleSpansZero(pointsYawRelative!![0], pointsYawRelative[1]) ||
                MathUtils.acuteAngleSpansZero(pointsYawRelative[0], pointsYawRelative[2]) ||
                MathUtils.acuteAngleSpansZero(pointsYawRelative[0], pointsYawRelative[3]) ||
                MathUtils.acuteAngleSpansZero(pointsYawRelative[1], pointsYawRelative[2]) ||
                MathUtils.acuteAngleSpansZero(pointsYawRelative[1], pointsYawRelative[3]) ||
                MathUtils.acuteAngleSpansZero(pointsYawRelative[2], pointsYawRelative[3])

        return yawInRange && pitchInRange
    }

    // endregion
    //region 方块穿墙可见性判断功能
    //region 方块锚点到方块面四顶点的偏移常量Vec3d[4]，稍微外扩0.05
    private val FACE_OFFSETS_UP_OUT = arrayOf<Vec3d?>(
        Vec3d(0.05, 1.05, 0.05),
        Vec3d(0.95, 1.05, 0.05),
        Vec3d(0.95, 1.05, 0.95),
        Vec3d(0.05, 1.05, 0.95)
    )

    private val FACE_OFFSETS_DOWN_OUT = arrayOf<Vec3d?>(
        Vec3d(0.05, -0.05, 0.05),
        Vec3d(0.95, -0.05, 0.05),
        Vec3d(0.95, -0.05, 0.95),
        Vec3d(0.05, -0.05, 0.95)
    )

    private val FACE_OFFSETS_NORTH_OUT = arrayOf<Vec3d?>(
        Vec3d(0.05, 0.05, -0.05),
        Vec3d(0.95, 0.05, -0.05),
        Vec3d(0.95, 0.95, -0.05),
        Vec3d(0.05, 0.95, -0.05)
    )

    private val FACE_OFFSETS_SOUTH_OUT = arrayOf<Vec3d?>(
        Vec3d(0.05, 0.05, 1.05),
        Vec3d(0.95, 0.05, 1.05),
        Vec3d(0.95, 0.95, 1.05),
        Vec3d(0.05, 0.95, 1.05)
    )

    private val FACE_OFFSETS_EAST_OUT = arrayOf<Vec3d?>(
        Vec3d(1.05, 0.05, 0.05),
        Vec3d(1.05, 0.05, 0.95),
        Vec3d(1.05, 0.95, 0.95),
        Vec3d(1.05, 0.95, 0.05)
    )

    private val FACE_OFFSETS_WEST_OUT = arrayOf<Vec3d?>(
        Vec3d(-0.05, 0.05, 0.05),
        Vec3d(-0.05, 0.05, 0.95),
        Vec3d(-0.05, 0.95, 0.95),
        Vec3d(-0.05, 0.95, 0.05)
    )

    //endregion
    //两点之间没有遮挡
    private fun isLineOfSightClear(start: Vec3d?, end: Vec3d?): Boolean {
        if (MeteorClient.mc.world == null) return false
        val context = RaycastContext(
            start, end, RaycastContext.ShapeType.COLLIDER,
            RaycastContext.FluidHandling.NONE, MeteorClient.mc.player
        )
        val result = MeteorClient.mc.world!!.raycast(context)
        return result.getType() == HitResult.Type.MISS
    }

    // 方块的面可见(不穿墙)
    fun isTheOutFaceVisibleOfBlock(blockPos: Vec3i, direction: Direction?): Boolean {
        if (MeteorClient.mc.player == null) return false
        val playerEye = MeteorClient.mc.player!!.getEyePos()

        val offsets = when (direction) {
            Direction.UP -> FACE_OFFSETS_UP_OUT
            Direction.DOWN -> FACE_OFFSETS_DOWN_OUT
            Direction.NORTH -> FACE_OFFSETS_NORTH_OUT
            Direction.SOUTH -> FACE_OFFSETS_SOUTH_OUT
            Direction.EAST -> FACE_OFFSETS_EAST_OUT
            Direction.WEST -> FACE_OFFSETS_WEST_OUT
        }

        for (offset in offsets) {
            val point = Vec3d(blockPos.getX() + offset.x, blockPos.getY() + offset.y, blockPos.getZ() + offset.z)
            if (isLineOfSightClear(playerEye, point)) {
                return true
            }
        }
        return false
    }

    // 点可见(不穿墙)
    fun isPointVisible(point: Vec3d?): Boolean {
        if (MeteorClient.mc.player == null) return false
        val playerEye = MeteorClient.mc.player!!.getEyePos()
        return isLineOfSightClear(playerEye, point)
    }


    // endregion
    // 选择仅一个合适的面根据砖块相对玩家的位置
    fun getDirectionFromPlayerPosition(pos: Vec3i): Direction {
        val eyePos = MeteorClient.mc.player!!.getEyePos()

        val blockCenter = Vec3d.ofCenter(pos)

        val direction = blockCenter.subtract(eyePos)

        val absX = abs(direction.x)
        val absY = abs(direction.y)
        val absZ = abs(direction.z)

        // Find which component is largest - this determines which face the ray hits
        if (absX > absY && absX > absZ) {
            // Ray hits either EAST or WEST face
            return if (direction.x > 0) Direction.WEST else Direction.EAST
        } else if (absY > absX && absY > absZ) {
            // Ray hits either UP or DOWN face
            return if (direction.y > 0) Direction.DOWN else Direction.UP
        } else {
            // Ray hits either SOUTH or NORTH face
            return if (direction.z > 0) Direction.NORTH else Direction.SOUTH
        }
    }
}
