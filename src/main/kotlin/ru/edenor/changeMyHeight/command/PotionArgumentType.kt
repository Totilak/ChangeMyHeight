package ru.edenor.changeMyHeight.command

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.MessageComponentSerializer
import io.papermc.paper.command.brigadier.argument.CustomArgumentType
import net.kyori.adventure.text.Component
import ru.edenor.changeMyHeight.data.Storage
import java.util.concurrent.CompletableFuture

class PotionArgumentType(private val storage: Storage) : CustomArgumentType<String, String> {
  companion object {
    @JvmStatic
    val ERROR_BAD_SOURCE =
        SimpleCommandExceptionType(
            MessageComponentSerializer.message()
                .serialize(Component.text("The source needs to be a CommandSourceStack!")))

    @JvmStatic
    val ERROR_POTION_NOT_FOUND = DynamicCommandExceptionType {
      MessageComponentSerializer.message()
          .serialize(Component.text("$it не является доступным зельем!"))
    }
  }

  override fun parse(reader: StringReader): String = reader.readUnquotedString()

  override fun getNativeType(): ArgumentType<String> = StringArgumentType.word()

  override fun <S : Any> parse(reader: StringReader, source: S): String {
    if (source !is CommandSourceStack) {
      throw ERROR_BAD_SOURCE.create()
    }

    val filter = nativeType.parse(reader)
    if (filter == null) {
      throw ERROR_POTION_NOT_FOUND.create(filter)
    }

    val potion = storage.getPotion(filter) ?: throw ERROR_POTION_NOT_FOUND.create(filter)

    return filter
  }

  override fun <S : Any> listSuggestions(
      context: CommandContext<S>,
      builder: SuggestionsBuilder
  ): CompletableFuture<Suggestions> {
    storage.getPotions().forEach { builder.suggest(it.name) }

    return builder.buildFuture()
  }
}
