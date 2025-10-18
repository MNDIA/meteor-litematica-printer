package com.kkllffaa.meteor_litematica_printer.Modules.Tools;

import com.google.common.collect.Streams;
import com.kkllffaa.meteor_litematica_printer.Addon;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.GUIMove;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;
import java.util.stream.Stream;


public class Parkour extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> edgeDistance = sgGeneral.add(new DoubleSetting.Builder()
            .name("edge-distance")
            .description("How far from the edge should you jump.")
            .range(0.001, 0.1)
            .defaultValue(0.004)
            .build());

    private final Setting<Double> minSpeed = sgGeneral.add(new DoubleSetting.Builder()
            .name("min-speed")
            .description("Minimum horizontal speed required to trigger automatic jumping.(block/tick)")
            .range(0.0, 0.39285)
            .defaultValue(0.085)
            .build());
    private final Setting<Double> 悬空高度 = sgGeneral.add(new DoubleSetting.Builder()
            .name("悬空高度")
            .description("脚下高度内没有碰撞才可以跳跃")
            .range(0.0001, 1.5)
            .defaultValue(0.1249)
            .build());
    private final Setting<Boolean> 垫幽灵砖 = sgGeneral.add( new BoolSetting.Builder()
            .name("垫幽灵砖")
            .description("Whether to place ghost blocks under the player.")
            .defaultValue(false)
            .build());

    public Parkour() {
        super(Addon.TOOLS, "parkour", "Automatically jumps at the edges of blocks.");
    }

    private boolean needEdgeJumping = false;

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        mc.options.jumpKey.setPressed(
            needEdgeJumping || (Input.isPressed(mc.options.jumpKey) && isPlayerInControl())
            );
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!mc.player.isOnGround() || mc.options.jumpKey.isPressed()||
        mc.player.isSneaking() || mc.options.sneakKey.isPressed()) {
            needEdgeJumping = false;
            if (垫幽灵砖.get()) {
                mc.world.setBlockState(mc.player.getBlockPos().down(), Blocks.STONE.getDefaultState());
            }
        } else {
            double horizontalSpeed = Math.sqrt(mc.player.getVelocity().x * mc.player.getVelocity().x + mc.player.getVelocity().z * mc.player.getVelocity().z);
            if (horizontalSpeed < minSpeed.get()) {
                needEdgeJumping = false;
            } else {
                Box adjustedBox = mc.player.getBoundingBox().offset(0, -悬空高度.get(), 0).expand(-edgeDistance.get(), 0, -edgeDistance.get());

                Stream<VoxelShape> blockCollisions = Streams.stream(mc.world.getBlockCollisions(mc.player, adjustedBox));

                if (blockCollisions.findAny().isPresent()) {
                    needEdgeJumping = false;
                } else {
                    needEdgeJumping = true;
                }
            }
        }

    }


    private boolean isPlayerInControl() {
         return mc.cameraEntity == mc.player && (mc.currentScreen == null || !Modules.get().get(GUIMove.class).skip()) && !Modules.get().isActive(meteordevelopment.meteorclient.systems.modules.render.Freecam.class);
    }
    

}
