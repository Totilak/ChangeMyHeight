package ru.edenor.changeMyHeight.data

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.persistence.PersistentDataType
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
  fun makePotion(): ItemStack {
    val item = ItemStack(Material.POTION)
    val meta = item.itemMeta as PotionMeta
    meta.displayName(Component.text(title))
    val bukkitColor = Color.fromRGB(color.red(), color.green(), color.blue())
    meta.color = bukkitColor
    Enchantment.getByKey(NamespacedKey.minecraft("luck"))?.let { meta.addEnchant(it, 1, true) }
    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS)
    meta.persistentDataContainer.set(potionNameKey, PersistentDataType.STRING, name)
    item.itemMeta = meta
    return item
  }
}
