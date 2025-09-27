package ru.edenor.changeMyHeight

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import java.time.Duration
import java.util.*
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Sound
import org.bukkit.SoundCategory
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
import ru.edenor.changeMyHeight.command.PotionListMessenger
import ru.edenor.changeMyHeight.data.ConfigStorage
import ru.edenor.changeMyHeight.data.Potion
import ru.edenor.changeMyHeight.data.Storage

class ChangeMyHeight : JavaPlugin(), Listener {

  private val activeTasks: MutableMap<UUID, BukkitTask> = mutableMapOf()

  override fun onEnable() {
    storage = ConfigStorage(this)

    remainingKey = NamespacedKey(this, "size-remaining")
    startKey = NamespacedKey(this, "size-start")
    potionNameKey = NamespacedKey(this, "size-potion-name")

    server.pluginManager.registerEvents(this, this)

    lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { commands ->
      Command(this, storage).commands().forEach { commands.registrar().register(it) }
    }
  }

  override fun onDisable() {
    server.onlinePlayers.forEach { resetScale(it) }
    activeTasks.values.forEach { it.cancel() }
    activeTasks.clear()
  }

  fun reload() {
    storage.reload()
  }

  @EventHandler
  fun onDrink(event: PlayerItemConsumeEvent) {
    val player = event.player
    val item = event.item
    if (item.type != Material.POTION || !item.hasItemMeta()) return

    val meta = item.itemMeta as PotionMeta
    val pdc = meta.persistentDataContainer
    if (!pdc.has(potionNameKey, PersistentDataType.STRING)) return

    val potionName = pdc.get(potionNameKey, PersistentDataType.STRING)
    val potion = storage.getPotion(potionName!!)!!

    applyScale(player, potion)
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
    if (remaining <= 0) return

    val potionName = pdc.get(potionNameKey, PersistentDataType.STRING) ?: return
    val potion = storage.getPotion(potionName) ?: return

    pdc.set(startKey, PersistentDataType.LONG, System.currentTimeMillis())

    applyWithRemaining(player, potion, remaining)
  }

  private fun applyWithRemaining(player: Player, potion: Potion, remaining: Long) {
    val pdc = player.persistentDataContainer
    val potionScale = potion.scale
    val potionColor = potion.color

    pdc.set(remainingKey, PersistentDataType.LONG, remaining)
    pdc.set(potionNameKey, PersistentDataType.STRING, potion.name)

    player.getAttribute(Attribute.SCALE)?.baseValue = potionScale
    activeTasks[player.uniqueId]?.cancel()

    val timeLeft = PotionListMessenger.pluralDuration(Duration.ofMillis(remaining))
    player.sendRichMessage(
        "<green>На вас действует зелье <gray>[</gray>" +
            "<hover:show_text:'${potion.description}'><${potionColor}>${potion.title}</${potionColor}></hover>" +
            "<gray>]</gray> - осталось $timeLeft")

    val task = server.scheduler.runTaskLater(this, Runnable { resetScale(player) }, remaining / 50)
    activeTasks[player.uniqueId] = task
  }

  private fun applyScale(player: Player, potion: Potion) {
    val pdc = player.persistentDataContainer
    val potionDuration = potion.duration.toMillis()
    val potionName = potion.name
    val potionScale = potion.scale
    val potionColor = potion.color
    val currentTime = System.currentTimeMillis()

    pdc.set(remainingKey, PersistentDataType.LONG, potionDuration)
    pdc.set(startKey, PersistentDataType.LONG, currentTime)
    pdc.set(potionNameKey, PersistentDataType.STRING, potionName)

    player.getAttribute(Attribute.SCALE)?.baseValue = potionScale
    activeTasks[player.uniqueId]?.cancel()

    val timeLeft = PotionListMessenger.pluralDuration(potion.duration)
    player.sendRichMessage(
        "<green>На вас действует зелье <gray>[</gray>" +
            "<hover:show_text:'${potion.description}'><${potionColor}>${potion.title}</${potionColor}></hover>" +
            "<gray>]</gray> - осталось $timeLeft")
    val task =
        server.scheduler.runTaskLater(this, Runnable { resetScale(player) }, potionDuration / 50)
    activeTasks[player.uniqueId] = task
  }

  private fun stopTaskAndSaveRemaining(player: Player) {
    val task = activeTasks.remove(player.uniqueId)
    task?.cancel()

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

  private fun resetScale(player: Player) {
    val attr = player.getAttribute(Attribute.SCALE)
    if (attr == null) {
      logger.warning("У игрока ${player.name} нет атрибута SCALE!")
    } else {
      attr.baseValue = 1.0
    }
    val pdc = player.persistentDataContainer
    val potionName = pdc.get(potionNameKey, PersistentDataType.STRING) ?: return
    val potion = storage.getPotion(potionName) ?: return
    val potionColor = potion.color

    pdc.remove(remainingKey)
    pdc.remove(potionNameKey)
    pdc.remove(startKey)

    player.playSound(player, Sound.BLOCK_BEACON_DEACTIVATE, SoundCategory.UI, 1f, 1.4f)
    player.sendRichMessage(
        "<green>Зелье <gray>[</gray>" +
            "<hover:show_text:'${potion.description}'><${potionColor}>${potion.title}</${potionColor}></hover>" +
            "<gray>]</gray> закончилось, размер возвращён в норму!")
  }

  companion object {
    const val GIVE_PERMISSION = "cmh.give"
    const val USE_PERMISSION = "cmh.use"
    const val POTION_SECTION = "potions"
    const val POTION_FILENAME = "config.yml"
    lateinit var remainingKey: NamespacedKey
    lateinit var startKey: NamespacedKey
    lateinit var potionNameKey: NamespacedKey
    lateinit var storage: Storage
  }
}
