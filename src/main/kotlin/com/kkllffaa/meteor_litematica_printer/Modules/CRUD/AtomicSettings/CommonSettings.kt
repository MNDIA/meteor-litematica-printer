package com.kkllffaa.meteor_litematica_printer.Modules.CRUD.AtomicSettings

import com.kkllffaa.meteor_litematica_printer.Addon
import com.kkllffaa.meteor_litematica_printer.Functions.*
import meteordevelopment.meteorclient.settings.*
import meteordevelopment.meteorclient.systems.modules.Module
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box

object CommonSettings : Module(Addon.SettingsForCRUD, "Common", "Module to configure AtomicSettings.") {
    override fun toggle() {
        if (isActive) return
        super.toggle()
    }

    init {
        toggle()
    }

    private val sgGeneral = settings.defaultGroup

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

}
