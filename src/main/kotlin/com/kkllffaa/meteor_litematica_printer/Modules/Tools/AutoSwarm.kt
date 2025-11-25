package com.kkllffaa.meteor_litematica_printer.Modules.Tools

import com.kkllffaa.meteor_litematica_printer.Addon
import meteordevelopment.meteorclient.events.game.GameJoinedEvent
import meteordevelopment.meteorclient.events.game.GameLeftEvent
import meteordevelopment.meteorclient.events.world.TickEvent
import meteordevelopment.meteorclient.settings.*
import meteordevelopment.meteorclient.systems.modules.Module
import meteordevelopment.meteorclient.systems.modules.Modules
import meteordevelopment.meteorclient.systems.modules.misc.swarm.Swarm
import meteordevelopment.meteorclient.systems.modules.misc.swarm.SwarmHost
import meteordevelopment.meteorclient.systems.modules.misc.swarm.SwarmWorker
import meteordevelopment.orbit.EventHandler

class AutoSwarm : Module(Addon.TOOLS, "auto-swarm", "Automatically manages swarm instances.") {
    private val sgGeneral: SettingGroup = settings.getDefaultGroup()

    private val checkCycle: Setting<Int?> = sgGeneral.add<Int?>(
        IntSetting.Builder()
            .name("check-cycle")
            .description("Delay in seconds between checkings")
            .defaultValue(1)
            .range(1, 60)
            .build()
    )

    private val checkDelayAfterWorldChanged: Setting<Int?> = sgGeneral.add<Int?>(
        IntSetting.Builder()
            .name("check-delay-after-world-changed")
            .description("Delay in seconds between checkings after world change")
            .defaultValue(1)
            .range(1, 60)
            .build()
    )

    val mode: Setting<Swarm.Mode?> = sgGeneral.add<Swarm.Mode?>(
        EnumSetting.Builder<Swarm.Mode?>()
            .name("mode")
            .description("What type of client to run.")
            .defaultValue(Swarm.Mode.Host)
            .build()
    )

    private val ipAddress: Setting<String?> = sgGeneral.add<String?>(
        StringSetting.Builder()
            .name("ip")
            .description("The IP address of the host server.")
            .defaultValue("localhost")
            .visible(IVisible { mode.get() == Swarm.Mode.Worker })
            .build()
    )

    private val serverPort: Setting<Int?> = sgGeneral.add<Int?>(
        IntSetting.Builder()
            .name("port")
            .description("The port used for connections.")
            .defaultValue(6969)
            .range(1, 65535)
            .noSlider()
            .build()
    )

    private var lastCheckTime: Long = 0
    private var lastWorldChangeTime: Long = 0

    override fun onDeactivate() {
        val swarm = Modules.get().get<Swarm?>(Swarm::class.java)
        if (swarm != null) {
            if (swarm.isActive()) {
                swarm.toggle()
            } else {
                swarm.close()
            }
        }
    }

    @EventHandler
    private fun onTick(event: TickEvent.Post?) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastCheckTime > checkCycle.get()!! * 1000L) {
            lastCheckTime = currentTime

            if (currentTime - lastWorldChangeTime > checkDelayAfterWorldChanged.get()!! * 1000L) {
                CheckSwarm()
            }
        }
    }

    @EventHandler
    private fun onGameLeft(event: GameLeftEvent?) {
        lastWorldChangeTime = System.currentTimeMillis()
    }

    @EventHandler
    private fun onGameJoin(event: GameJoinedEvent?) {
        lastWorldChangeTime = System.currentTimeMillis()
    }

    private fun CheckSwarm() {
        val swarm = Modules.get().get<Swarm?>(Swarm::class.java)
        if (swarm != null) {
            val isActive = swarm.isActive()
            val isHost = swarm.isHost()
            val isWorker = swarm.isWorker()

            if (!isActive) {
                swarm.toggle()
            }

            if (mode.get() == Swarm.Mode.Host && !isHost) {
                swarm.close()
                swarm.mode.set(Swarm.Mode.Host)
                swarm.host = SwarmHost(serverPort.get()!!)
            } else if (mode.get() == Swarm.Mode.Worker && (!isWorker || !swarm.worker.isAlive())) {
                swarm.close()
                swarm.mode.set(Swarm.Mode.Worker)
                swarm.worker = SwarmWorker(ipAddress.get(), serverPort.get()!!)
            }
        }
    }
}
