package com.kkllffaa.meteor_litematica_printer;

import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.block.*;
import net.minecraft.block.enums.BedPart;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import net.minecraft.state.property.Properties;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationPropertyHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.LightType;
import net.minecraft.world.RaycastContext;
import static meteordevelopment.meteorclient.MeteorClient.mc;

import java.util.function.Supplier;

import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import com.kkllffaa.meteor_litematica_printer.settings.InteractSettings;
import com.kkllffaa.meteor_litematica_printer.settings.PlaceSettings;


public class MyUtils {
	public static PlaceSettings PlaceSettingsModule = new PlaceSettings();
	public static InteractSettings InteractSettingsModule = new InteractSettings();


	//region 方块锚点到方块面四顶点的偏移常量Vec3d[4]
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

	// 玩家角度对准方块的面
	public static boolean isPlayerYawPitchInAFaceOfBlock(BlockPos blockPos, Direction direction) {

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
		boolean pitchInRange = CoverZero(pitchs[0], pitchs[1]) ||
				CoverZero(pitchs[0], pitchs[2]) ||
				CoverZero(pitchs[0], pitchs[3]) ||
				CoverZero(pitchs[1], pitchs[2]) ||
				CoverZero(pitchs[1], pitchs[3]) ||
				CoverZero(pitchs[2], pitchs[3]);
		boolean yawInRange = CoverZero(yaws[0], yaws[1]) ||
				CoverZero(yaws[0], yaws[2]) ||
				CoverZero(yaws[0], yaws[3]) ||
				CoverZero(yaws[1], yaws[2]) ||
				CoverZero(yaws[1], yaws[3]) ||
				CoverZero(yaws[2], yaws[3]);

		return yawInRange && pitchInRange;
	}

	// 方块的面不穿墙
	public static boolean isAFaceOutVisibleOfBlock(BlockPos blockPos, Direction direction) {
		ClientPlayerEntity player = mc.player;
		if (player == null)
			return false;
		Vec3d[] offsets = switch (direction) {
			case UP -> FACE_OFFSETS_UP_OUT;
			case DOWN -> FACE_OFFSETS_DOWN_OUT;
			case NORTH -> FACE_OFFSETS_NORTH_OUT;
			case SOUTH -> FACE_OFFSETS_SOUTH_OUT;
			case EAST -> FACE_OFFSETS_EAST_OUT;
			case WEST -> FACE_OFFSETS_WEST_OUT;
		};

		Vec3d playerEye = getPlayerEyePos(player);
		for (Vec3d offset : offsets) {
			Vec3d point = new Vec3d(blockPos.getX() + offset.x, blockPos.getY() + offset.y, blockPos.getZ() + offset.z);
			if (isLineOfSightClear(playerEye, point)) {
				return true;
			}
		}
		return false;
	}

	// 点不穿墙
	public static boolean isPointVisible(Vec3d point) {
		ClientPlayerEntity player = mc.player;
		if (player == null)
			return false;
		Vec3d playerEye = getPlayerEyePos(player);
		return isLineOfSightClear(playerEye, point);
	}

	// 获取玩家参考系下的相对方向
	public static @Nullable Direction getLeftDirectionFromPlayer(@NotNull Direction playerDirection) {
		return switch (playerDirection) {
			case NORTH -> Direction.WEST;
			case EAST -> Direction.NORTH;
			case SOUTH -> Direction.EAST;
			case WEST -> Direction.SOUTH;
			case UP, DOWN -> {
				ClientPlayerEntity player = mc.player;
				if (player == null) {
					yield null;
				} else {
					Direction horizontal = getHorizontalDirectionFromYaw(player.getYaw());
					yield getLeftDirectionFromPlayer(horizontal);
				}
			}
		};
	}

	public static @Nullable Direction getUpDirectionFromPlayer(@NotNull Direction playerDirection) {
		if (playerDirection == Direction.EAST || playerDirection == Direction.SOUTH || playerDirection == Direction.WEST
				|| playerDirection == Direction.NORTH) {
			return Direction.UP;
		}
		ClientPlayerEntity player = mc.player;
		if (player == null)
			return null;
		Direction horizontal = getHorizontalDirectionFromYaw(player.getYaw());

		if (playerDirection == Direction.UP) {
			return horizontal.getOpposite();
		}
		if (playerDirection == Direction.DOWN) {
			return horizontal;
		}
		throw new IllegalArgumentException("Unexpected direction: " + playerDirection);
	}

	//获取方块的一个可区分标志方向
	public static @Nullable Direction getATagFaceOf(@NotNull BlockState state) {
		if (state.contains(Properties.FACING))
			return state.get(Properties.FACING);
		else if (state.contains(Properties.HOPPER_FACING))
			return state.get(Properties.HOPPER_FACING);
		else if (state.contains(Properties.HORIZONTAL_FACING))
			return state.get(Properties.HORIZONTAL_FACING);
		else if (state.contains(Properties.AXIS))
			return Direction.from(state.get(Properties.AXIS), Direction.AxisDirection.POSITIVE);
		else if (state.contains(Properties.HORIZONTAL_AXIS))
			return Direction.from(state.get(Properties.HORIZONTAL_AXIS), Direction.AxisDirection.POSITIVE);
		else if (state.contains(Properties.STRAIGHT_RAIL_SHAPE))
			return railShapeToDirection(state.get(Properties.STRAIGHT_RAIL_SHAPE));
		else if (state.contains(Properties.VERTICAL_DIRECTION))
			return state.get(Properties.VERTICAL_DIRECTION);
		else if (state.contains(Properties.ROTATION))
			return RotationPropertyHelper.toDirection(state.get(Properties.ROTATION)).orElse(null);
		else if (state.contains(Properties.ORIENTATION))
			return state.get(Properties.ORIENTATION).getFacing();
		else
			return null; // Return null for blocks without directional properties
		
	}

    // Meteor原始方法
    private static Direction getDirection(BlockPos pos) {
        Vec3d eyesPos = new Vec3d(mc.player.getX(), mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()), mc.player.getZ());
        if ((double) pos.getY() > eyesPos.y) {
            if (mc.world.getBlockState(pos.add(0, -1, 0)).isReplaceable()) return Direction.DOWN;
            else return mc.player.getHorizontalFacing().getOpposite();
        }
        if (!mc.world.getBlockState(pos.add(0, 1, 0)).isReplaceable()) return mc.player.getHorizontalFacing().getOpposite();
        return Direction.UP;
    }
	// 不支持使用None 依据保护规则选择仅一个合适的面
	public static @Nullable Direction getASafetyFaceOrNull(@NotNull BlockPos pos,
			@NotNull SafetyFaceMode directionMode) {
		return switch (directionMode) {
			case PlayerRotation -> getDirection(pos);
			case PlayerPosition -> {
				ClientPlayerEntity player = mc.player;
				if (player == null)
					yield null;
				// Get player eye position
				Vec3d eyePos = getPlayerEyePos(player);

				// Get block center position
				Vec3d blockCenter = new Vec3d(
						pos.getX() + 0.5,
						pos.getY() + 0.5,
						pos.getZ() + 0.5);

				// Calculate direction vector from eye to block center
				Vec3d direction = blockCenter.subtract(eyePos);

				// Get absolute values of direction components
				double absX = Math.abs(direction.x);
				double absY = Math.abs(direction.y);
				double absZ = Math.abs(direction.z);

				// Find which component is largest - this determines which face the ray hits
				if (absX >= absY && absX >= absZ) {
					// Ray hits either EAST or WEST face
					yield direction.x > 0 ? Direction.WEST : Direction.EAST;
				} else if (absY >= absX && absY >= absZ) {
					// Ray hits either UP or DOWN face
					yield direction.y > 0 ? Direction.DOWN : Direction.UP;
				} else {
					// Ray hits either SOUTH or NORTH face
					yield direction.z > 0 ? Direction.NORTH : Direction.SOUTH;
				}
			}
			default -> null;
		};
	}


	public static boolean isInRangeOfValue(float subject, float value, float halfRange) {
		return isInRangeBetweenValues(subject, value - halfRange, value + halfRange);
	}


	public static boolean isBlockShapeFullCube(BlockState state) {
		try {
			return Block.isShapeFullCube(state.getCollisionShape(null, null));
		} catch (Exception ignored) {
			// if we can't get the collision shape, assume it's bad...
		}
		return false;
	}


	public static @NotNull Vec3d getPlayerEyePos(@NotNull ClientPlayerEntity player) {
		return new Vec3d(player.getX(), player.getY() + player.getEyeHeight(player.getPose()), player.getZ());
	}

	public static double getDistanceToPlayerEyes(BlockPos pos) {
        return  Utils.distance(
            mc.player.getX(), 
            mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()), 
            mc.player.getZ(), 
            pos.getX() + 0.5, 
            pos.getY() + 0.5, 
            pos.getZ() + 0.5
        );
    }

	public static boolean isMultiStructurePlacementAllowed(BlockState required) {
		if (required.contains(Properties.BED_PART)) {
			BedPart bedPart = required.get(Properties.BED_PART);
			if (bedPart == BedPart.HEAD) {
				return false;
			}
		}
		
		if (required.contains(Properties.DOUBLE_BLOCK_HALF)) {
			DoubleBlockHalf doubleBlockHalf = required.get(Properties.DOUBLE_BLOCK_HALF);
			if (doubleBlockHalf == DoubleBlockHalf.UPPER) {
				return false; 
			}
		}
		return true;
	}

    public static void renderPos(Render3DEvent event, BlockPos blockPos, ShapeMode shapeMode, SettingColor sideColorToUse, SettingColor lineColorToUse) {
            VoxelShape shape = mc.world.getBlockState(blockPos).getOutlineShape(mc.world, blockPos);
            double x1;
            double y1;
            double z1;
            double x2;
            double y2;
			double z2;
			if (!shape.isEmpty()) {
				x1 = blockPos.getX() + shape.getMin(Direction.Axis.X);
				y1 = blockPos.getY() + shape.getMin(Direction.Axis.Y);
				z1 = blockPos.getZ() + shape.getMin(Direction.Axis.Z);
				x2 = blockPos.getX() + shape.getMax(Direction.Axis.X);
				y2 = blockPos.getY() + shape.getMax(Direction.Axis.Y);
				z2 = blockPos.getZ() + shape.getMax(Direction.Axis.Z);
			} else {
				x1 = blockPos.getX();
				y1 = blockPos.getY();
				z1 = blockPos.getZ();
				x2 = blockPos.getX() + 1;
				y2 = blockPos.getY() + 1;
				z2 = blockPos.getZ() + 1;
			}
            event.renderer.box(x1, y1, z1, x2, y2, z2, sideColorToUse, lineColorToUse, shapeMode, 0);
    }















 	//两点之间没有遮挡
	private static boolean isLineOfSightClear(Vec3d start, Vec3d end) {
		if (mc.world == null)
			return false;
		RaycastContext context = new RaycastContext(start, end, RaycastContext.ShapeType.COLLIDER,
				RaycastContext.FluidHandling.NONE, mc.player);
		BlockHitResult result = mc.world.raycast(context);
		return result.getType() == HitResult.Type.MISS;
	}
	//yaw转换四方向
	private static @NotNull Direction getHorizontalDirectionFromYaw(float yaw) {
		yaw = Rotation.normalizeYaw(yaw);
		if ((yaw >= 45 && yaw < 135)) {
			return Direction.WEST;
		} else if ((yaw >= -45 && yaw < 45)) {
			return Direction.SOUTH;
		} else if ((yaw >= -135 && yaw < -45)) {
			return Direction.EAST;
		} else {
			return Direction.NORTH;
		}
	}
	//铁轨属性转换标志方向
	private static @Nullable Direction railShapeToDirection(net.minecraft.block.enums.RailShape shape) {
		return switch (shape) {
			case NORTH_SOUTH -> Direction.SOUTH;
			case EAST_WEST -> Direction.EAST;
			case ASCENDING_EAST -> Direction.EAST;
			case ASCENDING_WEST -> Direction.EAST;
			case ASCENDING_NORTH -> Direction.SOUTH;
			case ASCENDING_SOUTH -> Direction.SOUTH;
			default -> null; // For corner rails
		};
	}

	//region 数学工具
	private static boolean isInRangeBetweenValues(float subject, float min, float max) {
		return subject > min && subject < max;
	}
	
	private static boolean CoverZero(float yaw1, float yaw2) {
		return (Float.floatToIntBits(yaw1) ^ Float.floatToIntBits(yaw2)) < 0 && Math.abs(yaw2 - yaw1) < 180;
	}
	//endregion

	private static int usedSlot = -1;
	
	public int calculateRequiredInteractions(BlockState targetState, BlockPos pos) {
		return InteractSettingsModule.calculateRequiredInteractions(targetState, pos);
	}
	public int interactWithBlock(BlockPos pos, int count) {
		return InteractSettingsModule.interactWithBlock(pos, count);
	}

	public static boolean placeBlock(BlockState required, BlockPos pos) {
		return PlaceSettingsModule.placeBlock(required, pos);
	}
    
    public static int getLightLevel(BlockPos pos) {
		ClientWorld world = mc.world;
        if (world == null) return 15;
        return world.getLightLevel(LightType.BLOCK, pos);
    }
	public static enum SafetyFaceMode {
		PlayerRotation,
		PlayerPosition, // 射线方向
		None
	}
    public static enum RandomDelayMode {
        None(new int[]{0}),    
        Fast(new int[]{0, 0, 1}),     
        Balanced(new int[]{0, 0, 0, 0, 1, 1, 1, 2, 2, 3}), 
        Slow(new int[]{0, 0, 1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 4, 5, 6}),      
        Variable(new int[]{0, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10});

        public final int[] delays;

        RandomDelayMode(int[] delays) {
            this.delays = delays;
        }
    }

    public static enum DistanceMode {
        Auto,
        Max,
    }
    public static enum HeightReferenceMode {
        Player,
        World
    }
	public static enum ListMode {
        Whitelist,
        Blacklist
    }
    public static enum ColorScheme {
        红(new SettingColor(204, 0, 0, 10), new SettingColor(204, 0, 0, 255)),
        绿(new SettingColor(0, 204, 0, 10), new SettingColor(0, 204, 0, 255)),
        蓝(new SettingColor(0, 0, 204, 10), new SettingColor(0, 0, 204, 255)),
        黄(new SettingColor(204, 204, 0, 10), new SettingColor(204, 204, 0, 255)),
        紫(new SettingColor(204, 0, 204, 10), new SettingColor(204, 0, 204, 255)),
        青(new SettingColor(0, 204, 204, 10), new SettingColor(0, 204, 204, 255));

        public final SettingColor sideColor;
        public final SettingColor lineColor;

        ColorScheme(SettingColor side, SettingColor line) {
            this.sideColor = side;
            this.lineColor = line;
        }
    }

	public static boolean switchItem(Item item, BlockState state, boolean returnHand, Supplier<Boolean> action) {
		if (mc.player == null)
			return false;

		int selectedSlot = mc.player.getInventory().getSelectedSlot();
		boolean isCreative = mc.player.getAbilities().creativeMode;
		FindItemResult result = InvUtils.find(item);

		if (mc.player.getMainHandStack().getItem() == item) {
			if (action.get()) {
				usedSlot = mc.player.getInventory().getSelectedSlot();
				return true;
			} else
				return false;

		} else if (usedSlot != -1 &&
				mc.player.getInventory().getStack(usedSlot).getItem() == item) {
			InvUtils.swap(usedSlot, returnHand);
			if (action.get()) {
				return true;
			} else {
				InvUtils.swap(selectedSlot, returnHand);
				return false;
			}

		} else if (result.found()) {
			if (result.isHotbar()) {
				InvUtils.swap(result.slot(), returnHand);

				if (action.get()) {
					usedSlot = mc.player.getInventory().getSelectedSlot();
					return true;
				} else {
					InvUtils.swap(selectedSlot, returnHand);
					return false;
				}

			} else if (result.isMain()) {
				FindItemResult empty = InvUtils.findEmpty();

				if (empty.found() && empty.isHotbar()) {
					InvUtils.move().from(result.slot()).toHotbar(empty.slot());
					InvUtils.swap(empty.slot(), returnHand);

					if (action.get()) {
						usedSlot = mc.player.getInventory().getSelectedSlot();
						return true;
					} else {
						InvUtils.swap(selectedSlot, returnHand);
						return false;
					}

				} else if (usedSlot != -1) {
					InvUtils.move().from(result.slot()).toHotbar(usedSlot);
					InvUtils.swap(usedSlot, returnHand);

					if (action.get()) {
						return true;
					} else {
						InvUtils.swap(selectedSlot, returnHand);
						return false;
					}

				} else
					return false;
			} else
				return false;
		} else if (isCreative) {
			int slot = 0;
			FindItemResult fir = InvUtils.find(ItemStack::isEmpty, 0, 8);
			if (fir.found()) {
				slot = fir.slot();
			}
			mc.getNetworkHandler().sendPacket(new CreativeInventoryActionC2SPacket(36 + slot, item.getDefaultStack()));
			InvUtils.swap(slot, returnHand);
			return true;
		} else
			return false;
	}

}
