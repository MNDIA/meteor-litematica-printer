package com.kkllffaa.meteor_litematica_printer;

import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import net.minecraft.block.*;
import net.minecraft.block.enums.Attachment;
import net.minecraft.block.enums.BlockFace;
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

        ActionResult result = mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, blockHitResult);

        if (result == ActionResult.SUCCESS) {
            if (swing) mc.player.swingHand(Hand.MAIN_HAND);
            else mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
        }
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
				|| block instanceof RodBlock
				|| block instanceof TrapdoorBlock
				|| block instanceof PillarBlock
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
	 * 获取玩家当前面朝的方向
	 * Get the direction the player is currently facing
	 */
	public static Direction getPlayerFacingDirection(int angleRange) {
		if (mc.player == null) return null;
		
		float yaw = mc.player.getYaw();
		float pitch = mc.player.getPitch();
		
		if (isIn(pitch, 90, angleRange)) {
			return Direction.DOWN;
		} else if (isIn(pitch, -90, angleRange)) {
			return Direction.UP;
		} else if (isIn(pitch, 0, angleRange)) {
			return getHorizontalDirectionFromYaw(yaw, angleRange);
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

	public static Direction getHorizontalDirectionFromYaw(float yaw, int angleRange) {
        yaw = yaw % 360.0F;
        if (yaw > 180.0F) {
            yaw -= 360.0F;
        } else if (yaw < -180.0F) {
            yaw += 360.0F;
        }
        if (isIn(yaw, 90, angleRange)) {
            return Direction.WEST;
        } else if (isIn(yaw, 0, angleRange)) {
            return Direction.SOUTH;
        } else if (isIn(yaw, -90, angleRange)) {
            return Direction.EAST;
        } else if (isIn(yaw, 180, angleRange) || isIn(yaw, -180, angleRange)) {
            return Direction.NORTH;
        }else{
			return null;
		}
    }

	// 保留旧方法的兼容性，使用默认角度范围45
	public static Direction getHorizontalDirectionFromYaw(float yaw) {
		return getHorizontalDirectionFromYaw(yaw, 45);
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

	/**
	 * 计算到达目标状态需要的交互次数
	 * Calculate required interactions to reach target state
	 */
	private static int calculateRequiredInteractions(BlockPos pos, BlockState targetState) {
		if (mc.world == null)
			return 0;

		BlockState currentState = mc.world.getBlockState(pos);
		if (currentState.getBlock() != targetState.getBlock()) {
			return 0;
		}

		Block block = currentState.getBlock();
		if (isNonInteractable(block)) {
			return 0;
		}
		// For note blocks: calculate note difference
		if (block instanceof NoteBlock) {
			if (currentState.contains(Properties.NOTE)) {
				int currentNote = currentState.get(Properties.NOTE);
				int targetNote = targetState.get(Properties.NOTE);

				// Note blocks cycle through 0-24 (25 states)
				int diff = (targetNote - currentNote + 25) % 25;
				return diff;
			}
		}

		// For repeaters: calculate delay difference
		if (block instanceof RepeaterBlock) {
			if (currentState.contains(Properties.DELAY)) {
				int currentDelay = currentState.get(Properties.DELAY);
				int targetDelay = targetState.get(Properties.DELAY);

				// Repeaters cycle through 1-4 (4 states)
				int diff = (targetDelay - currentDelay + 4) % 4;
				return diff;
			}
		}

		// For comparators: calculate mode difference
		if (block instanceof ComparatorBlock) {
			if (currentState.contains(Properties.COMPARATOR_MODE)) {
				return currentState.get(Properties.COMPARATOR_MODE) == targetState.get(Properties.COMPARATOR_MODE) ? 0
						: 1;
			}
		}

		// For daylight detectors: check inverted state
		if (block instanceof DaylightDetectorBlock) {
			if (currentState.contains(Properties.INVERTED)) {
				return currentState.get(Properties.INVERTED) == targetState.get(Properties.INVERTED) ? 0 : 1;
			}
		}

		// For levers: check powered state
		if (block instanceof LeverBlock) {
			if (currentState.contains(Properties.POWERED)) {
				return currentState.get(Properties.POWERED) == targetState.get(Properties.POWERED) ? 0 : 1;
			}
		}

		// For fence gates: check open state
		// For trapdoors: check open state
		// For doors: check open state
		if (block instanceof FenceGateBlock || block instanceof TrapdoorBlock || block instanceof DoorBlock) {
			if (currentState.contains(Properties.OPEN)) {
				return currentState.get(Properties.OPEN) == targetState.get(Properties.OPEN) ? 0 : 1;
			}
		}

		return 0; // 未知类型或不可交互类型
	}

	private static boolean isNonInteractable(Block block) {
		return block == Blocks.IRON_DOOR || block == Blocks.IRON_TRAPDOOR;
	}


	/**
	 * Interact with block to change its state
	 */
	public static boolean interactWithBlock(BlockPos pos) {
		if (mc.player == null || mc.interactionManager == null) return false;
		
		Vec3d hitPos = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
		BlockHitResult blockHitResult = new BlockHitResult(hitPos, Direction.UP, pos, false);
		
		ActionResult result = mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, blockHitResult);
		return result.isAccepted();
	}

	/**
	 * Batch interact with block to reach target state
	 */
	public static boolean batchInteractToTargetState(BlockPos pos, BlockState targetState) {
		int requiredInteractions = calculateRequiredInteractions(pos, targetState);
		
		// Perform all required interactions at once
		for (int i = 0; i < requiredInteractions; i++) {
			if (!interactWithBlock(pos)) {
				return false; // Failed to interact
			}
		}
		
		return true;
	}

	/**
	 * Precise face-based placement that completely ignores player orientation
	 * Places blocks based purely on the required face interaction, like vanilla Minecraft
	 */
	public static boolean precisePlaceByFace(BlockPos blockPos, BlockState targetState, boolean airPlace, boolean swingHand, int range, java.util.List<Block> faceReverseList) {
		if (mc.player == null) return false;
		if (!canPlace(blockPos)) return false;

		// Get the required face for this block state
		Direction requiredFace = getPrecisePlacementFace(targetState);
		
		// Debug: Log placement face for torch, lantern, lever blocks
		Block block = targetState.getBlock();
		if (block == Blocks.TORCH || block == Blocks.REDSTONE_TORCH || 
			block == Blocks.WALL_TORCH || block == Blocks.REDSTONE_WALL_TORCH ||
			block == Blocks.LANTERN || block == Blocks.SOUL_LANTERN ||
			block == Blocks.LEVER) {
			
			// Check what properties this block state actually contains
			String properties = "";
			if (targetState.contains(Properties.FACING)) properties += "FACING=" + targetState.get(Properties.FACING) + " ";
			if (targetState.contains(Properties.HORIZONTAL_FACING)) properties += "HORIZONTAL_FACING=" + targetState.get(Properties.HORIZONTAL_FACING) + " ";
			if (targetState.contains(Properties.BLOCK_FACE)) properties += "BLOCK_FACE=" + targetState.get(Properties.BLOCK_FACE) + " ";
			if (targetState.contains(Properties.ATTACHMENT)) properties += "ATTACHMENT=" + targetState.get(Properties.ATTACHMENT) + " ";
			if (targetState.contains(Properties.HANGING)) properties += "HANGING=" + targetState.get(Properties.HANGING) + " ";
			
			meteordevelopment.meteorclient.utils.player.ChatUtils.info(
				"[DEBUG PLACEMENT] Block=" + block.getTranslationKey().replace("block.minecraft.", "") + 
				" Pos=" + blockPos + 
				" Properties=[" + properties.trim() + "]" +
				" RequiredFace=" + requiredFace +
				" PlayerYaw=" + (mc.player != null ? String.format("%.1f", mc.player.getYaw()) : "null")
			);
		}
		
		// Apply face reversal if block is in the reverse list
		if (faceReverseList != null && faceReverseList.contains(targetState.getBlock())) {
			requiredFace = requiredFace.getOpposite();
		}

		BlockPos neighbor;
		Vec3d hitPos;

		if (airPlace) {
			neighbor = blockPos;
			hitPos = new Vec3d(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5);
		} else {
			neighbor = blockPos.offset(requiredFace.getOpposite());
			if (!canPlaceAgainst(targetState, mc.world.getBlockState(neighbor), requiredFace)) {
				return false; // Cannot place against this block
			}
			hitPos = new Vec3d(
				neighbor.getX() + 0.5 + requiredFace.getOffsetX() * 0.5,
				neighbor.getY() + 0.5 + requiredFace.getOffsetY() * 0.5,
				neighbor.getZ() + 0.5 + requiredFace.getOffsetZ() * 0.5
			);
		}

		// Create precise hit result with correct face
		BlockHitResult blockHitResult = new BlockHitResult(hitPos, requiredFace, neighbor, false);
		
		// Place the block
		place(blockHitResult, swingHand);
		return true;
	}

	/**
	 * Determine the required face for precise placement based on target block state
	 */
	public static Direction getPrecisePlacementFace(BlockState targetState) {

		// For slabs
		if (targetState.contains(Properties.SLAB_TYPE)) {
			SlabType slabType = targetState.get(Properties.SLAB_TYPE);
			switch (slabType) {
				case BOTTOM: return Direction.UP;
				case TOP: return Direction.DOWN;
				case DOUBLE: return Direction.UP;
			}
		}

		// For stairs, trapdoors
		if (targetState.contains(Properties.BLOCK_HALF)) {
			BlockHalf half = targetState.get(Properties.BLOCK_HALF);
			switch (half) {
				case BOTTOM: return Direction.UP;   
				case TOP: return Direction.DOWN;    
			}
		}
	

		if (targetState.contains(Properties.ATTACHMENT)) {
			Attachment attachment = targetState.get(Properties.ATTACHMENT);
			switch (attachment) {
				case FLOOR:
					return Direction.UP;
				case CEILING:
					return Direction.DOWN;
				default:
					break;

			}
		}

		if (targetState.contains(Properties.HANGING)) {
			boolean hanging = targetState.get(Properties.HANGING);
			if (hanging) {
				return Direction.DOWN;
			} else {
				return Direction.UP;
			}
		}


		if (targetState.contains(Properties.BLOCK_FACE)) {
			BlockFace blockFace = targetState.get(Properties.BLOCK_FACE);
			switch (blockFace) {
				case FLOOR: return Direction.UP;
				case CEILING: return Direction.DOWN;
				case WALL: break;
			}
		}

		Direction facing = null;
		if (targetState.contains(Properties.FACING)) {
			facing = targetState.get(Properties.FACING);
		}
		if (facing == null && targetState.contains(Properties.HOPPER_FACING)) {
			facing = targetState.get(Properties.HOPPER_FACING);
		}
		if (facing == null && targetState.contains(Properties.HORIZONTAL_FACING)) {
			facing = targetState.get(Properties.HORIZONTAL_FACING);
		}
		if (facing == null && targetState.contains(Properties.VERTICAL_DIRECTION)) {
			facing = targetState.get(Properties.VERTICAL_DIRECTION);
		}
		if (facing != null) {
			return facing.getOpposite();
		}

		Axis axis = null;
		if (targetState.contains(Properties.AXIS)) {
			axis = targetState.get(Properties.AXIS);
		}
		if (axis == null && targetState.contains(Properties.HORIZONTAL_AXIS)) {
			axis = targetState.get(Properties.HORIZONTAL_AXIS);
		}
		if (axis != null) {
			switch (axis) {
				case X: return Direction.EAST;
				case Y: return Direction.UP;
				case Z: return Direction.SOUTH;
			}
		}
		return Direction.UP;
	}

}
