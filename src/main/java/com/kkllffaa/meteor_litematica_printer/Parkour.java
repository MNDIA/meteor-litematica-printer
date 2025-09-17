package com.kkllffaa.meteor_litematica_printer;

import com.google.common.collect.Streams;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;
import java.util.stream.Stream;

public class Parkour extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> edgeDistance = sgGeneral.add(new DoubleSetting.Builder()
            .name("edge-distance")
            .description("How far from the edge should you jump.")
            .range(0.001, 0.1)
            .defaultValue(0.001)
            .build());

    public Parkour() {
        super(Addon.CATEGORY, "parkour", "Automatically jumps at the edges of blocks.");
    }

    private boolean needJump = false;

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        mc.options.jumpKey.setPressed(needJump);
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!mc.player.isOnGround() || mc.options.jumpKey.isPressed()) {
            needJump = false;
        } else if (mc.player.isSneaking() || mc.options.sneakKey.isPressed()) {
            needJump = false;
        } else {
            Box box = mc.player.getBoundingBox();
            Box adjustedBox = box.offset(0, -0.5, 0).expand(-edgeDistance.get(), 0, -edgeDistance.get());

            Stream<VoxelShape> blockCollisions = Streams.stream(mc.world.getBlockCollisions(mc.player, adjustedBox));

            if (blockCollisions.findAny().isPresent()) {
                needJump = false;
            } else {
                needJump = true;
            }
        }

    }
}
