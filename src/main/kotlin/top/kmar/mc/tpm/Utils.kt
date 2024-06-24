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