package com.kkllffaa.meteor_litematica_printer.Modules.Tools


import com.kkllffaa.meteor_litematica_printer.Modules.CRUD.AtomicSettings.CommonSettings
import com.kkllffaa.meteor_litematica_printer.Functions.*
import com.kkllffaa.meteor_litematica_printer.Addon
import meteordevelopment.meteorclient.systems.modules.Module
import meteordevelopment.meteorclient.events.world.TickEvent
import meteordevelopment.meteorclient.settings.*
import meteordevelopment.meteorclient.utils.misc.input.Input
import meteordevelopment.orbit.EventHandler
import net.minecraft.client.option.Perspective
import net.minecraft.util.math.MathHelper


object BetterThirdPerson : Module(Addon.TOOLS, "BetterThirdPerson", "") {
    private val sgGeneral = settings.defaultGroup
    private val 移动分量阈值: Setting<Double> = sgGeneral.add(
        DoubleSetting.Builder()
            .name("Movement sensitivity")
            .description("The minimum movement input required to move the player while in better third person.")
            .defaultValue(0.4)
            .range(0.001, 1.0)
            .sliderRange(0.001, 1.0)
            .build()
    )
    val 禁用第三人称Back: Setting<Boolean> = sgGeneral.add(
        BoolSetting.Builder()
            .name("Disable Third Person Back")
            .description("Perspective.THIRD_PERSON_BACK")
            .defaultValue(true)
            .build()
    )

    override fun onActivate() {
        onPerspectiveChanged(mc.options.perspective)
    }

    override fun onDeactivate() {
        尝试退出第三人称代理()
    }

    private val 第三人称代理中 get() = CommonSettings.OnlyRotateCam.get()
    private fun 尝试进入第三人称代理() {
        if (第三人称代理中) return
        CommonSettings.OnlyRotateCam.set(true)
    }

    private fun 尝试退出第三人称代理() {
        if (!第三人称代理中) return
        mc.player?.yaw = MathHelper.wrapDegrees(CommonSettings.cameraYaw)
        mc.player?.pitch = MathHelper.clamp(CommonSettings.cameraPitch, -90f, 90f)
        CommonSettings.OnlyRotateCam.set(false)
        mc.options.forwardKey.isPressed = Input.isPressed(mc.options.forwardKey)
        mc.options.backKey.isPressed = Input.isPressed(mc.options.backKey)
        mc.options.rightKey.isPressed = Input.isPressed(mc.options.rightKey)
        mc.options.leftKey.isPressed = Input.isPressed(mc.options.leftKey)
    }

    /**
     * isActive时自动触发
     */
    fun onPerspectiveChanged(to: Perspective) {
        when (to) {
            Perspective.FIRST_PERSON -> 尝试退出第三人称代理()
            else -> 尝试进入第三人称代理()
        }
    }

    @EventHandler
    private fun onTick(event: TickEvent.Pre) {
        if (mc.options.perspective != Perspective.THIRD_PERSON_FRONT || !第三人称代理中) return
        val player = mc.player ?: return

        val 前 = Input.isPressed(mc.options.forwardKey)
        val 后 = Input.isPressed(mc.options.backKey)
        val 左 = Input.isPressed(mc.options.leftKey)
        val 右 = Input.isPressed(mc.options.rightKey)

        val intentForward = 前 - 后
        val intentRight = 右 - 左
        val 要移动 = intentForward != 0F || intentRight != 0F
        val targetYaw = if (要移动) {
            val 镜头参考系下的移动Yaw = kotlin.math.atan2(intentRight, intentForward)
            MathHelper.wrapDegrees((CommonSettings.cameraYaw + 镜头参考系下的移动Yaw * RAD_TO_DEG_F))
        } else {
            player.yaw
        }


        player.yaw = targetYaw
        player.pitch = CommonSettings.cameraPitch



        val yawDiff = Math.toRadians((CommonSettings.cameraYaw - player.yaw).toDouble())
        val cos = kotlin.math.cos(yawDiff).toFloat()
        val sin = kotlin.math.sin(yawDiff).toFloat()

        val actualForward = intentForward * cos - intentRight * sin
        val actualRight = intentForward * sin + intentRight * cos
        val 移动分量阈值 = 移动分量阈值.get().toFloat()
        mc.options.forwardKey.isPressed = actualForward > 移动分量阈值
        mc.options.backKey.isPressed = actualForward < -移动分量阈值
        mc.options.rightKey.isPressed = actualRight > 移动分量阈值
        mc.options.leftKey.isPressed = actualRight < -移动分量阈值
    }
}
