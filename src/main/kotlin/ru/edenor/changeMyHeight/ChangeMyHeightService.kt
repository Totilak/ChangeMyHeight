package ru.edenor.changeMyHeight

import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import io.papermc.paper.util.Tick
import org.bukkit.attribute.AttributeInstance
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataContainer
import ru.edenor.changeMyHeight.ChangeMyHeight.Companion.potionKey
import ru.edenor.changeMyHeight.ChangeMyHeight.Companion.storage
import ru.edenor.changeMyHeight.command.PotionListMessenger
import ru.edenor.changeMyHeight.data.Potion
import ru.edenor.changeMyHeight.util.PotionData
import ru.edenor.changeMyHeight.util.PotionDataPersistentDataType.Companion.POTION_DATA_LIST
import java.util.*

object ChangeMyHeightService {

  val activeTasks: MutableMap<UUID, ScheduledTask> = mutableMapOf()

  fun applyPotion(player: Player, potion: Potion) {
    val remaining = potion.duration
    val remainingTicks = Tick.tick().fromDuration(remaining)

    player.addPotionData(PotionData(potion.name, remainingTicks.toLong()))

    applyAttribute(player, potion)

    PotionListMessenger.sendStartInfo(player, potion, remaining)

    startTask(player)
  }

  fun startTask(player: Player) {
    activeTasks.remove(player.uniqueId)?.cancel()
    activeTasks[player.uniqueId] =
        player.scheduler.runAtFixedRate(
            ChangeMyHeight.plugin,
            {
              val expired = player.decrementAndReturnExpiredPotionData()
              for (potionData in expired) {
                val potion = storage.getPotion(potionData.potionName)
                if (potion == null) {
                  player.removePotionData(potionData.potionName)
                  continue
                }
                onPotionExpiration(player, potion)
              }
            },
            { stopTaskAndSaveRemaining(player) },
            1L,
            1L) ?: return
  }

  private fun applyAttribute(player: Player, potion: Potion) {
    for (configAttribute in potion.attributes) {
      val attribute = configAttribute.bukkitAttribute
      val playerAttribute =
          player.getAttribute(attribute)
              ?: throw IllegalStateException("Player ${player.name} has no attribute '$attribute'")

      clearPluginAttributeModifiers(playerAttribute)

      val base = playerAttribute.baseValue
      val result = configAttribute.value - base
      val attributeModifier =
          AttributeModifier(potion.key, result, AttributeModifier.Operation.ADD_NUMBER)

      playerAttribute.addModifier(attributeModifier)
    }
  }

  private fun clearPluginAttributeModifiers(attribute: AttributeInstance) {
    attribute.modifiers
        .filter { mod -> mod.key.namespace == ChangeMyHeight.plugin.name.lowercase(Locale.ROOT) }
        .forEach(attribute::removeModifier)
  }

  fun stopTaskAndSaveRemaining(player: Player) {
    activeTasks.remove(player.uniqueId)?.cancel()
  }

  fun clearPotionEffects(player: Player) {
    removeModifiers(player)
    val pdc = player.persistentDataContainer
    clearPdc(pdc)
  }

  fun onPotionExpiration(player: Player, potion: Potion) {
    removeModifiers(player, potion)
    PotionListMessenger.sendEndedInfo(player,potion)
  }

  fun removeModifiers(player: Player, potion : Potion) {
    player.removePotionData(potion.name)
    potion.attributes.forEach {
      player.getAttribute(it.bukkitAttribute)?.removeModifier(potion.key)
    }
  }

  fun removeModifiers(player: Player) {
    player.getPotionData().forEach {
      val potion = storage.getPotion(it.potionName) ?: return@forEach
      removeModifiers(player,potion)
    }
  }

  fun clearPdc(pdc: PersistentDataContainer) {
    pdc.remove(potionKey)
  }

  fun effectOnDrink(player: Player, potion: Potion) {
    val particle = potion.particleType ?: return
    player.world.spawnParticle(particle, player.location.add(0.0, 1.0, 0.0), 40, 0.5, 1.0, 0.5)
  }

  fun sendPotionInfo(player: Player) {
    val potionData = player.getPotionData()
    potionData.forEach {
      val potion = storage.getPotion(it.potionName) ?: return@forEach
      PotionListMessenger.sendStartInfo(player, potion, Tick.of(it.remaining))
    }
  }

  fun Player.addPotionData(potionData: PotionData) {
    val potionDataList =
        persistentDataContainer.get(potionKey, POTION_DATA_LIST)?.toMutableList() ?: mutableListOf()
    val foundIx = potionDataList.indexOfFirst { it.potionName == potionData.potionName }
    if (foundIx != -1) {
      potionDataList[foundIx] = potionData
    } else {
      potionDataList.add(potionData)
    }
    persistentDataContainer.set(potionKey, POTION_DATA_LIST, potionDataList)
  }

  fun Player.removePotionData(potionName: String) {
    val potionDataList =
        persistentDataContainer.get(potionKey, POTION_DATA_LIST)?.toMutableList() ?: return
    potionDataList.removeIf { it.potionName == potionName }
    persistentDataContainer.set(potionKey, POTION_DATA_LIST, potionDataList)
  }

  fun Player.getPotionData(): List<PotionData> {
    return persistentDataContainer.get(potionKey, POTION_DATA_LIST) ?: emptyList()
  }

  fun Player.decrementAndReturnExpiredPotionData(): List<PotionData> {
    val potionDataList =
        persistentDataContainer.get(potionKey, POTION_DATA_LIST)?.toMutableList()
            ?: return emptyList()

    for (i in 0 until potionDataList.size) {
      val potionData = potionDataList[i]
      potionDataList[i] = PotionData(potionData.potionName, potionData.remaining - 1)
    }

    persistentDataContainer.set(potionKey, POTION_DATA_LIST, potionDataList)

    return potionDataList.filter { it.remaining <= 0L }
  }
}
