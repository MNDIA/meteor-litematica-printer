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
import net.minecraft.client.option.Perspective
import net.minecraft.util.math.MathHelper

object Hello : Module(Addon.TOOLS, "Hello", "Say hello via showing your friends you're crazy :D") {
    init {
        toggleOnBindRelease = true
    }

    private val sgGeneral = settings.defaultGroup

    private var tickCounter = 0
    private var nextHandIsMain = true
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
        player.yaw = Random.nextFloat() * 360f
        player.pitch = (Random.nextFloat() - 0.5f) * 90f
        mc.options.sneakKey.isPressed = Random.nextBoolean()
        tickCounter++

        if (tickCounter % 4 == 0) {
            player.swingHand(if (nextHandIsMain) Hand.MAIN_HAND else Hand.OFF_HAND)
            nextHandIsMain = !nextHandIsMain
        }

    }

}
