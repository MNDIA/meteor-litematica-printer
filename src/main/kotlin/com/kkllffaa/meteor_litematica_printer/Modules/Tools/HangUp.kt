package com.kkllffaa.meteor_litematica_printer.Modules.Tools

import com.kkllffaa.meteor_litematica_printer.Addon
import meteordevelopment.meteorclient.events.world.TickEvent
import meteordevelopment.meteorclient.systems.modules.Module
import meteordevelopment.meteorclient.utils.Utils
import meteordevelopment.meteorclient.utils.misc.input.Input
import meteordevelopment.orbit.EventHandler
import net.minecraft.client.gui.screen.ingame.InventoryScreen

class HangUp : Module(Addon.TOOLS, "HangUp", "Hang up the player.") {
    init {
        if (isActive()) toggle()
    }

    @EventHandler
    private fun onTick(event: TickEvent.Pre?) {
        if (!isActive() || mc.player == null) return
        mc.options.sneakKey.setPressed(true)
        if (Input.isPressed(mc.options.sneakKey)) toggle()
    }

    /**
     * @see {@link net.minecraft.client.MinecraftClient.handleInputEvents
     */
    override fun onActivate() {
        Utils.screenToOpen = InventoryScreen(mc.player)
    }

    override fun onDeactivate() {
        mc.options.sneakKey.setPressed(Input.isPressed(mc.options.sneakKey))
    }
}
