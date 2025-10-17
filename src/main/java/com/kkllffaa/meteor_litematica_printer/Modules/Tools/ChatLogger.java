package com.kkllffaa.meteor_litematica_printer.Modules.Tools;

import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.text.Text;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.kkllffaa.meteor_litematica_printer.Addon;

public class ChatLogger extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<String> filePath = sgGeneral.add(new StringSetting.Builder()
        .name("file-path")
        .description("The file path to save chat logs. Use absolute path or relative to game directory.")
        .defaultValue("MySet/chat_logs.txt")
        .build()
    );

    public ChatLogger() {
        super(Addon.TOOLS, "chat-logger", "Logs chat messages to a file.");
    }

    @EventHandler
    private void onReceiveMessage(ReceiveMessageEvent event) {
        Text message = event.getMessage();
        if (message == null) return;
        String messageString = message.getString();
        if (messageString == null || messageString.trim().isEmpty()) return;
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        String logEntry = String.format("[%s]%s%n", timestamp, messageString);

        String path = filePath.get();
        if (path == null || path.trim().isEmpty()) return;

        try {
            Path logFilePath = Paths.get(path);
            if (logFilePath.getParent() != null && !Files.exists(logFilePath.getParent())) {
                Files.createDirectories(logFilePath.getParent());
            }
            try (PrintWriter writer = new PrintWriter(new FileWriter(logFilePath.toFile(), true))) {
                writer.print(logEntry);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}