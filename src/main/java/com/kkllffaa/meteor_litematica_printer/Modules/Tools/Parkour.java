package com.kkllffaa.meteor_litematica_printer.Modules.Tools;

import com.google.common.collect.Streams;
import com.kkllffaa.meteor_litematica_printer.Addon;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.misc.input.Input;
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

    private final Setting<Double> minSpeed = sgGeneral.add(new DoubleSetting.Builder()
            .name("min-speed")
            .description("Minimum horizontal speed required to trigger automatic jumping.(block/tick)")
            .range(0.0, 0.39285)
            .defaultValue(0.085)
            .build());

    public Parkour() {
        super(Addon.TOOLS, "parkour", "Automatically jumps at the edges of blocks.");
    }

    private boolean isForcedJumping = false;


    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (isForcedJumping){
            mc.options.jumpKey.setPressed(true);
        }else{
            if (Input.isPressed(mc.options.jumpKey)) {
                if (isLostControl()) {
                    mc.options.jumpKey.setPressed(false);
                } else {
                    mc.options.jumpKey.setPressed(true);
                }
            }else{
                mc.options.jumpKey.setPressed(false);
            }
        }
    }


    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (isLostControl()) {
            isForcedJumping = false;
            return;
        }

        if (!mc.player.isOnGround() || mc.options.jumpKey.isPressed()||
        mc.player.isSneaking() || mc.options.sneakKey.isPressed()) {
            isForcedJumping = false;
        } else {
            double horizontalSpeed = Math.sqrt(mc.player.getVelocity().x * mc.player.getVelocity().x + mc.player.getVelocity().z * mc.player.getVelocity().z);
            if (horizontalSpeed < minSpeed.get()) {
                isForcedJumping = false;
            } else {
                Box box = mc.player.getBoundingBox();
                Box adjustedBox = box.offset(0, -0.5, 0).expand(-edgeDistance.get(), 0, -edgeDistance.get());

                Stream<VoxelShape> blockCollisions = Streams.stream(mc.world.getBlockCollisions(mc.player, adjustedBox));

                if (blockCollisions.findAny().isPresent()) {
                    isForcedJumping = false;
                } else {
                    isForcedJumping = true;
                }
            }
        }

    }


    private boolean isLostControl() {
        return mc.cameraEntity != mc.player || mc.currentScreen != null || Modules.get().isActive(meteordevelopment.meteorclient.systems.modules.render.Freecam.class);
    }
}
