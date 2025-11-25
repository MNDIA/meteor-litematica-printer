package com.kkllffaa.meteor_litematica_printer.Modules.Tools.OnlyESP

import com.kkllffaa.meteor_litematica_printer.Addon
import meteordevelopment.meteorclient.events.render.Render2DEvent
import meteordevelopment.meteorclient.events.render.Render3DEvent
import meteordevelopment.meteorclient.renderer.Renderer2D
import meteordevelopment.meteorclient.renderer.ShapeMode
import meteordevelopment.meteorclient.settings.*
import meteordevelopment.meteorclient.systems.modules.Module
import meteordevelopment.meteorclient.systems.modules.render.blockesp.ESPBlockData
import meteordevelopment.meteorclient.utils.Utils
import meteordevelopment.meteorclient.utils.entity.EntityUtils
import meteordevelopment.meteorclient.utils.render.NametagUtils
import meteordevelopment.meteorclient.utils.render.RenderUtils
import meteordevelopment.meteorclient.utils.render.WireframeEntityRenderer
import meteordevelopment.meteorclient.utils.render.color.SettingColor
import meteordevelopment.orbit.EventHandler
import net.minecraft.block.Block
import net.minecraft.entity.ItemEntity
import net.minecraft.item.BlockItem
import net.minecraft.util.math.MathHelper
import org.joml.Vector3d
import java.util.function.Consumer

class ItemFinder : Module(Addon.TOOLS, "esp-Item-Entity", "Renders items through walls.") {
    private val sgGeneral: SettingGroup = settings.getDefaultGroup()
    private val sgColors: SettingGroup = settings.createGroup("Colors")

    //region General
    private val mode: Setting<Mode?> = sgGeneral.add<Mode?>(
        EnumSetting.Builder<Mode?>()
            .name("mode")
            .description("Rendering mode.")
            .defaultValue(Mode.Wireframe)
            .build()
    )

    //endregion
    //region Colors
    private val blocks: Setting<MutableList<Block?>?> = sgColors.add<MutableList<Block?>?>(
        BlockListSetting.Builder()
            .name("blocks")
            .description("Blocks to search for.")
            .onChanged(Consumer { blocks1: MutableList<Block?>? ->
                if (isActive() && Utils.canUpdate()) onActivate()
            })
            .build()
    )

    private val defaultBlockConfig: Setting<ESPBlockData?> = sgColors.add<ESPBlockData?>(
        GenericSetting.Builder<ESPBlockData?>()
            .name("default-block-config")
            .description("Default block config.")
            .defaultValue(
                ESPBlockData(
                    ShapeMode.Lines,
                    SettingColor(0, 255, 0),
                    SettingColor(0, 255, 0, 25),
                    true,
                    SettingColor(0, 255, 0, 125)
                )
            )
            .build()
    )

    private val blockConfigs: Setting<MutableMap<Block?, ESPBlockData?>?> =
        sgColors.add<MutableMap<Block?, ESPBlockData?>?>(
            BlockDataSetting.Builder<ESPBlockData?>()
                .name("block-configs")
                .description("Config for each block.")
                .defaultData(defaultBlockConfig)
                .build()
        )


    //endregion
    private val pos1 = Vector3d()
    private val pos2 = Vector3d()
    private val pos = Vector3d()

    private var count = 0

    // Box
    @EventHandler
    private fun onRender3D(event: Render3DEvent) {
        if (mode.get() == Mode._2D) return

        count = 0

        for (entity in mc.world!!.getEntities()) {
            if (entity !is ItemEntity || !EntityUtils.isInRenderDistance(entity)) continue
            drawBoundingBox(event, entity)
        }
    }

    private fun drawBoundingBox(event: Render3DEvent, itemEntity: ItemEntity) {
        val renderData = getItemColorData(itemEntity)
        if (renderData == null) return

        count++
        if (mode.get() == Mode.Wireframe) {
            WireframeEntityRenderer.render(
                event,
                itemEntity,
                1.0,
                renderData.sideColor,
                renderData.lineColor,
                renderData.shapeMode
            )
        } else if (mode.get() == Mode.Box) {
            val x = MathHelper.lerp(
                event.tickDelta.toDouble(),
                itemEntity.lastRenderX,
                itemEntity.getX()
            ) - itemEntity.getX()
            val y = MathHelper.lerp(
                event.tickDelta.toDouble(),
                itemEntity.lastRenderY,
                itemEntity.getY()
            ) - itemEntity.getY()
            val z = MathHelper.lerp(
                event.tickDelta.toDouble(),
                itemEntity.lastRenderZ,
                itemEntity.getZ()
            ) - itemEntity.getZ()

            val box = itemEntity.getBoundingBox()
            event.renderer.box(
                x + box.minX,
                y + box.minY,
                z + box.minZ,
                x + box.maxX,
                y + box.maxY,
                z + box.maxZ,
                renderData.sideColor,
                renderData.lineColor,
                renderData.shapeMode,
                0
            )
        }
        TryRenderTracer(event, itemEntity, renderData)
    }

    private fun TryRenderTracer(event: Render3DEvent, itemEntity: ItemEntity, renderData: ESPBlockData) {
        if (renderData.tracer) {
            event.renderer.line(
                RenderUtils.center.x, RenderUtils.center.y, RenderUtils.center.z,
                itemEntity.getX(), itemEntity.getY(), itemEntity.getZ(),
                renderData.tracerColor
            )
        }
    }

    // 2D
    @EventHandler
    private fun onRender2D(event: Render2DEvent) {
        if (mode.get() != Mode._2D) return

        Renderer2D.COLOR.begin()
        count = 0

        for (entity in mc.world!!.getEntities()) {
            if (entity !is ItemEntity || !EntityUtils.isInRenderDistance(entity)) continue

            val box = entity.getBoundingBox()

            val x = MathHelper.lerp(event.tickDelta.toDouble(), entity.lastRenderX, entity.getX()) - entity.getX()
            val y = MathHelper.lerp(event.tickDelta.toDouble(), entity.lastRenderY, entity.getY()) - entity.getY()
            val z = MathHelper.lerp(event.tickDelta.toDouble(), entity.lastRenderZ, entity.getZ()) - entity.getZ()

            // Check corners
            pos1.set(Double.Companion.MAX_VALUE, Double.Companion.MAX_VALUE, Double.Companion.MAX_VALUE)
            pos2.set(0.0, 0.0, 0.0)

            //     Bottom
            if (checkCorner(box.minX + x, box.minY + y, box.minZ + z, pos1, pos2)) continue
            if (checkCorner(box.maxX + x, box.minY + y, box.minZ + z, pos1, pos2)) continue
            if (checkCorner(box.minX + x, box.minY + y, box.maxZ + z, pos1, pos2)) continue
            if (checkCorner(box.maxX + x, box.minY + y, box.maxZ + z, pos1, pos2)) continue

            //     Top
            if (checkCorner(box.minX + x, box.maxY + y, box.minZ + z, pos1, pos2)) continue
            if (checkCorner(box.maxX + x, box.maxY + y, box.minZ + z, pos1, pos2)) continue
            if (checkCorner(box.minX + x, box.maxY + y, box.maxZ + z, pos1, pos2)) continue
            if (checkCorner(box.maxX + x, box.maxY + y, box.maxZ + z, pos1, pos2)) continue

            // Setup color
            val renderData = getItemColorData(entity)
            if (renderData == null) continue


            // Render
            if (renderData.shapeMode != ShapeMode.Lines && renderData.sideColor.a > 0) {
                Renderer2D.COLOR.quad(pos1.x, pos1.y, pos2.x - pos1.x, pos2.y - pos1.y, renderData.sideColor)
            }

            if (renderData.shapeMode != ShapeMode.Sides) {
                Renderer2D.COLOR.line(pos1.x, pos1.y, pos1.x, pos2.y, renderData.lineColor)
                Renderer2D.COLOR.line(pos2.x, pos1.y, pos2.x, pos2.y, renderData.lineColor)
                Renderer2D.COLOR.line(pos1.x, pos1.y, pos2.x, pos1.y, renderData.lineColor)
                Renderer2D.COLOR.line(pos1.x, pos2.y, pos2.x, pos2.y, renderData.lineColor)
            }

            count++
        }

        Renderer2D.COLOR.render()
    }

    private fun checkCorner(x: Double, y: Double, z: Double, min: Vector3d, max: Vector3d): Boolean {
        pos.set(x, y, z)
        if (!NametagUtils.to2D(pos, 1.0)) return true

        // Check Min
        if (pos.x < min.x) min.x = pos.x
        if (pos.y < min.y) min.y = pos.y
        if (pos.z < min.z) min.z = pos.z

        // Check Max
        if (pos.x > max.x) max.x = pos.x
        if (pos.y > max.y) max.y = pos.y
        if (pos.z > max.z) max.z = pos.z

        return false
    }

    // Utils
    private fun getItemColorData(itemEntity: ItemEntity): ESPBlockData? {
        val stack = itemEntity.getStack()
        val item = stack.getItem()
        if (item is BlockItem) {
            val block = item.getBlock()
            if (blocks.get()!!.contains(block)) {
                return getBlockData(block)
            }
        }
        return null
    }

    private fun getBlockData(block: Block?): ESPBlockData? {
        val blockData = blockConfigs.get()!!.get(block)
        return if (blockData == null) defaultBlockConfig.get() else blockData
    }

    override fun getInfoString(): String {
        return count.toString()
    }

    private enum class Mode {
        Box,
        Wireframe,
        _2D;

        override fun toString(): String {
            return if (this == Mode._2D) "2D" else super.toString()
        }
    }
}
