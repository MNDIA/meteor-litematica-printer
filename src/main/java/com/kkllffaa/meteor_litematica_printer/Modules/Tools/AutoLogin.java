package com.kkllffaa.meteor_litematica_printer.Modules.Tools;

import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;

import java.util.List;

import com.kkllffaa.meteor_litematica_printer.Addon;

public class AutoLogin extends Module {
    private enum LoginState {
        预备登录,
        等待登陆成功,
        预备打开菜单,
        等待打开菜单,
        预备点击入口,
        等待进入服务器,
        预备待命命令,
        等待传送完成,
        预备待命状态
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<String> triggerMessage = sgGeneral.add(new StringSetting.Builder()
        .name("trigger-message")
        .description("The message that triggers the auto login.")
        .defaultValue("Please login with")
        .build()
    );

    private final Setting<List<String>> loginCommands = sgGeneral.add(new StringListSetting.Builder()
    .name("login-commands")
    .description("List of player name to command mappings. Format: player:login_command:standby_command:standby_state")
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
    .defaultValue(">>>")
    .build()
    );

    private final Setting<String> 主城大区Message = sgGeneral.add(new StringSetting.Builder()
    .name("main-city-region-message")
    .description("The message that indicates the main city region.")
    .defaultValue("进入了 [主城大区]")
    .build()
    );

    private final Setting<String> 生存大区Message = sgGeneral.add(new StringSetting.Builder()
    .name("survival-region-message")
    .description("The message that indicates the survival region.")
    .defaultValue("进入了 [生存")
    .build()
    );
    
    private final Setting<Integer> CheckTicks = sgGeneral.add(new IntSetting.Builder()
        .name("check-ticks")
        .description("执行步骤后延迟后检查执行结果是否成功.")
        .defaultValue(300)
        .min(0)
        .build()
    );

    private final Setting<Integer> readyTicks = sgGeneral.add(new IntSetting.Builder()
        .name("delay-ticks")
        .description("预备执行的反应时间.")
        .defaultValue(3)
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
    
    private LoginState loginState = LoginState.预备登录;
    private int delayCounter = 0;
    
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

    @Override
    public void onActivate() {
        if (loginState == LoginState.等待登陆成功 || loginState == LoginState.等待打开菜单 || loginState == LoginState.等待进入服务器 || loginState == LoginState.等待传送完成) return;
        预备登录();
    }

    private void 预备登录() {
        loginState = LoginState.预备登录;
        delayCounter = 0;
    }
    @EventHandler
    private void onReceiveMessage(ReceiveMessageEvent event) {
        if (mc.player == null) return;
        Text originalText = event.getMessage();
        MessageIndicator originalIndicator = event.getIndicator();
        int originalId = event.id;

        String message = originalText.getString();

        if (message.contains(triggerMessage.get())) {
            if (loginState == LoginState.预备登录) {
                if (输入登录命令()) {
                    loginState = LoginState.等待登陆成功;
                    delayCounter = CheckTicks.get();
                } else {
                    预备登录();
                }
            }
        } else if (message.contains(successMessage.get())) {
            if (loginState == LoginState.等待登陆成功) {
                loginState = LoginState.预备打开菜单;
                delayCounter = readyTicks.get();
            }
        } else if (message.contains(mc.player.getName().getString())&&message.contains(主城大区Message.get())) {
            info("进入了 [主城大区] State: %s", loginState);
            if (loginState == LoginState.等待进入服务器) {
                loginState = LoginState.预备待命命令;
                delayCounter = readyTicks.get()+40;
            }
        } else if (message.contains(mc.player.getName().getString())&&message.contains(生存大区Message.get())) {
            info("进入了 [生存大区] State: %s", loginState);
            if (loginState == LoginState.等待传送完成) {
                loginState = LoginState.预备待命状态;
                delayCounter = readyTicks.get();
            }
        }

        if (event.getMessage() != originalText) {
            event.setMessage(originalText);
        }
        if (event.getIndicator() != originalIndicator) {
            event.setIndicator(originalIndicator);
        }
        if (event.id != originalId) {
            event.id = originalId;
        }
    }

    @EventHandler
    private void onOpenScreen(OpenScreenEvent event) {
        if (event.screen instanceof GenericContainerScreen) {
            if (loginState == LoginState.等待打开菜单) {
                loginState = LoginState.预备点击入口;
                delayCounter = readyTicks.get();
            }
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (delayCounter > 0) {
            delayCounter--;
        } else {
            switch (loginState) {
                case 预备登录 ->{ return;}
                case 等待登陆成功, 等待打开菜单,等待进入服务器, 等待传送完成 -> {
                    info("State %s timeout, resetting.", loginState);
                    // 延迟结束但没有成功消息，重置
                    预备登录();
                }
                case 预备打开菜单 -> {
                    if (搜索hotbar打开菜单物品()) {
                        loginState = LoginState.等待打开菜单;
                        delayCounter = CheckTicks.get();
                    } else {
                        loginState = LoginState.预备待命命令;
                    }
                }
                case 预备点击入口 -> {
                    if (搜索菜单点击入口物品()) {
                        info("Clicked entry item, waiting to enter server...");
                        loginState = LoginState.等待进入服务器;
                        delayCounter = CheckTicks.get();
                    } else {
                        loginState = LoginState.预备待命命令;
                    }
                }
                case 预备待命命令 -> {
                    if (输入待命命令()) {
                        loginState = LoginState.等待传送完成;
                        delayCounter = CheckTicks.get();
                    } else {
                        预备登录();
                    }
                }
                case 预备待命状态 -> {
                    打开待机状态();
                    预备登录();
                }
            }
        }
    }

    private void 打开待机状态() {
        String playerName = mc.player.getName().getString();
        List<String> commandsList = loginCommands.get();
        for (String pair : commandsList) {
            String[] parts = pair.split(":", 4);
            if (parts.length >= 4 && parts[0].trim().equals(playerName)) {
                String standbyState = parts[3].trim();
                if ("挂机".equals(standbyState)) {
                    HangUp hangUpModule = Modules.get().get(HangUp.class);
                    if (hangUpModule == null) return;
                    if (hangUpModule.isActive()) {
                        hangUpModule.toggle();
                        hangUpModule.toggle();
                    }
                    else hangUpModule.toggle();
                } else if ("商店限量".equals(standbyState)) {
                    ShopLimiter shopLimiter = Modules.get().get(ShopLimiter.class);
                    if (shopLimiter == null) return;
                    if (!shopLimiter.isActive()) shopLimiter.toggle();
                }
                return;
            }
        }
    }
    private boolean 输入登录命令() {
        String playerName = mc.player.getName().getString();
        List<String> commandsList = loginCommands.get();

        for (String pair : commandsList) {
            String[] parts = pair.split(":", 4);
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
            String[] parts = pair.split(":", 4);
            if (parts.length >= 3 && parts[0].trim().equals(playerName)) {
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