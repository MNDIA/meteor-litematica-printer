package com.kkllffaa.meteor_litematica_printer.settings;

import meteordevelopment.meteorclient.settings.BlockListSetting;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;

import net.minecraft.block.*;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import static com.kkllffaa.meteor_litematica_printer.Functions.MyUtils.*;

import java.util.List;

import com.kkllffaa.meteor_litematica_printer.Addon;
import com.kkllffaa.meteor_litematica_printer.Functions.BlockPosUtils;
import com.kkllffaa.meteor_litematica_printer.Functions.MyUtils.SafetyFaceMode;



public class InteractSettings extends Module {
	public InteractSettings() {
		super(Addon.SETTINGSCATEGORY, "InteractSettings", "Module to configure settings.");
	}

	private final SettingGroup sgGeneral = settings.getDefaultGroup();
	public final Setting<Boolean> enableInteraction = sgGeneral.add(new BoolSetting.Builder()
			.name("enable-interaction")
			.description("Enable interaction with blocks.")
			.defaultValue(true)
			.build());
            
	private final Setting<SafetyFaceMode> safetyInteractFaceMode = sgGeneral.add(new EnumSetting.Builder<SafetyFaceMode>()
			.name("safety-interact-face-mode")
			.description("Only interact with blocks on safe faces.")
			.defaultValue(SafetyFaceMode.PlayerPosition)
            .visible(enableInteraction::get)
			.build());
            
	private final Setting<Boolean> onlyInteractOnLook = sgGeneral.add(new BoolSetting.Builder()
			.name("only-interact-on-look")
			.description("Only interact with blocks on the face you are looking at.")
			.defaultValue(false)
            .visible(enableInteraction::get)
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
			.visible(enableInteraction::get)
            .build());

	private final Setting<Double> maxInteractionDistance = sgGeneral.add(new DoubleSetting.Builder()
			.name("max-interaction-distance")
			.description("Maximum distance to interact with blocks.")
			.defaultValue(5.5)
			.min(0.0)
			.max(10.0)
			.visible(enableInteraction::get)
			.build());








	public int interactWithBlock(BlockPos pos, int count) {
         if (!enableInteraction.get()) {
            return 0;
        }
        Direction face = switch (safetyInteractFaceMode.get()) {
            case SafetyFaceMode.None -> Direction.UP; // Default face when no safety is applied
            default -> getASafetyFaceOrNull(pos,safetyInteractFaceMode.get() );
        };
        if (face == null) {
            return 0;
        }

        if (onlyInteractOnLook.get() && !BlockPosUtils.isPlayerYawPitchInTheFaceOfBlock(pos, face)) {
            return 0;
        }
		
        ClientPlayerEntity player = mc.player;
        ClientPlayerInteractionManager interactionManager = mc.interactionManager;
        if (player == null || interactionManager == null) return 0;
		if (player.isSneaking()){
			return 0;
		}
        if (getPlayerEye(player).distanceTo(Vec3d.ofCenter(pos)) > maxInteractionDistance.get()) {
            return 0;
        }

        Vec3d hitPos = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        for (int i = 0; i < count; i++) {
		    BlockHitResult blockHitResult = new BlockHitResult(hitPos, face, pos, false);
		    ActionResult result = interactionManager.interactBlock(player, Hand.MAIN_HAND, blockHitResult);
		    if (!result.isAccepted()) {
                warning("Interaction not accepted at " + pos + ", result: " + result);
                return i;
            }
        }
        return count;
	}
	public int calculateRequiredInteractions(BlockState targetState, BlockPos pos) {
        ClientWorld world = mc.world;
		if (world == null)
			return 0;

		BlockState currentState = world.getBlockState(pos);
        Block currentblock = currentState.getBlock();
        if (currentblock != targetState.getBlock()) {
            return 0;
        }
		if (!stateBlocks.get().contains(currentblock)) {
			return 0;
		}

		// For note blocks: calculate note difference
		if (currentblock instanceof NoteBlock) {
				int currentNote = currentState.get(Properties.NOTE);
				int targetNote = targetState.get(Properties.NOTE);

				// Note blocks cycle through 0-24 (25 states)
				int diff = (targetNote - currentNote + 25) % 25;
				return diff;
		}

		// For repeaters: calculate delay difference
		if (currentblock instanceof RepeaterBlock) {
				int currentDelay = currentState.get(Properties.DELAY);
				int targetDelay = targetState.get(Properties.DELAY);

				// Repeaters cycle through 1-4 (4 states)
				int diff = (targetDelay - currentDelay + 4) % 4;
				return diff;
		}

		// For comparators: calculate mode difference
		if (currentblock instanceof ComparatorBlock) {
			return currentState.get(Properties.COMPARATOR_MODE) == targetState.get(Properties.COMPARATOR_MODE) ? 0
						: 1;
		}

		// For daylight detectors: check inverted state
		if (currentblock instanceof DaylightDetectorBlock) {
			return currentState.get(Properties.INVERTED) == targetState.get(Properties.INVERTED) ? 0 : 1;
			
		}

		// For levers: check powered state
		if (currentblock instanceof LeverBlock) {
			return currentState.get(Properties.POWERED) == targetState.get(Properties.POWERED) ? 0 : 1;
		}

		// For fence gates: check open state
		// For trapdoors: check open state
		// For doors: check open state
		if (currentState.contains(Properties.OPEN)) {
				return currentState.get(Properties.OPEN) == targetState.get(Properties.OPEN) ? 0 : 1;
		}
		

		return 0; // 未知类型或不可交互类型
	}

	





































	
}
