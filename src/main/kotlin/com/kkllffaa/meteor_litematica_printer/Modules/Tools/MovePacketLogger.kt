package com.kkllffaa.meteor_litematica_printer.Modules.Tools

import com.kkllffaa.meteor_litematica_printer.Addon
import meteordevelopment.meteorclient.events.packets.PacketEvent
import meteordevelopment.meteorclient.systems.modules.Module
import meteordevelopment.orbit.EventHandler
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket

object MovePacketLogger : Module(Addon.TOOLS, "move-packet-logger", "Logs PlayerMoveC2SPacket information when sent.") {
    private val sgGeneral = settings.defaultGroup

    @EventHandler
    private fun onPacketSend(event: PacketEvent.Send) {
        val packet = event.packet
        
        if (packet is PlayerMoveC2SPacket) {
            val info = buildString {
                append("[MovePacket] ")
                
                when (packet) {
                    is PlayerMoveC2SPacket.Full -> append("Full | ")
                    is PlayerMoveC2SPacket.PositionAndOnGround -> append("Position | ")
                    is PlayerMoveC2SPacket.LookAndOnGround -> append("Look | ")
                    is PlayerMoveC2SPacket.OnGroundOnly -> append("OnGround | ")
                    else -> append("Unknown | ")
                }
                
                if (packet.changesPosition()) {
                    append("Pos(%.2f, %.2f, %.2f) ".format(packet.getX(0.0), packet.getY(0.0), packet.getZ(0.0)))
                }
                
                if (packet.changesLook()) {
                    append("Yaw=%.2f Pitch=%.2f ".format(packet.getYaw(0f), packet.getPitch(0f)))
                }
                
                append("OnGround=${packet.isOnGround}")
            }
            
            info(info)
        }
    }
}
