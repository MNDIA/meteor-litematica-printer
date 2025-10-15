package com.kkllffaa.meteor_litematica_printer.Modules.Tools;

import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
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
    private final Setting<List<String>> loginCommands = sgGeneral.add(new StringListSetting.Builder()
    .name("login-commands")
    .description("List of player name to command mappings. Format: player:login_command:standby_command")
    .defaultValue(new java.util.ArrayList<>())
    .build()
    );
    
    private final Setting<String> successMessage = sgGeneral.add(new StringSetting.Builder()
        .name("success-message")
        .description("The message that indicates a successful login.")
        .defaultValue("Login suc")
        .build()
    );
    
    private final Setting<String> 菜单物品包含名字 = sgGeneral.add(new StringSetting.Builder()
    .name("menu-item-name-keyword")
    .description("The string that the item name must contain to be used after login.")
    .defaultValue("")
    .build()
    );
    
    private final Setting<String> 服务器入口物品包含名字 = sgGeneral.add(new StringSetting.Builder()
    .name("server-item-name-keyword")
    .description("The string that the item name in the GUI must contain to be clicked.")
    .defaultValue("")
    .build()
    );
    
    private final Setting<Integer> delayTicksSetting = sgGeneral.add(new IntSetting.Builder()
        .name("delay-ticks")
        .description("Number of ticks to delay before searching and clicking the entry item.")
        .defaultValue(10)
        .min(0)
        .max(100)
        .build()
    );
    
    private final Setting<Boolean> INFO = sgGeneral.add(new BoolSetting.Builder()
        .name("info")
        .description("Show info messages when auto login is triggered.")
        .defaultValue(false)
        .build()
    );
    
    public AutoLogin() {
        super(Addon.TOOLS, "auto-login", "Automatically logs in when receiving specific messages.");
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
    
    private enum State {
        预备触发登录,
        挂起操作打开菜单,
        挂起操作搜索入口,
        挂起操作输入待命命令,
        输入了登陆命令,
        进入了服务器
    }

    private State currentState = State.预备触发登录;
    private int 挂起操作Tick = 0;

    @EventHandler
    private void onGameJoined(GameJoinedEvent event) {
        if (currentState == State.挂起操作搜索入口) {
            挂起操作Tick = delayTicksSetting.get();
            currentState = State.挂起操作输入待命命令;
        } else {
            currentState = State.预备触发登录;
        }
    }
    @EventHandler
    private void onReceiveMessage(ReceiveMessageEvent event) {
        if (mc.player == null) return;
        Text message = event.getMessage();
        String messageString = message.getString();

        if (messageString.contains(triggerMessage.get())) {
            if (currentState == State.预备触发登录) {
                if (输入登录命令()) {
                    currentState = State.输入了登陆命令;
                }else{
                    currentState = State.预备触发登录;
                }
            }
        } else if (messageString.contains(successMessage.get())) {
            if (currentState == State.输入了登陆命令) {
                挂起操作Tick = delayTicksSetting.get();
                currentState = State.挂起操作打开菜单;
            }
        }
    }

    @EventHandler
    private void onOpenScreen(OpenScreenEvent event) {
        if (event.screen instanceof GenericContainerScreen && currentState == State.挂起操作打开菜单) {
            挂起操作Tick = delayTicksSetting.get();
            currentState = State.挂起操作搜索入口;
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (挂起操作Tick > 0) {
            挂起操作Tick--;
            if (挂起操作Tick == 0) {

                if (currentState == State.挂起操作打开菜单) {
                    if (搜索hotbar打开菜单物品()) {
                        //TO onOpenScreen
                    }
                    else{
                        currentState = State.预备触发登录;
                    }
                    return;


                } else if (currentState == State.挂起操作搜索入口) {
                    if (搜索菜单点击入口物品()) {
                        //TO  onGameJoined
                    } else {
                        currentState = State.预备触发登录;
                    }
                    return;


                }else if (currentState == State.挂起操作输入待命命令){
                    输入待命命令();
                    currentState = State.预备触发登录;
                    return;
                }


               
            }
        }
    }

    private boolean 输入登录命令() {
        String playerName = mc.player.getName().getString();
        List<String> commandsList = loginCommands.get();

        for (String pair : commandsList) {
            String[] parts = pair.split(":", 3);
            if (parts.length >= 2 && parts[0].trim().equals(playerName)) {
                String command = parts[1].trim();
                ChatUtils.sendPlayerMsg(command);
                info("%s with command: %s", playerName, command);
                return true;
            }
        }
        return false;
    }
    private boolean 输入待命命令() {
        String playerName = mc.player.getName().getString();
        List<String> commandsList = loginCommands.get();

        for (String pair : commandsList) {
            String[] parts = pair.split(":", 3);
            if (parts.length == 3 && parts[0].trim().equals(playerName)) {
                String standbyCommand = parts[2].trim();
                if (!standbyCommand.isEmpty()) {
                    ChatUtils.sendPlayerMsg(standbyCommand);
                    info("%s standby command: %s", playerName, standbyCommand);
                }
                return true;
            }
        }
        return false;
    }
    private boolean 搜索hotbar打开菜单物品() {
        if (菜单物品包含名字.get().isEmpty()) return true;
        info("Looking for menu item with name containing: %s in hotbar", 菜单物品包含名字.get());
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = mc.player.getInventory().getStack(slot);
            if (!stack.isEmpty() && stack.getName().getString().contains(菜单物品包含名字.get())) {
                InvUtils.swap(slot, false);
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                info("Used item: %s", stack.getName().getString());
                return true;
            }
        }
        return false;
    }

    private boolean 搜索菜单点击入口物品() {
        if (服务器入口物品包含名字.get().isEmpty()) return true;
        var slots = mc.player.currentScreenHandler.slots;
        info("Looking for entry item with name containing: %s in %s slots", 服务器入口物品包含名字.get(), slots.size());
        for (Slot slot : slots) {
            if (slot.hasStack()) {
                String name = slot.getStack().getName().getString();
                info("Found item in GUI: %s", name);
                if (name.contains(服务器入口物品包含名字.get())) {
                    InvUtils.click().slotId(slot.id);
                    info("Clicked item in GUI: %s", name);
                    return true;
                }
            }
        }
        return false;
    }


}