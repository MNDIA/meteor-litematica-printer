package com.kkllffaa.meteor_litematica_printer.Modules.CRUD.AtomicSettings.Executer;


import com.kkllffaa.meteor_litematica_printer.Addon;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

public class CRUDExecuter extends Module {
    public CRUDExecuter() {
        super(Addon.SettingsForCRUD, "CRUDExecuter", "Execute CRUD operations in TickEvent.");
    }
    
    @Override
    public void onActivate(){
    }

    @Override
    public void onDeactivate(){
    }

    
	private final SettingGroup sgGeneral = settings.getDefaultGroup();

    @EventHandler
    private void onTick(TickEvent.Pre event) {
    }
    @EventHandler
    private void onTick(TickEvent.Post event) {
    }

}
