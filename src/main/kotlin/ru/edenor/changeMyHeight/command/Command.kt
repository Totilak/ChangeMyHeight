package ru.edenor.changeMyHeight.command

import com.destroystokyo.paper.profile.PlayerProfile
import com.mojang.brigadier.context.CommandContext
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands.argument
import io.papermc.paper.command.brigadier.Commands.literal
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import io.papermc.paper.command.brigadier.argument.resolvers.PlayerProfileListResolver
import io.papermc.paper.util.Tick
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import ru.edenor.changeMyHeight.ChangeMyHeight
import ru.edenor.changeMyHeight.ChangeMyHeight.Companion.GIVE_PERMISSION
import ru.edenor.changeMyHeight.ChangeMyHeight.Companion.USE_PERMISSION
import ru.edenor.changeMyHeight.ChangeMyHeightService.getPotionData
import ru.edenor.changeMyHeight.command.CommandExtensions.requiresAnyPermission
import ru.edenor.changeMyHeight.command.CommandExtensions.requiresPermission
import ru.edenor.changeMyHeight.command.CommandExtensions.simplyRun
import ru.edenor.changeMyHeight.data.Potion
import ru.edenor.changeMyHeight.data.Storage

class Command(private val plugin: ChangeMyHeight, private val storage: Storage) {
  fun commands() = arrayOf(cmh)

  private val giveSection =
      literal("give")
          .requiresPermission(GIVE_PERMISSION)
          .then(
              argument("potion", PotionArgumentType(storage))
                  .then(
                      argument("username", ArgumentTypes.playerProfiles()).simplyRun(::givePotion)))

  private val listSection = literal("list").requiresAnyPermission().simplyRun(::sendList)

  private val checkSection = literal("check").requiresAnyPermission().simplyRun(::checkPotionEffect)

  private val reloadSection =
      literal("reload").requiresPermission(GIVE_PERMISSION).simplyRun(::reload)

  private val cmh =
      literal("cmh")
          .requiresAnyPermission()
          .simplyRun(::sendHelp)
          .then(listSection)
          .then(giveSection)
          .then(checkSection)
          .then(reloadSection)
          .build()

  private fun sendHelp(sender: CommandSender) {
    sender.sendRichMessage(
        "<bold><aqua>ChangeMyHeight</aqua></bold> <gray>(${plugin.pluginMeta.version})</gray> - Плагин добавляющий зелья для изменения размера игрока.")

    if (sender.hasPermission(USE_PERMISSION)) {
      sender.sendRichMessage("<green>/cmh check <yellow>- Показать активные эффекты")
      sender.sendRichMessage("<green>/cmh list <yellow>- Показать доступные зелья")
    }

    if (sender.hasPermission(GIVE_PERMISSION)) {
      sender.sendRichMessage(
          "<green>/cmh give <potion_name> <username> <yellow> - Выдать зелье игроку")
      sender.sendRichMessage("<green>/cmh reload <yellow>- Перезагружает конфигурацию плагина")
    }
  }

  private fun sendList(sender: CommandSender) {
    PotionListMessenger.sendList(sender, storage)
  }

  private fun reload(sender: CommandSender) {
    plugin.reload()
    sender.sendRichMessage("<green>Настройки успешно перезагружены")
  }

  private fun givePotion(context: CommandContext<CommandSourceStack>) {
    val sender = context.source.sender

    val profiles: Collection<PlayerProfile> =
        context
            .getArgument("username", PlayerProfileListResolver::class.java)
            .resolve(context.source)

    val profile =
        profiles.firstOrNull()
            ?: run {
              sender.sendRichMessage("<red>Профиль не найден!</red>")
              return
            }

    val uuid = profile.id
    if (uuid == null) {
      sender.sendRichMessage("<red>У профиля ${profile.name} нет UUID!</red>")
      return
    }

    val player = plugin.server.getPlayer(uuid)
    if (player == null) {
      sender.sendRichMessage("<red>Игрок ${profile.name} не в сети!</red>")
      return
    }

    val potion: Potion = context.getArgument("potion", Potion::class.java)
    val itemPotion = potion.makePotion()

    val leftover = player.inventory.addItem(itemPotion)
    if (leftover.isNotEmpty()) {
      sender.sendRichMessage("<red>Инвентарь игрока заполнен!")
      return
    }

    sender.sendRichMessage("<green>Выдано зелье '${potion.title}' игроку ${player.name}")
  }

  private fun checkPotionEffect(sender: CommandSender) {
    if (sender !is Player) {
      sender.sendRichMessage("<red>Эта команда доступна только игрокам!</red>")
      return
    }

    val potionData = sender.getPotionData()
    if (potionData.isEmpty()) {
      sender.sendRichMessage("<gray>На вас сейчас <red>нет</red> активных эффектов!</gray>")
      return
    }

    potionData.forEach { (potionName, remaining) ->
      val potion = storage.getPotion(potionName) ?: return@forEach
      val potionColor = potion.color

      val timeLeftText = PotionListMessenger.pluralDuration(Tick.of(remaining))
      sender.sendRichMessage(
          "<green>На вас действует зелье <gray>[</gray>" +
              "<hover:show_text:'${potion.description}'><${potionColor}>${potion.title}</${potionColor}></hover>" +
              "<gray>]</gray> - осталось $timeLeftText")
    }
  }
}
