package com.kkllffaa.meteor_litematica_printer.Modules.Tools;

import com.kkllffaa.meteor_litematica_printer.Addon;

import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;



public class MidiParser extends Module {

    public MidiParser() {
        super(Addon.TOOLSCATEGORY, "midi-parser", "Parses MIDI files.");
    }
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<String> midiFilePath = sgGeneral.add(new StringSetting.Builder()
        .name("midi-file-path")
        .description("Path to the MIDI file to parse.")
        .defaultValue("")
        .build()
    );

    private final Setting<Boolean> parseMidi = sgGeneral.add(new BoolSetting.Builder()
        .name("parse-midi")
        .description("Click to parse the MIDI file.")
        .defaultValue(false)
        .onChanged(this::onParseMidiChanged)
        .build()
    );

    private void onParseMidiChanged(Boolean value) {
        if (value) {
            parseMidiFile();
            parseMidi.set(false); // reset
        }
    }

    private void parseMidiFile() {
        String path = midiFilePath.get();
        if (path.isEmpty()) {
            error("MIDI file path is empty.");
            return;
        }
        info("Parsing MIDI file: " + path);
       
    }

}