package com.kkllffaa.meteor_litematica_printer;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class Deleter extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgFilters = settings.createGroup("Filters");
    private final SettingGroup sgRendering = settings.createGroup("Rendering");

    // General settings
    private final Setting<Integer> range = sgGeneral.add(new IntSetting.Builder()
        .name("range")
        .description("Maximum range for mining connected blocks.")
        .defaultValue(32)
        .min(1).sliderMin(1)
        .max(100).sliderMax(100)
        .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("Delay between mining blocks in ticks.")
        .defaultValue(2)
        .min(0).sliderMin(0)
        .max(20).sliderMax(20)
        .build()
    );

    private final Setting<Integer> blocksPerTick = sgGeneral.add(new IntSetting.Builder()
        .name("blocks-per-tick")
        .description("How many blocks to mine per tick.")
        .defaultValue(1)
        .min(1).sliderMin(1)
        .max(10).sliderMax(10)
        .build()
    );



    private final Setting<Boolean> includeDiagonals = sgGeneral.add(new BoolSetting.Builder()
        .name("include-diagonals")
        .description("Include diagonally adjacent blocks.")
        .defaultValue(false)
        .build()
    );

    private final Setting<SearchMode> searchMode = sgGeneral.add(new EnumSetting.Builder<SearchMode>()
        .name("search-mode")
        .description("How to search for connected blocks.")
        .defaultValue(SearchMode.BFS)
        .build()
    );

    private final Setting<Boolean> requireTool = sgGeneral.add(new BoolSetting.Builder()
        .name("require-tool")
        .description("Only mine blocks if you have the appropriate tool.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> swing = sgGeneral.add(new BoolSetting.Builder()
        .name("swing")
        .description("Swing hand when mining.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Rotate to face blocks being mined.")
        .defaultValue(false)
        .build()
    );

    // Filter settings
    private final Setting<List<Block>> whitelist = sgFilters.add(new BlockListSetting.Builder()
        .name("whitelist")
        .description("Only auto-mine these blocks. Leave empty to mine all blocks.")
        .build()
    );

    private final Setting<List<Block>> blacklist = sgFilters.add(new BlockListSetting.Builder()
        .name("blacklist")
        .description("Never auto-mine these blocks.")
        .build()
    );

    // Rendering settings
    private final Setting<Boolean> renderBlocks = sgRendering.add(new BoolSetting.Builder()
        .name("render-blocks")
        .description("Render blocks that will be mined.")
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

    private final Setting<SettingColor> queueColor = sgRendering.add(new ColorSetting.Builder()
        .name("queue-color")
        .description("Color for blocks in mining queue.")
        .defaultValue(new SettingColor(255, 255, 0, 100))
        .visible(renderBlocks::get)
        .build()
    );

    private final Setting<SettingColor> minedColor = sgRendering.add(new ColorSetting.Builder()
        .name("mined-color")
        .description("Color for recently mined blocks.")
        .defaultValue(new SettingColor(255, 0, 0, 100))
        .visible(renderBlocks::get)
        .build()
    );

    private int timer = 0;
    private final Queue<BlockPos> miningQueue = new ConcurrentLinkedQueue<>();
    private final Set<BlockPos> processed = new HashSet<>();
    private final List<Pair<Integer, BlockPos>> minedBlocks = new ArrayList<>();
    private Block lastMinedBlockType = null;
    private BlockPos lastMinedPos = null;
    private final Map<BlockPos, Block> pendingBreaks = new HashMap<>();

    public Deleter() {
        super(Addon.CATEGORY, "block-deleter", "Automatically mines connected blocks of the same type");
    }

    @Override
    public void onActivate() {
        clear();
    }

    @Override
    public void onDeactivate() {
        clear();
    }

    private void clear() {
        miningQueue.clear();
        processed.clear();
        minedBlocks.clear();
        pendingBreaks.clear();
        lastMinedBlockType = null;
        lastMinedPos = null;
        timer = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) {
            clear();
            return;
        }

        // Update rendered blocks
        minedBlocks.removeIf(pair -> {
            pair.setLeft(pair.getLeft() - 1);
            return pair.getLeft() <= 0;
        });

        // Check if we should start mining
        if (shouldStartMining()) {
            if (lastMinedPos != null && lastMinedBlockType != null) {
                findConnectedBlocks(lastMinedPos, lastMinedBlockType);
                lastMinedBlockType = null; // Reset to avoid repeated scanning
                lastMinedPos = null;
            }
        }

        // Process mining queue
        if (timer >= delay.get()) {
            processQueue();
            timer = 0;
        } else {
            timer++;
        }
    }

    private boolean shouldStartMining() {
        return miningQueue.isEmpty() && lastMinedBlockType != null;
    }



    private void findConnectedBlocks(BlockPos startPos, Block blockType) {
        processed.clear();
        miningQueue.clear();

        if (searchMode.get() == SearchMode.BFS) {
            findConnectedBlocksBFS(startPos, blockType);
        } else {
            findConnectedBlocksDFS(startPos, blockType);
        }
    }

    private void findConnectedBlocksBFS(BlockPos startPos, Block blockType) {
        Queue<BlockPos> searchQueue = new LinkedList<>();
        searchQueue.offer(startPos);
        processed.add(startPos);

        while (!searchQueue.isEmpty() && processed.size() < range.get()) {
            BlockPos current = searchQueue.poll();
            
            for (BlockPos adjacent : getAdjacentPositions(current)) {
                if (shouldMineBlock(adjacent, blockType) && !processed.contains(adjacent)) {
                    processed.add(adjacent);
                    searchQueue.offer(adjacent);
                    miningQueue.offer(adjacent);
                }
            }
        }
    }

    private void findConnectedBlocksDFS(BlockPos startPos, Block blockType) {
        Stack<BlockPos> searchStack = new Stack<>();
        searchStack.push(startPos);

        while (!searchStack.isEmpty() && processed.size() < range.get()) {
            BlockPos current = searchStack.pop();
            
            if (processed.contains(current)) continue;
            processed.add(current);

            for (BlockPos adjacent : getAdjacentPositions(current)) {
                if (shouldMineBlock(adjacent, blockType) && !processed.contains(adjacent)) {
                    searchStack.push(adjacent);
                    miningQueue.offer(adjacent);
                }
            }
        }
    }

    private List<BlockPos> getAdjacentPositions(BlockPos center) {
        List<BlockPos> adjacent = new ArrayList<>();
        
        if (includeDiagonals.get()) {
            // Include all 26 adjacent positions (including diagonals)
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        if (x == 0 && y == 0 && z == 0) continue;
                        adjacent.add(center.add(x, y, z));
                    }
                }
            }
        } else {
            // Only include the 6 face-adjacent positions
            for (Direction direction : Direction.values()) {
                adjacent.add(center.offset(direction));
            }
        }
        
        return adjacent;
    }



    private boolean shouldMineBlock(BlockPos pos, Block targetType) {
        if (mc.world == null || mc.player == null) return false;

        BlockState state = mc.world.getBlockState(pos);
        Block block = state.getBlock();

        // Check if it's the same type
        if (block != targetType) return false;

        // Check whitelist/blacklist
        if (!whitelist.get().isEmpty() && !whitelist.get().contains(block)) return false;
        if (blacklist.get().contains(block)) return false;

        // Check if within reach (creative: 5.0, survival: 4.5)
        double distance = mc.player.getPos().distanceTo(Vec3d.ofCenter(pos));
        double reachDistance = mc.player.isCreative() ? 5.0 : 4.5;
        if (distance > reachDistance) return false;

        // Check if we have appropriate tool
        if (requireTool.get() && !hasAppropriateTool(state)) return false;

        return true;
    }

    private boolean hasAppropriateTool(BlockState state) {
        // Check if player has an appropriate tool for this block
        FindItemResult tool = InvUtils.find(itemStack -> {
            if (itemStack.getItem() == Items.AIR) return false;
            return itemStack.isSuitableFor(state);
        });
        
        return tool.found() || mc.player.getMainHandStack().isSuitableFor(state);
    }

    private void processQueue() {
        if (miningQueue.isEmpty()) return;

        int mined = 0;
        while (!miningQueue.isEmpty() && mined < blocksPerTick.get()) {
            BlockPos pos = miningQueue.poll();
            if (mineBlock(pos)) {
                minedBlocks.add(new Pair<>(fadeTime.get(), pos));
                mined++;
            }
        }
    }

    private boolean mineBlock(BlockPos pos) {
        if (mc.player == null || mc.interactionManager == null || mc.world == null) return false;

        BlockState state = mc.world.getBlockState(pos);
        if (state.isAir()) return false;

        // Switch to appropriate tool if needed
        if (requireTool.get()) {
            FindItemResult tool = InvUtils.find(itemStack -> itemStack.isSuitableFor(state));
            if (tool.found()) {
                InvUtils.swap(tool.slot(), false);
            }
        }

        // Rotate if needed
        if (rotate.get()) {
            Vec3d blockCenter = Vec3d.ofCenter(pos);
            meteordevelopment.meteorclient.utils.player.Rotations.rotate(
                meteordevelopment.meteorclient.utils.player.Rotations.getYaw(blockCenter),
                meteordevelopment.meteorclient.utils.player.Rotations.getPitch(blockCenter),
                () -> performMining(pos)
            );
        } else {
            performMining(pos);
        }

        return true;
    }

    private void performMining(BlockPos pos) {
        if (mc.getNetworkHandler() == null || mc.player == null) return;

        BlockState state = mc.world.getBlockState(pos);
        if (state.isAir()) return;

        // Get the correct face to mine from
        Direction face = getMiningSide(pos);
        
        // Start mining
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
            PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, pos, face
        ));

        // Swing hand
        if (swing.get()) {
            mc.player.swingHand(Hand.MAIN_HAND);
        }

    }

    private Direction getMiningSide(BlockPos pos) {
        Vec3d playerPos = mc.player.getPos();
        Vec3d blockCenter = Vec3d.ofCenter(pos);
        
        // Find the face closest to the player
        Direction closestFace = Direction.UP;
        double closestDistance = Double.MAX_VALUE;
        
        for (Direction face : Direction.values()) {
            Vec3d faceCenter = blockCenter.add(
                face.getOffsetX() * 0.5,
                face.getOffsetY() * 0.5, 
                face.getOffsetZ() * 0.5
            );
            double distance = playerPos.distanceTo(faceCenter);
            if (distance < closestDistance) {
                closestDistance = distance;
                closestFace = face;
            }
        }
        
        return closestFace;
    }

    @EventHandler
    private void onPacketSend(PacketEvent.Send event) {
        if (mc.player == null || mc.world == null) return;
        
        if (event.packet instanceof PlayerActionC2SPacket packet) {
            BlockPos pos = packet.getPos();
            
            if (packet.getAction() == PlayerActionC2SPacket.Action.START_DESTROY_BLOCK) {
                // Record what block type is at this position before it's destroyed
                BlockState state = mc.world.getBlockState(pos);
                if (!state.isAir()) {
                    pendingBreaks.put(pos, state.getBlock());
                }
            } else if (packet.getAction() == PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK) {
                // Check if we have a pending break for this position
                Block brokenBlockType = pendingBreaks.remove(pos);
                if (brokenBlockType != null) {
                    // Schedule connected mining for this block type
                    lastMinedBlockType = brokenBlockType;
                    lastMinedPos = pos;
                }
            }
        }
    }



    @EventHandler
    private void onRender(Render3DEvent event) {
        if (!renderBlocks.get()) return;

        // Render mining queue
        for (BlockPos pos : miningQueue) {
            event.renderer.box(pos, queueColor.get(), queueColor.get(), ShapeMode.Both, 0);
        }

        // Render recently mined blocks with fade
        for (Pair<Integer, BlockPos> pair : minedBlocks) {
            int timeLeft = pair.getLeft();
            BlockPos pos = pair.getRight();
            
            int alpha = (int) (((float) timeLeft / (float) fadeTime.get()) * minedColor.get().a);
            Color fadeColor = new Color(minedColor.get().r, minedColor.get().g, minedColor.get().b, alpha);
            
            event.renderer.box(pos, fadeColor, fadeColor, ShapeMode.Both, 0);
        }
    }

    public enum SearchMode {
        BFS("Breadth First"),
        DFS("Depth First");

        private final String title;

        SearchMode(String title) {
            this.title = title;
        }

        @Override
        public String toString() {
            return title;
        }
    }
}
