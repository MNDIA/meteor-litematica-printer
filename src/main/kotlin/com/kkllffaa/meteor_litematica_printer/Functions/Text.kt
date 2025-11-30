package com.kkllffaa.meteor_litematica_printer.Functions

import com.kkllffaa.meteor_litematica_printer.Modules.CRUD.AtomicSettings.PlaceSettings
import net.minecraft.text.StringVisitable.StyledVisitor
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.text.TextColor
import net.minecraft.util.Formatting
import java.util.Locale


fun getFormattedLine(text: Text): String {
    val mode = PlaceSettings.SignTextWithColor.get()
    if (mode == SignColorMode.None) {
        return text.string
    }

    val controlChar = if (mode == SignColorMode.反三) '§' else '&'
    var lastStyle = Style.EMPTY
    val builder = StringBuilder()

    text.visit(StyledVisitor<Unit> { style, string ->
        if (style != lastStyle) {
            if (!lastStyle.isEmpty) {
                builder.append(controlChar).append('r')
            }
            appendStyleCodes(builder, style, controlChar)
            lastStyle = style
        }
        builder.append(string)
        java.util.Optional.empty<Unit>()
    }, Style.EMPTY)

    return builder.toString()
}


private fun appendStyleCodes(builder: StringBuilder, style: Style, controlChar: Char) {
    style.color?.let { appendColorCode(builder, it, controlChar) }
    if (style.isObfuscated) builder.append(controlChar).append('k')
    if (style.isBold) builder.append(controlChar).append('l')
    if (style.isStrikethrough) builder.append(controlChar).append('m')
    if (style.isUnderlined) builder.append(controlChar).append('n')
    if (style.isItalic) builder.append(controlChar).append('o')
}


private fun appendColorCode(builder: StringBuilder, color: TextColor, controlChar: Char) {
    getVanillaFormatting(color)?.let {
        builder.append(controlChar).append(it.code)
        return
    }

    val hex = "%06X".format(Locale.ROOT, color.rgb)
    builder.append(controlChar).append('x')
    hex.forEach { builder.append(controlChar).append(it.lowercaseChar()) }
}


private fun getVanillaFormatting(color: TextColor): Formatting? =
    Formatting.entries.find { it.colorValue == color.rgb }
