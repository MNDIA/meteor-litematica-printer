package com.kkllffaa.meteor_litematica_printer.Modules.Tools

import com.kkllffaa.meteor_litematica_printer.Addon
import meteordevelopment.meteorclient.settings.SettingGroup
import meteordevelopment.meteorclient.systems.modules.Module
import meteordevelopment.meteorclient.utils.misc.input.Input
import net.minecraft.util.Hand
import meteordevelopment.meteorclient.events.world.TickEvent
import meteordevelopment.meteorclient.settings.*
import meteordevelopment.orbit.EventHandler
import meteordevelopment.orbit.EventPriority

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

    @EventHandler
    private fun onTick(event: TickEvent.Pre) {
        val player = mc.player ?: return
    }

    @EventHandler
    private fun onTick(event: TickEvent.Post) {
        val player = mc.player ?: return
    }
}
