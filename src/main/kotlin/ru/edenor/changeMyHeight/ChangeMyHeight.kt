package ru.edenor.changeMyHeight

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import org.bukkit.NamespacedKey
import org.bukkit.plugin.java.JavaPlugin
import ru.edenor.changeMyHeight.ChangeMyHeightService.activeTasks
import ru.edenor.changeMyHeight.command.Command
import ru.edenor.changeMyHeight.data.ConfigStorage
import ru.edenor.changeMyHeight.data.Storage
import ru.edenor.changeMyHeight.handler.PlayerHandler

class ChangeMyHeight : JavaPlugin() {

  override fun onEnable() {
    plugin = this
    storage = ConfigStorage(config)

    remainingKey = NamespacedKey(this, "size_remaining")
    potionNameKey = NamespacedKey(this, "size_potion_name")

    server.pluginManager.registerEvents(PlayerHandler(), this)

    lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { commands ->
      Command(this, storage).commands().forEach { commands.registrar().register(it) }
    }
  }

  override fun onDisable() {
    activeTasks.values.forEach(ScheduledTask::cancel)
    activeTasks.clear()
  }

  fun reload() {
    storage.reload()
  }

  companion object {
    const val GIVE_PERMISSION = "cmh.give"
    const val USE_PERMISSION = "cmh.use"
    const val POTION_SECTION = "potions"
    lateinit var remainingKey: NamespacedKey
    lateinit var potionNameKey: NamespacedKey
    lateinit var storage: Storage
    lateinit var plugin: ChangeMyHeight
  }
}
