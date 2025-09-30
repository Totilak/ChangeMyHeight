package ru.edenor.changeMyHeight.util

import com.google.common.base.Function
import com.google.common.collect.Lists
import org.bukkit.persistence.ListPersistentDataType
import org.bukkit.persistence.PersistentDataAdapterContext
import org.bukkit.persistence.PersistentDataType

data class PotionData(val potionName: String, val remaining: Long)

class PotionDataPersistentDataType : PersistentDataType<String, PotionData> {
  override fun getPrimitiveType(): Class<String> = String::class.java

  override fun getComplexType(): Class<PotionData> = PotionData::class.java

  override fun toPrimitive(complex: PotionData, context: PersistentDataAdapterContext): String =
      complex.potionName + "|" + complex.remaining

  override fun fromPrimitive(primitive: String, context: PersistentDataAdapterContext) =
      primitive.split("|").let { PotionData(it[0], it[1].toLong()) }

  companion object {
    val POTION_DATA = PotionDataPersistentDataType()
    val POTION_DATA_LIST = PotionDataListPersistentDataType()
  }
}

@Suppress("UNCHECKED_CAST")
class PotionDataListPersistentDataType : ListPersistentDataType<String, PotionData> {
  private val innerType: PersistentDataType<String, PotionData> =
      PotionDataPersistentDataType.POTION_DATA

  override fun getPrimitiveType(): Class<List<String>> =
      List::class.java as Any as Class<List<String>>

  override fun getComplexType(): Class<List<PotionData>> =
      List::class.java as Any as Class<List<PotionData>>

  override fun toPrimitive(
      complex: MutableList<PotionData>,
      context: PersistentDataAdapterContext
  ): MutableList<String> =
      Lists.transform<PotionData, String>(
          complex, Function { s -> innerType.toPrimitive(s, context) })

  override fun fromPrimitive(
      primitive: MutableList<String>,
      context: PersistentDataAdapterContext
  ): MutableList<PotionData> =
      Lists.transform<String, PotionData>(
          primitive, Function { s -> innerType.fromPrimitive(s, context) })

  override fun elementType(): PersistentDataType<String, PotionData> = this.innerType
}
