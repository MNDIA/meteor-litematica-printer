package com.kkllffaa.meteor_litematica_printer.mixins

import com.kkllffaa.meteor_litematica_printer.Modules.Tools.BetterThirdPerson
import net.minecraft.client.option.GameOptions
import net.minecraft.client.option.Perspective
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Shadow
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

@Mixin(GameOptions::class)
class GameOptionsMixin {

    @Shadow
    private var perspective: Perspective = Perspective.FIRST_PERSON;

    @Inject(method = ["setPerspective"], at = [At("HEAD")], cancellable = true)
    private fun onSetPerspective(perspectiveIn: Perspective, ci: CallbackInfo) {
        if (!BetterThirdPerson.isActive) return

        val current = this.perspective
        val newPerspective =
            if (BetterThirdPerson.禁用第三人称Back.get() && perspectiveIn == Perspective.THIRD_PERSON_BACK) {
                when (current) {
                    Perspective.FIRST_PERSON -> Perspective.THIRD_PERSON_FRONT
                    else -> Perspective.FIRST_PERSON
                }
            } else {
                perspectiveIn
            }
        if (current != newPerspective) {
            this.perspective = newPerspective
            BetterThirdPerson.onPerspectiveChanged(newPerspective)
        }
        ci.cancel()
    }

    @Inject(method = ["getPerspective"], at = [At("HEAD")], cancellable = true)
    private fun onGetPerspective(ci: CallbackInfoReturnable<Perspective>) {
        if (!BetterThirdPerson.isActive) return

        ci.returnValue = when {
            BetterThirdPerson.禁用第三人称Back.get() && this.perspective == Perspective.THIRD_PERSON_BACK -> Perspective.THIRD_PERSON_FRONT
            else -> this.perspective
        }
    }
}
