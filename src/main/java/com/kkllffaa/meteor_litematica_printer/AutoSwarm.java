package com.kkllffaa.meteor_litematica_printer;

import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.swarm.Swarm;
import meteordevelopment.meteorclient.systems.modules.misc.swarm.Swarm.Mode;
import meteordevelopment.meteorclient.systems.modules.misc.swarm.SwarmHost;
import meteordevelopment.meteorclient.systems.modules.misc.swarm.SwarmWorker;
import meteordevelopment.orbit.EventHandler;

public class AutoSwarm extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> checkCycle = sgGeneral.add(new IntSetting.Builder()
            .name("check-cycle")
            .description("Delay in seconds between checkings")
            .defaultValue(1)
            .range(1, 60)
            .build());

    private final Setting<Integer> checkDelayAfterWorldChanged = sgGeneral.add(new IntSetting.Builder()
            .name("check-delay-after-world-changed")
            .description("Delay in seconds between checkings after world change")
            .defaultValue(1)
            .range(1, 60)
            .build());

    public final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("mode")
            .description("What type of client to run.")
            .defaultValue(Mode.Host)
            .build());

    private final Setting<String> ipAddress = sgGeneral.add(new StringSetting.Builder()
            .name("ip")
            .description("The IP address of the host server.")
            .defaultValue("localhost")
            .visible(() -> mode.get() == Mode.Worker)
            .build());

    private final Setting<Integer> serverPort = sgGeneral.add(new IntSetting.Builder()
            .name("port")
            .description("The port used for connections.")
            .defaultValue(6969)
            .range(1, 65535)
            .noSlider()
            .build());

    private long lastCheckTime = 0;
    private long lastWorldChangeTime = 0;

    public AutoSwarm() {
        super(Addon.TOOLSCATEGORY, "auto-swarm", "Automatically manages swarm instances.");
    }

    @Override
    public void onDeactivate() {
        Swarm swarm = Modules.get().get(Swarm.class);
        if (swarm != null) {
            if (swarm.isActive()) {
                swarm.toggle();
            } else {
                swarm.close();
            }
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCheckTime > checkCycle.get() * 1000L) {

            lastCheckTime = currentTime;

            if (currentTime - lastWorldChangeTime > checkDelayAfterWorldChanged.get() * 1000L) {

                CheckSwarm();
            }
        }
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        lastWorldChangeTime = System.currentTimeMillis();
    }

    @EventHandler
    private void onGameJoin(GameJoinedEvent event) {
        lastWorldChangeTime = System.currentTimeMillis();
    }

    private void CheckSwarm() {
        Swarm swarm = Modules.get().get(Swarm.class);
        if (swarm != null) {
            boolean isActive = swarm.isActive();
            boolean isHost = swarm.isHost();
            boolean isWorker = swarm.isWorker();

            if (!isActive) {
                swarm.toggle();
            }

            if (mode.get() == Mode.Host && !isHost) {

                swarm.close();
                swarm.mode.set(Mode.Host);
                swarm.host = new SwarmHost(serverPort.get());
            } else if (mode.get() == Mode.Worker && (!isWorker || !swarm.worker.isAlive())) {
                swarm.close();
                swarm.mode.set(Mode.Worker);
                swarm.worker = new SwarmWorker(ipAddress.get(), serverPort.get());
            }

        }
    }
}
