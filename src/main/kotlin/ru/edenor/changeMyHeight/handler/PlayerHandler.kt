package ru.edenor.changeMyHeight.handler

import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.persistence.PersistentDataType
import ru.edenor.changeMyHeight.ChangeMyHeight.Companion.potionKey
import ru.edenor.changeMyHeight.ChangeMyHeight.Companion.storage
import ru.edenor.changeMyHeight.ChangeMyHeightService
import ru.edenor.changeMyHeight.ChangeMyHeightService.getPotionData

class PlayerHandler : Listener {

  @EventHandler
  fun onDrink(event: PlayerItemConsumeEvent) {
    val player = event.player
    val item = event.item

    if (item.type == Material.MILK_BUCKET) {
      ChangeMyHeightService.clearPotionEffects(player)
      return
    }

    if (item.type != Material.POTION || !item.hasItemMeta()) return

    val pdc = item.itemMeta.persistentDataContainer
    val potionName = pdc.get(potionKey, PersistentDataType.STRING) ?: return
    val potion = storage.getPotion(potionName) ?: return

    ChangeMyHeightService.applyPotion(player, potion)
    ChangeMyHeightService.effectOnDrink(player, potion)
  }

  @EventHandler
  fun onJoin(event: PlayerJoinEvent) {
    val player = event.player
    val potionData = player.getPotionData()
    if (potionData.isNotEmpty()) {
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
