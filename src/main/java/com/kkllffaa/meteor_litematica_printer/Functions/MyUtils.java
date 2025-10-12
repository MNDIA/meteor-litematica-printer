package com.kkllffaa.meteor_litematica_printer.Functions;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.block.*;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationPropertyHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import static meteordevelopment.meteorclient.MeteorClient.mc;

import java.util.Random;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import com.kkllffaa.meteor_litematica_printer.Modules.CRUD.AtomicSettings.*;


public class MyUtils {
	public static PlaceSettings PlaceSettingsModule = new PlaceSettings();
	public static InteractSettings InteractSettingsModule = new InteractSettings();
	public static ContainerSettings ContainerSettingsModule = new ContainerSettings();
	public static BreakSettings BreakSettingsModule = new BreakSettings();


	//yaw转换四方向
	private static @NotNull Direction ConvertToDirectionFromYaw(float yaw) {
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
					Direction horizontal = ConvertToDirectionFromYaw(player.getYaw());
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
		Direction horizontal = ConvertToDirectionFromYaw(player.getYaw());

		if (playerDirection == Direction.UP) {
			return horizontal.getOpposite();
		}
		if (playerDirection == Direction.DOWN) {
			return horizontal;
		}
		throw new IllegalArgumentException("Unexpected direction: " + playerDirection);
	}



	//铁轨属性转换标志方向
	private static @Nullable Direction ConvertToDirection(net.minecraft.block.enums.RailShape shape) {
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
	//获取方块状态的一个可区分标志方向
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
			return ConvertToDirection(state.get(Properties.STRAIGHT_RAIL_SHAPE));
		else if (state.contains(Properties.VERTICAL_DIRECTION))
			return state.get(Properties.VERTICAL_DIRECTION);
		else if (state.contains(Properties.ROTATION))
			return RotationPropertyHelper.toDirection(state.get(Properties.ROTATION)).orElse(null);
		else if (state.contains(Properties.ORIENTATION))
			return state.get(Properties.ORIENTATION).getFacing();
		else
			return null; // Return null for blocks without directional properties
		
	}







	public static boolean isBlockShapeFullCube(BlockState state) {
		try {
			return Block.isShapeFullCube(state.getCollisionShape(null, null));
		} catch (Exception ignored) {
			// if we can't get the collision shape, assume it's bad...
		}
		return false;
	}

	public static @NotNull Vec3d getPlayerEye(@NotNull ClientPlayerEntity player) {
		return new Vec3d(player.getX(), player.getY() + player.getEyeHeight(player.getPose()), player.getZ());
	}

    public static void renderPos(Render3DEvent event, BlockPos blockPos, ShapeMode shapeMode, ColorScheme colorScheme) {
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
            event.renderer.box(x1, y1, z1, x2, y2, z2, colorScheme.sideColor, colorScheme.lineColor, shapeMode, 0);
    }



	public static enum SafetyFaceMode {
		PlayerRotation,
		PlayerPosition, // 射线方向
		None
	}
    
    private static final Random random = new Random();
	public static enum RandomDelayMode {
        None(null),    
        Fast(new int[]{0, 0, 1}),     
        Balanced(new int[]{0, 0, 0, 0, 1, 1, 1, 2, 2, 3}), 
        Slow(new int[]{0, 0, 1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 4, 5, 6}),      
        Variable(new int[]{0, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10});

        private final int[] delays;
		public int getTheDelay(){
			if (this == None) return 0;
			return delays[random.nextInt(delays.length)];
		}

        RandomDelayMode(int[] delays) {
            this.delays = delays;
        }
    }

    public static enum DistanceMode {
        Auto,
        Max,
    }
    public static enum ProtectMode {
		Off,
        ReferencePlayerY,
        ReferenceWorldY
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

	private static int usedSlot = -1;
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
			if (action.get()) {
				usedSlot = mc.player.getInventory().getSelectedSlot();
				return true;
			} else {
				InvUtils.swap(selectedSlot, returnHand);
				return false;
			}
		} else
			return false;
	}

	public static int calculateRequiredInteractions(BlockState targetState, BlockState currentState) {
		return InteractSettingsModule.calculateRequiredInteractions(targetState, currentState);
	}

	public static int interactWithBlock(BlockPos pos, int count) {
		return InteractSettingsModule.interactWithBlock(pos, count);
	}

	public static boolean placeBlock(BlockState required, BlockPos pos) {
		return PlaceSettingsModule.placeBlock(required, pos);
	}

}
