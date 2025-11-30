package com.kkllffaa.meteor_litematica_printer.Modules.Tools

import com.kkllffaa.meteor_litematica_printer.Addon
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent
import meteordevelopment.meteorclient.settings.StringSetting
import meteordevelopment.meteorclient.systems.modules.Module
import meteordevelopment.orbit.EventHandler
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ChatLogger : Module(Addon.TOOLS, "chat-logger", "Logs chat messages to a file.") {
    private val sgGeneral = settings.defaultGroup

    private val filePath = sgGeneral.add(
        StringSetting.Builder()
            .name("file-path")
            .description("The file path to save chat logs. Use absolute path or relative to game directory.")
            .defaultValue("MySet/chat_logs.txt")
            .build()
    )

    @EventHandler
    private fun onReceiveMessage(event: ReceiveMessageEvent) {
        val messageString = event.message?.string?.takeIf { it.isNotBlank() } ?: return
        val path = filePath.get()?.takeIf { it.isNotBlank() } ?: return

        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        val logEntry = "[$timestamp]$messageString\n"

        // runCatching {
        File(path).apply { parentFile?.mkdirs() }.appendText(logEntry)
        // }.onFailure { it.printStackTrace() }
    }
}
