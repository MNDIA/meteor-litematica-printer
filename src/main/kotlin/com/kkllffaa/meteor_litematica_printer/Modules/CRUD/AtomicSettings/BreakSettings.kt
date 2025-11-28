package com.kkllffaa.meteor_litematica_printer.Modules.CRUD.AtomicSettings

import com.kkllffaa.meteor_litematica_printer.Addon
import com.kkllffaa.meteor_litematica_printer.Functions.*
import meteordevelopment.meteorclient.MeteorClient
import meteordevelopment.meteorclient.events.world.TickEvent
import meteordevelopment.meteorclient.settings.EnumSetting
import meteordevelopment.meteorclient.settings.Setting
import meteordevelopment.meteorclient.systems.modules.Module
import meteordevelopment.meteorclient.systems.modules.Modules
import meteordevelopment.meteorclient.systems.modules.player.InstantRebreak
import meteordevelopment.meteorclient.utils.world.BlockUtils
import meteordevelopment.orbit.EventHandler
import meteordevelopment.orbit.EventPriority
import net.minecraft.util.math.BlockPos

object BreakSettings : Module(Addon.SettingsForCRUD, "Break", "Module to configure AtomicSettings.") {
    override fun toggle() {
        if (isActive) return
        super.toggle()
    }

    init {
        toggle()
    }

    private val sgGeneral = settings.defaultGroup
    private val instantRotation: Setting<ActionMode> = sgGeneral.add(
        EnumSetting.Builder<ActionMode>()
            .name("instant-rotate")
            .description("rotation pre mining.")
            .defaultValue(ActionMode.None)
            .build()
    )

    private val swingHand: Setting<ActionMode> = sgGeneral.add(
        EnumSetting.Builder<ActionMode>()
            .name("swing-hand")
            .description("swing hand post mining.")
            .defaultValue(ActionMode.None)
            .build()
    )

    private val FaceBy: Setting<SafetyFace> = sgGeneral.add(
        EnumSetting.Builder<SafetyFace>()
            .name("mining-face-by")
            .description("")
            .defaultValue(SafetyFace.PlayerPosition)
            .build()
    )

    fun breakBlockWithRotationCfg(blockPos: BlockPos) {
        RotateAndDo(blockPos, instantRotation.get()) { breakBlock(blockPos) }
    }

    fun breakBlock(blockPos: BlockPos) {
        val pos = if (blockPos is BlockPos.Mutable) BlockPos(blockPos) else blockPos

        Modules.get().get(InstantRebreak::class.java)?.takeIf {
            it.isActive && it.blockPos == pos && it.shouldMine()
        }?.run {
            sendPacket()
            return
        }

        mc.interactionManager?.run {
            val player = mc.player ?: return
            val direction = when (FaceBy.get()) {
                SafetyFace.PlayerRotation -> BlockUtils.getDirection(pos)
                SafetyFace.PlayerPosition -> pos.PickAFaceFromPlayerPosition(player)
            }
            if (isBreakingBlock) updateBlockBreakingProgress(pos, direction)
            else attackBlock(pos, direction)

            player.swing(swingMode = swingHand.get())

            breaking = true
            breakingThisTick = true
        }

    }

    @JvmField
    var breaking: Boolean = false
    private var breakingThisTick = false

    @EventHandler(priority = EventPriority.HIGHEST + 100)
    private fun onTickPre(event: TickEvent.Pre) {
        breakingThisTick = false
    }

    @EventHandler(priority = EventPriority.LOWEST - 100)
    private fun onTickPost(event: TickEvent.Post) {
        if (!breakingThisTick && breaking) {
            breaking = false
            MeteorClient.mc.interactionManager?.cancelBlockBreaking()
        }
    }

}
