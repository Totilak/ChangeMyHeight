package ru.edenor.changeMyHeight.command

import com.destroystokyo.paper.profile.PlayerProfile
import com.mojang.brigadier.context.CommandContext
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands.argument
import io.papermc.paper.command.brigadier.Commands.literal
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import io.papermc.paper.command.brigadier.argument.resolvers.PlayerProfileListResolver
import net.kyori.adventure.text.Component
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.command.CommandSender
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.persistence.PersistentDataType
import ru.edenor.changeMyHeight.ChangeMyHeight
import ru.edenor.changeMyHeight.ChangeMyHeight.Companion.GIVE_PERMISSION
import ru.edenor.changeMyHeight.ChangeMyHeight.Companion.USE_PERMISSION
import ru.edenor.changeMyHeight.ChangeMyHeight.Companion.remainingKey
import ru.edenor.changeMyHeight.ChangeMyHeight.Companion.sizeKey
import ru.edenor.changeMyHeight.command.CommandExtensions.requiresAnyPermission
import ru.edenor.changeMyHeight.command.CommandExtensions.requiresPermission
import ru.edenor.changeMyHeight.command.CommandExtensions.simplyRun
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
      sender.sendRichMessage("<green>/cmh give <potion_name> <username> <yellow> - Выдать зелье игроку")
      sender.sendRichMessage("<green>/cmh reload <yellow>- Перезагружает конфигурацию плагина")
    }
  }

  private fun sendList(sender: CommandSender) {
    PotionListMessanger.sendlist(sender, storage)
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

    val potionType: String = context.getArgument("potion", String::class.java)

    val potion =
        when (potionType.lowercase()) {
          "shrink" -> makePotion("Зелье уменьшения", Color.LIME, 0.5)
          "normal" -> makePotion("Зелье нормализации", Color.AQUA, 1.0)
          "giant" -> makePotion("Зелье гигантизма", Color.RED, 2.0)
          else -> {
            sender.sendRichMessage("<red>Неизвестный тип! Используй: shrink, normal, giant")
            return
          }
        }

    val leftover = player.inventory.addItem(potion)
    if (leftover.isNotEmpty()) {
      sender.sendRichMessage("<red>Инвентарь игрока заполнен!")
      return
    }

    sender.sendRichMessage("<green>Выдано '${potionType}' игроку ${player.name}")
  }

  private fun checkPotionEffect(sender: CommandSender) {
    if (sender !is Player) {
      sender.sendRichMessage("<red>Эта команда доступна только игрокам!</red>")
      return
    }

    val pdc = sender.persistentDataContainer
    val remaining = pdc.get(remainingKey, PersistentDataType.LONG)
    val scale = pdc.get(sizeKey, PersistentDataType.DOUBLE)
    val start = pdc.get(ChangeMyHeight.startKey, PersistentDataType.LONG)

    if (remaining == null || scale == null || start == null) {
      sender.sendRichMessage("<gray>На вас сейчас <red>нет</red> активных эффектов!</gray>")
      return
    }

    val elapsed = System.currentTimeMillis() - start
    val actualRemaining = (remaining - elapsed).coerceAtLeast(0)
    val seconds = actualRemaining / 1000

    val effectType =
        if (scale < 1.0) "<hover:show_text:'Уменьшает игрока'><aqua><u>Крошкинс</u></aqua></hover>"
        else if (scale > 1.0)
            "<hover:show_text:'Увеличивает игрока'><u><blue>Гигантизм</blue></u></hover>"
        else "<hover:show_text:'Нормализует размер игрока'><u><white>Нормализация</u></hover>"

    sender.sendRichMessage(
        "<green>Эффект:</green> $effectType <gray>(осталось $seconds сек.)</gray>")
  }

  private fun makePotion(name: String, color: Color, scale: Double): ItemStack {
    val item = ItemStack(Material.POTION)
    val meta = item.itemMeta as PotionMeta
    meta.displayName(Component.text(name))
    meta.color = color
    Enchantment.getByKey(NamespacedKey.minecraft("luck"))?.let { meta.addEnchant(it, 1, true) }
    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS)
    meta.persistentDataContainer.set(sizeKey, PersistentDataType.DOUBLE, scale)
    item.itemMeta = meta
    return item
  }
}
