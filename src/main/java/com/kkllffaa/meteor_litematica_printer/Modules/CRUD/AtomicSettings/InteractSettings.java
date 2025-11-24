package com.kkllffaa.meteor_litematica_printer.Modules.CRUD.AtomicSettings;

import meteordevelopment.meteorclient.settings.BlockListSetting;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import net.minecraft.block.*;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.List;

import com.kkllffaa.meteor_litematica_printer.Addon;
import com.kkllffaa.meteor_litematica_printer.Functions.BlockPosUtils;
import com.kkllffaa.meteor_litematica_printer.Functions.MyUtils.SafetyFace;



public class InteractSettings extends Module {
	public static InteractSettings Instance = new InteractSettings();
	public InteractSettings() {
		super(Addon.SettingsForCRUD, "Interact", "Module to configure AtomicSettings.");
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

    // private final Setting<ActionMode> swingHand = sgGeneral.add(new EnumSetting.Builder<ActionMode>()
    //     .name("swing-hand")
    //     .description("swing hand post interact.")
    //     .defaultValue(ActionMode.None)
    //     .build()
    // );

	private final Setting<SafetyFace> FaceBy = sgGeneral.add(new EnumSetting.Builder<SafetyFace>()
			.name("interact-face-by")
			.description("Determines which face of the block to interact with.")
			.defaultValue(SafetyFace.PlayerPosition)
			.build());
            
	private final Setting<Boolean> onlyInteractOnLook = sgGeneral.add(new BoolSetting.Builder()
			.name("only-interact-on-look-the-face")
			.description("Only interact with blocks when looking at the face to interact with.")
			.defaultValue(false)
			.build());

	private final Setting<List<Block>> stateBlocks = sgGeneral.add(new BlockListSetting.Builder()
			.name("state-blocks")
			.description("Blocks that need interaction to adjust their state.")
			.defaultValue(
					// 中继器、比较器
					Blocks.REPEATER, Blocks.COMPARATOR,
					// 音符盒
					Blocks.NOTE_BLOCK,

					// 拉杆
					Blocks.LEVER,
                    // 日光传感器
					Blocks.DAYLIGHT_DETECTOR,
					// 活板门
					Blocks.OAK_TRAPDOOR, Blocks.SPRUCE_TRAPDOOR, Blocks.BIRCH_TRAPDOOR, Blocks.JUNGLE_TRAPDOOR,
					Blocks.ACACIA_TRAPDOOR, Blocks.DARK_OAK_TRAPDOOR, Blocks.CRIMSON_TRAPDOOR,
					Blocks.WARPED_TRAPDOOR, Blocks.MANGROVE_TRAPDOOR, Blocks.BAMBOO_TRAPDOOR, Blocks.CHERRY_TRAPDOOR,
					Blocks.PALE_OAK_TRAPDOOR,
					Blocks.COPPER_TRAPDOOR, Blocks.EXPOSED_COPPER_TRAPDOOR, Blocks.WEATHERED_COPPER_TRAPDOOR, Blocks.OXIDIZED_COPPER_TRAPDOOR,
					// 门
					Blocks.OAK_DOOR, Blocks.SPRUCE_DOOR, Blocks.BIRCH_DOOR, Blocks.JUNGLE_DOOR,
					Blocks.ACACIA_DOOR, Blocks.DARK_OAK_DOOR, Blocks.CRIMSON_DOOR,
					Blocks.WARPED_DOOR, Blocks.MANGROVE_DOOR, Blocks.BAMBOO_DOOR, Blocks.CHERRY_DOOR,
					Blocks.PALE_OAK_DOOR,
					Blocks.COPPER_DOOR, Blocks.EXPOSED_COPPER_DOOR, Blocks.WEATHERED_COPPER_DOOR, Blocks.OXIDIZED_COPPER_DOOR,
					// 栅栏门
					Blocks.OAK_FENCE_GATE, Blocks.SPRUCE_FENCE_GATE, Blocks.BIRCH_FENCE_GATE, Blocks.JUNGLE_FENCE_GATE,
					Blocks.ACACIA_FENCE_GATE, Blocks.DARK_OAK_FENCE_GATE, Blocks.CRIMSON_FENCE_GATE,
					Blocks.WARPED_FENCE_GATE,
					Blocks.MANGROVE_FENCE_GATE, Blocks.BAMBOO_FENCE_GATE, Blocks.CHERRY_FENCE_GATE,
					Blocks.PALE_OAK_FENCE_GATE)
            .build());

	//todo: 添加穿墙保护
	public static int interactWithBlock(BlockPos blockPos, int count){
		return Instance.interactWithBlockStep(blockPos, count);
	}

	private int interactWithBlockStep(BlockPos pos, int count) {
		if (mc.player.isSneaking()||!CommonSettings.canTouchTheBlockAt(pos)){
			return 0;
		}
        Direction SafeFace = switch (FaceBy.get()) {
			case SafetyFace.PlayerRotation -> BlockUtils.getDirection(pos);
            case SafetyFace.PlayerPosition -> BlockPosUtils.getDirectionFromPlayerPosition(pos);
        };

        if (onlyInteractOnLook.get() && !BlockPosUtils.isPlayerYawPitchInTheFaceOfBlock(pos, SafeFace)) {
            return 0;
        }

        Vec3d hitPos = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        for (int i = 0; i < count; i++) {
		    BlockHitResult blockHitResult = new BlockHitResult(hitPos, SafeFace, pos, false);
		    ActionResult result = mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, blockHitResult);
		    if (!result.isAccepted()) {
                warning("Interaction not accepted at " + pos + ", result: " + result);
                return i;
            }
        }
		// if (count>0) {
        // 	switch (swingHand.get()) {
        // 	    case ActionMode.None -> {}
        // 	    case ActionMode.SendPacket -> MeteorClient.mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
        // 	    case ActionMode.Normal -> MeteorClient.mc.player.swingHand(Hand.MAIN_HAND);
        // 	}
		// }
        return count;
	}


	public static int calculateRequiredInteractions(BlockState targetState, BlockState currentState) {
		return Instance.calculateRequiredInteractionsStep(targetState, currentState);
	}

	private int calculateRequiredInteractionsStep(BlockState targetState, BlockState currentState) {
        Block currentblock = currentState.getBlock();
        if (currentblock != targetState.getBlock() || !stateBlocks.get().contains(currentblock)) {
            return 0;
        }

		// 音符盒
		if (currentblock instanceof NoteBlock) {
				int currentNote = currentState.get(Properties.NOTE);
				int targetNote = targetState.get(Properties.NOTE);

				// Note blocks cycle through 0-24 (25 states)
				int diff = (targetNote - currentNote + 25) % 25;
				return diff;
		}

		// 中继器
		if (currentblock instanceof RepeaterBlock) {
				int currentDelay = currentState.get(Properties.DELAY);
				int targetDelay = targetState.get(Properties.DELAY);

				// Repeaters cycle through 1-4 (4 states)
				int diff = (targetDelay - currentDelay + 4) % 4;
				return diff;
		}

		// 比较器
		if (currentblock instanceof ComparatorBlock) {
			return currentState.get(Properties.COMPARATOR_MODE) == targetState.get(Properties.COMPARATOR_MODE) ? 0
						: 1;
		}

		// 光传感器
		if (currentblock instanceof DaylightDetectorBlock) {
			return currentState.get(Properties.INVERTED) == targetState.get(Properties.INVERTED) ? 0 : 1;
			
		}

		// 拉杆
		if (currentblock instanceof LeverBlock) {
			return currentState.get(Properties.POWERED) == targetState.get(Properties.POWERED) ? 0 : 1;
		}

		// 栅栏门 活板门 门
		if (currentState.contains(Properties.OPEN)) {
				return currentState.get(Properties.OPEN) == targetState.get(Properties.OPEN) ? 0 : 1;
		}

		return 0; // 未知类型或不可交互类型
	}

}
