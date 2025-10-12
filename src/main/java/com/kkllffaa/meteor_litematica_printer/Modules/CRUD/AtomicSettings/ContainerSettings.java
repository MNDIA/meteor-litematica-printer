package com.kkllffaa.meteor_litematica_printer.Modules.CRUD.AtomicSettings;

import com.kkllffaa.meteor_litematica_printer.Addon;
import meteordevelopment.meteorclient.systems.modules.Module;

public class ContainerSettings extends Module {
    public ContainerSettings() {
		super(Addon.SettingsForCRUD, "ContainerSettings", "Module to configure AtomicSettings.");
	}

	@Override
	public void onActivate(){
		this.toggle();
	}
}
