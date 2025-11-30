package com.kkllffaa.meteor_litematica_printer.Modules.Tools

import com.kkllffaa.meteor_litematica_printer.Addon
import com.kkllffaa.meteor_litematica_printer.Functions.isPlayerInControl
import meteordevelopment.meteorclient.systems.modules.Module
import meteordevelopment.meteorclient.utils.misc.input.Input
import meteordevelopment.meteorclient.events.world.TickEvent
import meteordevelopment.meteorclient.events.meteor.KeyEvent
import meteordevelopment.meteorclient.events.meteor.MouseClickEvent
import meteordevelopment.meteorclient.utils.misc.input.KeyAction

import meteordevelopment.meteorclient.settings.*
import meteordevelopment.orbit.EventHandler
import net.minecraft.util.Hand
import meteordevelopment.orbit.EventPriority

object SwingHand : Module(Addon.TOOLS, "SwingHand", "Swing your hands with LR Click when module is enabled.") {
    init {
        toggleOnBindRelease = true
    }

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
            .defaultValue(4)
            .min(1).sliderMin(4)
            .max(1024).sliderMax(10)
            .visible { 持续连续挥手.get() }
            .build()
    )
    private var wasAttackPressed = false
    private var wasUsePressed = false

    private var tickCounter = 0

    private var nextHandIsMain = true

    override fun onActivate() {
        wasAttackPressed = false
        wasUsePressed = false
        tickCounter = 0
    }

    @EventHandler(priority = EventPriority.HIGHEST + 100)
    private fun onMouse(event: MouseClickEvent) {
        if (event.action == KeyAction.Press && isPlayerInControl
            && (mc.options.attackKey.matchesMouse(event.click) || mc.options.useKey.matchesMouse(event.click))
        ) {
            event.cancel()
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST + 100)
    private fun onKey(event: KeyEvent) {
        if (event.action == KeyAction.Press && isPlayerInControl
            && (Input.getKey(mc.options.attackKey) == event.key() || Input.getKey(mc.options.useKey) == event.key())
        ) {
            event.cancel()
        }
    }

    @EventHandler
    private fun onTick(event: TickEvent.Pre) {
        val player = mc.player ?: return

        mc.options.attackKey.isPressed = false
        mc.options.useKey.isPressed = false
        val attackPressed = Input.isPressed(mc.options.attackKey)
        val usePressed = Input.isPressed(mc.options.useKey)

        if (持续连续挥手.get()) {
            tickCounter++
            val interval = 持续挥手间隔tick.get()

            if (tickCounter % interval == 0) {
                if (attackPressed && usePressed) {
                    player.swingHand(if (nextHandIsMain) Hand.MAIN_HAND else Hand.OFF_HAND)
                    nextHandIsMain = !nextHandIsMain
                } else if (attackPressed) {
                    player.swingHand(Hand.OFF_HAND)
                } else if (usePressed) {
                    player.swingHand(Hand.MAIN_HAND)
                }
            }
        } else {
            if (attackPressed && !wasAttackPressed) {
                player.swingHand(Hand.OFF_HAND)
            } else if (usePressed && !wasUsePressed) {
                player.swingHand(Hand.MAIN_HAND)
            }
            wasAttackPressed = attackPressed
            wasUsePressed = usePressed
        }


    }

}
