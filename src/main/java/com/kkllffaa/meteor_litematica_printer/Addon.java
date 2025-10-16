package com.kkllffaa.meteor_litematica_printer;

import com.kkllffaa.meteor_litematica_printer.Modules.CRUD.AtomicSettings.*;
import com.kkllffaa.meteor_litematica_printer.Modules.CRUD.AtomicSettings.Executer.CRUDExecuter;
import com.kkllffaa.meteor_litematica_printer.Modules.CRUD.CRUDMainPanel.*;
import com.kkllffaa.meteor_litematica_printer.Modules.Tools.*;
import com.kkllffaa.meteor_litematica_printer.Modules.Tools.OnlyESP.*;

import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class Addon extends MeteorAddon {
	public static final Category CRUD = new Category("CRUD", new ItemStack(Items.PINK_CARPET));
	public static final Category SettingsForCRUD = new Category("SetForCRUD", new ItemStack(Items.PINK_CARPET));
	public static final Category TOOLS = new Category("EXTools", new ItemStack(Items.PINK_CARPET));
	
	@Override
	public void onInitialize() {
		// Modules
		Modules.get().add(PlaceSettings.Instance);
		Modules.get().add(InteractSettings.Instance);
		Modules.get().add(ContainerSettings.Instance);
		Modules.get().add(BreakSettings.Instance);
		Modules.get().add(CRUDExecuter.Instance);

		Modules.get().add(new Printer());
		Modules.get().add(new Deleter());
		Modules.get().add(new Deleter() {});
		Modules.get().add(new Deleter() {});
		Modules.get().add(new Deleter() {});
		Modules.get().add(new Deleter() {});

		Modules.get().add(new AutoSwarm());
		Modules.get().add(new AutoFix());
		Modules.get().add(new AutoRenew());
		Modules.get().add(new AutoLogin());
		Modules.get().add(new AutoTool());
		Modules.get().add(new AutoEat());
		Modules.get().add(new HangUp());

		Modules.get().add(new ItemFinder());
		Modules.get().add(new Parkour());
		Modules.get().add(new MidiParser());
		
	}

    @Override
    public String getPackage() {
        return "com.kkllffaa.meteor_litematica_printer";
    }

	@Override
	public void onRegisterCategories() {
		Modules.registerCategory(TOOLS);
		Modules.registerCategory(CRUD);
		Modules.registerCategory(SettingsForCRUD);
	}
}
