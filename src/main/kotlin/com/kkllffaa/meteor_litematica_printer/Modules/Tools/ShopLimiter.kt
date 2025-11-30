package com.kkllffaa.meteor_litematica_printer.Modules.Tools

import com.kkllffaa.meteor_litematica_printer.Addon
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent
import meteordevelopment.meteorclient.gui.GuiTheme
import meteordevelopment.meteorclient.gui.widgets.WWidget
import meteordevelopment.meteorclient.settings.*
import meteordevelopment.meteorclient.systems.modules.Module
import meteordevelopment.meteorclient.utils.player.ChatUtils
import meteordevelopment.orbit.EventHandler
import net.minecraft.text.Text
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

object ShopLimiter : Module(Addon.TOOLS, "shop-limiter", "Limits shop purchases and tracks statistics.") {
    private val sgGeneral = settings.defaultGroup

    private val messagePattern: Setting<String> = sgGeneral.add(
        StringSetting.Builder()
            .name("message-pattern")
            .description("Pattern to match purchase messages, using %player%, %count%, %item% as placeholders. Only write the key part that identifies purchases.")
            .defaultValue(" %player% 向你的商店购买 %count% 个 %item%，")
            .build()
    )

    private val resetTime: Setting<String> = sgGeneral.add(
        StringSetting.Builder()
            .name("reset-time")
            .description("Time to reset daily statistics (HH:mm format).")
            .defaultValue("00:00")
            .build()
    )

    private val settingsFilePath: Setting<String> = sgGeneral.add(
        StringSetting.Builder()
            .name("settings-file-path")
            .description("Path to the settings file.")
            .defaultValue("MySet/shop_limiter_settings.json")
            .build()
    )

    private val dataFilePath: Setting<String> = sgGeneral.add(
        StringSetting.Builder()
            .name("data-file-path")
            .description("Path to the data file.")
            .defaultValue("MySet/shop_limiter_data.json")
            .build()
    )

    private val removeCommand: Setting<MutableList<String>> = sgGeneral.add(
        StringListSetting.Builder()
            .name("remove-command")
            .description("Commands to remove player from territory, use %player% as placeholder.")
            .defaultValue("/res pset %player% tp false")
            .build()
    )

    private val INFO: Setting<Boolean> = sgGeneral.add(
        BoolSetting.Builder()
            .name("info")
            .description("Show info messages when auto is triggered.")
            .defaultValue(true)
            .build()
    )

    override fun info(message: String, vararg args: Any) {
        if (INFO.get()) {
            super.info(message, *args)
        }
    }

    override fun info(message: Text) {
        if (INFO.get()) {
            super.info(message)
        }
    }

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true      // 忽略文件中的未知字段
        coerceInputValues = true      // 遇到 null 时使用默认值
        encodeDefaults = true         // 编码默认值
    }

    private object LocalDateTimeSerializer : KSerializer<LocalDateTime> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LocalDateTime", PrimitiveKind.STRING)
        override fun serialize(encoder: Encoder, value: LocalDateTime) = encoder.encodeString(value.toString())
        override fun deserialize(decoder: Decoder): LocalDateTime = LocalDateTime.parse(decoder.decodeString())
    }

    @Serializable
    private data class Data(
        @Serializable(with = LocalDateTimeSerializer::class)
        var lastReset: LocalDateTime = LocalDateTime.now(),
        var playerStats: MutableMap<String, MutableMap<String, Int>> = mutableMapOf()
    )

    override fun getWidget(theme: GuiTheme): WWidget {
        val list = theme.verticalList()
        val execute = list.add(theme.button("立刻重置时间")).expandX().widget()
        execute.action = Runnable { manualReset() }
        return list
    }

    @EventHandler
    private fun onReceiveMessage(event: ReceiveMessageEvent) {
        val originalText = event.message
        // val originalIndicator = event.indicator
        // val originalId = event.id

        val message = originalText.string

        val regex = messagePattern.get()
            .replace("%player%", "(.+)")
            .replace("%count%", "(\\d+)")
            .replace("%item%", "(.+)")

        val matchResult = Regex(regex).find(message) ?: return

        val (player, countStr, item) = matchResult.destructured
        val count = countStr.toIntOrNull() ?: return

        processPurchase(player, count, item)

        // 恢复原始消息属性
        // if (event.message !== originalText) {
        //     event.message = originalText
        // }
        // if (event.indicator !== originalIndicator) {
        //     event.indicator = originalIndicator
        // }
        // if (event.id != originalId) {
        //     event.id = originalId
        // }
    }

    private fun processPurchase(player: String, count: Int, item: String) {
        // 每次都从文件读取最新数据
        val data = readData()
        val playerItems = data.playerStats.getOrPut(player) { mutableMapOf() }
        val currentCount = playerItems[item] ?: 0
        val newTotal = currentCount + count
        playerItems[item] = newTotal

        // 立即写回文件
        writeData(data)

        val limit: Int? = getItemLimits()[item]
        val 限量 = limit != null && limit > 0

        info("$player 买${count}个$item 总计：$newTotal/${if (限量) limit else "不限量"}")
        if (限量 && newTotal > limit) {
            info("玩家 $player 超过了 $item 的限制 ($newTotal/$limit)")
            removePlayer(player)
        }
    }

    private fun removePlayer(player: String) {
        removeCommand.get().forEach { cmd ->
            val command = cmd.replace("%player%", player)
            ChatUtils.sendPlayerMsg(command)
            info("发送命令 $player：$command")
        }
    }

    private fun manualReset() {
        val data = Data(
            lastReset = LocalDateTime.now(),
            playerStats = mutableMapOf()
        )
        writeData(data)
        info("已手动重置今日统计。")
    }


    private fun readData(): Data {
        val path = resolvePath(dataFilePath.get()) ?: return Data()
        val data = readJson<Data>(path) ?: return Data()

        // 检查是否需要自动重置
        if (checkAndResetIfNeeded(data)) {
            writeData(data)
        }

        return data
    }

    private fun writeData(data: Data) {
        val path = resolvePath(dataFilePath.get()) ?: return
        writeJson(path, data)
    }

    private fun getItemLimits(): Map<String, Int> {
        val path = resolvePath(settingsFilePath.get()) ?: return emptyMap()
        ensureSettingsFile(path)
        return readJson<Map<String, Int>>(path) ?: emptyMap()
    }

    private fun ensureSettingsFile(path: Path) {
        if (Files.exists(path)) return

        val defaults = mapOf("物品名" to 999)
        writeJson(path, defaults)

        if (Files.exists(path)) {
            info("已生成默认的限购配置 $path，请根据实际需求调整。")
        }
    }

    private fun checkAndResetIfNeeded(data: Data): Boolean {
        val now = LocalDateTime.now()
        val resetAtTime = parseResetTime()
        val todayReset = now.toLocalDate().atTime(resetAtTime)

        if (now.isAfter(todayReset) && data.lastReset.isBefore(todayReset)) {
            data.playerStats.clear()
            data.lastReset = now
            info("每日统计在 $now 重置")
            return true
        }
        return false
    }

    private fun parseResetTime(): LocalTime {
        return try {
            LocalTime.parse(resetTime.get(), RESET_FORMAT)
        } catch (_: Exception) {
            info("重置时间格式无效，已使用 00:00。")
            LocalTime.MIDNIGHT
        }
    }

    private fun resolvePath(rawPath: String?): Path? {
        if (rawPath.isNullOrBlank()) return null

        return try {
            val path = Paths.get(rawPath)
            path.parent?.let { Files.createDirectories(it) }
            path
        } catch (e: Exception) {
            info("无法解析路径 $rawPath：${e.message}")
            null
        }
    }

    // 使用 reified 类型参数，kotlinx.serialization 可以自动推断类型
    private inline fun <reified T> readJson(path: Path): T? {
        if (!Files.exists(path)) return null

        return try {
            val content = Files.readString(path)
            if (content.isBlank()) return null
            json.decodeFromString<T>(content)
        } catch (e: Exception) {
            // 任何读取失败都视为 null
            info("读取 $path 失败：${e.message}")
            null
        }
    }

    private inline fun <reified T> writeJson(path: Path, value: T) {
        try {
            Files.writeString(path, json.encodeToString(value))
        } catch (e: Exception) {
            info("无法保存数据到 $path：${e.message}")
        }
    }

    private val RESET_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
}
