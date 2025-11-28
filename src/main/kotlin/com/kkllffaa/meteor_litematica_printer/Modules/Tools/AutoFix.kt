package com.kkllffaa.meteor_litematica_printer.Modules.Tools

import com.kkllffaa.meteor_litematica_printer.Addon
import meteordevelopment.meteorclient.events.world.TickEvent
import meteordevelopment.meteorclient.settings.DoubleSetting
import meteordevelopment.meteorclient.settings.ItemListSetting
import meteordevelopment.meteorclient.settings.Setting
import meteordevelopment.meteorclient.systems.modules.Module
import meteordevelopment.meteorclient.utils.Utils
import meteordevelopment.meteorclient.utils.player.InvUtils
import meteordevelopment.orbit.EventHandler
import net.minecraft.enchantment.Enchantments
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items

class AutoFix : Module(Addon.TOOLS, "auto-fix", "把背包中和物品栏中(排除装备的盔甲栏)需要修复的物品切换到副手") {
    private val sgGeneral = settings.defaultGroup

    private val Durability: Setting<Double> = sgGeneral.add(
        DoubleSetting.Builder()
            .name("percentage")
            .description("The durability percentage.")
            .defaultValue(100.0)
            .range(1.0, 100.0)
            .sliderRange(1.0, 100.0)
            .build()
    )

    private val blacklist: Setting<MutableList<Item>> = sgGeneral.add(
        ItemListSetting.Builder()
            .name("blacklist")
            .description("Items that should not be auto-fixed.")
            .defaultValue(
                Items.NETHERITE_SWORD,
                Items.WOODEN_SWORD,
                Items.STONE_SWORD,
                Items.IRON_SWORD,
                Items.GOLDEN_SWORD,
                Items.DIAMOND_SWORD
            )
            .build()
    )

    @EventHandler
    private fun onTick(event: TickEvent.Post) {
        if (InvUtils.testInOffHand { itemStack: ItemStack -> !needFix(itemStack) }) {
            val result = InvUtils.find({ itemStack: ItemStack -> needFix(itemStack) }, 0, 35)
            if (result.found()) {
                InvUtils.move().from(result.slot()).toOffhand()
                if (mc.player?.currentScreenHandler?.cursorStack?.isEmpty == false) {
                    val emptySlot = InvUtils.findEmpty()
                    if (emptySlot.found()) {
                        InvUtils.click().slot(emptySlot.slot())
                    } else {
                        InvUtils.click().slot(result.slot())
                        warning("No empty slot found, put back to original slot")
                    }
                }
            }
        }
    }


    private fun needFix(itemStack: ItemStack): Boolean {
        return Utils.hasEnchantment(
            itemStack,
            Enchantments.MENDING
        ) && (itemStack.maxDamage - itemStack.damage) < (itemStack.maxDamage * Durability.get() / 100.0) && !blacklist.get()
            .contains(itemStack.item)
    }
}
