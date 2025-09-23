package ru.edenor.changeMyHeight

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import org.bukkit.*
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import ru.edenor.changeMyHeight.command.Command
import java.util.*

class ChangeMyHeight : JavaPlugin(), Listener {

  private val activeTasks: MutableMap<UUID, BukkitTask> = mutableMapOf()

  override fun onEnable() {
    sizeKey = NamespacedKey(this, "size-potion")
    remainingKey = NamespacedKey(this, "size-remaining")
    startKey = NamespacedKey(this, "size-start")

    server.pluginManager.registerEvents(this, this)

    lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { commands ->
      Command(this).commands().forEach { commands.registrar().register(it) }
    }

    logger.info("ChangeMyHeight enabled!")
  }

  override fun onDisable() {
    server.onlinePlayers.forEach { resetScale(it) }
    activeTasks.values.forEach { it.cancel() }
    activeTasks.clear()
    logger.info("ChangeMyHeight disabled!")
  }

  @EventHandler
  fun onDrink(event: PlayerItemConsumeEvent) {
    val player = event.player
    val item = event.item
    if (item.type != Material.POTION || !item.hasItemMeta()) return

    val meta = item.itemMeta as PotionMeta
    val container = meta.persistentDataContainer
    if (!container.has(sizeKey, PersistentDataType.DOUBLE)) return

    val scale = container.get(sizeKey, PersistentDataType.DOUBLE) ?: return
    val durationMs = 60_000L
    applyScale(player, scale, durationMs)
  }

  @EventHandler
  fun onQuit(event: PlayerQuitEvent) {
    val player = event.player
    stopTaskAndSaveRemaining(player)
  }

  @EventHandler
  fun onJoin(event: PlayerJoinEvent) {
    val player = event.player
    val pdc = player.persistentDataContainer

    val remaining = pdc.get(remainingKey, PersistentDataType.LONG) ?: return
    val scale = pdc.get(sizeKey, PersistentDataType.DOUBLE) ?: return

    if (remaining > 0) {
      applyScale(player, scale, remaining)
    }
  }

  private fun applyScale(player: Player, scale: Double, durationMs: Long) {
    val pdc = player.persistentDataContainer

    pdc.set(sizeKey, PersistentDataType.DOUBLE, scale)
    pdc.set(remainingKey, PersistentDataType.LONG, durationMs)

    val currentTime = System.currentTimeMillis()
    pdc.set(startKey, PersistentDataType.LONG, currentTime)

    player.getAttribute(Attribute.SCALE)?.baseValue = scale
    val effectType =
        if (scale < 1.0) "<aqua>Крошкинс</aqua>"
        else if (scale > 1.0) "<blue>Гигантизм</blue>" else "<white>Нормализация</white>"
    val timeDuration = durationMs / 1000
    player.sendRichMessage(
        "<green>На вас действует зелье <gray>[</gray>$effectType<gray>]</gray>, осталось $timeDuration секунд")
    activeTasks[player.uniqueId]?.cancel()

    when {
      scale < 1.0 -> playPotionParticles(player, "magic")
      scale > 1.0 -> playPotionParticles(player, "fire")
      else -> playPotionParticles(player, "cosmic")
    }

    val task = server.scheduler.runTaskLater(this, Runnable { resetScale(player) }, durationMs / 50)

    activeTasks[player.uniqueId] = task
  }

  fun playPotionParticles(player: Player, type: String) {
    val loc = player.location.add(0.0, 1.0, 0.0)
    val world = player.world

    when (type.lowercase()) {
      "magic" -> {
        world.spawnParticle(Particle.WITCH, loc, 40, 0.5, 1.0, 0.5, 0.1)
        world.spawnParticle(Particle.ENCHANT, loc, 60, 1.0, 1.0, 1.0, 0.0)
        player.playSound(player, Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.UI, 1f, 1.6f)
      }

      "fire" -> {
        world.spawnParticle(Particle.FLAME, loc, 30, 0.3, 0.5, 0.3, 0.01)
        world.spawnParticle(Particle.SMALL_FLAME, loc, 20, 0.3, 0.5, 0.3, 0.0)
        world.spawnParticle(Particle.FLASH, loc, 1)
        player.playSound(player, Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.UI, 1f, 1.4f)
      }

      "cosmic" -> {
        world.spawnParticle(Particle.DRAGON_BREATH, loc, 30, 0.5, 0.8, 0.5, 0.0)
        world.spawnParticle(Particle.END_ROD, loc, 15, 0.2, 0.3, 0.2, 0.0)
        player.playSound(player, Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.UI, 1f, 1.2f)
      }

      else -> {
        world.spawnParticle(Particle.TOTEM_OF_UNDYING, loc, 1)
      }
    }
  }

  private fun stopTaskAndSaveRemaining(player: Player) {
    val task = activeTasks.remove(player.uniqueId)
    if (task != null) {
      task.cancel()
      val pdc = player.persistentDataContainer

      val remaining = pdc.get(remainingKey, PersistentDataType.LONG) ?: return
      val start = pdc.get(startKey, PersistentDataType.LONG) ?: return

      val elapsed = System.currentTimeMillis() - start
      val newRemaining = remaining - elapsed

      if (newRemaining > 0) {
        pdc.set(remainingKey, PersistentDataType.LONG, newRemaining)
      } else {
        resetScale(player)
      }
    }
  }

  private fun resetScale(player: Player) {
    player.getAttribute(Attribute.SCALE)?.baseValue = 1.0
    val pdc = player.persistentDataContainer
    pdc.remove(sizeKey)
    pdc.remove(remainingKey)
    pdc.remove(startKey)

    player.playSound(player, Sound.BLOCK_BEACON_DEACTIVATE, SoundCategory.UI, 1f, 1.4f)
    player.sendRichMessage("<green>Зелье закончилось, размер возвращён в норму!")
  }

  companion object {
    const val GIVE_PERMISSION = "cmh.give"
    const val USE_PERMISSION = "cmh.use"
    lateinit var sizeKey: NamespacedKey
    lateinit var remainingKey: NamespacedKey
    lateinit var startKey: NamespacedKey
  }
}
