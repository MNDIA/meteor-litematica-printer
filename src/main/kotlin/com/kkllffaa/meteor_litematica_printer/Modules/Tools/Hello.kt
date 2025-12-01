package com.kkllffaa.meteor_litematica_printer.Modules.Tools


import com.kkllffaa.meteor_litematica_printer.Modules.CRUD.AtomicSettings.CommonSettings
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
    private val RotationtickDelay = sgGeneral.add(
        IntSetting.Builder()
            .name("Rotation-tick-delay")
            .description("Server animation updates don't work well if you rotation every tick.")
            .defaultValue(3)
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
        mc.options.forwardKey.isPressed = Input.isPressed(mc.options.forwardKey)
        mc.options.backKey.isPressed = Input.isPressed(mc.options.backKey)
        mc.options.rightKey.isPressed = Input.isPressed(mc.options.rightKey)
        mc.options.leftKey.isPressed = Input.isPressed(mc.options.leftKey)

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
        if (tickCounter % RotationtickDelay.get() == 0) {
            var yaw = Random.nextFloat() * 360f
            while (abs(MathHelper.wrapDegrees((player.yaw - yaw))) < 130F) {
                yaw = Random.nextFloat() * 360f
            }
            player.yaw = yaw
            var pitch = (Random.nextFloat() - 0.5f) * 179.998f
            while (abs(player.pitch - pitch) < 45F) {
                pitch = (Random.nextFloat() - 0.5f) * 179.998f
            }
            player.pitch = pitch
        }
        mc.options.sneakKey.isPressed = !mc.options.sneakKey.isPressed
        if (tickCounter % 4 == 0) {
            player.swingHand(if (tickCounter % 2 == 0) Hand.MAIN_HAND else Hand.OFF_HAND)
        }
        tickCounter++

        val intentForward =
            (if (Input.isPressed(mc.options.forwardKey)) 1f else 0f) - (if (Input.isPressed(mc.options.backKey)) 1f else 0f)
        val intentRight =
            (if (Input.isPressed(mc.options.rightKey)) 1f else 0f) - (if (Input.isPressed(mc.options.leftKey)) 1f else 0f)

        val yawDiff = Math.toRadians((CommonSettings.cameraYaw - player.yaw).toDouble())
        val cos = kotlin.math.cos(yawDiff).toFloat()
        val sin = kotlin.math.sin(yawDiff).toFloat()

        val actualForward = intentForward * cos - intentRight * sin
        val actualRight = intentForward * sin + intentRight * cos

        mc.options.forwardKey.isPressed = actualForward > 0.1f
        mc.options.backKey.isPressed = actualForward < -0.1f
        mc.options.rightKey.isPressed = actualRight > 0.1f
        mc.options.leftKey.isPressed = actualRight < -0.1f
    }
}
