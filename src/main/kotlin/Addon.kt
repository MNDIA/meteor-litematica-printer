package com.kkllffaa.meteor_litematica_printer

import com.kkllffaa.meteor_litematica_printer.Modules.CRUD.AtomicSettings.*
import com.kkllffaa.meteor_litematica_printer.Modules.CRUD.CRUDMainPanel.*
import com.kkllffaa.meteor_litematica_printer.Modules.Tools.*
import com.kkllffaa.meteor_litematica_printer.Modules.Tools.OnlyESP.*

import meteordevelopment.meteorclient.addons.MeteorAddon
import meteordevelopment.meteorclient.systems.modules.Category
import meteordevelopment.meteorclient.systems.modules.Modules
import net.minecraft.item.ItemStack
import net.minecraft.item.Items

class Addon : MeteorAddon() {
    override fun onInitialize() {
        // Modules
        Modules.get().add(CommonSettings)
        Modules.get().add(PlaceSettings)
        Modules.get().add(InteractSettings)
        Modules.get().add(ContainerSettings)
        Modules.get().add(BreakSettings)

        Modules.get().add(Printer)
        // Modules.get().add(Deleter())
        // Modules.get().add(Deleter())
        // Modules.get().add(Deleter())
        // Modules.get().add(Deleter())
        // Modules.get().add(Deleter())

        Modules.get().add(AutoSwarm)
        Modules.get().add(AutoFix)
        Modules.get().add(AutoRenew)
        Modules.get().add(AutoLogin)
        Modules.get().add(AutoTool)
        Modules.get().add(AutoEat)
        Modules.get().add(HangUp)
        Modules.get().add(SwingHand)
        Modules.get().add(Hello)
        Modules.get().add(ItemFinder)
        Modules.get().add(Parkour)
        Modules.get().add(MidiParser)
        Modules.get().add(ChatLogger)
        Modules.get().add(ShopLimiter)
        Modules.get().add(MovePacketLogger)
    }

    override fun getPackage(): String = "com.kkllffaa.meteor_litematica_printer"

    override fun onRegisterCategories() {
        Modules.registerCategory(TOOLS)
        Modules.registerCategory(CRUD)
        Modules.registerCategory(SettingsForCRUD)
    }

    companion object {
        @JvmField
        val CRUD = Category("CRUD", ItemStack(Items.PINK_CARPET))
        
        @JvmField
        val SettingsForCRUD = Category("SetForCRUD", ItemStack(Items.PINK_CARPET))
        
        @JvmField
        val TOOLS = Category("EXTools", ItemStack(Items.PINK_CARPET))
    }
}
