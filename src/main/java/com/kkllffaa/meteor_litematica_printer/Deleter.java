package com.kkllffaa.meteor_litematica_printer;

import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.player.InstantRebreak;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import net.minecraft.world.LightType;
import net.minecraft.item.Item;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.shape.VoxelShape;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class Deleter extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");
    private final SettingGroup sgProtection = settings.createGroup("Mining Protection");
    private final SettingGroup sgCache = settings.createGroup("Cache");
    private final SettingGroup sgLighting = settings.createGroup("Auto Lighting");

    private final Set<Vec3i> upperNeighbours = Set.of(
        new Vec3i(0, 1, 0)
    );

    private final Set<Vec3i> sideNeighbours = Set.of(
        new Vec3i(0, 0, 1),
        new Vec3i(1, 0, 0), new Vec3i(-1, 0, 0),
        new Vec3i(0, 0, -1)
    );

    private final Set<Vec3i> lowerNeighbours = Set.of(
        new Vec3i(0, -1, 0)
    );

    private final Set<Vec3i> blockNeighbours = Set.of(
        new Vec3i(1, -1, 1), new Vec3i(0, -1, 1), new Vec3i(-1, -1, 1),
        new Vec3i(1, -1, 0), new Vec3i(0, -1, 0), new Vec3i(-1, -1, 0),
        new Vec3i(1, -1, -1), new Vec3i(0, -1, -1), new Vec3i(-1, -1, -1),

        new Vec3i(1, 0, 1), new Vec3i(0, 0, 1), new Vec3i(-1, 0, 1),
        new Vec3i(1, 0, 0), new Vec3i(-1, 0, 0),
        new Vec3i(1, 0, -1), new Vec3i(0, 0, -1), new Vec3i(-1, 0, -1),

        new Vec3i(1, 1, 1), new Vec3i(0, 1, 1), new Vec3i(-1, 1, 1),
        new Vec3i(1, 1, 0), new Vec3i(0, 1, 0), new Vec3i(-1, 1, 0),
        new Vec3i(1, 1, -1), new Vec3i(0, 1, -1), new Vec3i(-1, 1, -1)
    );

    // General

    private final Setting<List<Block>> selectedBlocks = sgGeneral.add(new BlockListSetting.Builder()
        .name("blocks")
        .description("Which blocks to select.")
        .defaultValue(Blocks.STONE, Blocks.DIRT, Blocks.GRASS_BLOCK)
        .build()
    );

    private final Setting<ListMode> mode = sgGeneral.add(new EnumSetting.Builder<ListMode>()
        .name("mode")
        .description("Selection mode.")
        .defaultValue(ListMode.Blacklist)
        .build()
    );

    private final Setting<Boolean> continuousMode = sgGeneral.add(new BoolSetting.Builder()
        .name("continuous-mode")
        .description("Continuously mine whitelist blocks around player position. Only works in whitelist mode.")
        .defaultValue(false)
        .visible(() -> mode.get() == ListMode.Whitelist)
        .build()
    );

    private final Setting<Integer> scanRadius = sgGeneral.add(new IntSetting.Builder()
        .name("scan-radius")
        .description("Radius to scan for blocks in continuous mode.")
        .defaultValue(5)
        .min(1)
        .max(10)
        .sliderRange(1, 10)
        .visible(() -> mode.get() == ListMode.Whitelist && continuousMode.get())
        .build()
    );

    private final Setting<Integer> depth = sgGeneral.add(new IntSetting.Builder()
        .name("depth")
        .description("Amount of iterations used to scan for similar blocks.")
        .defaultValue(3)
        .min(1)
        .sliderRange(1, 15)
        .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("Delay between mining blocks.")
        .defaultValue(0)
        .min(0)
        .sliderRange(0, 20)
        .build()
    );

    private final Setting<Integer> maxBlocksPerTick = sgGeneral.add(new IntSetting.Builder()
        .name("max-blocks-per-tick")
        .description("Maximum blocks to try to mine per tick. Useful when insta mining.")
        .defaultValue(1)
        .min(1)
        .max(1024)
        .build()
    );

    private final Setting<RandomDelayMode> randomDelayMode = sgGeneral.add(new EnumSetting.Builder<RandomDelayMode>()
        .name("random-delay-mode")
        .description("Random delay distribution pattern.")
        .defaultValue(RandomDelayMode.Balanced)
        .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Sends rotation packets to the server when mining.")
        .defaultValue(true)
        .build()
    );

    // Render

    private final Setting<Boolean> swingHand = sgRender.add(new BoolSetting.Builder()
        .name("swing-hand")
        .description("Swing hand client-side.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
        .name("render")
        .description("Whether or not to render the block being mined.")
        .defaultValue(true)
        .build()
    );

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How the shapes are rendered.")
        .defaultValue(ShapeMode.Both)
        .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("side-color")
        .description("The color of the sides of the blocks being rendered.")
        .defaultValue(new SettingColor(204, 0, 0, 10))
        .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("line-color")
        .description("The color of the lines of the blocks being rendered.")
        .defaultValue(new SettingColor(204, 0, 0, 255))
        .build()
    );

    // Cache Settings
    private final Setting<Boolean> enableCache = sgCache.add(new BoolSetting.Builder()
        .name("enable-cache")
        .description("Enable position cache to prevent mining the same block multiple times (prevents rebounding block issues).")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> cacheSize = sgCache.add(new IntSetting.Builder()
        .name("cache-size")
        .description("Number of recently mined positions to cache.")
        .defaultValue(50)
        .min(10).sliderMin(10)
        .max(200).sliderMax(200)
        .visible(enableCache::get)
        .build()
    );

    private final Setting<Integer> cacheCleanupInterval = sgCache.add(new IntSetting.Builder()
        .name("cache-cleanup-interval")
        .description("Time in seconds between cache cleanups to prevent stale entries.")
        .defaultValue(5)
        .min(1).sliderMin(1)
        .max(15).sliderMax(15)
        .visible(enableCache::get)
        .build()
    );

    private final Setting<Boolean> renderReboundCache = sgCache.add(new BoolSetting.Builder()
        .name("render-rebound-cache")
        .description("Render green outlines for blocks in rebound cache.")
        .defaultValue(true)
        .visible(enableCache::get)
        .build()
    );

    // Protection Settings
    private final Setting<Boolean> fluidProtection = sgProtection.add(new BoolSetting.Builder()
        .name("fluid-protection")
        .description("Prevent mining blocks that are adjacent to fluids (water, lava, etc.).")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> customProtection = sgProtection.add(new BoolSetting.Builder()
        .name("custom-protection")
        .description("Prevent mining blocks that are adjacent to blocks from the protection lists.")
        .defaultValue(false)
        .build()
    );

    private final Setting<List<Block>> protectedBlocksUpper = sgProtection.add(new BlockListSetting.Builder()
        .name("protected-blocks-upper")
        .description("Blocks to avoid mining near (upper direction). Mining will be prevented if target block has any of these blocks above it.")
        .defaultValue(Blocks.LAVA, Blocks.WATER, Blocks.SAND, Blocks.GRAVEL, Blocks.RED_SAND, Blocks.ANVIL, Blocks.CHIPPED_ANVIL, Blocks.DAMAGED_ANVIL)
        .visible(customProtection::get)
        .build()
    );

    private final Setting<List<Block>> protectedBlocksSide = sgProtection.add(new BlockListSetting.Builder()
        .name("protected-blocks-side")
        .description("Blocks to avoid mining near (side directions). Mining will be prevented if target block has any of these blocks around its sides.")
        .defaultValue(Blocks.LAVA, Blocks.WATER)
        .visible(customProtection::get)
        .build()
    );

    private final Setting<List<Block>> protectedBlocksLower = sgProtection.add(new BlockListSetting.Builder()
        .name("protected-blocks-lower")
        .description("Blocks to avoid mining near (lower direction). Mining will be prevented if target block has any of these blocks below it.")
        .defaultValue(Blocks.LAVA, Blocks.WATER)
        .visible(customProtection::get)
        .build()
    );

    private final Setting<Boolean> timeoutProtection = sgProtection.add(new BoolSetting.Builder()
        .name("timeout-protection")
        .description("Skip blocks that take too long to mine and add them to cache.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> miningTimeout = sgProtection.add(new DoubleSetting.Builder()
        .name("mining-timeout")
        .description("Maximum time in seconds to spend mining a single block before skipping it.")
        .defaultValue(5.0)
        .min(0.5)
        .max(30.0)
        .sliderRange(0.5, 15.0)
        .visible(timeoutProtection::get)
        .build()
    );

    private final Setting<Boolean> distanceProtection = sgProtection.add(new BoolSetting.Builder()
        .name("distance-protection")
        .description("Prevent mining blocks that are too far or too close to the player.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> minDistance = sgProtection.add(new DoubleSetting.Builder()
        .name("min-distance")
        .description("Minimum distance from player to mine blocks.")
        .defaultValue(1.0)
        .min(0.0)
        .max(10.0)
        .sliderRange(0.0, 5.0)
        .visible(distanceProtection::get)
        .build()
    );

    private final Setting<Double> maxDistance = sgProtection.add(new DoubleSetting.Builder()
        .name("max-distance")
        .description("Maximum distance from player to mine blocks.")
        .defaultValue(4.5)
        .min(1.0)
        .max(10.0)
        .sliderRange(1.0, 8.0)
        .visible(distanceProtection::get)
        .build()
    );

    private final Setting<Boolean> heightProtection = sgProtection.add(new BoolSetting.Builder()
        .name("height-protection")
        .description("Prevent mining blocks outside specified height range.")
        .defaultValue(false)
        .build()
    );

    private final Setting<HeightReferenceMode> heightReferenceMode = sgProtection.add(new EnumSetting.Builder<HeightReferenceMode>()
        .name("height-reference")
        .description("Reference system for height protection.")
        .defaultValue(HeightReferenceMode.Player)
        .visible(heightProtection::get)
        .build()
    );

    private final Setting<Integer> minHeight = sgProtection.add(new IntSetting.Builder()
        .name("min-height")
        .description("Minimum height for mining blocks (relative to reference).")
        .defaultValue(-5)
        .min(-64)
        .max(320)
        .sliderRange(-20, 20)
        .visible(heightProtection::get)
        .build()
    );

    private final Setting<Integer> maxHeight = sgProtection.add(new IntSetting.Builder()
        .name("max-height")
        .description("Maximum height for mining blocks (relative to reference).")
        .defaultValue(5)
        .min(-64)
        .max(320)
        .sliderRange(-20, 20)
        .visible(heightProtection::get)
        .build()
    );

    private final Setting<Boolean> regionProtection = sgProtection.add(new BoolSetting.Builder()
        .name("region-protection")
        .description("Only mine blocks within a defined 3D region (world coordinates).")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> region1X = sgProtection.add(new IntSetting.Builder()
        .name("region-x1")
        .description("First corner X coordinate of the mining region.")
        .defaultValue(0)
        .min(-30000000)
        .max(30000000)
        .visible(regionProtection::get)
        .build()
    );

    private final Setting<Integer> region1Y = sgProtection.add(new IntSetting.Builder()
        .name("region-y1")
        .description("First corner Y coordinate of the mining region.")
        .defaultValue(0)
        .min(-64)
        .max(320)
        .visible(regionProtection::get)
        .build()
    );

    private final Setting<Integer> region1Z = sgProtection.add(new IntSetting.Builder()
        .name("region-z1")
        .description("First corner Z coordinate of the mining region.")
        .defaultValue(0)
        .min(-30000000)
        .max(30000000)
        .visible(regionProtection::get)
        .build()
    );

    private final Setting<Integer> region2X = sgProtection.add(new IntSetting.Builder()
        .name("region-x2")
        .description("Second corner X coordinate of the mining region.")
        .defaultValue(10)
        .min(-30000000)
        .max(30000000)
        .visible(regionProtection::get)
        .build()
    );

    private final Setting<Integer> region2Y = sgProtection.add(new IntSetting.Builder()
        .name("region-y2")
        .description("Second corner Y coordinate of the mining region.")
        .defaultValue(10)
        .min(-64)
        .max(320)
        .visible(regionProtection::get)
        .build()
    );

    private final Setting<Integer> region2Z = sgProtection.add(new IntSetting.Builder()
        .name("region-z2")
        .description("Second corner Z coordinate of the mining region.")
        .defaultValue(10)
        .min(-30000000)
        .max(30000000)
        .visible(regionProtection::get)
        .build()
    );

    private final Setting<Boolean> directionalProtection = sgProtection.add(new BoolSetting.Builder()
        .name("directional-protection")
        .description("Only mine blocks in the direction the player is facing (based on yaw angle).")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> directionalAngle = sgProtection.add(new IntSetting.Builder()
        .name("directional-angle")
        .description("Angle range in degrees from player's yaw direction (total range = 2 * this value).")
        .defaultValue(90)
        .min(30)
        .max(180)
        .sliderRange(30, 180)
        .visible(directionalProtection::get)
        .build()
    );

    // Auto Lighting Settings
    private final Setting<Boolean> autoLighting = sgLighting.add(new BoolSetting.Builder()
        .name("auto-lighting")
        .description("Automatically place light sources on dark blocks to prevent mob spawning.")
        .defaultValue(false)
        .build()
    );

    private final Setting<List<Block>> lightSources = sgLighting.add(new BlockListSetting.Builder()
        .name("light-sources")
        .description("List of blocks to use as light sources.")
        .defaultValue(Blocks.TORCH, Blocks.SEA_LANTERN)
        .visible(autoLighting::get)
        .build()
    );

    private final Setting<Integer> lightLevelThreshold = sgLighting.add(new IntSetting.Builder()
        .name("light-level-threshold")
        .description("Place light sources on blocks with light level below this value.")
        .defaultValue(8)
        .min(0)
        .max(15)
        .sliderRange(0, 15)
        .visible(autoLighting::get)
        .build()
    );

    private final Setting<Integer> lightingScanRadius = sgLighting.add(new IntSetting.Builder()
        .name("lighting-scan-radius")
        .description("Radius to scan for dark blocks around player position.")
        .defaultValue(8)
        .min(1)
        .max(16)
        .sliderRange(1, 16)
        .visible(autoLighting::get)
        .build()
    );

    private final Setting<Integer> lightingDelay = sgLighting.add(new IntSetting.Builder()
        .name("lighting-delay")
        .description("Delay between placing light sources (in ticks).")
        .defaultValue(10)
        .min(1)
        .max(60)
        .sliderRange(1, 40)
        .visible(autoLighting::get)
        .build()
    );

    private final Setting<Boolean> renderLightPositions = sgLighting.add(new BoolSetting.Builder()
        .name("render-light-positions")
        .description("Render positions where light sources will be placed.")
        .defaultValue(true)
        .visible(autoLighting::get)
        .build()
    );

    private final Setting<ListMode> lightingMode = sgLighting.add(new EnumSetting.Builder<ListMode>()
        .name("lighting-mode")
        .description("Mode for selecting suitable blocks for light source placement.")
        .defaultValue(ListMode.Whitelist)
        .visible(autoLighting::get)
        .build()
    );

    private final Setting<List<Block>> lightingBlocks = sgLighting.add(new BlockListSetting.Builder()
        .name("lighting-blocks")
        .description("Blocks to place light sources on (whitelist) or avoid (blacklist).")
        .defaultValue(Blocks.STONE, Blocks.COBBLESTONE, Blocks.DIRT, Blocks.GRASS_BLOCK, Blocks.NETHERRACK, 
                     Blocks.END_STONE, Blocks.DEEPSLATE, Blocks.COBBLED_DEEPSLATE, Blocks.ANDESITE, 
                     Blocks.GRANITE, Blocks.DIORITE, Blocks.SANDSTONE, Blocks.RED_SANDSTONE)
        .visible(autoLighting::get)
        .build()
    );

    private final Setting<Boolean> groundProtection = sgProtection.add(new BoolSetting.Builder()
        .name("ground-protection")
        .description("Stop mining when player is not on ground (airborne).")
        .defaultValue(false)
        .build()
    );

    private final Setting<DirectionMode> directionMode = sgGeneral.add(new EnumSetting.Builder<DirectionMode>()
        .name("direction-mode")
        .description("Method to determine which face of the block to mine.")
        .defaultValue(DirectionMode.RayCast)
        .build()
    );

    private final Pool<MyBlock> blockPool = new Pool<>(MyBlock::new);
    private final List<MyBlock> blocks = new ArrayList<>();
    private final List<BlockPos> foundBlockPositions = new ArrayList<>();
    
    // Mining cache to prevent rebounding block issues
    private final LinkedHashSet<BlockPos> minedBlockCache = new LinkedHashSet<>();
    // 二级缓存
    private final LinkedHashSet<BlockPos> minedBlockCache2 = new LinkedHashSet<>();
    private int cacheCleanupTickTimer = 0;

    // Light source placement
    private final List<BlockPos> potentialLightPositions = new ArrayList<>();
    private final LinkedHashSet<BlockPos> placedLightSources = new LinkedHashSet<>();
    private int lightingTickTimer = 0;

    private int tick = 0;
    
    private final Random random = new Random();
    
    // Continuous mode variables
    private BlockPos lastPlayerPos = null;
    private int continuousScanTimer = 0;
    
    // Static random delay arrays to avoid creating new arrays each time
    private static final int[] DELAY_NONE = {0};
    private static final int[] DELAY_FAST = {0, 0, 1};
    private static final int[] DELAY_BALANCED = {0, 0, 0, 0, 1, 1, 1, 2, 2, 3};
    private static final int[] DELAY_SLOW = {0, 0, 1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 4, 5, 6};
    private static final int[] DELAY_VARIABLE = {0, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10};

    public Deleter() {
        super(Categories.World, "deleter", "Mines all nearby blocks with this type");
    }

    @Override
    public void onDeactivate() {
        for (MyBlock block : blocks) blockPool.free(block);
        blocks.clear();
        foundBlockPositions.clear();
        minedBlockCache.clear();
        minedBlockCache2.clear();
        cacheCleanupTickTimer = 0;
        lastPlayerPos = null;
        continuousScanTimer = 0;
        potentialLightPositions.clear();
        placedLightSources.clear();
        lightingTickTimer = 0;
    }

    private boolean isMiningBlock(BlockPos pos) {
        for (MyBlock block : blocks) {
            if (block.blockPos.equals(pos)) return true;
        }

        return false;
    }

    @EventHandler
    private void onStartBreakingBlock(StartBreakingBlockEvent event) {
        // Skip manual breaking in continuous mode
        if (continuousMode.get() && mode.get() == ListMode.Whitelist) {
            return;
        }

        BlockState state = mc.world.getBlockState(event.blockPos);

        if (state.getHardness(mc.world, event.blockPos) < 0)
            return;
        if (mode.get() == ListMode.Whitelist && !selectedBlocks.get().contains(state.getBlock()))
            return;
        if (mode.get() == ListMode.Blacklist && selectedBlocks.get().contains(state.getBlock()))
            return;

        // Check if this position was recently mined (cache check for rebounding blocks)
        if (isPositionCached(event.blockPos))
            return;

        // Check if this position is protected (adjacent to fluids or protected blocks)
        if (isProtectedPosition(event.blockPos))
            return;

        foundBlockPositions.clear();

        if (!isMiningBlock(event.blockPos)) {
            MyBlock block = blockPool.get();
            block.set(event);
            blocks.add(block);
            mineNearbyBlocks(block.originalBlock.asItem(),event.blockPos,event.direction,depth.get());
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        blocks.removeIf(MyBlock::shouldRemove);

        // Cache cleanup timer - clears cache periodically to prevent stale entries
        if (enableCache.get()) {
            cacheCleanupTickTimer++;
            if (cacheCleanupTickTimer >= cacheCleanupInterval.get() * 20) {
                
                // 一级缓存中不是空气且不是流体的砖放入二级缓存
                for (BlockPos pos : minedBlockCache) {
                    BlockState state = mc.world.getBlockState(pos);
                    if (!isBlockMinedSuccessfully(state)) {
                        minedBlockCache2.add(pos);
                    }
                }
                minedBlockCache.clear();
                // 清理二级缓存中已变为空气或流体的砖
                minedBlockCache2.removeIf(pos -> isBlockMinedSuccessfully(mc.world.getBlockState(pos)));
                cacheCleanupTickTimer = 0;
            }
        }

        // Continuous mode logic - scan for blocks around player
        if (continuousMode.get()) {
            handleContinuousMode();
        }

        // Handle automatic lighting
        handleAutoLighting();

        if (!blocks.isEmpty()) {
            // Add random delay to the base delay
            int[] randomDelays = getRandomDelayArray();
            int randomDelay = randomDelays[random.nextInt(randomDelays.length)];
            int totalDelay = delay.get() + randomDelay;
            
            if (tick < totalDelay && !blocks.getFirst().mining) {
                tick++;
                return;
            }
            tick = 0;
            
            // Mine up to maxBlocksPerTick blocks per tick
            int count = 0;
            for (MyBlock block : blocks) {
                if (count >= maxBlocksPerTick.get()) break;
                
                // Add successfully mined block to cache
                addToMinedCache(block.blockPos);
                block.mine();
                
                count++;
                
                // If block is not being insta-mined, only process one block per tick
                if (!BlockUtils.canInstaBreak(block.blockPos)) break;
            }
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (render.get()) {
            for (MyBlock block : blocks) block.render(event);
        }
        // 渲染回弹砖块
        if (renderReboundCache.get()) {
            for (BlockPos pos : minedBlockCache2) {
                renderReboundBlock(event, pos);
            }
        }
        // Render light source positions
        if (autoLighting.get() && renderLightPositions.get()) {
            for (BlockPos pos : potentialLightPositions) {
                renderLightPosition(event, pos);
            }
        }
    }

    private class MyBlock {
        public BlockPos blockPos;
        public Direction direction;
        public Block originalBlock;
        public boolean mining;
        public long miningStartTime;
        public boolean timedOut;

        public void set(StartBreakingBlockEvent event) {
            this.blockPos = event.blockPos;
            this.direction = event.direction;
            this.originalBlock = mc.world.getBlockState(blockPos).getBlock();
            this.mining = false;
            this.miningStartTime = 0;
            this.timedOut = false;
        }

        public void set(BlockPos pos, Direction dir) {
            this.blockPos = pos;
            this.direction = dir;
            this.originalBlock = mc.world.getBlockState(pos).getBlock();
            this.mining = false;
            this.miningStartTime = 0;
            this.timedOut = false;
        }

        public boolean shouldRemove() {
            BlockState currentState = mc.world.getBlockState(blockPos);
            
            // Check if block has been successfully mined (air or fluid)
            if (isBlockMinedSuccessfully(currentState)) {
                return true;
            }
            
            // Check if block changed to something else or out of range
            if (currentState.getBlock() != originalBlock || 
                Utils.distance(mc.player.getX() - 0.5, mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()), 
                             mc.player.getZ() - 0.5, blockPos.getX() + direction.getOffsetX(), 
                             blockPos.getY() + direction.getOffsetY(), blockPos.getZ() + direction.getOffsetZ()) 
                             > mc.player.getBlockInteractionRange()) {
                return true;
            }

            if (isProtectedPosition(blockPos)) {
                return true;
            }

            // Check for mining timeout
            if (timeoutProtection.get() && mining && miningStartTime > 0) {
                long currentTime = System.currentTimeMillis();
                double elapsedSeconds = (currentTime - miningStartTime) / 1000.0;
                
                if (elapsedSeconds > miningTimeout.get()) {
                    // Add to cache to prevent re-mining this problematic block
                    addToMinedCache(blockPos);
                    timedOut = true;
                    return true;
                }
            }

            return false;
        }

        public void mine() {
            if (!mining) {
                mc.player.swingHand(Hand.MAIN_HAND);
                mining = true;
                miningStartTime = System.currentTimeMillis(); // Record mining start time
            }
            if (rotate.get()) Rotations.rotate(Rotations.getYaw(blockPos), Rotations.getPitch(blockPos), 50, this::updateBlockBreakingProgress);
            else updateBlockBreakingProgress();
        }

        private void updateBlockBreakingProgress() {
            breakBlock(blockPos, swingHand.get());
        }

        public void render(Render3DEvent event) {
            VoxelShape shape = mc.world.getBlockState(blockPos).getOutlineShape(mc.world, blockPos);

            double x1 = blockPos.getX();
            double y1 = blockPos.getY();
            double z1 = blockPos.getZ();
            double x2 = blockPos.getX() + 1;
            double y2 = blockPos.getY() + 1;
            double z2 = blockPos.getZ() + 1;

            if (!shape.isEmpty()) {
                x1 = blockPos.getX() + shape.getMin(Direction.Axis.X);
                y1 = blockPos.getY() + shape.getMin(Direction.Axis.Y);
                z1 = blockPos.getZ() + shape.getMin(Direction.Axis.Z);
                x2 = blockPos.getX() + shape.getMax(Direction.Axis.X);
                y2 = blockPos.getY() + shape.getMax(Direction.Axis.Y);
                z2 = blockPos.getZ() + shape.getMax(Direction.Axis.Z);
            }

            // Use different colors for timed out blocks
            SettingColor sideColorToUse = timedOut ? new SettingColor(255, 165, 0, 10) : sideColor.get(); // Orange for timed out
            SettingColor lineColorToUse = timedOut ? new SettingColor(255, 165, 0, 255) : lineColor.get(); // Orange for timed out

            event.renderer.box(x1, y1, z1, x2, y2, z2, sideColorToUse, lineColorToUse, shapeMode.get(), 0);
        }
    }

    /**
     * Check if a position is in the mined block cache
     */
    private boolean isPositionCached(BlockPos pos) {
        return enableCache.get() && (minedBlockCache.contains(pos) || minedBlockCache2.contains(pos));
    }

    /**
     * Add position to mined cache and manage cache size
     */
    private void addToMinedCache(BlockPos pos) {
        if (!enableCache.get()) return;
        
        minedBlockCache.add(pos);
        
        // Remove oldest entries if cache exceeds limit
        while (minedBlockCache.size() > cacheSize.get()) {
            var iterator = minedBlockCache.iterator();
            if (iterator.hasNext()) {
                iterator.next();
                iterator.remove();
            }
        }
    }

    /**
     * Check if a block has been successfully mined (is air or fluid)
     */
    private boolean isBlockMinedSuccessfully(BlockState state) {
        return state.isAir() || !state.getFluidState().isEmpty();
    }

    /**
     * Render a rebound block with green outline
     */
    private void renderReboundBlock(Render3DEvent event, BlockPos pos) {
        VoxelShape shape = mc.world.getBlockState(pos).getOutlineShape(mc.world, pos);

        double x1 = pos.getX();
        double y1 = pos.getY();
        double z1 = pos.getZ();
        double x2 = pos.getX() + 1;
        double y2 = pos.getY() + 1;
        double z2 = pos.getZ() + 1;

        if (!shape.isEmpty()) {
            x1 = pos.getX() + shape.getMin(Direction.Axis.X);
            y1 = pos.getY() + shape.getMin(Direction.Axis.Y);
            z1 = pos.getZ() + shape.getMin(Direction.Axis.Z);
            x2 = pos.getX() + shape.getMax(Direction.Axis.X);
            y2 = pos.getY() + shape.getMax(Direction.Axis.Y);
            z2 = pos.getZ() + shape.getMax(Direction.Axis.Z);
        }

        // Green colors for rebound blocks
        SettingColor greenSide = new SettingColor(0, 255, 0, 10);
        SettingColor greenLine = new SettingColor(0, 255, 0, 255);

        event.renderer.box(x1, y1, z1, x2, y2, z2, greenSide, greenLine, shapeMode.get(), 0);
    }

    /**
     * Render a potential light source position with yellow outline
     */
    private void renderLightPosition(Render3DEvent event, BlockPos pos) {
        double x1 = pos.getX();
        double y1 = pos.getY();
        double z1 = pos.getZ();
        double x2 = pos.getX() + 1;
        double y2 = pos.getY() + 1;
        double z2 = pos.getZ() + 1;

        // Get light level for color intensity
        int lightLevel = getLightLevel(pos);
        int intensity = Math.max(50, 255 - (lightLevel * 15)); // Darker = more intense yellow

        // Yellow colors for light positions (darker areas get brighter yellow)
        SettingColor yellowSide = new SettingColor(255, 255, 0, 15);
        SettingColor yellowLine = new SettingColor(255, 255, 0, intensity);

        event.renderer.box(x1, y1, z1, x2, y2, z2, yellowSide, yellowLine, shapeMode.get(), 0);
    }

    /**
     * Check if a block position should be protected from mining
     * Returns true if the block is adjacent to fluids, protected blocks, outside distance range, outside height range, outside region, or outside directional range
     */
    private boolean isProtectedPosition(BlockPos pos) {
        // Check ground protection
         if (groundProtection.get() && !mc.player.isOnGround()) {
            return true;
        }

        // Check distance protection
        if (distanceProtection.get()) {
            double distance = getDistanceToPlayer(pos);
            if (distance < minDistance.get() || distance > maxDistance.get()) {
                return true;
            }
        }
        
        // Check height protection
        if (heightProtection.get()) {
            if (!isWithinHeightRange(pos)) {
                return true;
            }
        }
        
        // Check region protection
        if (regionProtection.get()) {
            if (!isWithinRegion(pos)) {
                return true;
            }
        }
        
        // Check directional protection
        if (directionalProtection.get()) {
            if (!isWithinDirectionalRange(pos)) {
                return true;
            }
        }
        
        // Check fluid protection for all neighbors
        if (fluidProtection.get()) {
            for (Vec3i neighbourOffset : upperNeighbours) {
                BlockPos neighbour = pos.add(neighbourOffset);
                BlockState neighbourState = mc.world.getBlockState(neighbour);
                
                if (!neighbourState.getFluidState().isEmpty()) {
                    return true;
                }
            }
            for (Vec3i neighbourOffset : sideNeighbours) {
                BlockPos neighbour = pos.add(neighbourOffset);
                BlockState neighbourState = mc.world.getBlockState(neighbour);
                
                if (!neighbourState.getFluidState().isEmpty()) {
                    return true;
                }
            }
            for (Vec3i neighbourOffset : lowerNeighbours) {
                BlockPos neighbour = pos.add(neighbourOffset);
                BlockState neighbourState = mc.world.getBlockState(neighbour);
                
                if (!neighbourState.getFluidState().isEmpty()) {
                    return true;
                }
            }
        }
        
        // Check custom block protection by direction
        if (customProtection.get()) {
            // Check upper protection
            for (Vec3i neighbourOffset : upperNeighbours) {
                BlockPos neighbour = pos.add(neighbourOffset);
                BlockState neighbourState = mc.world.getBlockState(neighbour);
                
                if (protectedBlocksUpper.get().contains(neighbourState.getBlock())) {
                    return true;
                }
            }
            
            // Check side protection
            for (Vec3i neighbourOffset : sideNeighbours) {
                BlockPos neighbour = pos.add(neighbourOffset);
                BlockState neighbourState = mc.world.getBlockState(neighbour);
                
                if (protectedBlocksSide.get().contains(neighbourState.getBlock())) {
                    return true;
                }
            }
            
            // Check lower protection
            for (Vec3i neighbourOffset : lowerNeighbours) {
                BlockPos neighbour = pos.add(neighbourOffset);
                BlockState neighbourState = mc.world.getBlockState(neighbour);
                
                if (protectedBlocksLower.get().contains(neighbourState.getBlock())) {
                    return true;
                }
            }
        }
        
        return false;
    }

    /**
     * Calculate the distance from the player to a block position
     */
    private double getDistanceToPlayer(BlockPos pos) {
        return Utils.distance(
            mc.player.getX() - 0.5, 
            mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()), 
            mc.player.getZ() - 0.5, 
            pos.getX() + 0.5, 
            pos.getY() + 0.5, 
            pos.getZ() + 0.5
        );
    }

    /**
     * Check if a block position is within the allowed height range
     */
    private boolean isWithinHeightRange(BlockPos pos) {
        int referenceY;
        
        if (heightReferenceMode.get() == HeightReferenceMode.Player) {
            // Use player's Y position as reference
            referenceY = mc.player.getBlockPos().getY();
        } else {
            // Use world coordinates (Y=0 as reference)
            referenceY = 0;
        }
        
        int blockY = pos.getY();
        int relativeHeight = blockY - referenceY;
        
        return relativeHeight >= minHeight.get() && relativeHeight <= maxHeight.get();
    }

    /**
     * Check if a block position is within the defined region (world coordinates)
     */
    private boolean isWithinRegion(BlockPos pos) {
        // Calculate min and max coordinates from the two corner points
        int minX = Math.min(region1X.get(), region2X.get());
        int maxX = Math.max(region1X.get(), region2X.get());
        int minY = Math.min(region1Y.get(), region2Y.get());
        int maxY = Math.max(region1Y.get(), region2Y.get());
        int minZ = Math.min(region1Z.get(), region2Z.get());
        int maxZ = Math.max(region1Z.get(), region2Z.get());
        
        // Check if the position is within the region bounds
        return pos.getX() >= minX && pos.getX() <= maxX &&
               pos.getY() >= minY && pos.getY() <= maxY &&
               pos.getZ() >= minZ && pos.getZ() <= maxZ;
    }

    /**
     * Get the light level at a specific position
     */
    private int getLightLevel(BlockPos pos) {
        if (mc.world == null) return 15;
        return mc.world.getLightLevel(LightType.BLOCK, pos);
    }

    /**
     * Check if a position is suitable for placing a light source
     */
    private boolean isSuitableForLightSource(BlockPos pos) {
        if (mc.world == null || mc.player == null) return false;
        
        // Check if position is air
        if (!mc.world.getBlockState(pos).isAir()) return false;
        
        // Check if we have a solid block below to place the light source on
        BlockPos belowPos = pos.down();
        BlockState belowState = mc.world.getBlockState(belowPos);
        
        // Must have a solid block below for torches, or allow placement on any solid block for sea lanterns
        if (belowState.isAir() || !Block.isShapeFullCube(belowState.getCollisionShape(mc.world, belowPos))) {
            return false;
        }
        
        // Use blacklist/whitelist to determine if the block below is suitable
        Block belowBlock = belowState.getBlock();
        if (lightingMode.get() == ListMode.Whitelist) {
            // Only place on blocks in the whitelist
            if (!lightingBlocks.get().contains(belowBlock)) return false;
        } else {
            // Don't place on blocks in the blacklist
            if (lightingBlocks.get().contains(belowBlock)) return false;
        }
        
        // Don't place where we already have a light source
        if (placedLightSources.contains(pos)) return false;
        
        // Check if the position is within range
        double distance = getDistanceToPlayer(pos);
        if (distance > lightingScanRadius.get()) return false;
        
        // Check if light level is below threshold
        int lightLevel = getLightLevel(pos);
        return lightLevel < lightLevelThreshold.get();
    }

    /**
     * Find the best light source block to place from inventory
     */
    private Block getBestLightSource() {
        if (mc.player == null) return null;
        
        for (Block lightSource : lightSources.get()) {
            FindItemResult result = InvUtils.findInHotbar(lightSource.asItem());
            if (result.found()) {
                return lightSource;
            }
        }
        return null;
    }

    /**
     * Scan for positions where light sources should be placed
     */
    private void scanForLightPositions() {
        if (!autoLighting.get() || mc.player == null || mc.world == null) return;
        
        potentialLightPositions.clear();
        BlockPos playerPos = mc.player.getBlockPos();
        int radius = lightingScanRadius.get();
        
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos checkPos = playerPos.add(x, y, z);
                    
                    if (isSuitableForLightSource(checkPos)) {
                        potentialLightPositions.add(checkPos.toImmutable());
                    }
                }
            }
        }
        
        // Sort by light level (darkest first) then by distance to player
        potentialLightPositions.sort((pos1, pos2) -> {
            int light1 = getLightLevel(pos1);
            int light2 = getLightLevel(pos2);
            
            if (light1 != light2) {
                return Integer.compare(light1, light2); // Darker positions first
            }
            
            // If same light level, prefer closer positions
            double dist1 = getDistanceToPlayer(pos1);
            double dist2 = getDistanceToPlayer(pos2);
            return Double.compare(dist1, dist2);
        });
    }

    /**
     * Place a light source at the specified position
     */
    private boolean placeLightSource(BlockPos pos, Block lightSource) {
        if (mc.player == null || mc.world == null) return false;
        
        // Find the light source in inventory
        FindItemResult result = InvUtils.findInHotbar(lightSource.asItem());
        if (!result.found()) return false;
        
        // Switch to the light source item
        InvUtils.swap(result.slot(), false);
        
        // Place the light source using MyUtils placement system
        BlockState targetState = lightSource.getDefaultState();
        boolean placed = MyUtils.precisePlaceByFace(pos, targetState, false, true, null);
        
        if (placed) {
            placedLightSources.add(pos);
            return true;
        }
        
        return false;
    }

    /**
     * Handle automatic light source placement
     */
    private void handleAutoLighting() {
        if (!autoLighting.get()) return;
        
        lightingTickTimer++;
        if (lightingTickTimer < lightingDelay.get()) return;
        
        lightingTickTimer = 0;
        
        // Scan for positions that need lighting
        scanForLightPositions();
        
        if (potentialLightPositions.isEmpty()) return;
        
        // Get the best light source available
        Block lightSource = getBestLightSource();
        if (lightSource == null) return;
        
        // Try to place a light source at the darkest position
        BlockPos targetPos = potentialLightPositions.get(0);
        if (placeLightSource(targetPos, lightSource)) {
            // Successfully placed light source
        }
    }

    /**
     * Check if a block position is within the directional range based on player's yaw
     */
    private boolean isWithinDirectionalRange(BlockPos pos) {
        // Get player position and yaw
        double playerX = mc.player.getX();
        double playerZ = mc.player.getZ();
        
        // Get player's yaw and normalize it to -180 to 180
        float playerYaw = mc.player.getYaw() % 360;
        if (playerYaw > 180) playerYaw -= 360;
        if (playerYaw < -180) playerYaw += 360;
        
        // Define the four corners of the block (on the horizontal plane)
        double[][] corners = {
            {pos.getX(), pos.getZ()},           // Bottom-left
            {pos.getX() + 1, pos.getZ()},      // Bottom-right  
            {pos.getX(), pos.getZ() + 1},      // Top-left
            {pos.getX() + 1, pos.getZ() + 1}   // Top-right
        };
        
        // Check if all corners are within the directional range
        for (double[] corner : corners) {
            // Calculate the direction vector from player to corner
            double deltaYMath = corner[0] - playerX;
            double deltaXMath = corner[1] - playerZ;
            
            // Skip if corner is at player position (avoid division by zero)
            if (deltaYMath == 0 && deltaXMath == 0) continue;
            
            // Calculate the angle to the corner in degrees (-180 to 180)
            double angleToCorner = -Math.toDegrees(Math.atan2(deltaYMath, deltaXMath));
            
            // Calculate the angle difference
            double angleDifference = Math.abs(angleToCorner - playerYaw);
            if (angleDifference > 180) {
                angleDifference = 360 - angleDifference;
            }
            
            // If any corner is outside the allowed range, reject the block
            if (angleDifference > directionalAngle.get()) {
                return false;
            }
        }
        
        // All corners are within range
        return true;
    }

    private void mineNearbyBlocks(Item item, BlockPos pos, Direction dir, int depth) {
        if (depth<=0) return;
        if (foundBlockPositions.contains(pos)) return;
        foundBlockPositions.add(pos);
        if (Utils.distance(mc.player.getX() - 0.5, mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()), mc.player.getZ() - 0.5, pos.getX(), pos.getY(), pos.getZ()) > mc.player.getBlockInteractionRange()) return;
        for(Vec3i neighbourOffset: blockNeighbours) {
            BlockPos neighbour = pos.add(neighbourOffset);
            // Check if this neighbor block was recently mined (cache check for rebounding blocks)
            if (isPositionCached(neighbour)) continue;
            
            // Check if this neighbor position is protected (adjacent to fluids or protected blocks)
            if (isProtectedPosition(neighbour)) continue;
            
            if (mc.world.getBlockState(neighbour).getBlock().asItem() == item) {
                MyBlock block = blockPool.get();
                block.set(neighbour,dir);
                blocks.add(block);
                mineNearbyBlocks(item, neighbour, dir, depth-1);
            }
        }
    }

    /**
     * Handle continuous mining mode - scan for blocks around player position
     */
    private void handleContinuousMode() {
        BlockPos currentPlayerPos = mc.player.getBlockPos();
        
        // Check if player moved or it's time for a periodic scan (every 10 ticks)
        boolean playerMoved = lastPlayerPos == null || !lastPlayerPos.equals(currentPlayerPos);
        continuousScanTimer++;
        boolean timeForScan = continuousScanTimer >= 10;
        
        if (playerMoved || timeForScan) {
            lastPlayerPos = currentPlayerPos.toImmutable();
            continuousScanTimer = 0;
            scanBlocks(currentPlayerPos);
        }
    }

    /**
     * Get the random delay array based on current mode setting
     */
    private int[] getRandomDelayArray() {
        switch (randomDelayMode.get()) {
            case None:
                return DELAY_NONE;
            case Fast:
                return DELAY_FAST;
            case Balanced:
                return DELAY_BALANCED;
            case Slow:
                return DELAY_SLOW;
            case Variable:
                return DELAY_VARIABLE;
            default:
                return DELAY_BALANCED; // Default to Balanced
        }
    }

    /**
     * Scan for whitelist blocks around the given position
     */
    private void scanBlocks(BlockPos centerPos) {
        int radius = scanRadius.get();
        
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos scanPos = centerPos.add(x, y, z);
                    
                    // Check if within interaction range
                    if (Utils.distance(mc.player.getX() - 0.5, mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()), 
                                     mc.player.getZ() - 0.5, scanPos.getX(), scanPos.getY(), scanPos.getZ()) 
                                     > mc.player.getBlockInteractionRange()) {
                        continue;
                    }
                    
                    BlockState state = mc.world.getBlockState(scanPos);
                    
                    // Check if it's a whitelist block
                    if (mode.get() == ListMode.Whitelist && !selectedBlocks.get().contains(state.getBlock())) {
                        continue;
                    }
                    if (mode.get() == ListMode.Blacklist && selectedBlocks.get().contains(state.getBlock())) {
                        continue;
                    }
                    
                    // Check if already being mined or in cache
                    if (isMiningBlock(scanPos) || isPositionCached(scanPos)) {
                        continue;
                    }
                    
                    // Check protection
                    if (isProtectedPosition(scanPos)) {
                        continue;
                    }
                    
                    // Add to mining queue
                    MyBlock block = blockPool.get();
                    block.set(scanPos, Direction.UP); // Default direction for continuous mode
                    blocks.add(block);
                }
            }
        }
    }

    public boolean breakBlock(BlockPos blockPos, boolean swing) {
        if (! BlockUtils.canBreak(blockPos, mc.world.getBlockState(blockPos))) return false;

        // Creating new instance of block pos because minecraft assigns the parameter to a field, and we don't want it to change when it has been stored in a field somewhere
        BlockPos pos = blockPos instanceof BlockPos.Mutable ? new BlockPos(blockPos) : blockPos;

        InstantRebreak ir = Modules.get().get(InstantRebreak.class);
        if (ir != null && ir.isActive() && ir.blockPos.equals(pos) && ir.shouldMine()) {
            ir.sendPacket();
            return true;
        }

        if (mc.interactionManager.isBreakingBlock())
            mc.interactionManager.updateBlockBreakingProgress(pos, getDirection(blockPos));
        else mc.interactionManager.attackBlock(pos,getDirection(blockPos));

        if (swing) mc.player.swingHand(Hand.MAIN_HAND);
        else mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));

        return true;
    }
    // Finds the best block direction using ray casting from player eye to block center
    public Direction getDirection(BlockPos pos) {
        if (directionMode.get() == DirectionMode.Original) {
            return BlockUtils.getDirection(pos);
        }
        // Get player eye position
        Vec3d eyePos = new Vec3d(
            mc.player.getX(), 
            mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()), 
            mc.player.getZ()
        );
        
        // Get block center position
        Vec3d blockCenter = new Vec3d(
            pos.getX() + 0.5, 
            pos.getY() + 0.5, 
            pos.getZ() + 0.5
        );
        
        // Calculate direction vector from eye to block center
        Vec3d direction = blockCenter.subtract(eyePos);
        
        // Get absolute values of direction components
        double absX = Math.abs(direction.x);
        double absY = Math.abs(direction.y);
        double absZ = Math.abs(direction.z);
        
        // Find which component is largest - this determines which face the ray hits
        if (absX >= absY && absX >= absZ) {
            // Ray hits either EAST or WEST face
            return direction.x > 0 ? Direction.WEST : Direction.EAST;
        } else if (absY >= absX && absY >= absZ) {
            // Ray hits either UP or DOWN face
            return direction.y > 0 ? Direction.DOWN : Direction.UP;
        } else {
            // Ray hits either SOUTH or NORTH face
            return direction.z > 0 ? Direction.NORTH : Direction.SOUTH;
        }
    }
    @Override
    public String getInfoString() {
        long timedOutBlocks = minedBlockCache.size(); // Approximate count of cached (including timed out) blocks

        StringBuilder info = new StringBuilder();
        
        if (continuousMode.get() && mode.get() == ListMode.Whitelist) {
            info.append("Continuous (").append(selectedBlocks.get().size()).append(") - Mining: ").append(blocks.size());
        } else {
            info.append(mode.get().toString()).append(" (").append(selectedBlocks.get().size()).append(")");
        }
        
        // Add auto lighting info
        if (autoLighting.get()) {
            info.append(" | Lights: ").append(potentialLightPositions.size()).append("/").append(placedLightSources.size());
        }
        
        // Add protection info
        StringBuilder protections = new StringBuilder();
        if (distanceProtection.get()) {
            protections.append("D");
        }
        if (heightProtection.get()) {
            protections.append("H");
        }
        if (regionProtection.get()) {
            protections.append("R");
        }
        if (directionalProtection.get()) {
            protections.append("Dir");
        }
        if (timeoutProtection.get()) {
            protections.append("T");
        }
        if (fluidProtection.get() || customProtection.get()) {
            protections.append("P");
        }
        if (groundProtection.get()) {
            protections.append("G");
        }
        if (autoLighting.get()) {
            protections.append("L");
        }
        
        if (protections.length() > 0) {
            info.append(" [").append(protections).append("]");
        }
        
        if (timeoutProtection.get() && timedOutBlocks > 0) {
            info.append(" | Cached: ").append(timedOutBlocks);
        }
        
        return info.toString();
    }

    public enum ListMode {
        Whitelist,
        Blacklist
    }

    public enum HeightReferenceMode {
        Player,
        World
    }

    public enum RandomDelayMode {
        None,    
        Fast,     
        Balanced, 
        Slow,      
        Variable   
    }

    public enum DirectionMode {
        Original,  // Use BlockUtils.getDirection method
        RayCast    // Use ray casting from player eye to block center
    }
}
