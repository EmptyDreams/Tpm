package top.kmar.mc.tpm.commands.config

import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import top.kmar.mc.tpm.data.PlayerBlockPos
import top.kmar.mc.tpm.save.TpmWorldData

data class MultiLevelBlockPos(
    private val dataCollection: MutableList<PlayerBlockPos> = ArrayList(3)
) : TpmWorldData.NBTSerializable, Iterable<PlayerBlockPos> {

    fun get(level: ServerLevel): PlayerBlockPos? {
        return get(level.dimension().location())
    }

    fun get(location: ResourceLocation): PlayerBlockPos? {
        return dataCollection.find { it.level == location }
    }

    fun put(pos: PlayerBlockPos) {
        val index = dataCollection.indexOfLast { it.level == pos.level }
        if (index == -1) {
            dataCollection += pos
        } else {
            dataCollection[index] = pos
        }
    }

    operator fun contains(level: ServerLevel): Boolean {
        return dataCollection.indexOfFirst { it.level == level.dimension().location() } != -1
    }

    override fun saveTo(compoundTag: CompoundTag) {
        for ((index, pos) in dataCollection.withIndex()) {
            val sonCompound = CompoundTag()
            pos.saveTo(sonCompound)
            compoundTag.put(index.toString(), sonCompound)
        }
    }

    override fun iterator(): Iterator<PlayerBlockPos> {
        return dataCollection.iterator()
    }

    companion object {

        @JvmStatic
        val builder = { _: MinecraftServer, compound: CompoundTag? ->
            if (compound == null) null
            else {
                val result = MultiLevelBlockPos()
                for (i in 0 until compound.size()) {
                    val sonCompound = compound.getCompound(i.toString())
                    val pos = PlayerBlockPos.buildFrom(sonCompound)
                    result.put(pos)
                }
                result
            }
        }

    }

}