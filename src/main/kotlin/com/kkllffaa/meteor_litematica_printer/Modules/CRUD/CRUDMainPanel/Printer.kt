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
import net.minecraft.util.math.BlockPos
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
            .defaultValue(6)
            .min(1).sliderMin(1)
            .max(1024).sliderMax(6)
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
    private val toSort = mutableListOf<BlockPos>()

    // Fade rendering: position -> remaining ticks
    private val placedFade = mutableListOf<FadeEntry>()

    // LRU cache using LinkedHashMap with accessOrder=true for automatic eviction
    private val positionCache = object : LinkedHashMap<BlockPos, Unit>(256, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<BlockPos, Unit>?): Boolean =
            size > cacheSize.get()
    }
    private var cacheCleanupTickTimer = 0

    // Pending interactions: position -> remaining interactions needed
    private val pendingInteractions = mutableMapOf<BlockPos, Int>()

    private data class FadeEntry(var remainingTicks: Int, val pos: BlockPos)

    private fun isPositionCached(pos: BlockPos): Boolean =
        enableCache.get() && pos in positionCache

    private fun addToCache(pos: BlockPos) {
        if (enableCache.get()) {
            positionCache[pos] = Unit
        }
    }

    override fun onActivate() {
        onDeactivate()
    }

    override fun onDeactivate() {
        placedFade.clear()
        positionCache.clear()
        cacheCleanupTickTimer = 0
        pendingInteractions.clear()
    }

    @EventHandler
    private fun onTick(event: TickEvent.Post) {
        val player = mc.player ?: return placedFade.clear()
        val world = mc.world ?: return placedFade.clear()

        // Update fade entries and remove expired ones
        placedFade.onEach { it.remainingTicks-- }
            .removeIf { it.remainingTicks <= 0 }

        // Periodic cache cleanup
        if (enableCache.get()) {
            cacheCleanupTickTimer++
            if (cacheCleanupTickTimer >= cacheCleanupInterval.get() * 20) {
                positionCache.clear()
                cacheCleanupTickTimer = 0
            }
        }

        val worldSchematic = SchematicWorldHandler.getSchematicWorld() ?: run {
            placedFade.clear()
            toggle()
            return
        }

        toSort.clear()

        if (timer >= printing_delay.get()) {
            val range = printing_range.get() + 1
            BlockIterator.register(range, range) { pos, blockState ->
                val required = worldSchematic.getBlockState(pos)

                // Handle pending interactions
                if (启用交互.get() && pos !in pendingInteractions) {
                    blockState.needInteractionCountsTo(required)
                        .takeIf { it > 0 }
                        ?.let { pendingInteractions[BlockPos(pos)] = it }
                }

                // Add to sort list if valid
                if (!isPositionCached(pos) && DataManager.getRenderLayerRange().isPositionWithinRange(pos)) {
                    if (!whitelistenabled.get() || required.block in whitelist.get()) {
                        toSort += BlockPos(pos)
                    }
                }
            }

            BlockIterator.after {
                // Apply sorting algorithms
                if (firstAlgorithm.get() != SortAlgorithm.None) {
                    if (firstAlgorithm.get().applySecondSorting && secondAlgorithm.get() != SortingSecond.None) {
                        toSort.sortWith(secondAlgorithm.get().algorithm)
                    }
                    toSort.sortWith(firstAlgorithm.get().algorithm)
                }

                var placed = 0

                // Process pending interactions using iterator with removal
                pendingInteractions.entries.iterator().run {
                    while (hasNext() && placed < blocksPerTick.get()) {
                        val (pos, remaining) = next()
                        if (remaining <= 0) {
                            remove()
                            return@run
                        }

                        val toDo = min(remaining, blocksPerTick.get() - placed)
                        val did = pos.TryInteractIt(toDo)

                        if (did > 0) timer = 0
                        placed += did

                        val newRemaining = remaining - did
                        if (newRemaining <= 0) remove()
                        else pendingInteractions[pos] = newRemaining
                    }
                }

                // Place blocks
                for (pos in toSort) {
                    if (placed >= blocksPerTick.get()) break

                    val state = worldSchematic.getBlockState(pos)
                    if (state.TryPlaceIt(pos)) {
                        timer = 0
                        placed++
                        addToCache(pos)

                        if (renderBlocks.get()) {
                            placedFade += FadeEntry(fadeTime.get(), BlockPos(pos))
                        }
                    }
                }
            }
        } else {
            timer++
        }
    }

    @EventHandler
    private fun onRender(event: Render3DEvent) {
        placedFade.forEach { (remainingTicks, blockPos) ->
            val alphaRatio = remainingTicks.toFloat() / fadeTime.get()
            val alpha = (alphaRatio * colour.get().a).toInt()
            val sideColor = Color(colour.get().r, colour.get().g, colour.get().b, alpha)
            event.renderer.box(blockPos, sideColor, null, ShapeMode.Sides, 0)
        }
    }

    enum class SortAlgorithm(val applySecondSorting: Boolean, val algorithm: Comparator<BlockPos>) {
        None(false, Comparator { _, _ -> 0 }),
        TopDown(true, compareBy { -it.y }),
        DownTop(true, compareBy { it.y }),
        Nearest(false, compareBy {
            MeteorClient.mc.player?.let { player ->
                Utils.squaredDistance(
                    player.x, player.y, player.z,
                    it.x + 0.5, it.y + 0.5, it.z + 0.5
                )
            } ?: 0.0
        }),
        Furthest(false, compareByDescending {
            MeteorClient.mc.player?.let { player ->
                Utils.squaredDistance(
                    player.x, player.y, player.z,
                    it.x + 0.5, it.y + 0.5, it.z + 0.5
                )
            } ?: 0.0
        });
    }

    enum class SortingSecond(val algorithm: Comparator<BlockPos>) {
        None(SortAlgorithm.None.algorithm),
        Nearest(SortAlgorithm.Nearest.algorithm),
        Furthest(SortAlgorithm.Furthest.algorithm);
    }
}
