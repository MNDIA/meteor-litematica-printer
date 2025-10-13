package com.kkllffaa.meteor_litematica_printer.Functions;

import static meteordevelopment.meteorclient.MeteorClient.mc;

import org.jetbrains.annotations.NotNull;


import static com.kkllffaa.meteor_litematica_printer.Functions.MyUtils.*;
import meteordevelopment.meteorclient.utils.Utils;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.LightType;
import net.minecraft.world.RaycastContext;


public class BlockPosUtils {
	
    public static int getLightLevel(BlockPos pos) {
        if (mc.world == null) return 15;
        return mc.world.getLightLevel(LightType.BLOCK, pos);
    }

	//region 距离计算
    public static int getManhattanDistance(Vec3i pos1, Vec3i pos2) {
        return Math.abs(pos1.getX() - pos2.getX()) + 
               Math.abs(pos1.getY() - pos2.getY()) + 
               Math.abs(pos1.getZ() - pos2.getZ());
    }

	public static double getDistanceFromPosCenterToPlayerEyes(Vec3i pos) {
		if (mc.player == null) return Double.MAX_VALUE;
		// 等价MyUtils.getPlayerEye(player).distanceTo(Vec3d.ofCenter(pos));
		return Utils.distance(
				mc.player.getX(),
				mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()),
				mc.player.getZ(),
				pos.getX() + 0.5,
				pos.getY() + 0.5,
				pos.getZ() + 0.5);
	}
	// endregion

	//region 玩家角度对准方块的面
    //region 方块锚点到方块面(四顶点)的偏移常量Vec3d[4]
    private static final Vec3d[] FACE_OFFSETS_UP = {
    		new Vec3d(0, 1, 0),
    		new Vec3d(1, 1, 0),
    		new Vec3d(1, 1, 1),
    		new Vec3d(0, 1, 1)
    };

    private static final Vec3d[] FACE_OFFSETS_DOWN = {
			new Vec3d(0, 0, 0),
			new Vec3d(1, 0, 0),
			new Vec3d(1, 0, 1),
			new Vec3d(0, 0, 1)
	};

	private static final Vec3d[] FACE_OFFSETS_NORTH = {
			new Vec3d(0, 0, 0),
			new Vec3d(1, 0, 0),
			new Vec3d(1, 1, 0),
			new Vec3d(0, 1, 0)
	};

	private static final Vec3d[] FACE_OFFSETS_SOUTH = {
			new Vec3d(0, 0, 1),
			new Vec3d(1, 0, 1),
			new Vec3d(1, 1, 1),
			new Vec3d(0, 1, 1)
	};

	private static final Vec3d[] FACE_OFFSETS_EAST = {
			new Vec3d(1, 0, 0),
			new Vec3d(1, 0, 1),
			new Vec3d(1, 1, 1),
			new Vec3d(1, 1, 0)
	};

	private static final Vec3d[] FACE_OFFSETS_WEST = {
			new Vec3d(0, 0, 0),
			new Vec3d(0, 0, 1),
			new Vec3d(0, 1, 1),
			new Vec3d(0, 1, 0)
	};
	//endregion

    public static boolean isPlayerYawPitchInTheFaceOfBlock(Vec3i blockPos, Direction direction) {
    	if (mc.player == null) return false;

    	Vec3d playerEye = new Vec3d(mc.player.getX(), mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()),
    			mc.player.getZ());
    	float playerYaw = Rotation.normalizeYaw(mc.player.getYaw());
    	float playerPitch = mc.player.getPitch();
    	Vec3d[] offsets = switch (direction) {
    		case UP -> FACE_OFFSETS_UP;
    		case DOWN -> FACE_OFFSETS_DOWN;
    		case NORTH -> FACE_OFFSETS_NORTH;
    		case SOUTH -> FACE_OFFSETS_SOUTH;
    		case EAST -> FACE_OFFSETS_EAST;
    		case WEST -> FACE_OFFSETS_WEST;
    	};
    	float pointsYawRelative[] = new float[4];
    	float pointsPitchRelative[] = new float[4];
    	for (int i = 0; i < offsets.length; i++) {
    		Vec3d offset = offsets[i];
    		Vec3d point = new Vec3d(blockPos.getX() + offset.x, blockPos.getY() + offset.y, blockPos.getZ() + offset.z);
    		Rotation pointRot = RotationStuff.calcRotationFromVec3d(playerEye, point);
    		pointsYawRelative[i] = Rotation.normalizeYaw(pointRot.getYaw() - playerYaw);
    		pointsPitchRelative[i] = pointRot.getPitch() - playerPitch;
    	}
    	boolean pitchInRange = MathUtils.acuteAngleSpansZero(pointsPitchRelative[0], pointsPitchRelative[1]) ||
    			MathUtils.acuteAngleSpansZero(pointsPitchRelative[0], pointsPitchRelative[2]) ||
    			MathUtils.acuteAngleSpansZero(pointsPitchRelative[0], pointsPitchRelative[3]) ||
    			MathUtils.acuteAngleSpansZero(pointsPitchRelative[1], pointsPitchRelative[2]) ||
    			MathUtils.acuteAngleSpansZero(pointsPitchRelative[1], pointsPitchRelative[3]) ||
    			MathUtils.acuteAngleSpansZero(pointsPitchRelative[2], pointsPitchRelative[3]);
    	boolean yawInRange = MathUtils.acuteAngleSpansZero(pointsYawRelative[0], pointsYawRelative[1]) ||
    			MathUtils.acuteAngleSpansZero(pointsYawRelative[0], pointsYawRelative[2]) ||
    			MathUtils.acuteAngleSpansZero(pointsYawRelative[0], pointsYawRelative[3]) ||
    			MathUtils.acuteAngleSpansZero(pointsYawRelative[1], pointsYawRelative[2]) ||
    			MathUtils.acuteAngleSpansZero(pointsYawRelative[1], pointsYawRelative[3]) ||
    			MathUtils.acuteAngleSpansZero(pointsYawRelative[2], pointsYawRelative[3]);
    
    	return yawInRange && pitchInRange;
    }
	// endregion

	//region 方块穿墙可见性判断功能
	//region 方块锚点到方块面四顶点的偏移常量Vec3d[4]，稍微外扩0.05
	private static final Vec3d[] FACE_OFFSETS_UP_OUT = {
			new Vec3d(0.05, 1.05, 0.05),
			new Vec3d(0.95, 1.05, 0.05),
			new Vec3d(0.95, 1.05, 0.95),
			new Vec3d(0.05, 1.05, 0.95)
	};

	private static final Vec3d[] FACE_OFFSETS_DOWN_OUT = {
			new Vec3d(0.05, -0.05, 0.05),
			new Vec3d(0.95, -0.05, 0.05),
			new Vec3d(0.95, -0.05, 0.95),
			new Vec3d(0.05, -0.05, 0.95)
	};

	private static final Vec3d[] FACE_OFFSETS_NORTH_OUT = {
			new Vec3d(0.05, 0.05, -0.05),
			new Vec3d(0.95, 0.05, -0.05),
			new Vec3d(0.95, 0.95, -0.05),
			new Vec3d(0.05, 0.95, -0.05)
	};

	private static final Vec3d[] FACE_OFFSETS_SOUTH_OUT = {
			new Vec3d(0.05, 0.05, 1.05),
			new Vec3d(0.95, 0.05, 1.05),
			new Vec3d(0.95, 0.95, 1.05),
			new Vec3d(0.05, 0.95, 1.05)
	};

	private static final Vec3d[] FACE_OFFSETS_EAST_OUT = {
			new Vec3d(1.05, 0.05, 0.05),
			new Vec3d(1.05, 0.05, 0.95),
			new Vec3d(1.05, 0.95, 0.95),
			new Vec3d(1.05, 0.95, 0.05)
	};

	private static final Vec3d[] FACE_OFFSETS_WEST_OUT = {
			new Vec3d(-0.05, 0.05, 0.05),
			new Vec3d(-0.05, 0.05, 0.95),
			new Vec3d(-0.05, 0.95, 0.95),
			new Vec3d(-0.05, 0.95, 0.05)
	};
	//endregion
	
	//两点之间没有遮挡
	private static boolean isLineOfSightClear(Vec3d start, Vec3d end) {
		if (mc.world == null) return false;
		RaycastContext context = new RaycastContext(start, end, RaycastContext.ShapeType.COLLIDER,
				RaycastContext.FluidHandling.NONE, mc.player);
		BlockHitResult result = mc.world.raycast(context);
		return result.getType() == HitResult.Type.MISS;
	}

	// 方块的面可见(不穿墙)
	public static boolean isTheOutFaceVisibleOfBlock(Vec3i blockPos, Direction direction) {
		if (mc.player == null) return false;
		Vec3d playerEye = MyUtils.getPlayerEye(mc.player);

		Vec3d[] offsets = switch (direction) {
			case UP -> FACE_OFFSETS_UP_OUT;
			case DOWN -> FACE_OFFSETS_DOWN_OUT;
			case NORTH -> FACE_OFFSETS_NORTH_OUT;
			case SOUTH -> FACE_OFFSETS_SOUTH_OUT;
			case EAST -> FACE_OFFSETS_EAST_OUT;
			case WEST -> FACE_OFFSETS_WEST_OUT;
		};

		for (Vec3d offset : offsets) {
			Vec3d point = new Vec3d(blockPos.getX() + offset.x, blockPos.getY() + offset.y, blockPos.getZ() + offset.z);
			if (isLineOfSightClear(playerEye, point)) {
				return true;
			}
		}
		return false;
	}

	// 点可见(不穿墙)
	public static boolean isPointVisible(Vec3d point) {
		if (mc.player == null) return false;
		Vec3d playerEye = MyUtils.getPlayerEye(mc.player);
		return isLineOfSightClear(playerEye, point);
	}
	// endregion


	// 选择仅一个合适的面根据砖块相对玩家的位置
	public static @NotNull Direction getDirectionFromPlayerPosition(@NotNull Vec3i pos) {
		Vec3d eyePos = getPlayerEye(mc.player);

		Vec3d blockCenter = Vec3d.ofCenter(pos);

		Vec3d direction = blockCenter.subtract(eyePos);

		double absX = Math.abs(direction.x);
		double absY = Math.abs(direction.y);
		double absZ = Math.abs(direction.z);

		// Find which component is largest - this determines which face the ray hits
		if (absX > absY && absX > absZ) {
			// Ray hits either EAST or WEST face
			return direction.x > 0 ? Direction.WEST : Direction.EAST;
		} else if (absY > absX && absY > absZ) {
			// Ray hits either UP or DOWN face
			return direction.y > 0 ? Direction.DOWN : Direction.UP;
		} else {
			// Ray hits either SOUTH or NORTH face
			return direction.z > 0 ? Direction.NORTH : Direction.SOUTH;
		}
	}


}