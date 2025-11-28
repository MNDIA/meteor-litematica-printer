package com.kkllffaa.meteor_litematica_printer.Modules.Tools

import com.kkllffaa.meteor_litematica_printer.Addon
import com.kkllffaa.meteor_litematica_printer.Modules.CRUD.CRUDMainPanel.Printer
import meteordevelopment.meteorclient.events.entity.player.ItemUseCrosshairTargetEvent
import meteordevelopment.meteorclient.events.world.TickEvent
import meteordevelopment.meteorclient.pathing.PathManagers
import meteordevelopment.meteorclient.settings.*
import meteordevelopment.meteorclient.systems.modules.Module
import meteordevelopment.meteorclient.systems.modules.Modules
import meteordevelopment.meteorclient.systems.modules.combat.AnchorAura
import meteordevelopment.meteorclient.systems.modules.combat.BedAura
import meteordevelopment.meteorclient.systems.modules.combat.CrystalAura
import meteordevelopment.meteorclient.systems.modules.combat.KillAura
import meteordevelopment.meteorclient.systems.modules.player.AutoEat
import meteordevelopment.meteorclient.systems.modules.player.AutoGap
import meteordevelopment.meteorclient.utils.Utils
import meteordevelopment.meteorclient.utils.misc.input.Input
import meteordevelopment.meteorclient.utils.player.InvUtils
import meteordevelopment.meteorclient.utils.player.SlotUtils
import meteordevelopment.orbit.EventHandler
import meteordevelopment.orbit.EventPriority
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.FoodComponent
import net.minecraft.item.Item
import net.minecraft.item.Items
import java.util.function.BiPredicate

// import com.kkllffaa.meteor_litematica_printer.Modules.CRUD.CRUDMainPanel.Deleter;
class AutoEat : Module(Addon.TOOLS, "auto-eat-+", "Automatically eats food.") {
    // Settings groups
    private val sgGeneral = settings.defaultGroup
    private val sgThreshold = settings.createGroup("Threshold")

    // General
    val blacklist: Setting<MutableList<Item>> = sgGeneral.add(
        ItemListSetting.Builder()
            .name("blacklist")
            .description("Which items to not eat.")
            .defaultValue(
                Items.ENCHANTED_GOLDEN_APPLE,
                Items.GOLDEN_APPLE,
                Items.CHORUS_FRUIT,
                Items.POISONOUS_POTATO,
                Items.PUFFERFISH,
                Items.CHICKEN,
                Items.ROTTEN_FLESH,
                Items.SPIDER_EYE,
                Items.SUSPICIOUS_STEW
            )
            .filter { item: Item ->
                item.components.get<FoodComponent>(DataComponentTypes.FOOD) != null
            }
            .build()
    )

    private val pauseAuras: Setting<Boolean> = sgGeneral.add(
        BoolSetting.Builder()
            .name("pause-auras")
            .description("Pauses all auras when eating.")
            .defaultValue(true)
            .build()
    )

    private val pauseBaritone: Setting<Boolean> = sgGeneral.add(
        BoolSetting.Builder()
            .name("pause-baritone")
            .description("Pause baritone when eating.")
            .defaultValue(true)
            .build()
    )

    // Threshold
    private val thresholdMode: Setting<ThresholdMode> = sgThreshold.add(
        EnumSetting.Builder<ThresholdMode>()
            .name("threshold-mode")
            .description("The threshold mode to trigger auto eat.")
            .defaultValue(ThresholdMode.Any)
            .build()
    )

    private val healthThreshold: Setting<Double> = sgThreshold.add(
        DoubleSetting.Builder()
            .name("health-threshold")
            .description("The level of health you eat at.")
            .defaultValue(19.0)
            .range(1.0, 19.0)
            .sliderRange(1.0, 19.0)
            .visible { thresholdMode.get() != ThresholdMode.Hunger }
            .build()
    )

    private val hungerThreshold: Setting<Int> = sgThreshold.add(
        IntSetting.Builder()
            .name("hunger-threshold")
            .description("The level of hunger you eat at.")
            .defaultValue(7)
            .range(1, 19)
            .sliderRange(1, 19)
            .visible { thresholdMode.get() != ThresholdMode.Health }
            .build()
    )

    // Module state
    var eating: Boolean = false
    private var slot = 0
    private var prevSlot = 0

    private val wasAura: MutableList<Class<out Module>> = ArrayList()
    private var wasBaritone = false

    override fun onActivate() {
        resolveModuleConflict()
    }

    private fun resolveModuleConflict() {
        val meteorAutoEat = Modules.get().get(AutoEat::class.java)
        if (meteorAutoEat != null && meteorAutoEat.isActive) {
            meteorAutoEat.toggle()
        }
    }

    override fun onDeactivate() {
        if (eating) stopEating()
    }

    /**
     * Main tick handler for the module's eating logic
     */
    @EventHandler(priority = EventPriority.LOW)
    private fun onTick(event: TickEvent.Pre) {
        // Don't eat if AutoGap is already eating
        if (Modules.get().get(AutoGap::class.java)?.isEating == true) return

        // case 1: Already eating
        if (eating) {
            // Stop eating if we shouldn't eat anymore
            if (!shouldEat()) {
                stopEating()
                return
            }

            // Check if the item in current slot is not food anymore
            if (mc.player?.getInventory()?.getStack(slot)?.get(DataComponentTypes.FOOD) == null) {
                val newSlot = findSlot()

                // Stop if no food found
                if (newSlot == -1) {
                    stopEating()
                    return
                }

                changeSlot(newSlot)
            }

            // Continue eating the food
            eat()
            return
        }

        // case 2: Not eating yet but should start
        if (shouldEat()) {
            startEating()
        }
    }

    @EventHandler
    private fun onItemUseCrosshairTarget(event: ItemUseCrosshairTargetEvent) {
        if (eating) event.target = null
    }

    private fun startEating() {
        prevSlot = mc.player?.getInventory()?.selectedSlot ?: prevSlot
        eat()

        // Pause auras
        wasAura.clear()
        if (pauseAuras.get()) {
            for (klass in AURAS) {
                val module: Module = Modules.get().get(klass)

                if (module.isActive) {
                    wasAura.add(klass)
                    module.toggle()
                }
            }
        }

        // Pause baritone
        if (pauseBaritone.get() && PathManagers.get().isPathing && !wasBaritone) {
            wasBaritone = true
            PathManagers.get().pause()
        }
    }

    private fun eat() {
        changeSlot(slot)
        setPressed(true)
        if (mc.player?.isUsingItem == false) Utils.rightClick()

        eating = true
    }

    private fun stopEating() {
        if (prevSlot != SlotUtils.OFFHAND) changeSlot(prevSlot)
        setPressed(false)

        eating = false

        // Resume auras
        if (pauseAuras.get()) {
            for (klass in AURAS) {
                val module: Module = Modules.get().get(klass)

                if (klass in wasAura && !module.isActive) {
                    module.toggle()
                }
            }
        }

        // Resume baritone
        if (pauseBaritone.get() && wasBaritone) {
            wasBaritone = false
            PathManagers.get().resume()
        }
    }

    private fun setPressed(pressed: Boolean) {
        mc.options.useKey.isPressed = pressed
    }

    private fun changeSlot(slot: Int) {
        InvUtils.swap(slot, false)
        this.slot = slot
    }

    fun shouldEat(): Boolean {
        val player = mc.player ?: return false
        val healthLow = player.health <= healthThreshold.get()
        val hungerLow = player.getHungerManager().foodLevel <= hungerThreshold.get()
        slot = findSlot()
        if (slot == -1) return false

        val food = player.getInventory().getStack(slot).get(DataComponentTypes.FOOD) ?: return false

        if (Input.isPressed(mc.options.attackKey) || Input.isPressed(mc.options.useKey)) {
            return false
        }

        return thresholdMode.get().test(healthLow, hungerLow)
                && (player.getHungerManager().isNotFull || food.canAlwaysEat())
    }

    private fun findSlot(): Int {
        var slot = -1
        var bestHunger = -1
        val player = mc.player ?: return slot

        for (i in 0..8) {
            // Skip if item isn't food
            val item = player.getInventory().getStack(i).item
            val foodComponent = item.components.get(DataComponentTypes.FOOD) ?: continue

            // Check if hunger value is better
            val hunger = foodComponent.nutrition()
            if (hunger > bestHunger) {
                // Skip if item is in blacklist
                if (blacklist.get().contains(item)) continue

                // Select the current item
                slot = i
                bestHunger = hunger
            }
        }

        val offHandItem = player.offHandStack.item
        val offHandFood = offHandItem.components.get(DataComponentTypes.FOOD)
        if (offHandFood != null && !blacklist.get().contains(offHandItem) && offHandFood.nutrition() > bestHunger) {
            slot = SlotUtils.OFFHAND
        }

        return slot
    }

    enum class ThresholdMode(private val predicate: BiPredicate<Boolean, Boolean>) {
        Health(BiPredicate { health: Boolean, hunger: Boolean -> health }),
        Hunger(BiPredicate { health: Boolean, hunger: Boolean -> hunger }),
        Any(BiPredicate { health: Boolean, hunger: Boolean -> health || hunger }),
        Both(BiPredicate { health: Boolean, hunger: Boolean -> health && hunger });

        fun test(health: Boolean, hunger: Boolean): Boolean = predicate.test(health, hunger)
    }

    companion object {
        private val AURAS = arrayOf(
            KillAura::class.java,
            CrystalAura::class.java,
            AnchorAura::class.java,
            BedAura::class.java,  // Deleter.class,
            Printer::class.java
        )
    }
}
