package com.kkllffaa.meteor_litematica_printer.Modules.Tools

import com.google.common.collect.Streams
import com.kkllffaa.meteor_litematica_printer.Addon
import meteordevelopment.meteorclient.events.world.TickEvent
import meteordevelopment.meteorclient.settings.BoolSetting
import meteordevelopment.meteorclient.settings.DoubleSetting
import meteordevelopment.meteorclient.settings.Setting
import meteordevelopment.meteorclient.settings.SettingGroup
import meteordevelopment.meteorclient.systems.modules.Module
import meteordevelopment.meteorclient.systems.modules.Modules
import meteordevelopment.meteorclient.systems.modules.movement.GUIMove
import meteordevelopment.meteorclient.systems.modules.render.Freecam
import meteordevelopment.meteorclient.utils.misc.input.Input
import meteordevelopment.orbit.EventHandler
import net.minecraft.block.Blocks
import net.minecraft.util.shape.VoxelShape
import kotlin.math.sqrt

class Parkour : Module(Addon.TOOLS, "parkour", "Automatically jumps at the edges of blocks.") {
    private val sgGeneral: SettingGroup = settings.getDefaultGroup()

    private val edgeDistance: Setting<Double?> = sgGeneral.add<Double?>(
        DoubleSetting.Builder()
            .name("edge-distance")
            .description("How far from the edge should you jump.")
            .range(0.001, 0.1)
            .defaultValue(0.004)
            .build()
    )

    private val minSpeed: Setting<Double?> = sgGeneral.add<Double?>(
        DoubleSetting.Builder()
            .name("min-speed")
            .description("Minimum horizontal speed required to trigger automatic jumping.(block/tick)")
            .range(0.0, 0.39285)
            .defaultValue(0.085)
            .build()
    )
    private val 悬空高度: Setting<Double?> = sgGeneral.add<Double?>(
        DoubleSetting.Builder()
            .name("悬空高度")
            .description("脚下高度内没有碰撞才可以跳跃")
            .range(0.0001, 1.5)
            .defaultValue(0.1249)
            .build()
    )
    private val 垫幽灵砖: Setting<Boolean?> = sgGeneral.add<Boolean?>(
        BoolSetting.Builder()
            .name("垫幽灵砖")
            .description("Whether to place ghost blocks under the player.")
            .defaultValue(false)
            .build()
    )

    private var needEdgeJumping = false

    @EventHandler
    private fun onTick(event: TickEvent.Pre?) {
        mc.options.jumpKey.setPressed(
            needEdgeJumping || (Input.isPressed(mc.options.jumpKey) && this.isPlayerInControl)
        )
    }

    @EventHandler
    private fun onTick(event: TickEvent.Post?) {
        if (!mc.player!!.isOnGround() || mc.options.jumpKey.isPressed() ||
            mc.player!!.isSneaking() || mc.options.sneakKey.isPressed()
        ) {
            needEdgeJumping = false
            if (垫幽灵砖.get()) {
                mc.world!!.setBlockState(mc.player!!.getBlockPos().down(), Blocks.STONE.getDefaultState())
            }
        } else {
            val horizontalSpeed =
                sqrt(mc.player!!.getVelocity().x * mc.player!!.getVelocity().x + mc.player!!.getVelocity().z * mc.player!!.getVelocity().z)
            if (horizontalSpeed < minSpeed.get()!!) {
                needEdgeJumping = false
            } else {
                val adjustedBox = mc.player!!.getBoundingBox().offset(0.0, -悬空高度.get()!!, 0.0)
                    .expand(-edgeDistance.get()!!, 0.0, -edgeDistance.get()!!)

                val blockCollisions = Streams.stream<VoxelShape?>(mc.world!!.getBlockCollisions(mc.player, adjustedBox))

                if (blockCollisions.findAny().isPresent()) {
                    needEdgeJumping = false
                } else {
                    needEdgeJumping = true
                }
            }
        }
    }


    private val isPlayerInControl: Boolean
        get() = (mc.currentScreen == null || !Modules.get()
            .get<GUIMove?>(GUIMove::class.java).skip()) && !Modules.get()
            .isActive(Freecam::class.java)
}
