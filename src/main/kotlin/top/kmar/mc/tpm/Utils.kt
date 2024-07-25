package top.kmar.mc.tpm

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import java.text.DecimalFormat

private val levelLocalNameMap = Object2ObjectArrayMap<ResourceLocation, String>(3).apply {
    put(ResourceLocation("minecraft", "overworld"), "flat_world_preset.minecraft.overworld")
    put(ResourceLocation("minecraft", "the_end"), "advancements.end.root.title")
    put(ResourceLocation("minecraft", "the_nether"), "advancements.nether.root.title")
}

private val decimalFormat = DecimalFormat("#.##").apply {
    minimumFractionDigits = 0
    maximumFractionDigits = 2
}

/** 获取维度的 I18n 的 key */
fun getLevelLocalName(level: ResourceLocation) =
    levelLocalNameMap[level] ?: level.toString()

/** 获取世界维度的 I18n 的 key */
val ServerLevel.localName: String
    get() = getLevelLocalName(dimension().location())

/** 获取玩家的权限等级 */
val ServerPlayer.permissions: Int
    get() = server.getProfilePermissions(gameProfile)

/** 将浮点数转换为字符串，保留小数点后两位，不保留末尾的 0 */
fun Double.formatToString(): String {
    return decimalFormat.format(this)
}

/**
 * 数组的 map，功能同 [List.map]
 *
 * 警告：该函数使用了反射，存在性能问题，仅适用于对性能要求不高的场景
 */
inline fun <T, reified R> Array<T>.arrayMap(transform: (T, Int) -> R): Array<R> {
    @Suppress("UNCHECKED_CAST")
    val cpy = java.lang.reflect.Array.newInstance(R::class.java, size) as Array<R>
    for (index in this.indices) {
        cpy[index] = transform(this[index], index)
    }
    return cpy
}

/**
 * 判断一个字符串是否包含了另一个字符串，忽略当前字符串中的下划线（不会忽略 [other] 中的下划线）
 */
fun String.containsWithoutUnderline(other: String): Int {
    if (contains(other)) return 1
    if (replace("_", "").contains(other)) return -1
    return 0
}