package com.kkllffaa.meteor_litematica_printer.Modules.AtomicSettings

import com.kkllffaa.meteor_litematica_printer.Addon
import com.kkllffaa.meteor_litematica_printer.Functions.SwapDoResult
import meteordevelopment.meteorclient.settings.*
import meteordevelopment.meteorclient.settings.Setting
import meteordevelopment.meteorclient.systems.modules.Module
import meteordevelopment.meteorclient.utils.player.InvUtils
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.item.Item
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket

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

    private val useSlots get() = 9 - useSlotsLen.get()..8

    private val 使用的Slots = linkedSetOf<Int>()
    private var 最近的使用槽位: Int
        get() = 使用的Slots.lastOrNull() ?: 8
        set(value) {
            使用的Slots.remove(value)
            使用的Slots.add(value)
        }
    private val 最久没用过的槽位: Int
        get() =
            useSlots.sortedDescending().firstOrNull { it !in 使用的Slots } ?: 使用的Slots.first()

    fun switchTo(
        player: ClientPlayerEntity,
        item: Item,
    ): Boolean {
        val result by lazy { InvUtils.find(item) }
        val recentSlot by lazy { 最近的使用槽位 }
        fun 切换(slot: Int): Boolean {
            return if (InvUtils.swap(slot, true)) {
                最近的使用槽位 = slot
                true
            } else false
        }
        return when {
            // 情况1：主手已持有目标物品
            player.mainHandStack.item === item -> true

            // 情况2：之前使用的槽位仍有目标物品
            player.inventory.getStack(recentSlot).item === item -> 切换(recentSlot)
            // 情况3：创造模式，直接生成物品
            player.abilities.creativeMode -> {
                val slot = 8
                val stack = item.defaultStack
                mc.networkHandler?.sendPacket(
                    CreativeInventoryActionC2SPacket(36 + slot, stack)
                )
                player.inventory.setStack(slot, stack)
                切换(slot)
            }

            // 情况4：在背包中找到目标物品
            result.found() -> when {
                result.isHotbar -> 切换(result.slot)

                result.isMain -> {
                    // 物品在主背包，需要先移到快捷栏
                    val emptySlot = useSlots.asSequence()
                        .sortedDescending()
                        .firstOrNull { player.inventory.getStack(it).isEmpty }

                    when {
                        emptySlot != null -> {
                            InvUtils.quickSwap().fromId(emptySlot).to(result.slot)
                            切换(emptySlot)
                        }

                        else -> {
                            val lruSlot = 最久没用过的槽位
                            InvUtils.quickSwap().fromId(lruSlot).to(result.slot)
                            切换(lruSlot)
                        }
                    }
                }

                else -> false
            }

            else -> false
        }

    }

}
