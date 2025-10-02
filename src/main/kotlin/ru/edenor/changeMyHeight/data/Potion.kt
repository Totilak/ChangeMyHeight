package ru.edenor.changeMyHeight.data

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.TooltipDisplay
import java.time.Duration
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Particle
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.persistence.PersistentDataType
import ru.edenor.changeMyHeight.ChangeMyHeight
import ru.edenor.changeMyHeight.ChangeMyHeight.Companion.potionKey
import ru.edenor.changeMyHeight.command.PotionListMessenger.pluralDuration

data class Potion(
    val name: String,
    val title: String,
    val attributes: List<ConfigAttribute>,
    val color: TextColor,
    val duration: Duration,
    val description: String,
    val particleType: Particle?
) {
  val key: NamespacedKey
    get() = NamespacedKey(ChangeMyHeight.plugin, name)

  @Suppress("UnstableApiUsage")
  fun makePotion(): ItemStack {
    val item = ItemStack(Material.POTION)
    item.editMeta(PotionMeta::class.java) { meta ->
      meta.displayName(Component.text(title, color).decoration(TextDecoration.ITALIC, false))

      val bukkitColor = Color.fromRGB(color.red(), color.green(), color.blue())
      meta.color = bukkitColor
      meta.setEnchantmentGlintOverride(true)
      meta.persistentDataContainer.set(potionKey, PersistentDataType.STRING, name)

      meta.lore(
          listOf(
              Component.text(description, NamedTextColor.GRAY)
                  .decoration(TextDecoration.ITALIC, false),
              Component.text("Время действия: ${pluralDuration(duration)}", NamedTextColor.GRAY)
                  .decoration(TextDecoration.ITALIC, false)))
    }
    item.setData(
        DataComponentTypes.TOOLTIP_DISPLAY,
        TooltipDisplay.tooltipDisplay()
            .addHiddenComponents(DataComponentTypes.POTION_CONTENTS)
            .build())

    return item
  }
}
