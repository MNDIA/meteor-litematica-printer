package com.kkllffaa.meteor_litematica_printer.Modules.Tools;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;

import com.kkllffaa.meteor_litematica_printer.Addon;

public class HangUp extends Module {

    public HangUp() {
        super(Addon.TOOLS, "HangUp", "Hang up the player.");
        if (isActive()) toggle();
    }
    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (!isActive() || mc.player == null) return;
        mc.options.sneakKey.setPressed(true);
        if(Input.isPressed(mc.options.sneakKey)) toggle();
    }
    /**
     * @see {@link net.minecraft.client.MinecraftClient#handleInputEvents()}
     */
    @Override
    public void onActivate() {
        Utils.screenToOpen = new InventoryScreen(mc.player);
    }
    @Override
    public void onDeactivate() {
        mc.options.sneakKey.setPressed(Input.isPressed(mc.options.sneakKey));
    }

}