package com.kkllffaa.meteor_litematica_printer.Functions

import com.kkllffaa.meteor_litematica_printer.Modules.CRUD.AtomicSettings.*
import meteordevelopment.meteorclient.MeteorClient.mc
import meteordevelopment.meteorclient.events.render.Render3DEvent
import meteordevelopment.meteorclient.renderer.ShapeMode
import meteordevelopment.meteorclient.utils.world.BlockUtils
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.Vec3i
import net.minecraft.world.LightType

import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.world.RaycastContext
import net.minecraft.block.BlockState
import net.minecraft.block.Block
import kotlin.math.abs


val BlockPos.canTouch get() = CommonSettings.playerCanTouchBlockPos(this)
fun BlockPos.canBreakIt(blockState: BlockState? = null): Boolean {
    return canTouch && BlockUtils.canBreak(
        this,
        blockState ?: mc.world?.getBlockState(this) ?: return false
    )
}


fun BlockPos.BreakIt() {
    BreakSettings.breakBlockWithRotationCfg(this)
}

fun BlockPos.TryBreakIt(blockState: BlockState? = null): Boolean {
    if (canBreakIt(blockState)) {
        BreakIt()
        return true
    }
    return false
}

fun BlockState.needInteractionCountsTo(targetState: BlockState): Int =
    InteractSettings.calculateRequiredInteractions(targetState, this)

fun BlockPos.TryInteractIt(count: Int = 1): Int = InteractSettings.TryInteractBlock(this, count)

fun BlockState.TryPlaceIt(pos: BlockPos): Boolean = PlaceSettings.TryPlaceBlock(this, pos)





val BlockState.isBlockCollisionFullCube: Boolean
    get() {
        try {
            return Block.isShapeFullCube(getCollisionShape(null, null))
        } catch (ignored: Exception) {
            // if we can't get the collision shape, assume it's bad...
        }
        return false
    }

fun BlockPos.Render(event: Render3DEvent, colorScheme: ColorScheme, shapeMode: ShapeMode = ShapeMode.Lines) {
    val shape = mc.world?.getBlockState(this)?.getOutlineShape(mc.world, this) ?: return
    val x1: Double
    val y1: Double
    val z1: Double
    val x2: Double
    val y2: Double
    val z2: Double
    if (!shape.isEmpty) {
        x1 = x + shape.getMin(Direction.Axis.X)
        y1 = y + shape.getMin(Direction.Axis.Y)
        z1 = z + shape.getMin(Direction.Axis.Z)
        x2 = x + shape.getMax(Direction.Axis.X)
        y2 = y + shape.getMax(Direction.Axis.Y)
        z2 = z + shape.getMax(Direction.Axis.Z)
    } else {
        x1 = x.toDouble()
        y1 = y.toDouble()
        z1 = z.toDouble()
        x2 = (x + 1).toDouble()
        y2 = (y + 1).toDouble()
        z2 = (z + 1).toDouble()
    }
    event.renderer.box(x1, y1, z1, x2, y2, z2, colorScheme.sideColor, colorScheme.lineColor, shapeMode, 0)
}

val BlockPos.LightLevel get() = mc.world?.getLightLevel(LightType.BLOCK, this) ?: 15

infix fun Vec3i.ManhattanDistanceTo(pos: Vec3i): Int = abs(x - pos.x) + abs(y - pos.y) + abs(z - pos.z)

val Vec3i.Center: Vec3d get() = Vec3d.ofCenter(this)

val Vec3d.isVisible
    get() = mc.player?.let {
        mc.world?.raycast(
            RaycastContext(
                it.EyeCenterPos, this, RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.NONE, it
            )
        )?.type == HitResult.Type.MISS
    } ?: false

val Pair<Vec3i, Direction>.isVisible: Boolean
    get() = when (second) {
        Direction.UP -> FACE_OFFSETS_UP_OUT
        Direction.DOWN -> FACE_OFFSETS_DOWN_OUT
        Direction.NORTH -> FACE_OFFSETS_NORTH_OUT
        Direction.SOUTH -> FACE_OFFSETS_SOUTH_OUT
        Direction.EAST -> FACE_OFFSETS_EAST_OUT
        Direction.WEST -> FACE_OFFSETS_WEST_OUT
    }.any { (first.vec3d + it).isVisible }


fun Vec3i.PickAFaceFromPlayerPosition(player: ClientPlayerEntity): Direction {
    val direction = this.Center.subtract(player.EyeCenterPos)

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
    }
    // Ray hits either SOUTH or NORTH face
    return if (direction.z > 0) Direction.NORTH else Direction.SOUTH
}

//region 方块锚点到方块面四顶点的偏移常量Vec3d[4]，稍微外扩0.05,用于方块的面穿墙可见性判断
private val FACE_OFFSETS_UP_OUT = arrayOf(
    Vec3d(0.05, 1.05, 0.05),
    Vec3d(0.95, 1.05, 0.05),
    Vec3d(0.95, 1.05, 0.95),
    Vec3d(0.05, 1.05, 0.95)
)

private val FACE_OFFSETS_DOWN_OUT = arrayOf(
    Vec3d(0.05, -0.05, 0.05),
    Vec3d(0.95, -0.05, 0.05),
    Vec3d(0.95, -0.05, 0.95),
    Vec3d(0.05, -0.05, 0.95)
)

private val FACE_OFFSETS_NORTH_OUT = arrayOf(
    Vec3d(0.05, 0.05, -0.05),
    Vec3d(0.95, 0.05, -0.05),
    Vec3d(0.95, 0.95, -0.05),
    Vec3d(0.05, 0.95, -0.05)
)

private val FACE_OFFSETS_SOUTH_OUT = arrayOf(
    Vec3d(0.05, 0.05, 1.05),
    Vec3d(0.95, 0.05, 1.05),
    Vec3d(0.95, 0.95, 1.05),
    Vec3d(0.05, 0.95, 1.05)
)

private val FACE_OFFSETS_EAST_OUT = arrayOf(
    Vec3d(1.05, 0.05, 0.05),
    Vec3d(1.05, 0.05, 0.95),
    Vec3d(1.05, 0.95, 0.95),
    Vec3d(1.05, 0.95, 0.05)
)

private val FACE_OFFSETS_WEST_OUT = arrayOf(
    Vec3d(-0.05, 0.05, 0.05),
    Vec3d(-0.05, 0.05, 0.95),
    Vec3d(-0.05, 0.95, 0.95),
    Vec3d(-0.05, 0.95, 0.05)
)

//endregion
