package com.kkllffaa.meteor_litematica_printer.Modules.AtomicSettings

import com.kkllffaa.meteor_litematica_printer.Addon
import com.kkllffaa.meteor_litematica_printer.Functions.物品及非耐久属性全部相同于
import meteordevelopment.meteorclient.events.world.TickEvent
import meteordevelopment.meteorclient.settings.*
import meteordevelopment.meteorclient.settings.Setting
import meteordevelopment.meteorclient.systems.modules.Module
import meteordevelopment.meteorclient.utils.player.InvUtils
import meteordevelopment.meteorclient.utils.player.SlotUtils
import meteordevelopment.orbit.EventHandler
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.item.Item
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket
import net.minecraft.item.ItemStack

object SwapSettings : Module(Addon.SettingsForCRUD, "Swap", "Module to configure AtomicSettings.") {
    override fun toggle() {
        if (isActive) return
        super.toggle()
    }

    init {
        this.toggle()
    }

    private val sgGeneral = settings.defaultGroup
    private val useSlotsLen: Setting<Int> = sgGeneral.add(
        IntSetting.Builder()
            .name("use-slots")
            .description("The number of slots assigned to automatic processing, reverse(default is Slots 8 and 9)")
            .defaultValue(2)
            .range(1, 9)
            .sliderRange(1, 9)
            .build()
    )

    private val SelectBackDelay: Setting<Int> = sgGeneral.add(
        IntSetting.Builder()
            .name("select-back-delay")
            .description("Delay in ticks for free SlotSelect back.")
            .defaultValue(10)
            .range(0, 100)
            .build()
    )

    private val FreeItemsDelay: Setting<Int> = sgGeneral.add(
        IntSetting.Builder()
            .name("free-items-delay")
            .description("Delay in ticks for free SlotItems back.")
            .defaultValue(10)
            .range(0, 100)
            .build()
    )
    private val stopMovingWhenSwitchItemStack: Setting<Boolean> = sgGeneral.add(
        BoolSetting.Builder()
            .name("stop-moving-when-switch")
            .description("Stop moving when switching items.")
            .defaultValue(true)
            .build()
    )

    @EventHandler
    private fun onTick(event: TickEvent.Post) {
        if (HBSlot回切倒计时 > 0) {
            HBSlot回切倒计时--
        } else {
            切回Slot()
            HBSlot回切倒计时 = SelectBackDelay.get()
        }
        if (HBItems回切倒计时 > 0) {
            HBItems回切倒计时--
        } else {
            切回全部Item()
            HBItems回切倒计时 = FreeItemsDelay.get()
        }
    }

    private val 分配的自动HBSlots get() = 9 - useSlotsLen.get()..8
    private var HBSlot回切倒计时 = 0
    private var HBItems回切倒计时 = 0

    private val 使用过HBSlots = linkedSetOf<Int>()
    private var 使用的HBSlot: Int
        get() = 使用过HBSlots.lastOrNull() ?: 8
        set(value) {
            使用过HBSlots.remove(value)
            使用过HBSlots.add(value)
        }
    private val 最久的HBSlot: Int
        get() = 分配的自动HBSlots.sortedDescending().firstOrNull { it !in 使用过HBSlots } ?: 使用过HBSlots.first()

    private fun 切换Slot(slot: Int): Boolean {
        val player = mc.player ?: return false
        if (player.inventory.selectedSlot != slot) InvUtils.swap(slot, true)
        使用的HBSlot = slot
        HBSlot回切倒计时 = SelectBackDelay.get()
        HBItems回切倒计时 = FreeItemsDelay.get()
        return true
    }

    private fun 切回Slot() {
        if (InvUtils.previousSlot == -1) return
        val player = mc.player ?: return
        val currentHBSlot = player.inventory.selectedSlot
        if (currentHBSlot == 使用的HBSlot //玩家没有手动更换选框
            && currentHBSlot != InvUtils.previousSlot
        ) {
            InvUtils.swapBack()
        } else {
            InvUtils.previousSlot = -1
        }
    }


    private val previousItems: Array<ItemStack?> = arrayOfNulls(9)
    private val 使用的Items: Array<ItemStack?> = arrayOfNulls(9)

    private fun 切换Item(FromMainSlot: Int, ToHotbarSlot: Int) {
        val player = mc.player ?: return
        使用的Items[ToHotbarSlot] = player.inventory.getStack(FromMainSlot)
        if (previousItems[ToHotbarSlot] == null) previousItems[ToHotbarSlot] = player.inventory.getStack(ToHotbarSlot)
        InvUtils.quickSwap().fromId(ToHotbarSlot).to(FromMainSlot)
    }

    private fun 切回Item(HBSlot: Int) {
        val previousItem = previousItems[HBSlot]
        if (previousItem == null) return
        val player = mc.player ?: return
        val currentItem = player.inventory.getStack(HBSlot)
        if (currentItem.物品及非耐久属性全部相同于(使用的Items[HBSlot]) //玩家没有手动更换物品
            && !currentItem.物品及非耐久属性全部相同于(previousItem)
        ) {
            var lookingfor =
                InvUtils.find({ previousItem.物品及非耐久属性全部相同于(it) }, 9, 35)
            if (!lookingfor.found()) lookingfor =
                InvUtils.find({ previousItem.物品及非耐久属性全部相同于(it) }, 0, 8)
            if (lookingfor.found()) InvUtils.quickSwap().fromId(HBSlot).to(lookingfor.slot)
        }
        previousItems[HBSlot] = null

    }

    private fun 切回全部Item() {
        for (HBSlot in 分配的自动HBSlots) {
            切回Item(HBSlot)
        }
    }

    fun switchTo(
        player: ClientPlayerEntity,
        slot: Int,
    ): Boolean {
        return when {
            SlotUtils.isHotbar(slot) -> 切换Slot(slot)
            SlotUtils.isMain(slot) -> {
                // 物品在主背包，需要先移到快捷栏
                val emptySlot = 分配的自动HBSlots.asSequence()
                    .sortedDescending()
                    .firstOrNull { player.inventory.getStack(it).isEmpty }

                when {
                    emptySlot != null -> {
                        切换Item(slot, emptySlot)
                        切换Slot(emptySlot)
                    }

                    else -> {
                        val lruSlot = 最久的HBSlot
                        切换Item(slot, lruSlot)
                        切换Slot(lruSlot)
                    }
                }
            }

            else -> false
        }

    }

    fun switchTo(
        player: ClientPlayerEntity,
        item: Item,
    ): Boolean {
        val result by lazy { InvUtils.find(item) }
        val recentSlot by lazy { 使用的HBSlot }
        return when {
            // 情况1：主手已持有目标物品
            player.mainHandStack.item === item -> 切换Slot(player.inventory.selectedSlot)

            // 情况2：之前使用的槽位仍有目标物品
            player.inventory.getStack(recentSlot).item === item -> 切换Slot(recentSlot)
            // 情况3：创造模式，直接生成物品
            player.abilities.creativeMode -> {
                val slot = 8
                val stack = item.defaultStack
                mc.networkHandler?.sendPacket(
                    CreativeInventoryActionC2SPacket(36 + slot, stack)
                )
                player.inventory.setStack(slot, stack)
                切换Slot(slot)
            }

            // 情况4：在背包中找到目标物品
            result.found() -> when {
                result.isHotbar -> 切换Slot(result.slot)

                result.isMain -> {
                    // 物品在主背包，需要先移到快捷栏
                    val emptySlot = 分配的自动HBSlots.asSequence()
                        .sortedDescending()
                        .firstOrNull { player.inventory.getStack(it).isEmpty }

                    when {
                        emptySlot != null -> {
                            切换Item(result.slot, emptySlot)
                            切换Slot(emptySlot)
                        }

                        else -> {
                            val lruSlot = 最久的HBSlot
                            切换Item(result.slot, lruSlot)
                            切换Slot(lruSlot)
                        }
                    }
                }

                else -> false
            }

            else -> false
        }

    }

}
