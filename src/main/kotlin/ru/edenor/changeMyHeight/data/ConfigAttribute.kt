package ru.edenor.changeMyHeight.data

import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.attribute.Attribute

data class ConfigAttribute(
  val key : NamespacedKey,
  val value : Double
) {
  val bukkitAttribute: Attribute = Registry.ATTRIBUTE.getOrThrow(key)
}