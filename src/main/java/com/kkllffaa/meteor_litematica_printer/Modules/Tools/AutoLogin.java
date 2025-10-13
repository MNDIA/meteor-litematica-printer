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
    private final Setting<List<String>> loginCommands = sgGeneral.add(new StringListSetting.Builder()
    .name("login-commands")
    .description("List of player name to command mappings. Format: player:/login password")
    .defaultValue(new java.util.ArrayList<>())
    .build()
    );
    
    private final Setting<String> successMessage = sgGeneral.add(new StringSetting.Builder()
        .name("success-message")
        .description("The message that indicates a successful login.")
        .defaultValue("Login suc")
        .build()
    );
    
    private final Setting<String> 菜单入口物品包含名字 = sgGeneral.add(new StringSetting.Builder()
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
        NONE,
        输入了登录命令,
        使用了菜单,
    }
    private State currentState = State.NONE;
    private int 挂起操作Tick = 0;
    @EventHandler
    private void onGameJoined(GameJoinedEvent event) {
        currentState = State.NONE;
    }
    @EventHandler
    private void onReceiveMessage(ReceiveMessageEvent event) {
        if (mc.player == null) return;
        Text message = event.getMessage();
        String messageString = message.getString();

        if (messageString.contains(triggerMessage.get())) {
            if (currentState == State.NONE) {
                输入登录命令();
            }
        } else if (messageString.contains(successMessage.get())) {
            if (currentState == State.输入了登录命令) {
                if (!菜单入口物品包含名字.get().isEmpty()){
                    挂起操作Tick = delayTicksSetting.get();
                }else{
                    currentState = State.NONE;
                }
            }
        }
    }

    @EventHandler
    private void onOpenScreen(OpenScreenEvent event) {
        if (event.screen instanceof GenericContainerScreen && currentState == State.使用了菜单) {
            挂起操作Tick = delayTicksSetting.get();
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (挂起操作Tick > 0) {
            挂起操作Tick--;
            if (挂起操作Tick == 0) {

                if (currentState == State.输入了登录命令) {
                    使用菜单入口物品();
                    currentState = State.使用了菜单;
                    return;

                }else if (currentState == State.使用了菜单){
                    if (!服务器入口物品包含名字.get().isEmpty()) {
                        搜索菜单并点击入口物品();
                    }
                    currentState = State.NONE;
                    return;
                }


               
            }
        }
    }

    private void 输入登录命令() {
        String playerName = mc.player.getName().getString();
        List<String> commandsList = loginCommands.get();

        for (String pair : commandsList) {
            String[] parts = pair.split(":", 2);
            if (parts.length == 2 && parts[0].trim().equals(playerName)) {
                String command = parts[1].trim();
                ChatUtils.sendPlayerMsg(command);
                currentState = State.输入了登录命令;
                info("%s with command: %s", playerName, command);
                return;
            }
        }
    }
    private void 使用菜单入口物品() {
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = mc.player.getInventory().getStack(slot);
            if (!stack.isEmpty() && stack.getName().getString().contains(菜单入口物品包含名字.get())) {
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

    private void 搜索菜单并点击入口物品() {
        var slots = mc.player.currentScreenHandler.slots;
        info("Looking for item with name containing: %s in %s slots", 服务器入口物品包含名字.get(), slots.size());
        for (Slot slot : slots) {
            if (slot.hasStack()) {
                String name = slot.getStack().getName().getString();
                info("Found item in GUI: %s", name);
                if (name.contains(服务器入口物品包含名字.get())) {
                    InvUtils.click().slotId(slot.id);
                    info("Clicked item in GUI: %s", name);
                    break;
                }
            }
        }
    }


}