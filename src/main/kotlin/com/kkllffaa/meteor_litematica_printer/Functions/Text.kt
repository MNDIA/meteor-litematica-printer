package com.kkllffaa.meteor_litematica_printer.Functions

import com.kkllffaa.meteor_litematica_printer.Modules.CRUD.AtomicSettings.PlaceSettings
import net.minecraft.text.*
import net.minecraft.util.Formatting
import java.util.*

fun getFormattedLine(text: Text): String {
    val mode = PlaceSettings.SignTextWithColor.get()
    val controlChar = when (mode) {
        SignColorMode.None -> return text.string
        SignColorMode.`§` -> '§'
        SignColorMode.`&` -> '&'
    }

    val result = StringBuilder()
    var lastStyle: Style? = null

    text.visit({ style, content ->
        if (content.isNotEmpty()) {
            if (style != lastStyle) {
                result.append(styleToFormattingCodes(style, controlChar))
                lastStyle = style
            }
            result.append(content)
        }
        Optional.empty<Unit>()
    }, Style.EMPTY)

    return result.toString()
}


private fun styleToFormattingCodes(style: Style, controlChar: Char): String {
    if (style.isEmpty) return ""

    val codes = StringBuilder()

    style.color?.let { textColor ->
        val formatting = getFormattingFromColor(textColor)
        if (formatting != null) {
            codes.append(controlChar).append(formatting.code)
        }
    }

    if (style.isBold) {
        codes.append(controlChar).append(Formatting.BOLD.code)
    }
    if (style.isItalic) {
        codes.append(controlChar).append(Formatting.ITALIC.code)
    }
    if (style.isUnderlined) {
        codes.append(controlChar).append(Formatting.UNDERLINE.code)
    }
    if (style.isStrikethrough) {
        codes.append(controlChar).append(Formatting.STRIKETHROUGH.code)
    }
    if (style.isObfuscated) {
        codes.append(controlChar).append(Formatting.OBFUSCATED.code)
    }

    return codes.toString()
}


private fun getFormattingFromColor(textColor: TextColor): Formatting? {
    for (formatting in Formatting.entries) {
        if (formatting.isColor) {
            val colorValue = formatting.colorValue
            if (colorValue != null && colorValue == textColor.rgb) {
                return formatting
            }
        }
    }
    return null
}