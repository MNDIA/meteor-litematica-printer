package com.kkllffaa.meteor_litematica_printer.Modules.Tools.OnlyESP;

import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.render.blockesp.ESPBlockData;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.WireframeEntityRenderer;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;

import java.util.List;
import java.util.Map;

import org.joml.Vector3d;

import com.kkllffaa.meteor_litematica_printer.Addon;


public class ItemFinder extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgColors = settings.createGroup("Colors");

    //region General

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("Rendering mode.")
        .defaultValue(Mode.Wireframe)
        .build()
    );

    //endregion

    //region Colors

    private final Setting<List<Block>> blocks = sgColors.add(new BlockListSetting.Builder()
        .name("blocks")
        .description("Blocks to search for.")
        .onChanged(blocks1 -> {
            if (isActive() && Utils.canUpdate()) onActivate();
        })
        .build()
    );

    private final Setting<ESPBlockData> defaultBlockConfig = sgColors.add(new GenericSetting.Builder<ESPBlockData>()
        .name("default-block-config")
        .description("Default block config.")
        .defaultValue(
            new ESPBlockData(
                ShapeMode.Lines,
                new SettingColor(0, 255, 0),
                new SettingColor(0, 255, 0, 25),
                true,
                new SettingColor(0, 255, 0, 125)
            )
        )
        .build()
    );

    private final Setting<Map<Block, ESPBlockData>> blockConfigs = sgColors.add(new BlockDataSetting.Builder<ESPBlockData>()
        .name("block-configs")
        .description("Config for each block.")
        .defaultData(defaultBlockConfig)
        .build()
    );



    //endregion

    private final Vector3d pos1 = new Vector3d();
    private final Vector3d pos2 = new Vector3d();
    private final Vector3d pos = new Vector3d();

    private int count;

    public ItemFinder() {
        super(Addon.TOOLS, "esp-Item-Entity", "Renders items through walls.");
    }

    // Box

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (mode.get() == Mode._2D) return;

        count = 0;

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof ItemEntity itemEntity) || !EntityUtils.isInRenderDistance(entity)) continue;
            drawBoundingBox(event, itemEntity);
        }
    }

    private void drawBoundingBox(Render3DEvent event, ItemEntity itemEntity) {
        ESPBlockData renderData = getItemColorData(itemEntity);
        if (renderData == null) return;

        count++;
        if (mode.get() == Mode.Wireframe) {
            WireframeEntityRenderer.render(event, itemEntity, 1, renderData.sideColor, renderData.lineColor, renderData.shapeMode);
        } else if (mode.get() == Mode.Box) {
            double x = MathHelper.lerp(event.tickDelta, itemEntity.lastRenderX, itemEntity.getX()) - itemEntity.getX();
            double y = MathHelper.lerp(event.tickDelta, itemEntity.lastRenderY, itemEntity.getY()) - itemEntity.getY();
            double z = MathHelper.lerp(event.tickDelta, itemEntity.lastRenderZ, itemEntity.getZ()) - itemEntity.getZ();

            Box box = itemEntity.getBoundingBox();
            event.renderer.box(x + box.minX, y + box.minY, z + box.minZ, x + box.maxX, y + box.maxY, z + box.maxZ, renderData.sideColor, renderData.lineColor, renderData.shapeMode, 0);
        }
        TryRenderTracer(event, itemEntity, renderData);
    }

    private void TryRenderTracer(Render3DEvent event, ItemEntity itemEntity, ESPBlockData renderData) {
        if (renderData.tracer) {
            event.renderer.line(RenderUtils.center.x, RenderUtils.center.y, RenderUtils.center.z,
                itemEntity.getX(), itemEntity.getY(), itemEntity.getZ(),
                renderData.tracerColor);
        }
    }

    // 2D

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (mode.get() != Mode._2D) return;

        Renderer2D.COLOR.begin();
        count = 0;

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof ItemEntity itemEntity) || !EntityUtils.isInRenderDistance(entity)) continue;

            Box box = itemEntity.getBoundingBox();

            double x = MathHelper.lerp(event.tickDelta, itemEntity.lastRenderX, itemEntity.getX()) - itemEntity.getX();
            double y = MathHelper.lerp(event.tickDelta, itemEntity.lastRenderY, itemEntity.getY()) - itemEntity.getY();
            double z = MathHelper.lerp(event.tickDelta, itemEntity.lastRenderZ, itemEntity.getZ()) - itemEntity.getZ();

            // Check corners
            pos1.set(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE);
            pos2.set(0, 0, 0);

            //     Bottom
            if (checkCorner(box.minX + x, box.minY + y, box.minZ + z, pos1, pos2)) continue;
            if (checkCorner(box.maxX + x, box.minY + y, box.minZ + z, pos1, pos2)) continue;
            if (checkCorner(box.minX + x, box.minY + y, box.maxZ + z, pos1, pos2)) continue;
            if (checkCorner(box.maxX + x, box.minY + y, box.maxZ + z, pos1, pos2)) continue;

            //     Top
            if (checkCorner(box.minX + x, box.maxY + y, box.minZ + z, pos1, pos2)) continue;
            if (checkCorner(box.maxX + x, box.maxY + y, box.minZ + z, pos1, pos2)) continue;
            if (checkCorner(box.minX + x, box.maxY + y, box.maxZ + z, pos1, pos2)) continue;
            if (checkCorner(box.maxX + x, box.maxY + y, box.maxZ + z, pos1, pos2)) continue;

            // Setup color
            ESPBlockData renderData = getItemColorData(itemEntity);
            if (renderData == null) continue;
            
            // Render
            if (renderData.shapeMode != ShapeMode.Lines && renderData.sideColor.a > 0) {
                Renderer2D.COLOR.quad(pos1.x, pos1.y, pos2.x - pos1.x, pos2.y - pos1.y, renderData.sideColor);
            }

            if (renderData.shapeMode != ShapeMode.Sides) {
                Renderer2D.COLOR.line(pos1.x, pos1.y, pos1.x, pos2.y, renderData.lineColor);
                Renderer2D.COLOR.line(pos2.x, pos1.y, pos2.x, pos2.y, renderData.lineColor);
                Renderer2D.COLOR.line(pos1.x, pos1.y, pos2.x, pos1.y, renderData.lineColor);
                Renderer2D.COLOR.line(pos1.x, pos2.y, pos2.x, pos2.y, renderData.lineColor);
            }

            count++;
        }

        Renderer2D.COLOR.render();
    }

    private boolean checkCorner(double x, double y, double z, Vector3d min, Vector3d max) {
        pos.set(x, y, z);
        if (!NametagUtils.to2D(pos, 1)) return true;

        // Check Min
        if (pos.x < min.x) min.x = pos.x;
        if (pos.y < min.y) min.y = pos.y;
        if (pos.z < min.z) min.z = pos.z;

        // Check Max
        if (pos.x > max.x) max.x = pos.x;
        if (pos.y > max.y) max.y = pos.y;
        if (pos.z > max.z) max.z = pos.z;

        return false;
    }

    // Utils
    private ESPBlockData getItemColorData(ItemEntity itemEntity) {
            ItemStack stack = itemEntity.getStack();
            Item item = stack.getItem();
            if (item instanceof BlockItem blockItem) {
                Block block = blockItem.getBlock();
                if (blocks.get().contains(block)) {
                    return getBlockData(block);
                }
            }
            return null;
    }
    private ESPBlockData getBlockData(Block block) {
        ESPBlockData blockData = blockConfigs.get().get(block);
        return blockData == null ? defaultBlockConfig.get() : blockData;
    }
    @Override
    public String getInfoString() {
        return Integer.toString(count);
    }

    private enum Mode {
        Box,
        Wireframe,
        _2D;

        @Override
        public String toString() {
            return this == _2D ? "2D" : super.toString();
        }
    }
}
