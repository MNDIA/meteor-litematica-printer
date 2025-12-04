package com.kkllffaa.meteor_litematica_printer.Modules.Tools

import com.kkllffaa.meteor_litematica_printer.Addon
import com.kkllffaa.meteor_litematica_printer.Functions.switchTo
import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent
import meteordevelopment.meteorclient.events.world.TickEvent
import meteordevelopment.meteorclient.settings.*
import meteordevelopment.meteorclient.systems.modules.Module
import meteordevelopment.meteorclient.systems.modules.Modules
import meteordevelopment.meteorclient.systems.modules.player.AutoTool
import meteordevelopment.meteorclient.systems.modules.render.Xray
import meteordevelopment.meteorclient.systems.modules.world.InfinityMiner
import meteordevelopment.meteorclient.utils.Utils
import meteordevelopment.meteorclient.utils.player.InvUtils
import meteordevelopment.meteorclient.utils.player.SlotUtils
import meteordevelopment.meteorclient.utils.world.BlockUtils
import meteordevelopment.orbit.EventHandler
import meteordevelopment.orbit.EventPriority
import net.minecraft.block.*
import net.minecraft.component.DataComponentTypes
import net.minecraft.enchantment.Enchantments
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.ShearsItem
import net.minecraft.registry.tag.BlockTags
import net.minecraft.registry.tag.ItemTags
import java.util.function.Predicate

object AutoTool :
    Module(Addon.TOOLS, "auto-tool-+", "Automatically switches to the most effective tool when performing an action.") {
    private val sgGeneral = settings.defaultGroup
    private val sgWhitelist = settings.createGroup("Whitelist")

    //region General
    private val prefer: Setting<EnchantPreference> = sgGeneral.add(
        EnumSetting.Builder<EnchantPreference>()
            .name("prefer")
            .description("Either to prefer Silk Touch, Fortune, or none.")
            .defaultValue(EnchantPreference.SilkTouch)
            .build()
    )

    private val silkTouchForEnderChest: Setting<Boolean> = sgGeneral.add(
        BoolSetting.Builder()
            .name("silk-touch-for-ender-chest")
            .description("Mines Ender Chests only with the Silk Touch enchantment.")
            .defaultValue(true)
            .build()
    )

    private val fortuneForOresCrops: Setting<Boolean> = sgGeneral.add(
        BoolSetting.Builder()
            .name("fortune-for-ores-and-crops")
            .description("Mines Ores and crops only with the Fortune enchantment.")
            .defaultValue(false)
            .build()
    )

    private val antiBreak: Setting<Boolean> = sgGeneral.add(
        BoolSetting.Builder()
            .name("anti-break")
            .description("Stops you from breaking your tool.")
            .defaultValue(true)
            .build()
    )

    private val breakDurability: Setting<Int> = sgGeneral.add(
        IntSetting.Builder()
            .name("anti-break-percentage")
            .description("The durability percentage to stop using a tool.")
            .defaultValue(9)
            .range(1, 100)
            .sliderRange(1, 100)
            .visible { antiBreak.get() }
            .build()
    )

    //endregion
    //region Whitelist and blacklist
    private val listMode: Setting<ListMode> = sgWhitelist.add(
        EnumSetting.Builder<ListMode>()
            .name("list-mode")
            .description("Selection mode.")
            .defaultValue(ListMode.Blacklist)
            .build()
    )

    private val whitelist: Setting<MutableList<Item>> = sgWhitelist.add(
        ItemListSetting.Builder()
            .name("whitelist")
            .description("The tools you want to use.")
            .visible { listMode.get() == ListMode.Whitelist }
            .filter { item: Item -> isTool(item) }
            .build()
    )

    private val blacklist: Setting<MutableList<Item>> = sgWhitelist.add(
        ItemListSetting.Builder()
            .name("blacklist")
            .description("The tools you don't want to use.")
            .visible { listMode.get() == ListMode.Blacklist }
            .filter { item: Item -> isTool(item) }
            .build()
    )

    override fun onActivate() {
        resolveModuleConflict()
    }

    private fun resolveModuleConflict() {
        val meteorAutoTool = Modules.get().get(AutoTool::class.java)
        if (meteorAutoTool != null && meteorAutoTool.isActive) {
            meteorAutoTool.toggle()
        }
    }

    @EventHandler
    private fun onTick(event: TickEvent.Post) {
        if (Modules.get().isActive(InfinityMiner::class.java)) return
        val interactionManager = mc.interactionManager ?: return
        val player = mc.player ?: return
        if (interactionManager.isBreakingBlock) {
            player.switchTo(player.inventory.selectedSlot)
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    private fun onStartBreakingBlock(event: StartBreakingBlockEvent) {
        if (Modules.get().isActive(InfinityMiner::class.java)) return
        val player = mc.player ?: return
        if (player.isCreative) return

        val blockState = mc.world?.getBlockState(event.blockPos) ?: return
        if (!BlockUtils.canBreak(event.blockPos, blockState)) return

        // Check if we should switch to a better tool
        var bestScore = -1.0
        var bestSlot = -1

        for (i in 0..35) {
            val itemStack = player.getInventory().getStack(i)

            if (listMode.get() == ListMode.Whitelist && !whitelist.get().contains(itemStack.item)) continue
            if (listMode.get() == ListMode.Blacklist && blacklist.get().contains(itemStack.item)) continue

            val score: Double = getScore(
                itemStack,
                blockState,
                silkTouchForEnderChest.get(),
                fortuneForOresCrops.get(),
                prefer.get()
            ) { itemStack2: ItemStack -> !shouldStopUsing(itemStack2) }

            if (score > bestScore) {
                bestScore = score
                bestSlot = i
            }
        }
        if (bestSlot == -1) {
            val cursorStack = player.currentScreenHandler.cursorStack
            if (!cursorStack.isEmpty && !(listMode.get() == ListMode.Whitelist && cursorStack.item !in whitelist.get()) && !(listMode.get() == ListMode.Blacklist && cursorStack.item in blacklist.get())
            ) {
                val score: Double = getScore(
                    cursorStack,
                    blockState,
                    silkTouchForEnderChest.get(),
                    fortuneForOresCrops.get(),
                    prefer.get()
                ) { itemStack2: ItemStack -> !shouldStopUsing(itemStack2) }
                if (score > bestScore) {
                    bestScore = score
                    bestSlot = -2
                }
            }
        }

        if (bestSlot != -1 && bestScore > getScore(
                player.mainHandStack,
                blockState,
                silkTouchForEnderChest.get(),
                fortuneForOresCrops.get(),
                prefer.get()
            ) { itemStack: ItemStack -> !shouldStopUsing(itemStack) }
        ) {
            if (bestSlot == -2) {
                InvUtils.click().slot(8)
                if (!player.currentScreenHandler.cursorStack.isEmpty) {
                    val emptySlot = InvUtils.findEmpty()
                    if (emptySlot.found()) {
                        InvUtils.click().slot(emptySlot.slot())
                    } else {
                        warning("No empty slot found")
                    }
                }
                player.switchTo(8)
            } else {
                player.switchTo(bestSlot)
            }

        } else {
            //没有有耐久的工具
        }
        // Anti break
        val currentStack = player.mainHandStack

        if (shouldStopUsing(currentStack) && isTool(currentStack)) {
            mc.options.attackKey.isPressed = false
            event.cancel()
        } else {
            player.switchTo(player.inventory.selectedSlot)
        }
    }

    private fun shouldStopUsing(itemStack: ItemStack): Boolean {
        return antiBreak.get() && (itemStack.maxDamage - itemStack.damage) < (itemStack.maxDamage * breakDurability.get() / 100)
    }

    enum class EnchantPreference {
        None,
        Fortune,
        SilkTouch
    }

    enum class ListMode {
        Whitelist,
        Blacklist
    }

    //endregion
    var busyTick: Int = -1
    fun getScore(
        itemStack: ItemStack,
        state: BlockState,
        silkTouchEnderChest: Boolean,
        fortuneOre: Boolean,
        enchantPreference: EnchantPreference,
        good: Predicate<ItemStack>
    ): Double {
        if (!good.test(itemStack) || !isTool(itemStack)) return -1.0
        if (!itemStack.isSuitableFor(state) && !(itemStack.isIn(ItemTags.SWORDS) && (state.block is BambooBlock || state.block is BambooShootBlock)) && !(itemStack.item is ShearsItem && state.block is LeavesBlock || state.isIn(
                BlockTags.WOOL
            ))
        ) return -1.0

        if (silkTouchEnderChest
            && state.block === Blocks.ENDER_CHEST && !Utils.hasEnchantments(itemStack, Enchantments.SILK_TOUCH)
        ) {
            return -1.0
        }

        if (fortuneOre
            && isFortunable(state.block)
            && !Utils.hasEnchantments(itemStack, Enchantments.FORTUNE)
        ) {
            return -1.0
        }

        var score = 0.0

        score += (itemStack.getMiningSpeedMultiplier(state) * 1000).toDouble()
        score += Utils.getEnchantmentLevel(itemStack, Enchantments.UNBREAKING).toDouble()
        score += Utils.getEnchantmentLevel(itemStack, Enchantments.EFFICIENCY).toDouble()
        score += Utils.getEnchantmentLevel(itemStack, Enchantments.MENDING).toDouble()

        if (enchantPreference == EnchantPreference.Fortune)
            score += Utils.getEnchantmentLevel(itemStack, Enchantments.FORTUNE).toDouble()
        if (enchantPreference == EnchantPreference.SilkTouch)
            score += Utils.getEnchantmentLevel(itemStack, Enchantments.SILK_TOUCH).toDouble()

        if (itemStack.isIn(ItemTags.SWORDS) && (state.block is BambooBlock || state.block is BambooShootBlock))
            score += (9000 +
                    ((itemStack.get(DataComponentTypes.TOOL)?.getSpeed(state) ?: 0f) * 1000)
                    ).toDouble()

        return score
    }

    fun isTool(item: Item): Boolean {
        return isTool(item.defaultStack)
    }

    fun isTool(itemStack: ItemStack): Boolean {
        return itemStack.isIn(ItemTags.AXES) || itemStack.isIn(ItemTags.HOES) || itemStack.isIn(ItemTags.PICKAXES) || itemStack.isIn(
            ItemTags.SHOVELS
        ) || itemStack.item is ShearsItem
    }

    private fun isFortunable(block: Block): Boolean {
        if (block === Blocks.ANCIENT_DEBRIS) return false
        return Xray.ORES.contains(block) || block is CropBlock
    }

}
