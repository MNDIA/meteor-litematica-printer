package com.kkllffaa.meteor_litematica_printer.Modules.Tools

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import com.kkllffaa.meteor_litematica_printer.Addon
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent
import meteordevelopment.meteorclient.gui.GuiTheme
import meteordevelopment.meteorclient.gui.widgets.WWidget
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton
import meteordevelopment.meteorclient.settings.*
import meteordevelopment.meteorclient.systems.modules.Module
import meteordevelopment.meteorclient.utils.player.ChatUtils
import meteordevelopment.orbit.EventHandler
import net.minecraft.text.Text
import java.io.IOException
import java.lang.reflect.Type
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.*
import java.util.Map
import java.util.function.BinaryOperator
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Supplier
import java.util.regex.Pattern
import java.util.stream.Collectors

class ShopLimiter : Module(Addon.TOOLS, "shop-limiter", "Limits shop purchases and tracks statistics.") {
    private val sgGeneral: SettingGroup = settings.getDefaultGroup()

    private val messagePattern: Setting<String?> = sgGeneral.add<String?>(
        StringSetting.Builder()
            .name("message-pattern")
            .description("Pattern to match purchase messages, using %player%, %count%, %item% as placeholders. Only write the key part that identifies purchases.")
            .defaultValue(" %player% 向你的商店购买 %count% 个 %item%，")
            .build()
    )

    private val resetTime: Setting<String?> = sgGeneral.add<String?>(
        StringSetting.Builder()
            .name("reset-time")
            .description("Time to reset daily statistics (HH:mm format).")
            .defaultValue("00:00")
            .build()
    )

    private val settingsFilePath: Setting<String?> = sgGeneral.add<String?>(
        StringSetting.Builder()
            .name("settings-file-path")
            .description("Path to the settings file.")
            .defaultValue("MySet/shop_limiter_settings.json")
            .build()
    )

    private val dataFilePath: Setting<String?> = sgGeneral.add<String?>(
        StringSetting.Builder()
            .name("data-file-path")
            .description("Path to the data file.")
            .defaultValue("MySet/shop_limiter_data.json")
            .build()
    )

    private val removeCommand: Setting<MutableList<String?>?> = sgGeneral.add<MutableList<String?>?>(
        StringListSetting.Builder()
            .name("remove-command")
            .description("Commands to remove player from territory, use %player% as placeholder.")
            .defaultValue("/res pset %player% tp false")
            .build()
    )

    private val INFO: Setting<Boolean?> = sgGeneral.add<Boolean?>(
        BoolSetting.Builder()
            .name("info")
            .description("Show info messages when auto login is triggered.")
            .defaultValue(true)
            .build()
    )

    override fun info(message: String?, vararg args: Any?) {
        if (INFO.get()) {
            super.info(message, *args)
        }
    }

    override fun info(message: Text?) {
        if (INFO.get()) {
            super.info(message)
        }
    }

    private class LocalDateTimeAdapter : JsonSerializer<LocalDateTime?>, JsonDeserializer<LocalDateTime?> {
        override fun serialize(src: LocalDateTime, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
            return JsonPrimitive(src.toString())
        }

        @Throws(JsonParseException::class)
        override fun deserialize(
            json: JsonElement,
            typeOfT: Type?,
            context: JsonDeserializationContext?
        ): LocalDateTime {
            return LocalDateTime.parse(json.getAsString())
        }
    }

    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeAdapter())
        .setPrettyPrinting()
        .create()

    private class Data {
        var lastReset: LocalDateTime? = LocalDateTime.now()
        var playerStats: MutableMap<String?, MutableMap<String?, Int?>>? = HashMap<String?, MutableMap<String?, Int?>>()
    }

    override fun getWidget(theme: GuiTheme): WWidget {
        val list = theme.verticalList()
        val execute = list.add<WButton>(theme.button("立刻重置时间")).expandX().widget()
        execute.action = Runnable { 手动重置() }
        return list
    }

    @EventHandler
    private fun onReceiveMessage(event: ReceiveMessageEvent) {
        val originalText = event.getMessage()
        val originalIndicator = event.getIndicator()
        val originalId = event.id

        val message = originalText.getString()

        val regex = messagePattern.get()!!
            .replace("%player%", "(.+)")
            .replace("%count%", "(\\d+)")
            .replace("%item%", "(.+)")
        val pattern = Pattern.compile(regex)
        val matcher = pattern.matcher(message)
        if (matcher.find()) {
            val player = matcher.group(1)
            val count = matcher.group(2).toInt()
            val item = matcher.group(3)
            processPurchase(player, count, item)

            if (event.getMessage() !== originalText) {
                event.setMessage(originalText)
            }
            if (event.getIndicator() !== originalIndicator) {
                event.setIndicator(originalIndicator)
            }
            if (event.id != originalId) {
                event.id = originalId
            }
        }
    }

    private fun processPurchase(player: String, count: Int, item: String?) {
        val data = this.data!!
        val stats = data.playerStats!!.computeIfAbsent(player) { ignored: String? -> HashMap<String?, Int?>() }
        val newTotal: Int = stats.merge(item, count) { a: Int?, b: Int? -> Integer.sum(a!!, b!!) }!!
        this.data = data

        info(player + " 买" + count + "个" + item + " 总计：" + newTotal)

        val limit = this.itemLimits.get(item)
        if (limit != null && limit > 0 && newTotal > limit) {
            info("玩家 " + player + " 超过了 " + item + " 的限制 (" + newTotal + "/" + limit + ")")
            removePlayer(player)
        }
    }

    private fun removePlayer(player: String) {
        removeCommand.get()!!.stream()
            .map<String?> { cmd: String? -> cmd!!.replace("%player%", player) }
            .forEach { command: String? ->
                ChatUtils.sendPlayerMsg(command)
                info("发送命令 " + player + "：" + command)
            }
    }

    private fun 手动重置() {
        val data = this.data!!
        data.playerStats!!.clear()
        data.lastReset = LocalDateTime.now()
        this.data = data
        info("已手动重置今日统计。")
    }

    private var data: Data?
        get() {
            val data = dataPath()
                .flatMap<Data?>(Function { path: Path? ->
                    readJson<Data?>(
                        path!!,
                        Data::class.java
                    )
                })
                .map<Data>(Function { data: Data? ->
                    this.normalizeData(
                        data!!
                    )
                })
                .orElseGet(Supplier { Data() })

            if (refreshReset(data)) {
                this.data = data
            }
            return data
        }
        private set(data) {
            dataPath().ifPresent(Consumer { path: Path? -> writeJson(path!!, data) })
        }

    private fun normalizeData(data: Data): Data {
        if (data.lastReset == null) {
            data.lastReset = LocalDateTime.now()
        }
        if (data.playerStats == null) {
            data.playerStats = HashMap<String?, MutableMap<String?, Int?>>()
        }
        return data
    }

    private val itemLimits: MutableMap<String?, Int?>
        get() = settingsPath()
            .map<Path?>(Function { path: Path? ->
                this.ensureSettingsFile(
                    path!!
                )
            })
            .flatMap<MutableMap<String?, Int?>?>(Function { path: Path? ->
                this.readJson<MutableMap<String?, Int?>?>(
                    path!!,
                    LIMITS_TYPE
                )
            })
            .map<MutableMap<String?, Int?>>(Function { limits: MutableMap<String?, Int?>? ->
                this.stableLimits(
                    limits
                )
            })
            .orElseGet(Supplier { Map.of() })

    private fun ensureSettingsFile(path: Path): Path {
        if (Files.exists(path)) {
            return path
        }

        val defaults: MutableMap<String?, Int?> = HashMap<String?, Int?>()
        defaults.put("物品名", 999)
        writeJson(path, defaults)

        if (Files.exists(path)) {
            info("已生成默认的限购配置 " + path + "，请根据实际需求调整。")
        }

        return path
    }

    private fun refreshReset(data: Data): Boolean {
        val now = LocalDateTime.now()
        val todayReset = now.toLocalDate().atTime(resetAt())
        if (now.isAfter(todayReset) && data.lastReset!!.isBefore(todayReset)) {
            data.playerStats!!.clear()
            data.lastReset = now
            info("每日统计在 " + now + " 重置")
            return true
        }
        return false
    }

    private fun resetAt(): LocalTime? {
        try {
            return LocalTime.parse(resetTime.get(), RESET_FORMAT)
        } catch (e: DateTimeParseException) {
            info("重置时间格式无效，已使用 00:00。")
            return LocalTime.MIDNIGHT
        }
    }

    private fun stableLimits(limits: MutableMap<String?, Int?>?): MutableMap<String?, Int?>? {
        if (limits == null || limits.isEmpty()) {
            return Map.of<String?, Int?>()
        }
        return limits.entries.stream()
            .filter { entry: MutableMap.MutableEntry<String?, Int?>? -> entry!!.key != null && entry.value != null }
            .collect(
                Collectors.collectingAndThen(
                    Collectors.toMap(
                        Function { Map.Entry.key },
                        Function { Map.Entry.value },
                        BinaryOperator { first: Int?, second: Int? -> second },
                        Supplier { HashMap() }),
                    Function { m: HashMap<String?, Int?>? -> Collections.unmodifiableMap(m) }
                ))
    }

    private fun dataPath(): Optional<Path?> {
        return resolvePath(dataFilePath.get())
    }

    private fun settingsPath(): Optional<Path?> {
        return resolvePath(settingsFilePath.get())
    }

    private fun resolvePath(rawPath: String?): Optional<Path?> {
        if (rawPath == null || rawPath.isBlank()) {
            return Optional.empty<Path?>()
        }
        try {
            val path = Paths.get(rawPath)
            val parent = path.getParent()
            if (parent != null) {
                Files.createDirectories(parent)
            }
            return Optional.of<Path?>(path)
        } catch (e: InvalidPathException) {
            info("无法解析路径 " + rawPath + "：" + e.message)
            return Optional.empty<Path?>()
        } catch (e: IOException) {
            info("无法解析路径 " + rawPath + "：" + e.message)
            return Optional.empty<Path?>()
        }
    }

    private fun <T> readJson(path: Path, type: Class<T?>?): Optional<T?> {
        return readJson<T?>(path, (type as java.lang.reflect.Type?)!!)
    }

    private fun <T> readJson(path: Path, type: Type): Optional<T?> {
        if (!Files.exists(path)) {
            return Optional.empty<T?>()
        }
        try {
            val content = Files.readString(path)
            if (content.isBlank()) {
                return Optional.empty<T?>()
            }
            return Optional.ofNullable<T?>(gson.fromJson<T?>(content, type))
        } catch (e: IOException) {
            info("读取 " + path + " 失败：" + e.message)
            return Optional.empty<T?>()
        }
    }

    private fun writeJson(path: Path, value: Any?) {
        try {
            Files.writeString(path, gson.toJson(value))
        } catch (e: IOException) {
            info("无法保存数据到 " + path + "：" + e.message)
        }
    }

    companion object {
        private val RESET_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        private val LIMITS_TYPE: Type = object : TypeToken<MutableMap<String?, Int?>?>() {}.getType()
    }
}
