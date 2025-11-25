package com.kkllffaa.meteor_litematica_printer.Modules.CRUD.AtomicSettings

import com.kkllffaa.meteor_litematica_printer.Addon
import com.kkllffaa.meteor_litematica_printer.Functions.BlockPosUtils
import com.kkllffaa.meteor_litematica_printer.Functions.MyUtils.ActionMode
import com.kkllffaa.meteor_litematica_printer.Functions.MyUtils.SafetyFace
import meteordevelopment.meteorclient.MeteorClient
import meteordevelopment.meteorclient.events.world.TickEvent
import meteordevelopment.meteorclient.settings.EnumSetting
import meteordevelopment.meteorclient.settings.Setting
import meteordevelopment.meteorclient.settings.SettingGroup
import meteordevelopment.meteorclient.systems.modules.Module
import meteordevelopment.meteorclient.systems.modules.Modules
import meteordevelopment.meteorclient.systems.modules.player.InstantRebreak
import meteordevelopment.meteorclient.utils.player.Rotations
import meteordevelopment.meteorclient.utils.world.BlockUtils
import meteordevelopment.orbit.EventHandler
import meteordevelopment.orbit.EventPriority
import net.minecraft.block.BlockState
import net.minecraft.util.math.BlockPos


class BreakSettings : Module(Addon.SettingsForCRUD, "Break", "Module to configure AtomicSettings.") {
    override fun toggle() {
        if (isActive()) {
            return
        }
        super.toggle()
    }

    private val sgGeneral: SettingGroup = settings.getDefaultGroup()
    private val instantRotation: Setting<ActionMode?> = sgGeneral.add<ActionMode?>(
        EnumSetting.Builder<ActionMode?>()
            .name("instant-rotate")
            .description("rotation pre mining.")
            .defaultValue(ActionMode.None)
            .build()
    )

    // private final Setting<ActionMode> swingHand = sgGeneral.add(new EnumSetting.Builder<ActionMode>()
    //     .name("swing-hand")
    //     .description("swing hand post mining.")
    //     .defaultValue(ActionMode.None)
    //     .build()
    // );
    private val FaceBy: Setting<SafetyFace?> = sgGeneral.add<SafetyFace?>(
        EnumSetting.Builder<SafetyFace?>()
            .name("mining-face-by")
            .description("")
            .defaultValue(SafetyFace.PlayerPosition)
            .build()
    )

    init {
        this.toggle()
    }

    private fun breakBlockStep1(blockPos: BlockPos) {
        when (instantRotation.get()) {
            ActionMode.None -> breakBlockStep2(blockPos)
            ActionMode.SendPacket -> Rotations.rotate(
                Rotations.getYaw(blockPos),
                Rotations.getPitch(blockPos),
                50,
                false,
                Runnable { breakBlockStep2(blockPos) })

            ActionMode.Normal -> Rotations.rotate(
                Rotations.getYaw(blockPos),
                Rotations.getPitch(blockPos),
                50,
                true,
                Runnable { breakBlockStep2(blockPos) })
        }
    }

    private fun breakBlockStep2(blockPos: BlockPos?) {
        // Creating new instance of block pos because minecraft assigns the parameter to a field, and we don't want it to change when it has been stored in a field somewhere
        val pos: BlockPos = (if (blockPos is BlockPos.Mutable) BlockPos(blockPos) else blockPos)!!

        val ir = Modules.get().get<InstantRebreak?>(InstantRebreak::class.java)
        if (ir != null && ir.isActive() && ir.blockPos == pos && ir.shouldMine()) {
            ir.sendPacket()
            return
        }
        val direction = when (FaceBy.get()) {
            SafetyFace.PlayerRotation -> BlockUtils.getDirection(pos)
            SafetyFace.PlayerPosition -> BlockPosUtils.getDirectionFromPlayerPosition(pos)
        }

        if (MeteorClient.mc.interactionManager!!.isBreakingBlock()) MeteorClient.mc.interactionManager!!.updateBlockBreakingProgress(
            pos,
            direction
        )
        else MeteorClient.mc.interactionManager!!.attackBlock(pos, direction)

        // switch (swingHand.get()) {
        //     case ActionMode.None -> {}
        //     case ActionMode.SendPacket -> MeteorClient.mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
        //     case ActionMode.Normal -> MeteorClient.mc.player.swingHand(Hand.MAIN_HAND);
        // }
        breaking = true
        breakingThisTick = true
    }

    companion object {
        var Instance: BreakSettings = BreakSettings()
        @JvmField
        var breaking: Boolean = false
        private var breakingThisTick = false

        @EventHandler(priority = EventPriority.HIGHEST + 100)
        private fun onTickPre(event: TickEvent.Pre?) {
            breakingThisTick = false
        }

        @EventHandler(priority = EventPriority.LOWEST - 100)
        private fun onTickPost(event: TickEvent.Post?) {
            if (!breakingThisTick && breaking) {
                breaking = false
                if (MeteorClient.mc.interactionManager != null) MeteorClient.mc.interactionManager!!.cancelBlockBreaking()
            }
        }

        fun canBreakByObjectiveStep1(blockPos: BlockPos?): Boolean {
            return CommonSettings.canTouchTheBlockAt(blockPos)
        }

        fun canBreakByObjectiveStep2(blockPos: BlockPos?, blockState: BlockState): Boolean {
            return BlockUtils.canBreak(blockPos, blockState)
        }

        private fun canBreakByObjective(blockPos: BlockPos?): Boolean {
            if (!canBreakByObjectiveStep1(blockPos)) return false
            val blockState = MeteorClient.mc.world!!.getBlockState(blockPos)
            if (!canBreakByObjectiveStep2(blockPos, blockState)) return false
            return true
        }

        fun breakBlock(blockPos: BlockPos) {
            Instance.breakBlockStep1(blockPos)
        }

        fun TryBreakBlock(blockPos: BlockPos): Boolean {
            if (!canBreakByObjective(blockPos)) return false
            breakBlock(blockPos)
            return true
        }
    }
}
