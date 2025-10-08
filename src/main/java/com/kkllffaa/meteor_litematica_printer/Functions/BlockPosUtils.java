package com.kkllffaa.meteor_litematica_printer.Functions;

import static meteordevelopment.meteorclient.MeteorClient.mc;

import meteordevelopment.meteorclient.utils.Utils;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

public class BlockPosUtils {


    public static int getManhattanDistance(Vec3i pos1, Vec3i pos2) {
        return Math.abs(pos1.getX() - pos2.getX()) + 
               Math.abs(pos1.getY() - pos2.getY()) + 
               Math.abs(pos1.getZ() - pos2.getZ());
    }

    public static double getDistanceFromPosCenterToPlayerEyes(Vec3i pos) {
        ClientPlayerEntity player = mc.player;
        if (player == null) return Double.MAX_VALUE;
        return  Utils.distance(
            player.getX(), 
            player.getY() + player.getEyeHeight(player.getPose()), 
            player.getZ(), 
            pos.getX() + 0.5, 
            pos.getY() + 0.5, 
            pos.getZ() + 0.5
        );
    }

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

    // 玩家角度对准方块的面
    public static boolean isPlayerYawPitchInTheFaceOfBlock(Vec3i blockPos, Direction direction) {
    
    	ClientPlayerEntity player = mc.player;
    	if (player == null)
    		return false;
    
    	Vec3d playerEye = new Vec3d(player.getX(), player.getY() + player.getEyeHeight(player.getPose()),
    			player.getZ());
    	float playerYaw = Rotation.normalizeYaw(player.getYaw());
    	float playerPitch = player.getPitch();
    	Vec3d[] offsets = switch (direction) {
    		case UP -> FACE_OFFSETS_UP;
    		case DOWN -> FACE_OFFSETS_DOWN;
    		case NORTH -> FACE_OFFSETS_NORTH;
    		case SOUTH -> FACE_OFFSETS_SOUTH;
    		case EAST -> FACE_OFFSETS_EAST;
    		case WEST -> FACE_OFFSETS_WEST;
    	};
    	float yaws[] = new float[4];
    	float pitchs[] = new float[4];
    	for (int i = 0; i < offsets.length; i++) {
    		Vec3d offset = offsets[i];
    		Vec3d point = new Vec3d(blockPos.getX() + offset.x, blockPos.getY() + offset.y, blockPos.getZ() + offset.z);
    		Rotation rot = RotationStuff.calcRotationFromVec3d(playerEye, point);
    		yaws[i] = Rotation.normalizeYaw(rot.getYaw() - playerYaw);
    		pitchs[i] = rot.getPitch() - playerPitch;
    	}
    	boolean pitchInRange = MathUtils.acuteAngleSpansZero(pitchs[0], pitchs[1]) ||
    			MathUtils.acuteAngleSpansZero(pitchs[0], pitchs[2]) ||
    			MathUtils.acuteAngleSpansZero(pitchs[0], pitchs[3]) ||
    			MathUtils.acuteAngleSpansZero(pitchs[1], pitchs[2]) ||
    			MathUtils.acuteAngleSpansZero(pitchs[1], pitchs[3]) ||
    			MathUtils.acuteAngleSpansZero(pitchs[2], pitchs[3]);
    	boolean yawInRange = MathUtils.acuteAngleSpansZero(yaws[0], yaws[1]) ||
    			MathUtils.acuteAngleSpansZero(yaws[0], yaws[2]) ||
    			MathUtils.acuteAngleSpansZero(yaws[0], yaws[3]) ||
    			MathUtils.acuteAngleSpansZero(yaws[1], yaws[2]) ||
    			MathUtils.acuteAngleSpansZero(yaws[1], yaws[3]) ||
    			MathUtils.acuteAngleSpansZero(yaws[2], yaws[3]);
    
    	return yawInRange && pitchInRange;
    }


}