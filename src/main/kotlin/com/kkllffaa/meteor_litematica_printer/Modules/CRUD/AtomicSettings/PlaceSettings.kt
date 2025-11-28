package com.kkllffaa.meteor_litematica_printer.Modules.CRUD.AtomicSettings

import com.kkllffaa.meteor_litematica_printer.Addon
import com.kkllffaa.meteor_litematica_printer.Functions.*
import meteordevelopment.meteorclient.settings.*
import meteordevelopment.meteorclient.systems.modules.Module
import meteordevelopment.meteorclient.utils.world.BlockUtils
import net.minecraft.block.*
import net.minecraft.block.enums.*
import net.minecraft.item.Items
import net.minecraft.state.property.Properties
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d

object PlaceSettings : Module(Addon.SettingsForCRUD, "Place", "Module to configure AtomicSettings.") {
    override fun toggle() {
        if (isActive) return
        super.toggle()
    }

    init {
        this.toggle()
    }

    private val sgGeneral = settings.defaultGroup
    private val sgDirectional = settings.createGroup("Directional Protection")
    private val sgClickFace = settings.createGroup("Click Face")
    private val sgNeighbor = settings.createGroup("Place On Neighbor Blocks")

    private val enableBlacklist: Setting<Boolean> = sgNeighbor.add(
        BoolSetting.Builder()
            .name("enable-blacklist")
            .description("Enable blacklist for neighbor blocks.")
            .defaultValue(false)
            .build()
    )

    private val blacklist: Setting<MutableList<Block>> = sgNeighbor.add(
        BlockListSetting.Builder()
            .name("blacklist")
            .description("Blocks that cannot be placed against.(Click Face Center Pos is Air)")
            .defaultValue( //面中心没有体积的砖
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
            .visible { enableBlacklist.get() }
            .build()
    )

    private val enableAddList: Setting<Boolean> = sgNeighbor.add(
        BoolSetting.Builder()
            .name("enable-additional-list")
            .description("Enable additional list for neighbor blocks.")
            .defaultValue(false)
            .build()
    )

    private val addList: Setting<MutableList<Block>> = sgNeighbor.add(
        BlockListSetting.Builder()
            .name("additional-list")
            .description("Additional blocks allowed after collision box filtering.")
            .visible { enableAddList.get() }
            .build()
    )
    private val swingHand: Setting<ActionMode> = sgGeneral.add(
        EnumSetting.Builder<ActionMode>()
            .name("swing-hand")
            .description("swing hand post place.")
            .defaultValue(ActionMode.None)
            .build()
    )


    private val airPlace: Setting<Boolean> = sgGeneral.add(
        BoolSetting.Builder()
            .name("air-place")
            .description("Allow the bot to place in the air.")
            .defaultValue(true)
            .build()
    )

    private val airplaceBlacklist: Setting<MutableList<Block>> = sgGeneral.add(
        BlockListSetting.Builder()
            .name("airplace-blacklist")
            .description("Blocks that cannot be placed in airplace.")
            .defaultValue(
                Blocks.GRINDSTONE
                // Blocks.LANTERN, Blocks.SOUL_LANTERN, *Blocks.COPPER_LANTERNS.all.toTypedArray()
            )
            .visible { airPlace.get() }
            .build()
    )
    private val placeThroughWall: Setting<Boolean> = sgGeneral.add(
        BoolSetting.Builder()
            .name("Place Through Wall")
            .description("Allow the bot to place through walls.")
            .defaultValue(true)
            .build()
    )

    private val safetyPlaceFaceMode: Setting<SafetyFaceMode> = sgGeneral.add(
        EnumSetting.Builder<SafetyFaceMode>()
            .name("direction-mode")
            .description("Only place blocks on A safe faces.")
            .defaultValue(SafetyFaceMode.None)
            .build()
    )

    private val onlyPlaceOnLookFace: Setting<Boolean> = sgGeneral.add(
        BoolSetting.Builder()
            .name("only-place-on-look-face")
            .description("Only place blocks on the face you are looking at direction")
            .defaultValue(false)
            .build()
    )

    private val returnHand: Setting<Boolean> = sgGeneral.add(
        BoolSetting.Builder()
            .name("return-slot")
            .description("Return to old slot.")
            .defaultValue(false)
            .build()
    )

    private val dirtgrass: Setting<Boolean> = sgGeneral.add(
        BoolSetting.Builder()
            .name("dirt-as-grass")
            .description("Use dirt instead of grass.")
            .defaultValue(false)
            .build()
    )

    val SignTextWithColor: Setting<SignColorMode> = sgGeneral.add(
        EnumSetting.Builder<SignColorMode>()
            .name("sign-text-with-color")
            .description("Use colored text for signs.")
            .defaultValue(SignColorMode.None)
            .build()
    )

    enum class SignColorMode {
        None,
        反三,
        八字符号
    }


    // Directional Protection Settings
    private val directionProtection: Setting<Boolean> = sgDirectional.add(
        BoolSetting.Builder()
            .name("direction-protection")
            .description("Only place directional blocks when player is facing the correct direction.")
            .defaultValue(true)
            .build()
    )

    private val angleRangeForDirectionProtection: Setting<Int> = sgDirectional.add(
        IntSetting.Builder()
            .name("angle-range")
            .description("Angle range for direction detection (degrees).")
            .defaultValue(25)
            .min(1).sliderMin(1)
            .max(45).sliderMax(44)
            .visible { directionProtection.get() }
            .build()
    )

    // Blocks that face the same direction as player (Forward)
    private val dirForward: Setting<MutableList<Block>> = sgDirectional.add(
        BlockListSetting.Builder()
            .name("facing-forward")
            .description("Blocks that should face the same direction as player.")
            .defaultValue( // 侦测器、钟
                Blocks.OBSERVER,
                Blocks.BELL,  // 砂轮
                Blocks.GRINDSTONE,  // 拉杆
                Blocks.LEVER,  // 按钮

                Blocks.STONE_BUTTON,
                Blocks.OAK_BUTTON,
                Blocks.SPRUCE_BUTTON,
                Blocks.BIRCH_BUTTON,
                Blocks.JUNGLE_BUTTON,
                Blocks.ACACIA_BUTTON,
                Blocks.DARK_OAK_BUTTON,
                Blocks.CRIMSON_BUTTON,
                Blocks.WARPED_BUTTON,
                Blocks.MANGROVE_BUTTON,
                Blocks.BAMBOO_BUTTON,
                Blocks.CHERRY_BUTTON,
                Blocks.POLISHED_BLACKSTONE_BUTTON,  // 铁轨
                Blocks.RAIL,
                Blocks.POWERED_RAIL,
                Blocks.DETECTOR_RAIL,
                Blocks.ACTIVATOR_RAIL,  // 楼梯
                Blocks.OAK_STAIRS,
                Blocks.SPRUCE_STAIRS,
                Blocks.BIRCH_STAIRS,
                Blocks.JUNGLE_STAIRS,
                Blocks.ACACIA_STAIRS,
                Blocks.DARK_OAK_STAIRS,
                Blocks.STONE_STAIRS,
                Blocks.COBBLESTONE_STAIRS,
                Blocks.BRICK_STAIRS,
                Blocks.STONE_BRICK_STAIRS,
                Blocks.NETHER_BRICK_STAIRS,
                Blocks.SANDSTONE_STAIRS,
                Blocks.QUARTZ_STAIRS,
                Blocks.RED_SANDSTONE_STAIRS,
                Blocks.PURPUR_STAIRS,
                Blocks.PRISMARINE_STAIRS,
                Blocks.PRISMARINE_BRICK_STAIRS,
                Blocks.DARK_PRISMARINE_STAIRS,
                Blocks.GRANITE_STAIRS,
                Blocks.DIORITE_STAIRS,
                Blocks.ANDESITE_STAIRS,
                Blocks.POLISHED_GRANITE_STAIRS,
                Blocks.POLISHED_DIORITE_STAIRS,
                Blocks.POLISHED_ANDESITE_STAIRS,
                Blocks.MOSSY_STONE_BRICK_STAIRS,
                Blocks.MOSSY_COBBLESTONE_STAIRS,
                Blocks.SMOOTH_SANDSTONE_STAIRS,
                Blocks.SMOOTH_RED_SANDSTONE_STAIRS,
                Blocks.SMOOTH_QUARTZ_STAIRS,
                Blocks.END_STONE_BRICK_STAIRS,
                Blocks.BLACKSTONE_STAIRS,
                Blocks.POLISHED_BLACKSTONE_STAIRS,
                Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS,
                Blocks.CRIMSON_STAIRS,
                Blocks.WARPED_STAIRS,
                Blocks.MANGROVE_STAIRS,
                Blocks.BAMBOO_STAIRS,
                Blocks.BAMBOO_MOSAIC_STAIRS,
                Blocks.CHERRY_STAIRS,
                Blocks.COBBLED_DEEPSLATE_STAIRS,
                Blocks.POLISHED_DEEPSLATE_STAIRS,
                Blocks.DEEPSLATE_BRICK_STAIRS,
                Blocks.DEEPSLATE_TILE_STAIRS,
                Blocks.PALE_OAK_STAIRS,
                Blocks.RED_NETHER_BRICK_STAIRS,
                Blocks.RESIN_BRICK_STAIRS,
                Blocks.MUD_BRICK_STAIRS,
                Blocks.TUFF_BRICK_STAIRS,
                Blocks.POLISHED_TUFF_STAIRS,
                Blocks.TUFF_STAIRS,  // 栅栏门
                Blocks.OAK_FENCE_GATE,
                Blocks.SPRUCE_FENCE_GATE,
                Blocks.BIRCH_FENCE_GATE,
                Blocks.JUNGLE_FENCE_GATE,
                Blocks.ACACIA_FENCE_GATE,
                Blocks.DARK_OAK_FENCE_GATE,
                Blocks.CRIMSON_FENCE_GATE,
                Blocks.WARPED_FENCE_GATE,
                Blocks.MANGROVE_FENCE_GATE,
                Blocks.BAMBOO_FENCE_GATE,
                Blocks.CHERRY_FENCE_GATE,
                Blocks.PALE_OAK_FENCE_GATE,  // 床
                Blocks.WHITE_BED,
                Blocks.ORANGE_BED,
                Blocks.MAGENTA_BED,
                Blocks.LIGHT_BLUE_BED,
                Blocks.YELLOW_BED,
                Blocks.LIME_BED,
                Blocks.PINK_BED,
                Blocks.GRAY_BED,
                Blocks.LIGHT_GRAY_BED,
                Blocks.CYAN_BED,
                Blocks.PURPLE_BED,
                Blocks.BLUE_BED,
                Blocks.BROWN_BED,
                Blocks.GREEN_BED,
                Blocks.RED_BED,
                Blocks.BLACK_BED
            )
            .visible { directionProtection.get() }
            .build()
    )

    // Blocks that face away from player (Backward)
    private val dirBackward: Setting<MutableList<Block>> = sgDirectional.add(
        BlockListSetting.Builder()
            .name("directional-backward")
            .description("Blocks that should face away from player.")
            .defaultValue( //合成器
                Blocks.CRAFTER,  // 活塞
                Blocks.PISTON,
                Blocks.STICKY_PISTON,  // 箱子
                Blocks.CHEST,
                Blocks.TRAPPED_CHEST,
                Blocks.ENDER_CHEST,  // 铁轨
                Blocks.RAIL,
                Blocks.POWERED_RAIL,
                Blocks.DETECTOR_RAIL,
                Blocks.ACTIVATOR_RAIL,  // 桶、切石机
                Blocks.BARREL,
                Blocks.STONECUTTER,  // 蜂箱、蜂巢
                Blocks.BEE_NEST,
                Blocks.BEEHIVE,  // 发射器、投掷器
                Blocks.DISPENSER,
                Blocks.DROPPER,  // 中继器、比较器
                Blocks.REPEATER,
                Blocks.COMPARATOR,  // 失水恶魂
                Blocks.SOUL_SOIL,  // 雕刻南瓜、发光南瓜
                Blocks.CARVED_PUMPKIN,
                Blocks.JACK_O_LANTERN,  // 讲台
                Blocks.LECTERN,  // 炉子
                Blocks.FURNACE,
                Blocks.BLAST_FURNACE,
                Blocks.SMOKER,  // 雕文书架
                Blocks.CHISELED_BOOKSHELF,  // 铁活板门
                Blocks.IRON_TRAPDOOR,  // 木活板门
                Blocks.OAK_TRAPDOOR,
                Blocks.SPRUCE_TRAPDOOR,
                Blocks.BIRCH_TRAPDOOR,
                Blocks.JUNGLE_TRAPDOOR,
                Blocks.ACACIA_TRAPDOOR,
                Blocks.DARK_OAK_TRAPDOOR,
                Blocks.CRIMSON_TRAPDOOR,
                Blocks.PALE_OAK_TRAPDOOR,
                Blocks.WARPED_TRAPDOOR,
                Blocks.MANGROVE_TRAPDOOR,
                Blocks.BAMBOO_TRAPDOOR,
                Blocks.CHERRY_TRAPDOOR,  // 铜活板门
                Blocks.COPPER_TRAPDOOR,
                Blocks.EXPOSED_COPPER_TRAPDOOR,
                Blocks.WEATHERED_COPPER_TRAPDOOR,
                Blocks.OXIDIZED_COPPER_TRAPDOOR,
                Blocks.WAXED_COPPER_TRAPDOOR,
                Blocks.WAXED_EXPOSED_COPPER_TRAPDOOR,
                Blocks.WAXED_WEATHERED_COPPER_TRAPDOOR,
                Blocks.WAXED_OXIDIZED_COPPER_TRAPDOOR

            )
            .visible { directionProtection.get() }
            .build()
    )

    // Blocks that face to the left of player
    private val dirLeft: Setting<MutableList<Block>> = sgDirectional.add(
        BlockListSetting.Builder()
            .name("directional-left")
            .description("Blocks that should face to the left of player.")
            .defaultValue( // 铁砧
                Blocks.ANVIL, Blocks.CHIPPED_ANVIL, Blocks.DAMAGED_ANVIL
            )
            .visible { directionProtection.get() }
            .build()
    )

    // Blocks that face to the right of player
    private val dirRight: Setting<MutableList<Block>> = sgDirectional.add(
        BlockListSetting.Builder()
            .name("directional-right")
            .description("Blocks that should face to the right of player.")
            .visible { directionProtection.get() }
            .build()
    )

    // Blocks that face upward from player
    private val dirUp: Setting<MutableList<Block>> = sgDirectional.add(
        BlockListSetting.Builder()
            .name("directional-up")
            .description("Blocks that should face upward from player.")
            .visible { directionProtection.get() }
            .build()
    )

    // Blocks that face downward from player
    private val dirDown: Setting<MutableList<Block>> = sgDirectional.add(
        BlockListSetting.Builder()
            .name("directional-down")
            .description("Blocks that should face downward from player.")
            .visible { directionProtection.get() }
            .build()
    )


    private val precisePlacement: Setting<Boolean> = sgClickFace.add(
        BoolSetting.Builder()
            .name("precise-placement")
            .description(
                "Use precise face-based placement for stairs, slabs, trapdoors etc. (ignores player orientation completely)"
            )
            .defaultValue(true)
            .build()
    )
    private val freeFaceForDefaultTorch: Setting<Boolean> = sgClickFace.add(
        BoolSetting.Builder()
            .name("free-face-of-default-torch")
            .description("Allow placing default torches without precise placement.")
            .defaultValue(false)
            .visible { precisePlacement.get() }
            .build()
    )


    // Precise Placement Face Lists
    private val preciseForward: Setting<MutableList<Block>> = sgClickFace.add(
        BlockListSetting.Builder()
            .name("precise-facing-forward")
            .description("Blocks for precise placement facing forward.")
            .defaultValue( // 火把
                Blocks.TORCH,
                Blocks.SOUL_TORCH,
                Blocks.REDSTONE_TORCH,  // 梯子
                Blocks.LADDER,  // 拉杆
                Blocks.LEVER,  // 按钮

                Blocks.STONE_BUTTON,
                Blocks.OAK_BUTTON,
                Blocks.SPRUCE_BUTTON,
                Blocks.BIRCH_BUTTON,
                Blocks.JUNGLE_BUTTON,
                Blocks.ACACIA_BUTTON,
                Blocks.DARK_OAK_BUTTON,
                Blocks.CRIMSON_BUTTON,
                Blocks.WARPED_BUTTON,
                Blocks.MANGROVE_BUTTON,
                Blocks.BAMBOO_BUTTON,
                Blocks.CHERRY_BUTTON,
                Blocks.POLISHED_BLACKSTONE_BUTTON,  // 潜影盒
                Blocks.SHULKER_BOX,
                Blocks.WHITE_SHULKER_BOX,
                Blocks.ORANGE_SHULKER_BOX,
                Blocks.MAGENTA_SHULKER_BOX,
                Blocks.LIGHT_BLUE_SHULKER_BOX,
                Blocks.YELLOW_SHULKER_BOX,
                Blocks.LIME_SHULKER_BOX,
                Blocks.PINK_SHULKER_BOX,
                Blocks.GRAY_SHULKER_BOX,
                Blocks.LIGHT_GRAY_SHULKER_BOX,
                Blocks.CYAN_SHULKER_BOX,
                Blocks.PURPLE_SHULKER_BOX,
                Blocks.BLUE_SHULKER_BOX,
                Blocks.BROWN_SHULKER_BOX,
                Blocks.GREEN_SHULKER_BOX,
                Blocks.RED_SHULKER_BOX,
                Blocks.BLACK_SHULKER_BOX,  // 铁活板门
                Blocks.IRON_TRAPDOOR,  // 木活板门
                Blocks.OAK_TRAPDOOR,
                Blocks.SPRUCE_TRAPDOOR,
                Blocks.BIRCH_TRAPDOOR,
                Blocks.JUNGLE_TRAPDOOR,
                Blocks.ACACIA_TRAPDOOR,
                Blocks.DARK_OAK_TRAPDOOR,
                Blocks.CRIMSON_TRAPDOOR,
                Blocks.PALE_OAK_TRAPDOOR,
                Blocks.WARPED_TRAPDOOR,
                Blocks.MANGROVE_TRAPDOOR,
                Blocks.BAMBOO_TRAPDOOR,
                Blocks.CHERRY_TRAPDOOR,  // 铜活板门
                Blocks.COPPER_TRAPDOOR,
                Blocks.EXPOSED_COPPER_TRAPDOOR,
                Blocks.WEATHERED_COPPER_TRAPDOOR,
                Blocks.OXIDIZED_COPPER_TRAPDOOR,
                Blocks.WAXED_COPPER_TRAPDOOR,
                Blocks.WAXED_EXPOSED_COPPER_TRAPDOOR,
                Blocks.WAXED_WEATHERED_COPPER_TRAPDOOR,
                Blocks.WAXED_OXIDIZED_COPPER_TRAPDOOR,  //单头棍子

                Blocks.END_ROD,
                Blocks.LIGHTNING_ROD,  // 双头棍子

                Blocks.HAY_BLOCK,
                Blocks.IRON_CHAIN,  // 原木
                Blocks.OAK_LOG,
                Blocks.SPRUCE_LOG,
                Blocks.BIRCH_LOG,
                Blocks.JUNGLE_LOG,
                Blocks.ACACIA_LOG,
                Blocks.DARK_OAK_LOG,
                Blocks.CRIMSON_STEM,
                Blocks.PALE_OAK_LOG,
                Blocks.WARPED_STEM,
                Blocks.MANGROVE_LOG,
                Blocks.CHERRY_LOG,  // 去皮原木
                Blocks.STRIPPED_OAK_LOG,
                Blocks.STRIPPED_SPRUCE_LOG,
                Blocks.STRIPPED_BIRCH_LOG,
                Blocks.STRIPPED_JUNGLE_LOG,
                Blocks.STRIPPED_ACACIA_LOG,
                Blocks.STRIPPED_DARK_OAK_LOG,
                Blocks.STRIPPED_CRIMSON_STEM,
                Blocks.STRIPPED_WARPED_STEM,
                Blocks.STRIPPED_MANGROVE_LOG,
                Blocks.STRIPPED_CHERRY_LOG,
                Blocks.STRIPPED_PALE_OAK_LOG,  // 木头
                Blocks.OAK_WOOD,
                Blocks.SPRUCE_WOOD,
                Blocks.BIRCH_WOOD,
                Blocks.JUNGLE_WOOD,
                Blocks.ACACIA_WOOD,
                Blocks.DARK_OAK_WOOD,
                Blocks.CRIMSON_HYPHAE,
                Blocks.WARPED_HYPHAE,
                Blocks.MANGROVE_WOOD,
                Blocks.CHERRY_WOOD,
                Blocks.PALE_OAK_WOOD,  // 去皮木头
                Blocks.STRIPPED_OAK_WOOD,
                Blocks.STRIPPED_SPRUCE_WOOD,
                Blocks.STRIPPED_BIRCH_WOOD,
                Blocks.STRIPPED_JUNGLE_WOOD,
                Blocks.STRIPPED_ACACIA_WOOD,
                Blocks.STRIPPED_DARK_OAK_WOOD,
                Blocks.STRIPPED_CRIMSON_HYPHAE,
                Blocks.STRIPPED_WARPED_HYPHAE,
                Blocks.STRIPPED_MANGROVE_WOOD,
                Blocks.STRIPPED_CHERRY_WOOD,
                Blocks.STRIPPED_PALE_OAK_WOOD,  // 其他AXIS块
                Blocks.BAMBOO_BLOCK,
                Blocks.STRIPPED_BAMBOO_BLOCK,
                Blocks.QUARTZ_PILLAR,
                Blocks.BONE_BLOCK,
                Blocks.PURPUR_PILLAR,
                Blocks.BASALT,
                Blocks.POLISHED_BASALT,  // 蛙鸣灯
                Blocks.OCHRE_FROGLIGHT,
                Blocks.PEARLESCENT_FROGLIGHT,
                Blocks.VERDANT_FROGLIGHT


            )
            .visible { precisePlacement.get() }
            .build()
    )

    private val preciseBackward: Setting<MutableList<Block>> = sgClickFace.add(
        BlockListSetting.Builder()
            .name("precise-facing-backward")
            .description("Blocks for precise placement facing backward.")
            .defaultValue( // 钟
                Blocks.BELL,  // 漏斗
                Blocks.HOPPER,  // 双头棍子
                Blocks.HAY_BLOCK,
                Blocks.IRON_CHAIN,  // 原木
                Blocks.OAK_LOG,
                Blocks.SPRUCE_LOG,
                Blocks.BIRCH_LOG,
                Blocks.JUNGLE_LOG,
                Blocks.ACACIA_LOG,
                Blocks.DARK_OAK_LOG,
                Blocks.CRIMSON_STEM,
                Blocks.PALE_OAK_LOG,
                Blocks.WARPED_STEM,
                Blocks.MANGROVE_LOG,
                Blocks.CHERRY_LOG,  // 去皮原木
                Blocks.STRIPPED_OAK_LOG,
                Blocks.STRIPPED_SPRUCE_LOG,
                Blocks.STRIPPED_BIRCH_LOG,
                Blocks.STRIPPED_JUNGLE_LOG,
                Blocks.STRIPPED_ACACIA_LOG,
                Blocks.STRIPPED_DARK_OAK_LOG,
                Blocks.STRIPPED_CRIMSON_STEM,
                Blocks.STRIPPED_WARPED_STEM,
                Blocks.STRIPPED_MANGROVE_LOG,
                Blocks.STRIPPED_CHERRY_LOG,
                Blocks.STRIPPED_PALE_OAK_LOG,  // 木头
                Blocks.OAK_WOOD,
                Blocks.SPRUCE_WOOD,
                Blocks.BIRCH_WOOD,
                Blocks.JUNGLE_WOOD,
                Blocks.ACACIA_WOOD,
                Blocks.DARK_OAK_WOOD,
                Blocks.CRIMSON_HYPHAE,
                Blocks.WARPED_HYPHAE,
                Blocks.MANGROVE_WOOD,
                Blocks.CHERRY_WOOD,
                Blocks.PALE_OAK_WOOD,  // 去皮木头
                Blocks.STRIPPED_OAK_WOOD,
                Blocks.STRIPPED_SPRUCE_WOOD,
                Blocks.STRIPPED_BIRCH_WOOD,
                Blocks.STRIPPED_JUNGLE_WOOD,
                Blocks.STRIPPED_ACACIA_WOOD,
                Blocks.STRIPPED_DARK_OAK_WOOD,
                Blocks.STRIPPED_CRIMSON_HYPHAE,
                Blocks.STRIPPED_WARPED_HYPHAE,
                Blocks.STRIPPED_MANGROVE_WOOD,
                Blocks.STRIPPED_CHERRY_WOOD,
                Blocks.STRIPPED_PALE_OAK_WOOD,  // 其他AXIS块
                Blocks.BAMBOO_BLOCK,
                Blocks.STRIPPED_BAMBOO_BLOCK,
                Blocks.QUARTZ_PILLAR,
                Blocks.BONE_BLOCK,
                Blocks.PURPUR_PILLAR,
                Blocks.BASALT,
                Blocks.POLISHED_BASALT,  // 蛙鸣灯
                Blocks.OCHRE_FROGLIGHT,
                Blocks.PEARLESCENT_FROGLIGHT,
                Blocks.VERDANT_FROGLIGHT

            )
            .visible { precisePlacement.get() }
            .build()
    )

    private val preciseLeft: Setting<MutableList<Block>> = sgClickFace.add(
        BlockListSetting.Builder()
            .name("precise-facing-left")
            .description("Blocks for precise placement facing left.")
            .defaultValue(*wallHangingSigns.toTypedArray())
            .visible { precisePlacement.get() }
            .build()
    )
    private val preciseRight: Setting<MutableList<Block>> = sgClickFace.add(
        BlockListSetting.Builder()
            .name("precise-facing-right")
            .description("Blocks for precise placement facing right.")
            .defaultValue(*wallHangingSigns.toTypedArray())
            .visible { precisePlacement.get() }
            .build()
    )

    fun TryPlaceBlock(required: BlockState, pos: BlockPos): Boolean {
        val player = mc.player ?: return false
        val world = mc.world ?: return false
        // 检查点
        if (!required.canPlaceAt(world, pos)) return false //没有墙体支撑导致会实际放置状态fallback
        if (!BlockUtils.canPlace(pos) || !required.isMultiStructurePlacementAllowed) return false
        
        // 检查面
        val block = required.block
        val isPlaceAllowedFromPlayerRotation by lazy { required.isPlaceAllowedFromPlayerRotation }
        val posCenterVisible by lazy { pos.Center.isVisible }
        val airPlaceAllowed by lazy { airPlace.get() && block !in airplaceBlacklist.get() }
        for (face in Direction.entries) {

            var tempHitPos = Vec3d(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5)
            // 特殊砖:根据状态属性/点击面有 不同的 点击面保护和视角保护的变化/点击点偏移/面不能点

            var disableDirectionProtection = false
            var disableFaceProtection = false
            if (block is WallTorchBlock || block is WallRedstoneTorchBlock) {//墙火把 面不同禁用不同保护/面不能点
                when (face) {
                    Direction.UP -> continue
                    Direction.DOWN -> {
                        // 方向取决于玩家，禁用点击面保护
                        disableFaceProtection = true
                    }

                    else -> {//侧面
                        //方向取决于点击面，禁用方向保护
                        disableDirectionProtection = true
                    }
                }
            } else if (block is TorchBlock || block is RedstoneTorchBlock) {// 直立式火把  面不能点
                if (!freeFaceForDefaultTorch.get() && face != Direction.UP) {//只能放在邻居上面
                    continue
                }
            } else if (block is TrapdoorBlock) { //活板门 面不同禁用不同保护/点击点偏移/面不能点
                val blockHalf = required.get<BlockHalf>(Properties.BLOCK_HALF)
                when {
                    (blockHalf == BlockHalf.TOP && face == Direction.UP)
                            || (blockHalf == BlockHalf.BOTTOM && face == Direction.DOWN) -> continue// 半砖类型会错误
                    face == Direction.UP || face == Direction.DOWN -> {
                        //方向依据玩家，禁用点击面保护
                        disableFaceProtection = true
                    }

                    else -> {//侧面方向取决于点击面，禁用方向保护，设置半砖偏移
                        disableDirectionProtection = true
                        if (blockHalf == BlockHalf.TOP) {
                            tempHitPos = tempHitPos.add(0.0, 0.25, 0.0)
                        } else {
                            tempHitPos = tempHitPos.add(0.0, -0.25, 0.0)
                        }
                    }
                }
            } else if (block is StairsBlock) { //楼梯 点击点偏移/面不能点
                val blockHalf = required.get<BlockHalf>(Properties.BLOCK_HALF)
                when {
                    (blockHalf == BlockHalf.TOP && face == Direction.UP)
                            || (blockHalf == BlockHalf.BOTTOM && face == Direction.DOWN) -> continue// 半砖类型会错误
                    face == Direction.UP || face == Direction.DOWN -> {
                    }

                    else -> {//侧面设置半砖偏移
                        if (blockHalf == BlockHalf.TOP) {
                            tempHitPos = tempHitPos.add(0.0, 0.25, 0.0)
                        } else {
                            tempHitPos = tempHitPos.add(0.0, -0.25, 0.0)
                        }
                    }
                }
            } else if (block is SlabBlock) { //半砖 点击点偏移/面不能点
                val slabTpye = required.get<SlabType>(Properties.SLAB_TYPE)
                when {
                    (slabTpye == SlabType.TOP && face == Direction.UP)
                            || (slabTpye == SlabType.BOTTOM && face == Direction.DOWN) -> continue // 半砖类型会错误
                    face == Direction.UP || face == Direction.DOWN -> {
                    }

                    else -> {//侧面设置半砖偏移
                        when (slabTpye) {
                            SlabType.TOP -> tempHitPos = tempHitPos.add(0.0, 0.25, 0.0)
                            SlabType.BOTTOM -> tempHitPos = tempHitPos.add(0.0, -0.25, 0.0)
                            SlabType.DOUBLE -> {}
                        }
                    }
                }
            } else if (block is HangingSignBlock) {//悬挂告示牌 面不能点
                val attached = required.get<Boolean>(Properties.ATTACHED)
                if (attached) {
                    if (!(face == Direction.DOWN && player.isSneaking)) continue
                } else {
                    if (!(face == Direction.DOWN && !player.isSneaking)) continue
                }
            } else if (block is SignBlock) {

            } else if (block is WallSignBlock) {

            } else if (block is SkullBlock) {

            } else if (block is WallSkullBlock) {

            } else if (block is BannerBlock) {

            } else if (block is WallBannerBlock) {


            } else if (Properties.ATTACHMENT in required) { //钟既 属性不同禁用不同保护/面不能点
                when (required.get<Attachment>(Properties.ATTACHMENT)) {
                    Attachment.FLOOR -> {
                        if (face != Direction.UP) continue  // 只能放在邻居上方

                        // 方向取决于玩家，禁用点击面保护
                        disableFaceProtection = true
                    }

                    Attachment.CEILING -> {
                        if (face != Direction.DOWN) continue  // 只能放在邻居下方

                        // 没有方向，禁用全部
                        disableFaceProtection = true
                        disableDirectionProtection = true
                    }

                    else -> {
                        if (face == Direction.UP || face == Direction.DOWN) continue  // 只能放在邻居四周

                        //放置在四周方向取决于点击面，禁用方向保护
                        disableDirectionProtection = true
                    }
                }
            } else if (Properties.HANGING in required) { //灯笼 面不能点
                if (required.get<Boolean>(Properties.HANGING)) {
                    // 吊着的
                    if (face != Direction.DOWN) continue  // 只能放在邻居下方
                } else {
                    // 不吊着的
                    if (face == Direction.DOWN) continue  // 不能放在邻居下方
                }
            } else if (Properties.BLOCK_FACE in required) { //拉杆 按钮 磨石 block is WallMountedBlock 属性不同禁用不同保护/面不能点
                when (required.get<BlockFace>(Properties.BLOCK_FACE)) {
                    BlockFace.FLOOR -> {
                        if (face != Direction.UP) continue  // 只能放在邻居上方

                        // 方向取决于玩家，禁用点击面保护
                        disableFaceProtection = true
                    }

                    BlockFace.CEILING -> {
                        if (face != Direction.DOWN) continue  // 只能放在邻居下方

                        // 方向取决于玩家，禁用点击面保护
                        disableFaceProtection = true
                    }

                    BlockFace.WALL -> {
                        if (face == Direction.UP || face == Direction.DOWN) continue  // 只能放在邻居四周

                        //方向取决于点击面，禁用方向保护
                        disableDirectionProtection = true
                    }
                }
            }

            if (airPlaceAllowed) {
                disableDirectionProtection = false
            }

            if (directionProtection.get() && !disableDirectionProtection
                && !isPlaceAllowedFromPlayerRotation
            ) {
                continue
            }
            if (precisePlacement.get() && !disableFaceProtection
                && !required.isPlaceAllowedFromClickFace(face)
            ) {
                continue
            }

            // 确定要点一个邻居面不会导致状态错误的话，计算邻居和点击位置(面中心+侧方半砖偏移)

            val neighbour: BlockPos

            if (airPlaceAllowed) {
                neighbour = pos
            } else {
                val OppositeFace = face.opposite
                neighbour = pos.offset(OppositeFace)
                if (!required.canPlaceAgainst(neighbour, face)) {
                    continue
                }
                tempHitPos = tempHitPos.add(
                    OppositeFace.offsetX * 0.5, OppositeFace.offsetY * 0.5,
                    OppositeFace.offsetZ * 0.5
                )
            }
            val hitPos = tempHitPos

            // 确定了face 邻居坐标 点击位置

            // 距离保护
            if (hitPos.distanceTo(player.eyePos) > PlayerHandDistance) {
                continue
            }

            // 筛出不安全的面
            when (safetyPlaceFaceMode.get()) {
                SafetyFaceMode.PlayerRotation -> BlockUtils.getDirection(pos)
                SafetyFaceMode.PlayerPosition -> pos.PickAFaceFromPlayerPosition(player)
                SafetyFaceMode.None -> null
            }?.let {
                if (face != it) continue
            }

            if (!placeThroughWall.get()) {
                if (airPlaceAllowed) {
                    if (!posCenterVisible) {
                        continue
                    }
                } else {
                    if (!(neighbour to face).isVisible) {
                        continue
                    }
                }
            }
            if (onlyPlaceOnLookFace.get() && !player.RotationInTheFaceOfBlock(neighbour, face)) {
                continue
            }
            var item = required.block.asItem()
            if (dirtgrass.get() && item === Items.GRASS_BLOCK) item = Items.DIRT
            return player.switchItem(item, returnHand.get()) {
                place(BlockHitResult(hitPos, face, neighbour, false))
            }
        }
        return false
    }

    private fun BlockState.isPlaceAllowedFromClickFace(clickFace: Direction): Boolean {
        //方块能判断方向
        val requiredDirection = this.ATagFaceOf6 ?: return true

        val block = this.block
        val inListForward = block in preciseForward.get()
        val inListBackward = block in preciseBackward.get()
        val inListLeft = block in preciseLeft.get()
        val inListRight = block in preciseRight.get()
        if (!(inListForward || inListBackward || inListLeft || inListRight)) return true
        return when (requiredDirection) {
            clickFace -> inListForward
            clickFace.opposite -> inListBackward
            clickFace.Left -> inListLeft
            clickFace.Right -> inListRight
            else -> false
        }

    }

    private val BlockState.isPlaceAllowedFromPlayerRotation: Boolean
        get() {
            val block = this.block
            val inListUp = block in dirUp.get()
            val inListDown = block in dirDown.get()
            val inListForward = block in dirForward.get()
            val inListBackward = block in dirBackward.get()
            val inListLeft = block in dirLeft.get()
            val inListRight = block in dirRight.get()
            if (!(inListUp || inListDown || inListForward || inListBackward || inListLeft || inListRight)) return true
            val 容差 = angleRangeForDirectionProtection.get().toFloat()


            if (Properties.ROTATION in this) {
                val YawInt16 = mc.player?.YawInt16By(容差 / 4) ?: return false
                val BlockInt16 = this.get(Properties.ROTATION)
                return when (BlockInt16) {
                    YawInt16 -> inListForward
                    YawInt16.opposite -> inListBackward
                    YawInt16.Left -> inListLeft
                    YawInt16.Right -> inListRight
                    else -> false
                }
            }

            val requiredDirection = this.ATagFaceOf6 ?: return true
            val 六向砖 = inListUp || inListDown
            val playerPitchDirection = mc.player?.PitchDirectionBy(容差)
            val playerYawDirection = mc.player?.YawDirectionBy(容差)

            if (六向砖 && (playerPitchDirection == Direction.UP || playerPitchDirection == Direction.DOWN)) {
                if (Properties.ORIENTATION !in this || playerYawDirection?.let {
                        this.get(Properties.ORIENTATION).name.endsWith(it.name)
                    } == true
                ) {
                    return when (requiredDirection) {
                        playerPitchDirection -> inListUp
                        playerPitchDirection.opposite -> inListDown
                        else -> false
                    }
                }
            } else if (playerYawDirection != null && !(六向砖 && playerPitchDirection == null)) {
                return when (requiredDirection) {
                    playerYawDirection -> inListForward
                    playerYawDirection.opposite -> inListBackward
                    playerYawDirection.Left -> inListLeft
                    playerYawDirection.Right -> inListRight
                    else -> false
                }
            }
            return false
        }


    private fun BlockState.canPlaceAgainst(neighbourPos: BlockPos, neighbourFace: Direction): Boolean {
        val world = mc.world ?: return false
        val player = mc.player ?: return false
        val neighbour = world.getBlockState(neighbourPos)
        val neighbourBlock = neighbour.block
        return !neighbour.isAir && neighbour.fluidState.isEmpty//有砖
                // 不出GUI
                && (!BlockUtils.isClickable(neighbourBlock) || player.isSneaking)
                // 不在额外黑名单
                && !(enableBlacklist.get() && neighbourBlock in blacklist.get())
                // 在白名单组合
                && (neighbour.isBlockShapeFullCube
                || neighbourBlock === Blocks.GLASS
                || neighbourBlock is StainedGlassBlock
                || neighbourBlock is StairsBlock
                || (enableAddList.get() && neighbourBlock in addList.get())
                //不会重叠的半砖
                || (neighbourBlock is SlabBlock
                && (this.block !== neighbour.block //类型不同不会融合
                || neighbour.get<SlabType>(SlabBlock.TYPE) == SlabType.DOUBLE //邻居双层不会融合
                || !(//同类型,邻居单层半砖 附带不会融合约束
                //从上向下放到半砖底 会融合
                (neighbour.get<SlabType>(SlabBlock.TYPE) == SlabType.BOTTOM && neighbourFace == Direction.UP)
                        //从下向上放到半砖顶 会融合
                        || (neighbour.get<SlabType>(SlabBlock.TYPE) == SlabType.TOP && neighbourFace == Direction.DOWN)
                        //两半砖顶底不同时 从侧面放置 会融合
                        || (neighbourFace != Direction.UP && neighbourFace != Direction.DOWN //侧面放置
                        && neighbour.get<SlabType>(SlabBlock.TYPE) != this.get<SlabType>(SlabBlock.TYPE)) //顶底不同
                ))))
    }


    private fun place(blockHitResult: BlockHitResult): Boolean {
        val result = mc.interactionManager?.interactBlock(mc.player, Hand.MAIN_HAND, blockHitResult)
        if (result == ActionResult.SUCCESS) {
            mc.player?.swing(swingMode = swingHand.get())
            return true
        }
        return false
    }


    private val BlockState.isMultiStructurePlacementAllowed: Boolean
        get() {
            if (Properties.BED_PART in this) {
                val bedPart = this.get<BedPart>(Properties.BED_PART)
                if (bedPart == BedPart.HEAD) {
                    return false
                }
            }

            if (Properties.DOUBLE_BLOCK_HALF in this) {
                val doubleBlockHalf = this.get<DoubleBlockHalf>(Properties.DOUBLE_BLOCK_HALF)
                if (doubleBlockHalf == DoubleBlockHalf.UPPER) {
                    return false
                }
            }
            return true
        }

}
