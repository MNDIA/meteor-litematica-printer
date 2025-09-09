package com.kkllffaa.meteor_litematica_printer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Supplier;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.BlockListSetting;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockIterator;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.BedPart;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.block.enums.SlabType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

public class Printer extends Module {
	private final SettingGroup sgGeneral = settings.getDefaultGroup();
	private final SettingGroup sgWhitelist = settings.createGroup("Whitelist");
	private final SettingGroup sgDirectional = settings.createGroup("Directional Protection, make sure blocks based on players direction are placed in the correct direction");
	private final SettingGroup sgClickFace = settings.createGroup("Make sure the blocks based on the click face are placed in the correct direction");
	private final SettingGroup sgBlockState = settings.createGroup("Blocks that need state interaction (repeaters, comparators, note blocks, levers, trapdoors, doors, fence gates, etc.)");
	private final SettingGroup sgCache = settings.createGroup("Prevent repeated placement of the same block in a short period of time");
    private final SettingGroup sgRendering = settings.createGroup("Rendering");

	private final Setting<Integer> printing_range = sgGeneral.add(new IntSetting.Builder()
			.name("printing-range")
			.description("The block place range.")
			.defaultValue(2)
			.min(1).sliderMin(1)
			.max(6).sliderMax(6)
			.build()
	);

	private final Setting<Integer> printing_delay = sgGeneral.add(new IntSetting.Builder()
			.name("printing-delay")
			.description("Delay between printing blocks in ticks.")
			.defaultValue(2)
			.min(0).sliderMin(0)
			.max(100).sliderMax(40)
			.build()
	);

	private final Setting<Integer> bpt = sgGeneral.add(new IntSetting.Builder()
			.name("blocks/tick")
			.description("How many blocks place per tick.")
			.defaultValue(1)
			.min(1).sliderMin(1)
			.max(100).sliderMax(100)
			.build()
	);

	private final Setting<Boolean> airPlace = sgGeneral.add(new BoolSetting.Builder()
			.name("air-place")
			.description("Allow the bot to place in the air.")
			.defaultValue(true)
			.build()
	);

	private final Setting<Boolean> placeThroughWall = sgGeneral.add(new BoolSetting.Builder()
			.name("Place Through Wall")
			.description("Allow the bot to place through walls.")
			.defaultValue(true)
			.build()
	);

	private final Setting<Boolean> swing = sgGeneral.add(new BoolSetting.Builder()
			.name("swing")
			.description("Swing hand when placing.")
			.defaultValue(false)
			.build()
	);

    private final Setting<Boolean> returnHand = sgGeneral.add(new BoolSetting.Builder()
			.name("return-slot")
			.description("Return to old slot.")
			.defaultValue(false)
			.build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
			.name("rotate")
			.description("Rotate to the blocks being placed.")
			.defaultValue(false)
			.build()
    );

    private final Setting<Boolean> clientSide = sgGeneral.add(new BoolSetting.Builder()
			.name("Client side Rotation")
			.description("Rotate to the blocks being placed on client side.")
			.defaultValue(false)
			.visible(rotate::get)
			.build()
    );

    private final Setting<Boolean> precisePlacement = sgClickFace.add(new BoolSetting.Builder()
			.name("precise-placement")
			.description("Use precise face-based placement for stairs, slabs, trapdoors etc. (ignores player orientation completely)")
			.defaultValue(true)
			.build()
    );

	private final Setting<Boolean> dirtgrass = sgGeneral.add(new BoolSetting.Builder()
			.name("dirt-as-grass")
			.description("Use dirt instead of grass.")
			.defaultValue(false)
			.build()
	);

	private final Setting<Boolean> enableCache = sgCache.add(new BoolSetting.Builder()
			.name("enable-cache")
			.description("Enable position cache to prevent placing at the same position multiple times.")
			.defaultValue(true)
			.build()
	);

	private final Setting<Integer> cacheSize = sgCache.add(new IntSetting.Builder()
			.name("cache-size")
			.description("Number of recent positions to cache.")
			.defaultValue(50)
			.min(10).sliderMin(10)
			.max(200).sliderMax(200)
			.visible(enableCache::get)
			.build()
	);

	private final Setting<Integer> cacheCleanupInterval = sgCache.add(new IntSetting.Builder()
			.name("cache-cleanup-interval")
			.description("Time in seconds between cache cleanups to prevent stale entries.")
			.defaultValue(3)
			.min(1).sliderMin(1)
			.max(10).sliderMax(10)
			.visible(enableCache::get)
			.build()
	);

	// Directional Protection Settings
	private final Setting<Boolean> directionProtection = sgDirectional.add(new BoolSetting.Builder()
			.name("direction-protection")
			.description("Only place directional blocks when player is facing the correct direction.")
			.defaultValue(true)
			.build()
	);

	private final Setting<Integer> angleRange = sgDirectional.add(new IntSetting.Builder()
			.name("angle-range")
			.description("Angle range for direction detection (degrees).")
			.defaultValue(25)
			.min(1).sliderMin(1)
			.max(45).sliderMax(45)
			.visible(directionProtection::get)
			.build()
	);

	// Blocks that face the same direction as player (Forward)
	private final Setting<List<Block>> facingForward = sgDirectional.add(new BlockListSetting.Builder()
			.name("facing-forward")
			.description("Blocks that should face the same direction as player (e.g., Observer, Piston).")
			.visible(directionProtection::get)
			.build()
	);

	// Blocks that face away from player (Backward)
	private final Setting<List<Block>> facingBackward = sgDirectional.add(new BlockListSetting.Builder()
			.name("facing-backward")
			.description("Blocks that should face away from player (e.g., Furnace, Chest).")
			.visible(directionProtection::get)
			.build()
	);

	// Blocks that face to the left of player
	private final Setting<List<Block>> facingLeft = sgDirectional.add(new BlockListSetting.Builder()
			.name("facing-left")
			.description("Blocks that should face to the left of player.")
			.visible(directionProtection::get)
			.build()
	);

	// Blocks that face to the right of player
	private final Setting<List<Block>> facingRight = sgDirectional.add(new BlockListSetting.Builder()
			.name("facing-right")
			.description("Blocks that should face to the right of player.")
			.visible(directionProtection::get)
			.build()
	);

	// Blocks that face upward from player
	private final Setting<List<Block>> facingUp = sgDirectional.add(new BlockListSetting.Builder()
			.name("facing-up")
			.description("Blocks that should face upward from player.")
			.visible(directionProtection::get)
			.build()
	);

	// Blocks that face downward from player
	private final Setting<List<Block>> facingDown = sgDirectional.add(new BlockListSetting.Builder()
			.name("facing-down")
			.description("Blocks that should face downward from player.")
			.visible(directionProtection::get)
			.build()
	);

	// Blocks that need state interaction (repeaters, comparators, note blocks, etc.)
	private final Setting<List<Block>> stateBlocks = sgBlockState.add(new BlockListSetting.Builder()
			.name("state-blocks")
			.description("Blocks that need interaction to adjust their state (repeaters, comparators, note blocks, levers, trapdoors, doors, fence gates, etc.).")
			.build()
	);

    private final Setting<SortAlgorithm> firstAlgorithm = sgGeneral.add(new EnumSetting.Builder<SortAlgorithm>()
			.name("first-sorting-mode")
			.description("The blocks you want to place first.")
			.defaultValue(SortAlgorithm.None)
			.build()
	);

    private final Setting<SortingSecond> secondAlgorithm = sgGeneral.add(new EnumSetting.Builder<SortingSecond>()
			.name("second-sorting-mode")
			.description("Second pass of sorting eg. place first blocks higher and closest to you.")
			.defaultValue(SortingSecond.None)
			.visible(()-> firstAlgorithm.get().applySecondSorting)
			.build()
	);

    private final Setting<Boolean> whitelistenabled = sgWhitelist.add(new BoolSetting.Builder()
			.name("whitelist-enabled")
			.description("Only place selected blocks.")
			.defaultValue(false)
			.build()
	);

	// TODO: Add blacklist option
    private final Setting<List<Block>> whitelist = sgWhitelist.add(new BlockListSetting.Builder()
			.name("whitelist")
			.description("Blocks to place.")
			.visible(whitelistenabled::get)
			.build()
	);

    private final Setting<Boolean> renderBlocks = sgRendering.add(new BoolSetting.Builder()
        .name("render-placed-blocks")
        .description("Renders block placements.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> fadeTime = sgRendering.add(new IntSetting.Builder()
        .name("fade-time")
        .description("Time for the rendering to fade, in ticks.")
        .defaultValue(3)
        .min(1).sliderMin(1)
        .max(1000).sliderMax(20)
        .visible(renderBlocks::get)
        .build()
    );

    private final Setting<SettingColor> colour = sgRendering.add(new ColorSetting.Builder()
        .name("colour")
        .description("The cubes colour.")
        .defaultValue(new SettingColor(95, 190, 255))
        .visible(renderBlocks::get)
        .build()
    );

    private int timer;
    private int usedSlot = -1;
    private final List<BlockPos> toSort = new ArrayList<>();
    private final List<Pair<Integer, BlockPos>> placed_fade = new ArrayList<>();
    
    // Position cache to prevent repeated placement attempts
    private final LinkedHashSet<BlockPos> positionCache = new LinkedHashSet<>();
    private int cacheCleanupTickTimer = 0;


	// TODO: Add an option for smooth rotation. Make it look legit.
	// Might use liquidbounce RotationUtils to make it happen.
	// https://github.com/CCBlueX/LiquidBounce/blob/nextgen/src/main/kotlin/net/ccbluex/liquidbounce/utils/aiming/RotationsUtil.kt#L257

	public Printer() {
		super(Addon.CATEGORY, "litematica-printer", "Automatically prints open schematics");
	}

    @Override
    public void onActivate() {
        onDeactivate();
    }

	@Override
    public void onDeactivate() {
		placed_fade.clear();
		positionCache.clear();
		cacheCleanupTickTimer = 0;
	}

	@EventHandler
	private void onTick(TickEvent.Post event) {
		if (mc.player == null || mc.world == null) {
			placed_fade.clear();
			return;
		}

		placed_fade.forEach(s -> s.setLeft(s.getLeft() - 1));
		placed_fade.removeIf(s -> s.getLeft() <= 0);

		// Cache cleanup timer - clears cache periodically to prevent stale entries
		if (enableCache.get()) {
			cacheCleanupTickTimer++;

			if (cacheCleanupTickTimer >= cacheCleanupInterval.get() * 20) {
				positionCache.clear();
				cacheCleanupTickTimer = 0;
			}
		}

		WorldSchematic worldSchematic = SchematicWorldHandler.getSchematicWorld();
		if (worldSchematic == null) {
			placed_fade.clear();
			toggle();
			return;
		}

		toSort.clear();


		if (timer >= printing_delay.get()) {
			BlockIterator.register(printing_range.get() + 1, printing_range.get() + 1, (pos, blockState) -> {
				BlockState required = worldSchematic.getBlockState(pos);

				if (
						mc.player.getBlockPos().isWithinDistance(pos, printing_range.get())
						&& blockState.isReplaceable()
						&& required.getFluidState().isEmpty()
						&& !required.isAir()
						&& blockState.getBlock() != required.getBlock()
						&& DataManager.getRenderLayerRange().isPositionWithinRange(pos)
						&& !mc.player.getBoundingBox().intersects(Vec3d.of(pos), Vec3d.of(pos).add(1, 1, 1))
						&& required.canPlaceAt(mc.world, pos)
					) {
					boolean isBlockInLineOfSight = MyUtils.isBlockInLineOfSight(pos, required);
			    	// Advanced mode disabled - all orientation variables removed

					// Simplified placement conditions - no advanced logic
					if(
						airPlace.get()
						|| !airPlace.get() && (placeThroughWall.get() ? BlockUtils.getPlaceSide(pos) != null : isBlockInLineOfSight)
					) {
						if (!whitelistenabled.get() || whitelist.get().contains(required.getBlock())) {
							boolean shouldPlace = true;
							// Multi-structure block protection: check bed parts and double-height blocks
							if (shouldPlace) {
								shouldPlace = isMultiStructurePlacementAllowed(required);
							}
							// Check if position is in cache (recently attempted)
							if (isPositionCached(pos)) {
								shouldPlace = false;
							}
							
							// Direction protection: check if directional block's facing matches player direction
							if (shouldPlace && directionProtection.get()) {
								Direction requiredDirection = dir(required);
								if (requiredDirection !=null ){
									Direction playerDirection = MyUtils.getPlayerFacingDirection(angleRange.get());
									if (playerDirection == null) {
										shouldPlace = false;
									} else {
										shouldPlace = isDirectionalPlacementAllowed(required.getBlock(), requiredDirection, playerDirection);
									}
								}
							}
							
							if (shouldPlace) {
								toSort.add(new BlockPos(pos));
							}
						}
					}
				}
			});

			BlockIterator.after(() -> {
				//if (!tosort.isEmpty()) info(tosort.toString());

				if (firstAlgorithm.get() != SortAlgorithm.None) {
					if (firstAlgorithm.get().applySecondSorting) {
						if (secondAlgorithm.get() != SortingSecond.None) {
							toSort.sort(secondAlgorithm.get().algorithm);
						}
					}
					toSort.sort(firstAlgorithm.get().algorithm);
				}


				int placed = 0;
				for (BlockPos pos : toSort) {

					BlockState state = worldSchematic.getBlockState(pos);
					Item item = state.getBlock().asItem();

					if (dirtgrass.get() && item == Items.GRASS_BLOCK)
						item = Items.DIRT;
					if (switchItem(item, state, () -> place(state, pos))) {
						timer = 0;
						placed++;
						
						// Add position to cache after successful placement
						addToCache(pos);
						
						// 检查是否需要状态交互
						if (stateBlocks.get().contains(state.getBlock())) {
							mc.execute(() -> {
								if (!MyUtils.batchInteractToTargetState(pos, state)) {
									warning("Failed to interact with block to set correct state at " + pos);
								}
							});
						}
						
						if (renderBlocks.get()) {
							placed_fade.add(new Pair<>(fadeTime.get(), new BlockPos(pos)));
						}
						if (placed >= bpt.get()) {
							return;
						}
					}
				}
			});


		} else timer++;
	}

	public boolean place(BlockState required, BlockPos pos) {

		if (mc.player == null || mc.world == null) return false;
		if (!mc.world.getBlockState(pos).isReplaceable()) return false;

        if (precisePlacement.get()) {
            return MyUtils.precisePlaceByFace(pos, required, airPlace.get(), swing.get(), printing_range.get());
        } else {
    		// Legacy mode - disabled advanced and rotate features
    		Direction wantedSide = null; // disabled: dir(required)
        	SlabType wantedSlabType = null; // disabled: required.contains(Properties.SLAB_TYPE) ? required.get(Properties.SLAB_TYPE) : null;
        	BlockHalf wantedBlockHalf = null; // disabled: required.contains(Properties.BLOCK_HALF) ? required.get(Properties.BLOCK_HALF) : null;
        	Direction wantedHorizontalOrientation = null; // disabled: required.contains(Properties.HORIZONTAL_FACING) ? required.get(Properties.HORIZONTAL_FACING) : null;
        	Axis wantedAxies = null; // disabled: required.contains(Properties.AXIS) ? required.get(Properties.AXIS) : null;
        	Direction wantedHopperOrientation = null; // disabled: required.contains(Properties.HOPPER_FACING) ? required.get(Properties.HOPPER_FACING) : null;
        	
        	Direction placeSide = placeThroughWall.get() ?
        						MyUtils.getPlaceSide(
        								pos,
        								required,
        								wantedSlabType, 
        								wantedBlockHalf,
        								wantedHorizontalOrientation != null ? wantedHorizontalOrientation : wantedHopperOrientation,
        								wantedAxies,
        								wantedSide)
        						: MyUtils.getVisiblePlaceSide(
        								pos,
        								required,
        								wantedSlabType, 
        								wantedBlockHalf,
        								wantedHorizontalOrientation != null ? wantedHorizontalOrientation : wantedHopperOrientation,
        								wantedAxies,
        								printing_range.get(),
        								wantedSide
    							);

            return MyUtils.place(pos, placeSide, wantedSlabType, wantedBlockHalf, wantedHorizontalOrientation != null ? wantedHorizontalOrientation : wantedHopperOrientation, wantedAxies, airPlace.get(), swing.get(), false, false, printing_range.get());
        }
	}

	private boolean switchItem(Item item, BlockState state, Supplier<Boolean> action) {
		if (mc.player == null) return false;

		int selectedSlot = mc.player.getInventory().getSelectedSlot();
		boolean isCreative = mc.player.getAbilities().creativeMode;
		FindItemResult result = InvUtils.find(item);


		// TODO: Check if ItemStack nbt has BlockStateTag == BlockState required when in creative
		// TODO: Fix check nbt
		// TODO: Fix not acquiring blocks in creative mode

		if (
			mc.player.getMainHandStack().getItem() == item
		) {
			if (action.get()) {
				usedSlot = mc.player.getInventory().getSelectedSlot();
				return true;
			} else return false;

		} else if (
			usedSlot != -1 &&
			mc.player.getInventory().getStack(usedSlot).getItem() == item
		) {
			InvUtils.swap(usedSlot, returnHand.get());
			if (action.get()) {
				return true;
			} else {
				InvUtils.swap(selectedSlot, returnHand.get());
				return false;
			}

		} else if (
			result.found()
		) {
			if (result.isHotbar()) {
				InvUtils.swap(result.slot(), returnHand.get());

				if (action.get()) {
					usedSlot = mc.player.getInventory().getSelectedSlot();
					return true;
				} else {
					InvUtils.swap(selectedSlot, returnHand.get());
					return false;
				}

			} else if (result.isMain()) {
				FindItemResult empty = InvUtils.findEmpty();

				if (empty.found() && empty.isHotbar()) {
					InvUtils.move().from(result.slot()).toHotbar(empty.slot());
					InvUtils.swap(empty.slot(), returnHand.get());

					if (action.get()) {
						usedSlot = mc.player.getInventory().getSelectedSlot();
						return true;
					} else {
						InvUtils.swap(selectedSlot, returnHand.get());
						return false;
					}

				} else if (usedSlot != -1) {
					InvUtils.move().from(result.slot()).toHotbar(usedSlot);
					InvUtils.swap(usedSlot, returnHand.get());

					if (action.get()) {
						return true;
					} else {
						InvUtils.swap(selectedSlot, returnHand.get());
						return false;
					}

				} else return false;
			} else return false;
		} else if (isCreative) {
			int slot = 0;
            FindItemResult fir = InvUtils.find(ItemStack::isEmpty, 0, 8);
            if (fir.found()) {
                slot = fir.slot();
            }
			mc.getNetworkHandler().sendPacket(new CreativeInventoryActionC2SPacket(36 + slot, item.getDefaultStack()));
			InvUtils.swap(slot, returnHand.get());
            return true;
		} else return false;
	}

	private Direction dir(BlockState state) {
		if (state.contains(Properties.FACING)) return state.get(Properties.FACING);
		else if (state.contains(Properties.AXIS)) return Direction.from(state.get(Properties.AXIS), Direction.AxisDirection.POSITIVE);
		else if (state.contains(Properties.HORIZONTAL_FACING)) return state.get(Properties.HORIZONTAL_FACING);
		else if (state.contains(Properties.HORIZONTAL_AXIS)) return Direction.from(state.get(Properties.HORIZONTAL_AXIS), Direction.AxisDirection.POSITIVE);
		else if (state.contains(Properties.STRAIGHT_RAIL_SHAPE)) return railShapeToDirection(state.get(Properties.STRAIGHT_RAIL_SHAPE));
		else return null; // Return null for blocks without directional properties
	}

	/**
	 * Convert RailShape to Direction for directional placement logic
	 */
	private Direction railShapeToDirection(net.minecraft.block.enums.RailShape shape) {
		switch (shape) {
			case NORTH_SOUTH:
				return Direction.SOUTH;
			case EAST_WEST:
				return Direction.EAST;
			case ASCENDING_EAST:
				return Direction.EAST;
			case ASCENDING_WEST:
				return Direction.EAST;
			case ASCENDING_NORTH:
				return Direction.SOUTH;
			case ASCENDING_SOUTH:
				return Direction.SOUTH;
			default:
				// For corner rails
				return null;
		}
	}

	/**
	 * Check if directional block placement is allowed based on configuration
	 */
	private boolean[] inList = new boolean[6];
	private boolean isDirectionalPlacementAllowed(Block block, Direction requiredDirection, Direction playerDirection) {
		// Check each directional list to see if this block is configured
		inList[0] = facingForward.get().contains(block);
		inList[1] = facingBackward.get().contains(block);
		inList[2] = facingLeft.get().contains(block);
		inList[3] = facingRight.get().contains(block);
		inList[4] = facingUp.get().contains(block);
		inList[5] = facingDown.get().contains(block);
		if (inList[0] || inList[1] || inList[2] || inList[3] || inList[4] || inList[5]) {
			if (inList[0]) {
				// Block should face same direction as player
				if (requiredDirection.equals(playerDirection)) {
					return true;
				}
			}
			if (inList[1]) {
				// Block should face away from player
				if (requiredDirection.equals(playerDirection.getOpposite())) {
					return true;
				}
			}
			if (inList[2]) {
				// Block should face to the left of player
				Direction leftDirection = getLeftDirection(playerDirection);
				if (leftDirection != null && requiredDirection.equals(leftDirection)) {
					return true;
				}
			}
			if (inList[3]) {
				// Block should face to the right of player
				Direction rightDirection = getRightDirection(playerDirection);
				if (rightDirection != null && requiredDirection.equals(rightDirection)) {
					return true;
				}
			}
			if (inList[4]) {
				// Block should face upward relative to player
				Direction upDirection = getUpDirection(playerDirection);
				if (upDirection != null && requiredDirection.equals(upDirection)) {
					return true;
				}
			}
			if (inList[5]) {
				// Block should face downward relative to player
				Direction downDirection = getDownDirection(playerDirection);
				if (downDirection != null && requiredDirection.equals(downDirection)) {
					return true;
				}
			}
			return false;
		} else {
			// Block is not in any directional list, allow placement
			return true;
		}
	}

	/**
	 * Get the direction to the left of the given direction
	 */
	private Direction getLeftDirection(Direction direction) {
		switch (direction) {
			case NORTH: return Direction.WEST;
			case EAST: return Direction.NORTH;
			case SOUTH: return Direction.EAST;
			case WEST: return Direction.SOUTH;
			default: return null; // No left for up/down
		}
	}

	/**
	 * Get the direction to the right of the given direction
	 */
	private Direction getRightDirection(Direction direction) {
		switch (direction) {
			case NORTH: return Direction.EAST;
			case EAST: return Direction.SOUTH;
			case SOUTH: return Direction.WEST;
			case WEST: return Direction.NORTH;
			default: return null; // No right for up/down
		}
	}

	/**
	 * Get the upward direction relative to player facing
	 */
	private Direction getUpDirection(Direction playerDirection) {
		if (playerDirection == Direction.UP) {
			Direction horizontal = MyUtils.getHorizontalDirectionFromYaw(mc.player.getYaw(), angleRange.get());
			return horizontal != null ? horizontal.getOpposite() : Direction.UP;
		} else if (playerDirection == Direction.DOWN) {
			return MyUtils.getHorizontalDirectionFromYaw(mc.player.getYaw(), angleRange.get());
		} else if (playerDirection != null) {
			// Player is looking horizontally, so "up" is simply UP
			return Direction.UP;
		}else {
			return null;
		}
	}
	/**
	 * Get the downward direction relative to player facing
	 */
	private Direction getDownDirection(Direction playerDirection) {
		if (playerDirection == Direction.UP) {
			return MyUtils.getHorizontalDirectionFromYaw(mc.player.getYaw(), angleRange.get());
		} else if (playerDirection == Direction.DOWN) {
			Direction horizontal = MyUtils.getHorizontalDirectionFromYaw(mc.player.getYaw(), angleRange.get());
			return horizontal != null ? horizontal.getOpposite() : Direction.DOWN;
		} else if (playerDirection != null) {
			// Player is looking horizontally, so "down" is simply DOWN
			return Direction.DOWN;
		} else {
			return null;
		}
	}
	/**
	 * Check if position is in cache (recently attempted)
	 */
	private boolean isPositionCached(BlockPos pos) {
		return enableCache.get() && positionCache.contains(pos);
	}

	/**
	 * Add position to cache and manage cache size
	 */
	private void addToCache(BlockPos pos) {
		if (!enableCache.get()) return;
		
		positionCache.add(pos);
		
		// Remove oldest entries if cache exceeds limit
		while (positionCache.size() > cacheSize.get()) {
			var iterator = positionCache.iterator();
			if (iterator.hasNext()) {
				iterator.next();
				iterator.remove();
			}
		}
	}



	@EventHandler
	private void onRender(Render3DEvent event) {
		placed_fade.forEach(s -> {
			Color a = new Color(colour.get().r, colour.get().g, colour.get().b, (int) (((float)s.getLeft() / (float) fadeTime.get()) * colour.get().a));
			event.renderer.box(s.getRight(), a, null, ShapeMode.Sides, 0);
		});
	}

	/**
	 * Check if multi-structure block placement is allowed based on BED_PART and DOUBLE_BLOCK_HALF
	 * Returns false for certain parts to prevent improper placement
	 */
	private boolean isMultiStructurePlacementAllowed(BlockState required) {
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

	@SuppressWarnings("unused")
	public enum SortAlgorithm {
		None(false, (a, b) -> 0),
		TopDown(true, Comparator.comparingInt(value -> value.getY() * -1)),
		DownTop(true, Comparator.comparingInt(Vec3i::getY)),
		Nearest(false, Comparator.comparingDouble(value -> MeteorClient.mc.player != null ? Utils.squaredDistance(MeteorClient.mc.player.getX(), MeteorClient.mc.player.getY(), MeteorClient.mc.player.getZ(), value.getX() + 0.5, value.getY() + 0.5, value.getZ() + 0.5) : 0)),
		Furthest(false, Comparator.comparingDouble(value -> MeteorClient.mc.player != null ? (Utils.squaredDistance(MeteorClient.mc.player.getX(), MeteorClient.mc.player.getY(), MeteorClient.mc.player.getZ(), value.getX() + 0.5, value.getY() + 0.5, value.getZ() + 0.5)) * -1 : 0));


		final boolean applySecondSorting;
		final Comparator<BlockPos> algorithm;

		SortAlgorithm(boolean applySecondSorting, Comparator<BlockPos> algorithm) {
			this.applySecondSorting = applySecondSorting;
			this.algorithm = algorithm;
		}
	}

	@SuppressWarnings("unused")
	public enum SortingSecond {
		None(SortAlgorithm.None.algorithm),
		Nearest(SortAlgorithm.Nearest.algorithm),
		Furthest(SortAlgorithm.Furthest.algorithm);

		final Comparator<BlockPos> algorithm;

		SortingSecond(Comparator<BlockPos> algorithm) {
			this.algorithm = algorithm;
		}
	}
}
