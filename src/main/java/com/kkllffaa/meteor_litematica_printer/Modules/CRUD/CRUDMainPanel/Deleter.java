package com.kkllffaa.meteor_litematica_printer.Modules.CRUD.CRUDMainPanel;

import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.world.EChestFarmer;
import meteordevelopment.meteorclient.systems.modules.world.HighwayBuilder;
import meteordevelopment.meteorclient.systems.modules.world.Nuker;
import meteordevelopment.meteorclient.systems.modules.world.VeinMiner;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.Objects;


import com.kkllffaa.meteor_litematica_printer.Addon;
import com.kkllffaa.meteor_litematica_printer.Functions.BlockPosUtils;
import com.kkllffaa.meteor_litematica_printer.Functions.MyUtils;
import com.kkllffaa.meteor_litematica_printer.Functions.MyUtils.*;
import com.kkllffaa.meteor_litematica_printer.Modules.CRUD.AtomicSettings.BreakSettings;
import com.kkllffaa.meteor_litematica_printer.Modules.CRUD.AtomicSettings.CommonSettings;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;


public class Deleter extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");
    private final SettingGroup sgProtection = settings.createGroup("Mining Protection");
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
        .defaultValue(Blocks.NETHERRACK)
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

    private final Setting<MeshMineMode> meshMineMode = sgGeneral.add(new EnumSetting.Builder<MeshMineMode>()
        .name("mesh-mining-mode")
        .description("Quickly probe blocks")
        .defaultValue(MeshMineMode.CacheAndAir)
        .visible(() -> TriggerMode.get() == 触发模式.自动半径全部 && MeshMine.get())
        .build()
    );

    private final Setting<Boolean> OreChannel = sgGeneral.add(new BoolSetting.Builder()
        .name("ore-channel")
        .description("打通路径")
        .defaultValue(false)
        .visible(() -> TriggerMode.get() == 触发模式.自动半径全部)
        .build()
    );
    private final Setting<OreMode> 矿物挖掘模式 = sgGeneral.add(new EnumSetting.Builder<OreMode>()
        .name("ore-mode")
        .description("矿物挖掘模式")
        .defaultValue(OreMode.强制不挖掘)
        .visible(() -> TriggerMode.get() == 触发模式.自动半径全部 && OreChannel.get())
        .build()
    );
    private final Setting<List<Block>> OreBlocksForChannel = sgGeneral.add(new BlockListSetting.Builder()
        .name("ore-blocks-for-channel")
        .description("玩家到矿物的垂直路径不做网格挖掘(打通路径).")
        .defaultValue(
            Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE, 
            Blocks.DEEPSLATE_LAPIS_ORE, Blocks.DEEPSLATE_EMERALD_ORE, Blocks.DEEPSLATE_COAL_ORE, 
            Blocks.ANCIENT_DEBRIS
        )
        .visible(() -> TriggerMode.get() == 触发模式.自动半径全部 && OreChannel.get())
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
        .description("Delay before mining the next non-instant block.")
        .defaultValue(10)
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
        .defaultValue(20)
        .sliderRange(1, 30)
        .range(1, 1024)
        .build()
    );

    //endregion

    //region Render

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

    private final Setting<Boolean> renderReboundCache = sgRender.add(new BoolSetting.Builder()
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
    private final Setting<Boolean> 流体相邻保护 = sgProtection.add(new BoolSetting.Builder()
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

    private final Setting<Double> 挖掘回弹时间 = sgProtection.add(new DoubleSetting.Builder()
        .name("mining-rebound-time")
        .description("Duration in seconds to keep mined positions in cache for rebound protection.")
        .defaultValue(1.7)
        .min(0.06).sliderMin(0.5)
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
        .defaultValue(9.0)
        .min(0.5)
        .max(1024.0)
        .sliderRange(0.5, 15.0)
        .visible(启用挖掘超时保护::get)
        .build()
    );

    private final Setting<ProtectMode> standProtectionMode = sgProtection.add(new EnumSetting.Builder<ProtectMode>()
        .name("stand-protection")
        .description("Reference system for standing protection.")
        .defaultValue(ProtectMode.ReferencePlayerY)
        .build()
    );
    private final Setting<Integer> 站立高度 = sgProtection.add(new IntSetting.Builder()
        .name("below-your-foot")
        .description("Height you need to stand on top.")
        .defaultValue(62)
        .min(-64)
        .max(320)
        .visible(() -> standProtectionMode.get() == ProtectMode.ReferenceWorldY)
        .build()
    );

    private final Setting<ProtectMode> heightProtectionMode = sgProtection.add(new EnumSetting.Builder<ProtectMode>()
        .name("height-protection")
        .description("Reference system for height protection.")
        .defaultValue(ProtectMode.Off)
        .build()
    );

    private final Setting<Integer> minHeight = sgProtection.add(new IntSetting.Builder()
        .name("min-height")
        .description("Minimum height for mining blocks (relative to reference).")
        .defaultValue(0)
        .min(-64)
        .max(320)
        .sliderRange(-20, 20)
        .visible(() -> heightProtectionMode.get() != ProtectMode.Off)
        .build()
    );

    private final Setting<Integer> maxHeight = sgProtection.add(new IntSetting.Builder()
        .name("max-height")
        .description("Maximum height for mining blocks (relative to reference).")
        .defaultValue(1)
        .min(-64)
        .max(320)
        .sliderRange(-20, 20)
        .visible(() -> heightProtectionMode.get() != ProtectMode.Off)
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

    private int 硬砖前摇 = 0;
    private int 硬砖后摇 = 0;
    private MyBlock 上一冷却刻挖掘的一个硬砖 = null;
    public static boolean isBusy = false;
    
    private BlockPos lastPlayerPos = null;
    private int continuousScanTimer = 0;
    private static int 序号 = 1;
    public Deleter() {
        super(Addon.CRUD, "deleter-ps" + 序号, "Deletes blocks as you whish.");
        序号++;
    }

    @SuppressWarnings("unchecked")
    private static final Class<? extends Module>[] BlackModuleList = new Class[] {
            EChestFarmer.class, HighwayBuilder.class, Nuker.class, VeinMiner.class };

    private boolean isAirOrFluid(BlockState state) {
        return state.isAir() || !state.getFluidState().isEmpty();
    }
    
    private static final double 速度阈值 = 0.009;
    private boolean isPlayerSurrounding(BlockPos pos) {
        Vec3d playerPos = mc.player.getPos();
        double playerX = playerPos.x;
        double playerY = playerPos.y;
        double playerZ = playerPos.z;
        
        Vec3d velocity = mc.player.getVelocity();
        
        int xMin = (int) Math.floor(playerX - 0.5);
        int xMax = (int) Math.floor(playerX + 0.5);
        int zMin = (int) Math.floor(playerZ - 0.5);
        int zMax = (int) Math.floor(playerZ + 0.5);
        int yMin = (int) Math.floor(playerY + 0.001);
        int yMax = (int) Math.floor(playerY + 1.001);
        
        if (velocity.x > 速度阈值) xMax += 1;
        else if (velocity.x < -速度阈值) xMin -= 1;
        
        if (velocity.z > 速度阈值) zMax += 1;
        else if (velocity.z < -速度阈值) zMin -= 1;
        
        if (velocity.y > 速度阈值) yMax += 1;
        else if (velocity.y < -速度阈值) yMin -= 1;
        
        return pos.getY() >= yMin && pos.getY() <= yMax &&
               pos.getX() >= xMin && pos.getX() <= xMax &&
               pos.getZ() >= zMin && pos.getZ() <= zMax;
    }
    
    private boolean 无视网格挖掘和站立保护(Vec3i pos, List<Vec3i> OreBlocks, Vec3i PlayerPos) {
        if (PlayerPos == null || OreBlocks == null || OreBlocks.isEmpty()) return false;
        int playerY= PlayerPos.getY();
        int posY = pos.getY();
        int posX = pos.getX();
        int posZ = pos.getZ();

        for (Vec3i Ore : OreBlocks) {
            int OreY= Ore.getY();
            int OreX= Ore.getX();
            int OreZ= Ore.getZ();
            int minY = Math.min(playerY, OreY);
            int maxY = Math.max(playerY, OreY);

            int XZ半径 = Math.abs(posY - OreY);
            if (OreY > playerY) {
                XZ半径 = switch (XZ半径) {
                    case 0 -> 1;
                    default -> 2;
                };
            } 

            if (minY <= posY && posY <= maxY && Math.abs(posX - OreX) <= XZ半径 && Math.abs(posZ - OreZ) <= XZ半径) {
                return true;
            }   
        }
        return false;


    }
    private boolean isStandBlock(BlockPos pos) {
        ProtectMode standProtectionStatus = standProtectionMode.get();
        if (standProtectionStatus == ProtectMode.Off) {
            return false;
        }
        if (standProtectionStatus == ProtectMode.ReferencePlayerY) {
            return pos.getY() == mc.player.getBlockPos().getY() - 1;
        } else {
            return pos.getY() == 站立高度.get();
        }
    }

    private boolean isProtectedPosition(BlockPos pos) {
        if ((groundProtection.get() && !mc.player.isOnGround())
        ||(!isWithinWidthRange(pos))
        ||(!isWithinHeightRange(pos))
        ||(!isWithinRegion(pos))
        ||(!isWithinDirectionalRange(pos))
        ) {
            return true;
        }

        if (流体相邻保护.get()) {
            for (Vec3i neighbourOffset : faceNeighbours) {
                BlockPos neighbour = pos.add(neighbourOffset);
                BlockState neighbourState = mc.world.getBlockState(neighbour);
                
                if (!neighbourState.getFluidState().isEmpty()) {
                    return true;
                }
            }
        }
        
        if (自定义相邻保护.get()) {
            for (Vec3i neighbourOffset : upperNeighbours) {
                BlockPos neighbour = pos.add(neighbourOffset);
                BlockState neighbourState = mc.world.getBlockState(neighbour);
                
                if (相邻保护上.get().contains(neighbourState.getBlock())) {
                    return true;
                }
            }
            
            for (Vec3i neighbourOffset : sideNeighbours) {
                BlockPos neighbour = pos.add(neighbourOffset);
                BlockState neighbourState = mc.world.getBlockState(neighbour);
                
                if (相邻保护侧.get().contains(neighbourState.getBlock())) {
                    return true;
                }
            }
            
            for (Vec3i neighbourOffset : lowerNeighbours) {
                BlockPos neighbour = pos.add(neighbourOffset);
                BlockState neighbourState = mc.world.getBlockState(neighbour);

                if (相邻保护下.get().contains(neighbourState.getBlock())) {
                    return true;
                }
            }
        }
        
        return false;
    }

    //region ProtectionChecks

    private boolean isWithinHeightRange(BlockPos pos) {
        ProtectMode heightProtectionStatus = heightProtectionMode.get();
        if (heightProtectionStatus == ProtectMode.Off) {
            return true;
        }
        int referenceY = 0;
        if (heightProtectionStatus == ProtectMode.ReferencePlayerY) {
            referenceY = mc.player.getBlockPos().getY();
        }
        
        int relativeHeight = pos.getY() - referenceY;
        
        return relativeHeight >= minHeight.get() && relativeHeight <= maxHeight.get();
    }


    private boolean isWithinRegion(BlockPos pos) {
        if (!regionProtection.get()) return true;
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



    private boolean isWithinDirectionalRange(BlockPos pos) {
        if (!directionalProtection.get()) return true;
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

    //region WidthProtection
    private boolean isWithinWidthRange(BlockPos pos) {
        if (!widthProtection.get()) return true;
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

    //endregion

    //endregion

    private boolean 允许存入挖掘表(BlockPos pos){
        if(TriggerMode.get() == 触发模式.自动半径全部 && OreChannel.get()){
            return BreakSettings.canBreakByObjective(pos) && !isProtectedPosition(pos);
        }
        return BreakSettings.canBreakByObjective(pos) && !isStandBlock(pos) && !isProtectedPosition(pos);
    }
    private boolean 允许存入挖掘表(BlockState state){
        return !isAirOrFluid(state);
    }
    private boolean 允许存入挖掘表(Block block){
        if(TriggerMode.get() == 触发模式.自动半径全部 && OreChannel.get()){
            switch (矿物挖掘模式.get()){
                case 强制挖掘 -> {
                    if (OreBlocksForChannel.get().contains(block)) return true;
                }
                case 强制不挖掘 -> {
                    if (OreBlocksForChannel.get().contains(block)) return false;
                }
                case 遵循黑白名单 -> {

                }
            }
        }
        if (BlockListMode.get() == ListMode.Whitelist && !whiteListBlocks.get().contains(block))
            return false;
        if (BlockListMode.get() == ListMode.Blacklist && blackListBlocks.get().contains(block))
            return false;

        return true;
    }

    @Override
    public void onActivate() {
        for (Class<? extends Module> klass : BlackModuleList) {
                Module module = Modules.get().get(klass);
                if (module.isActive()) {
                    module.toggle();
                }
            }

        Collection<Module> allModules = Modules.get().getAll();
        for (Module module : allModules) {
            if (module instanceof Deleter && module != this && module.isActive()) {
                module.toggle();
            }
        }
    }

    @Override
    public void onDeactivate() {
        blocks.forEach(blockPool::free);
        blocks.clear();
        foundBlockPos.clear();
        isBusy = false;
    }

   
    private void 自动半径加入挖掘表() {
        if (TriggerMode.get() == 触发模式.手动相连同类) return;
        BlockPos currentPlayerPos = mc.player.getBlockPos();
        
        boolean playerMoved = lastPlayerPos == null || !lastPlayerPos.equals(currentPlayerPos);
        continuousScanTimer++;
        
        if (playerMoved || continuousScanTimer > 10) {
            lastPlayerPos = currentPlayerPos.toImmutable();
            continuousScanTimer = 0;

            Vec3d centerPos = mc.player.getEyePos();
            double radius = CommonSettings.getHandDistance();

            int minX = (int) (Math.floor(centerPos.x - radius) + 0.01);
            int maxX = (int) (Math.floor(centerPos.x + radius) + 0.01);
            int minY = (int) (Math.floor(centerPos.y - radius) + 0.01);
            int maxY = (int) (Math.floor(centerPos.y + radius) + 0.01);
            int minZ = (int) (Math.floor(centerPos.z - radius) + 0.01);
            int maxZ = (int) (Math.floor(centerPos.z + radius) + 0.01);
            if (TriggerMode.get() == 触发模式.自动半径全部) {
                scanWorld(minX, maxX, minY, maxY, minZ, maxZ);
            }else{
                scanWorldAndLitemetica(minX, maxX, minY, maxY, minZ, maxZ);
            }
        }
    }

    private void scanWorld(int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        Vec3i playerPos = null;
        List<Vec3i> OreBlocks = null;
        if (OreChannel.get()) {
            playerPos = mc.player.getBlockPos();

            OreBlocks = new ArrayList<>();
            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        BlockPos scanPos = new BlockPos(x, y, z);
                        BlockState scanState = mc.world.getBlockState(scanPos);
                        Block scanBlock = scanState.getBlock();
                        if (OreBlocksForChannel.get().contains(scanBlock)) {
                            OreBlocks.add(scanPos);
                        }
                    }
                }
            }
            
        }

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos scanPos = new BlockPos(x, y, z);

                    if (!允许存入挖掘表(scanPos))  continue;
                    BlockState scanState = mc.world.getBlockState(scanPos);
                    if (!允许存入挖掘表(scanState))  continue;
                    Block scanBlock = scanState.getBlock();
                    if (!允许存入挖掘表(scanBlock))  continue;

                    if(OreChannel.get()) {
                        if (无视网格挖掘和站立保护(scanPos, OreBlocks, playerPos)){
                            TryBlocksAdd(scanPos, scanBlock);
                            continue;
                        }
                        if(isStandBlock(scanPos))  continue;
                    }



                    if (
                        MeshMine.get() && !isPlayerSurrounding(scanPos)
                    ) {
                        boolean hasNeighbourInBlocks = false;
                        outer: for (Vec3i offset : faceNeighbours) {
                            BlockPos neighbour = scanPos.add(offset);
                            switch (meshMineMode.get()) {
                                case Cache -> {
                                    if (表里已经包含(neighbour)) {
                                        hasNeighbourInBlocks = true;
                                        break outer;
                                    }
                                }
                                case CacheAndAir -> {
                                    if (表里已经包含(neighbour) || mc.world.getBlockState(neighbour).isAir()) {
                                        hasNeighbourInBlocks = true;
                                        break outer;
                                    }
                                }
                                case CacheAndAirAndFluid -> {
                                    if (表里已经包含(neighbour) || isAirOrFluid(mc.world.getBlockState(neighbour))) {
                                        hasNeighbourInBlocks = true;
                                        break outer;
                                    }
                                }
                            }
                        }
                        if (hasNeighbourInBlocks) {
                            continue;
                        }
                    }
                    
                    TryBlocksAdd(scanPos, scanBlock);
                    
                }
            }
        }
    }
    private void scanWorldAndLitemetica(int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        WorldSchematic worldSchematic = SchematicWorldHandler.getSchematicWorld();
        if (worldSchematic == null) return;

        SchematicPlacement placement = DataManager.getSchematicPlacementManager().getSelectedSchematicPlacement();
        if (placement == null) return;

        BlockPos origin = placement.getOrigin();
        Vec3i size = placement.getSchematic().getMetadata().getEnclosingSize();
        if (origin == null || size == null) return;

        int schematicMinX = origin.getX();
        int schematicMaxX = origin.getX() + size.getX() - 1;
        int schematicMinY = origin.getY();
        int schematicMaxY = origin.getY() + size.getY() - 1;
        int schematicMinZ = origin.getZ();
        int schematicMaxZ = origin.getZ() + size.getZ() - 1;

        int interMinX = Math.max(minX, schematicMinX);
        int interMaxX = Math.min(maxX, schematicMaxX);
        int interMinY = Math.max(minY, schematicMinY);
        int interMaxY = Math.min(maxY, schematicMaxY);
        int interMinZ = Math.max(minZ, schematicMinZ);
        int interMaxZ = Math.min(maxZ, schematicMaxZ);

        if (interMinX > interMaxX || interMinY > interMaxY || interMinZ > interMaxZ) return;

        for (int x = interMinX; x <= interMaxX; x++) {
            for (int y = interMinY; y <= interMaxY; y++) {
                for (int z = interMinZ; z <= interMaxZ; z++) {
                    BlockPos worldPos = new BlockPos(x, y, z);
                    BlockPos schematicPos = worldPos.subtract(origin);
                    BlockState worldState = mc.world.getBlockState(worldPos);
                    BlockState schematicState = worldSchematic.getBlockState(schematicPos);
                    Block schematicBlock = schematicState.getBlock();
                    Block worldBlock = worldState.getBlock();
                    if (schematicBlock != worldBlock && 允许存入挖掘表(worldPos) && 允许存入挖掘表(worldState) && 允许存入挖掘表(worldBlock)) {
                        TryBlocksAdd(worldPos, worldBlock);
                    }
                }
            }
        }
    }
    @EventHandler
    private void onStartBreakingBlock(StartBreakingBlockEvent event) {
        if (!isActive() || TriggerMode.get() != 触发模式.手动相连同类) return;

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

        自动半径加入挖掘表();

        blocks.forEach(MyBlock::UpdateState);
        blocks.removeIf(block -> {
            if (block.shouldRemove()) {
                blockPool.free(block);
                return true;
            }
            return false;
        });

        if (!blocks.isEmpty()) {
            BlockPos playerPos = mc.player.getBlockPos();
            List<MyBlock> FliterBlocks = blocks.stream()
            .filter(b -> b.state == MyBlock.State.ToMine || b.state == MyBlock.State.Mining)
            .sorted(Comparator.comparingInt(b -> BlockPosUtils.getManhattanDistance(b.blockPos, playerPos)))
            .toList();
            
            List<MyBlock> ToAttackBlocks = FliterBlocks.stream()
            .filter(b -> b.canInstaBreak())
            .toList();
            
            int Attacks = Math.min(ToAttackBlocks.size(), maxBlocksPerTick.get());

            MyBlock 本tick需要挖掘的一个硬砖 = null;
            if (Attacks < maxBlocksPerTick.get()) {
                List<MyBlock> HardBlocks = FliterBlocks.stream()
                .filter(b -> !b.canInstaBreak())
                .toList();

                if (TriggerMode.get() == 触发模式.自动半径全部 && OreChannel.get()) {
                    List<MyBlock> Ores = HardBlocks.stream()
                            .filter(b -> OreBlocksForChannel.get().contains(b.originalBlock))
                            .toList();
                    本tick需要挖掘的一个硬砖 = Ores.stream()
                            .filter(b -> b.state == MyBlock.State.Mining)
                            .findFirst()
                            .orElseGet(() -> Ores.stream()
                                    .findFirst()
                                    .orElse(null));
                }

                if (本tick需要挖掘的一个硬砖 == null) {
                    本tick需要挖掘的一个硬砖 = HardBlocks.stream()
                            .filter(b -> b.state == MyBlock.State.Mining)
                            .findFirst()
                            .orElseGet(() -> HardBlocks.stream()
                                    .findFirst()
                                    .orElse(null));
                }
            }
            
            boolean 要挖同一个硬砖 = 上一冷却刻挖掘的一个硬砖 != null && 本tick需要挖掘的一个硬砖 != null
                && 本tick需要挖掘的一个硬砖.blockPos.equals(上一冷却刻挖掘的一个硬砖.blockPos);
            boolean 能连续挖掘硬砖 = Attacks < 1;
            
            if (能连续挖掘硬砖 && 要挖同一个硬砖) {
                硬砖后摇 = 0;
            }

            if (硬砖后摇 > 0) {
                硬砖后摇--;
                return;
            } 
            
            ToAttackBlocks.stream()
            .limit(Attacks)
            .forEach(MyBlock::mine);

            if (!能连续挖掘硬砖){
                硬砖前摇 = delay.get() + randomDelayMode.get().getTheDelay();
            }
            
            if(硬砖前摇>0){
                硬砖前摇--;
                return;
            }
            
            
            上一冷却刻挖掘的一个硬砖 = 本tick需要挖掘的一个硬砖;
            if (本tick需要挖掘的一个硬砖 != null){
                本tick需要挖掘的一个硬砖.mine();
                
                硬砖后摇 = 5;
            }
        }

        if(blocks.stream().filter( b -> b.state == MyBlock.State.Mining || b.state == MyBlock.State.ToMine).count() == 0){
            isBusy = false;

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
            if (!允许存入挖掘表(blockPos) || !允许存入挖掘表(originalBlock)) {
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
            isBusy = true;
            if (startTime == 0) {
                startTime = System.currentTimeMillis();
            }
            updateBlockBreakingProgress();
        }

        public boolean canInstaBreak() {
            if (Modules.get().isActive(com.kkllffaa.meteor_litematica_printer.Modules.Tools.AutoTool.class)) {
                if (mc.player.isCreative())
                    return true;
                BlockState state = mc.world.getBlockState(blockPos);
                for (int slot = 0; slot < 36; slot++) {
                    if (BlockUtils.getBreakDelta(slot, state) >= 1)
                        return true;
                }
                return false;
            } else if (Modules.get().isActive(meteordevelopment.meteorclient.systems.modules.player.AutoTool.class)) {
                if (mc.player.isCreative())
                    return true;
                BlockState state = mc.world.getBlockState(blockPos);
                for (int slot = 0; slot < 9; slot++) {
                    if (BlockUtils.getBreakDelta(slot, state) >= 1)
                        return true;
                }
                return false;
            }else{
                return BlockUtils.canInstaBreak(blockPos);
            }
        }

        private void updateBlockBreakingProgress() {
            BreakSettings.breakBlock(blockPos);
        }
       
        public void render(Render3DEvent event) {
            ColorScheme color = null;
            switch (state){
                case ToMine ->{
                    if(renderToMine.get()){
                        color = ColorScheme.红;
                    }
                }
                case Mining ->{
                    if(renderMining.get()){
                        color = ColorScheme.蓝;
                    }
                }
                case TimeOut ->{
                    if(renderTimeOut.get()){
                        color = ColorScheme.黄;
                    }
                }
                case MinedMayRebound ->{
                    if(renderMined.get()){
                        color = ColorScheme.紫;
                    }
                }
                case Rebound ->{
                    if(renderReboundCache.get()){
                        color = ColorScheme.绿;
                    }
                }
                case Canceled, MinedEndWithoutRebound ->{
                    if(renderCanceledOrMinedEndWithoutRebound.get()){
                        color = ColorScheme.青;
                    }
                }
            }
            if (color != null){
                MyUtils.renderPos(event, blockPos, shapeMode.get(), color);
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
        return TriggerMode.get().toString() + " " + blocks.size() + " blocks";
    }




    public static enum 触发模式 {
        自动半径全部,
        手动相连同类,
        litematica,
    }
    public static enum OreMode {
        遵循黑白名单,
        强制挖掘,
        强制不挖掘,
    }

    public static enum MeshMineMode{
        Cache,
        CacheAndAir,
        CacheAndAirAndFluid,
    }

}
