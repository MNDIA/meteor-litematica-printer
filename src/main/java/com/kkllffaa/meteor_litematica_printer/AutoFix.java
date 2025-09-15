package com.kkllffaa.meteor_litematica_printer;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;

public class AutoFix extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> Durability = sgGeneral.add(new DoubleSetting.Builder()
        .name("percentage")
        .description("The durability percentage.")
        .defaultValue(99.9)
        .range(1, 100)
        .sliderRange(1, 100)
        .build()
    );

    public AutoFix () {
        super(Addon.CATEGORY, "auto-fix", "把背包中和物品栏中(排除装备的盔甲栏)需要修复的物品切换到副手");
    }


    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!InvUtils.testInOffHand(itemStack -> !needFix(itemStack))) {
            FindItemResult result = InvUtils.find(itemStack -> needFix(itemStack), 0, 35);
            if (result.found()) {
                InvUtils.move().from(result.slot()).toOffhand();
                if (!mc.player.currentScreenHandler.getCursorStack().isEmpty()) {
                    FindItemResult emptySlot = InvUtils.findEmpty();
                    if (emptySlot.found()) {
                        InvUtils.click().slot(emptySlot.slot());
                    } else {
                    }
                }
            }
        }
    }


    private boolean needFix (ItemStack itemStack){
        return Utils.hasEnchantment(itemStack, Enchantments.MENDING) && (itemStack.getMaxDamage() - itemStack.getDamage()) < (itemStack.getMaxDamage() * Durability.get() / 100D);
    }
 
}
