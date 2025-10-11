package com.kkllffaa.meteor_litematica_printer.Modules.ESP;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.utils.misc.IChangeable;
import meteordevelopment.meteorclient.utils.misc.ICopyable;
import meteordevelopment.meteorclient.utils.misc.ISerializable;
import net.minecraft.item.Item;

public interface IItemData<T extends ICopyable<T> & ISerializable<T> & IChangeable & IItemData<T>> {
    WidgetScreen createScreen(GuiTheme theme, Item item, ItemDataSetting<T> setting);
}