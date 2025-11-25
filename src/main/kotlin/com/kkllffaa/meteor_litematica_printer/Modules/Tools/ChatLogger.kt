package com.kkllffaa.meteor_litematica_printer.Modules.Tools

import com.kkllffaa.meteor_litematica_printer.Addon
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent
import meteordevelopment.meteorclient.settings.Setting
import meteordevelopment.meteorclient.settings.SettingGroup
import meteordevelopment.meteorclient.settings.StringSetting
import meteordevelopment.meteorclient.systems.modules.Module
import meteordevelopment.orbit.EventHandler
import java.io.FileWriter
import java.io.IOException
import java.io.PrintWriter
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ChatLogger : Module(Addon.TOOLS, "chat-logger", "Logs chat messages to a file.") {
    private val sgGeneral: SettingGroup = settings.getDefaultGroup()

    private val filePath: Setting<String?> = sgGeneral.add<String?>(
        StringSetting.Builder()
            .name("file-path")
            .description("The file path to save chat logs. Use absolute path or relative to game directory.")
            .defaultValue("MySet/chat_logs.txt")
            .build()
    )

    @EventHandler
    private fun onReceiveMessage(event: ReceiveMessageEvent) {
        val message = event.getMessage()
        if (message == null) return
        val messageString = message.getString()
        if (messageString == null || messageString.trim { it <= ' ' }.isEmpty()) return
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

        val logEntry = String.format("[%s]%s%n", timestamp, messageString)

        val path = filePath.get()
        if (path == null || path.trim { it <= ' ' }.isEmpty()) return

        try {
            val logFilePath = Paths.get(path)
            if (logFilePath.getParent() != null && !Files.exists(logFilePath.getParent())) {
                Files.createDirectories(logFilePath.getParent())
            }
            PrintWriter(FileWriter(logFilePath.toFile(), true)).use { writer ->
                writer.print(logEntry)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
