package com.kkllffaa.meteor_litematica_printer.mixins

import com.kkllffaa.meteor_litematica_printer.Modules.AtomicSettings.BreakSettings
import net.minecraft.client.network.ClientPlayerInteractionManager
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(ClientPlayerInteractionManager::class)
class ClientPlayerInteractionManagerMixin {
    @Inject(method = ["cancelBlockBreaking"], at = [At("HEAD")], cancellable = true)
    private fun onCancelBlockBreaking(info: CallbackInfo) {
        if (BreakSettings.breaking) info.cancel()
    }
}
