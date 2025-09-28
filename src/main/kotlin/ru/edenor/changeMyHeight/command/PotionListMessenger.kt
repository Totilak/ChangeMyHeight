package ru.edenor.changeMyHeight.command

import net.kyori.adventure.text.format.TextColor
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import ru.edenor.changeMyHeight.data.Potion
import ru.edenor.changeMyHeight.data.Storage
import java.time.Duration

object PotionListMessenger {

  fun sendList(sender: CommandSender, storage: Storage) {
    val potions = storage.getPotions().sortedBy { it.name }

    if (potions.isEmpty()) {
      sender.sendRichMessage("<red>Список пуст!")
      return
    }

    sender.sendRichMessage("<green>Доступные зелья:</green>")
    potions.forEach { (name, title, _, color, duration, description) ->
      sender.sendRichMessage(clickableTemplate(name, title, color, duration, description))
    }
  }

  private fun clickableTemplate(
      name: String,
      title: String,
      color: TextColor,
      duration: Duration,
      description: String
  ): String =
      "<green><hover:show_text:'Длительность: ${pluralDuration(duration)}'>" +
          "<$color>${title}</$color> ($name) <yellow>- $description"

  fun sendStartInfo(player: Player, potion: Potion, remaining: Duration) {
    player.playSound(player, Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.UI, 1f, 1.4f)
    player.sendRichMessage(
        "<green>На вас действует зелье <gray>[</gray>" +
            "<hover:show_text:'${potion.description}'><${potion.color}>${potion.title}</${potion.color}></hover>" +
            "<gray>]</gray> - осталось ${pluralDuration(remaining)}")
  }

  fun sendEndedInfo(player: Player, potion: Potion) {
    player.playSound(player, Sound.BLOCK_BEACON_DEACTIVATE, SoundCategory.UI, 1f, 1.4f)
    player.sendRichMessage(
        "<green>Зелье <gray>[</gray>" +
            "<hover:show_text:'${potion.description}'><${potion.color}>${potion.title}</${potion.color}></hover>" +
            "<gray>]</gray> закончилось, размер возвращён в норму!")
  }

  fun pluralDuration(duration: Duration): String {
    val totalSeconds = duration.seconds
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60

    val parts = mutableListOf<String>()

    if (minutes > 0) {
      parts +=
          when {
            minutes % 10 == 1L && minutes % 100 != 11L -> "$minutes минута"
            minutes % 10 in 2..4 && (minutes % 100 !in 12..14) -> "$minutes минуты"
            else -> "$minutes минут"
          }
    }

    if (seconds > 0) {
      parts +=
          when {
            seconds % 10 == 1L && seconds % 100 != 11L -> "$seconds секунда"
            seconds % 10 in 2..4 && (seconds % 100 !in 12..14) -> "$seconds секунды"
            else -> "$seconds секунд"
          }
    }

    return parts.joinToString(" ")
  }
}
