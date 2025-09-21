package com.kkllffaa.meteor_litematica_printer.CRUD;

import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;

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
import net.minecraft.item.Item;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import com.kkllffaa.meteor_litematica_printer.Addon;
import com.kkllffaa.meteor_litematica_printer.MyUtils;

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
    private final Set<Vec3i> faceNeighbours = Set.of(
            new Vec3i(0, 1, 0),

            new Vec3i(0, 0, 1),
            new Vec3i(1, 0, 0), new Vec3i(-1, 0, 0),
            new Vec3i(0, 0, -1),

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

    private final Setting<Boolean> whiteList = sgGeneral.add(new BoolSetting.Builder()
        .name("whiteList")
        .description("Whitelist for selected blocks.")
        .defaultValue(true)
        .build()
    );
    private final Setting<List<Block>> whiteListBlocks = sgGeneral.add(new BlockListSetting.Builder()
        .name("whiteListBlocks")
        .description("Which blocks to select.")
        .visible(whiteList::get)
        .build()
    );

    private final Setting<Boolean> blackList = sgGeneral.add(new BoolSetting.Builder()
        .name("blackList")
        .description("Blacklist for selected blocks.")
        .defaultValue(true)
        .build()
    );
    private final Setting<List<Block>> blackListBlocks = sgGeneral.add(new BlockListSetting.Builder()
        .name("blackListBlocks")
        .description("Which blocks to ignore.")
        .defaultValue(
            // Falling blocks
            Blocks.SAND, Blocks.GRAVEL, Blocks.RED_SAND,
            Blocks.ANVIL, Blocks.CHIPPED_ANVIL, Blocks.DAMAGED_ANVIL,
            Blocks.WHITE_CONCRETE_POWDER, Blocks.ORANGE_CONCRETE_POWDER, Blocks.MAGENTA_CONCRETE_POWDER,
            Blocks.LIGHT_BLUE_CONCRETE_POWDER, Blocks.YELLOW_CONCRETE_POWDER, Blocks.LIME_CONCRETE_POWDER,
            Blocks.PINK_CONCRETE_POWDER, Blocks.GRAY_CONCRETE_POWDER, Blocks.LIGHT_GRAY_CONCRETE_POWDER,
            Blocks.CYAN_CONCRETE_POWDER, Blocks.PURPLE_CONCRETE_POWDER, Blocks.BLUE_CONCRETE_POWDER,
            Blocks.BROWN_CONCRETE_POWDER, Blocks.GREEN_CONCRETE_POWDER, Blocks.RED_CONCRETE_POWDER,
            Blocks.BLACK_CONCRETE_POWDER,
            // Leaves
            Blocks.OAK_LEAVES, Blocks.SPRUCE_LEAVES, Blocks.BIRCH_LEAVES, Blocks.JUNGLE_LEAVES,
            Blocks.ACACIA_LEAVES, Blocks.DARK_OAK_LEAVES, Blocks.MANGROVE_LEAVES,
            Blocks.AZALEA_LEAVES, Blocks.FLOWERING_AZALEA_LEAVES
        )
        .visible(blackList::get)
        .build()
    );

    private final Setting<Boolean> continuousMode = sgGeneral.add(new BoolSetting.Builder()
        .name("continuous-mode")
        .description("Continuously mine blocks around player position.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> scanMode = sgGeneral.add(new BoolSetting.Builder()
        .name("scan-mode")
        .description("mesh mining")
        .defaultValue(false)
        .visible(continuousMode ::get)
        .build()
    );

    private final Setting<Double> normalDistance = sgGeneral.add(new DoubleSetting.Builder()
        .name("scan-distance")
        .description("Distance within which blocks are normally destroyed to avoid affecting player movement.")
        .defaultValue(1.0)
        .min(0.0)
        .max(10.0)
        .sliderRange(0.0, 10.0)
        .visible(scanMode::get)
        .build()
    );

    private final Setting<Integer> scanRadius = sgGeneral.add(new IntSetting.Builder()
        .name("scan-radius")
        .description("Radius to scan for blocks in continuous mode.")
        .defaultValue(10)
        .min(1)
        .max(10)
        .sliderRange(1, 10)
        .visible(continuousMode::get)
        .build()
    );

    private final Setting<Integer> depth = sgGeneral.add(new IntSetting.Builder()
        .name("depth")
        .description("Amount of iterations used to scan for similar blocks.")
        .defaultValue(15)
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
    private final Setting<MyUtils.RandomDelayMode> randomDelayMode = sgGeneral.add(new EnumSetting.Builder<MyUtils.RandomDelayMode>()
        .name("random-delay-mode")
        .description("Random delay distribution pattern.")
        .defaultValue(MyUtils.RandomDelayMode.Balanced)
        .build()
    );

    private final Setting<Integer> maxBlocksPerTick = sgGeneral.add(new IntSetting.Builder()
        .name("max-blocks-per-tick")
        .description("Maximum blocks to try to mine per tick. Useful when insta mining.")
        .defaultValue(11)
        .min(1)
        .max(1024)
        .build()
    );
    private final Setting<MyUtils.SafetyFaceMode> directionMode = sgGeneral.add(new EnumSetting.Builder<MyUtils.SafetyFaceMode>()
        .name("direction-mode")
        .description("Method to determine which face of the block to mine.")
        .defaultValue(MyUtils.SafetyFaceMode.PlayerPosition)
        .build()
    );

    private final Setting<Boolean> OnlyAttack = sgGeneral.add(new BoolSetting.Builder()
        .name("only-attack")
        .description("Only sends attack packets without updating block breaking progress.")
        .defaultValue(false)
        .build()
    );
    


    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Sends rotation packets to the server when mining.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> swingHand = sgRender.add(new BoolSetting.Builder()
        .name("swing-hand")
        .description("Swing hand client-side.")
        .defaultValue(false)
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
        .defaultValue(ShapeMode.Lines)
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
        .defaultValue(200)
        .min(10).sliderMin(10)
        .max(200).sliderMax(200)
        .visible(enableCache::get)
        .build()
    );

    private final Setting<Integer> cacheCleanupInterval = sgCache.add(new IntSetting.Builder()
        .name("cache-cleanup-interval")
        .description("Time in seconds between cache cleanups to prevent stale entries.")
        .defaultValue(1)
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
        .defaultValue(true)
        .build()
    );

    private final Setting<List<Block>> protectedBlocksUpper = sgProtection.add(new BlockListSetting.Builder()
        .name("protected-blocks-upper")
        .description("Blocks to avoid mining near (upper direction). Mining will be prevented if target block has any of these blocks above it.")
        .defaultValue(Blocks.LAVA, Blocks.WATER, 
        
            Blocks.SAND, Blocks.GRAVEL, Blocks.RED_SAND,
            Blocks.ANVIL, Blocks.CHIPPED_ANVIL, Blocks.DAMAGED_ANVIL,
            Blocks.WHITE_CONCRETE_POWDER, Blocks.ORANGE_CONCRETE_POWDER, Blocks.MAGENTA_CONCRETE_POWDER,
            Blocks.LIGHT_BLUE_CONCRETE_POWDER, Blocks.YELLOW_CONCRETE_POWDER, Blocks.LIME_CONCRETE_POWDER,
            Blocks.PINK_CONCRETE_POWDER, Blocks.GRAY_CONCRETE_POWDER, Blocks.LIGHT_GRAY_CONCRETE_POWDER,
            Blocks.CYAN_CONCRETE_POWDER, Blocks.PURPLE_CONCRETE_POWDER, Blocks.BLUE_CONCRETE_POWDER,
            Blocks.BROWN_CONCRETE_POWDER, Blocks.GREEN_CONCRETE_POWDER, Blocks.RED_CONCRETE_POWDER,
            Blocks.BLACK_CONCRETE_POWDER
        
        
        )
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
        .defaultValue(2.0)
        .min(0.5)
        .max(30.0)
        .sliderRange(0.5, 15.0)
        .visible(timeoutProtection::get)
        .build()
    );

    private final Setting<DistanceMode> distanceProtection = sgProtection.add(new EnumSetting.Builder<DistanceMode>()
        .name("distance-protection")
        .description("Prevent mining blocks that are too far or too close to the player.")
        .defaultValue(DistanceMode.Auto)
        .build()
    );

    private final Setting<Double> maxDistance = sgProtection.add(new DoubleSetting.Builder()
        .name("max-distance")
        .description("Maximum distance from player to mine blocks.")
        .defaultValue(4.5)
        .min(1.0)
        .max(1024.0)
        .sliderRange(1.0, 8.0)
        .visible(() -> distanceProtection.get() == DistanceMode.Max)
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

    
    private final Setting<Integer> standHeight = sgProtection.add(new IntSetting.Builder()
        .name("stand-height")
        .description("No digging height, you need to stand on top.")
        .defaultValue(-1)
        .min(-64)
        .max(320)
        .sliderRange(-20, 20)
        .visible(heightProtection::get)
        .build()
    );

    private final Setting<Integer> minHeight = sgProtection.add(new IntSetting.Builder()
        .name("min-height")
        .description("Minimum height for mining blocks (relative to reference).")
        .defaultValue(0)
        .min(-64)
        .max(320)
        .sliderRange(-20, 20)
        .visible(heightProtection::get)
        .build()
    );

    private final Setting<Integer> maxHeight = sgProtection.add(new IntSetting.Builder()
        .name("max-height")
        .description("Maximum height for mining blocks (relative to reference).")
        .defaultValue(1)
        .min(-64)
        .max(320)
        .sliderRange(-20, 20)
        .visible(heightProtection::get)
        .build()
    );

    private final Setting<Boolean> widthProtection = sgProtection.add(new BoolSetting.Builder()
        .name("width-protection")
        .description("Limit mining within a width tunnel relative to player's facing direction (player reference frame).")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> widthLeft = sgProtection.add(new IntSetting.Builder()
        .name("width-left")
        .description("Number of blocks to the left of player's facing direction (negative values = right side).")
        .defaultValue(0)
        .min(-10)
        .max(10)
        .sliderRange(-5, 5)
        .visible(widthProtection::get)
        .build()
    );

    private final Setting<Integer> widthRight = sgProtection.add(new IntSetting.Builder()
        .name("width-right")
        .description("Number of blocks to the right of player's facing direction (negative values = left side).")
        .defaultValue(0)
        .min(-10)
        .max(10)
        .sliderRange(-5, 5)
        .visible(widthProtection::get)
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

    private final Setting<Boolean> groundProtection = sgProtection.add(new BoolSetting.Builder()
        .name("ground-protection")
        .description("Stop mining when player is not on ground (airborne).")
        .defaultValue(false)
        .build()
    );



    // Auto Lighting Settings
    private final Setting<Boolean> autoLighting = sgLighting.add(new BoolSetting.Builder()
        .name("auto-lighting")
        .description("Automatically place light sources on dark blocks to prevent mob spawning.")
        .defaultValue(true)
        .build()
    );

    private final Setting<List<Block>> lightSources = sgLighting.add(new BlockListSetting.Builder()
        .name("light-sources")
        .description("List of blocks to use as light sources.")
        .defaultValue(Blocks.TORCH)
        .visible(autoLighting::get)
        .build()
    );

    private final Setting<Integer> lightLevelThreshold = sgLighting.add(new IntSetting.Builder()
        .name("light-level-threshold")
        .description("Place light sources on blocks with light level below this value.")
        .defaultValue(3)
        .min(0)
        .max(15)
        .sliderRange(0, 15)
        .visible(autoLighting::get)
        .build()
    );

    private final Setting<Integer> lightingScanRadius = sgLighting.add(new IntSetting.Builder()
        .name("lighting-scan-radius")
        .description("Radius to scan for dark blocks around player position.")
        .defaultValue(6)
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

    private final Setting<Boolean> earlyTermination = sgLighting.add(new BoolSetting.Builder()
        .name("early-termination")
        .description("Stop searching when a completely dark position (light level 0) is found close to the player.")
        .defaultValue(true)
        .visible(autoLighting::get)
        .build()
    );

    private final Setting<Double> earlyTerminationDistance = sgLighting.add(new DoubleSetting.Builder()
        .name("early-termination-distance")
        .description("Maximum distance for early termination when a dark position is found.")
        .defaultValue(3.0)
        .min(1.0)
        .max(8.0)
        .sliderRange(1.0, 8.0)
        .visible(() -> autoLighting.get() && earlyTermination.get())
        .build()
    );

    private final Setting<ListMode> lightingMode = sgLighting.add(new EnumSetting.Builder<ListMode>()
        .name("lighting-mode")
        .description("Mode for selecting suitable blocks for light source placement.")
        .defaultValue(ListMode.Blacklist)
        .visible(autoLighting::get)
        .build()
    );

    private final Setting<List<Block>> lightingBlocks = sgLighting.add(new BlockListSetting.Builder()
        .name("lighting-blocks")
        .description("Blocks to place light sources on (whitelist) or avoid (blacklist).")
        .defaultValue()
        .visible(autoLighting::get)
        .build()
    );

    private final Pool<MyBlock> blockPool = new Pool<>(MyBlock::new);
    private final List<MyBlock> blocks = new ArrayList<>();
    private final List<BlockPos> foundBlockPositions = new ArrayList<>();
    
    // Mining cache to prevent rebounding block issues
    private final LinkedHashSet<BlockPos> minedBlockCache = new LinkedHashSet<>();
    private final LinkedHashSet<BlockPos> minedBlockCache2 = new LinkedHashSet<>();
    private int cacheCleanupTickTimer = 0;

    // Static color constants for rebound cache rendering
    private static final SettingColor REBOUND_CACHE_SIDE_COLOR = new SettingColor(0, 255, 0, 10);
    private static final SettingColor REBOUND_CACHE_LINE_COLOR = new SettingColor(0, 255, 0, 255);

    // Light source placement - optimized to only store the single best position
    // This reduces memory usage and improves performance compared to storing all potential positions
    private BlockPos bestLightPosition = null;
    private int lightingTickTimer = 0;

    private int tick = 0;
    
    private final Random random = new Random();
    
    // Continuous mode variables
    private BlockPos lastPlayerPos = null;
    private int continuousScanTimer = 0;
    


    public Deleter() {
        super(Addon.CRUDCATEGORY, "deleter", "Mines all nearby blocks with this type");
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
        bestLightPosition = null;
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
        if (continuousMode.get()) {
            return;
        }

        BlockState state = mc.world.getBlockState(event.blockPos);
        if (isAirOrFluid(state)||
            isPositionCached(event.blockPos)||
            !isListBlock(state)||
            isProtectedPosition(event.blockPos)
        ) {
            return;
        }
        
        foundBlockPositions.clear();

        if (!isMiningBlock(event.blockPos)) {
            MyBlock block = blockPool.get();
            block.set(event);
            blocks.add(block);
            mineNearbyBlocks(block.originalBlock.asItem(),event.blockPos,event.direction,depth.get());
        }
    }

    private void mineNearbyBlocks(Item item, BlockPos pos, Direction dir, int depth) {
        if (depth<=0) return;
        if (foundBlockPositions.contains(pos)) return;
        foundBlockPositions.add(pos);
        if (Utils.distance(mc.player.getX() - 0.5, mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()), mc.player.getZ() - 0.5, pos.getX(), pos.getY(), pos.getZ()) > mc.player.getBlockInteractionRange()) return;
        for(Vec3i neighbourOffset: blockNeighbours) {
            BlockPos neighbour = pos.add(neighbourOffset);
            BlockState neighbourState = mc.world.getBlockState(neighbour);
            if (isAirOrFluid(neighbourState)||
                    isPositionCached(neighbour)||
                    !isListBlock(neighbourState)||
                    isProtectedPosition(neighbour)
                ) {
                    continue;
            }

            if (neighbourState.getBlock().asItem() == item) {
                MyBlock block = blockPool.get();
                block.set(neighbour,dir);
                blocks.add(block);
                mineNearbyBlocks(item, neighbour, dir, depth-1);
            }
        }
    }
    
    @EventHandler
    private void onTick(TickEvent.Pre event) {
        blocks.removeIf(MyBlock::shouldRemove);
        // Cache cleanup timer - clears cache periodically to prevent stale entries
        if (enableCache.get()) {
            cacheCleanupTickTimer++;
            if (cacheCleanupTickTimer >= cacheCleanupInterval.get() * 20) {
                
                // 清理一级缓存，固体转移到二级缓存
                for (BlockPos pos : minedBlockCache) {
                    BlockState state = mc.world.getBlockState(pos);
                    if (!isAirOrFluid(state)) {
                        minedBlockCache2.add(pos);
                    }
                }
                minedBlockCache.clear();
                // 清理二级缓存中非固体
                minedBlockCache2.removeIf(pos -> isAirOrFluid(mc.world.getBlockState(pos)));
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
            int[] randomDelays = MyUtils.getRandomDelayArray(randomDelayMode.get());
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
                block.mine();
                if (OnlyAttack.get()){
                    addToMinedCache(block.blockPos);
                }
                
                count++;
                
                if (BlockUtils.canInstaBreak(block.blockPos)){
                } 
                else{
                    // If block is not being insta-mined, only process one block per tick
                    break;
                }
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
                MyUtils.renderPos(event, pos, shapeMode.get(), REBOUND_CACHE_SIDE_COLOR, REBOUND_CACHE_LINE_COLOR);
            }
        }
        // Render the best light source position
        if (autoLighting.get() && renderLightPositions.get() && bestLightPosition != null) {
            int lightLevel = MyUtils.getLightLevel(bestLightPosition);
            int intensity = Math.max(50, 255 - (lightLevel * 15)); // Darker = more intense yellow

            SettingColor yellowLine = new SettingColor(255, 255, 0, intensity);
            MyUtils.renderPos(event, bestLightPosition, ShapeMode.Lines, yellowLine, yellowLine);
        }
    }

    private class MyBlock {
        public BlockPos blockPos;
        public Block originalBlock;
        public boolean mining;
        public long miningStartTime;
        public boolean timedOut;

        public void set(StartBreakingBlockEvent event) {
            this.blockPos = event.blockPos;
            this.originalBlock = mc.world.getBlockState(blockPos).getBlock();
            this.mining = false;
            this.miningStartTime = 0;
            this.timedOut = false;
        }

        public void set(BlockPos pos, Direction dir) {
            this.blockPos = pos;
            this.originalBlock = mc.world.getBlockState(pos).getBlock();
            this.mining = false;
            this.miningStartTime = 0;
            this.timedOut = false;
        }

        public boolean shouldRemove() {
            if (isPositionCached(lastPlayerPos)) {
                return true;
            }
            BlockState currentState = mc.world.getBlockState(blockPos);
            // Check if block changed to something else or out of range
            if (currentState.getBlock() != originalBlock){
                addToMinedCache(blockPos);
                return true;
            }

            if( isOutOfDistance(blockPos)||
                !isListBlock(currentState)||
                isProtectedPosition(blockPos)
            ){
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
            if (rotate.get()) Rotations.rotate(Rotations.getYaw(blockPos), Rotations.getPitch(blockPos), 50, this::updateBlockBreakingProgress);
            else updateBlockBreakingProgress();
            if (!mining) {
                mining = true;
                miningStartTime = System.currentTimeMillis(); // Record mining start time
            }
        }

        private void updateBlockBreakingProgress() {
            if(OnlyAttack.get()){
                MyUtils.breakBlockONLYAttack(blockPos, swingHand.get(), directionMode.get()); 
            }else{
                MyUtils.breakBlock(blockPos, swingHand.get(), directionMode.get(),mining);
            }
        }

        public void render(Render3DEvent event) {
            // Use different colors for timed out blocks
            SettingColor sideColorToUse = timedOut ? new SettingColor(255, 165, 0, 10) : sideColor.get(); // Orange for timed out
            SettingColor lineColorToUse = timedOut ? new SettingColor(255, 165, 0, 255) : lineColor.get(); // Orange for timed out
            MyUtils.renderPos(event, blockPos, shapeMode.get(), sideColorToUse, lineColorToUse);
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

    
    private boolean isOutOfDistance(BlockPos Pos) {
        double handDistance =  distanceProtection.get() == DistanceMode.Auto ? mc.player.getBlockInteractionRange() : maxDistance.get();
        return MyUtils.getDistanceToPlayerEyes(Pos) > handDistance;
    }
    private void scanBlocks(BlockPos centerPos) {
        int radius = scanRadius.get();
        
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos scanPos = centerPos.add(x, y, z);
                    BlockState state = mc.world.getBlockState(scanPos);

                    if (
                        isOutOfDistance(scanPos) ||
                        isPositionCached(scanPos) ||
                        isAirOrFluid(state) ||
                        !isListBlock(state) ||
                        isMiningBlock(scanPos) || 
                        isProtectedPosition(scanPos)
                    ) {
                        continue;
                    }
                   
                    if (scanMode.get()&& 
                    
                    !(Utils.distance(
                        mc.player.getX(), 0, mc.player.getZ(),
                         scanPos.getX()+0.5, 0, scanPos.getZ()+0.5) < normalDistance.get() && (

                         mc.player.getY()-0.1<scanPos.getY()&&scanPos.getY()<mc.player.getY()+1.1
                         )
                         
                         )
                    
                    
                    ) {
                        boolean hasNeighbourInBlocks = false;
                        for (Vec3i offset : faceNeighbours) {
                            BlockPos neighbour = scanPos.add(offset);
                            if (mc.world.getBlockState(neighbour).isAir() || isMiningBlock(neighbour)) {
                                hasNeighbourInBlocks = true;
                                break;
                            }
                        }
                        if (!hasNeighbourInBlocks) {
                            MyBlock block = blockPool.get();
                            block.set(scanPos, Direction.UP); // Default direction for continuous mode
                            blocks.add(block);
                        }
                    } else {
                        MyBlock block = blockPool.get();
                        block.set(scanPos, Direction.UP); // Default direction for continuous mode
                        blocks.add(block);
                    }
                }
            }
        }
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
     * Check if a position is in the mined block cache
     */
    private boolean isPositionCached(BlockPos pos) {
        return enableCache.get() && (minedBlockCache.contains(pos) || minedBlockCache2.contains(pos));
    }
    /**
     * Check if a block has been successfully mined (is air or fluid)
     */
    private boolean isAirOrFluid(BlockState state) {
        return state.isAir() || !state.getFluidState().isEmpty();
    }
    private boolean isListBlock(BlockState state) {
        // Check list block
        if (whiteList.get() && ! whiteListBlocks.get().contains(state.getBlock())) {
            return false;
        }
        if (blackList.get() && blackListBlocks.get().contains(state.getBlock())) {
            return false;
        }
        return true;
    }
    private boolean isProtectedPosition(BlockPos pos) {
        if (isGroundProtection()) {
            return true;
        }
        
        // Check width protection
        if (widthProtection.get()) {
            if (!isWithinWidthRange(pos)) {
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

    
    private boolean isGroundProtection(){
        if (groundProtection.get() && !mc.player.isOnGround()) {
            return true;
        }
        return false;
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
        
        return relativeHeight >= minHeight.get() && relativeHeight <= maxHeight.get() && relativeHeight != standHeight.get();
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

    /**
     * Check if a block position is within the width tunnel relative to player's reference frame
     */
    private boolean isWithinWidthRange(BlockPos pos) {
        if (mc.player == null) return false;
        
        // Get player position
        double playerX = mc.player.getX();
        double playerZ = mc.player.getZ();
        
        // Get player's yaw and normalize it to -180 to 180
        float playerYaw = mc.player.getYaw() % 360;
        if (playerYaw > 180) playerYaw -= 360;
        if (playerYaw < -180) playerYaw += 360;
        
        // Classify player's yaw into four main directions (East, South, West, North)
        Direction primaryDirection = classifyYawToDirection(playerYaw);
        
        // Transform block position to player's reference frame
        int[] playerRefCoords = transformToPlayerReference(pos, playerX, playerZ, primaryDirection);
        int leftRightPos = playerRefCoords[0]; // Left-right position in player's reference frame
        
        // Check if the block is within the specified width range
        // widthLeft is the leftmost boundary (negative = right side)
        // widthRight is the rightmost boundary (negative = left side)
        int minWidth = Math.min(widthLeft.get(), widthRight.get());
        int maxWidth = Math.max(widthLeft.get(), widthRight.get());
        
        return leftRightPos >= minWidth && leftRightPos <= maxWidth;
    }

    /**
     * Classify player's yaw angle into one of four cardinal directions
     */
    private Direction classifyYawToDirection(float yaw) {
        // Normalize yaw to 0-360 range
        yaw = yaw % 360;
        if (yaw < 0) yaw += 360;
        
        // Classify into four directions with 90-degree ranges
        if (yaw >= 315 || yaw < 45) {
            return Direction.SOUTH; // 0° (facing negative Z)
        } else if (yaw >= 45 && yaw < 135) {
            return Direction.WEST; // 90° (facing negative X) 
        } else if (yaw >= 135 && yaw < 225) {
            return Direction.NORTH; // 180° (facing positive Z)
        } else {
            return Direction.EAST; // 270° (facing positive X)
        }
    }

    /**
     * Transform world coordinates to player's reference frame
     * Returns [leftRight, forwardBack] where:
     * - leftRight: negative = left of player, positive = right of player  
     * - forwardBack: negative = behind player, positive = in front of player
     */
    private int[] transformToPlayerReference(BlockPos blockPos, double playerX, double playerZ, Direction playerFacing) {
        // Calculate relative position from player to block
        int relativeX = blockPos.getX() - (int)Math.floor(playerX);
        int relativeZ = blockPos.getZ() - (int)Math.floor(playerZ);
        
        int leftRight, forwardBack;
        
        // Transform coordinates based on player's primary facing direction
        switch (playerFacing) {
            case SOUTH: // Player facing negative Z (yaw ≈ 0°)
                leftRight = -relativeX;   // Left = negative X direction
                forwardBack = -relativeZ; // Forward = negative Z direction
                break;
                
            case WEST: // Player facing negative X (yaw ≈ 90°)  
                leftRight = -relativeZ;   // Left = negative Z direction
                forwardBack = relativeX;  // Forward = positive X direction
                break;
                
            case NORTH: // Player facing positive Z (yaw ≈ 180°)
                leftRight = relativeX;    // Left = positive X direction  
                forwardBack = relativeZ;  // Forward = positive Z direction
                break;
                
            case EAST: // Player facing positive X (yaw ≈ 270°)
                leftRight = relativeZ;    // Left = positive Z direction
                forwardBack = -relativeX; // Forward = negative X direction
                break;
                
            default:
                leftRight = 0;
                forwardBack = 0;
                break;
        }
        
        return new int[]{leftRight, forwardBack};
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
        
        // Check if light level is below threshold
        int lightLevel = MyUtils.getLightLevel(pos);
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
     * Scan for the best position where a light source should be placed
     * Optimized to only keep track of the single best position during scanning
     */
    private void scanForLightPositions() {
        if (!autoLighting.get() || mc.player == null || mc.world == null) return;
        
        bestLightPosition = null;
        BlockPos playerPos = mc.player.getBlockPos();
        int radius = lightingScanRadius.get();
        
        // Variables to track the best position found so far
        int bestLightLevel = Integer.MAX_VALUE;
        double bestDistance = Double.MAX_VALUE;
        
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos checkPos = playerPos.add(x, y, z);
                    
                    if (isSuitableForLightSource(checkPos)) {
                        int lightLevel = MyUtils.getLightLevel(checkPos);
                        double distance = MyUtils.getDistanceToPlayerEyes(checkPos);
                        
                        // Check if this position is better than the current best
                        boolean isBetter = false;
                        
                        if (lightLevel < bestLightLevel) {
                            // Darker position is always better
                            isBetter = true;
                        } else if (lightLevel == bestLightLevel && distance < bestDistance) {
                            // Same light level but closer distance
                            isBetter = true;
                        }
                        
                        if (isBetter) {
                            bestLightPosition = checkPos.toImmutable();
                            bestLightLevel = lightLevel;
                            bestDistance = distance;
                            
                            // Early termination: if enabled and we found a completely dark position (light level 0)
                            // and it's within the specified distance, we can stop searching
                            if (earlyTermination.get() && lightLevel == 0 && distance <= earlyTerminationDistance.get()) {
                                return;
                            }
                        }
                    }
                }
            }
        }
    }


    /**
     * Handle automatic light source placement
     */
    private void handleAutoLighting() {
        if (!autoLighting.get()) return;
        
        lightingTickTimer++;
        if (lightingTickTimer < lightingDelay.get()) return;
        
        lightingTickTimer = 0;
        
        // Scan for the best position that needs lighting
        scanForLightPositions();
        
        if (bestLightPosition == null) return;
        
        // Get the best light source available
        Block lightSource = getBestLightSource();
        if (lightSource == null)
            return;
        BlockState liteState = lightSource.getDefaultState();
        if (MyUtils.switchItem(lightSource.asItem(), liteState, false,
                () -> MyUtils.placeBlock(liteState, bestLightPosition))) {
            bestLightPosition = null;
        }

    }


    @Override
    public String getInfoString() {
        return " (" + blackListBlocks.get().size() + whiteListBlocks.get().size() + ")";
    }

    public enum ListMode {
        Whitelist,
        Blacklist
    }
    public enum DistanceMode {
        Auto,
        Max,
    }
    public enum HeightReferenceMode {
        Player,
        World
    }


}
