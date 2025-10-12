package com.kkllffaa.meteor_litematica_printer;

import com.kkllffaa.meteor_litematica_printer.Functions.MyUtils;
import com.kkllffaa.meteor_litematica_printer.Modules.CRUD.CRUDMainPanel.*;
import com.kkllffaa.meteor_litematica_printer.Modules.CRUD.Executer.*;
import com.kkllffaa.meteor_litematica_printer.Modules.Tools.*;
import com.kkllffaa.meteor_litematica_printer.Modules.Tools.OnlyESP.*;

import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class Addon extends MeteorAddon {
	public static final Category CRUDCATEGORY = new Category("EXCRUD", new ItemStack(Items.PINK_CARPET));
	public static final Category SettingsForCRUD = new Category("EXSettings", new ItemStack(Items.PINK_CARPET));
	public static final Category TOOLSCATEGORY = new Category("EXTools", new ItemStack(Items.PINK_CARPET));
	
	@Override
	public void onInitialize() {
		// Modules
		Modules.get().add(MyUtils.PlaceSettingsModule);
		Modules.get().add(MyUtils.InteractSettingsModule);
		Modules.get().add(new Printer());
		Modules.get().add(new Deleter("deleter-ps1"));
		Modules.get().add(new Deleter("deleter-ps2") {});
		Modules.get().add(new Deleter("deleter-ps3") {});
		Modules.get().add(new Deleter("deleter-ps4") {});
		Modules.get().add(new Deleter("deleter-ps5") {});

		Modules.get().add(new AutoSwarm());
		Modules.get().add(new AutoFix());
		Modules.get().add(new AutoRenew());
		Modules.get().add(new AutoLogin());
		Modules.get().add(new AutoTool());
		Modules.get().add(new AutoEat());

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
		Modules.registerCategory(CRUDCATEGORY);
		Modules.registerCategory(SettingsForCRUD);
		Modules.registerCategory(TOOLSCATEGORY);
	}
}
