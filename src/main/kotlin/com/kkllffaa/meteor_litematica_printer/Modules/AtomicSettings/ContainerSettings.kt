package com.kkllffaa.meteor_litematica_printer.Modules.AtomicSettings

import com.kkllffaa.meteor_litematica_printer.Addon
import meteordevelopment.meteorclient.settings.SettingGroup
import meteordevelopment.meteorclient.systems.modules.Module

object ContainerSettings : Module(Addon.SettingsForCRUD, "Container", "Module to configure AtomicSettings.") {
    override fun toggle() {
        if (isActive) return
        super.toggle()
    }

    init {
        this.toggle()
    }

    private val sgGeneral = settings.defaultGroup


}
