package com.kkllffaa.meteor_litematica_printer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
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

    private final Setting<Boolean> instantBreak = sgGeneral.add(new BoolSetting.Builder()
            .name("instant-break")
            .description("Try to break blocks instantly (works better in creative mode).")
            .defaultValue(false)
            .build()
    );

    private final Setting<Integer> miningTimeout = sgGeneral.add(new IntSetting.Builder()
            .name("mining-timeout")
            .description("Maximum ticks to spend mining a single block before giving up.")
            .defaultValue(100)
            .min(20).sliderMin(20)
            .max(600).sliderMax(600)
            .build()
    );

    private final Setting<Boolean> mineThoughWalls = sgGeneral.add(new BoolSetting.Builder()
            .name("mine-through-walls")
            .description("Allow mining blocks that are not in direct line of sight.")
            .defaultValue(false)
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

    private final Setting<Boolean> debugMode = sgSafety.add(new BoolSetting.Builder()
            .name("debug-mode")
            .description("Show debug messages in chat.")
            .defaultValue(false)
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
    
    // Mining progress tracking
    private final Map<BlockPos, Float> miningProgress = new ConcurrentHashMap<>();
    private final Map<BlockPos, Integer> miningStartTime = new ConcurrentHashMap<>();
    private BlockPos currentlyMining = null;

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
        miningProgress.clear();
        miningStartTime.clear();
        currentlyMining = null;
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
        miningProgress.clear();
        miningStartTime.clear();
        currentlyMining = null;
        lastMinedBlock = null;
    }

    // Track blocks before they change to detect what was broken
    private final Map<BlockPos, BlockState> blockStateCache = new ConcurrentHashMap<>();
    private final Set<BlockPos> recentlyMinedPositions = ConcurrentHashMap.newKeySet();

    @EventHandler
    private void onPacketSend(PacketEvent.Send event) {
        if (mc.player == null || mc.world == null) return;
        
        // Listen for player mining actions
        if (event.packet instanceof PlayerActionC2SPacket packet) {
            if (packet.getAction() == PlayerActionC2SPacket.Action.START_DESTROY_BLOCK ||
                packet.getAction() == PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK) {
                
                BlockPos pos = packet.getPos();
                BlockState state = mc.world.getBlockState(pos);
                
                if (debugMode.get()) {
                    info("Player mining action: " + packet.getAction() + " at " + pos + 
                         " block: " + state.getBlock().getName().getString());
                }
                
                // Cache the current state before it potentially changes
                if (!state.isAir()) {
                    blockStateCache.put(pos, state);
                }
            }
        }
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (mc.player == null || mc.world == null) return;
        
        // Listen for block update packets which indicate block changes
        if (event.packet instanceof BlockUpdateS2CPacket packet) {
            BlockPos pos = packet.getPos();
            BlockState newState = packet.getState();
            
            // Debug logging
            if (debugMode.get()) {
                info("BlockUpdate at " + pos + ": " + newState.getBlock().getName().getString());
            }
            
            // Get the previous state from our cache - this is the key fix!
            BlockState oldState = blockStateCache.get(pos);
            if (oldState == null) {
                // If we don't have cached state, we can't determine what was destroyed
                // So we just update our cache and continue
                if (debugMode.get()) {
                    info("No cached state for " + pos + ", caching new state");
                }
                if (!newState.isAir()) {
                    blockStateCache.put(pos, newState);
                }
                return;
            }
            
            // Detect if a solid block was broken/destroyed (not just replaced)
            if (wasBlockDestroyed(oldState, newState)) {
                Block destroyedBlock = oldState.getBlock();
                
                if (debugMode.get()) {
                    info("Block destroyed: " + destroyedBlock.getName().getString() + " at " + pos);
                }
                
                // Only trigger if:
                // 1. The destroyed block should be chain-mined
                // 2. Player is close enough
                // 3. This position wasn't recently mined by us (avoid feedback loops)
                if (shouldMineBlockWithTypeFliter(destroyedBlock) && 
                    mc.player.getBlockPos().isWithinDistance(pos, miningRange.get() + 2) &&
                    !recentlyMinedPositions.contains(pos)) {
                    
                    if (debugMode.get()) {
                        info("Starting chain mining for " + destroyedBlock.getName().getString());
                    }
                    startChainMining(pos, destroyedBlock);
                } else {
                    if (debugMode.get()) {
                        info("Not starting chain mining: filter=" + shouldMineBlockWithTypeFliter(destroyedBlock) + 
                            " distance=" + mc.player.getBlockPos().isWithinDistance(pos, miningRange.get() + 2) +
                            " notRecent=" + !recentlyMinedPositions.contains(pos));
                    }
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
        
        if (debugMode.get()) {
            info("Starting chain mining session for " + block.getName().getString() + " at " + pos);
        }
        
        // Add adjacent blocks to queue
        addAdjacentBlocks(pos, block, 0);
        
        if (debugMode.get()) {
            info("Added " + miningQueue.size() + " blocks to mining queue");
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.world == null) return;

        // Update rendering fade
        minedBlocks.removeIf(pair -> {
            pair.setLeft(pair.getLeft() - 1);
            return pair.getLeft() <= 0;
        });

        // Pre-cache block states around player every few ticks
        if (timer % 10 == 0) { // Every 0.5 seconds
            cacheNearbyBlocks();
        }

        // Clean up cache periodically to prevent memory leaks
        if (timer % 100 == 0) { // Every 5 seconds
            cleanupCaches();
        }

        // Continue mining current block if we're in progress
        if (currentlyMining != null) {
            continueMining(currentlyMining);
        }

        // Process mining queue
        if (timer >= miningDelay.get() && !miningQueue.isEmpty() && currentlyMining == null) {
            int blocksStartedThisTick = 0;
            
            while (!miningQueue.isEmpty() && blocksStartedThisTick < maxBlocksPerTick.get() && currentlyMining == null) {
                BlockPos pos = miningQueue.poll();
                
                if (pos != null && startMiningBlock(pos)) {
                    blocksStartedThisTick++;
                    currentlyMining = pos;
                    miningStartTime.put(pos, timer);
                }
            }
            
            timer = 0;
        } else {
            timer++;
        }
        
        // Clean up timed-out mining operations
        cleanupTimedOutMining();
    }

    /**
     * Pre-cache block states around the player to detect changes
     */
    private void cacheNearbyBlocks() {
        if (mc.player == null || mc.world == null) return;
        
        BlockPos playerPos = mc.player.getBlockPos();
        int range = miningRange.get() + 1;
        
        // Cache blocks in a cube around the player
        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    BlockState state = mc.world.getBlockState(pos);
                    
                    // Only cache non-air blocks to save memory
                    if (!state.isAir()) {
                        blockStateCache.put(pos, state);
                    }
                }
            }
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
            if (adjacentBlock == targetBlock && shouldMineBlockWithTypeFliter(adjacentBlock)) {
                processedBlocks.add(adjacentPos);
                miningQueue.offer(adjacentPos);
                
                // Recursively add adjacent blocks (limited by chain length)
                addAdjacentBlocks(adjacentPos, targetBlock, chainLength + 1);
            }
        }
    }

    private boolean shouldMineBlockWithTypeFliter(Block block) {
        // Check blacklist first
        if (blacklist.get().contains(block)) return false;
        
        // Check whitelist if enabled
        if (whitelistEnabled.get() && !whitelist.get().contains(block)) return false;
        
        // Don't mine air or fluids
        if (block == Blocks.AIR || block == Blocks.VOID_AIR || block == Blocks.CAVE_AIR) return false;
        
        return true;
    }

    private boolean startMiningBlock(BlockPos pos) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return false;
        
        BlockState state = mc.world.getBlockState(pos);
        Block block = state.getBlock();
        
        if (debugMode.get()) {
            info("Starting to mine block at " + pos + ": " + block.getName().getString());
        }
        
        // Verify the block is still the same type and mineable
        if (lastMinedBlock == null || block != lastMinedBlock || !shouldMineBlockWithTypeFliter(block)) {
            if (debugMode.get()) {
                info("Block verification failed: lastMined=" + (lastMinedBlock != null ? lastMinedBlock.getName().getString() : "null") +
                     " current=" + block.getName().getString() + " filter=" + shouldMineBlockWithTypeFliter(block));
            }
            return false;
        }
        
        // Check if we can actually mine this block
        if (!canMineBlock(pos, state)) {
            if (debugMode.get()) {
                info("Cannot mine block at " + pos + " (hardness, range, or line of sight)");
            }
            return false;
        }
        
        // Start breaking the block - use actual game mining logic
        Direction direction = getBreakDirection(pos);
        if (direction == null) {
            if (debugMode.get()) {
                info("Could not determine break direction for " + pos);
            }
            return false;
        }
        
        // Initialize mining progress
        miningProgress.put(pos, 0.0f);
        
        // Start the mining process
        boolean result = performActualMining(pos, direction);
        
        if (debugMode.get()) {
            info("Mining start result: " + result + " for " + pos);
        }
        
        return result;
    }
    
    private void continueMining(BlockPos pos) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) {
            currentlyMining = null;
            return;
        }
        
        BlockState state = mc.world.getBlockState(pos);
        
        // Check if block still exists and is the same type
        if (state.isAir() || (lastMinedBlock != null && state.getBlock() != lastMinedBlock)) {
            // Block was broken or changed, we're done
            completeMining(pos);
            return;
        }
        
        // Check for timeout
        Integer startTime = miningStartTime.get(pos);
        if (startTime != null && (timer - startTime) > miningTimeout.get()) {
            if (debugMode.get()) {
                info("Mining timed out for " + pos);
            }
            currentlyMining = null;
            miningProgress.remove(pos);
            miningStartTime.remove(pos);
            return;
        }
        
        // Continue mining process
        Direction direction = getBreakDirection(pos);
        if (direction != null) {
            if (instantBreak.get()) {
                // For instant break mode, try to break immediately
                mc.interactionManager.attackBlock(pos, direction);
                // Force completion
                completeMining(pos);
            } else {
                // Normal mining - continue the mining process
                mc.interactionManager.updateBlockBreakingProgress(pos, direction);
            }
        }
    }
    
    private void completeMining(BlockPos pos) {
        currentlyMining = null;
        miningProgress.remove(pos);
        miningStartTime.remove(pos);
        
        // Track that we mined this position
        recentlyMinedPositions.add(pos);
        
        // Add to rendering if enabled
        if (renderBlocks.get()) {
            minedBlocks.add(new Pair<>(fadeTime.get(), pos));
        }
        
        if (debugMode.get()) {
            info("Completed mining block at " + pos);
        }
    }
    
    private void cleanupTimedOutMining() {
        // Clean up any stale mining operations
        miningStartTime.entrySet().removeIf(entry -> {
            int elapsedTime = timer - entry.getValue();
            if (elapsedTime > miningTimeout.get()) {
                BlockPos pos = entry.getKey();
                miningProgress.remove(pos);
                if (pos.equals(currentlyMining)) {
                    currentlyMining = null;
                }
                if (debugMode.get()) {
                    info("Cleaned up timed-out mining for " + pos);
                }
                return true;
            }
            return false;
        });
    }

    private boolean performActualMining(BlockPos pos, Direction direction) {
      
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
                return true;
            }
            
            return false;
   
    }

    private boolean canMineBlock(BlockPos pos, BlockState state) {
        // Check if the block can be broken
        if (state.getHardness(mc.world, pos) < 0) return false;
        
        // Check if player can reach the block
        if (!mc.player.getBlockPos().isWithinDistance(pos, miningRange.get())) return false;
        
        // Skip line of sight check if mine-through-walls is enabled
        if (mineThoughWalls.get()) {
            return true;
        }
        
        // Check line of sight - only mine blocks we can see
        Vec3d playerPos = mc.player.getEyePos();
        Vec3d blockPos = Vec3d.ofCenter(pos);
        
        var hitResult = mc.world.raycast(new net.minecraft.world.RaycastContext(
                playerPos, blockPos,
                net.minecraft.world.RaycastContext.ShapeType.OUTLINE,
                net.minecraft.world.RaycastContext.FluidHandling.NONE,
                mc.player
        ));
        
        // Allow mining if:
        // 1. No blocks in the way (MISS)
        // 2. Only hit the target block itself  
        // 3. Hit a block very close to target (for chain mining)
        if (hitResult.getType() == net.minecraft.util.hit.HitResult.Type.MISS) {
            return true;
        } else if (hitResult.getType() == net.minecraft.util.hit.HitResult.Type.BLOCK) {
            BlockPos hitPos = ((net.minecraft.util.hit.BlockHitResult) hitResult).getBlockPos();
            // Allow if we hit the target block itself or a nearby block
            return hitPos.equals(pos) || hitPos.isWithinDistance(pos, 1);
        }
        
        return false;
    }


    private Direction getBreakDirection(BlockPos pos) {
        Vec3d playerPos = mc.player.getEyePos();
        Vec3d blockCenter = Vec3d.ofCenter(pos);
        Vec3d diff = blockCenter.subtract(playerPos);
        
        return Direction.getFacing(diff.x, diff.y, diff.z);
    }


}