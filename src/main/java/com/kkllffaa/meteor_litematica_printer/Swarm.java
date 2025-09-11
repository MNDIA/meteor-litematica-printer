
package com.kkllffaa.meteor_litematica_printer;

import meteordevelopment.meteorclient.systems.modules.misc.swarm.SwarmHost;
import meteordevelopment.meteorclient.systems.modules.misc.swarm.SwarmWorker;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.DeathMessageS2CPacket;
import net.minecraft.util.Util;

public class Swarm extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgAutoCommand = settings.createGroup("Auto Command");

    public final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("What type of client to run.")
        .defaultValue(Mode.Host)
        .build()
    );

    private final Setting<String> ipAddress = sgGeneral.add(new StringSetting.Builder()
        .name("ip")
        .description("The IP address of the host server.")
        .defaultValue("localhost")
        .visible(() -> mode.get() == Mode.Worker)
        .build()
    );

    private final Setting<Integer> serverPort = sgGeneral.add(new IntSetting.Builder()
        .name("port")
        .description("The port used for connections.")
        .defaultValue(6969)
        .range(1, 65535)
        .noSlider()
        .build()
    );

    // Auto Command Settings
    private final Setting<Boolean> autoCommand = sgAutoCommand.add(new BoolSetting.Builder()
        .name("auto-command")
        .description("Automatically executes a command after death.")
        .defaultValue(false)
        .build()
    );

    private final Setting<String> deathCommand = sgAutoCommand.add(new StringSetting.Builder()
        .name("death-command")
        .description("The command to execute after death.")
        .defaultValue("/back")
        .visible(autoCommand::get)
        .build()
    );

    private final Setting<Integer> commandDelay = sgAutoCommand.add(new IntSetting.Builder()
        .name("command-delay")
        .description("Delay in ticks before executing the command (20 ticks = 1 second).")
        .defaultValue(10)
        .range(1, 100)
        .sliderMin(1)
        .sliderMax(100)
        .visible(autoCommand::get)
        .build()
    );

    public SwarmHost host;
    public SwarmWorker worker;

    // Auto Command variables
    private int commandTicks = -1;

    public Swarm() {
        super(Categories.Misc, "swarm+", "Allows you to control multiple instances of Meteor from one central host.");
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        WVerticalList list = theme.verticalList();

        WHorizontalList b = list.add(theme.horizontalList()).expandX().widget();

        WButton start = b.add(theme.button("Start")).expandX().widget();
        start.action = () -> {
            if (!isActive()) return;

            close();
            if (mode.get() == Mode.Host) host = new SwarmHost(serverPort.get());
            else worker = new SwarmWorker(ipAddress.get(), serverPort.get());
        };

        WButton stop = b.add(theme.button("Stop")).expandX().widget();
        stop.action = this::close;

        WButton guide = list.add(theme.button("Guide")).expandX().widget();
        guide.action = () -> Util.getOperatingSystem().open("https://github.com/MeteorDevelopment/meteor-client/wiki/Swarm-Guide");

        return list;
    }

    @Override
    public String getInfoString() {
        return mode.get().name();
    }

    @Override
    public void onActivate() {
        close();
    }

    @Override
    public void onDeactivate() {
        close();
    }

    public void close() {
        try {
            if (host != null) {
                host.disconnect();
                host = null;
            }
            if (worker != null) {
                worker.disconnect();
                worker = null;
            }
        } catch (Exception ignored) {}
    }

    @Override
    public void toggle() {
        close();
        super.toggle();
    }

    public boolean isHost() {
        return mode.get() == Mode.Host && host != null && !host.isInterrupted();
    }

    public boolean isWorker() {
        return mode.get() == Mode.Worker && worker != null && !worker.isInterrupted();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (isWorker()) worker.tick();
        
        // Handle scheduled command execution
        if (commandTicks > 0) {
            commandTicks--;
            if (commandTicks < 0) {
                executeDeathCommand();
            }
        }
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (!autoCommand.get()) return;
        
        if (event.packet instanceof DeathMessageS2CPacket packet) {
            Entity entity = mc.world.getEntityById(packet.playerId());
            if (entity == mc.player) {
                commandTicks = commandDelay.get();
            }
        }
    }

    private void executeDeathCommand() {
        if (mc.player == null || mc.world == null) return;
        
        String command = deathCommand.get().trim();
        if (!command.isEmpty()) {
            ChatUtils.sendPlayerMsg(command);
        }
    }

    public enum Mode {
        Host,
        Worker
    }
}
