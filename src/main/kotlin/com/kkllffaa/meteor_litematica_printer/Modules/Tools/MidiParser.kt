package com.kkllffaa.meteor_litematica_printer.Modules.Tools

import com.kkllffaa.meteor_litematica_printer.Addon
import meteordevelopment.meteorclient.settings.BoolSetting
import meteordevelopment.meteorclient.settings.Setting
import meteordevelopment.meteorclient.settings.StringSetting
import meteordevelopment.meteorclient.systems.modules.Module

class MidiParser : Module(Addon.TOOLS, "midi-parser", "Parses MIDI files.") {
    private val sgGeneral = settings.defaultGroup

    private val midiFilePath: Setting<String> = sgGeneral.add(
        StringSetting.Builder()
            .name("midi-file-path")
            .description("Path to the MIDI file to parse.")
            .defaultValue("")
            .build()
    )

    private val parseMidi: Setting<Boolean> = sgGeneral.add(
        BoolSetting.Builder()
            .name("parse-midi")
            .description("Click to parse the MIDI file.")
            .defaultValue(false)
            .onChanged { value: Boolean -> this.onParseMidiChanged(value) }
            .build()
    )

    private fun onParseMidiChanged(value: Boolean) {
        if (value) {
            parseMidiFile()
            parseMidi.set(false) // reset
        }
    }

    private fun parseMidiFile() {
        val path = midiFilePath.get()
        if (path.isEmpty()) {
            error("MIDI file path is empty.")
            return
        }
        info("Parsing MIDI file: $path")
    }
}
