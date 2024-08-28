package top.kmar.mc.tpm.save

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.saveddata.SavedData
import net.minecraft.world.level.storage.DimensionDataStorage
import java.util.function.Function
import java.util.function.Supplier

object TpmWorldData : SavedData() {

    private lateinit var server: MinecraftServer
    private lateinit var worldData: DimensionDataStorage
    private lateinit var compoundTag: CompoundTag
    private val dataMap = Object2ObjectOpenHashMap<String, NBTSerializable>()

    fun initWorld() {
        ServerLifecycleEvents.SERVER_STARTED.register { server ->
            this.server = server
            worldData = server.overworld().dataStorage
            try {
                worldData.computeIfAbsent(this::readFrom, { readFrom(null) }, "tpm-data")
            } catch (_: NoSuchMethodError) {
                val worldDataClass = worldData::class
                val typeClass = SavedData::class.nestedClasses.let {
                    require(it.size == 1) {
                        """
                            TPM 不支持当前游戏版本。
                            this: ${this.javaClass}
                            worldDataClass: $worldDataClass
                            allClass: ${it.joinToString(" | ")}
                        """.trimIndent()
                    }
                    it.first()
                }
                val constructor = typeClass.constructors.let {
                    require(it.size == 1) {
                        """
                            TPM 不支持当前游戏版本。
                            this: ${this.javaClass}
                            worldDataClass: $worldDataClass
                            typeClass: $typeClass
                            constructors: ${it.joinToString(" | ")}
                        """.trimIndent()
                    }
                    it.first()
                }
                val typeObj = constructor.call(
                    Supplier { readFrom(null) },
                    Function<CompoundTag, TpmWorldData> { readFrom(it) },
                    null
                )
                val methodList = worldDataClass.java.methods.asSequence()
                    .filter { it.parameterCount == 2 }
                    .filter { it.parameterTypes[1] === String::class.java && it.parameterTypes[0] === typeClass.java }
                    .toList()
                require(methodList.isNotEmpty()) {
                    """
                        TPM 不支持当前游戏版本。
                        this: ${this.javaClass}
                        worldDataClass: $worldDataClass
                        typeClass: $typeClass
                        allMethods: ${worldDataClass.java.methods.joinToString(" | ") { it.toString() }}
                    """.trimIndent()
                }
                try {
                    for (method in methodList) {
                        val result = method.invoke(worldData, typeObj, "tpm-data")
                        if (result != null) return@register
                    }
                    throw AssertionError("代码进入了不应当进入的分支")
                } catch (e: Throwable) {
                    throw RuntimeException(
                        """
                            TPM 不支持当前游戏版本。
                            this: ${this.javaClass}
                            worldDataClass: $worldDataClass
                            typeClass: $typeClass
                            getterList: ${methodList.joinToString(" | ") { it.toString() }}
                            allMethods: ${worldDataClass.java.methods.joinToString(" | ") { it.toString() }}
                        """.trimIndent(), e
                    )
                }
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

    private fun readFrom(compoundTag: CompoundTag?): TpmWorldData {
        this.compoundTag = compoundTag?.getCompound("tpm") ?: CompoundTag()
        return this
    }

    interface NBTSerializable {

        fun saveTo(compoundTag: CompoundTag)

    }

}