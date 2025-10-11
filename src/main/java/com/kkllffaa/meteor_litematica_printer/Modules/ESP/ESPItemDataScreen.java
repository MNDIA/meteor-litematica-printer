package com.kkllffaa.meteor_litematica_printer.Modules.ESP;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.item.Item;

public class ESPItemDataScreen extends WindowScreen {
    private final ESPItemData itemData;
    private final Item item;
    private final ItemDataSetting<ESPItemData> setting;

    public ESPItemDataScreen(GuiTheme theme, ESPItemData itemData, Item item, ItemDataSetting<ESPItemData> setting) {
        super(theme, "Configure Item");

        this.itemData = itemData;
        this.item = item;
        this.setting = setting;
    }

    @Override
    public void initWidgets() {
        Settings settings = new Settings();
        SettingGroup sgGeneral = settings.getDefaultGroup();
        SettingGroup sgTracer = settings.createGroup("Tracer");

        sgGeneral.add(new EnumSetting.Builder<ShapeMode>()
            .name("shape-mode")
            .description("How the shape is rendered.")
            .defaultValue(ShapeMode.Lines)
            .onModuleActivated(shapeModeSetting -> shapeModeSetting.set(itemData.shapeMode))
            .onChanged(shapeMode -> {
                if (itemData.shapeMode != shapeMode) {
                    itemData.shapeMode = shapeMode;
                    onChanged();
                }
            })
            .build()
        );

        sgGeneral.add(new ColorSetting.Builder()
            .name("line-color")
            .description("Color of lines.")
            .defaultValue(new SettingColor(0, 255, 200))
            .onModuleActivated(settingColorSetting -> settingColorSetting.get().set(itemData.lineColor))
            .onChanged(settingColor -> {
                if (!itemData.lineColor.equals(settingColor)) {
                    itemData.lineColor.set(settingColor);
                    onChanged();
                }
            })
            .build()
        );

        sgGeneral.add(new ColorSetting.Builder()
            .name("side-color")
            .description("Color of sides.")
            .defaultValue(new SettingColor(0, 255, 200, 25))
            .onModuleActivated(settingColorSetting -> settingColorSetting.get().set(itemData.sideColor))
            .onChanged(settingColor -> {
                if (!itemData.sideColor.equals(settingColor)) {
                    itemData.sideColor.set(settingColor);
                    onChanged();
                }
            })
            .build()
        );

        sgTracer.add(new BoolSetting.Builder()
            .name("tracer")
            .description("If tracer line is allowed to this item.")
            .defaultValue(true)
            .onModuleActivated(booleanSetting -> booleanSetting.set(itemData.tracer))
            .onChanged(aBoolean -> {
                if (itemData.tracer != aBoolean) {
                    itemData.tracer = aBoolean;
                    onChanged();
                }
            })
            .build()
        );

        sgTracer.add(new ColorSetting.Builder()
            .name("tracer-color")
            .description("Color of tracer line.")
            .defaultValue(new SettingColor(0, 255, 200, 125))
            .onModuleActivated(settingColorSetting -> settingColorSetting.get().set(itemData.tracerColor))
            .onChanged(settingColor -> {
                if (!itemData.tracerColor.equals(settingColor)) {
                    itemData.tracerColor.set(settingColor);
                    onChanged();
                }
            })
            .build()
        );

        settings.onActivated();
        add(theme.settings(settings)).expandX();
    }

    private void onChanged() {
        if (!itemData.isChanged() && item != null && setting != null) {
            setting.get().put(item, itemData);
            setting.onChanged();
        }

        itemData.changed();
    }
}