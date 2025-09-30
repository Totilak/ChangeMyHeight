package ru.edenor.changeMyHeight.data

import java.time.Duration
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Particle
import org.bukkit.configuration.Configuration
import org.bukkit.configuration.ConfigurationSection
import ru.edenor.changeMyHeight.ChangeMyHeight
import ru.edenor.changeMyHeight.ChangeMyHeight.Companion.POTION_SECTION

class ConfigStorage(private var config: Configuration) : Storage {

  init {
    reload()
  }

  override fun getPotions(): List<Potion> {
    val section = config.getConfigurationSection(POTION_SECTION) ?: return listOf()
    return section
        .getKeys(false)
        .map { k -> section.getConfigurationSection(k)!! }
        .map { s -> readTemplate(s) }
  }

  override fun getPotion(name: String): Potion? {
    val section = config.getConfigurationSection(POTION_SECTION) ?: return null
    val template = section.getConfigurationSection(name) ?: return null
    return readTemplate(template)
  }

  override fun reload() {
    val plugin = ChangeMyHeight.plugin
    plugin.saveDefaultConfig()
    plugin.reloadConfig()
    config = plugin.config
  }

  private fun readTemplate(section: ConfigurationSection): Potion {
    return Potion(
        name = section.name,
        title = section.getString("title") ?: "No name",
        scale = section.getDouble("scale"),
        color = TextColor.fromHexString(section.getString("color") ?: "#bfff00")!!,
        duration = Duration.ofSeconds(section.getLong("duration")),
        description = section.getString("description") ?: "No description",
        particleType = section.getString("particleType")?.uppercase()?.let(Particle::valueOf))
  }
}
