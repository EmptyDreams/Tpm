package top.kmar.mc.tpm.save

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.saveddata.SavedData
import net.minecraft.world.level.storage.DimensionDataStorage

object TpmWorldData : SavedData() {

    private lateinit var server: MinecraftServer
    private lateinit var worldData: DimensionDataStorage
    private lateinit var compoundTag: CompoundTag
    private val dataMap = Object2ObjectOpenHashMap<String, NBTSerializable>()

    fun initWorld() {
        ServerLifecycleEvents.SERVER_STARTED.register { server ->
            this.server = server
            worldData = server.overworld().dataStorage
            val data = worldData.get({
                readFrom(it)
                this
            }, "tpm-data")
            if (data == null) {
                compoundTag = CompoundTag()
                worldData.set("tpm-data", this)
            }
        }
    }

    operator fun set(key: String, data: NBTSerializable) {
        dataMap[key] = data
        val sonCompoundTag = CompoundTag()
        data.saveTo(sonCompoundTag)
        compoundTag.put(key, sonCompoundTag)
        setDirty()
    }

    fun <T : NBTSerializable> get(key: String, builder: (MinecraftServer, CompoundTag?) -> T?): T? {
        @Suppress("UNCHECKED_CAST")
        return dataMap.getOrPut(key) {
            val compound = if (compoundTag.contains(key)) compoundTag.getCompound(key) else null
            builder(server, compound)
        } as T?
    }

    fun clearCache() {
        dataMap.clear()
    }

    fun remove(key: String) {
        dataMap.remove(key)
        compoundTag.remove(key)
    }

    override fun save(compoundTag: CompoundTag): CompoundTag {
        compoundTag.put("tpm", this.compoundTag)
        return compoundTag
    }

    private fun readFrom(compoundTag: CompoundTag) {
        this.compoundTag = compoundTag.getCompound("tpm") ?: CompoundTag()
    }

    interface NBTSerializable {

        fun saveTo(compoundTag: CompoundTag)

    }

}