package com.kkllffaa.meteor_litematica_printer.mixins

import com.kkllffaa.meteor_litematica_printer.Functions.SignColorMode
import com.kkllffaa.meteor_litematica_printer.Modules.CRUD.AtomicSettings.PlaceSettings
import com.mojang.authlib.GameProfile
import fi.dy.masa.litematica.world.SchematicWorldHandler
import net.minecraft.block.entity.SignBlockEntity
import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.AbstractClientPlayerEntity
import net.minecraft.client.network.ClientPlayNetworkHandler
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.client.world.ClientWorld
import net.minecraft.network.packet.c2s.play.UpdateSignC2SPacket
import net.minecraft.text.StringVisitable.StyledVisitor
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.text.TextColor
import net.minecraft.util.Formatting
import org.spongepowered.asm.mixin.Final
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Shadow
import org.spongepowered.asm.mixin.Unique
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import java.util.Locale

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
        if (sign.world == null) return null
        return worldSchematic.getBlockEntity(sign.pos) as? SignBlockEntity
    }

    @Unique
    private fun getFormattedLine(text: Text): String {
        val mode = PlaceSettings.SignTextWithColor.get()
        if (mode == SignColorMode.None) {
            return text.string
        }

        val controlChar = if (mode == SignColorMode.反三) '§' else '&'
        var lastStyle = Style.EMPTY
        val builder = StringBuilder()

        text.visit(StyledVisitor<Unit> { style, string ->
            if (style != lastStyle) {
                if (!lastStyle.isEmpty) {
                    builder.append(controlChar).append('r')
                }
                appendStyleCodes(builder, style, controlChar)
                lastStyle = style
            }
            builder.append(string)
            java.util.Optional.empty<Unit>()
        }, Style.EMPTY)

        return builder.toString()
    }

    @Unique
    private fun appendStyleCodes(builder: StringBuilder, style: Style, controlChar: Char) {
        style.color?.let { appendColorCode(builder, it, controlChar) }
        if (style.isObfuscated) builder.append(controlChar).append('k')
        if (style.isBold) builder.append(controlChar).append('l')
        if (style.isStrikethrough) builder.append(controlChar).append('m')
        if (style.isUnderlined) builder.append(controlChar).append('n')
        if (style.isItalic) builder.append(controlChar).append('o')
    }

    @Unique
    private fun appendColorCode(builder: StringBuilder, color: TextColor, controlChar: Char) {
        getVanillaFormatting(color)?.let {
            builder.append(controlChar).append(it.code)
            return
        }

        val hex = "%06X".format(Locale.ROOT, color.rgb)
        builder.append(controlChar).append('x')
        hex.forEach { builder.append(controlChar).append(it.lowercaseChar()) }
    }

    @Unique
    private fun getVanillaFormatting(color: TextColor): Formatting? =
        Formatting.entries.find { it.colorValue == color.rgb }
}
