package com.kkllffaa.meteor_litematica_printer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.ClientPlayerEntity;

import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class Deleter extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgWhitelist = settings.createGroup("Whitelist");
    private final SettingGroup sgSafety = settings.createGroup("Safety");
    private final SettingGroup sgRendering = settings.createGroup("Rendering");

    // General settings
    private final Setting<Integer> miningRange = sgGeneral.add(new IntSetting.Builder()
            .name("mining-range")
            .description("Maximum range for mining adjacent blocks.")
            .defaultValue(4)
            .min(1).sliderMin(1)
            .max(8).sliderMax(8)
            .build()
    );

    private final Setting<Integer> miningDelay = sgGeneral.add(new IntSetting.Builder()
            .name("mining-delay")
            .description("Delay between mining blocks in ticks.")
            .defaultValue(1)
            .min(0).sliderMin(0)
            .max(20).sliderMax(20)
            .build()
    );

    private final Setting<Integer> maxBlocksPerTick = sgGeneral.add(new IntSetting.Builder()
            .name("max-blocks-per-tick")
            .description("Maximum blocks to mine per tick.")
            .defaultValue(2)
            .min(1).sliderMin(1)
            .max(10).sliderMax(10)
            .build()
    );

    private final Setting<Boolean> swingHand = sgGeneral.add(new BoolSetting.Builder()
            .name("swing-hand")
            .description("Swing hand when mining blocks.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> autoTool = sgGeneral.add(new BoolSetting.Builder()
            .name("auto-tool")
            .description("Automatically switch to the best tool for mining.")
            .defaultValue(true)
            .build()
    );

    // Whitelist settings
    private final Setting<Boolean> whitelistEnabled = sgWhitelist.add(new BoolSetting.Builder()
            .name("whitelist-enabled")
            .description("Only mine blocks in the whitelist.")
            .defaultValue(false)
            .build()
    );

    private final Setting<List<Block>> whitelist = sgWhitelist.add(new BlockListSetting.Builder()
            .name("whitelist")
            .description("Blocks that can be mined automatically.")
            .visible(whitelistEnabled::get)
            .build()
    );

    // Safety settings
    private final Setting<List<Block>> blacklist = sgSafety.add(new BlockListSetting.Builder()
            .name("blacklist")
            .description("Blocks that should never be mined automatically.")
            .defaultValue(Arrays.asList(
                    Blocks.BEDROCK, Blocks.BARRIER, Blocks.COMMAND_BLOCK, Blocks.CHAIN_COMMAND_BLOCK,
                    Blocks.REPEATING_COMMAND_BLOCK, Blocks.STRUCTURE_BLOCK, Blocks.JIGSAW,
                    Blocks.SPAWNER, Blocks.END_PORTAL, Blocks.END_PORTAL_FRAME,
                    Blocks.NETHER_PORTAL, Blocks.OBSIDIAN, Blocks.CRYING_OBSIDIAN
            ))
            .build()
    );

    private final Setting<Integer> maxChainLength = sgSafety.add(new IntSetting.Builder()
            .name("max-chain-length")
            .description("Maximum number of blocks to mine in a single chain.")
            .defaultValue(64)
            .min(1).sliderMin(1)
            .max(512).sliderMax(512)
            .build()
    );

    private final Setting<Boolean> requireValidTool = sgSafety.add(new BoolSetting.Builder()
            .name("require-valid-tool")
            .description("Only mine blocks if holding a valid tool.")
            .defaultValue(true)
            .build()
    );

    // Rendering settings
    private final Setting<Boolean> renderBlocks = sgRendering.add(new BoolSetting.Builder()
            .name("render-mined-blocks")
            .description("Render blocks being mined.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Integer> fadeTime = sgRendering.add(new IntSetting.Builder()
            .name("fade-time")
            .description("Time for the rendering to fade, in ticks.")
            .defaultValue(20)
            .min(1).sliderMin(1)
            .max(100).sliderMax(100)
            .visible(renderBlocks::get)
            .build()
    );

    private final Setting<SettingColor> miningColor = sgRendering.add(new ColorSetting.Builder()
            .name("mining-color")
            .description("Color for blocks being mined.")
            .defaultValue(new SettingColor(255, 0, 0, 150))
            .visible(renderBlocks::get)
            .build()
    );

    // Internal state
    private int timer = 0;
    private final Queue<BlockPos> miningQueue = new LinkedList<>();
    private final Set<BlockPos> processedBlocks = ConcurrentHashMap.newKeySet();
    private final List<Pair<Integer, BlockPos>> minedBlocks = new ArrayList<>();
    private Block lastMinedBlock = null;

    // Directions for adjacent block checking
    private static final Direction[] DIRECTIONS = {
            Direction.NORTH, Direction.SOUTH, Direction.EAST,
            Direction.WEST, Direction.UP, Direction.DOWN
    };

    public Deleter() {
        super(Addon.CATEGORY, "deleter", "Automatically mines adjacent same blocks when you break one");
    }

    @Override
    public void onActivate() {
        miningQueue.clear();
        processedBlocks.clear();
        minedBlocks.clear();
        blockStateCache.clear();
        recentlyMinedPositions.clear();
        lastMinedBlock = null;
        timer = 0;
    }

    @Override
    public void onDeactivate() {
        miningQueue.clear();
        processedBlocks.clear();
        minedBlocks.clear();
        blockStateCache.clear();
        recentlyMinedPositions.clear();
        lastMinedBlock = null;
    }

    // Track blocks before they change to detect what was broken
    private final Map<BlockPos, BlockState> blockStateCache = new ConcurrentHashMap<>();
    private final Set<BlockPos> recentlyMinedPositions = ConcurrentHashMap.newKeySet();

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (mc.player == null || mc.world == null) return;
        
        // Listen for block update packets which indicate block changes
        if (event.packet instanceof BlockUpdateS2CPacket packet) {
            BlockPos pos = packet.getPos();
            BlockState newState = packet.getState();
            
            // Get the previous state from our cache
            BlockState oldState = blockStateCache.getOrDefault(pos, mc.world.getBlockState(pos));
            
            // Detect if a solid block was broken/destroyed (not just replaced)
            if (wasBlockDestroyed(oldState, newState)) {
                Block destroyedBlock = oldState.getBlock();
                
                // Only trigger if:
                // 1. The destroyed block should be chain-mined
                // 2. Player is close enough
                // 3. This position wasn't recently mined by us (avoid feedback loops)
                if (shouldMineBlock(destroyedBlock) && 
                    mc.player.getBlockPos().isWithinDistance(pos, miningRange.get() + 2) &&
                    !recentlyMinedPositions.contains(pos)) {
                    
                    startChainMining(pos, destroyedBlock);
                }
            }
            
            // Update our cache with the new state
            if (newState.isAir()) {
                blockStateCache.remove(pos);
            } else {
                blockStateCache.put(pos, newState);
            }
        }
    }

    /**
     * Determines if a block was actually destroyed rather than just replaced
     * This handles various scenarios like water/lava flow, explosions, player mining, etc.
     */
    private boolean wasBlockDestroyed(BlockState oldState, BlockState newState) {
        // Block was solid and is now air/fluid - likely destroyed
        if (!oldState.isAir() && oldState.getBlock() != Blocks.WATER && oldState.getBlock() != Blocks.LAVA &&
            (newState.isAir() || newState.getBlock() == Blocks.WATER || newState.getBlock() == Blocks.LAVA)) {
            return true;
        }
        
        // Block changed to a different solid block - could be replacement, not destruction
        return false;
    }

    private void startChainMining(BlockPos pos, Block block) {
        // Clear previous mining session
        miningQueue.clear();
        processedBlocks.clear();
        
        // Start new mining session
        lastMinedBlock = block;
        
        // Add adjacent blocks to queue
        addAdjacentBlocks(pos, block, 0);
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.world == null) return;

        // Update rendering fade
        minedBlocks.removeIf(pair -> {
            pair.setLeft(pair.getLeft() - 1);
            return pair.getLeft() <= 0;
        });

        // Clean up cache periodically to prevent memory leaks
        if (timer % 100 == 0) { // Every 5 seconds
            cleanupCaches();
        }

        // Process mining queue
        if (timer >= miningDelay.get() && !miningQueue.isEmpty()) {
            int blocksMinedThisTick = 0;
            
            while (!miningQueue.isEmpty() && blocksMinedThisTick < maxBlocksPerTick.get()) {
                BlockPos pos = miningQueue.poll();
                
                if (pos != null && tryMineBlock(pos)) {
                    blocksMinedThisTick++;
                    
                    // Track that we mined this position
                    recentlyMinedPositions.add(pos);
                    
                    // Add to rendering if enabled
                    if (renderBlocks.get()) {
                        minedBlocks.add(new Pair<>(fadeTime.get(), pos));
                    }
                }
            }
            
            timer = 0;
        } else {
            timer++;
        }
    }

    /**
     * Clean up caches to prevent memory leaks and false positives
     */
    private void cleanupCaches() {
        // Clear recently mined positions that are far from player
        recentlyMinedPositions.removeIf(pos -> 
            !mc.player.getBlockPos().isWithinDistance(pos, miningRange.get() * 2));
        
        // Clear block state cache for positions far from player
        blockStateCache.entrySet().removeIf(entry ->
            !mc.player.getBlockPos().isWithinDistance(entry.getKey(), miningRange.get() * 3));
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (!renderBlocks.get()) return;
        
        for (Pair<Integer, BlockPos> pair : minedBlocks) {
            BlockPos pos = pair.getRight();
            float alpha = (float) pair.getLeft() / fadeTime.get();
            
            Color color = new Color(
                    miningColor.get().r,
                    miningColor.get().g,
                    miningColor.get().b,
                    (int) (miningColor.get().a * alpha)
            );
            
            event.renderer.box(pos, color, null, ShapeMode.Sides, 0);
        }
    }

    private void addAdjacentBlocks(BlockPos startPos, Block targetBlock, int chainLength) {
        if (chainLength >= maxChainLength.get()) return;
        
        for (Direction direction : DIRECTIONS) {
            BlockPos adjacentPos = startPos.offset(direction);
            
            // Skip if already processed or out of range
            if (processedBlocks.contains(adjacentPos) || 
                !mc.player.getBlockPos().isWithinDistance(adjacentPos, miningRange.get())) {
                continue;
            }
            
            BlockState adjacentState = mc.world.getBlockState(adjacentPos);
            Block adjacentBlock = adjacentState.getBlock();
            
            // Check if this is the same block type we want to mine
            if (adjacentBlock == targetBlock && shouldMineBlock(adjacentBlock)) {
                processedBlocks.add(adjacentPos);
                miningQueue.offer(adjacentPos);
                
                // Recursively add adjacent blocks (limited by chain length)
                addAdjacentBlocks(adjacentPos, targetBlock, chainLength + 1);
            }
        }
    }

    private boolean shouldMineBlock(Block block) {
        // Check blacklist first
        if (blacklist.get().contains(block)) return false;
        
        // Check whitelist if enabled
        if (whitelistEnabled.get() && !whitelist.get().contains(block)) return false;
        
        // Don't mine air or fluids
        if (block == Blocks.AIR || block == Blocks.VOID_AIR || block == Blocks.CAVE_AIR) return false;
        
        return true;
    }

    private boolean tryMineBlock(BlockPos pos) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return false;
        
        BlockState state = mc.world.getBlockState(pos);
        Block block = state.getBlock();
        
        // Verify the block is still the same type and mineable
        if (lastMinedBlock == null || block != lastMinedBlock || !shouldMineBlock(block)) return false;
        
        // Check if we can actually mine this block
        if (!canMineBlock(pos, state)) return false;
        
        // Switch to best tool if auto-tool is enabled
        if (autoTool.get()) {
            switchToBestTool(state);
        }
        
        // Check if we have a valid tool
        if (requireValidTool.get() && !hasValidTool(state)) return false;
        
        // Start breaking the block - use actual game mining logic
        Direction direction = getBreakDirection(pos);
        if (direction == null) return false;
        
        // Use the game's actual mining interaction instead of packets
        // This respects all game rules, enchantments, effects, etc.
        return performActualMining(pos, direction);
    }

    private boolean performActualMining(BlockPos pos, Direction direction) {
        try {
            // Use the interaction manager's actual attack block method
            // This handles all the game logic including:
            // - Enchantments (efficiency, silk touch, fortune)
            // - Status effects (haste, mining fatigue)  
            // - Tool durability and breaking
            // - Block hardness and mining time
            // - Creative vs survival mode differences
            // - Proper drop calculation
            boolean started = mc.interactionManager.attackBlock(pos, direction);
            
            if (started) {
                // Swing hand if enabled
                if (swingHand.get()) {
                    mc.player.swingHand(Hand.MAIN_HAND);
                }
                return true;
            }
            
            return false;
        } catch (Exception e) {
            // Fallback to packet-based mining if interaction fails
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                    PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, pos, direction));
            
            if (swingHand.get()) {
                mc.player.swingHand(Hand.MAIN_HAND);
            }
            
            return true;
        }
    }

    private boolean canMineBlock(BlockPos pos, BlockState state) {
        // Check if the block can be broken
        if (state.getHardness(mc.world, pos) < 0) return false;
        
        // Check if player can reach the block
        if (!mc.player.getBlockPos().isWithinDistance(pos, miningRange.get())) return false;
        
        // Check line of sight (basic check)
        Vec3d playerPos = mc.player.getEyePos();
        Vec3d blockPos = Vec3d.ofCenter(pos);
        return mc.world.raycast(new net.minecraft.world.RaycastContext(
                playerPos, blockPos,
                net.minecraft.world.RaycastContext.ShapeType.OUTLINE,
                net.minecraft.world.RaycastContext.FluidHandling.NONE,
                mc.player
        )).getType() == net.minecraft.util.hit.HitResult.Type.MISS;
    }

    private void switchToBestTool(BlockState state) {
        ClientPlayerEntity player = mc.player;
        if (player == null) return;
        
        int bestSlot = -1;
        float bestSpeed = 1.0f;
        
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;
            
            float speed = stack.getMiningSpeedMultiplier(state);
            if (speed > bestSpeed) {
                bestSpeed = speed;
                bestSlot = i;
            }
        }
        
        if (bestSlot != -1) {
            InvUtils.swap(bestSlot, false);
        }
    }

    private boolean hasValidTool(BlockState state) {
        ItemStack heldItem = mc.player.getMainHandStack();
        return heldItem.isSuitableFor(state) || heldItem.getMiningSpeedMultiplier(state) > 1.0f;
    }

    private Direction getBreakDirection(BlockPos pos) {
        Vec3d playerPos = mc.player.getEyePos();
        Vec3d blockCenter = Vec3d.ofCenter(pos);
        Vec3d diff = blockCenter.subtract(playerPos);
        
        return Direction.getFacing(diff.x, diff.y, diff.z);
    }


}