package com.kkllffaa.meteor_litematica_printer.Modules.Tools;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

import com.kkllffaa.meteor_litematica_printer.Addon;

public class AlwaysSneak extends Module {

    public AlwaysSneak() {
        super(Addon.TOOLS, "always-sneak", "Makes the player always sneak.");
    }
    @EventHandler
    private void onTick(TickEvent.Pre event) {
        mc.options.sneakKey.setPressed(true);
    }
}