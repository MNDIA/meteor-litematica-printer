package com.kkllffaa.meteor_litematica_printer.mixins

import com.kkllffaa.meteor_litematica_printer.Functions.*
import com.mojang.authlib.GameProfile
import fi.dy.masa.litematica.world.SchematicWorldHandler
import net.minecraft.block.entity.SignBlockEntity
import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.AbstractClientPlayerEntity
import net.minecraft.client.network.ClientPlayNetworkHandler
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.client.world.ClientWorld
import net.minecraft.network.packet.c2s.play.UpdateSignC2SPacket
import org.spongepowered.asm.mixin.Final
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Shadow
import org.spongepowered.asm.mixin.Unique
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(ClientPlayerEntity::class)
open class MixinClientPlayerEntity(world: ClientWorld, profile: GameProfile) :
    AbstractClientPlayerEntity(world, profile) {
    @Final
    @Shadow
    protected var client: MinecraftClient? = null

    @Final
    @Shadow
    var networkHandler: ClientPlayNetworkHandler? = null

    @Inject(method = ["openEditSignScreen"], at = [At("HEAD")], cancellable = true)
    fun openEditSignScreen(sign: SignBlockEntity, front: Boolean, ci: CallbackInfo) {
        getTargetSignEntity(sign)?.let { signBlockEntity ->
            val targetText = signBlockEntity.getText(front)
            val lines = (0..3).map { getFormattedLine(targetText.getMessage(it, false)) }

            val packet = UpdateSignC2SPacket(
                sign.pos,
                front,
                lines[0],
                lines[1],
                lines[2],
                lines[3]
            )
            networkHandler?.sendPacket(packet)
            ci.cancel()
        }
    }

    @Unique
    private fun getTargetSignEntity(sign: SignBlockEntity): SignBlockEntity? {
        val worldSchematic = SchematicWorldHandler.getSchematicWorld() ?: return null
        return worldSchematic.getBlockEntity(sign.pos) as? SignBlockEntity
    }
}
