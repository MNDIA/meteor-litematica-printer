package com.kkllffaa.meteor_litematica_printer.Modules.CRUD.AtomicSettings

import com.kkllffaa.meteor_litematica_printer.Addon
import com.kkllffaa.meteor_litematica_printer.Functions.MyUtils.ActionMode
import com.kkllffaa.meteor_litematica_printer.Functions.MyUtils.DistanceMode
import meteordevelopment.meteorclient.MeteorClient
import meteordevelopment.meteorclient.settings.*
import meteordevelopment.meteorclient.systems.modules.Module
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket
import net.minecraft.util.Hand
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box

class CommonSettings : Module(Addon.SettingsForCRUD, "Common", "Module to configure AtomicSettings.") {
    override fun toggle() {
        if (isActive()) {
            return
        }
        super.toggle()
    }

    private val sgGeneral: SettingGroup = settings.getDefaultGroup()
    private val swingHand: Setting<ActionMode?> = sgGeneral.add<ActionMode?>(
        EnumSetting.Builder<ActionMode?>()
            .name("swing-hand")
            .description("swing hand post mining.")
            .defaultValue(ActionMode.None)
            .build()
    )

    private val distanceProtection: Setting<DistanceMode?> = sgGeneral.add<DistanceMode?>(
        EnumSetting.Builder<DistanceMode?>()
            .name("distance-protection")
            .description("Prevent CRUD blocks that are too far to the player.")
            .defaultValue(DistanceMode.Max)
            .build()
    )

    private val maxDistance: Setting<Double?> = sgGeneral.add<Double?>(
        DoubleSetting.Builder()
            .name("max-distance")
            .description("Maximum distance from player to mine block face.")
            .defaultValue(5.4)
            .min(1.0)
            .max(1024.0)
            .sliderRange(1.0, 5.5)
            .visible(IVisible { distanceProtection.get() == DistanceMode.Max })
            .build()
    )

    init {
        this.toggle()
    }

    private fun swingHand(hand: Hand?) {
        when (swingHand.get()) {
            ActionMode.None -> {}
            ActionMode.SendPacket -> MeteorClient.mc.getNetworkHandler()!!.sendPacket(HandSwingC2SPacket(hand))
            ActionMode.Normal -> MeteorClient.mc.player!!.swingHand(hand)
        }
    }

    private val handDistanceStep: Double
        get() = when (distanceProtection.get()) {
            DistanceMode.Auto -> {
                val playerHandRange = mc.player!!.getBlockInteractionRange()
                if (mc.player!!.isCreative()) playerHandRange + 0.5 else playerHandRange
            }

            DistanceMode.Max -> maxDistance.get()
        }

    private fun canInteractWithBlockAt(pos: BlockPos): Boolean {
        return when (distanceProtection.get()) {
            DistanceMode.Auto -> mc.player!!.canInteractWithBlockAt(pos, if (mc.player!!.isCreative()) 0.5 else 0.0)
            DistanceMode.Max -> (Box(pos)).squaredMagnitude(mc.player!!.getEyePos()) < maxDistance.get()!! * maxDistance.get()!!
        }
    }

    companion object {
        var Instance: CommonSettings = CommonSettings()
        fun swing(hand: Hand?) {
            Instance.swingHand(hand)
        }

        val handDistance: Double
            get() = Instance.handDistanceStep

        @JvmStatic
        fun canTouchTheBlockAt(pos: BlockPos): Boolean {
            return Instance.canInteractWithBlockAt(pos)
        }
    }
}
