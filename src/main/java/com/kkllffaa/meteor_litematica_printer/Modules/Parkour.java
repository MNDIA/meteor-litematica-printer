package com.kkllffaa.meteor_litematica_printer.Modules;

import com.google.common.collect.Streams;
import com.kkllffaa.meteor_litematica_printer.Addon;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;
import org.lwjgl.glfw.GLFW;
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
            .description("Minimum horizontal speed required to trigger automatic jumping.")
            .range(0.0, 12.0)
            .defaultValue(4)
            .build());

    public Parkour() {
        super(Addon.TOOLSCATEGORY, "parkour", "Automatically jumps at the edges of blocks.");
    }

    private boolean isForcedJumping = false;


    @EventHandler
    private void onTick(TickEvent.Pre event) {
        long window = mc.getWindow().getHandle();
        int keyCode = mc.options.jumpKey.getDefaultKey().getCode();
        boolean isPhysicalJumpKeyActive = GLFW.glfwGetKey(window, keyCode) == GLFW.GLFW_PRESS;
        if (isForcedJumping){
            mc.options.jumpKey.setPressed(true);
        }else{
            if (isPhysicalJumpKeyActive) {
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
        return mc.cameraEntity != mc.player || mc.currentScreen != null;
    }
}
