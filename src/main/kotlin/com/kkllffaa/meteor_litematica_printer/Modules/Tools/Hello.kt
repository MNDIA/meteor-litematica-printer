package com.kkllffaa.meteor_litematica_printer.Modules.Tools


import com.kkllffaa.meteor_litematica_printer.Modules.CRUD.AtomicSettings.CommonSettings
import com.kkllffaa.meteor_litematica_printer.Functions.*
import com.kkllffaa.meteor_litematica_printer.Addon
import meteordevelopment.meteorclient.systems.modules.Module
import meteordevelopment.meteorclient.events.world.TickEvent
import meteordevelopment.meteorclient.settings.*
import meteordevelopment.meteorclient.utils.misc.input.Input
import meteordevelopment.orbit.EventHandler
import net.minecraft.util.Hand
import kotlin.random.Random
import kotlin.math.abs
import net.minecraft.client.option.Perspective
import net.minecraft.util.math.MathHelper

object Hello : Module(Addon.TOOLS, "Hello", "Say hello via showing your friends you're crazy :D") {
    init {
        toggleOnBindRelease = true
    }

    private val sgGeneral = settings.defaultGroup
    private val tickDelay = sgGeneral.add(
        IntSetting.Builder()
            .name("tick-delay")
            .description("Ticks between actions")
            .defaultValue(1)
            .min(1).sliderMax(10)
            .build()
    )

    private var tickCounter = 0
    private var 视野模式OnActivate: Perspective? = null

    override fun onActivate() {
        val player = mc.player ?: run {
            this.toggle()
            return
        }
        tickCounter = 0
        视野模式OnActivate = mc.options.perspective
        mc.options.perspective = Perspective.THIRD_PERSON_FRONT
        CommonSettings.OnlyRotateCam.set(true)
    }

    override fun onDeactivate() {
        mc.options.sneakKey.isPressed = Input.isPressed(mc.options.sneakKey)

        mc.player?.yaw = MathHelper.wrapDegrees(CommonSettings.cameraYaw)
        mc.player?.pitch = MathHelper.clamp(CommonSettings.cameraPitch, -90f, 90f)

        视野模式OnActivate?.let {
            mc.options.perspective = it
        }
        CommonSettings.OnlyRotateCam.set(false)
    }

    @EventHandler
    private fun onTick(event: TickEvent.Pre) {
        val player = mc.player ?: return
        if (tickCounter % tickDelay.get() == 0) {
            var yaw = Random.nextFloat() * 360f
            while (abs(player.yaw - yaw) < 90F) {
                yaw = Random.nextFloat() * 360f
            }
            player.yaw = yaw
            var pitch = (Random.nextFloat() - 0.5f) * 179.998f
            while (abs(player.pitch - pitch) < 45F) {
                pitch = (Random.nextFloat() - 0.5f) * 179.998f
            }
            player.pitch = pitch
            mc.options.sneakKey.isPressed = Random.nextBoolean()
        }
        tickCounter++

        if (tickCounter % 4 == 0) {
            player.swingHand(if (tickCounter % 2 == 0) Hand.MAIN_HAND else Hand.OFF_HAND)
        }

    }

}
