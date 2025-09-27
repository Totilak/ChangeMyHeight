package ru.edenor.changeMyHeight.data

import net.kyori.adventure.text.format.TextColor
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import ru.edenor.changeMyHeight.ChangeMyHeight
import ru.edenor.changeMyHeight.ChangeMyHeight.Companion.POTION_FILENAME
import ru.edenor.changeMyHeight.ChangeMyHeight.Companion.POTION_SECTION
import java.nio.file.Files
import java.time.Duration

class ConfigStorage(private val plugin: ChangeMyHeight) : Storage {

  private var templatesFile = plugin.dataFolder.resolve(POTION_FILENAME)
  private lateinit var templatesConfig: YamlConfiguration

  init {
    if (!templatesFile.exists()) {
      templatesFile.parentFile?.mkdirs()
      ConfigStorage::class.java.getResourceAsStream("/config.yml").use { s ->
        Files.copy(s!!, templatesFile.toPath())
      }
    }
    reload()
  }

  override fun getPotions(): List<Potion> {
    val section = templatesConfig.getConfigurationSection(POTION_SECTION) ?: return listOf()
    return section
        .getKeys(false)
        .map { k -> section.getConfigurationSection(k)!! }
        .map { s -> readTemplate(s) }
  }

  override fun getPotion(name: String): Potion? {
    val section = templatesConfig.getConfigurationSection(POTION_SECTION) ?: return null
    val template = section.getConfigurationSection(name) ?: return null
    return readTemplate(template)
  }

  override fun reload() {
    templatesConfig = YamlConfiguration.loadConfiguration(templatesFile)
  }

  private fun readTemplate(section: ConfigurationSection): Potion {
    return Potion(
        name = section.name,
        title = section.getString("title") ?: "No name",
        scale = section.getDouble("scale"),
        color = TextColor.fromHexString(section.getString("color") ?: "#bfff00")!!,
        duration = Duration.ofSeconds(section.getLong("duration")),
        description = section.getString("description") ?: "No description")
  }
}
