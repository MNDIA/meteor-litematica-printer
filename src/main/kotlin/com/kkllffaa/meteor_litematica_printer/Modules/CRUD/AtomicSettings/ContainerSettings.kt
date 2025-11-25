package com.kkllffaa.meteor_litematica_printer.Modules.CRUD.AtomicSettings

import com.kkllffaa.meteor_litematica_printer.Addon
import meteordevelopment.meteorclient.settings.SettingGroup
import meteordevelopment.meteorclient.systems.modules.Module

class ContainerSettings : Module(Addon.SettingsForCRUD, "Container", "Module to configure AtomicSettings.") {
    override fun toggle() {
        if (isActive()) {
            return
        }
        super.toggle()
    }


    private val sgGeneral: SettingGroup? = settings.getDefaultGroup()

    init {
        this.toggle()
    }

    companion object {
        var Instance: ContainerSettings = ContainerSettings()
    }
}
