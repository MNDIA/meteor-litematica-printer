package com.kkllffaa.meteor_litematica_printer.Modules.Tools

import com.kkllffaa.meteor_litematica_printer.Addon
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
import net.minecraft.component.type.ToolComponent
import net.minecraft.enchantment.Enchantments
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.ShearsItem
import net.minecraft.registry.tag.BlockTags
import net.minecraft.registry.tag.ItemTags
import java.util.function.Predicate

class AutoTool :
    Module(Addon.TOOLS, "auto-tool-+", "Automatically switches to the most effective tool when performing an action.") {
    private val sgGeneral: SettingGroup = settings.getDefaultGroup()
    private val sgWhitelist: SettingGroup = settings.createGroup("Whitelist")

    //region General
    private val prefer: Setting<EnchantPreference?> = sgGeneral.add<EnchantPreference?>(
        EnumSetting.Builder<EnchantPreference?>()
            .name("prefer")
            .description("Either to prefer Silk Touch, Fortune, or none.")
            .defaultValue(EnchantPreference.SilkTouch)
            .build()
    )

    private val silkTouchForEnderChest: Setting<Boolean?> = sgGeneral.add<Boolean?>(
        BoolSetting.Builder()
            .name("silk-touch-for-ender-chest")
            .description("Mines Ender Chests only with the Silk Touch enchantment.")
            .defaultValue(true)
            .build()
    )

    private val fortuneForOresCrops: Setting<Boolean?> = sgGeneral.add<Boolean?>(
        BoolSetting.Builder()
            .name("fortune-for-ores-and-crops")
            .description("Mines Ores and crops only with the Fortune enchantment.")
            .defaultValue(false)
            .build()
    )

    private val antiBreak: Setting<Boolean?> = sgGeneral.add<Boolean?>(
        BoolSetting.Builder()
            .name("anti-break")
            .description("Stops you from breaking your tool.")
            .defaultValue(true)
            .build()
    )

    private val breakDurability: Setting<Int?> = sgGeneral.add<Int?>(
        IntSetting.Builder()
            .name("anti-break-percentage")
            .description("The durability percentage to stop using a tool.")
            .defaultValue(9)
            .range(1, 100)
            .sliderRange(1, 100)
            .visible(IVisible { antiBreak.get()!! })
            .build()
    )

    private val switchBackDelay: Setting<Int?> = sgGeneral.add<Int?>(
        (IntSetting.Builder()
            .name("switch-back-delay")
            .description("Delay in ticks for switching tools back.")
            .defaultValue(15)
            .range(0, 100)
            .build()
                )
    )

    private val useSlot: Setting<Int?> = sgGeneral.add<Int?>(
        IntSetting.Builder()
            .name("use-slot")
            .description("the only one static hotbar slot to use.")
            .defaultValue(2)
            .range(1, 9)
            .sliderRange(1, 9)
            .build()
    )

    //endregion
    //region Whitelist and blacklist
    private val listMode: Setting<ListMode?> = sgWhitelist.add<ListMode?>(
        EnumSetting.Builder<ListMode?>()
            .name("list-mode")
            .description("Selection mode.")
            .defaultValue(ListMode.Blacklist)
            .build()
    )

    private val whitelist: Setting<MutableList<Item?>?> = sgWhitelist.add<MutableList<Item?>?>(
        ItemListSetting.Builder()
            .name("whitelist")
            .description("The tools you want to use.")
            .visible(IVisible { listMode.get() == ListMode.Whitelist })
            .filter(Predicate { item: Item? -> Companion.isTool(item!!) })
            .build()
    )

    private val blacklist: Setting<MutableList<Item?>?> = sgWhitelist.add<MutableList<Item?>?>(
        ItemListSetting.Builder()
            .name("blacklist")
            .description("The tools you don't want to use.")
            .visible(IVisible { listMode.get() == ListMode.Blacklist })
            .filter(Predicate { item: Item? -> Companion.isTool(item!!) })
            .build()
    )

    private var useSlotIndex = useSlot.get()!! - 1

    override fun onActivate() {
        resolveModuleConflict()
    }

    private fun resolveModuleConflict() {
        val meteorAutoTool: Module? = Modules.get().get<AutoTool?>(AutoTool::class.java)
        if (meteorAutoTool != null && meteorAutoTool.isActive()) {
            meteorAutoTool.toggle()
        }
    }

    @EventHandler
    private fun onTick(event: TickEvent.Post?) {
        if (Modules.get().isActive(InfinityMiner::class.java)) return

        if (mc.interactionManager != null && mc.interactionManager!!.isBreakingBlock()) {
            busyTick = switchBackDelay.get()!!
        }
        if (busyTick == 0) {
            if (useSlotIndex == mc.player!!.getInventory().getSelectedSlot()) {
                InvUtils.swapBack()
            } else {
                InvUtils.previousSlot = -1
            }
            busyTick = -1
        }
        if (busyTick > 0) {
            busyTick--
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    private fun onStartBreakingBlock(event: StartBreakingBlockEvent) {
        if (Modules.get().isActive(InfinityMiner::class.java)) return
        if (mc.player!!.isCreative()) return

        val blockState = mc.world!!.getBlockState(event.blockPos)
        if (!BlockUtils.canBreak(event.blockPos, blockState)) return

        // Check if we should switch to a better tool
        var bestScore = -1.0
        var bestSlot = -1

        for (i in 0..35) {
            val itemStack = mc.player!!.getInventory().getStack(i)

            if (listMode.get() == ListMode.Whitelist && !whitelist.get()!!.contains(itemStack.getItem())) continue
            if (listMode.get() == ListMode.Blacklist && blacklist.get()!!.contains(itemStack.getItem())) continue

            val score: Double = Companion.getScore(
                itemStack,
                blockState,
                silkTouchForEnderChest.get()!!,
                fortuneForOresCrops.get()!!,
                prefer.get(),
                Predicate { itemStack2: ItemStack? -> !shouldStopUsing(itemStack2!!) })

            if (score > bestScore) {
                bestScore = score
                bestSlot = i
            }
        }
        if (bestSlot == -1) {
            val cursorStack = mc.player!!.currentScreenHandler.getCursorStack()
            if (!cursorStack.isEmpty() && !(listMode.get() == ListMode.Whitelist && !whitelist.get()!!
                    .contains(cursorStack.getItem())) && !(listMode.get() == ListMode.Blacklist && blacklist.get()!!
                    .contains(cursorStack.getItem()))
            ) {
                val score: Double = Companion.getScore(
                    cursorStack,
                    blockState,
                    silkTouchForEnderChest.get()!!,
                    fortuneForOresCrops.get()!!,
                    prefer.get(),
                    Predicate { itemStack2: ItemStack? -> !shouldStopUsing(itemStack2!!) })
                if (score > bestScore) {
                    bestScore = score
                    bestSlot = -2
                }
            }
        }

        if (bestSlot != -1 && bestScore > Companion.getScore(
                mc.player!!.getMainHandStack(),
                blockState,
                silkTouchForEnderChest.get()!!,
                fortuneForOresCrops.get()!!,
                prefer.get(),
                Predicate { itemStack: ItemStack? -> !shouldStopUsing(itemStack!!) })
        ) {
            if (bestSlot != mc.player!!.getInventory().getSelectedSlot()) {
                if (SlotUtils.isHotbar(bestSlot)) {
                    useSlotIndex = bestSlot
                } else {
                    useSlotIndex = useSlot.get()!! - 1
                    if (bestSlot == -2) {
                        InvUtils.click().slot(useSlotIndex)
                        if (!mc.player!!.currentScreenHandler.getCursorStack().isEmpty()) {
                            val emptySlot = InvUtils.findEmpty()
                            if (emptySlot.found()) {
                                InvUtils.click().slot(emptySlot.slot())
                            } else {
                                warning("No empty slot found")
                            }
                        }
                    } else {
                        InvUtils.move().fromHotbar(bestSlot).to(useSlotIndex)
                        if (!mc.player!!.currentScreenHandler.getCursorStack().isEmpty()) {
                            val emptySlot = InvUtils.findEmpty()
                            if (emptySlot.found()) {
                                InvUtils.click().slot(emptySlot.slot())
                            } else {
                                InvUtils.click().slot(bestSlot)
                                warning("No empty slot found, put back to original slot")
                            }
                        }
                    }
                }
                InvUtils.swap(useSlotIndex, true)
            }
        } else {
            //没有有耐久的工具
        }
        // Anti break
        val currentStack = mc.player!!.getMainHandStack()

        if (shouldStopUsing(currentStack) && isTool(currentStack)) {
            mc.options.attackKey.setPressed(false)
            event.cancel()
        } else {
            busyTick = switchBackDelay.get()!!
        }
    }

    private fun shouldStopUsing(itemStack: ItemStack): Boolean {
        return antiBreak.get() && (itemStack.getMaxDamage() - itemStack.getDamage()) < (itemStack.getMaxDamage() * breakDurability.get()!! / 100)
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

    companion object {
        //endregion
        var busyTick: Int = -1
        fun getScore(
            itemStack: ItemStack,
            state: BlockState,
            silkTouchEnderChest: Boolean,
            fortuneOre: Boolean,
            enchantPreference: EnchantPreference?,
            good: Predicate<ItemStack?>
        ): Double {
            if (!good.test(itemStack) || !isTool(itemStack)) return -1.0
            if (!itemStack.isSuitableFor(state) && !(itemStack.isIn(ItemTags.SWORDS) && (state.getBlock() is BambooBlock || state.getBlock() is BambooShootBlock)) && !(itemStack.getItem() is ShearsItem && state.getBlock() is LeavesBlock || state.isIn(
                    BlockTags.WOOL
                ))
            ) return -1.0

            if (silkTouchEnderChest
                && state.getBlock() === Blocks.ENDER_CHEST && !Utils.hasEnchantments(itemStack, Enchantments.SILK_TOUCH)
            ) {
                return -1.0
            }

            if (fortuneOre
                && isFortunable(state.getBlock())
                && !Utils.hasEnchantments(itemStack, Enchantments.FORTUNE)
            ) {
                return -1.0
            }

            var score = 0.0

            score += (itemStack.getMiningSpeedMultiplier(state) * 1000).toDouble()
            score += Utils.getEnchantmentLevel(itemStack, Enchantments.UNBREAKING).toDouble()
            score += Utils.getEnchantmentLevel(itemStack, Enchantments.EFFICIENCY).toDouble()
            score += Utils.getEnchantmentLevel(itemStack, Enchantments.MENDING).toDouble()

            if (enchantPreference == EnchantPreference.Fortune) score += Utils.getEnchantmentLevel(
                itemStack,
                Enchantments.FORTUNE
            ).toDouble()
            if (enchantPreference == EnchantPreference.SilkTouch) score += Utils.getEnchantmentLevel(
                itemStack,
                Enchantments.SILK_TOUCH
            ).toDouble()

            if (itemStack.isIn(ItemTags.SWORDS) && (state.getBlock() is BambooBlock || state.getBlock() is BambooShootBlock)) score += (9000 + (itemStack.get<ToolComponent?>(
                DataComponentTypes.TOOL
            )!!.getSpeed(state) * 1000)).toDouble()

            return score
        }

        fun isTool(item: Item): Boolean {
            return isTool(item.getDefaultStack())
        }

        fun isTool(itemStack: ItemStack): Boolean {
            return itemStack.isIn(ItemTags.AXES) || itemStack.isIn(ItemTags.HOES) || itemStack.isIn(ItemTags.PICKAXES) || itemStack.isIn(
                ItemTags.SHOVELS
            ) || itemStack.getItem() is ShearsItem
        }

        private fun isFortunable(block: Block?): Boolean {
            if (block === Blocks.ANCIENT_DEBRIS) return false
            return Xray.ORES.contains(block) || block is CropBlock
        }
    }
}
