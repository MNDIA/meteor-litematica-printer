package com.kkllffaa.meteor_litematica_printer;

import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import net.minecraft.block.*;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.enums.SlabType;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import static meteordevelopment.meteorclient.utils.world.BlockUtils.canPlace;

//import baritone.api.utils.BetterBlockPos;
//import baritone.api.utils.RayTraceUtils;
//import baritone.api.utils.Rotation;
//import baritone.api.utils.RotationUtils;

public class MyUtils {

	public static boolean place(BlockPos blockPos, Direction direction, SlabType slabType, BlockHalf blockHalf, Direction blockHorizontalOrientation, Axis wantedAxies, boolean airPlace, boolean swingHand, boolean rotate, boolean clientSide, int range) {
		if (mc.player == null) return false;
		if (!canPlace(blockPos)) return false;

		Vec3d hitPos = new Vec3d(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5);

		BlockPos neighbour;

		if (direction == null) {
			if ((slabType != null && slabType != SlabType.DOUBLE || blockHalf != null || blockHorizontalOrientation != null || wantedAxies != null) && !mc.player.isCreative()) return false;
            direction = Direction.UP;
			neighbour = blockPos;
		} else if(airPlace) {
			neighbour = blockPos;
		}else {
			neighbour = blockPos.offset(direction.getOpposite());
			hitPos.add(direction.getOffsetX() * 0.5, direction.getOffsetY() * 0.5, direction.getOffsetZ() * 0.5);
		}


		Direction s = direction;

        if (rotate) {
        	//BetterBlockPos placeAgainstPos = new BetterBlockPos(neighbour.getX(), neighbour.getY(), neighbour.getZ());
			VoxelShape collisionShape = mc.world.getBlockState(neighbour).getCollisionShape(mc.world, neighbour);

			if(collisionShape.isEmpty()) {
				Rotations.rotate(Rotations.getYaw(hitPos), Rotations.getPitch(hitPos), 50, clientSide,
	                    () ->
	                    	place(new BlockHitResult(hitPos, s, neighbour, false), swingHand)
	                    );
				return true;
			}

			Box aabb = collisionShape.getBoundingBox();

            for (double z = 0.1; z < 0.9; z+=0.2)
            for (double x = 0.1; x < 0.9; x+=0.2)
            for (Vec3d placementMultiplier : aabbSideMultipliers(direction.getOpposite())) {

            	double placeX = neighbour.getX() + aabb.minX * x + aabb.maxX * (1 - x);
				if((slabType != null && slabType != SlabType.DOUBLE || blockHalf != null && direction != Direction.UP && direction != Direction.DOWN) && !mc.player.isCreative()) {
					if (slabType == SlabType.BOTTOM || blockHalf == BlockHalf.BOTTOM) {
						if (placementMultiplier.y <= 0.5) continue;
					} else {
						if (placementMultiplier.y > 0.5) continue;
					}
				}
				double placeY = neighbour.getY() + aabb.minY * placementMultiplier.y + aabb.maxY * (1 - placementMultiplier.y);
				double placeZ = neighbour.getZ() + aabb.minZ * z + aabb.maxZ * (1 - z);

                Vec3d testHitPos = new Vec3d(placeX, placeY, placeZ);
     	        Vec3d playerHead = new Vec3d(mc.player.getX(), mc.player.getEyeY(), mc.player.getZ());

     			Rotation rot = RotationStuff.calcRotationFromVec3d(playerHead, testHitPos, new Rotation(mc.player.getYaw(), mc.player.getPitch()));
     			Direction testHorizontalDirection = getHorizontalDirectionFromYaw(rot.normalize().getYaw());
     			if (blockHorizontalOrientation != null
     					&& ( 	testHorizontalDirection.getAxis() != blockHorizontalOrientation.getAxis())) continue;
     			HitResult res = RotationStuff.rayTraceTowards(mc.player, rot, range);
     			BlockHitResult blockHitRes = ((BlockHitResult) res);
     			if(
 					res == null ||
 					res.getType() != HitResult.Type.BLOCK ||
 					!blockHitRes.getBlockPos().equals(neighbour) ||
 					blockHitRes.getSide() != direction
 				) continue;


                Rotations.rotate(Rotations.getYaw(testHitPos), Rotations.getPitch(testHitPos), 50, clientSide,
                    () ->
                    	place(new BlockHitResult(testHitPos, s, neighbour, false), swingHand)
                    );

     			return true;
            }
        } else {
            place(new BlockHitResult(hitPos, s, neighbour, false), swingHand);
        }

		return true;
	}
    
    private static void place(BlockHitResult blockHitResult, boolean swing) {
        if (mc.player == null || mc.interactionManager == null || mc.getNetworkHandler() == null) return;
        boolean wasSneaking = mc.player.input.playerInput.sneak();
        boolean sneak = mc.player.input.playerInput.sneak();
        sneak = false;

        ActionResult result = mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, blockHitResult);

        if (result == ActionResult.SUCCESS) {
            if (swing) mc.player.swingHand(Hand.MAIN_HAND);
            else mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
        }

        sneak = wasSneaking;
    }

	public static boolean isBlockNormalCube(BlockState state) {
	        Block block = state.getBlock();
	        if (block instanceof ScaffoldingBlock
	                || block instanceof ShulkerBoxBlock
	                || block instanceof PointedDripstoneBlock
	                || block instanceof AmethystClusterBlock) {
	            return false;
	        }
	        try {
	            return Block.isShapeFullCube(state.getCollisionShape(null, null)) || state.getBlock() instanceof StairsBlock;
	        } catch (Exception ignored) {
	            // if we can't get the collision shape, assume it's bad...
	        }
	        return false;
    }

	public static boolean canPlaceAgainst(BlockState placeAtState, BlockState placeAgainstState, Direction against) {
	        // can we look at the center of a side face of this block and likely be able to place?
	        // therefore dont include weird things that we technically could place against (like carpet) but practically can't


		return isBlockNormalCube(placeAgainstState) ||
        		placeAgainstState.getBlock() == Blocks.GLASS ||
        		placeAgainstState.getBlock() instanceof StainedGlassBlock ||
        		placeAgainstState.getBlock() instanceof StairsBlock ||
        		placeAgainstState.getBlock() instanceof SlabBlock &&
        		(
	        		placeAgainstState.get(SlabBlock.TYPE) != SlabType.BOTTOM &&
    				placeAtState.getBlock() == placeAgainstState.getBlock() &&
					against != Direction.DOWN ||
					placeAtState.getBlock() != placeAgainstState.getBlock()
				);
	}

	public static boolean isBlockInLineOfSight(BlockPos placeAt, BlockState placeAtState) {
		Vec3d playerHead = new Vec3d(mc.player.getX(), mc.player.getEyeY(), mc.player.getZ());
		Vec3d placeAtVec = new Vec3d(placeAt.getX() + 0.5, placeAt.getY() + 0.5, placeAt.getZ() + 0.5);

		ShapeType type = RaycastContext.ShapeType.COLLIDER;
		FluidHandling fluid = RaycastContext.FluidHandling.NONE;

		RaycastContext context =
			new RaycastContext(playerHead, placeAtVec, type, fluid, mc.player);
		BlockHitResult bhr = mc.world.raycast(context);
			// check line of sight
		return (bhr.getType() == HitResult.Type.MISS);

	}

	/**
	 *
	 * @param block
	 * @return Weather a block will orient towards a block it is placed on
	 */
	public static boolean isBlockSameAsPlaceDir(Block block) {
		return block instanceof HopperBlock;
	}

	/**
	 *
	 * @param block
	 * @return Weather a block will orient opposite to a block it is placed on
	 */
	public static boolean isBlockPlacementOppositeToPlacePos(Block block) {
		return block instanceof AmethystClusterBlock
				|| block instanceof EndRodBlock
				|| block instanceof LightningRodBlock
				|| block instanceof TrapdoorBlock
				|| block instanceof ChainBlock
				|| block == Blocks.OAK_LOG
				|| block == Blocks.SPRUCE_LOG
				|| block == Blocks.BIRCH_LOG
				|| block == Blocks.JUNGLE_LOG
				|| block == Blocks.ACACIA_LOG
				|| block == Blocks.DARK_OAK_LOG
				|| block == Blocks.STRIPPED_SPRUCE_LOG
				|| block == Blocks.STRIPPED_BIRCH_LOG
				|| block == Blocks.STRIPPED_JUNGLE_LOG
				|| block == Blocks.STRIPPED_ACACIA_LOG
				|| block == Blocks.STRIPPED_DARK_OAK_LOG
				;
	}



	/**
	 * Normal behaviour in this case is considered as when blocks are placed they take direction opposite to players direction.
	 *
	 * Pitch between 45 (excluding) and -45 (excluding) means we are looking forward, below 45 (including) means we are looking down, and below -45 (including) means we are looking up
	 *
	 *
	 * ObserverBlock faces same direction as player, ObserverBlock also checks pitch to place observer upwards or downwards.
	 * AnvilBlock will face to direction clockwise of current look direction
	 * Buttons face same direction as player when on floor or ceiling but when on the wall it takes opposite to block it is placed on
	 * Bell acts same as Buttons
	 * GrindstoneBlock acts same as Buttons
	 * TrapdoorBlock normal facing when on floor or ceiling but when on the wall it takes opposite to block it is placed on
	 *
	 * @param block
	 * @return weather a block is a special case in terms of rotation
	 */
	public static boolean isBlockSpecialCase(Block block) {
		return block instanceof ObserverBlock
				|| block instanceof AnvilBlock
				|| block instanceof GrindstoneBlock
				|| block instanceof ButtonBlock
				;
	}

	/**
	 * @param block
	 * @return weather block will face same direction as player when on floor or ceiling but when on the wall it takes opposite to block it is placed on
	 */
	public static boolean isBlockLikeButton(Block block) {
		return block instanceof ButtonBlock
				|| block instanceof BellBlock
				|| block instanceof GrindstoneBlock
				|| block instanceof TrapdoorBlock
				;
	}

	/**
	 * Pitch between 45 (excluding) and -45 (excluding) means we are looking forward, below 45 (including) means we are looking down, and below -45 (including) means we are looking up
	 *
	 * @param block
	 * @return Weather block checks pitch to orient upwards or downwards
	 */
	public static boolean isBlockCheckingPitchForVerticalDir(Block block) {
		return block instanceof ObserverBlock
				|| block instanceof PistonBlock
				;
	}

	/**
	 * Check if a block is directional and should be protected from wrong-direction placement
	 * These blocks should only be placed when player direction matches the intended direction
	 */
	/**
	 * 检查方块是否具有方向性（具有影响放置的朝向属性）
	 * 这些方块只应在玩家朝向与预期方向匹配时放置
	 * Check if a block is directional (has facing properties that matter for placement)
	 * These blocks should only be placed when player direction matches the intended direction
	 */
	public static boolean isDirectionalBlock(Block block) {
		return block instanceof ObserverBlock        // 观察者方块 - 具有检测朝向
				|| block instanceof PistonBlock      // 活塞 - 推动方向很重要
				|| block instanceof RepeaterBlock    // 红石中继器 - 信号传递方向
				|| block instanceof ComparatorBlock  // 红石比较器 - 比较和传递方向
				|| block instanceof DropperBlock     // 投掷器 - 物品输出方向
				|| block instanceof DispenserBlock   // 发射器 - 物品发射方向
				|| block instanceof FurnaceBlock     // 熔炉 - 操作面朝向
				|| block instanceof BlastFurnaceBlock // 高炉 - 操作面朝向
				|| block instanceof SmokerBlock      // 烟熏炉 - 操作面朝向
				|| block instanceof HopperBlock      // 漏斗 - 物品传输方向
				|| block instanceof ShulkerBoxBlock  // 潜影盒 - 开口朝向
				|| block instanceof EndRodBlock      // 末地烛 - 指向方向
				|| block instanceof BedBlock         // 床 - 头部朝向
				|| block instanceof ChestBlock       // 箱子 - 开口朝向
				|| block instanceof EnderChestBlock  // 末影箱 - 开口朝向
				|| block instanceof BarrelBlock      // 桶 - 开口朝向
				|| block instanceof LecternBlock     // 讲台 - 阅读面朝向
				|| block instanceof LoomBlock        // 织布机 - 操作面朝向
				|| block instanceof StonecutterBlock // 切石机 - 操作面朝向
				|| block instanceof AnvilBlock       // 铁砧 - 使用方向
				|| block instanceof GrindstoneBlock  // 砂轮 - 操作面朝向
				;
	}

	/**
	 * 检查方块的所需方向是否与玩家的朝向兼容
	 * Check if the block's required direction is compatible with player's facing direction
	 */
	public static boolean isDirectionCompatible(Block block, Direction requiredDirection, Direction playerDirection) {
		if (block instanceof ObserverBlock) {
			// 观察者朝向它们观察的方向（与玩家看的方向相同）
			// Observers face towards what they're observing (same direction as player looking)
			return requiredDirection.equals(playerDirection);
		} else if (block instanceof PistonBlock) {
			// 活塞朝向它们推动的方向（与玩家看的方向相同）
			// Pistons face towards what they push (same direction as player looking)
			return requiredDirection.equals(playerDirection);
		} else if (block instanceof RepeaterBlock || block instanceof ComparatorBlock) {
			// 中继器和比较器朝向远离玩家的方向
			// Repeaters and comparators face away from the player
			return requiredDirection.equals(playerDirection.getOpposite());
		} else if (block instanceof DropperBlock || block instanceof DispenserBlock) {
			// 投掷器和发射器朝向它们输出的方向（与玩家方向相同）
			// Droppers and dispensers face towards where they output (same as player direction)
			return requiredDirection.equals(playerDirection);
		} else if (block instanceof FurnaceBlock || block instanceof BlastFurnaceBlock || block instanceof SmokerBlock) {
			// 熔炉类方块朝向远离玩家的方向（输入面朝向玩家）
			// Furnaces face away from player (input side towards player)
			return requiredDirection.equals(playerDirection.getOpposite());
		} else if (block instanceof HopperBlock) {
			// 漏斗更灵活 - 可以朝下或任何水平方向
			// Hoppers are more flexible - they can face down or any horizontal direction
			if (requiredDirection == Direction.DOWN) return true;
			// 对于水平漏斗，它们通常指向物品去向
			// For horizontal hoppers, they usually point towards where items go
			return true; // 暂时允许漏斗的任何方向 Allow any direction for hoppers for now
		} else if (block instanceof ChestBlock || block instanceof EnderChestBlock || block instanceof BarrelBlock) {
			// 箱子和桶朝向远离玩家的方向
			// Chests and barrels face away from player
			return requiredDirection.equals(playerDirection.getOpposite());
		} else if (block instanceof ShulkerBoxBlock) {
			// 潜影盒朝向远离玩家的方向
			// Shulker boxes face away from player
			return requiredDirection.equals(playerDirection.getOpposite());
		} else if (block instanceof EndRodBlock) {
			// 末地烛朝向与玩家相同的方向
			// End rods face the same direction as player
			return requiredDirection.equals(playerDirection);
		} else if (block instanceof BedBlock) {
			// 床：头部朝向远离玩家的方向
			// Beds: head faces away from player
			return requiredDirection.equals(playerDirection.getOpposite());
		} else if (block instanceof LecternBlock || block instanceof LoomBlock || block instanceof StonecutterBlock) {
			// 这些方块朝向远离玩家的方向
			// These face away from player
			return requiredDirection.equals(playerDirection.getOpposite());
		} else if (block instanceof AnvilBlock || block instanceof GrindstoneBlock) {
			// 这些方块朝向远离玩家的方向
			// These face away from player
			return requiredDirection.equals(playerDirection.getOpposite());
		} else {
			// 对于其他方向性方块，使用通用规则：朝向远离玩家的方向
			// For other directional blocks, use general rule: face away from player
			return requiredDirection.equals(playerDirection.getOpposite());
		}
	}

	/**
	 * 获取玩家当前面朝的方向
	 * Get the direction the player is currently facing
	 */
	public static Direction getPlayerFacingDirection() {
		if (mc.player == null) return null;
		
		float yaw = mc.player.getYaw();
		float pitch = mc.player.getPitch();
		
		if (isIn(pitch, 90, 25)) {
			return Direction.DOWN;
		} else if (isIn(pitch, -90, 25)) {
			return Direction.UP;
		} else if (isIn(pitch, 0, 25)) {
			return getHorizontalDirectionFromYaw(yaw);
		} else {
			return null;
		}
	}

	public static boolean isFaceDesired(Block block, Direction blockHorizontalOrientation, Direction against) {
		return blockHorizontalOrientation == null || !(isBlockSameAsPlaceDir(block) || isBlockPlacementOppositeToPlacePos(block)) || (
				isBlockSameAsPlaceDir(block) && blockHorizontalOrientation == against
				|| block instanceof TrapdoorBlock && against.getOpposite() == blockHorizontalOrientation
				|| !(block instanceof TrapdoorBlock) && (
        		isBlockPlacementOppositeToPlacePos(block) && blockHorizontalOrientation == against.getOpposite()
        		|| isBlockLikeButton(block) && against != Direction.UP && against != Direction.DOWN && blockHorizontalOrientation == against)
        		);
	}

	public static boolean isPlayerOrientationDesired(Block block, Direction blockHorizontalOrientation, Direction playerOrientation) {
		return blockHorizontalOrientation == null
				|| (
				block instanceof StairsBlock && playerOrientation == blockHorizontalOrientation ||
				!(block instanceof StairsBlock) &&
				!isBlockPlacementOppositeToPlacePos(block) && !isBlockSameAsPlaceDir(block) && playerOrientation == blockHorizontalOrientation.getOpposite()

					);
	}

	public static Direction getVisiblePlaceSide(BlockPos placeAt, BlockState placeAtState, SlabType slabType, BlockHalf blockHalf, Direction blockHorizontalOrientation, Axis wantedAxies, int range, Direction requiredDir) {
		if (mc.world == null) return null;
		for (Direction against : Direction.values()) {
            //BetterBlockPos placeAgainstPos = new BetterBlockPos(placeAt.getX(), placeAt.getY(), placeAt.getZ()).relative(against);
            // BlockState placeAgainstState = mc.world.getBlockState(placeAgainstPos);

            if(wantedAxies != null && against.getAxis() != wantedAxies || blockHalf != null && (against == Direction.UP && blockHalf == BlockHalf.BOTTOM || against == Direction.DOWN && blockHalf == BlockHalf.TOP))
            	continue;

            if((slabType != null && slabType != SlabType.DOUBLE) && !mc.player.isCreative()) {
				if (slabType == SlabType.BOTTOM) {
					if (against == Direction.DOWN) continue;
				} else {
					if (against == Direction.UP) continue;
				}
			}

            if (wantedAxies == null && !isFaceDesired(placeAtState.getBlock(), blockHorizontalOrientation, against) || wantedAxies != null && wantedAxies != against.getAxis()) continue;

            if(!canPlaceAgainst(
        		placeAtState,
				mc.world.getBlockState(placeAt),
				against
			) || BlockUtils.isClickable(mc.world.getBlockState(placeAt).getBlock()))
			continue;
            Box aabb = mc.world.getBlockState(placeAt).getCollisionShape(mc.world, placeAt).getBoundingBox();

            for (double z = 0.1; z < 0.9; z+=0.2)
            for (double x = 0.1; x < 0.9; x+=0.2)
            for (Vec3d placementMultiplier : aabbSideMultipliers(against)) {
            	 double placeX = placeAt.getX() + aabb.minX * x + aabb.maxX * (1 - x);
            	 if((slabType != null && slabType != SlabType.DOUBLE || blockHalf != null && against != Direction.DOWN && against != Direction.UP) && !mc.player.isCreative()) {
 					if (slabType == SlabType.BOTTOM || blockHalf == BlockHalf.BOTTOM) {
 						if (placementMultiplier.y <= 0.5) continue;
 					} else {
 						if (placementMultiplier.y > 0.5) continue;
 					}
 				}
                 double placeY = placeAt.getY() + aabb.minY * placementMultiplier.y + aabb.maxY * (1 - placementMultiplier.y);
                 double placeZ = placeAt.getZ() + aabb.minZ * z + aabb.maxZ * (1 - z);

                Vec3d hitPos = new Vec3d(placeX, placeY, placeZ);
     	        Vec3d playerHead = new Vec3d(mc.player.getX(), mc.player.getEyeY(), mc.player.getZ());
     			Rotation rot = RotationStuff.calcRotationFromVec3d(playerHead, hitPos, new Rotation(mc.player.getYaw(), mc.player.getPitch()));

				Direction testHorizontalDirection = getHorizontalDirectionFromYaw(rot.normalize().getYaw());
				if (placeAtState.getBlock() instanceof TrapdoorBlock && !(against != Direction.DOWN && against != Direction.UP) && !isPlayerOrientationDesired(placeAtState.getBlock(), blockHorizontalOrientation, testHorizontalDirection)
						|| !(placeAtState.getBlock() instanceof TrapdoorBlock) && !isPlayerOrientationDesired(placeAtState.getBlock(), blockHorizontalOrientation, testHorizontalDirection)
						) continue;
     			HitResult res = RotationStuff.rayTraceTowards(mc.player, rot, range);
     			BlockHitResult blockHitRes = ((BlockHitResult) res);

     			if(
 					res == null
 					|| res.getType() != HitResult.Type.BLOCK
 					|| !blockHitRes.getBlockPos().equals(placeAt)
 					|| blockHitRes.getSide() != against.getOpposite()
 				) continue;


    			return against.getOpposite();

            }
		}
		return null;
	}


	public static Direction getPlaceSide(BlockPos blockPos, BlockState placeAtState, SlabType slabType, BlockHalf blockHalf, Direction blockHorizontalOrientation, Axis wantedAxies, Direction requiredDir) {
        for (Direction side : Direction.values()) {

            BlockPos neighbor = blockPos.offset(side);
            Direction side2 = side.getOpposite();

        	if(wantedAxies != null && side.getAxis() != wantedAxies || blockHalf != null && (side == Direction.UP && blockHalf == BlockHalf.BOTTOM || side == Direction.DOWN && blockHalf == BlockHalf.TOP))
        		continue;


        	if((slabType != null && slabType != SlabType.DOUBLE || blockHalf != null) && !mc.player.isCreative()) {
				if (slabType == SlabType.BOTTOM || blockHalf == BlockHalf.BOTTOM) {
					if (side2 == Direction.DOWN) continue;
				} else {
					if (side2 == Direction.UP) continue;
				}
			}
            BlockState state = mc.world.getBlockState(neighbor);
            if (wantedAxies == null && !isFaceDesired(placeAtState.getBlock(), blockHorizontalOrientation, side) || wantedAxies != null && wantedAxies != side.getAxis()) continue;

            // Check if neighbour isn't empty
            if (state.isAir() || BlockUtils.isClickable(state.getBlock()) || state.contains(Properties.SLAB_TYPE)
            		&& (state.get(Properties.SLAB_TYPE) == SlabType.DOUBLE
            		|| side == Direction.UP && state.get(Properties.SLAB_TYPE) == SlabType.TOP
            		|| side == Direction.DOWN && state.get(Properties.SLAB_TYPE) == SlabType.BOTTOM
            		)) continue;

            // Check if neighbour is a fluid
            if (!state.getFluidState().isEmpty()) continue;

            Vec3d hitPos = new Vec3d(neighbor.getX(), neighbor.getY(), neighbor.getZ());
 	        Vec3d playerHead = new Vec3d(mc.player.getX(), mc.player.getEyeY(), mc.player.getZ());
 			Rotation rot = RotationStuff.calcRotationFromVec3d(playerHead, hitPos, new Rotation(mc.player.getYaw(), mc.player.getPitch()));

			Direction testHorizontalDirection = getHorizontalDirectionFromYaw(rot.normalize().getYaw());

			if (placeAtState.getBlock() instanceof TrapdoorBlock && !(side != Direction.DOWN && side != Direction.UP) && !isPlayerOrientationDesired(placeAtState.getBlock(), blockHorizontalOrientation, testHorizontalDirection)
					|| !(placeAtState.getBlock() instanceof TrapdoorBlock) && !isPlayerOrientationDesired(placeAtState.getBlock(), blockHorizontalOrientation, testHorizontalDirection)
					) continue;

            return side2;
        }

        return null;
    }

    /*
	public static NbtCompound getNbtFromBlockState (ItemStack itemStack, BlockState state) {
		//NbtCompound nbt = itemStack.getOrCreateNbt();
		NbtCompound nbt = itemStack.getOrCreateNbt();
		NbtCompound subNbt = new NbtCompound();
		for (Property<?> property : state.getProperties()) {
			subNbt.putString(property.getName(), state.get(property).toString());
		}
		nbt.put("BlockStateTag", subNbt);

		return nbt;
	}
	*/
	private static Vec3d[] aabbSideMultipliers(Direction side) {
        switch (side) {
            case UP:
                return new Vec3d[]{new Vec3d(0.5, 1, 0.5), new Vec3d(0.1, 1, 0.5), new Vec3d(0.9, 1, 0.5), new Vec3d(0.5, 1, 0.1), new Vec3d(0.5, 1, 0.9)};
            case DOWN:
                return new Vec3d[]{new Vec3d(0.5, 0, 0.5), new Vec3d(0.1, 0, 0.5), new Vec3d(0.9, 0, 0.5), new Vec3d(0.5, 0, 0.1), new Vec3d(0.5, 0, 0.9)};
            case NORTH:
            case SOUTH:
            case EAST:
            case WEST:
                double x = side.getOffsetX() == 0 ? 0.5 : (1 + side.getOffsetX()) / 2D;
                double z = side.getOffsetZ() == 0 ? 0.5 : (1 + side.getOffsetZ()) / 2D;
                return new Vec3d[]{new Vec3d(x, 0.25, z), new Vec3d(x, 0.75, z)};
            default: // null
                throw new IllegalStateException();
        }
    }

	public static Direction getHorizontalDirectionFromYaw(float yaw) {
        yaw = yaw % 360.0F;
        if (yaw > 180.0F) {
            yaw -= 360.0F;
        } else if (yaw < -180.0F) {
            yaw += 360.0F;
        }
        if (isIn(yaw, 90,25)) {
            return Direction.WEST;
        } else if (isIn(yaw, 0, 25)) {
            return Direction.SOUTH;
        } else if (isIn(yaw, -90, 25)) {
            return Direction.EAST;
        } else if (isIn(yaw, 180, 25) || isIn(yaw, -180, 25)) {
            return Direction.NORTH;
        }else{
			return null;
		}
    }
	private static boolean isIn(float subject, float value, float range) {
		return subject > value - range && subject < value + range;
	}

	public static Direction getVerticalDirectionFromPitch(float pitch) {
        if (pitch >= 0.0F) {
            return Direction.UP;
        } else {
            return Direction.DOWN;
        }
    }
}
