package com.kkllffaa.meteor_litematica_printer.Modules.AtomicSettings

import com.kkllffaa.meteor_litematica_printer.Addon
import meteordevelopment.meteorclient.settings.SettingGroup
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


    private var usedSlot = 8
    fun switchItem(
        item: Item,
        returnHand: Boolean,
        action: () -> Boolean
    ): Boolean {
        val player = mc.player ?: return false
        val selectedSlot = player.inventory.selectedSlot
        val result = InvUtils.find(item)

        // 执行操作并处理槽位记录
        fun tryAction(updateUsedSlot: Boolean = true): Boolean {
            return if (action()) {
                if (updateUsedSlot) usedSlot = player.inventory.selectedSlot
                true
            } else false
        }

        // 切换槽位，执行操作，失败则切回
        fun swapAndTry(slot: Int, updateUsedSlot: Boolean = true): Boolean {
            InvUtils.swap(slot, returnHand)
            return if (tryAction(updateUsedSlot)) {
                true
            } else {
                InvUtils.swap(selectedSlot, returnHand)
                false
            }
        }

        return when {
            // 情况1：主手已持有目标物品
            player.mainHandStack.item === item -> tryAction()

            // 情况2：之前使用的槽位仍有目标物品
            player.inventory.getStack(usedSlot).item === item -> swapAndTry(usedSlot, updateUsedSlot = false)

            // 情况3：在背包中找到目标物品
            result.found() -> when {
                result.isHotbar -> swapAndTry(result.slot())
                result.isMain -> {
                    // 物品在主背包，需要先移到快捷栏
                    val empty = InvUtils.find({ it.isEmpty }, 0, 8)
                    when {
                        empty.found() -> {
                            InvUtils.move().from(result.slot).toHotbar(empty.slot)
                            swapAndTry(empty.slot)
                        }

                        else -> {
                            InvUtils.move().from(result.slot).toHotbar(usedSlot)
                            swapAndTry(usedSlot, updateUsedSlot = false)
                        }
                    }
                }

                else -> false
            }

            // 情况4：创造模式，直接生成物品
            player.abilities.creativeMode -> {
                val slot = InvUtils.find({ it.isEmpty }, 0, 8)
                    .takeIf { it.found() }?.slot() ?: 0
                val stack = item.defaultStack
                mc.networkHandler?.sendPacket(
                    CreativeInventoryActionC2SPacket(36 + slot, stack)
                )
                player.inventory.setStack(slot, stack)
                swapAndTry(slot)
            }

            else -> false
        }
    }
}
