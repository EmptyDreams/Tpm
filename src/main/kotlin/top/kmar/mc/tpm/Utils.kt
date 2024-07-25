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

fun getLevelLocalName(level: ResourceLocation) =
    levelLocalNameMap[level] ?: level.toString()

val ServerLevel.localName: String
    get() = getLevelLocalName(dimension().location())

val ServerPlayer.permissions: Int
    get() = server.getProfilePermissions(gameProfile)

fun Double.formatToString(): String {
    return decimalFormat.format(this)
}

inline fun <T, reified R> Array<T>.arrayMap(transform: (T, Int) -> R): Array<R> {
    @Suppress("UNCHECKED_CAST")
    val cpy = java.lang.reflect.Array.newInstance(R::class.java, size) as Array<R>
    for (index in this.indices) {
        cpy[index] = transform(this[index], index)
    }
    return cpy
}

fun String.containsWithoutUnderline(other: String): Int {
    if (contains(other)) return 1
    if (replace("_", "").contains(other)) return -1
    return 0
}