package com.kkllffaa.meteor_litematica_printer.Modules.Tools

import com.kkllffaa.meteor_litematica_printer.Addon
import meteordevelopment.meteorclient.systems.modules.Module
import meteordevelopment.meteorclient.events.world.TickEvent
import meteordevelopment.meteorclient.settings.*
import meteordevelopment.meteorclient.utils.misc.input.Input
import meteordevelopment.orbit.EventHandler
import net.minecraft.util.Hand
import kotlin.random.Random
import net.minecraft.util.math.MathHelper

object Hello : Module(Addon.TOOLS, "Hello", "Say hello via showing your friends you're crazy") {
    init {
        toggleOnBindRelease = true
    }

    private val sgGeneral = settings.defaultGroup

    private var tickCounter = 0
    private var nextHandIsMain = true

    override fun onActivate() {
        tickCounter = 0
    }

    override fun onDeactivate() {
        mc.options.sneakKey.isPressed = Input.isPressed(mc.options.sneakKey)
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
