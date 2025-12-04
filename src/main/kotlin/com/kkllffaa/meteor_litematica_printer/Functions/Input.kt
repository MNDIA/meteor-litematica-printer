package com.kkllffaa.meteor_litematica_printer.Functions


import meteordevelopment.meteorclient.utils.misc.input.Input
import net.minecraft.client.option.KeyBinding

fun 恢复按键到物理状态(vararg keys: KeyBinding) {
    keys.forEach { it.isPressed = Input.isPressed(it) }
}

fun 松开按键(vararg keys: KeyBinding) {
    keys.forEach { it.isPressed = false }
}

fun 按下按键(vararg keys: KeyBinding) {
    keys.forEach { it.isPressed = true }
}