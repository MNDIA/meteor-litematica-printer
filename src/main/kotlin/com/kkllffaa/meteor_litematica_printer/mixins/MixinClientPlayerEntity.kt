package com.kkllffaa.meteor_litematica_printer.mixins

import com.kkllffaa.meteor_litematica_printer.Modules.CRUD.AtomicSettings.PlaceSettings
import com.kkllffaa.meteor_litematica_printer.Modules.CRUD.AtomicSettings.PlaceSettings.SignColorMode
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
import java.util.*
import java.util.function.Consumer

@Mixin(ClientPlayerEntity::class)
class MixinClientPlayerEntity(world: ClientWorld, profile: GameProfile) : AbstractClientPlayerEntity(world, profile) {
    @Final
    @Shadow
    protected var client: MinecraftClient? = null

    @Final
    @Shadow
    var networkHandler: ClientPlayNetworkHandler? = null

    @Inject(method = ["openEditSignScreen"], at = [At("HEAD")], cancellable = true)
    fun openEditSignScreen(sign: SignBlockEntity, front: Boolean, ci: CallbackInfo) {
        getTargetSignEntity(sign).ifPresent(Consumer { signBlockEntity: SignBlockEntity? ->
            val targetText = signBlockEntity!!.getText(front)
            val line0 = getFormattedLine(targetText.getMessage(0, false))
            val line1 = getFormattedLine(targetText.getMessage(1, false))
            val line2 = getFormattedLine(targetText.getMessage(2, false))
            val line3 = getFormattedLine(targetText.getMessage(3, false))

            val packet = UpdateSignC2SPacket(
                sign.getPos(),
                front,
                line0,
                line1,
                line2,
                line3
            )
            this.networkHandler!!.sendPacket(packet)
            ci.cancel()
        })
    }

    @Unique
    private fun getTargetSignEntity(sign: SignBlockEntity): Optional<SignBlockEntity?> {
        val worldSchematic = SchematicWorldHandler.getSchematicWorld()
        if (sign.getWorld() == null || worldSchematic == null) {
            return Optional.empty<SignBlockEntity?>()
        }

        val targetBlockEntity = worldSchematic.getBlockEntity(sign.getPos())

        if (targetBlockEntity is SignBlockEntity) {
            return Optional.of<SignBlockEntity?>(targetBlockEntity)
        }

        return Optional.empty<SignBlockEntity?>()
    }

    @Unique
    private fun getFormattedLine(text: Text): String? {
        val mode = PlaceSettings.Instance.SignTextWithColor.get()
        if (mode == SignColorMode.None) {
            return text.getString()
        }

        val controlChar = if (mode == SignColorMode.反三) '§' else '&'
        val lastStyle: Array<Style?> = arrayOf<Style>(Style.EMPTY)
        val builder = StringBuilder()

        text.visit<Any?>(StyledVisitor { style: Style?, string: String? ->
            if (style != lastStyle[0]) {
                if (!lastStyle[0]!!.isEmpty()) {
                    builder.append(controlChar).append('r')
                }
                appendStyleCodes(builder, style!!, controlChar)
                lastStyle[0] = style
            }
            builder.append(string)
            Optional.empty<Any?>()
        }, Style.EMPTY)

        return builder.toString()
    }

    @Unique
    private fun appendStyleCodes(builder: StringBuilder, style: Style, controlChar: Char) {
        val textColor = style.getColor()
        if (textColor != null) {
            appendColorCode(builder, textColor, controlChar)
        }
        if (style.isObfuscated()) {
            builder.append(controlChar).append('k')
        }
        if (style.isBold()) {
            builder.append(controlChar).append('l')
        }
        if (style.isStrikethrough()) {
            builder.append(controlChar).append('m')
        }
        if (style.isUnderlined()) {
            builder.append(controlChar).append('n')
        }
        if (style.isItalic()) {
            builder.append(controlChar).append('o')
        }
    }

    @Unique
    private fun appendColorCode(builder: StringBuilder, color: TextColor, controlChar: Char) {
        val vanillaFormatting = getVanillaFormatting(color)
        if (vanillaFormatting != null) {
            builder.append(controlChar).append(vanillaFormatting.getCode())
            return
        }

        val rgb = color.getRgb()
        val hex = String.format(Locale.ROOT, "%06X", rgb)
        builder.append(controlChar).append('x')
        for (c in hex.toCharArray()) {
            builder.append(controlChar).append(c.lowercaseChar())
        }
    }

    @Unique
    private fun getVanillaFormatting(color: TextColor): Formatting? {
        val rgb = color.getRgb()
        for (formatting in Formatting.entries) {
            val colorValue = formatting.getColorValue()
            if (colorValue != null && colorValue == rgb) {
                return formatting
            }
        }
        return null
    }

    companion object {
        @Unique
        private const val didCheckForUpdates = false
    }
}
