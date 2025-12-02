package com.kkllffaa.meteor_litematica_printer.Modules.Tools


import com.kkllffaa.meteor_litematica_printer.Modules.CRUD.AtomicSettings.CommonSettings
import com.kkllffaa.meteor_litematica_printer.Functions.*
import com.kkllffaa.meteor_litematica_printer.Addon
import meteordevelopment.meteorclient.systems.modules.Module
import meteordevelopment.meteorclient.events.world.TickEvent
import meteordevelopment.meteorclient.settings.*
import meteordevelopment.meteorclient.systems.modules.Modules
import meteordevelopment.meteorclient.systems.modules.render.Freecam
import meteordevelopment.meteorclient.utils.misc.input.Input
import meteordevelopment.orbit.EventHandler
import net.minecraft.client.option.Perspective
import net.minecraft.util.math.MathHelper
import kotlin.math.abs
import kotlin.random.Random


object BetterThirdPerson : Module(Addon.TOOLS, "BetterThirdPerson", "") {
    private val sgGeneral = settings.defaultGroup
    private val 移动分量阈值: Setting<Double> = sgGeneral.add(
        DoubleSetting.Builder()
            .name("Movement sensitivity")
            .description("The minimum movement input required to move the player while in better third person.")
            .defaultValue(0.3)
            .range(0.001, 1.0)
            .sliderRange(0.001, 1.0)
            .build()
    )
    val 禁用第三人称Front: Setting<Boolean> = sgGeneral.add(
        BoolSetting.Builder()
            .name("Disable Third Person Front")
            .description("Perspective.THIRD_PERSON_FRONT")
            .defaultValue(true)
            .build()
    )
    private val 旋转加速度: Setting<Double> = sgGeneral.add(
        DoubleSetting.Builder()
            .name("Rotation Acceleration")
            .description("How fast the rotation speed increases (degrees/tick²)")
            .defaultValue(8.0)
            .range(1.0, 50.0)
            .sliderRange(1.0, 50.0)
            .build()
    )
    private var 当前旋转速度: Float = 0f

    override fun onActivate() {
        onPerspectiveChanged(mc.options.perspective)
    }

    override fun onDeactivate() {
        尝试退出第三人称代理()
    }

    private val 第三人称代理中 get() = CommonSettings.OnlyRotateCam.get()
    private fun 尝试进入第三人称代理() {
        if (第三人称代理中) return
        当前旋转速度 = 0f
        CommonSettings.OnlyRotateCam.set(true)
    }

    private fun 尝试退出第三人称代理() {
        if (!第三人称代理中) return
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
        if (mc.options.perspective != Perspective.THIRD_PERSON_BACK || !第三人称代理中 || Modules.get().isActive(Freecam::class.java)) return
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
            CommonSettings.cameraYaw + 镜头参考系下的移动Yaw * RAD_TO_DEG_F
        } else {
            player.yaw
        }

        val yawDelta = MathHelper.wrapDegrees(targetYaw - player.yaw)
        val absYawDelta = kotlin.math.abs(yawDelta)
        val 加速度 = 旋转加速度.get().toFloat()
        val smoothYaw = if (absYawDelta < 0.1f) {
            当前旋转速度 = 0f
            player.yaw + yawDelta
        } else {
            val direction = kotlin.math.sign(yawDelta)
            val 刹车距离 = (当前旋转速度 * 当前旋转速度) / (2f * 加速度)

            if (absYawDelta <= 刹车距离 && direction * 当前旋转速度 > 0) {
                当前旋转速度 -= direction * 加速度
                if (direction * 当前旋转速度 <= 0 || abs(当前旋转速度) > absYawDelta) {
                    当前旋转速度 = 0f
                    player.yaw + yawDelta / 2
                } else {
                    player.yaw + 当前旋转速度
                }
            } else {
                当前旋转速度 += direction * 加速度
                if (direction * 当前旋转速度 > 0 && abs(当前旋转速度) > absYawDelta) {
                    当前旋转速度 = 0f
                    player.yaw + yawDelta / 2
                } else {
                    player.yaw + 当前旋转速度
                }
            }
        }
        player.yaw = if (Random.nextFloat() < 0.05f) player.yaw else smoothYaw
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
