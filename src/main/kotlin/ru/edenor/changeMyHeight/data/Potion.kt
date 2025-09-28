package ru.edenor.changeMyHeight.data

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.persistence.PersistentDataType
import ru.edenor.changeMyHeight.ChangeMyHeight
import ru.edenor.changeMyHeight.ChangeMyHeight.Companion.potionNameKey
import java.time.Duration

data class Potion(
    val name: String,
    val title: String,
    val scale: Double,
    val color: TextColor,
    val duration: Duration,
    val description: String
) {
  val key: NamespacedKey
    get() = NamespacedKey(ChangeMyHeight.plugin, name)

  fun makePotion(): ItemStack {
    val item = ItemStack(Material.POTION)
    item.editMeta(PotionMeta::class.java) { meta ->
      meta.displayName(Component.text(title, color)
        .decoration(TextDecoration.ITALIC, false))

      val bukkitColor = Color.fromRGB(color.red(), color.green(), color.blue())
      meta.color = bukkitColor
      meta.setEnchantmentGlintOverride(true)
      meta.persistentDataContainer.set(potionNameKey, PersistentDataType.STRING, name)
    }

    return item
  }
}
