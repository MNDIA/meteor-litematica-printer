package com.kkllffaa.meteor_litematica_printer.Functions

import meteordevelopment.meteorclient.utils.render.color.SettingColor
import kotlin.random.Random.Default

enum class SignColorMode {
    None,
    `§`,
    `&`
}

enum class SafetyFaceMode {
    PlayerRotation,
    PlayerPosition,  // 射线方向
    None,
}

enum class ColorScheme(val sideColor: SettingColor, val lineColor: SettingColor) {
    红(SettingColor(204, 0, 0, 10), SettingColor(204, 0, 0, 255)),
    绿(SettingColor(0, 204, 0, 10), SettingColor(0, 204, 0, 255)),
    蓝(SettingColor(0, 0, 204, 10), SettingColor(0, 0, 204, 255)),
    黄(SettingColor(204, 204, 0, 10), SettingColor(204, 204, 0, 255)),
    紫(SettingColor(204, 0, 204, 10), SettingColor(204, 0, 204, 255)),
    青(SettingColor(0, 204, 204, 10), SettingColor(0, 204, 204, 255))
}

enum class RandomDelayMode(private val delays: IntArray?) {
    None(null),
    Fast(intArrayOf(0, 0, 1)),
    Balanced(intArrayOf(0, 0, 0, 0, 1, 1, 1, 2, 2, 3)),
    Slow(intArrayOf(0, 0, 1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 4, 5, 6)),
    Variable(intArrayOf(0, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10));


    val theDelay get() = delays?.get(Default.nextInt(delays.size)) ?: 0
}

enum class DistanceMode {
    Auto,
    Max,
}

enum class ProtectMode {
    Off,
    ReferencePlayerY,
    ReferenceWorldY
}

enum class ListMode {
    Whitelist,
    Blacklist,
    None
}


enum class ActionMode {
    None,
    SendPacket,
    Normal
}

enum class SafetyFace {
    PlayerRotation,
    PlayerPosition,
}

enum class PreferPerspective {
    NONE,
    FIRST_PERSON,
    THIRD_PERSON_BACK,
}
