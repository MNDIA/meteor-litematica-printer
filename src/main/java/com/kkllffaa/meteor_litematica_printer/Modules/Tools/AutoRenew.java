package com.kkllffaa.meteor_litematica_printer.Modules.Tools;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import java.util.List;

import com.kkllffaa.meteor_litematica_printer.Addon;

public class AutoRenew extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> Durability = sgGeneral.add(new DoubleSetting.Builder()
            .name("percentage")
            .description("The durability percentage.")
            .defaultValue(9.5)
            .range(1, 100)
            .sliderRange(1, 100)
            .build());

    private final Setting<List<Item>> blacklist = sgGeneral.add(new ItemListSetting.Builder()
            .name("blacklist")
            .description("Items that should not be auto-renew in hand.")
            .defaultValue(Items.LEATHER_HELMET,
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
                    Items.ELYTRA)
            .build());

    public AutoRenew() {
        super(Addon.TOOLSCATEGORY, "auto-renew", "手持工具耐久度低于阈值时，从背包内替换同类物品");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        ItemStack mainHandStack = mc.player.getMainHandStack();

        if (!mainHandStack.isEmpty() && needRew(mainHandStack)) {
            int bestSlot = findBestReplacement(mainHandStack);

            if (bestSlot != -1) {
                int selectedSlot = mc.player.getInventory().getSelectedSlot();

                InvUtils.move().from(bestSlot).toHotbar(selectedSlot);

                if (!mc.player.currentScreenHandler.getCursorStack().isEmpty()) {
                    FindItemResult emptySlot = InvUtils.findEmpty();
                    if (emptySlot.found()) {
                        InvUtils.click().slot(emptySlot.slot());
                    } else {
                        InvUtils.click().slot(bestSlot);
                        warning("No empty slot found, put back to original slot");
                    }
                }
            }
        }
    }

    // 查找背包中同类物品，耐久最高的槽位
    private int findBestReplacement(ItemStack referenceStack) {
        int bestSlot = -1;
        int bestDurability = -1;

        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() == referenceStack.getItem() && !needRew(stack)) { // 同类物品
                int durability = stack.getMaxDamage() - stack.getDamage();
                if (durability > bestDurability) {
                    bestDurability = durability;
                    bestSlot = i;
                }
            }
        }
        return bestSlot;
    }

    private boolean needRew(ItemStack itemStack) {
        return (itemStack.getMaxDamage() - itemStack.getDamage()) < (itemStack.getMaxDamage() * Durability.get() / 100D)
                && !blacklist.get().contains(itemStack.getItem());
    }

}
