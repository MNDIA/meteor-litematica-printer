package com.kkllffaa.meteor_litematica_printer.Modules.Tools;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import java.util.List;

import com.kkllffaa.meteor_litematica_printer.Addon;

public class AutoFix extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> Durability = sgGeneral.add(new DoubleSetting.Builder()
        .name("percentage")
        .description("The durability percentage.")
        .defaultValue(100)
        .range(1, 100)
        .sliderRange(1, 100)
        .build()
    );

    private final Setting<List<Item>> blacklist = sgGeneral.add(new ItemListSetting.Builder()
        .name("blacklist")
        .description("Items that should not be auto-fixed.")
        .defaultValue(Items.NETHERITE_SWORD,
        Items.WOODEN_SWORD,
        Items.STONE_SWORD,
        Items.IRON_SWORD,
        Items.GOLDEN_SWORD,
        Items.DIAMOND_SWORD
        )
        .build()
    );

    public AutoFix () {
        super(Addon.TOOLS, "auto-fix", "把背包中和物品栏中(排除装备的盔甲栏)需要修复的物品切换到副手");
    }


    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (InvUtils.testInOffHand(itemStack -> !needFix(itemStack))) {
            FindItemResult result = InvUtils.find(itemStack -> needFix(itemStack), 0, 35);
            if (result.found()) {
                InvUtils.move().from(result.slot()).toOffhand();
                if (!mc.player.currentScreenHandler.getCursorStack().isEmpty()) {
                    FindItemResult emptySlot = InvUtils.findEmpty();
                    if (emptySlot.found()) {
                        InvUtils.click().slot(emptySlot.slot());
                    } else {
                        InvUtils.click().slot(result.slot());
                        warning("No empty slot found, put back to original slot");
                    }
                }
            }
        }
    }


    private boolean needFix (ItemStack itemStack){
        return Utils.hasEnchantment(itemStack, Enchantments.MENDING) && (itemStack.getMaxDamage() - itemStack.getDamage()) < (itemStack.getMaxDamage() * Durability.get() / 100D) && !blacklist.get().contains(itemStack.getItem());
    }
 
}
