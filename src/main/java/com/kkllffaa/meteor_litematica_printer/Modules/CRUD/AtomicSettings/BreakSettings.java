package com.kkllffaa.meteor_litematica_printer.Modules.CRUD.AtomicSettings;


import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.player.InstantRebreak;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.function.Supplier;

import com.kkllffaa.meteor_litematica_printer.Addon;


public class BreakSettings extends Module {
    public BreakSettings() {
		super(Addon.SettingsForCRUD, "BreakSettings", "Module to configure AtomicSettings.");
        this.toggle();
	}

	@Override
	public void toggle() {
        if (isActive()) {
            return;
        }
        super.toggle();
    }
    
	private final SettingGroup sgGeneral = settings.getDefaultGroup();


    
    public static boolean breaking;
    private static boolean breakingThisTick;

    @EventHandler(priority = EventPriority.HIGHEST + 100)
    private static void onTickPre(TickEvent.Pre event) {
        breakingThisTick = false;
    }

    @EventHandler(priority = EventPriority.LOWEST - 100)
    private static void onTickPost(TickEvent.Post event) {
        if (!breakingThisTick && breaking) {
            meteordevelopment.meteorclient.utils.world.BlockUtils.breaking = false;
            breaking = false;
            if (MeteorClient.mc.interactionManager != null) MeteorClient.mc.interactionManager.cancelBlockBreaking();
        }
    }

    public static boolean breakBlock(BlockPos blockPos, SwingMode swing, Supplier<Direction> getDirection) {
        if (!meteordevelopment.meteorclient.utils.world.BlockUtils.canBreak(blockPos, MeteorClient.mc.world.getBlockState(blockPos))) return false;

        // Creating new instance of block pos because minecraft assigns the parameter to a field, and we don't want it to change when it has been stored in a field somewhere
        BlockPos pos = blockPos instanceof BlockPos.Mutable ? new BlockPos(blockPos) : blockPos;

        InstantRebreak ir = Modules.get().get(InstantRebreak.class);
        if (ir != null && ir.isActive() && ir.blockPos.equals(pos) && ir.shouldMine()) {
            ir.sendPacket();
            return true;
        }

        Direction direction = getDirection.get();
        if (MeteorClient.mc.interactionManager.isBreakingBlock())
            MeteorClient.mc.interactionManager.updateBlockBreakingProgress(pos, direction);
        else MeteorClient.mc.interactionManager.attackBlock(pos, direction);

        switch (swing) {
            case None -> {}
            case SendPacket -> MeteorClient.mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            case Normal -> MeteorClient.mc.player.swingHand(Hand.MAIN_HAND);
        }
        meteordevelopment.meteorclient.utils.world.BlockUtils.breaking = true;
        breaking = true;
        breakingThisTick = true;

        return true;
    }
    public enum SwingMode {
        None,
        SendPacket,
        Normal
    }
}
