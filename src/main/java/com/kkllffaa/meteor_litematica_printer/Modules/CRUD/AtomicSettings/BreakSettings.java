package com.kkllffaa.meteor_litematica_printer.Modules.CRUD.AtomicSettings;


import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.player.InstantRebreak;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;


import com.kkllffaa.meteor_litematica_printer.Addon;
import com.kkllffaa.meteor_litematica_printer.Functions.BlockPosUtils;
import com.kkllffaa.meteor_litematica_printer.Functions.MyUtils.ActionMode;
import com.kkllffaa.meteor_litematica_printer.Functions.MyUtils.SafetyFace;



public class BreakSettings extends Module {
    public static BreakSettings Instance = new BreakSettings();
    public BreakSettings() {
		super(Addon.SettingsForCRUD, "Break", "Module to configure AtomicSettings.");
        this.toggle();
	}

	@Override
	public void toggle() {
        if (isActive()) {
            return;
        }
        super.toggle();
    }
    
	private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<ActionMode> instantRotation = sgGeneral.add(new EnumSetting.Builder<ActionMode>()
        .name("instant-rotate")
        .description("rotation pre mining.")
        .defaultValue(ActionMode.None)
        .build()
    );
    // private final Setting<ActionMode> swingHand = sgGeneral.add(new EnumSetting.Builder<ActionMode>()
    //     .name("swing-hand")
    //     .description("swing hand post mining.")
    //     .defaultValue(ActionMode.None)
    //     .build()
    // );
    private final Setting<SafetyFace> FaceBy = sgGeneral.add(new EnumSetting.Builder<SafetyFace>()
        .name("mining-face-by")
        .description("")
        .defaultValue(SafetyFace.PlayerPosition)
        .build()
    );
    
    public static boolean breaking;
    private static boolean breakingThisTick;

    @EventHandler(priority = EventPriority.HIGHEST + 100)
    private static void onTickPre(TickEvent.Pre event) {
        breakingThisTick = false;
    }

    @EventHandler(priority = EventPriority.LOWEST - 100)
    private static void onTickPost(TickEvent.Post event) {
        if (!breakingThisTick && breaking) {
            breaking = false;
            if (MeteorClient.mc.interactionManager != null) MeteorClient.mc.interactionManager.cancelBlockBreaking();
        }
    }

    public static boolean canBreakByObjectiveStep1(BlockPos blockPos){
        return CommonSettings.canTouchTheBlockAt(blockPos);
    }
    public static boolean canBreakByObjectiveStep2(BlockPos blockPos, BlockState blockState){
        return BlockUtils.canBreak(blockPos, blockState);
    }

    private static boolean canBreakByObjective(BlockPos blockPos){
        if (!canBreakByObjectiveStep1(blockPos)) return false;
        BlockState blockState = MeteorClient.mc.world.getBlockState(blockPos);
        if (!canBreakByObjectiveStep2(blockPos, blockState)) return false;
        return true;
    }

    public static void breakBlock(BlockPos blockPos){
        Instance.breakBlockStep1(blockPos);
    }

    public static boolean TryBreakBlock(BlockPos blockPos){
        if (!canBreakByObjective(blockPos)) return false;
        breakBlock(blockPos);
        return true;
    }

    private void breakBlockStep1(BlockPos blockPos) {
        switch (instantRotation.get()) {
            case ActionMode.None -> breakBlockStep2(blockPos);
            case ActionMode.SendPacket -> Rotations.rotate(Rotations.getYaw(blockPos), Rotations.getPitch(blockPos), 50, false, () -> breakBlockStep2(blockPos));
            case ActionMode.Normal -> Rotations.rotate(Rotations.getYaw(blockPos), Rotations.getPitch(blockPos), 50, true, () -> breakBlockStep2(blockPos));
        }
    }

    private void breakBlockStep2(BlockPos blockPos) {
        // Creating new instance of block pos because minecraft assigns the parameter to a field, and we don't want it to change when it has been stored in a field somewhere
        BlockPos pos = blockPos instanceof BlockPos.Mutable ? new BlockPos(blockPos) : blockPos;

        InstantRebreak ir = Modules.get().get(InstantRebreak.class);
        if (ir != null && ir.isActive() && ir.blockPos.equals(pos) && ir.shouldMine()) {
            ir.sendPacket();
            return;
        }
        Direction direction = switch (FaceBy.get()) {
            case SafetyFace.PlayerRotation -> BlockUtils.getDirection(pos);
            case SafetyFace.PlayerPosition -> BlockPosUtils.getDirectionFromPlayerPosition(pos);
        };

        if (MeteorClient.mc.interactionManager.isBreakingBlock())
            MeteorClient.mc.interactionManager.updateBlockBreakingProgress(pos, direction);
        else MeteorClient.mc.interactionManager.attackBlock(pos, direction);

        // switch (swingHand.get()) {
        //     case ActionMode.None -> {}
        //     case ActionMode.SendPacket -> MeteorClient.mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
        //     case ActionMode.Normal -> MeteorClient.mc.player.swingHand(Hand.MAIN_HAND);
        // }
        breaking = true;
        breakingThisTick = true;

    }

}
