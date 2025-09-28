package ru.edenor.changeMyHeight

import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import io.papermc.paper.util.Tick
import java.util.*
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType
import ru.edenor.changeMyHeight.ChangeMyHeight.Companion.potionNameKey
import ru.edenor.changeMyHeight.ChangeMyHeight.Companion.remainingKey
import ru.edenor.changeMyHeight.ChangeMyHeight.Companion.storage
import ru.edenor.changeMyHeight.command.PotionListMessenger
import ru.edenor.changeMyHeight.data.Potion

object ChangeMyHeightService {

  val activeTasks: MutableMap<UUID, ScheduledTask> = mutableMapOf()

  fun applyPotion(player: Player, potion: Potion) {
    val pdc = player.persistentDataContainer
    val remaining = potion.duration
    val remainingTicks = Tick.tick().fromDuration(remaining)

    pdc.set(remainingKey, PersistentDataType.LONG, remainingTicks.toLong())
    pdc.set(potionNameKey, PersistentDataType.STRING, potion.name)

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
              val pdc = player.persistentDataContainer
              val ticksLeft = pdc.getOrDefault(remainingKey, PersistentDataType.LONG, 0L)
              if (ticksLeft <= 0) {
                onPotionExpiration(player)
                return@runAtFixedRate
              }
              pdc.set(remainingKey, PersistentDataType.LONG, ticksLeft - 1)
            },
            { stopTaskAndSaveRemaining(player) },
            1L,
            1L) ?: return
  }

  private fun applyAttribute(player: Player, potion: Potion) {
    val playerAttributeScale =
        player.getAttribute(Attribute.SCALE)
            ?: throw IllegalStateException("Player ${player.name} has no attribute scale!")

    playerAttributeScale.modifiers
      .filter { it.key.namespace == ChangeMyHeight.plugin.name.lowercase(Locale.ROOT) }
      .forEach { playerAttributeScale.removeModifier(it) }

    val baseScale = playerAttributeScale.baseValue
    val resultScale = potion.scale - baseScale
    val attributeModifier =
        AttributeModifier(potion.key, resultScale, AttributeModifier.Operation.ADD_NUMBER)

    playerAttributeScale.addModifier(attributeModifier)
  }

  fun stopTaskAndSaveRemaining(player: Player) {
    activeTasks.remove(player.uniqueId)?.cancel()
  }

  fun onPotionExpiration(player: Player) {
    removeModifiers(player)
    val pdc = player.persistentDataContainer
    val potionName = pdc.get(potionNameKey, PersistentDataType.STRING) ?: return
    val potion = storage.getPotion(potionName) ?: return
    clearPdc(pdc)

    PotionListMessenger.sendEndedInfo(player, potion)
  }

  fun removeModifiers(player: Player) {
    val playerScale = player.getAttribute(Attribute.SCALE) ?: return
    playerScale.modifiers
        .filter { it.key.namespace == ChangeMyHeight.plugin.name.lowercase(Locale.ROOT) }
        .forEach(playerScale::removeModifier)
  }

  fun clearPdc(pdc: PersistentDataContainer) {
    pdc.remove(remainingKey)
    pdc.remove(potionNameKey)
  }

  fun sendPotionInfo(player: Player) {
    val pdc = player.persistentDataContainer
    val remaining = pdc.get(remainingKey, PersistentDataType.LONG) ?: return
    val potionName = pdc.get(potionNameKey, PersistentDataType.STRING) ?: return
    val potion = storage.getPotion(potionName) ?: return

    PotionListMessenger.sendStartInfo(player, potion, Tick.of(remaining))
  }
}
