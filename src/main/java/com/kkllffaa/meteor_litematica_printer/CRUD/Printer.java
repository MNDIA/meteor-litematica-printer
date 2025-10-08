package com.kkllffaa.meteor_litematica_printer.CRUD;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import com.kkllffaa.meteor_litematica_printer.Addon;
import com.kkllffaa.meteor_litematica_printer.Functions.MyUtils;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.BlockListSetting;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockIterator;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;


public class Printer extends Module {
	private final SettingGroup sgGeneral = settings.getDefaultGroup();
	private final SettingGroup sgWhitelist = settings.createGroup("Whitelist");
    private final SettingGroup sgRendering = settings.createGroup("Rendering");

	private final SettingGroup sgCache = settings.createGroup("Cache");
	private final Setting<Integer> printing_range = sgGeneral.add(new IntSetting.Builder()
			.name("printing-range")
			.description("The block place range.")
			.defaultValue(2)
			.min(1).sliderMin(1)
			.max(6).sliderMax(6)
			.build()
	);

	private final Setting<Integer> printing_delay = sgGeneral.add(new IntSetting.Builder()
			.name("printing-delay")
			.description("Delay between printing blocks in ticks.")
			.defaultValue(2)
			.min(0).sliderMin(0)
			.max(100).sliderMax(40)
			.build()
	);

	private final Setting<Integer> bpt = sgGeneral.add(new IntSetting.Builder()
			.name("blocks/tick")
			.description("How many blocks place per tick.")
			.defaultValue(1)
			.min(1).sliderMin(1)
			.max(100).sliderMax(100)
			.build()
	);



    private final Setting<SortAlgorithm> firstAlgorithm = sgGeneral.add(new EnumSetting.Builder<SortAlgorithm>()
			.name("first-sorting-mode")
			.description("The blocks you want to place first.")
			.defaultValue(SortAlgorithm.None)
			.build()
	);

    private final Setting<SortingSecond> secondAlgorithm = sgGeneral.add(new EnumSetting.Builder<SortingSecond>()
			.name("second-sorting-mode")
			.description("Second pass of sorting eg. place first blocks higher and closest to you.")
			.defaultValue(SortingSecond.None)
			.visible(()-> firstAlgorithm.get().applySecondSorting)
			.build()
	);

    private final Setting<Boolean> whitelistenabled = sgWhitelist.add(new BoolSetting.Builder()
			.name("whitelist-enabled")
			.description("Only place selected blocks.")
			.defaultValue(false)
			.build()
	);

    private final Setting<List<Block>> whitelist = sgWhitelist.add(new BlockListSetting.Builder()
			.name("whitelist")
			.description("Blocks to place.")
			.visible(whitelistenabled::get)
			.build()
	);

    private final Setting<Boolean> renderBlocks = sgRendering.add(new BoolSetting.Builder()
        .name("render-placed-blocks")
        .description("Renders block placements.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> fadeTime = sgRendering.add(new IntSetting.Builder()
        .name("fade-time")
        .description("Time for the rendering to fade, in ticks.")
        .defaultValue(3)
        .min(1).sliderMin(1)
        .max(1000).sliderMax(20)
        .visible(renderBlocks::get)
        .build()
    );

    private final Setting<SettingColor> colour = sgRendering.add(new ColorSetting.Builder()
        .name("colour")
        .description("The cubes colour.")
        .defaultValue(new SettingColor(95, 190, 255))
        .visible(renderBlocks::get)
        .build()
    );






	private final Setting<Boolean> enableCache = sgCache.add(new BoolSetting.Builder()
			.name("enable-cache")
			.description("Enable position cache to prevent placing at the same position multiple times.")
			.defaultValue(true)
			.build());

	private final Setting<Integer> cacheSize = sgCache.add(new IntSetting.Builder()
			.name("cache-size")
			.description("Number of recent positions to cache.")
			.defaultValue(50)
			.min(10).sliderMin(10)
			.max(200).sliderMax(200)
			.visible(enableCache::get)
			.build());

	private final Setting<Integer> cacheCleanupInterval = sgCache.add(new IntSetting.Builder()
			.name("cache-cleanup-interval")
			.description("Time in seconds between cache cleanups to prevent stale entries.")
			.defaultValue(2)
			.min(1).sliderMin(1)
			.max(10).sliderMax(10)
			.visible(enableCache::get)
			.build());








    private int timer;
    private final List<BlockPos> toSort = new ArrayList<>();
    private final List<Pair<Integer, BlockPos>> placed_fade = new ArrayList<>();

    // Position cache to prevent repeated placement attempts
    private final LinkedHashSet<BlockPos> positionCache = new LinkedHashSet<>();
    private int cacheCleanupTickTimer = 0;

    // Pending interactions: position -> remaining interactions needed
    private final Map<BlockPos, Integer> pendingInteractions = new HashMap<>();

	public Printer() {
		super(Addon.CRUDCATEGORY, "litematica-printer", "Automatically prints open schematics");
	}
	private boolean isPositionCached(BlockPos pos) {
		return enableCache.get() && positionCache.contains(pos);
	}
	private void addToCache(BlockPos pos) {
		if (!enableCache.get()) return;
		
		positionCache.add(pos);
		
		// Remove oldest entries if cache exceeds limit
		while (positionCache.size() > cacheSize.get()) {
			var iterator = positionCache.iterator();
			if (iterator.hasNext()) {
				iterator.next();
				iterator.remove();
			}
		}
	}
    @Override
    public void onActivate() {
        onDeactivate();
    }

	@Override
    public void onDeactivate() {
		placed_fade.clear();
		positionCache.clear();
		cacheCleanupTickTimer = 0;
		pendingInteractions.clear();
	}

	@EventHandler
	private void onTick(TickEvent.Post event) {
		if (mc.player == null || mc.world == null) {
			placed_fade.clear();
			return;
		}

		placed_fade.forEach(s -> s.setLeft(s.getLeft() - 1));
		placed_fade.removeIf(s -> s.getLeft() <= 0);

		// Cache cleanup timer - clears cache periodically to prevent stale entries
		if (enableCache.get()) {
			cacheCleanupTickTimer++;

			if (cacheCleanupTickTimer >= cacheCleanupInterval.get() * 20) {
				positionCache.clear();
				cacheCleanupTickTimer = 0;
			}
		}
		WorldSchematic worldSchematic = SchematicWorldHandler.getSchematicWorld();
		if (worldSchematic == null) {
			placed_fade.clear();
			toggle();
			return;
		}

		
		toSort.clear();


		if (timer >= printing_delay.get()) {
			BlockIterator.register(printing_range.get() + 1, printing_range.get() + 1, (pos, blockState) -> {
				BlockState required = worldSchematic.getBlockState(pos);
				if (MyUtils.InteractSettingsModule.enableInteraction.get() && !pendingInteractions.containsKey(pos)) {
					int requiredInteractions = MyUtils.InteractSettingsModule.calculateRequiredInteractions(required,
							pos);
					if (requiredInteractions > 0) {
						pendingInteractions.put(new BlockPos(pos), requiredInteractions);
					}
				}
				if (mc.player.getBlockPos().isWithinDistance(pos, printing_range.get())
						&& blockState.isReplaceable()
						&& required.getFluidState().isEmpty()
						&& !required.isAir()
						&& blockState.getBlock() != required.getBlock()
						&& DataManager.getRenderLayerRange().isPositionWithinRange(pos)
						&& !mc.player.getBoundingBox().intersects(Vec3d.of(pos), Vec3d.of(pos).add(1, 1, 1))
						&& required.canPlaceAt(mc.world, pos)
						&& !(isPositionCached(pos))) {
					if (!whitelistenabled.get() || whitelist.get().contains(required.getBlock())) {
						toSort.add(new BlockPos(pos));
					}
				}
			});

			BlockIterator.after(() -> {

				if (firstAlgorithm.get() != SortAlgorithm.None) {
					if (firstAlgorithm.get().applySecondSorting) {
						if (secondAlgorithm.get() != SortingSecond.None) {
							toSort.sort(secondAlgorithm.get().algorithm);
						}
					}
					toSort.sort(firstAlgorithm.get().algorithm);
				}


				int placed = 0;
				
				Iterator<Map.Entry<BlockPos, Integer>> iterator = pendingInteractions.entrySet().iterator();
				while (iterator.hasNext() && placed < bpt.get()) {
					Map.Entry<BlockPos, Integer> entry = iterator.next();
					BlockPos pos = entry.getKey();
					int remaining = entry.getValue();
					if (remaining > 0) {
						int toDo = Math.min(remaining, bpt.get() - placed);
						int did = MyUtils.InteractSettingsModule.interactWithBlock(pos, toDo);
						if (did > 0){
							timer = 0;
						}
						placed += did;
						int newRemaining = remaining - did;
						if (newRemaining <= 0) {
							iterator.remove();
						}else{
							entry.setValue(newRemaining);
						}
					} else {
						iterator.remove();
					}
				}

				for (BlockPos pos : toSort) {
					BlockState state = worldSchematic.getBlockState(pos);

					if (MyUtils.placeBlock(state, pos)) {
						timer = 0;
						placed++;
						addToCache(pos);
						
						if (renderBlocks.get()) {
							placed_fade.add(new Pair<>(fadeTime.get(), new BlockPos(pos)));
						}
						if (placed >= bpt.get()) {
							return;
						}
					}
				}
			});


		} else timer++;
	}

	@EventHandler
	private void onRender(Render3DEvent event) {
		placed_fade.forEach(s -> {
			Color a = new Color(colour.get().r, colour.get().g, colour.get().b, (int) (((float)s.getLeft() / (float) fadeTime.get()) * colour.get().a));
			event.renderer.box(s.getRight(), a, null, ShapeMode.Sides, 0);
		});
	}

	public enum SortAlgorithm {
		None(false, (a, b) -> 0),
		TopDown(true, Comparator.comparingInt(value -> value.getY() * -1)),
		DownTop(true, Comparator.comparingInt(Vec3i::getY)),
		Nearest(false, Comparator.comparingDouble(value -> MeteorClient.mc.player != null ? Utils.squaredDistance(MeteorClient.mc.player.getX(), MeteorClient.mc.player.getY(), MeteorClient.mc.player.getZ(), value.getX() + 0.5, value.getY() + 0.5, value.getZ() + 0.5) : 0)),
		Furthest(false, Comparator.comparingDouble(value -> MeteorClient.mc.player != null ? (Utils.squaredDistance(MeteorClient.mc.player.getX(), MeteorClient.mc.player.getY(), MeteorClient.mc.player.getZ(), value.getX() + 0.5, value.getY() + 0.5, value.getZ() + 0.5)) * -1 : 0));


		final boolean applySecondSorting;
		final Comparator<BlockPos> algorithm;

		SortAlgorithm(boolean applySecondSorting, Comparator<BlockPos> algorithm) {
			this.applySecondSorting = applySecondSorting;
			this.algorithm = algorithm;
		}
	}

	public enum SortingSecond {
		None(SortAlgorithm.None.algorithm),
		Nearest(SortAlgorithm.Nearest.algorithm),
		Furthest(SortAlgorithm.Furthest.algorithm);

		final Comparator<BlockPos> algorithm;

		SortingSecond(Comparator<BlockPos> algorithm) {
			this.algorithm = algorithm;
		}
	}
}
