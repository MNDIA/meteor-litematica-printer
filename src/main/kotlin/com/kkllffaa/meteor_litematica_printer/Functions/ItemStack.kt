package com.kkllffaa.meteor_litematica_printer.Functions

import net.minecraft.component.DataComponentTypes
import net.minecraft.item.ItemStack


fun ItemStack.物品及非耐久属性全部相同于(other: ItemStack?): Boolean {
    if (other == null) return false
    if (this === other) return true
    if (!this.isOf(other.item)) return false
    if (this.isEmpty && other.isEmpty) return true
    
    val thisComponents = this.components
    val otherComponents = other.components
    
    val allTypes = buildSet {
        thisComponents.types.forEach { add(it) }
        otherComponents.types.forEach { add(it) }
    }
    
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
