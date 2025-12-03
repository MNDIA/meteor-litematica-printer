package com.kkllffaa.meteor_litematica_printer.Modules.AtomicSettings

import com.kkllffaa.meteor_litematica_printer.Addon
import com.kkllffaa.meteor_litematica_printer.Functions.*
import meteordevelopment.meteorclient.events.world.TickEvent
import meteordevelopment.meteorclient.settings.*
import meteordevelopment.meteorclient.systems.modules.Module
import meteordevelopment.orbit.EventHandler
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Direction
import kotlin.math.floor

object CommonSettings : Module(Addon.SettingsForCRUD, "Common", "Module to configure AtomicSettings.") {
    override fun toggle() {
        if (isActive) return
        super.toggle()
    }

    init {
        toggle()
    }

    private val sgGeneral = settings.defaultGroup

    private val sgRotation = settings.createGroup("Rotation")
    private val sgOther = settings.createGroup("Other")

    private val distanceProtection: Setting<DistanceMode> = sgGeneral.add(
        EnumSetting.Builder<DistanceMode>()
            .name("distance-protection")
            .description("Prevent CRUD blocks that are too far to the player.")
            .defaultValue(DistanceMode.Max)
            .build()
    )

    private val maxDistance: Setting<Double> = sgGeneral.add(
        DoubleSetting.Builder()
            .name("max-distance")
            .description("Maximum distance from player to mine block face.")
            .defaultValue(5.49)
            .min(1.0)
            .max(1024.0)
            .sliderRange(1.0, 5.5)
            .visible { distanceProtection.get() == DistanceMode.Max }
            .build()
    )

    private val 容差: Setting<Int> = sgRotation.add(
        IntSetting.Builder()
            .name("angle-range")
            .description("Angle range for direction detection (degrees).")
            .defaultValue(25)
            .min(1).sliderMin(1)
            .max(45).sliderMax(44)
            .build()
    )
    private val stableTicks: Setting<Int> = sgRotation.add(
        IntSetting.Builder()
            .name("rotation-stable-ticks")
            .description("Number of ticks the rotation must remain stable.")
            .defaultValue(10)
            .min(1).sliderMin(1)
            .max(40).sliderMax(10)
            .build()
    )
    val MaxTextWidth: Setting<Int> = sgOther.add(
        IntSetting.Builder()
            .name("max-text-width")
            .description("0 for default auto")
            .defaultValue(99999)
            .min(0)
            .build()
    )

    val NoFogInLava: Setting<Boolean> = sgOther.add(
        BoolSetting.Builder()
            .name("No-fog-in-lava")
            .description("See blocks in lava.")
            .defaultValue(true)
            .build()
    )

    // val WhitespaceIsNOTBlank: Setting<Boolean> = sgOther.add(
    //     BoolSetting.Builder()
    //         .name("whitespace-is-NOT-blank")
    //         .description("You can use the anvil to name blank names(not removing DataComponentTypes.CUSTOM_NAME)")
    //         .defaultValue(true)
    //         .build()
    // )

    val EveryCharIsValid: Setting<Boolean> = sgOther.add(
        BoolSetting.Builder()
            .name("every-char-is-valid")
            .description("Allow every char in text everywhere.")
            .defaultValue(true)
            .build()
    )

    val OnlyRotateCam: Setting<Boolean> = sgOther.add(
        BoolSetting.Builder()
            .name("only-rotate-cam")
            .description("Only rotate camera when rotating player.")
            .defaultValue(false)
            .onChanged {
                val player = mc.player ?: return@onChanged
                if (OnlyRotateCam.get()) {
                    cameraYaw = player.yaw
                    cameraPitch = player.pitch
                } else {
                    player.yaw = CommonSettings.cameraYaw
                    player.pitch = CommonSettings.cameraPitch
                }
            }
            .build()
    )
    var cameraYaw: Float = 0F
    var cameraPitch: Float = 0F

    val PlayerHandDistance: Double
        get() = when (distanceProtection.get()) {
            DistanceMode.Auto -> {
                mc.player?.let {
                    val playerHandRange = it.blockInteractionRange
                    if (it.isCreative) playerHandRange + 0.5 else playerHandRange
                } ?: 4.5
            }

            DistanceMode.Max -> maxDistance.get()
        }


    fun playerCanTouchBlockPos(pos: BlockPos): Boolean =
        // DistanceMode.Auto -> player.canInteractWithBlockAt(pos, if (player.isCreative) 0.5 else 0.0)
        mc.player?.let {
            PlayerHandDistance.let { maxDist ->
                Box(pos).squaredMagnitude(it.eyePos) < maxDist * maxDist
            }
        } ?: false

    // 稳定过滤器：连续 stableTicks 个 tick 结果相同才返回
    private class StableFilter<T>(private val compute: () -> T?) {
        private var lastValue: T? = null
        private var stableCount: Int = 0

        fun tick() {
            val current = compute()
            if (current == lastValue) {
                stableCount++
            } else {
                lastValue = current
                stableCount = 1
            }
        }

        fun value(): T? = if (stableCount >= stableTicks.get()) lastValue else null
    }

    private val yawDirectionFilter = StableFilter { mc.player?.computeYawDirection() }
    private val pitchDirectionFilter = StableFilter { mc.player?.computePitchDirection() }
    private val yawInt16Filter = StableFilter { mc.player?.computeYawInt16() }

    @EventHandler
    private fun onTick(event: TickEvent.Post) {
        yawDirectionFilter.tick()
        pitchDirectionFilter.tick()
        yawInt16Filter.tick()
    }

    val playerYawDirection: Direction? get() = yawDirectionFilter.value()

    val playerPitchDirection: Direction? get() = pitchDirectionFilter.value()

    val playerYawInt16: Int? get() = yawInt16Filter.value()

    private fun ClientPlayerEntity.computeYawDirection(): Direction? {
        val 容差 = 容差.get().toFloat()
        val yaw = yaw.normalizeAsYaw
        if (yaw.isNearIn(90f, 容差)) {
            return Direction.WEST
        } else if (yaw.isNearIn(0f, 容差)) {
            return Direction.SOUTH
        } else if (yaw.isNearIn(-90f, 容差)) {
            return Direction.EAST
        } else if (yaw.isNearIn(180f, 容差) || yaw.isNearIn(-180f, 容差)) {
            return Direction.NORTH
        }
        return null
    }

    private fun ClientPlayerEntity.computePitchDirection(): Direction? {
        val 容差 = 容差.get().toFloat()
        val pitch = pitch.clampAsPitch
        if (pitch.isNearIn(90f, 容差)) {
            return Direction.DOWN
        } else if (pitch.isNearIn(-90f, 容差)) {
            return Direction.UP
        } else if (pitch.isNearIn(0f, 容差)) {
            return Direction.NORTH
        }
        return null
    }

    const val 十六分之周 = 22.50f
    private fun ClientPlayerEntity.computeYawInt16(): Int? {
        val 容差 = 容差.get().toFloat() / 4
        val yaw = ((yaw % 360.00f) + 360.00f) % 360.00f
        val 周期 = floor(yaw / 十六分之周).toInt()
        val 余数 = yaw - 周期 * 十六分之周

        val result = if (余数.isNearIn(0f, 容差)) {
            周期
        } else if (余数.isNearIn(十六分之周, 容差)) {
            周期 + 1
        } else {
            return null
        }
        // 规范化到 0-15
        return ((result % 16) + 16) % 16
    }
}
