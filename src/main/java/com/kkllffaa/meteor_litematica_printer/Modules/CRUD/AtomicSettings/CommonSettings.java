package com.kkllffaa.meteor_litematica_printer.Modules.CRUD.AtomicSettings;

import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;


import com.kkllffaa.meteor_litematica_printer.Addon;
import com.kkllffaa.meteor_litematica_printer.Functions.MyUtils.DistanceMode;



public class CommonSettings extends Module {
    public static CommonSettings Instance = new CommonSettings();
    public CommonSettings() {
		super(Addon.SettingsForCRUD, "Common", "Module to configure AtomicSettings.");
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
    
    private final Setting<DistanceMode> distanceProtection = sgGeneral.add(new EnumSetting.Builder<DistanceMode>()
        .name("distance-protection")
        .description("Prevent CRUD blocks that are too far to the player.")
        .defaultValue(DistanceMode.Max)
        .build()
    );

    private final Setting<Double> maxDistance = sgGeneral.add(new DoubleSetting.Builder()
        .name("max-distance")
        .description("Maximum distance from player to mine block face.")
        .defaultValue(5.4)
        .min(1.0)
        .max(1024.0)
        .sliderRange(1.0, 5.5)
        .visible(() -> distanceProtection.get() == DistanceMode.Max)
        .build()
    );

    public static double getHandDistance() {
        return Instance.getHandDistanceStep();
    }
    private double getHandDistanceStep() {
        return switch(distanceProtection.get()){
            case Auto -> {
                double playerHandRange = mc.player.getBlockInteractionRange();
                yield mc.player.isCreative() ? playerHandRange + 0.5 : playerHandRange;
            }
            case Max -> maxDistance.get();
        };
    }

    public static boolean canTouchTheBlockAt(BlockPos pos) {
        return Instance.canInteractWithBlockAt(pos);
    }

    private boolean canInteractWithBlockAt(BlockPos pos) {
        return switch (distanceProtection.get()) {
            case Auto ->
                mc.player.canInteractWithBlockAt(pos, mc.player.isCreative() ? 0.5 : 0.0);
            case Max ->
                (new Box(pos)).squaredMagnitude(mc.player.getEyePos()) < maxDistance.get() * maxDistance.get();
        };
    }
}
