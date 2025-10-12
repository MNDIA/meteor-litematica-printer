package com.kkllffaa.meteor_litematica_printer.Modules;

import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import static net.minecraft.util.ActionResult.*;
import net.minecraft.util.Hand;

import java.util.List;

import com.kkllffaa.meteor_litematica_printer.Addon;

public class AutoLogin extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<String> triggerMessage = sgGeneral.add(new StringSetting.Builder()
        .name("trigger-message")
        .description("The message that triggers the auto login.")
        .defaultValue("Please login with")
        .build()
    );
    private final Setting<String> successMessage = sgGeneral.add(new StringSetting.Builder()
        .name("success-message")
        .description("The message that indicates a successful login.")
        .defaultValue("Login successful")
        .build()
    );
    private final Setting<List<String>> loginCommands = sgGeneral.add(new StringListSetting.Builder()
        .name("login-commands")
        .description("List of player name to command mappings. Format: player:/login password")
        .defaultValue(new java.util.ArrayList<>())
        .build()
    );

    private final Setting<Integer> cooldown = sgGeneral.add(new IntSetting.Builder()
        .name("cooldown")
        .description("Cooldown time in seconds between login attempts.")
        .defaultValue(5)
        .min(0)
        .sliderMax(60)
        .build()
    );
    private final Setting<Boolean> INFO = sgGeneral.add(new BoolSetting.Builder()
        .name("info")
        .description("Show info messages when auto login is triggered.")
        .defaultValue(false)
        .build()
    );

    private final Setting<String> itemNameContains = sgGeneral.add(new StringSetting.Builder()
        .name("item-name-contains")
        .description("The string that the item name must contain to be used after login.")
        .defaultValue("右键前往服务器")
        .build()
    );

    private final Setting<String> guiItemNameContains = sgGeneral.add(new StringSetting.Builder()
        .name("gui-item-name-contains")
        .description("The string that the item name in the GUI must contain to be clicked.")
        .defaultValue("前往服务器")
        .build()
    );

    private long lastLoginTime = 0;

    public AutoLogin() {
        super(Addon.TOOLSCATEGORY, "auto-login", "Automatically logs in when receiving specific messages.");
    }

    @Override
    public void info(String message, Object... args){
        if (INFO.get()){
            super.info(message, args);
        }
    }
    @Override
    public void info(Text message) {
        if (INFO.get()){
            super.info(message);
        }
    }

    @EventHandler
    private void onReceiveMessage(ReceiveMessageEvent event) {
        Text message = event.getMessage();
        String messageString = message.getString();

        if (messageString.contains(triggerMessage.get())) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastLoginTime < cooldown.get() * 1000L) {
                return;
            }

            if (mc.player != null) {
                String playerName = mc.player.getName().getString();
                List<String> commandsList = loginCommands.get();

                for (String pair : commandsList) {
                    String[] parts = pair.split(":", 2);
                    if (parts.length == 2 && parts[0].trim().equals(playerName)) {
                        String command = parts[1].trim();
                        ChatUtils.sendPlayerMsg(command);
                        lastLoginTime = currentTime;
                        info("%s with command: %s", playerName, command);
                        
                        break;
                    }
                }
            }
        }
        if (messageString.contains(successMessage.get())) {
            // 使用背包里的特定名称的物品
            String TargeItemName = itemNameContains.get();
            if (!TargeItemName.isEmpty()) {
                for (int slot = 0; slot < 9; slot++) {
                    ItemStack stack = mc.player.getInventory().getStack(slot);
                    if (!stack.isEmpty() && stack.getName().getString().contains(TargeItemName)) {
                        InvUtils.swap(slot, false);
                        ActionResult result = mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                        String resultMessage = result == SUCCESS ? "Used item successfully" :
                                                result == FAIL ? "Failed to use item" :
                                                result == PASS ? "Item usage passed" : "Unknown result";

                        info("Used item: %s - %s", stack.getName().getString(), resultMessage);
                        
                        break;
                    }
                }
            }
        }
    }

    @EventHandler
    private void onOpenScreen(OpenScreenEvent event) {
        if (event.screen instanceof GenericContainerScreen) {
            String guiItemName = guiItemNameContains.get();
            if (!guiItemName.isEmpty()) {
                for (Slot slot : mc.player.currentScreenHandler.slots) {
                    if (slot.hasStack() && slot.getStack().getName().getString().contains(guiItemName)) {
                        InvUtils.click().slotId(slot.id);
                        info("Clicked item in GUI: %s", slot.getStack().getName().getString());
                        break;
                    }
                }
            }
        }
    }
}