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
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.Objects;

import org.apache.commons.lang3.NotImplementedException;

import com.kkllffaa.meteor_litematica_printer.Addon;
import com.kkllffaa.meteor_litematica_printer.MyUtils;
import com.kkllffaa.meteor_litematica_printer.MyUtils.*;


public class DeleterRef extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");
    private final SettingGroup sgProtection = settings.createGroup("Mining Protection");
    private final SettingGroup sgCache = settings.createGroup("Cache");
    private final SettingGroup sgLighting = settings.createGroup("Auto Lighting");
    //region 砖块偏移常量
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
    //endregion
   

    //region General
    private final Setting<ListMode> BlockListMode = sgGeneral.add(new EnumSetting.Builder<ListMode>()
        .name("BlockListMode")
        .description("Selection mode.")
        .defaultValue(ListMode.Whitelist)
        .build()
    );

    private final Setting<List<Block>> whiteListBlocks = sgGeneral.add(new BlockListSetting.Builder()
        .name("white-blocks")
        .description("Which blocks to mine.")
        .defaultValue(Blocks.NETHERITE_BLOCK)
        .visible(() -> BlockListMode.get() == ListMode.Whitelist)
        .build()
    );
    private final Setting<List<Block>> blackListBlocks = sgGeneral.add(new BlockListSetting.Builder()
        .name("black-blocks")
        .description("Which blocks to ignore.")
        .defaultValue(Blocks.BEDROCK)
        .visible(() -> BlockListMode.get() == ListMode.Blacklist)
        .build()
    );

    private final Setting<触发模式> TriggerMode = sgGeneral.add(new EnumSetting.Builder<触发模式>()
        .name("TriggerMode")
        .description("")
        .defaultValue(触发模式.手动相连同类)
        .build()
    );

    private final Setting<Boolean> MeshMine = sgGeneral.add(new BoolSetting.Builder()
        .name("mesh-mining")
        .description("Quickly probe blocks")
        .defaultValue(false)
        .visible(() -> TriggerMode.get() == 触发模式.自动半径全部)
        .build()
    );

    private final Setting<Integer> depth = sgGeneral.add(new IntSetting.Builder()
        .name("depth")
        .description("Amount of iterations used to scan for similar blocks.")
        .defaultValue(3)
        .min(1)
        .sliderRange(1, 15)
        .visible(() -> TriggerMode.get() == 触发模式.手动相连同类)
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
        .defaultValue(MyUtils.RandomDelayMode.None)
        .build()
    );

    private final Setting<Integer> maxBlocksPerTick = sgGeneral.add(new IntSetting.Builder()
        .name("max-blocks-per-tick")
        .description("Maximum blocks per tick.")
        .defaultValue(9)
        .min(1)
        .max(1024)
        .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Sends rotation packets to the server when mining.")
        .defaultValue(false)
        .build()
    );
    //endregion

    //region Render
    private final Setting<Boolean> showSwing = sgRender.add(new BoolSetting.Builder()
        .name("show-swing")
        .description("Swing hand client-side.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> renderToMine = sgRender.add(new BoolSetting.Builder()
        .name("render-to-mine")
        .description("渲染需要挖掘的方块")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> renderMining = sgRender.add(new BoolSetting.Builder()
        .name("render-mining")
        .description("渲染正在挖掘的方块")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> renderTimeOut = sgRender.add(new BoolSetting.Builder()
        .name("render-time-out")
        .description("渲染挖掘超时的方块")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> renderMined = sgRender.add(new BoolSetting.Builder()
        .name("render-mined")
        .description("渲染已挖掘的方块")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> renderReboundCache = sgCache.add(new BoolSetting.Builder()
        .name("render-rebound-cache")
        .description("渲染挖掘后回弹的方块")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> renderCanceledOrMinedEndWithoutRebound = sgRender.add(new BoolSetting.Builder()
        .name("render-canceled-or-mined-end-without-rebound")
        .description("渲染挖掘取消或挖掘完毕(不回弹)的方块")
        .defaultValue(true)
        .build()
    );

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How the shapes are rendered.")
        .defaultValue(ShapeMode.Lines)
        .build()
    );
    //endregion

    //region Protection Settings
    private final Setting<Boolean> 启用流体相邻保护 = sgProtection.add(new BoolSetting.Builder()
        .name("enable-fluid-adjacent-protection")
        .description("Enable protection to prevent mining blocks that are adjacent to fluids (water, lava, etc.).")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> 自定义相邻保护 = sgProtection.add(new BoolSetting.Builder()
        .name("enable-custom-adjacent-protection")
        .description("Enable custom protection to prevent mining blocks that are adjacent to blocks from the protection lists.")
        .defaultValue(true)
        .build()
    );

    private final Setting<List<Block>> 相邻保护上 = sgProtection.add(new BlockListSetting.Builder()
        .name("adjacent-protection-upper")
        .description("Blocks to protect against mining in the upper direction. Mining will be prevented if the target block has any of these blocks above it.")
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
        .visible(自定义相邻保护::get)
        .build()
    );

    private final Setting<List<Block>> 相邻保护侧 = sgProtection.add(new BlockListSetting.Builder()
        .name("adjacent-protection-side")
        .description("Blocks to protect against mining in the side directions. Mining will be prevented if the target block has any of these blocks around its sides.")
        .defaultValue(Blocks.LAVA, Blocks.WATER)
        .visible(自定义相邻保护::get)
        .build()
    );

    private final Setting<List<Block>> 相邻保护下= sgProtection.add(new BlockListSetting.Builder()
        .name("adjacent-protection-lower")
        .description("Blocks to protect against mining in the lower direction. Mining will be prevented if the target block has any of these blocks below it.")
        .defaultValue(Blocks.LAVA, Blocks.WATER)
        .visible(自定义相邻保护::get)
        .build()
    );

    private final Setting<Double> 挖掘回弹时间 = sgCache.add(new DoubleSetting.Builder()
        .name("mining-rebound-time")
        .description("Duration in seconds to keep mined positions in cache for rebound protection.")
        .defaultValue(2)
        .min(0.5).sliderMin(0.5)
        .max(15).sliderMax(15)
        .build()
    );

    private final Setting<Boolean> 启用挖掘超时保护 = sgProtection.add(new BoolSetting.Builder()
        .name("enable-mining-timeout-protection")
        .description("Enable protection that skips blocks taking too long to mine and adds them to cache.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> 挖掘超时时间 = sgProtection.add(new DoubleSetting.Builder()
        .name("mining-timeout-duration")
        .description("Maximum duration in seconds to spend mining a single block before skipping it.")
        .defaultValue(2.0)
        .min(0.5)
        .max(30.0)
        .sliderRange(0.5, 15.0)
        .visible(启用挖掘超时保护::get)
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
    //endregion

    private final Pool<MyBlock> blockPool = new Pool<>(MyBlock::new);
    //存储将要挖掘，挖掘中，挖掘超时，挖掘完毕(延迟未完成)，挖掘完毕延迟后回弹的方块，
    //不存储挖掘取消，挖掘完毕(延迟后不回弹)的方块
    private final List<MyBlock> blocks = new ArrayList<>();

    private final List<BlockPos> foundBlockPos = new ArrayList<>();

    private int tick = 0;
    private final Random random = new Random();

    public DeleterRef() {
        super(Addon.CRUDCATEGORY, "block-deleter", "Deletes all nearby blocks with this type");
    }

    private boolean isOutOfDistance(BlockPos Pos) {
        double handDistance =  distanceProtection.get() == DistanceMode.Auto ? mc.player.getBlockInteractionRange() : maxDistance.get();
        return MyUtils.getDistanceToPlayerEyes(Pos) > handDistance;
    }

    private boolean 允许存入挖掘表(BlockPos pos){
        if (isOutOfDistance(pos)) return false;
        throw new NotImplementedException();
    }
    private boolean 允许存入挖掘表(BlockState state){
        throw new NotImplementedException();
    }
    private boolean 允许存入挖掘表(Block block){
        if (BlockListMode.get() == ListMode.Whitelist && !whiteListBlocks.get().contains(block))
            return false;
        if (BlockListMode.get() == ListMode.Blacklist && blackListBlocks.get().contains(block))
            return false;

        return true;
    }



    @Override
    public void onDeactivate() {
        blocks.forEach(blockPool::free);
        blocks.clear();
        foundBlockPos.clear();
    }



    @EventHandler
    private void onStartBreakingBlock(StartBreakingBlockEvent event) {
        if (!isActive()) return;
        if (TriggerMode.get() != 触发模式.手动相连同类) return;

        BlockPos pos = event.blockPos;
        if (!允许存入挖掘表(pos)) return;

        BlockState state = mc.world.getBlockState(pos);
        if (!允许存入挖掘表(state)) return;
        
        Block block = state.getBlock();
        if (!允许存入挖掘表(block)) return;

        TryBlocksAdd(pos, block);

        foundBlockPos.clear();
        mineNearbyBlocks(block.asItem(), pos, depth.get());
    }

    private void mineNearbyBlocks(Item item, BlockPos pos, int depth) {
        if (depth<=0) return;
        if (foundBlockPos.contains(pos)) return;
        foundBlockPos.add(pos);


        for(Vec3i neighbourOffset: blockNeighbours) {
            BlockPos neighbourPos = pos.add(neighbourOffset);
            if (!允许存入挖掘表(neighbourPos)) continue;
            BlockState neighbourState = mc.world.getBlockState(neighbourPos);
            if (!允许存入挖掘表(neighbourState)) continue;
            Block neighbourBlock = neighbourState.getBlock();

            if (neighbourBlock.asItem() == item) {
                TryBlocksAdd(neighbourPos, neighbourBlock);
                mineNearbyBlocks(item, neighbourPos, depth-1);
            }
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (!isActive()) return;

        blocks.forEach(MyBlock::UpdateState);
        blocks.removeIf(block -> {
            if (block.shouldRemove()) {
                blockPool.free(block);
                return true;
            }
            return false;
        });

        if (!blocks.isEmpty()) {
            int[] randomDelays = randomDelayMode.get().delays;
            int randomDelay = randomDelays[random.nextInt(randomDelays.length)];
            int totalDelay = delay.get() + randomDelay;
            
            if (tick < totalDelay) {
                tick++;
                return;
            }
            tick = 0;

            int count = 0;
            for (MyBlock block : blocks) {
                if (count >= maxBlocksPerTick.get()) break;

                if (block.state != MyBlock.State.ToMine && block.state != MyBlock.State.Mining) continue;

                if (BlockUtils.canInstaBreak(block.blockPos)) {
                    if (block.state == MyBlock.State.ToMine) {
                        block.mine();
                        count++;
                    }
                } else {
                    block.mine();
                    break;
                }

            }
            
        }

    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        blocks.forEach(block -> block.render(event));
    }

    private class MyBlock {
        private BlockPos blockPos;
        private Block originalBlock;
        private long startTime = 0;
        private long finishTime = 0;
        public State state = State.ToMine;
        private static enum State {
            ToMine,
            Mining,
            TimeOut,
            MinedMayRebound,
            Rebound,
            Canceled,
            MinedEndWithoutRebound,
        }
        public void UpdateState(){
            state = calculateState();
        }
        private State calculateState() {
            if (!允许存入挖掘表(blockPos)) {
                return State.Canceled;
            }

            if (finishTime == 0) {
                var world = mc.world;
                if (world == null || world.getBlockState(blockPos).getBlock() != originalBlock) {
                    finishTime = System.currentTimeMillis();
                }
            }

            if (startTime == 0) {
                if (finishTime == 0) {
                    return State.ToMine;
                }else{
                    return State.MinedEndWithoutRebound;
                }
            }else {
                if (finishTime == 0){
                    if (启用挖掘超时保护.get()){
                        long currentTime = System.currentTimeMillis();
                        double elapsedSeconds = (currentTime - startTime) / 1000.0;
                        if (elapsedSeconds > 挖掘超时时间.get()) {
                            return State.TimeOut;
                        }
                    }
                    return State.Mining;
                }else{
                    long currentTime = System.currentTimeMillis();
                    double elapsedSeconds = (currentTime - finishTime) / 1000.0;
                    if (elapsedSeconds < 挖掘回弹时间.get()){
                        return State.MinedMayRebound;
                    }else{
                        var world = mc.world;
                        if (world == null || world.getBlockState(blockPos).getBlock() != originalBlock) {
                            return State.MinedEndWithoutRebound;
                        }else{
                            return State.Rebound;
                        }
                    }
                }
            }
        }
            


        public void set(BlockPos pos, Block originalBlock) {
            this.blockPos = pos;
            this.originalBlock = originalBlock;
            this.startTime = 0;
            this.finishTime = 0;
            this.state = State.ToMine;
        }

        public boolean shouldRemove() {
            return state == State.Canceled || state == State.MinedEndWithoutRebound;
        }

        public void mine() {
            if (startTime == 0) {
                startTime = System.currentTimeMillis();
            }
            if (rotate.get()) Rotations.rotate(Rotations.getYaw(blockPos), Rotations.getPitch(blockPos), 50, this::updateBlockBreakingProgress);
            else updateBlockBreakingProgress();
        }

        private void updateBlockBreakingProgress() {
            BlockUtils.breakBlock(blockPos, showSwing.get());
        }

        public void render(Render3DEvent event) {
            switch (state){
                case ToMine ->{
                    if(renderToMine.get()){
                        var color = ColorScheme.红;
                        MyUtils.renderPos(event, blockPos, shapeMode.get(), color.sideColor, color.lineColor);
                    }
                }
                case Mining ->{
                    if(renderMining.get()){
                        var color = ColorScheme.蓝;
                        MyUtils.renderPos(event, blockPos, shapeMode.get(), color.sideColor, color.lineColor);
                    }
                }
                case TimeOut ->{
                    if(renderTimeOut.get()){
                        var color = ColorScheme.黄;
                        MyUtils.renderPos(event, blockPos, shapeMode.get(), color.sideColor, color.lineColor);
                    }
                }
                case MinedMayRebound ->{
                    if(renderMined.get()){
                        var color = ColorScheme.紫;
                        MyUtils.renderPos(event, blockPos, shapeMode.get(), color.sideColor, color.lineColor);
                    }
                }
                case Rebound ->{
                    if(renderReboundCache.get()){
                        var color = ColorScheme.绿;
                        MyUtils.renderPos(event, blockPos, shapeMode.get(), color.sideColor, color.lineColor);
                    }
                }
                case Canceled, MinedEndWithoutRebound ->{
                    if(renderCanceledOrMinedEndWithoutRebound.get()){
                        var color = ColorScheme.青;
                        MyUtils.renderPos(event, blockPos, shapeMode.get(), color.sideColor, color.lineColor);
                    }
                }
            }
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            MyBlock myBlock = (MyBlock) obj;
            return Objects.equals(blockPos, myBlock.blockPos);
        }

        @Override
        public int hashCode() {
            return Objects.hash(blockPos);
        }
    }


    private void TryBlocksAdd(BlockPos pos, Block originalBlock){
        if (表里已经包含(pos)) return;
        MyBlock block = blockPool.get(); 
        block.set(pos, originalBlock);
        blocks.add(block);
    }
    private boolean 表里已经包含(BlockPos pos) {
        for (MyBlock block : blocks) {
            if (block.blockPos.equals(pos)) return  true;
        }

        return false;
    }

    @Override
    public String getInfoString() {
        return BlockListMode.get().toString();
    }



    public enum 触发模式 {
        自动半径全部,
        手动相连同类,
    }



}
