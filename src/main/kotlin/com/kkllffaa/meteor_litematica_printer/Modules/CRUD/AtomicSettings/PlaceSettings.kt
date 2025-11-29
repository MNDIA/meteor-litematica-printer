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

    private val BlacklistforFullCube: Setting<Boolean> = sgNeighbor.add(
        BoolSetting.Builder()
            .name("Black-list-for-FullCube")
            .description("Enable blacklist for neighbor blocks.")
            .defaultValue(false)
            .build()
    )

    private val blacklist: Setting<MutableList<Block>> = sgNeighbor.add(
        BlockListSetting.Builder()
            .name("blacklist")
            .description("Blocks that cannot be placed against.(CollisionFullCube but cannot be clicked)")
            .defaultValue(
                //碰撞箱完整但是不能放的
                // *潜影盒.toTypedArray(),
            )
            .visible { BlacklistforFullCube.get() }
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
            .description("Allow placing in the air.")
            .defaultValue(true)
            .build()
    )
    private val airplaceBlacklist: Setting<MutableList<Block>> = sgGeneral.add(
        BlockListSetting.Builder()
            .name("airplace-blacklist")
            .description("Blocks that cannot be placed in airplace.")
            .defaultValue(

                // Blocks.TRIPWIRE_HOOK, // 绊线钩
                *墙上H告示牌.toTypedArray(),

                // *地面火把.toTypedArray(),
                // *墙上火把.toTypedArray(),

                // *地面告示牌.toTypedArray(),
                // *墙上告示牌.toTypedArray(),

                // *墙上旗帜.toTypedArray(),
                // *地面旗帜.toTypedArray(),

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
            .name("safetyPlace-mode")
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
            .defaultValue(SignColorMode.反三)
            .build()
    )


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

    private val YawForward: Setting<MutableList<Block>> = sgDirectional.add(
        BlockListSetting.Builder()
            .name("yaw-forward")
            .description("Blocks that should face the same direction as player.")
            .defaultValue(
                Blocks.LEVER, // 拉杆
                Blocks.CALIBRATED_SCULK_SENSOR, // 校准潜影传感器
                Blocks.OBSERVER,// 侦测器
                Blocks.GRINDSTONE,// 砂轮
                Blocks.CAMPFIRE, // 篝火
                Blocks.SOUL_CAMPFIRE, // 蓝篝火
                Blocks.BELL, // 钟
                Blocks.DECORATED_POT, // 装饰花盆
                *地面头颅.toTypedArray(),
                *按钮.toTypedArray(),
                *铁轨.toTypedArray(),
                *栅栏门.toTypedArray(),
                *楼梯.toTypedArray(),
                *门.toTypedArray(),//TODO: 旁边砖块个数和点击偏移决定左右门
                *床.toTypedArray(),
            )
            .visible { directionProtection.get() }
            .build()
    )

    private val YawBackward: Setting<MutableList<Block>> = sgDirectional.add(
        BlockListSetting.Builder()
            .name("yaw-backward")
            .description("Blocks that should face to player.")
            .defaultValue(
                Blocks.REPEATER,// 中继器
                Blocks.COMPARATOR,// 比较器
                Blocks.TRIPWIRE_HOOK, // 绊线钩
                Blocks.LECTERN, // 讲台
                Blocks.PISTON, // 活塞
                Blocks.STICKY_PISTON, // 粘性活塞
                Blocks.DISPENSER,// 发射器
                Blocks.DROPPER,//投掷器
                Blocks.CRAFTER, //合成器
                Blocks.BARREL,// 木桶
                Blocks.CHISELED_BOOKSHELF, // 雕文书架
                Blocks.STONECUTTER,// 切石机
                Blocks.LOOM, //织布机
                Blocks.FURNACE, // 炉子
                Blocks.SMOKER, // 食物炉子
                Blocks.BLAST_FURNACE, // 矿炉子
                Blocks.SOUL_SOIL, // 失水恶魂
                Blocks.CARVED_PUMPKIN,   // 雕刻南瓜
                Blocks.JACK_O_LANTERN,   // 发光南瓜
                Blocks.BEE_NEST,// 蜂巢
                Blocks.BEEHIVE,// 蜂箱
                Blocks.BIG_DRIPLEAF,// 大滴水叶
                Blocks.END_PORTAL_FRAME, // 末地传送门框架
                Blocks.VAULT, // 宝库
                *地面旗帜.toTypedArray(),
                *墙上旗帜.toTypedArray(),
                *墙上头颅.toTypedArray(),
                *墙上火把.toTypedArray(),
                *墙上告示牌.toTypedArray(),
                *地面告示牌.toTypedArray(),
                *天花板H告示牌.toTypedArray(),
                *墙上H告示牌.toTypedArray(),
                *开合箱子.toTypedArray(),
                *三框展示架.toTypedArray(),
                *活板门.toTypedArray(),
                *铁轨.toTypedArray(),
                *栅栏门.toTypedArray(),
                *铜雕像.toTypedArray(),
            )
            .visible { directionProtection.get() }
            .build()
    )

    private val YawLeft: Setting<MutableList<Block>> = sgDirectional.add(
        BlockListSetting.Builder()
            .name("yaw-left")
            .description("Blocks that should face to the left of player.")
            .visible { directionProtection.get() }
            .build()
    )

    private val YawRight: Setting<MutableList<Block>> = sgDirectional.add(
        BlockListSetting.Builder()
            .name("yaw-right")
            .description("Blocks that should face to the right of player.")
            .defaultValue(*铁砧.toTypedArray())
            .visible { directionProtection.get() }
            .build()
    )

    private val PitchForward: Setting<MutableList<Block>> = sgDirectional.add(
        BlockListSetting.Builder()
            .name("pitch-forward")
            .description("Blocks that should face the same direction as player when UpDown")
            .defaultValue(
                Blocks.OBSERVER,// 侦测器
            )
            .visible { directionProtection.get() }
            .build()
    )

    private val PitchBackward: Setting<MutableList<Block>> = sgDirectional.add(
        BlockListSetting.Builder()
            .name("pitch-backward")
            .description("Blocks that should face to player when UpDown")
            .defaultValue(
                Blocks.PISTON, // 活塞
                Blocks.STICKY_PISTON, // 粘性活塞
                Blocks.DISPENSER,// 发射器
                Blocks.DROPPER,//投掷器
                Blocks.CRAFTER, //合成器
                Blocks.BARREL,// 木桶
            )
            .visible { directionProtection.get() }
            .build()
    )


    private val clickProtection: Setting<Boolean> = sgClickFace.add(
        BoolSetting.Builder()
            .name("click-protection")
            .description("Only place directional blocks when click-face is correct direction.")
            .defaultValue(true)
            .build()
    )

    private val freeFaceForDefaultTorch: Setting<Boolean> = sgClickFace.add(
        BoolSetting.Builder()
            .name("free-face-of-default-torch")
            .description("Allow placing default torches without precise placement.")
            .defaultValue(false)
            .visible { clickProtection.get() }
            .build()
    )

    private val clickForward: Setting<MutableList<Block>> = sgClickFace.add(
        BlockListSetting.Builder()
            .name("click-forward")
            .description("Blocks that should face the same direction as click face.")
            .defaultValue(
                Blocks.LADDER,   // 梯子
                Blocks.LEVER,   // 拉杆
                Blocks.GRINDSTONE,  // 砂轮
                Blocks.END_ROD, //末地烛 TODO:末地烛放在末地烛上反向特例
                Blocks.TRIPWIRE_HOOK, // 绊线钩
                *按钮.toTypedArray(),
                *墙上旗帜.toTypedArray(),
                *墙上头颅.toTypedArray(),
                *墙上火把.toTypedArray(),
                *墙上告示牌.toTypedArray(),
                *潜影盒.toTypedArray(),
                *活板门.toTypedArray(),
                *避雷针.toTypedArray(),

                *链.toTypedArray(),
                *原木log.toTypedArray(),
                *去皮原木log.toTypedArray(),
                *木块wood.toTypedArray(),
                *去皮木块wood.toTypedArray(),
                *个别AxisBlocks.toTypedArray(),
            )
            .visible { clickProtection.get() }
            .build()
    )

    private val clickBackward: Setting<MutableList<Block>> = sgClickFace.add(
        BlockListSetting.Builder()
            .name("click-backward")
            .description("Blocks that should face to click face(back).")
            .defaultValue(
                Blocks.BELL,  // 钟
                Blocks.HOPPER,    // 漏斗
                *链.toTypedArray(),
                *原木log.toTypedArray(),
                *去皮原木log.toTypedArray(),
                *木块wood.toTypedArray(),
                *去皮木块wood.toTypedArray(),
                *个别AxisBlocks.toTypedArray(),
            )
            .visible { clickProtection.get() }
            .build()
    )

    private val clickLeft: Setting<MutableList<Block>> = sgClickFace.add(
        BlockListSetting.Builder()
            .name("click-left")
            .description("Blocks for precise placement facing left.")
            .defaultValue(*墙上H告示牌.toTypedArray())
            .visible { clickProtection.get() }
            .build()
    )

    private val clickRight: Setting<MutableList<Block>> = sgClickFace.add(
        BlockListSetting.Builder()
            .name("click-right")
            .description("Blocks for precise placement facing right.")
            .defaultValue(*墙上H告示牌.toTypedArray())
            .visible { clickProtection.get() }
            .build()
    )


    private fun BlockState.isPlaceAllowedFromClickFace(clickFace: Direction): Boolean {
        //方块能判断方向
        val requiredDirection = this.ATagFaceOf6 ?: return true

        val block = this.block
        val inListForward = block in clickForward.get()
        val inListBackward = block in clickBackward.get()
        val inListLeft = block in clickLeft.get()
        val inListRight = block in clickRight.get()
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
            val inListUp = block in PitchForward.get()
            val inListDown = block in PitchBackward.get()
            val inListForward = block in YawForward.get()
            val inListBackward = block in YawBackward.get()
            val inListLeft = block in YawLeft.get()
            val inListRight = block in YawRight.get()
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
        val neighbourisBlockCollisionFullCube by lazy { neighbour.isBlockCollisionFullCube }
        return !neighbour.isAir && neighbour.fluidState.isEmpty//有砖
                // 不出GUI
                && (!BlockUtils.isClickable(neighbourBlock) || player.isSneaking)
                // 特例
                && !(this.block is WallHangingSignBlock && !neighbourisBlockCollisionFullCube)
                // 不在额外黑名单
                && !(BlacklistforFullCube.get() && neighbourBlock in blacklist.get())
                // 在白名单组合
                && (neighbourisBlockCollisionFullCube
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
        val enableAirPlace by lazy { airPlace.get() && block !in airplaceBlacklist.get() }
        for (face in Direction.entries) {
            var tempHitPos = Vec3d(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5)
            // 特殊砖:根据状态属性/点击面有 不同的 点击面保护和视角保护的变化/点击点偏移/面不能点
            var disableDirectionProtection = false
            var disableFaceProtection = false
            var disableAirPlace = false
            if (block is WallTorchBlock || block is WallRedstoneTorchBlock) {//墙火把 面不同禁用不同保护/面不能点
                when (face) {
                    Direction.UP -> continue
                    Direction.DOWN -> disableFaceProtection = true
                    else -> disableDirectionProtection = true
                }
            } else if (block is TorchBlock || block is RedstoneTorchBlock) {// 直立式火把  面不能点
                if (!freeFaceForDefaultTorch.get() && face != Direction.UP) continue
            } else if (block is TrapdoorBlock) { //活板门 面不同禁用不同保护/点击点偏移/面不能点
                val blockHalf = required.get<BlockHalf>(Properties.BLOCK_HALF)
                when {
                    (blockHalf == BlockHalf.TOP && face == Direction.UP)
                            || (blockHalf == BlockHalf.BOTTOM && face == Direction.DOWN) -> continue// 半砖类型会错误
                    face == Direction.UP || face == Direction.DOWN -> {
                        disableFaceProtection = true//方向依据玩家
                    }

                    else -> {
                        if (blockHalf == BlockHalf.TOP) {
                            tempHitPos = tempHitPos.add(0.0, 0.25, 0.0)
                        } else {
                            disableAirPlace = true
                            tempHitPos = tempHitPos.add(0.0, -0.25, 0.0)
                        }
                        disableDirectionProtection = true//侧面方向取决于点击面
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
                            disableAirPlace = true
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
                            SlabType.BOTTOM -> {
                                disableAirPlace = true
                                tempHitPos = tempHitPos.add(0.0, -0.25, 0.0)
                            }

                            SlabType.DOUBLE -> {}
                        }
                    }
                }
            } else if (block is WallHangingSignBlock) {//墙面悬挂告示牌 面不能点
                if (face == Direction.DOWN || face == Direction.UP) continue
            } else if (block is HangingSignBlock) {//天花板悬挂告示牌 面不能点
                if (face != Direction.DOWN) continue
                val attached = required.get<Boolean>(Properties.ATTACHED)
                if (attached != player.isSneaking) continue
            } else if (block is WallSignBlock || block is WallSkullBlock || block is WallBannerBlock) {
                when (face) {
                    Direction.UP -> continue
                    Direction.DOWN -> {//TODO:确保fallback的前提是目标侧墙有砖
                        disableFaceProtection = true // 方向取决于玩家
                    }

                    else -> {
                        disableDirectionProtection = true //方向取决于点击面
                    }
                }
            } else if (block is SignBlock || block is SkullBlock || block is BannerBlock) {
                if (face != Direction.UP) continue
            } else if (Properties.ATTACHMENT in required) { //钟既 属性不同禁用不同保护/面不能点
                when (required.get<Attachment>(Properties.ATTACHMENT)) {
                    Attachment.FLOOR -> {
                        if (face != Direction.UP) continue  // 只能放在邻居上方
                        disableFaceProtection = true  // 方向取决于玩家
                    }

                    Attachment.CEILING -> {
                        if (face != Direction.DOWN) continue  // 只能放在邻居下方
                        disableFaceProtection = true  // 方向取决于玩家
                    }

                    else -> {
                        if (face == Direction.UP || face == Direction.DOWN) continue  // 只能放在邻居四周
                        disableDirectionProtection = true//方向取决于点击面
                    }
                }
            } else if (Properties.HANGING in required) { //灯笼 面不能点
                if (required.get<Boolean>(Properties.HANGING)) { // 吊着的
                    if (face != Direction.DOWN) continue  // 只能放在邻居下方
                } else {   // 不吊着的
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
            val airPlaceAllowed = !disableAirPlace && enableAirPlace
            val isPlaceAllowedFromClickFace by lazy { required.isPlaceAllowedFromClickFace(face) }
            val isFaceSafe by lazy {
                when (safetyPlaceFaceMode.get()) {
                    SafetyFaceMode.PlayerRotation -> BlockUtils.getDirection(pos)
                    SafetyFaceMode.PlayerPosition -> pos.PickAFaceFromPlayerPosition(player)
                    SafetyFaceMode.None -> null
                }?.let {
                    face == it
                } ?: true
            }

            for (i in 0..1) {
                val useAirPlace = i == 0
                if (useAirPlace && !airPlaceAllowed) continue

                if (useAirPlace) {
                    disableDirectionProtection = false
                    disableFaceProtection = true
                }
                if (directionProtection.get() && !disableDirectionProtection && !isPlaceAllowedFromPlayerRotation) continue
                if (clickProtection.get() && !disableFaceProtection && !isPlaceAllowedFromClickFace) continue


                val neighbour = if (useAirPlace) pos else {
                    val oppositeFace = face.opposite
                    val neighborPos = pos.offset(oppositeFace)
                    if (!required.canPlaceAgainst(neighborPos, face)) continue
                    neighborPos
                }
                val hitPos = if (useAirPlace) tempHitPos else {
                    val oppositeFace = face.opposite
                    tempHitPos.add(
                        oppositeFace.offsetX * 0.5,
                        oppositeFace.offsetY * 0.5,
                        oppositeFace.offsetZ * 0.5
                    )
                }

                // 已经确定了face hitPos neighbour

                if (hitPos.distanceTo(player.eyePos) > PlayerHandDistance) continue


                if (!isFaceSafe) break

                if (!placeThroughWall.get()) {
                    val isVisible =
                        if (useAirPlace) posCenterVisible else (neighbour to face).isVisible
                    if (!isVisible) continue
                }
                if (onlyPlaceOnLookFace.get() && !player.RotationInTheFaceOfBlock(neighbour, face)) continue


                var item = required.block.asItem()
                if (dirtgrass.get() && item === Items.GRASS_BLOCK) item = Items.DIRT
                val result = player.switchItem(item, returnHand.get()) {
                    place(BlockHitResult(hitPos, face, neighbour, false))
                }
                if (!result) info("$block 失败放在 $pos,  \n点了$neighbour 的$face 面 于$hitPos")
                // else  info("$block 成功放在 $pos,  \n点了$neighbour 的$face 面 于$hitPos")
                return result
            }

        }
        return false
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
