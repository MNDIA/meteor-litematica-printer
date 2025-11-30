package com.kkllffaa.meteor_litematica_printer.Modules.Tools

import com.kkllffaa.meteor_litematica_printer.Addon
import meteordevelopment.meteorclient.systems.modules.Module
import meteordevelopment.meteorclient.utils.misc.input.Input
import meteordevelopment.meteorclient.events.world.TickEvent
import meteordevelopment.meteorclient.settings.*
import meteordevelopment.orbit.EventHandler
import net.minecraft.util.Hand

object SwingHand : Module(Addon.TOOLS, "SwingHand", "Swing your hands with LR Click when module is enabled.") {
    private val sgGeneral = settings.defaultGroup

    private val 持续连续挥手: Setting<Boolean> = sgGeneral.add(
        BoolSetting.Builder()
            .name("continuously-swing")
            .description("Wave your hand continuously when the key is pressed instead of just once.")
            .defaultValue(true)
            .build()
    )
    private val 持续挥手间隔tick: Setting<Int> = sgGeneral.add(
        IntSetting.Builder()
            .name("swing-interval-tick")
            .description("The interval tick for continuously swinging hand.")
            .defaultValue(2)
            .min(1).sliderMin(1)
            .max(1024).sliderMax(20)
            .visible { 持续连续挥手.get() }
            .build()
    )

    private var wasAttackPressed = false
    private var wasUsePressed = false

    private var tickCounter = 0
    override fun onActivate() {
        wasAttackPressed = false
        wasUsePressed = false
        tickCounter = 0
    }

    @EventHandler
    private fun onTick(event: TickEvent.Pre) {
        val player = mc.player ?: return
        val options = mc.options ?: return

        val attackPressed = Input.isPressed(options.attackKey)
        val usePressed = Input.isPressed(options.useKey)

        options.attackKey.isPressed = false
        options.useKey.isPressed = false


        if (持续连续挥手.get()) {
            tickCounter++
            val interval = 持续挥手间隔tick.get()

            if (attackPressed && tickCounter % interval == 0) {
                player.swingHand(Hand.OFF_HAND)
            }
            if (usePressed && tickCounter % interval == 0) {
                player.swingHand(Hand.MAIN_HAND)
            }
        } else {
            if (attackPressed && !wasAttackPressed) {
                player.swingHand(Hand.OFF_HAND)
            }
            if (usePressed && !wasUsePressed) {
                player.swingHand(Hand.MAIN_HAND)
            }
            // 更新上一次的按键状态
            wasAttackPressed = attackPressed
            wasUsePressed = usePressed
        }

    }

}
