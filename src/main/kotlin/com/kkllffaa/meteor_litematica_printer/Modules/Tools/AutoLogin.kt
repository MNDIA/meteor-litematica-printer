package com.kkllffaa.meteor_litematica_printer.Modules.Tools

import com.kkllffaa.meteor_litematica_printer.Addon
import meteordevelopment.meteorclient.events.game.OpenScreenEvent
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent
import meteordevelopment.meteorclient.events.world.TickEvent
import meteordevelopment.meteorclient.settings.*
import meteordevelopment.meteorclient.systems.modules.Module
import meteordevelopment.meteorclient.systems.modules.Modules
import meteordevelopment.meteorclient.utils.player.ChatUtils
import meteordevelopment.meteorclient.utils.player.InvUtils
import meteordevelopment.orbit.EventHandler
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.text.Text
import net.minecraft.util.Hand

class AutoLogin : Module(Addon.TOOLS, "auto-login", "Automatically logs in when receiving specific messages.") {
    private enum class LoginState {
        预备登录,
        等待登陆成功,
        预备打开菜单,
        等待打开菜单,
        预备点击入口,
        等待进入服务器,
        预备待命命令,
        等待传送完成,
        预备待命状态
    }

    private val sgGeneral: SettingGroup = settings.getDefaultGroup()

    private val triggerMessage: Setting<String?> = sgGeneral.add<String?>(
        StringSetting.Builder()
            .name("trigger-message")
            .description("The message that triggers the auto login.")
            .defaultValue("Please login with")
            .build()
    )

    private val loginCommands: Setting<MutableList<String>> = sgGeneral.add<MutableList<String?>?>(
        StringListSetting.Builder()
            .name("login-commands")
            .description("List of player name to command mappings. Format: player:login_command:standby_command:standby_state")
            .defaultValue(ArrayList<String?>())
            .build()
    )

    private val successMessage: Setting<String?> = sgGeneral.add<String?>(
        StringSetting.Builder()
            .name("success-message")
            .description("The message that indicates a successful login.")
            .defaultValue("Login suc")
            .build()
    )

    private val 菜单物品包含名字: Setting<String?> = sgGeneral.add<String?>(
        StringSetting.Builder()
            .name("menu-item-name-keyword")
            .description("The string that the item name must contain to be used after login.")
            .defaultValue("")
            .build()
    )

    private val 服务器入口物品包含名字: Setting<String?> = sgGeneral.add<String?>(
        StringSetting.Builder()
            .name("server-item-name-keyword")
            .description("The string that the item name in the GUI must contain to be clicked.")
            .defaultValue(">>>")
            .build()
    )

    private val 主城大区Message: Setting<String?> = sgGeneral.add<String?>(
        StringSetting.Builder()
            .name("main-city-region-message")
            .description("The message that indicates the main city region.")
            .defaultValue("进入了 [主城大区]")
            .build()
    )

    private val 生存大区Message: Setting<String?> = sgGeneral.add<String?>(
        StringSetting.Builder()
            .name("survival-region-message")
            .description("The message that indicates the survival region.")
            .defaultValue("进入了 [生存")
            .build()
    )

    private val CheckTicks: Setting<Int?> = sgGeneral.add<Int?>(
        IntSetting.Builder()
            .name("check-ticks")
            .description("执行步骤后延迟后检查执行结果是否成功.")
            .defaultValue(300)
            .min(0)
            .build()
    )

    private val readyTicks: Setting<Int?> = sgGeneral.add<Int?>(
        IntSetting.Builder()
            .name("delay-ticks")
            .description("预备执行的反应时间.")
            .defaultValue(3)
            .min(0)
            .max(100)
            .build()
    )

    private val INFO: Setting<Boolean?> = sgGeneral.add<Boolean?>(
        BoolSetting.Builder()
            .name("info")
            .description("Show info messages when auto login is triggered.")
            .defaultValue(false)
            .build()
    )

    private var loginState = LoginState.预备登录
    private var delayCounter = 0

    override fun info(message: String?, vararg args: Any?) {
        if (INFO.get()) {
            super.info(message, *args)
        }
    }

    override fun info(message: Text?) {
        if (INFO.get()) {
            super.info(message)
        }
    }

    override fun onActivate() {
        if (loginState == LoginState.等待登陆成功 || loginState == LoginState.等待打开菜单 || loginState == LoginState.等待进入服务器 || loginState == LoginState.等待传送完成) return
        预备登录()
    }

    private fun 预备登录() {
        loginState = LoginState.预备登录
        delayCounter = 0
    }

    @EventHandler
    private fun onReceiveMessage(event: ReceiveMessageEvent) {
        if (mc.player == null) return
        val originalText = event.getMessage()
        val originalIndicator = event.getIndicator()
        val originalId = event.id

        val message = originalText.getString()

        if (message.contains(triggerMessage.get()!!)) {
            if (loginState == LoginState.预备登录) {
                if (输入登录命令()) {
                    loginState = LoginState.等待登陆成功
                    delayCounter = CheckTicks.get()!!
                } else {
                    预备登录()
                }
            }
        } else if (message.contains(successMessage.get()!!)) {
            if (loginState == LoginState.等待登陆成功) {
                loginState = LoginState.预备打开菜单
                delayCounter = readyTicks.get()!!
            }
        } else if (message.contains(mc.player!!.getName().getString()) && message.contains(主城大区Message.get()!!)) {
            info("进入了 [主城大区] State: %s", loginState)
            if (loginState == LoginState.等待进入服务器) {
                loginState = LoginState.预备待命命令
                delayCounter = readyTicks.get()!! + 40
            }
        } else if (message.contains(mc.player!!.getName().getString()) && message.contains(生存大区Message.get()!!)) {
            info("进入了 [生存大区] State: %s", loginState)
            if (loginState == LoginState.等待传送完成) {
                loginState = LoginState.预备待命状态
                delayCounter = readyTicks.get()!!
            }
        }

        if (event.getMessage() !== originalText) {
            event.setMessage(originalText)
        }
        if (event.getIndicator() !== originalIndicator) {
            event.setIndicator(originalIndicator)
        }
        if (event.id != originalId) {
            event.id = originalId
        }
    }

    @EventHandler
    private fun onOpenScreen(event: OpenScreenEvent) {
        if (event.screen is GenericContainerScreen) {
            if (loginState == LoginState.等待打开菜单) {
                loginState = LoginState.预备点击入口
                delayCounter = readyTicks.get()!!
            }
        }
    }

    @EventHandler
    private fun onTick(event: TickEvent.Post?) {
        if (delayCounter > 0) {
            delayCounter--
        } else {
            when (loginState) {
                LoginState.预备登录 -> {
                    return
                }

                LoginState.等待登陆成功, LoginState.等待打开菜单, LoginState.等待进入服务器, LoginState.等待传送完成 -> {
                    info("State %s timeout, resetting.", loginState)
                    // 延迟结束但没有成功消息，重置
                    预备登录()
                }

                LoginState.预备打开菜单 -> {
                    if (搜索hotbar打开菜单物品()) {
                        loginState = LoginState.等待打开菜单
                        delayCounter = CheckTicks.get()!!
                    } else {
                        loginState = LoginState.预备待命命令
                    }
                }

                LoginState.预备点击入口 -> {
                    if (搜索菜单点击入口物品()) {
                        info("Clicked entry item, waiting to enter server...")
                        loginState = LoginState.等待进入服务器
                        delayCounter = CheckTicks.get()!!
                    } else {
                        loginState = LoginState.预备待命命令
                    }
                }

                LoginState.预备待命命令 -> {
                    if (输入待命命令()) {
                        loginState = LoginState.等待传送完成
                        delayCounter = CheckTicks.get()!!
                    } else {
                        预备登录()
                    }
                }

                LoginState.预备待命状态 -> {
                    打开待机状态()
                    预备登录()
                }
            }
        }
    }

    private fun 打开待机状态() {
        val playerName = mc.player!!.getName().getString()
        val commandsList = loginCommands.get()
        for (pair in commandsList) {
            val parts: Array<String?> = pair.split(":".toRegex(), limit = 4).toTypedArray()
            if (parts.size >= 4 && parts[0]!!.trim { it <= ' ' } == playerName) {
                val standbyState = parts[3]!!.trim { it <= ' ' }
                if ("挂机" == standbyState) {
                    val hangUpModule = Modules.get().get<HangUp?>(HangUp::class.java)
                    if (hangUpModule == null) return
                    if (hangUpModule.isActive()) {
                        hangUpModule.toggle()
                        hangUpModule.toggle()
                    } else hangUpModule.toggle()
                } else if ("商店限量" == standbyState) {
                    val shopLimiter = Modules.get().get<ShopLimiter?>(ShopLimiter::class.java)
                    if (shopLimiter == null) return
                    if (!shopLimiter.isActive()) shopLimiter.toggle()
                }
                return
            }
        }
    }

    private fun 输入登录命令(): Boolean {
        val playerName = mc.player!!.getName().getString()
        val commandsList = loginCommands.get()

        for (pair in commandsList) {
            val parts: Array<String?> = pair.split(":".toRegex(), limit = 4).toTypedArray()
            if (parts.size >= 2 && parts[0]!!.trim { it <= ' ' } == playerName) {
                val command = parts[1]!!.trim { it <= ' ' }
                ChatUtils.sendPlayerMsg(command)
                info("%s with command: %s", playerName, command)
                return true
            }
        }
        return false
    }

    private fun 输入待命命令(): Boolean {
        val playerName = mc.player!!.getName().getString()
        val commandsList = loginCommands.get()

        for (pair in commandsList) {
            val parts: Array<String?> = pair.split(":".toRegex(), limit = 4).toTypedArray()
            if (parts.size >= 3 && parts[0]!!.trim { it <= ' ' } == playerName) {
                val standbyCommand = parts[2]!!.trim { it <= ' ' }
                if (!standbyCommand.isEmpty()) {
                    ChatUtils.sendPlayerMsg(standbyCommand)
                    info("%s standby command: %s", playerName, standbyCommand)
                }
                return true
            }
        }
        return false
    }

    private fun 搜索hotbar打开菜单物品(): Boolean {
        if (菜单物品包含名字.get()!!.isEmpty()) return true
        info("Looking for menu item with name containing: %s in hotbar", 菜单物品包含名字.get())
        for (slot in 0..8) {
            val stack = mc.player!!.getInventory().getStack(slot)
            if (!stack.isEmpty() && stack.getName().getString().contains(菜单物品包含名字.get()!!)) {
                InvUtils.swap(slot, false)
                mc.interactionManager!!.interactItem(mc.player, Hand.MAIN_HAND)
                info("Used item: %s", stack.getName().getString())
                return true
            }
        }
        return false
    }

    private fun 搜索菜单点击入口物品(): Boolean {
        if (服务器入口物品包含名字.get()!!.isEmpty()) return true
        val slots = mc.player!!.currentScreenHandler.slots
        info("Looking for entry item with name containing: %s in %s slots", 服务器入口物品包含名字.get(), slots.size)
        for (slot in slots) {
            if (slot.hasStack()) {
                val name = slot.getStack().getName().getString()
                info("Found item in GUI: %s", name)
                if (name.contains(服务器入口物品包含名字.get()!!)) {
                    InvUtils.click().slotId(slot.id)
                    info("Clicked item in GUI: %s", name)
                    return true
                }
            }
        }
        return false
    }
}
