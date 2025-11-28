package com.kkllffaa.meteor_litematica_printer.Modules.CRUD.AtomicSettings

import com.kkllffaa.meteor_litematica_printer.Addon

import com.kkllffaa.meteor_litematica_printer.Functions.*
import meteordevelopment.meteorclient.settings.*
import meteordevelopment.meteorclient.systems.modules.Module
import meteordevelopment.meteorclient.utils.world.BlockUtils
import net.minecraft.block.*
import net.minecraft.block.enums.ComparatorMode
import net.minecraft.state.property.Properties
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d

object InteractSettings : Module(Addon.SettingsForCRUD, "Interact", "Module to configure AtomicSettings.") {
    override fun toggle() {
        if (isActive) return
        super.toggle()
    }

    init {
        this.toggle()
    }

    private val sgGeneral = settings.defaultGroup

    private val swingHand: Setting<ActionMode> = sgGeneral.add(
        EnumSetting.Builder<ActionMode>()
            .name("swing-hand")
            .description("swing hand post interact.")
            .defaultValue(ActionMode.None)
            .build()
    )

    private val FaceBy: Setting<SafetyFace> = sgGeneral.add(
        EnumSetting.Builder<SafetyFace>()
            .name("interact-face-by")
            .description("Determines which face of the block to interact with.")
            .defaultValue(SafetyFace.PlayerPosition)
            .build()
    )

    private val onlyInteractOnLook: Setting<Boolean> = sgGeneral.add(
        BoolSetting.Builder()
            .name("only-interact-on-look-the-face")
            .description("Only interact with blocks when looking at the face to interact with.")
            .defaultValue(false)
            .build()
    )

    private val stateBlocks: Setting<MutableList<Block>> = sgGeneral.add(
        BlockListSetting.Builder()
            .name("state-blocks")
            .description("Blocks that need interaction to adjust their state.")
            .defaultValue(
                // 中继器、比较器
                Blocks.REPEATER,
                Blocks.COMPARATOR,
                // 音符盒
                Blocks.NOTE_BLOCK,
                // 拉杆
                Blocks.LEVER,
                // 日光传感器
                Blocks.DAYLIGHT_DETECTOR,
                // 活板门
                Blocks.OAK_TRAPDOOR,
                Blocks.SPRUCE_TRAPDOOR,
                Blocks.BIRCH_TRAPDOOR,
                Blocks.JUNGLE_TRAPDOOR,
                Blocks.ACACIA_TRAPDOOR,
                Blocks.DARK_OAK_TRAPDOOR,
                Blocks.CRIMSON_TRAPDOOR,
                Blocks.WARPED_TRAPDOOR,
                Blocks.MANGROVE_TRAPDOOR,
                Blocks.BAMBOO_TRAPDOOR,
                Blocks.CHERRY_TRAPDOOR,
                Blocks.PALE_OAK_TRAPDOOR,
                Blocks.COPPER_TRAPDOOR,
                Blocks.EXPOSED_COPPER_TRAPDOOR,
                Blocks.WEATHERED_COPPER_TRAPDOOR,
                Blocks.OXIDIZED_COPPER_TRAPDOOR,
                // 门
                Blocks.OAK_DOOR,
                Blocks.SPRUCE_DOOR,
                Blocks.BIRCH_DOOR,
                Blocks.JUNGLE_DOOR,
                Blocks.ACACIA_DOOR,
                Blocks.DARK_OAK_DOOR,
                Blocks.CRIMSON_DOOR,
                Blocks.WARPED_DOOR,
                Blocks.MANGROVE_DOOR,
                Blocks.BAMBOO_DOOR,
                Blocks.CHERRY_DOOR,
                Blocks.PALE_OAK_DOOR,
                Blocks.COPPER_DOOR,
                Blocks.EXPOSED_COPPER_DOOR,
                Blocks.WEATHERED_COPPER_DOOR,
                Blocks.OXIDIZED_COPPER_DOOR,
                // 栅栏门
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
                Blocks.PALE_OAK_FENCE_GATE
                //TODO:1.21.10 添加更多默认项
            )
            .build()
    )


    fun TryInteractBlock(blockPos: BlockPos, count: Int = 1): Int {
        val player = mc.player ?: return 0
        if (player.isSneaking || !blockPos.canTouch) {
            return 0
        }
        val pos = if (blockPos is BlockPos.Mutable) BlockPos(blockPos) else blockPos
        val direction = when (FaceBy.get()) {
            SafetyFace.PlayerRotation -> BlockUtils.getDirection(pos)
            SafetyFace.PlayerPosition -> pos.PickAFaceFromPlayerPosition(player)
        }
        if (onlyInteractOnLook.get() && !player.RotationInTheFaceOfBlock(pos, direction)) {
            return 0
        }
        val interactionManager = mc.interactionManager ?: return 0
        val hitPos = Vec3d(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5)
        val blockHitResult = BlockHitResult(hitPos, direction, pos, false)
        for (i in 0..<count) {
            val result = interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, blockHitResult)
            if (!result.isAccepted) {
                warning("Interaction not accepted at $pos, result: $result")
                return i
            }
            player.swing(swingMode = swingHand.get())
        }
        return count
    }


    fun calculateRequiredInteractions(targetState: BlockState, currentState: BlockState): Int {
        val currentblock = currentState.block
        if (currentblock !== targetState.block || currentblock !in stateBlocks.get()) {
            return 0
        }

        // 音符盒
        if (currentblock is NoteBlock) {
            val currentNote = currentState.get<Int>(Properties.NOTE)
            val targetNote = targetState.get<Int>(Properties.NOTE)

            // Note blocks cycle through 0-24 (25 states)
            val diff = (targetNote - currentNote + 25) % 25
            return diff
        }

        // 中继器
        if (currentblock is RepeaterBlock) {
            val currentDelay = currentState.get<Int>(Properties.DELAY)
            val targetDelay = targetState.get<Int>(Properties.DELAY)

            // Repeaters cycle through 1-4 (4 states)
            val diff = (targetDelay - currentDelay + 4) % 4
            return diff
        }

        // 比较器
        if (currentblock is ComparatorBlock) {
            return if (currentState.get<ComparatorMode>(Properties.COMPARATOR_MODE)
                == targetState.get<ComparatorMode>(Properties.COMPARATOR_MODE)
            )
                0
            else
                1
        }

        // 光传感器
        if (currentblock is DaylightDetectorBlock) {
            return if (currentState.get<Boolean>(Properties.INVERTED) == targetState.get<Boolean>(Properties.INVERTED)) 0 else 1
        }

        // 拉杆
        if (currentblock is LeverBlock) {
            return if (currentState.get<Boolean>(Properties.POWERED) == targetState.get<Boolean>(Properties.POWERED)) 0 else 1
        }

        // 栅栏门 活板门 门
        if (Properties.OPEN in currentState) {
            return if (currentState.get<Boolean>(Properties.OPEN) == targetState.get<Boolean>(Properties.OPEN)) 0 else 1
        }

        return 0 // 未知类型或不可交互类型
    }

}
