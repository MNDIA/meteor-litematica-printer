package com.kkllffaa.meteor_litematica_printer.Modules;

import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.text.Text;

import java.util.List;

import com.kkllffaa.meteor_litematica_printer.Addon;

public class AutoLogin extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<String> triggerMessage = sgGeneral.add(new StringSetting.Builder()
        .name("trigger-message")
        .description("The message that triggers the auto login.")
        .defaultValue("Please login with \"/login <password>\"")
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

    private long lastLoginTime = 0;

    public AutoLogin() {
        super(Addon.TOOLSCATEGORY, "auto-login", "Automatically logs in when receiving specific messages.");
    }

    @EventHandler
    private void onReceiveMessage(ReceiveMessageEvent event) {
        Text message = event.getMessage();
        String messageString = message.getString();

        if (messageString.contains(triggerMessage.get())) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastLoginTime < cooldown.get() * 1000L) {
                return; // Cooldown active, skip login
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
                        if (INFO.get()){
                            info("%s with command: %s", playerName, command);
                        }
                        break;
                    }
                }
            }
        }
    }
}