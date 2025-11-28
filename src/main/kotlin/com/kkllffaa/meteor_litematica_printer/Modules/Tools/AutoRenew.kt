package com.kkllffaa.meteor_litematica_printer.Modules.Tools

import com.kkllffaa.meteor_litematica_printer.Addon
import meteordevelopment.meteorclient.events.world.TickEvent
import meteordevelopment.meteorclient.settings.DoubleSetting
import meteordevelopment.meteorclient.settings.ItemListSetting
import meteordevelopment.meteorclient.settings.Setting
import meteordevelopment.meteorclient.settings.SettingGroup
import meteordevelopment.meteorclient.systems.modules.Module
import meteordevelopment.meteorclient.utils.player.InvUtils
import meteordevelopment.orbit.EventHandler
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items

class AutoRenew : Module(Addon.TOOLS, "auto-renew", "手持工具耐久度低于阈值时，从背包内替换同类物品") {
    private val sgGeneral = settings.defaultGroup

    private val Durability: Setting<Double> = sgGeneral.add(
        DoubleSetting.Builder()
            .name("percentage")
            .description("The durability percentage.")
            .defaultValue(9.5)
            .range(1.0, 100.0)
            .sliderRange(1.0, 100.0)
            .build()
    )

    private val blacklist: Setting<MutableList<Item>> = sgGeneral.add(
        ItemListSetting.Builder()
            .name("blacklist")
            .description("Items that should not be auto-renew in hand.")
            .defaultValue(
                Items.LEATHER_HELMET,
                Items.CHAINMAIL_HELMET,
                Items.IRON_HELMET,
                Items.GOLDEN_HELMET,
                Items.DIAMOND_HELMET,
                Items.NETHERITE_HELMET,
                Items.TURTLE_HELMET,
                Items.LEATHER_CHESTPLATE,
                Items.CHAINMAIL_CHESTPLATE,
                Items.IRON_CHESTPLATE,
                Items.GOLDEN_CHESTPLATE,
                Items.DIAMOND_CHESTPLATE,
                Items.NETHERITE_CHESTPLATE,
                Items.LEATHER_LEGGINGS,
                Items.CHAINMAIL_LEGGINGS,
                Items.IRON_LEGGINGS,
                Items.GOLDEN_LEGGINGS,
                Items.DIAMOND_LEGGINGS,
                Items.NETHERITE_LEGGINGS,
                Items.LEATHER_BOOTS,
                Items.CHAINMAIL_BOOTS,
                Items.IRON_BOOTS,
                Items.GOLDEN_BOOTS,
                Items.DIAMOND_BOOTS,
                Items.NETHERITE_BOOTS,
                Items.ELYTRA
            )
            .build()
    )

    @EventHandler
    private fun onTick(event: TickEvent.Post) {
        val player = mc.player ?: return
        val mainHandStack = player.mainHandStack

        if (!mainHandStack.isEmpty && needRew(mainHandStack)) {
            val bestSlot = findBestReplacement(mainHandStack)

            if (bestSlot != -1) {
                val selectedSlot = player.getInventory().selectedSlot

                InvUtils.move().from(bestSlot).toHotbar(selectedSlot)

                if (!player.currentScreenHandler.cursorStack.isEmpty) {
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
    }

    // 查找背包中同类物品，耐久最高的槽位
    private fun findBestReplacement(referenceStack: ItemStack): Int {
        var bestSlot = -1
        var bestDurability = -1

        for (i in 0..35) {
            val stack = mc.player!!.getInventory().getStack(i)
            if (!stack.isEmpty && stack.item === referenceStack.item && !needRew(stack)) { // 同类物品
                val durability = stack.maxDamage - stack.damage
                if (durability > bestDurability) {
                    bestDurability = durability
                    bestSlot = i
                }
            }
        }
        return bestSlot
    }

    private fun needRew(itemStack: ItemStack): Boolean {
        return (itemStack.maxDamage - itemStack.damage) < (itemStack.maxDamage * Durability.get() / 100.0)
                && !blacklist.get().contains(itemStack.item)
    }
}
