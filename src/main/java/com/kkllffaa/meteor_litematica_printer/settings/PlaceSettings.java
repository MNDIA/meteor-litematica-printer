package com.kkllffaa.meteor_litematica_printer.settings;

import meteordevelopment.meteorclient.settings.BlockListSetting;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.Rotations;

import net.minecraft.block.*;
import net.minecraft.block.enums.SlabType;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import javax.annotation.Nullable;

import com.kkllffaa.meteor_litematica_printer.Addon;
import com.kkllffaa.meteor_litematica_printer.MyUtils;
import com.kkllffaa.meteor_litematica_printer.Rotation;
import com.kkllffaa.meteor_litematica_printer.MyUtils.SafetyFaceMode;

import static meteordevelopment.meteorclient.utils.world.BlockUtils.canPlace;
import static meteordevelopment.meteorclient.utils.world.BlockUtils.isClickable;
import static com.kkllffaa.meteor_litematica_printer.MyUtils.*;



public class PlaceSettings extends Module {
	public PlaceSettings() {
		super(Addon.SETTINGSCATEGORY, "PlaceSettings", "Module to configure settings.");
	}

	private final SettingGroup sgGeneral = settings.getDefaultGroup();
	private final SettingGroup sgDirectional = settings.createGroup("Directional Protection");
	private final SettingGroup sgClickFace = settings.createGroup("Click Face");
	private final SettingGroup sgNeighbor = settings.createGroup("Place On Neighbor Blocks");

	private final Setting<Boolean> enableBlacklist = sgNeighbor.add(new BoolSetting.Builder()
			.name("enable-blacklist")
			.description("Enable blacklist for neighbor blocks.")
			.defaultValue(false)
			.build());

	private final Setting<List<Block>> blacklist = sgNeighbor.add(new BlockListSetting.Builder()
			.name("blacklist")
			.description("Blocks that cannot be placed against.(Click Face Center Pos is Air)")
			.defaultValue(
				//面中心没有体积的砖
					Blocks.SCAFFOLDING,
					Blocks.WHITE_SHULKER_BOX, Blocks.ORANGE_SHULKER_BOX, Blocks.MAGENTA_SHULKER_BOX,
					Blocks.LIGHT_BLUE_SHULKER_BOX, Blocks.YELLOW_SHULKER_BOX, Blocks.LIME_SHULKER_BOX,
					Blocks.PINK_SHULKER_BOX, Blocks.GRAY_SHULKER_BOX, Blocks.LIGHT_GRAY_SHULKER_BOX,
					Blocks.CYAN_SHULKER_BOX, Blocks.PURPLE_SHULKER_BOX, Blocks.BLUE_SHULKER_BOX,
					Blocks.BROWN_SHULKER_BOX, Blocks.GREEN_SHULKER_BOX, Blocks.RED_SHULKER_BOX,
					Blocks.BLACK_SHULKER_BOX,
					Blocks.POINTED_DRIPSTONE,
					Blocks.AMETHYST_CLUSTER
			)
			.visible(enableBlacklist::get)
			.build());

	private final Setting<Boolean> enableAddList = sgNeighbor.add(new BoolSetting.Builder()
			.name("enable-additional-list")
			.description("Enable additional list for neighbor blocks.")
			.defaultValue(false)
			.build());

	private final Setting<List<Block>> addList = sgNeighbor.add(new BlockListSetting.Builder()
			.name("additional-list")
			.description("Additional blocks allowed after collision box filtering.")
			.visible(enableAddList::get)
			.build());

	private final Setting<Double> placeRange = sgGeneral.add(new DoubleSetting.Builder()
			.name("place-range")
			.description("The block place range.")
			.defaultValue(5.5)
			.min(1).sliderMin(1)
			.max(7).sliderMax(7)
			.build()
	);

	private final Setting<Boolean> airPlace = sgGeneral.add(new BoolSetting.Builder()
			.name("air-place")
			.description("Allow the bot to place in the air.")
			.defaultValue(true)
			.build());

	private final Setting<List<Block>> airplaceBlacklist = sgGeneral.add(new BlockListSetting.Builder()
			.name("airplace-blacklist")
			.description("Blocks that cannot be placed in airplace.")
			.defaultValue(Blocks.GRINDSTONE)
			.visible(airPlace::get)
			.build());
	private final Setting<Boolean> placeThroughWall = sgGeneral.add(new BoolSetting.Builder()
			.name("Place Through Wall")
			.description("Allow the bot to place through walls.")
			.defaultValue(true)
			.build());

	private final Setting<SafetyFaceMode> safetyPlaceFaceMode = sgGeneral.add(new EnumSetting.Builder<SafetyFaceMode>()
			.name("direction-mode")
			.description("Only place blocks on A safe faces.")
			.defaultValue(SafetyFaceMode.None)
			.build());

	private final Setting<Boolean> onlyPlaceOnLookFace = sgGeneral.add(new BoolSetting.Builder()
			.name("only-place-on-look-face")
			.description("Only place blocks on the face you are looking at direction")
			.defaultValue(false)
			.build());

	private final Setting<Boolean> swing = sgGeneral.add(new BoolSetting.Builder()
			.name("swing")
			.description("Swing hand when placing.")
			.defaultValue(false)
			.build());

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

    private final Setting<Boolean> returnHand = sgGeneral.add(new BoolSetting.Builder()
			.name("return-slot")
			.description("Return to old slot.")
			.defaultValue(false)
			.build()
    );

	private final Setting<Boolean> dirtgrass = sgGeneral.add(new BoolSetting.Builder()
			.name("dirt-as-grass")
			.description("Use dirt instead of grass.")
			.defaultValue(false)
			.build()
	);













	// Directional Protection Settings
	private final Setting<Boolean> directionProtection = sgDirectional.add(new BoolSetting.Builder()
			.name("direction-protection")
			.description("Only place directional blocks when player is facing the correct direction.")
			.defaultValue(true)
			.build());

	private final Setting<Integer> angleRangeForDirectionProtection = sgDirectional.add(new IntSetting.Builder()
			.name("angle-range")
			.description("Angle range for direction detection (degrees).")
			.defaultValue(25)
			.min(1).sliderMin(1)
			.max(45).sliderMax(45)
			.visible(directionProtection::get)
			.build());

	// Blocks that face the same direction as player (Forward)
	private final Setting<List<Block>> dirForward = sgDirectional.add(new BlockListSetting.Builder()
			.name("facing-forward")
			.description("Blocks that should face the same direction as player.")
			.defaultValue(
					// 侦测器、钟
					Blocks.OBSERVER, Blocks.BELL,
					// 砂轮
					Blocks.GRINDSTONE,
					// 拉杆
					Blocks.LEVER,

					// 按钮
					Blocks.STONE_BUTTON, Blocks.OAK_BUTTON, Blocks.SPRUCE_BUTTON, Blocks.BIRCH_BUTTON,
					Blocks.JUNGLE_BUTTON, Blocks.ACACIA_BUTTON, Blocks.DARK_OAK_BUTTON, Blocks.CRIMSON_BUTTON,
					Blocks.WARPED_BUTTON, Blocks.MANGROVE_BUTTON, Blocks.BAMBOO_BUTTON, Blocks.CHERRY_BUTTON,
					Blocks.POLISHED_BLACKSTONE_BUTTON,
					// 铁轨
					Blocks.RAIL, Blocks.POWERED_RAIL, Blocks.DETECTOR_RAIL, Blocks.ACTIVATOR_RAIL,
					// 楼梯
					Blocks.OAK_STAIRS, Blocks.SPRUCE_STAIRS, Blocks.BIRCH_STAIRS, Blocks.JUNGLE_STAIRS,
					Blocks.ACACIA_STAIRS, Blocks.DARK_OAK_STAIRS, Blocks.STONE_STAIRS, Blocks.COBBLESTONE_STAIRS,
					Blocks.BRICK_STAIRS, Blocks.STONE_BRICK_STAIRS, Blocks.NETHER_BRICK_STAIRS, Blocks.SANDSTONE_STAIRS,
					Blocks.QUARTZ_STAIRS, Blocks.RED_SANDSTONE_STAIRS, Blocks.PURPUR_STAIRS, Blocks.PRISMARINE_STAIRS,
					Blocks.PRISMARINE_BRICK_STAIRS, Blocks.DARK_PRISMARINE_STAIRS, Blocks.GRANITE_STAIRS,
					Blocks.DIORITE_STAIRS, Blocks.ANDESITE_STAIRS, Blocks.POLISHED_GRANITE_STAIRS,
					Blocks.POLISHED_DIORITE_STAIRS, Blocks.POLISHED_ANDESITE_STAIRS, Blocks.MOSSY_STONE_BRICK_STAIRS,
					Blocks.MOSSY_COBBLESTONE_STAIRS, Blocks.SMOOTH_SANDSTONE_STAIRS, Blocks.SMOOTH_RED_SANDSTONE_STAIRS,
					Blocks.SMOOTH_QUARTZ_STAIRS, Blocks.END_STONE_BRICK_STAIRS, Blocks.BLACKSTONE_STAIRS,
					Blocks.POLISHED_BLACKSTONE_STAIRS, Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, Blocks.CRIMSON_STAIRS,
					Blocks.WARPED_STAIRS, Blocks.MANGROVE_STAIRS, Blocks.BAMBOO_STAIRS, Blocks.BAMBOO_MOSAIC_STAIRS,
					Blocks.CHERRY_STAIRS, Blocks.COBBLED_DEEPSLATE_STAIRS, Blocks.POLISHED_DEEPSLATE_STAIRS,
					Blocks.DEEPSLATE_BRICK_STAIRS, Blocks.DEEPSLATE_TILE_STAIRS,
					Blocks.PALE_OAK_STAIRS, Blocks.RED_NETHER_BRICK_STAIRS, Blocks.RESIN_BRICK_STAIRS, Blocks.MUD_BRICK_STAIRS,
					Blocks.TUFF_BRICK_STAIRS, Blocks.POLISHED_TUFF_STAIRS, Blocks.TUFF_STAIRS,
					// 栅栏门
					Blocks.OAK_FENCE_GATE, Blocks.SPRUCE_FENCE_GATE, Blocks.BIRCH_FENCE_GATE, Blocks.JUNGLE_FENCE_GATE,
					Blocks.ACACIA_FENCE_GATE, Blocks.DARK_OAK_FENCE_GATE, Blocks.CRIMSON_FENCE_GATE, Blocks.WARPED_FENCE_GATE,
					Blocks.MANGROVE_FENCE_GATE, Blocks.BAMBOO_FENCE_GATE, Blocks.CHERRY_FENCE_GATE, Blocks.PALE_OAK_FENCE_GATE,
					// 床
					Blocks.WHITE_BED, Blocks.ORANGE_BED, Blocks.MAGENTA_BED, Blocks.LIGHT_BLUE_BED, Blocks.YELLOW_BED,
					Blocks.LIME_BED, Blocks.PINK_BED, Blocks.GRAY_BED, Blocks.LIGHT_GRAY_BED, Blocks.CYAN_BED,
					Blocks.PURPLE_BED, Blocks.BLUE_BED, Blocks.BROWN_BED, Blocks.GREEN_BED, Blocks.RED_BED,
					Blocks.BLACK_BED)
			.visible(
					directionProtection::get)
			.build());

	// Blocks that face away from player (Backward)
	private final Setting<List<Block>> dirBackward = sgDirectional.add(new BlockListSetting.Builder()
			.name("idirectional-backward")
			.description("Blocks that should face away from player.")
			.defaultValue(
					//合成器
					Blocks.CRAFTER,
					// 活塞
					Blocks.PISTON, Blocks.STICKY_PISTON,
					// 箱子
					Blocks.CHEST, Blocks.TRAPPED_CHEST, Blocks.ENDER_CHEST,
					// 铁轨
					Blocks.RAIL, Blocks.POWERED_RAIL, Blocks.DETECTOR_RAIL, Blocks.ACTIVATOR_RAIL,
					// 桶、切石机
					Blocks.BARREL, Blocks.STONECUTTER,
					// 蜂箱、蜂巢
					Blocks.BEE_NEST, Blocks.BEEHIVE,
					// 发射器、投掷器
					Blocks.DISPENSER, Blocks.DROPPER,
					// 中继器、比较器
					Blocks.REPEATER, Blocks.COMPARATOR,
					// 失水恶魂
					Blocks.SOUL_SOIL,
					// 雕刻南瓜、发光南瓜
					Blocks.CARVED_PUMPKIN, Blocks.JACK_O_LANTERN,
					// 讲台
					Blocks.LECTERN,
					// 炉子
					Blocks.FURNACE, Blocks.BLAST_FURNACE, Blocks.SMOKER,
					// 雕文书架
					Blocks.CHISELED_BOOKSHELF,
					// 铁活板门
					Blocks.IRON_TRAPDOOR,
					// 木活板门
					Blocks.OAK_TRAPDOOR, Blocks.SPRUCE_TRAPDOOR, Blocks.BIRCH_TRAPDOOR, Blocks.JUNGLE_TRAPDOOR,
					Blocks.ACACIA_TRAPDOOR, Blocks.DARK_OAK_TRAPDOOR, Blocks.CRIMSON_TRAPDOOR, Blocks.PALE_OAK_TRAPDOOR,
					Blocks.WARPED_TRAPDOOR, Blocks.MANGROVE_TRAPDOOR, Blocks.BAMBOO_TRAPDOOR, Blocks.CHERRY_TRAPDOOR,
					// 铜活板门
					Blocks.COPPER_TRAPDOOR, Blocks.EXPOSED_COPPER_TRAPDOOR, Blocks.WEATHERED_COPPER_TRAPDOOR,
					Blocks.OXIDIZED_COPPER_TRAPDOOR,
					Blocks.WAXED_COPPER_TRAPDOOR, Blocks.WAXED_EXPOSED_COPPER_TRAPDOOR,
					Blocks.WAXED_WEATHERED_COPPER_TRAPDOOR, Blocks.WAXED_OXIDIZED_COPPER_TRAPDOOR

			)
			.visible(directionProtection::get)
			.build());

	// Blocks that face to the left of player
	private final Setting<List<Block>> dirLeft = sgDirectional.add(new BlockListSetting.Builder()
			.name("idirectional-left")
			.description("Blocks that should face to the left of player.")
			.defaultValue(
					// 铁砧
					Blocks.ANVIL, Blocks.CHIPPED_ANVIL, Blocks.DAMAGED_ANVIL
					)
			.visible(directionProtection::get)
			.build());

	// Blocks that face to the right of player
	private final Setting<List<Block>> dirRight = sgDirectional.add(new BlockListSetting.Builder()
			.name("idirectional-right")
			.description("Blocks that should face to the right of player.")
			.visible(directionProtection::get)
			.build());

	// Blocks that face upward from player
	private final Setting<List<Block>> dirUp = sgDirectional.add(new BlockListSetting.Builder()
			.name("idirectional-up")
			.description("Blocks that should face upward from player.")
			.visible(directionProtection::get)
			.build());

	// Blocks that face downward from player
	private final Setting<List<Block>> dirDown = sgDirectional.add(new BlockListSetting.Builder()
			.name("idirectional-down")
			.description("Blocks that should face downward from player.")
			.visible(directionProtection::get)
			.build());


















	private final Setting<Boolean> precisePlacement = sgClickFace.add(new BoolSetting.Builder()
			.name("precise-placement")
			.description(
					"Use precise face-based placement for stairs, slabs, trapdoors etc. (ignores player orientation completely)")
			.defaultValue(true)
			.build());
	private final Setting<Boolean> freeFaceForDefaultTorch = sgClickFace.add(new BoolSetting.Builder()
			.name("free-face-of-default-torch")
			.description("Allow placing default torches without precise placement.")
			.defaultValue(false)
			.visible(precisePlacement::get)
			.build());

	// Precise Placement Face Lists
	private final Setting<List<Block>> preciseForward = sgClickFace.add(new BlockListSetting.Builder()
			.name("precise-facing-forward")
			.description("Blocks for precise placement facing forward.")
			.defaultValue(
				// 火把
				Blocks.TORCH, Blocks.SOUL_TORCH, Blocks.REDSTONE_TORCH,
				// 梯子
				Blocks.LADDER,
				// 拉杆
				Blocks.LEVER,

				// 按钮
				Blocks.STONE_BUTTON, Blocks.OAK_BUTTON, Blocks.SPRUCE_BUTTON, Blocks.BIRCH_BUTTON, 
				Blocks.JUNGLE_BUTTON, Blocks.ACACIA_BUTTON, Blocks.DARK_OAK_BUTTON, Blocks.CRIMSON_BUTTON,
				Blocks.WARPED_BUTTON, Blocks.MANGROVE_BUTTON, Blocks.BAMBOO_BUTTON, Blocks.CHERRY_BUTTON,
				Blocks.POLISHED_BLACKSTONE_BUTTON,
				// 潜影盒
				Blocks.SHULKER_BOX, Blocks.WHITE_SHULKER_BOX, Blocks.ORANGE_SHULKER_BOX, Blocks.MAGENTA_SHULKER_BOX,
				Blocks.LIGHT_BLUE_SHULKER_BOX, Blocks.YELLOW_SHULKER_BOX, Blocks.LIME_SHULKER_BOX,
				Blocks.PINK_SHULKER_BOX, Blocks.GRAY_SHULKER_BOX, Blocks.LIGHT_GRAY_SHULKER_BOX,
				Blocks.CYAN_SHULKER_BOX, Blocks.PURPLE_SHULKER_BOX, Blocks.BLUE_SHULKER_BOX,
				Blocks.BROWN_SHULKER_BOX, Blocks.GREEN_SHULKER_BOX, Blocks.RED_SHULKER_BOX,
				Blocks.BLACK_SHULKER_BOX,
				// 铁活板门
				Blocks.IRON_TRAPDOOR,
				// 木活板门
				Blocks.OAK_TRAPDOOR, Blocks.SPRUCE_TRAPDOOR, Blocks.BIRCH_TRAPDOOR, Blocks.JUNGLE_TRAPDOOR,
				Blocks.ACACIA_TRAPDOOR, Blocks.DARK_OAK_TRAPDOOR, Blocks.CRIMSON_TRAPDOOR, Blocks.PALE_OAK_TRAPDOOR,
				Blocks.WARPED_TRAPDOOR, Blocks.MANGROVE_TRAPDOOR, Blocks.BAMBOO_TRAPDOOR, Blocks.CHERRY_TRAPDOOR,
				// 铜活板门
				Blocks.COPPER_TRAPDOOR, Blocks.EXPOSED_COPPER_TRAPDOOR, Blocks.WEATHERED_COPPER_TRAPDOOR, Blocks.OXIDIZED_COPPER_TRAPDOOR,
				Blocks.WAXED_COPPER_TRAPDOOR, Blocks.WAXED_EXPOSED_COPPER_TRAPDOOR, Blocks.WAXED_WEATHERED_COPPER_TRAPDOOR, Blocks.WAXED_OXIDIZED_COPPER_TRAPDOOR,
				
				//单头棍子
				Blocks.END_ROD, Blocks.LIGHTNING_ROD,

				//双头棍子
				Blocks.HAY_BLOCK, Blocks.CHAIN,
				// 原木
				Blocks.OAK_LOG, Blocks.SPRUCE_LOG, Blocks.BIRCH_LOG, Blocks.JUNGLE_LOG, 
				Blocks.ACACIA_LOG, Blocks.DARK_OAK_LOG, Blocks.CRIMSON_STEM, Blocks.PALE_OAK_LOG,
				Blocks.WARPED_STEM, Blocks.MANGROVE_LOG, Blocks.CHERRY_LOG,
				// 去皮原木
				Blocks.STRIPPED_OAK_LOG, Blocks.STRIPPED_SPRUCE_LOG, Blocks.STRIPPED_BIRCH_LOG, Blocks.STRIPPED_JUNGLE_LOG,
				Blocks.STRIPPED_ACACIA_LOG, Blocks.STRIPPED_DARK_OAK_LOG, Blocks.STRIPPED_CRIMSON_STEM, Blocks.STRIPPED_WARPED_STEM,
				Blocks.STRIPPED_MANGROVE_LOG, Blocks.STRIPPED_CHERRY_LOG, Blocks.STRIPPED_PALE_OAK_LOG,
				// 木头
				Blocks.OAK_WOOD, Blocks.SPRUCE_WOOD, Blocks.BIRCH_WOOD, Blocks.JUNGLE_WOOD, 
				Blocks.ACACIA_WOOD, Blocks.DARK_OAK_WOOD,Blocks.CRIMSON_HYPHAE, Blocks.WARPED_HYPHAE, 
				Blocks.MANGROVE_WOOD, Blocks.CHERRY_WOOD, Blocks.PALE_OAK_WOOD,
				// 去皮木头
				Blocks.STRIPPED_OAK_WOOD, Blocks.STRIPPED_SPRUCE_WOOD, Blocks.STRIPPED_BIRCH_WOOD, Blocks.STRIPPED_JUNGLE_WOOD,
				Blocks.STRIPPED_ACACIA_WOOD, Blocks.STRIPPED_DARK_OAK_WOOD, Blocks.STRIPPED_CRIMSON_HYPHAE, Blocks.STRIPPED_WARPED_HYPHAE,
				Blocks.STRIPPED_MANGROVE_WOOD, Blocks.STRIPPED_CHERRY_WOOD, Blocks.STRIPPED_PALE_OAK_WOOD,
				// 其他AXIS块
				Blocks.BAMBOO_BLOCK, Blocks.STRIPPED_BAMBOO_BLOCK, 
				Blocks.QUARTZ_PILLAR, Blocks.BONE_BLOCK, Blocks.PURPUR_PILLAR, Blocks.BASALT, Blocks.POLISHED_BASALT, 
				// 蛙鸣灯
				Blocks.OCHRE_FROGLIGHT, Blocks.PEARLESCENT_FROGLIGHT, Blocks.VERDANT_FROGLIGHT
				
				
				)
			.visible(precisePlacement::get)
			.build());

	private final Setting<List<Block>> preciseBackward = sgClickFace.add(new BlockListSetting.Builder()
			.name("precise-facing-backward")
			.description("Blocks for precise placement facing backward.")
			.defaultValue(
				
				// 钟
				Blocks.BELL,
				// 漏斗
				Blocks.HOPPER,
				//双头棍子
				Blocks.HAY_BLOCK, Blocks.CHAIN,
				// 原木
				Blocks.OAK_LOG, Blocks.SPRUCE_LOG, Blocks.BIRCH_LOG, Blocks.JUNGLE_LOG, 
				Blocks.ACACIA_LOG, Blocks.DARK_OAK_LOG, Blocks.CRIMSON_STEM, Blocks.PALE_OAK_LOG,
				Blocks.WARPED_STEM, Blocks.MANGROVE_LOG, Blocks.CHERRY_LOG,
				// 去皮原木
				Blocks.STRIPPED_OAK_LOG, Blocks.STRIPPED_SPRUCE_LOG, Blocks.STRIPPED_BIRCH_LOG, Blocks.STRIPPED_JUNGLE_LOG,
				Blocks.STRIPPED_ACACIA_LOG, Blocks.STRIPPED_DARK_OAK_LOG, Blocks.STRIPPED_CRIMSON_STEM, Blocks.STRIPPED_WARPED_STEM,
				Blocks.STRIPPED_MANGROVE_LOG, Blocks.STRIPPED_CHERRY_LOG, Blocks.STRIPPED_PALE_OAK_LOG,
				// 木头
				Blocks.OAK_WOOD, Blocks.SPRUCE_WOOD, Blocks.BIRCH_WOOD, Blocks.JUNGLE_WOOD, 
				Blocks.ACACIA_WOOD, Blocks.DARK_OAK_WOOD,Blocks.CRIMSON_HYPHAE, Blocks.WARPED_HYPHAE, 
				Blocks.MANGROVE_WOOD, Blocks.CHERRY_WOOD, Blocks.PALE_OAK_WOOD,
				// 去皮木头
				Blocks.STRIPPED_OAK_WOOD, Blocks.STRIPPED_SPRUCE_WOOD, Blocks.STRIPPED_BIRCH_WOOD, Blocks.STRIPPED_JUNGLE_WOOD,
				Blocks.STRIPPED_ACACIA_WOOD, Blocks.STRIPPED_DARK_OAK_WOOD, Blocks.STRIPPED_CRIMSON_HYPHAE, Blocks.STRIPPED_WARPED_HYPHAE,
				Blocks.STRIPPED_MANGROVE_WOOD, Blocks.STRIPPED_CHERRY_WOOD, Blocks.STRIPPED_PALE_OAK_WOOD,
				// 其他AXIS块
				Blocks.BAMBOO_BLOCK, Blocks.STRIPPED_BAMBOO_BLOCK, 
				Blocks.QUARTZ_PILLAR, Blocks.BONE_BLOCK, Blocks.PURPUR_PILLAR, Blocks.BASALT, Blocks.POLISHED_BASALT, 
				// 蛙鸣灯
				Blocks.OCHRE_FROGLIGHT, Blocks.PEARLESCENT_FROGLIGHT, Blocks.VERDANT_FROGLIGHT
				
			)
			.visible(precisePlacement::get)
			.build());
	












































	public boolean placeBlock(BlockState required, BlockPos pos) {
		//检查点
		ClientPlayerEntity player = mc.player;
		ClientWorld world = mc.world;
		if (player == null || world == null || !canPlace(pos))
			return false;
		if (!isMultiStructurePlacementAllowed(required)) {
			return false;
		}
		Block block = required.getBlock();
		// 检查面
		for (Direction face : Direction.values()) {
			final Vec3d hitPos;
			Vec3d tempHitPos = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
			boolean disableDirectionProtection = false;
			boolean disableFaceProtection = false;
			if (block instanceof TorchBlock && !freeFaceForDefaultTorch.get() && face != Direction.UP) {
				continue;// 直立式火把只能放在邻居上面
			}
			//根据特殊状态过滤面，设置pos偏移，对不同的面选择性禁用两种保护
			else if (block instanceof TrapdoorBlock) {//活板门有取半，既有点击面向又有玩家方向，要根据面不同禁用不同保护，
				switch (required.get(Properties.BLOCK_HALF)) {
					case TOP:// 上半活板门
						switch (face) {
							case UP:
								continue;// 不能放在邻居上方
							case DOWN:
							// 正常放在邻居下方方向取决于玩家，禁用点击面保护
								disableFaceProtection = true;
								break;
							case NORTH, SOUTH, EAST, WEST:
							//放置在四周方向取决于点击面，禁用方向保护，设置半砖偏移
								tempHitPos = tempHitPos.add(0, 0.25, 0);
								disableDirectionProtection = true;
								break;
						}
						break;
					case BOTTOM://下半活板门
						switch (face) {
							case UP:
							// 正常放在邻居上方方向取决于玩家，禁用点击面保护
								disableFaceProtection = true;
								break;
							case DOWN:
								continue;// 不能放在邻居下方
							case NORTH, SOUTH, EAST, WEST:
							//放置在四周方向取决于点击面，禁用方向保护，设置半砖偏移
								tempHitPos = tempHitPos.add(0, -0.25, 0);
								disableDirectionProtection = true;
								break;
						}
						break;
				}

			} else if (block instanceof StairsBlock) {//楼梯有取半，没有点击面向只有玩家方向
				switch (required.get(Properties.BLOCK_HALF)) {
					case TOP://上半楼梯
						switch (face) {
							case UP:
								continue;// 不能放在邻居上方
							case DOWN:
								break;
							case NORTH, SOUTH, EAST, WEST://放置在四周设置半砖偏移
								tempHitPos = tempHitPos.add(0, 0.25, 0);
								break;
						}
					break;
					case BOTTOM://下半楼梯
						switch (face) {
							case UP:
								break;
							case DOWN:
								continue;// 不能放在邻居下方
							case NORTH, SOUTH, EAST, WEST://放置在四周设置半砖偏移
								tempHitPos = tempHitPos.add(0, -0.25, 0);
								break;
						}
					break;
				}

			} else if (block instanceof SlabBlock) {//半砖有取半，没有点击面向没有玩家方向
				switch (required.get(Properties.SLAB_TYPE)) {
					case TOP://上半砖
						switch (face) {
							case UP:
								continue;// 不能放在邻居上方
							case DOWN:
								break;
							case NORTH, SOUTH, EAST, WEST://放置在四周设置半砖偏移
								tempHitPos = tempHitPos.add(0, 0.25, 0);
								break;
						}
					break;
					case BOTTOM://下半砖
						switch (face) {
							case UP:
								break;
							case DOWN:
								continue;// 不能放在邻居下方
							case NORTH, SOUTH, EAST, WEST://放置在四周设置半砖偏移
								tempHitPos = tempHitPos.add(0, -0.25, 0);
								break;
						}
					break;
					case DOUBLE:
						//整砖随机结果
					break;
				}
			} else if (required.contains(Properties.ATTACHMENT)) {//钟既有点击面向又有玩家方向，要根据面不同禁用不同保护
				switch (required.get(Properties.ATTACHMENT)) {
					case FLOOR:
						if (face != Direction.UP) continue;// 只能放在邻居上方
						// 正常放在邻居下方方向取决于玩家，禁用点击面保护
						disableFaceProtection = true;
					break;
					case CEILING:
						if (face != Direction.DOWN) continue;// 只能放在邻居下方
						// 正常放在邻居下方方向取决于玩家，禁用点击面保护
						disableFaceProtection = true;
						break;	
					default:
						if (face == Direction.UP || face == Direction.DOWN) continue;// 只能放在邻居四周
						//放置在四周方向取决于点击面，禁用方向保护
						disableDirectionProtection = true;
					break;
				}
			} else if (required.contains(Properties.HANGING)) {//灯笼没有方向和点击面向
				if (required.get(Properties.HANGING)) {
					// 吊着的
					if (face != Direction.DOWN) continue;// 只能放在邻居下方
				} else {
					// 不吊着的
					if (face == Direction.DOWN) continue;// 不能放在邻居下方
				}
			} else if (required.contains(Properties.BLOCK_FACE)){//拉杆 按钮 既有点击面向又有玩家方向，要根据面不同禁用不同保护
				switch (required.get(Properties.BLOCK_FACE)) {
					case FLOOR:
						if (face != Direction.UP) continue;// 只能放在邻居上方
						// 正常放在邻居下方方向取决于玩家，禁用点击面保护
						disableFaceProtection = true;
					break;
					case CEILING:
						if (face != Direction.DOWN) continue;// 只能放在邻居下方
						// 正常放在邻居下方方向取决于玩家，禁用点击面保护
						disableFaceProtection = true;
						break;	
					case WALL:
						if (face == Direction.UP || face == Direction.DOWN) continue;// 只能放在邻居四周
						//放置在四周方向取决于点击面，禁用方向保护
						disableDirectionProtection = true;
					break;
				}
			}

			if (airPlace.get()) {
				disableDirectionProtection = false;
			}
			if (directionProtection.get() && !disableDirectionProtection
					&& !isPlaceAllowedFromPlayerDirection(required)) {
				continue;
			}
			if (precisePlacement.get() && !disableFaceProtection
					&& !isPlaceAllowedFromFace(required, face)) {
				continue;
			}

			// 确定要点一个邻居面不会导致状态错误的话，计算邻居和点击位置(面中心+侧方半砖偏移)
			BlockPos neighbour;

			if (airPlace.get() && !airplaceBlacklist.get().contains(block)) {
				neighbour = pos;
			} else {
				Direction OppositeFace = face.getOpposite();
				neighbour = pos.offset(OppositeFace);
				if (!canPlaceAgainst(required, neighbour, face)) {
					continue;
				}
				tempHitPos = tempHitPos.add(OppositeFace.getOffsetX() * 0.5, OppositeFace.getOffsetY() * 0.5,
						OppositeFace.getOffsetZ() * 0.5);
			}
			hitPos = tempHitPos;
			// 确定了face 邻居坐标 点击位置

			// 距离保护
			if (hitPos.distanceTo(getPlayerEyePos(player)) > placeRange.get()) {
				continue;
			}

			// 筛出不安全的面
			if (safetyPlaceFaceMode.get() != SafetyFaceMode.None
					&& face != getASafetyFaceOrNull(neighbour, safetyPlaceFaceMode.get())) {
				continue;
			}
			if (!placeThroughWall.get()) {
				if (airPlace.get()) {
					if (!isPointVisible(hitPos)) {
						continue;
					}
				} else {
					if (!isAFaceOutVisibleOfBlock(neighbour, face)) {
						continue;
					}
				}
			}
			if (onlyPlaceOnLookFace.get() && !isPlayerYawPitchInAFaceOfBlock(neighbour, face)) {
				continue;
			}
			Item item = required.getBlock().asItem();
			if (dirtgrass.get() && item == Items.GRASS_BLOCK)
				item = Items.DIRT;
			return MyUtils.switchItem(item, required, returnHand.get(),
					() -> {
						if (rotate.get()) {
							Rotations.rotate(Rotations.getYaw(hitPos), Rotations.getPitch(hitPos), 50, clientSide.get(),
									() -> place(new BlockHitResult(hitPos, face, neighbour, false), swing.get()));
						} else {
							place(new BlockHitResult(hitPos, face, neighbour, false), swing.get());
						}
						return true;
					});
		}
		return false;
	}
	private boolean isPlaceAllowedFromFace(BlockState blockState, Direction chosenFace) {
		Direction requiredDirection = getATagFaceOf(blockState);
		if (requiredDirection == null) {
			return true;
		}
		//方块能判断方向

		Block block = blockState.getBlock();
		// Check each directional list to see if this block is configured
		boolean inListForward = preciseForward.get().contains(block);
		boolean inListBackward = preciseBackward.get().contains(block);
		if (inListForward || inListBackward) {//方块需要判断方向
			if (inListForward) {
				// Block should face same direction as player
				if (requiredDirection.equals(chosenFace)) {
					return true;
				}
			}
			if (inListBackward) {
				// Block should face away from player
				if (requiredDirection.equals(chosenFace.getOpposite())) {
					return true;
				}
			}
			
			return false;
		} else {
			return true;
		}
	}
	private boolean isPlaceAllowedFromPlayerDirection(BlockState blockState) {
		Direction requiredDirection = getATagFaceOf(blockState);
		if (requiredDirection == null) {
			return true;
		}
		//方块能判断方向

		Block block = blockState.getBlock();
		// Check each directional list to see if this block is configured
		boolean inListForward = dirForward.get().contains(block);
		boolean inListBackward = dirBackward.get().contains(block);
		boolean inListLeft = dirLeft.get().contains(block);
		boolean inListRight = dirRight.get().contains(block);
		boolean inListUp = dirUp.get().contains(block);
		boolean inListDown = dirDown.get().contains(block);
		if (inListForward || inListBackward || inListLeft || inListRight || inListUp || inListDown) {//方块需要判断方向
			Direction playerDirection = getPlayerFacingDirectionOrNull();
			if (playerDirection == null) {
				return false;
			}
			//判断方块方向适配玩家方向
			if (inListForward) {
				// Block should face same direction as player
				if (requiredDirection.equals(playerDirection)) {
					return true;
				}
			}
			if (inListBackward) {
				// Block should face away from player
				if (requiredDirection.equals(playerDirection.getOpposite())) {
					return true;
				}
			}
			if (inListLeft) {
				// Block should face to the left of player
				Direction leftDirection = getLeftDirectionFromPlayer(playerDirection);
				if (leftDirection != null && requiredDirection.equals(leftDirection)) {
					return true;
				}
			}
			if (inListRight) {
				// Block should face to the right of player
				Direction leftDirection = getLeftDirectionFromPlayer(playerDirection);
				if (leftDirection != null && requiredDirection.equals(leftDirection.getOpposite())) {
					return true;
				}
			}
			if (inListUp) {
				// Block should face upward relative to player
				Direction upDirection = getUpDirectionFromPlayer(playerDirection);
				if (upDirection != null && requiredDirection.equals(upDirection)) {
					return true;
				}
			}
			if (inListDown) {
				// Block should face downward relative to player
				Direction upDirection = getUpDirectionFromPlayer(playerDirection);
				if (upDirection != null && requiredDirection.equals(upDirection.getOpposite())) {
					return true;
				}
			}
			return false;
		} else {
			return true;
		}
	}

	private @Nullable Direction getPlayerFacingDirectionOrNull() {
		ClientPlayerEntity player = mc.player;
		if (player == null) {
			return null;
		}
		float pitch = player.getPitch();
		if (isInRangeOfValue(pitch, 90, angleRangeForDirectionProtection.get())) {
			return Direction.DOWN;
		} else if (isInRangeOfValue(pitch, -90, angleRangeForDirectionProtection.get())) {
			return Direction.UP;
		} else if (isInRangeOfValue(pitch, 0, angleRangeForDirectionProtection.get())) {
			float yaw = Rotation.normalizeYaw(player.getYaw());
			if (isInRangeOfValue(yaw, 90, angleRangeForDirectionProtection.get())) {
				return Direction.WEST;
			} else if (isInRangeOfValue(yaw, 0, angleRangeForDirectionProtection.get())) {
				return Direction.SOUTH;
			} else if (isInRangeOfValue(yaw, -90, angleRangeForDirectionProtection.get())) {
				return Direction.EAST;
			} else if (isInRangeOfValue(yaw, 180, angleRangeForDirectionProtection.get()) || isInRangeOfValue(yaw, -180, angleRangeForDirectionProtection.get())) {
				return Direction.NORTH;
			} else {
				return null;
			}
		} else {
			return null;
		}
	}

	private boolean canPlaceAgainst(BlockState self, BlockPos neighbourPos, Direction neighbourFace) {
		ClientWorld world = mc.world;
		if (world == null) return false;
		ClientPlayerEntity player = mc.player;
		if (player == null) return false;
		BlockState neighbour = world.getBlockState(neighbourPos);
		var neighbourBlock = neighbour.getBlock();
		return 
		!neighbour.isAir()
		&& neighbour.getFluidState().isEmpty()
		&& (
			isClickable(neighbourBlock) && player.isSneaking()
			||
			!isClickable(neighbourBlock)
			)
		&& !(enableBlacklist.get() && blacklist.get().contains(neighbourBlock))
		&&
		(
			isBlockShapeFullCube(neighbour)||
			neighbourBlock == Blocks.GLASS || 
			neighbourBlock instanceof StainedGlassBlock ||
			neighbourBlock instanceof StairsBlock ||
			(enableAddList.get() && addList.get().contains(neighbourBlock)) ||
			(
				neighbourBlock instanceof SlabBlock //不会重叠的半砖
				&&
				(
					self.getBlock() != neighbour.getBlock()//类型不同不融合
					||
					neighbour.get(SlabBlock.TYPE) == SlabType.DOUBLE//邻居双层不融合
					||
					(
						self.getBlock() == neighbour.getBlock() //同类型半砖
						&&
						(
							!(//不能从上向下放到半砖底 会融合
							neighbour.get(SlabBlock.TYPE) == SlabType.BOTTOM &&
							neighbourFace == Direction.UP
							)&&
							!(//不能从下向上放到半砖顶 会融合
							neighbour.get(SlabBlock.TYPE) == SlabType.TOP &&
							neighbourFace == Direction.DOWN
							)
							&&
							!(
								//两半砖顶底不同时 从侧面放置 会融合
								neighbourFace != Direction.UP && neighbourFace != Direction.DOWN //侧面放置
								&&
								!(
									//顶底相同
									neighbour.get(SlabBlock.TYPE) == self.get(SlabBlock.TYPE)
								)
							)
						)
					)
				)
			)
		);
	}



	private void place(BlockHitResult blockHitResult, boolean swing) {
		if (mc.player == null || mc.interactionManager == null || mc.getNetworkHandler() == null)
			return;
		ActionResult result = mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, blockHitResult);
		if (result == ActionResult.SUCCESS) {
			if (swing)
				mc.player.swingHand(Hand.MAIN_HAND);
			else
				mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
		}
	}

}
