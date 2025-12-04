package com.kkllffaa.meteor_litematica_printer.Modules.AtomicSettings

import com.kkllffaa.meteor_litematica_printer.Addon
import meteordevelopment.meteorclient.events.world.TickEvent
import meteordevelopment.meteorclient.settings.*
import meteordevelopment.meteorclient.settings.Setting
import meteordevelopment.meteorclient.systems.modules.Module
import meteordevelopment.meteorclient.systems.modules.Modules
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
            .defaultValue(15)
            .range(0, 100)
            .build()
    )

    private val FreeItemsDelay: Setting<Int> = sgGeneral.add(
        IntSetting.Builder()
            .name("free-items-delay")
            .description("Delay in ticks for free SlotItems back.")
            .defaultValue(15)
            .range(0, 100)
            .build()
    )

    @EventHandler
    private fun onTick(event: TickEvent.Post) {
        val interactionManager = mc.interactionManager ?: return
        val player = mc.player ?: return

        if (useSlotIndex == player.getInventory().selectedSlot) {
            InvUtils.swapBack()
        } else {
            InvUtils.previousSlot = -1
        }

    }

    private val useSlots get() = 9 - useSlotsLen.get()..8
    private var 回切倒计时 = 0
    private var 物品回切倒计时 = 0

    private val 自动的Slots = linkedSetOf<Int>()
    private var 最近的自动槽位: Int
        get() = 自动的Slots.lastOrNull() ?: 8
        set(value) {
            自动的Slots.remove(value)
            自动的Slots.add(value)
        }
    private val 最久没用过的槽位: Int
        get() =
            useSlots.sortedDescending().firstOrNull { it !in 自动的Slots } ?: 自动的Slots.first()

    private fun 切换Slot(slot: Int): Boolean {
        val player = mc.player ?: return false
        if (player.inventory.selectedSlot != slot) InvUtils.swap(slot, true)
        最近的自动槽位 = slot
        回切倒计时 = SelectBackDelay.get()
        物品回切倒计时 = FreeItemsDelay.get()
        return true
    }

    private fun 切回Slot() {
        val player = mc.player ?: return
        if (InvUtils.previousSlot != -1) return
        val current = player.inventory.selectedSlot
        if (current == 最近的自动槽位 && current != InvUtils.previousSlot) {
            InvUtils.swapBack()
        } else {
            InvUtils.previousSlot = -1
        }
    }


    private val hotbarPrevious: Array<ItemStack?> = arrayOfNulls(9)
    private val hotbar最近使用: Array<ItemStack?> = arrayOfNulls(9)

    private fun 切换Item(FromMainSlot: Int, ToHotbarSlot: Int) {
        val player = mc.player ?: return
        if (hotbarPrevious[ToHotbarSlot] == null) hotbarPrevious[ToHotbarSlot] =
            player.inventory.getStack(ToHotbarSlot)
        hotbar最近使用[ToHotbarSlot] = player.inventory.getStack(FromMainSlot)
        InvUtils.quickSwap().fromId(ToHotbarSlot).to(FromMainSlot)
    }

    private fun 切回Item(slot: Int) {
        val player = mc.player ?: return
        val previousStack = hotbarPrevious[slot]
        if (previousStack == null) return
        val currentStack = player.inventory.getStack(slot)
        if (ItemStack.areItemsAndComponentsEqual(currentStack, hotbar最近使用[slot])
            && !ItemStack.areItemsAndComponentsEqual(previousStack, currentStack)
        ) {
            val previousStackSlot = InvUtils.find({ ItemStack.areItemsAndComponentsEqual(previousStack, it) }, 0, 35)
            InvUtils.quickSwap().fromId(slot).to(previousStackSlot)
        }
        hotbarPrevious[slot] = null
        
    }

    private fun 切回全部Item() {
        for (slot in useSlots) {
            切回Item(slot)
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
                val emptySlot = useSlots.asSequence()
                    .sortedDescending()
                    .firstOrNull { player.inventory.getStack(it).isEmpty }

                when {
                    emptySlot != null -> {
                        切换Item(slot, emptySlot)
                        切换Slot(emptySlot)
                    }

                    else -> {
                        val lruSlot = 最久没用过的槽位
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
        val recentSlot by lazy { 最近的自动槽位 }
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
                    val emptySlot = useSlots.asSequence()
                        .sortedDescending()
                        .firstOrNull { player.inventory.getStack(it).isEmpty }

                    when {
                        emptySlot != null -> {
                            切换Item(result.slot, emptySlot)
                            切换Slot(emptySlot)
                        }

                        else -> {
                            val lruSlot = 最久没用过的槽位
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
