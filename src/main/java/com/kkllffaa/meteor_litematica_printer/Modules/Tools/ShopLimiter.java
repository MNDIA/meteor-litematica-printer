package com.kkllffaa.meteor_litematica_printer.Modules.Tools;

import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.text.Text;

import com.kkllffaa.meteor_litematica_printer.Addon;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ShopLimiter extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<String> messagePattern = sgGeneral.add(new StringSetting.Builder()
        .name("message-pattern")
        .description("Pattern to match purchase messages, using %player%, %count%, %item% as placeholders. Only write the key part that identifies purchases.")
        .defaultValue(" %player% 向你的商店购买 %count% 个 %item%，")
        .build()
    );

    private final Setting<String> resetTime = sgGeneral.add(new StringSetting.Builder()
        .name("reset-time")
        .description("Time to reset daily statistics (HH:mm format).")
        .defaultValue("00:00")
        .build()
    );

    private final Setting<String> settingsFilePath = sgGeneral.add(new StringSetting.Builder()
        .name("settings-file-path")
        .description("Path to the settings file.")
        .defaultValue("MySet/shop_limiter_settings.json")
        .build()
    );

    private final Setting<String> dataFilePath = sgGeneral.add(new StringSetting.Builder()
        .name("data-file-path")
        .description("Path to the data file.")
        .defaultValue("MySet/shop_limiter_data.json")
        .build()
    );

    private final Setting<List<String>> removeCommand = sgGeneral.add(new StringListSetting.Builder()
        .name("remove-command")
        .description("Commands to remove player from territory, use %player% as placeholder.")
        .defaultValue("/res pset %player% tp false")
        .build()
    );

    private final Setting<Boolean> INFO = sgGeneral.add(new BoolSetting.Builder()
        .name("info")
        .description("Show info messages when auto login is triggered.")
        .defaultValue(true)
        .build()
    );
    
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
    private static final DateTimeFormatter RESET_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final Type LIMITS_TYPE = new TypeToken<Map<String, Integer>>(){}.getType();

    private static class LocalDateTimeAdapter implements JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {
        @Override
        public JsonElement serialize(LocalDateTime src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.toString());
        }

        @Override
        public LocalDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return LocalDateTime.parse(json.getAsString());
        }
    }

    private final Gson gson = new GsonBuilder()
        .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
        .setPrettyPrinting()
        .create();

    private static class Data {
        LocalDateTime lastReset = LocalDateTime.now();
        Map<String, Map<String, Integer>> playerStats = new HashMap<>();
    }

    public ShopLimiter() {
        super(Addon.TOOLS, "shop-limiter", "Limits shop purchases and tracks statistics.");
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        WVerticalList list = theme.verticalList();
        WButton execute = list.add(theme.button("立刻重置时间")).expandX().widget();
        execute.action = () -> 手动重置();
        return list;
    }

    @EventHandler
    private void onReceiveMessage(ReceiveMessageEvent event) {
        Text originalText = event.getMessage();
        MessageIndicator originalIndicator = event.getIndicator();
        int originalId = event.id;

        String message = originalText.getString();

        String regex = messagePattern.get()
            .replace("%player%", "(.+)")
            .replace("%count%", "(\\d+)")
            .replace("%item%", "(.+)");
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(message);
        if (matcher.find()) {
            String player = matcher.group(1);
            int count = Integer.parseInt(matcher.group(2));
            String item = matcher.group(3);
            processPurchase(player, count, item);

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
    }
    
    private void processPurchase(String player, int count, String item) {
        Data data = getData();
        Map<String, Integer> stats = data.playerStats.computeIfAbsent(player, ignored -> new HashMap<>());
        int newTotal = stats.merge(item, count, Integer::sum);
        setData(data);

        info(player + " 买" + count + "个" + item + " 总计：" + newTotal);

        Integer limit = getItemLimits().get(item);
        if (limit != null && limit > 0 && newTotal > limit) {
            info("玩家 " + player + " 超过了 " + item + " 的限制 (" + newTotal + "/" + limit + ")");
            removePlayer(player);
        }
    }

    private void removePlayer(String player) {
        removeCommand.get().stream()
            .map(cmd -> cmd.replace("%player%", player))
            .forEach(command -> {
                ChatUtils.sendPlayerMsg(command);
                info("发送命令 " + player + "：" + command);
            });
    }

    private void 手动重置() {
        Data data = getData();
        data.playerStats.clear();
        data.lastReset = LocalDateTime.now();
        setData(data);
        info("已手动重置今日统计。");
    }

    private Data getData() {
        Data data = dataPath()
            .flatMap(path -> readJson(path, Data.class))
            .map(this::normalizeData)
            .orElseGet(Data::new);

        if (refreshReset(data)) {
            setData(data);
        }
        return data;
    }

    private Data normalizeData(Data data) {
        if (data.lastReset == null) {
            data.lastReset = LocalDateTime.now();
        }
        if (data.playerStats == null) {
            data.playerStats = new HashMap<>();
        }
        return data;
    }

    private void setData(Data data) {
        dataPath().ifPresent(path -> writeJson(path, data));
    }

    private Map<String, Integer> getItemLimits() {
        return settingsPath()
            .map(this::ensureSettingsFile)
            .flatMap(path -> this.<Map<String, Integer>>readJson(path, LIMITS_TYPE))
            .map(this::stableLimits)
            .orElseGet(Map::of);
    }

    private Path ensureSettingsFile(Path path) {
        if (Files.exists(path)) {
            return path;
        }

        Map<String, Integer> defaults = new HashMap<>();
        defaults.put("物品名", 999);
        writeJson(path, defaults);

        if (Files.exists(path)) {
            info("已生成默认的限购配置 " + path + "，请根据实际需求调整。");
        }

        return path;
    }

    private boolean refreshReset(Data data) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayReset = now.toLocalDate().atTime(resetAt());
        if (now.isAfter(todayReset) && data.lastReset.isBefore(todayReset)) {
            data.playerStats.clear();
            data.lastReset = now;
            info("每日统计在 " + now + " 重置");
            return true;
        }
        return false;
    }

    private LocalTime resetAt() {
        try {
            return LocalTime.parse(resetTime.get(), RESET_FORMAT);
        } catch (DateTimeParseException e) {
            info("重置时间格式无效，已使用 00:00。");
            return LocalTime.MIDNIGHT;
        }
    }

    private Map<String, Integer> stableLimits(Map<String, Integer> limits) {
        if (limits == null || limits.isEmpty()) {
            return Map.of();
        }
        return limits.entrySet().stream()
            .filter(entry -> entry.getKey() != null && entry.getValue() != null)
            .collect(Collectors.collectingAndThen(
                Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (first, second) -> second, HashMap::new),
                Collections::unmodifiableMap
            ));
    }

    private Optional<Path> dataPath() {
        return resolvePath(dataFilePath.get());
    }

    private Optional<Path> settingsPath() {
        return resolvePath(settingsFilePath.get());
    }

    private Optional<Path> resolvePath(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            return Optional.empty();
        }
        try {
            Path path = Paths.get(rawPath);
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            return Optional.of(path);
        } catch (InvalidPathException | IOException e) {
            info("无法解析路径 " + rawPath + "：" + e.getMessage());
            return Optional.empty();
        }
    }

    private <T> Optional<T> readJson(Path path, Class<T> type) {
        return readJson(path, (Type) type);
    }

    private <T> Optional<T> readJson(Path path, Type type) {
        if (!Files.exists(path)) {
            return Optional.empty();
        }
        try {
            String content = Files.readString(path);
            if (content.isBlank()) {
                return Optional.empty();
            }
            return Optional.ofNullable(gson.fromJson(content, type));
        } catch (IOException e) {
            info("读取 " + path + " 失败：" + e.getMessage());
            return Optional.empty();
        }
    }

    private void writeJson(Path path, Object value) {
        try {
            Files.writeString(path, gson.toJson(value));
        } catch (IOException e) {
            info("无法保存数据到 " + path + "：" + e.getMessage());
        }
    }
}
