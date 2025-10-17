package com.kkllffaa.meteor_litematica_printer.mixins;

import com.mojang.authlib.GameProfile;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import net.minecraft.block.entity.BlockEntity;
import com.kkllffaa.meteor_litematica_printer.Modules.CRUD.AtomicSettings.PlaceSettings;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.c2s.play.UpdateSignC2SPacket;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Locale;
import java.util.Optional;

@Mixin(ClientPlayerEntity.class)
public class MixinClientPlayerEntity extends AbstractClientPlayerEntity {
    @Unique
    private static boolean didCheckForUpdates = false;
    @Final
    @Shadow
    protected MinecraftClient client;
    @Final
    @Shadow
    public ClientPlayNetworkHandler networkHandler;

    public MixinClientPlayerEntity(ClientWorld world, GameProfile profile) {
        super(world, profile);
    }

    @Inject(method = "openEditSignScreen", at = @At("HEAD"), cancellable = true)
    public void openEditSignScreen(SignBlockEntity sign, boolean front, CallbackInfo ci) {
        getTargetSignEntity(sign).ifPresent(signBlockEntity ->
        {
            SignText targetText = signBlockEntity.getText(front);
            String line0 = getFormattedLine(targetText.getMessage(0, false));
            String line1 = getFormattedLine(targetText.getMessage(1, false));
            String line2 = getFormattedLine(targetText.getMessage(2, false));
            String line3 = getFormattedLine(targetText.getMessage(3, false));

            UpdateSignC2SPacket packet = new UpdateSignC2SPacket(sign.getPos(),
                    front,
                    line0,
                    line1,
                    line2,
                    line3);
            this.networkHandler.sendPacket(packet);
            ci.cancel();
        });
    }

    @Unique
    private Optional<SignBlockEntity> getTargetSignEntity(SignBlockEntity sign) {
        WorldSchematic worldSchematic = SchematicWorldHandler.getSchematicWorld();
        if (sign.getWorld() == null || worldSchematic == null) {
            return Optional.empty();
        }

        BlockEntity targetBlockEntity = worldSchematic.getBlockEntity(sign.getPos());

        if (targetBlockEntity instanceof SignBlockEntity targetSignEntity) {
            return Optional.of(targetSignEntity);
        }

        return Optional.empty();
    }

    @Unique
    private String getFormattedLine(Text text) {
        PlaceSettings.SignColorMode mode = PlaceSettings.Instance.SignTextWithColor.get();
        if (mode == PlaceSettings.SignColorMode.None) {
            return text.getString();
        }

        final char controlChar = mode == PlaceSettings.SignColorMode.反三 ? '§' : '&';
        final Style[] lastStyle = new Style[]{Style.EMPTY};
        StringBuilder builder = new StringBuilder();

        text.visit((style, string) -> {
            if (!style.equals(lastStyle[0])) {
                if (!lastStyle[0].isEmpty()) {
                    builder.append(controlChar).append('r');
                }
                appendStyleCodes(builder, style, controlChar);
                lastStyle[0] = style;
            }
            builder.append(string);
            return Optional.empty();
        }, Style.EMPTY);

        return builder.toString();
    }

    @Unique
    private void appendStyleCodes(StringBuilder builder, Style style, char controlChar) {
        TextColor textColor = style.getColor();
        if (textColor != null) {
            appendColorCode(builder, textColor, controlChar);
        }
        if (style.isObfuscated()) {
            builder.append(controlChar).append('k');
        }
        if (style.isBold()) {
            builder.append(controlChar).append('l');
        }
        if (style.isStrikethrough()) {
            builder.append(controlChar).append('m');
        }
        if (style.isUnderlined()) {
            builder.append(controlChar).append('n');
        }
        if (style.isItalic()) {
            builder.append(controlChar).append('o');
        }
    }

    @Unique
    private void appendColorCode(StringBuilder builder, TextColor color, char controlChar) {
        Formatting vanillaFormatting = getVanillaFormatting(color);
        if (vanillaFormatting != null) {
            builder.append(controlChar).append(vanillaFormatting.getCode());
            return;
        }

        int rgb = color.getRgb();
        String hex = String.format(Locale.ROOT, "%06X", rgb);
        builder.append(controlChar).append('x');
        for (char c : hex.toCharArray()) {
            builder.append(controlChar).append(Character.toLowerCase(c));
        }
    }

    @Unique
    private Formatting getVanillaFormatting(TextColor color) {
        int rgb = color.getRgb();
        for (Formatting formatting : Formatting.values()) {
            Integer colorValue = formatting.getColorValue();
            if (colorValue != null && colorValue == rgb) {
                return formatting;
            }
        }
        return null;
    }
}
