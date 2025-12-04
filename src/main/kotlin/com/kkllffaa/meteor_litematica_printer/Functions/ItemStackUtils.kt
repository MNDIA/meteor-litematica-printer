package com.kkllffaa.meteor_litematica_printer.Functions

import net.minecraft.component.DataComponentTypes
import net.minecraft.item.ItemStack

/**
 * 比较两个 ItemStack 是否相等，忽略耐久度差异。
 * 
 * 这个方法会比较物品类型和所有组件，但忽略 DAMAGE 组件的差异。
 * 适用于需要判断两个物品是否是"同一种物品"但可能有不同损耗的场景。
 */
fun ItemStack.isItemsAndComponentsEqualIgnoringDamage(other: ItemStack?): Boolean {
    if (other == null) return false
    if (this === other) return true
    if (!this.isOf(other.item)) return false
    if (this.isEmpty && other.isEmpty) return true
    
    // 获取两个 ItemStack 的组件
    val thisComponents = this.components
    val otherComponents = other.components
    
    // 获取所有组件类型（合并两边的类型）
    val allTypes = buildSet {
        thisComponents.types.forEach { add(it) }
        otherComponents.types.forEach { add(it) }
    }
    
    // 比较所有组件，但跳过 DAMAGE 组件
    for (type in allTypes) {
        if (type == DataComponentTypes.DAMAGE) continue
        
        val thisValue = thisComponents.get(type)
        val otherValue = otherComponents.get(type)
        
        if (thisValue != otherValue) {
            return false
        }
    }
    
    return true
}
