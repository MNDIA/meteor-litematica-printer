package com.kkllffaa.meteor_litematica_printer.tools;

import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;

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

    private final Setting<Boolean> debug = sgGeneral.add(new BoolSetting.Builder()
        .name("debug")
        .description("Print received messages to console for copying trigger message.")
        .defaultValue(false)
        .build()
    );

    public AutoLogin() {
        super(Addon.TOOLSCATEGORY, "auto-login", "Automatically logs in when receiving specific messages.");
    }

    @EventHandler
    private void onReceiveMessage(ReceiveMessageEvent event) {
        Text message = event.getMessage();
        String messageString = message.getString();

        if (debug.get()) {
            Text copyText = Texts.bracketed(
                Text.literal("Copy Message")
                    .fillStyle(Style.EMPTY
                        .withColor(Formatting.GREEN)
                        .withClickEvent(new ClickEvent.CopyToClipboard(messageString))
                        .withHoverEvent(new HoverEvent.ShowText(Text.literal("Click to copy the message")))
                        .withInsertion(messageString)
                    )
            );
            info(copyText);
        }

        if (messageString.contains(triggerMessage.get())) {
            if (mc.player != null) {
                String playerName = mc.player.getName().getString();
                List<String> commandsList = loginCommands.get();

                for (String pair : commandsList) {
                    String[] parts = pair.split(":", 2);
                    if (parts.length == 2 && parts[0].trim().equals(playerName)) {
                        String command = parts[1].trim();
                        ChatUtils.sendPlayerMsg(command);
                        info("Auto-logged in for %s with command: %s", playerName, command);
                        break;
                    }
                }
            }
        }
    }
}