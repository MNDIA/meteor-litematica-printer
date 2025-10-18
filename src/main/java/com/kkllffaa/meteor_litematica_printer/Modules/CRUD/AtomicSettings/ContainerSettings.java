package com.kkllffaa.meteor_litematica_printer.Modules.CRUD.AtomicSettings;

import com.kkllffaa.meteor_litematica_printer.Addon;

import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;

public class ContainerSettings extends Module {
    public static ContainerSettings Instance = new ContainerSettings();
    public ContainerSettings() {
		super(Addon.SettingsForCRUD, "Container", "Module to configure AtomicSettings.");
		this.toggle();
	}

	@Override
	public void toggle() {
        if (isActive()) {
            return;
        }
        super.toggle();
    }

	
	private final SettingGroup sgGeneral = settings.getDefaultGroup();
}
