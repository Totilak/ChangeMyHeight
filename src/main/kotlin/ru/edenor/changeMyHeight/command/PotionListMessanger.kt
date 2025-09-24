package ru.edenor.changeMyHeight.command

import org.bukkit.command.CommandSender
import ru.edenor.changeMyHeight.data.Storage

object PotionListMessanger {
  fun sendlist(sender: CommandSender, storage: Storage) {
    val potions = storage.getPotions().sortedBy { it.name }

    if (potions.isEmpty()) {
      sender.sendRichMessage("<red>Список пуст!")
      return
    }

    sender.sendRichMessage("<green>Доступные зелья:</green>")
    potions.forEach { (_, title, scale, _, durationSecond) ->
      var scaleText = "Не действует"
      val durationM = durationSecond / 60

      if (scale == 1.0) {
        scaleText = "Возвращает нормальный"
      }

      if (scale < 1.0) {
        scaleText = "Уменьшает"
      }

      if (scale > 1.0) {
        scaleText = "Увеличивает"
      }

      sender.sendRichMessage(clickableTemplate(title, scaleText, durationM))
    }
  }

  private fun clickableTemplate(title: String, scale: String, duration: Long): String =
      "<green><hover:show_text:'Длительность: ${pluralMinutes(duration)}'>" +
          "<green>${title}</green> <yellow>- $scale размер игрока"

  fun pluralMinutes(minutes: Long): String {
    val lastDigit = (minutes % 10).toInt()
    val lastTwoDigits = (minutes % 100).toInt()

    val word =
        when {
          lastTwoDigits in 11..14 -> "минут"
          lastDigit == 1 -> "минута"
          lastDigit in 2..4 -> "минуты"
          else -> "минут"
        }

    return "$minutes $word"
  }
}
