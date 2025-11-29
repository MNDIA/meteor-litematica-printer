package com.kkllffaa.meteor_litematica_printer.Modules.CRUD.CRUDMainPanel

import com.kkllffaa.meteor_litematica_printer.Addon
import com.kkllffaa.meteor_litematica_printer.Functions.*
import fi.dy.masa.litematica.data.DataManager
import fi.dy.masa.litematica.world.SchematicWorldHandler
import meteordevelopment.meteorclient.MeteorClient
import meteordevelopment.meteorclient.events.render.Render3DEvent
import meteordevelopment.meteorclient.events.world.TickEvent
import meteordevelopment.meteorclient.renderer.ShapeMode
import meteordevelopment.meteorclient.settings.*
import meteordevelopment.meteorclient.systems.modules.Module
import meteordevelopment.meteorclient.utils.Utils
import meteordevelopment.meteorclient.utils.render.color.Color
import meteordevelopment.meteorclient.utils.render.color.SettingColor
import meteordevelopment.meteorclient.utils.world.BlockIterator
import meteordevelopment.orbit.EventHandler
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.util.Pair
import net.minecraft.util.math.BlockPos
import java.util.function.Consumer
import java.util.function.ToDoubleFunction
import java.util.function.ToIntFunction
import kotlin.math.min

class Printer : Module(Addon.CRUD, "litematica-printer", "Automatically prints open schematics") {
    private val sgGeneral = settings.defaultGroup
    private val sgWhitelist = settings.createGroup("Whitelist")
    private val sgRendering = settings.createGroup("Rendering")

    private val sgCache = settings.createGroup("Cache")

    //region General settings
    private val printing_range: Setting<Int> = sgGeneral.add(
        IntSetting.Builder()
            .name("printing-range")
            .description("The block place range.")
            .defaultValue(2)
            .min(1).sliderMin(1)
            .max(6).sliderMax(6)
            .build()
    )

    private val printing_delay: Setting<Int> = sgGeneral.add(
        IntSetting.Builder()
            .name("printing-delay")
            .description("Delay between printing blocks in ticks.")
            .defaultValue(2)
            .min(0).sliderMin(0)
            .max(100).sliderMax(40)
            .build()
    )

    private val blocksPerTick: Setting<Int> = sgGeneral.add(
        IntSetting.Builder()
            .name("blocks/tick")
            .description("How many blocks place per tick.")
            .defaultValue(3)
            .min(1).sliderMin(1)
            .max(100).sliderMax(100)
            .build()
    )

    private val 启用交互: Setting<Boolean> = sgGeneral.add(
        BoolSetting.Builder()
            .name("enable-interaction")
            .description("Enable block interaction.")
            .defaultValue(true)
            .build()
    )

    private val firstAlgorithm: Setting<SortAlgorithm> = sgGeneral.add(
        EnumSetting.Builder<SortAlgorithm>()
            .name("first-sorting-mode")
            .description("The blocks you want to place first.")
            .defaultValue(SortAlgorithm.None)
            .build()
    )

    private val secondAlgorithm: Setting<SortingSecond> = sgGeneral.add(
        EnumSetting.Builder<SortingSecond>()
            .name("second-sorting-mode")
            .description("Second pass of sorting eg. place first blocks higher and closest to you.")
            .defaultValue(SortingSecond.None)
            .visible { firstAlgorithm.get().applySecondSorting }
            .build()
    )

    //endregion
    //region Whitelist settings
    private val whitelistenabled: Setting<Boolean> = sgWhitelist.add(
        BoolSetting.Builder()
            .name("whitelist-enabled")
            .description("Only place selected blocks.")
            .defaultValue(false)
            .build()
    )

    private val whitelist: Setting<MutableList<Block>> = sgWhitelist.add(
        BlockListSetting.Builder()
            .name("whitelist")
            .description("Blocks to place.")
            .visible { whitelistenabled.get() }
            .build()
    )

    //endregion
    //region Rendering settings
    private val renderBlocks: Setting<Boolean> = sgRendering.add(
        BoolSetting.Builder()
            .name("render-placed-blocks")
            .description("Renders block placements.")
            .defaultValue(true)
            .build()
    )

    private val fadeTime: Setting<Int> = sgRendering.add(
        IntSetting.Builder()
            .name("fade-time")
            .description("Time for the rendering to fade, in ticks.")
            .defaultValue(3)
            .min(1).sliderMin(1)
            .max(1000).sliderMax(20)
            .visible { renderBlocks.get() }
            .build()
    )

    private val colour: Setting<SettingColor> = sgRendering.add(
        ColorSetting.Builder()
            .name("colour")
            .description("The cubes colour.")
            .defaultValue(SettingColor(95, 190, 255))
            .visible { renderBlocks.get() }
            .build()
    )


    // endregion
    //region Cache settings
    private val enableCache: Setting<Boolean> = sgCache.add(
        BoolSetting.Builder()
            .name("enable-cache")
            .description("Enable position cache to prevent placing at the same position multiple times.")
            .defaultValue(true)
            .build()
    )

    private val cacheSize: Setting<Int> = sgCache.add(
        IntSetting.Builder()
            .name("cache-size")
            .description("Number of recent positions to cache.")
            .defaultValue(50)
            .min(10).sliderMin(10)
            .max(200).sliderMax(200)
            .visible { enableCache.get() }
            .build()
    )

    private val cacheCleanupInterval: Setting<Int> = sgCache.add(
        IntSetting.Builder()
            .name("cache-cleanup-interval")
            .description("Time in seconds between cache cleanups to prevent stale entries.")
            .defaultValue(2)
            .min(1).sliderMin(1)
            .max(10).sliderMax(10)
            .visible { enableCache.get() }
            .build()
    )


    //endregion
    private var timer = 0
    private val toSort: MutableList<BlockPos> = ArrayList<BlockPos>()
    private val placed_fade: MutableList<Pair<Int, BlockPos>> = ArrayList()

    // Position cache to prevent repeated placement attempts
    private val positionCache = LinkedHashSet<BlockPos>()
    private var cacheCleanupTickTimer = 0

    // Pending interactions: position -> remaining interactions needed
    private val pendingInteractions: MutableMap<BlockPos, Int> = HashMap()

    private fun isPositionCached(pos: BlockPos): Boolean {
        return enableCache.get() && pos in positionCache
    }

    private fun addToCache(pos: BlockPos) {
        if (!enableCache.get()) return

        positionCache.add(pos)


        // Remove oldest entries if cache exceeds limit
        while (positionCache.size > cacheSize.get()) {
            val iterator = positionCache.iterator()
            if (iterator.hasNext()) {
                iterator.next()
                iterator.remove()
            }
        }
    }

    override fun onActivate() {
        onDeactivate()
    }

    override fun onDeactivate() {
        placed_fade.clear()
        positionCache.clear()
        cacheCleanupTickTimer = 0
        pendingInteractions.clear()
    }

    @EventHandler
    private fun onTick(event: TickEvent.Post) {
        // TODO: if (Deleter.isBusy){
        // 	return;
        // }

        val player: ClientPlayerEntity = mc.player ?: run {
            placed_fade.clear()
            return
        }
        val world = mc.world ?: run {
            placed_fade.clear()
            return
        }

        placed_fade.forEach(Consumer { s: Pair<Int, BlockPos> -> s.left = s.getLeft() - 1 })
        placed_fade.removeIf { s: Pair<Int, BlockPos> -> s.getLeft() <= 0 }

        // Cache cleanup timer - clears cache periodically to prevent stale entries
        if (enableCache.get()) {
            cacheCleanupTickTimer++

            if (cacheCleanupTickTimer >= cacheCleanupInterval.get() * 20) {
                positionCache.clear()
                cacheCleanupTickTimer = 0
            }
        }
        val worldSchematic = SchematicWorldHandler.getSchematicWorld()
        if (worldSchematic == null) {
            placed_fade.clear()
            toggle()
            return
        }


        toSort.clear()


        if (timer >= printing_delay.get()) {
            BlockIterator.register(
                printing_range.get() + 1,
                printing_range.get() + 1
            ) { pos: BlockPos, blockState: BlockState ->
                val required = worldSchematic.getBlockState(pos)
                if (启用交互.get() && !pendingInteractions.containsKey(pos)) {
                    val requiredInteractions = blockState.needInteractionCountsTo(required)
                    if (requiredInteractions > 0) {
                        pendingInteractions[BlockPos(pos)] = requiredInteractions
                    }
                }
                if (player.blockPos.isWithinDistance(pos, printing_range.get().toDouble())
                    && blockState.isReplaceable
                    && required.fluidState.isEmpty
                    && !required.isAir && blockState.block !== required.block && DataManager.getRenderLayerRange()
                        .isPositionWithinRange(pos)

                    && !(isPositionCached(pos))
                ) {
                    if (!whitelistenabled.get() || whitelist.get().contains(required.block)) {
                        toSort.add(BlockPos(pos))
                    }
                }
            }

            BlockIterator.after {
                if (firstAlgorithm.get() != SortAlgorithm.None) {
                    if (firstAlgorithm.get().applySecondSorting) {
                        if (secondAlgorithm.get() != SortingSecond.None) {
                            toSort.sortWith(secondAlgorithm.get().algorithm)
                        }
                    }
                    toSort.sortWith(firstAlgorithm.get().algorithm)
                }
                var placed = 0

                val iterator = pendingInteractions.entries.iterator()
                while (iterator.hasNext() && placed < blocksPerTick.get()) {
                    val entry = iterator.next()
                    val pos = entry.key
                    val remaining: Int = entry.value
                    if (remaining > 0) {
                        val toDo = min(remaining, blocksPerTick.get() - placed)
                        val did = pos.TryInteractIt(toDo)
                        if (did > 0) {
                            timer = 0
                        }
                        placed += did
                        val newRemaining = remaining - did
                        if (newRemaining <= 0) {
                            iterator.remove()
                        } else {
                            entry.setValue(newRemaining)
                        }
                    } else {
                        iterator.remove()
                    }
                }
                for (pos in toSort) {
                    val state = worldSchematic.getBlockState(pos)

                    if (state.TryPlaceIt(pos)) {
                        timer = 0
                        placed++
                        addToCache(pos)

                        if (renderBlocks.get()) {
                            placed_fade.add(Pair<Int, BlockPos>(fadeTime.get(), BlockPos(pos)))
                        }
                        if (placed >= blocksPerTick.get()) {
                            return@after
                        }
                    }
                }
            }
        } else timer++
    }

    @EventHandler
    private fun onRender(event: Render3DEvent) {
        placed_fade.forEach(Consumer { fadeEntry: Pair<Int, BlockPos> ->
            val remainingTicks: Int = fadeEntry.getLeft()
            val alphaRatio = remainingTicks.toFloat() / fadeTime.get()
            val alpha = (alphaRatio * colour.get().a).toInt()
            val sideColor = Color(colour.get().r, colour.get().g, colour.get().b, alpha)

            val blockPos = fadeEntry.getRight()
            event.renderer.box(blockPos, sideColor, null, ShapeMode.Sides, 0)
        })
    }

    enum class SortAlgorithm(val applySecondSorting: Boolean, algorithm: Comparator<BlockPos>) {
        None(false, Comparator { a: BlockPos, b: BlockPos -> 0 }),
        TopDown(true, Comparator.comparingInt<BlockPos>(ToIntFunction { value: BlockPos -> value.y * -1 })),
        DownTop(true, Comparator.comparingInt<BlockPos>(ToIntFunction { obj: BlockPos -> obj.y })),
        Nearest(
            false,
            Comparator.comparingDouble<BlockPos>(ToDoubleFunction { value: BlockPos ->
                val player = MeteorClient.mc.player
                if (player != null) Utils.squaredDistance(
                    player.x,
                    player.y,
                    player.z,
                    value.x + 0.5,
                    value.y + 0.5,
                    value.z + 0.5
                ) else 0.0
            })
        ),
        Furthest(
            false,
            Comparator.comparingDouble<BlockPos>(ToDoubleFunction { value: BlockPos ->
                val player = MeteorClient.mc.player
                if (player != null) (Utils.squaredDistance(
                    player.x,
                    player.y,
                    player.z,
                    value.x + 0.5,
                    value.y + 0.5,
                    value.z + 0.5
                )) * -1 else 0.0
            })
        );


        val algorithm: Comparator<BlockPos>

        init {
            this.algorithm = algorithm
        }
    }

    enum class SortingSecond(algorithm: Comparator<BlockPos>) {
        None(SortAlgorithm.None.algorithm),
        Nearest(SortAlgorithm.Nearest.algorithm),
        Furthest(SortAlgorithm.Furthest.algorithm);

        val algorithm: Comparator<BlockPos>

        init {
            this.algorithm = algorithm
        }
    }
}
