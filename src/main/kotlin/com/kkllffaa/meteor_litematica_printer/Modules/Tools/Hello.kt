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
    private val RotationtickDelay = sgGeneral.add(
        IntSetting.Builder()
            .name("Rotation-tick-delay")
            .description("Server animation updates don't work well if you rotation every tick.")
            .defaultValue(3)
            .min(1).sliderMax(10)
            .build()
    )
    private val preferPerspectiveSetting = sgGeneral.add(
        EnumSetting.Builder<PreferPerspective>()
            .name("prefer-perspective")
            .description("Preferred perspective mode when activating the module.")
            .defaultValue(PreferPerspective.THIRD_PERSON_BACK)
            .build()
    )

    private var tickCounter = 0
    private var 视野模式OnActivate: Perspective? = null
    private var wasOnlyRotateCam = false
    private var wasRotation = Rotation(0F, 0F)
    override fun onActivate() {
        val player = mc.player ?: run {
            this.toggle()
            return
        }
        wasRotation = Rotation(player.yaw, player.pitch)
        wasOnlyRotateCam = CommonSettings.OnlyRotateCam.get()
        if (!wasOnlyRotateCam) CommonSettings.OnlyRotateCam.set(true)


        tickCounter = 0
        视野模式OnActivate = mc.options.perspective
        when (preferPerspectiveSetting.get()) {
            PreferPerspective.NONE -> {}
            PreferPerspective.FIRST_PERSON -> mc.options.perspective = Perspective.FIRST_PERSON
            PreferPerspective.THIRD_PERSON_BACK -> mc.options.perspective = Perspective.THIRD_PERSON_BACK
        }
    }

    override fun onDeactivate() {
        视野模式OnActivate?.let {
            if (preferPerspectiveSetting.get() != PreferPerspective.NONE) mc.options.perspective = it
        }
        mc.player?.let {
            it.yaw = wasRotation.yaw
            it.pitch = wasRotation.pitch
        }
        if (!wasOnlyRotateCam) CommonSettings.OnlyRotateCam.set(false)
        mc.options.sneakKey.isPressed = Input.isPressed(mc.options.sneakKey)
        mc.options.forwardKey.isPressed = Input.isPressed(mc.options.forwardKey)
        mc.options.backKey.isPressed = Input.isPressed(mc.options.backKey)
        mc.options.rightKey.isPressed = Input.isPressed(mc.options.rightKey)
        mc.options.leftKey.isPressed = Input.isPressed(mc.options.leftKey)
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

        val yawDiff = Math.toRadians((CommonSettings.cameraYaw - player.yaw).toDouble())
        val cos = kotlin.math.cos(yawDiff).toFloat()
        val sin = kotlin.math.sin(yawDiff).toFloat()

        val intentForward =
            (if (Input.isPressed(mc.options.forwardKey)) 1f else 0f) - (if (Input.isPressed(mc.options.backKey)) 1f else 0f)
        val intentRight =
            (if (Input.isPressed(mc.options.rightKey)) 1f else 0f) - (if (Input.isPressed(mc.options.leftKey)) 1f else 0f)

        val actualForward = intentForward * cos - intentRight * sin
        val actualRight = intentForward * sin + intentRight * cos

        mc.options.forwardKey.isPressed = actualForward > 0.1f
        mc.options.backKey.isPressed = actualForward < -0.1f
        mc.options.rightKey.isPressed = actualRight > 0.1f
        mc.options.leftKey.isPressed = actualRight < -0.1f
    }
}
