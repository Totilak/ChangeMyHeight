package ru.edenor.changeMyHeight.handler

import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.persistence.PersistentDataType
import ru.edenor.changeMyHeight.ChangeMyHeight.Companion.potionNameKey
import ru.edenor.changeMyHeight.ChangeMyHeight.Companion.remainingKey
import ru.edenor.changeMyHeight.ChangeMyHeight.Companion.storage
import ru.edenor.changeMyHeight.ChangeMyHeightService

class PlayerHandler : Listener {

  @EventHandler
  fun onDrink(event: PlayerItemConsumeEvent) {
    val player = event.player
    val item = event.item
    if (item.type != Material.POTION || !item.hasItemMeta()) return

    val pdc = item.itemMeta.persistentDataContainer
    val potionName = pdc.get(potionNameKey, PersistentDataType.STRING) ?: return
    val potion = storage.getPotion(potionName) ?: return

    ChangeMyHeightService.applyPotion(player, potion)
  }

  @EventHandler
  fun onJoin(event: PlayerJoinEvent) {
    val player = event.player
    val pdc = player.persistentDataContainer

    if (pdc.has(remainingKey, PersistentDataType.LONG)) {
      ChangeMyHeightService.startTask(player)
      ChangeMyHeightService.sendPotionInfo(player)
    }
  }

  @EventHandler
  fun onQuit(event: PlayerQuitEvent) {
    val player = event.player
    ChangeMyHeightService.stopTaskAndSaveRemaining(player)
  }
}
