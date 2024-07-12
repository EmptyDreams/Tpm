package top.kmar.mc.tpm

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer

private val levelLocalNameMap = Object2ObjectArrayMap<ResourceLocation, String>(3).apply {
    put(ResourceLocation("minecraft", "overworld"), "flat_world_preset.minecraft.overworld")
    put(ResourceLocation("minecraft", "the_end"), "advancements.end.root.title")
    put(ResourceLocation("minecraft", "the_nether"), "advancements.nether.root.title")
}

fun getLevelLocalName(level: ResourceLocation) =
    levelLocalNameMap[level] ?: level.toString()

val ServerLevel.localName: String
    get() = getLevelLocalName(dimension().location())

val ServerPlayer.permissions: Int
    get() = server.getProfilePermissions(gameProfile)

fun Double.formatToString(decimalPlaces: Int = 2): String {
    return "%.${decimalPlaces}f".format(this)
}

inline fun <T, reified R> Array<T>.arrayMap(transform: (T) -> R): Array<R> {
    @Suppress("UNCHECKED_CAST")
    val cpy = java.lang.reflect.Array.newInstance(R::class.java, size) as Array<R>
    for (index in this.indices) {
        cpy[index] = transform(this[index])
    }
    return cpy
}