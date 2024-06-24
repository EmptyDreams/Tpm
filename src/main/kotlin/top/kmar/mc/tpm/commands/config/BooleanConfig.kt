package top.kmar.mc.tpm.commands.config

import com.google.gson.JsonElement
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.MinecraftServer
import top.kmar.mc.tpm.save.TpmWorldData

enum class BooleanConfig(
    val value: Boolean
) : TpmWorldData.NBTSerializable {

    TRUE(true),
    FALSE(false);

    override fun saveTo(compoundTag: CompoundTag) {
        compoundTag.putBoolean("v", value)
    }

    companion object {

        @JvmStatic
        val builder = { _: MinecraftServer, compoundTag: CompoundTag? ->
            if (compoundTag == null) FALSE
            else from(compoundTag)
        }

        @JvmStatic
        val jsonParser = { _: MinecraftServer, json: JsonElement ->
            require(json.isJsonPrimitive && json.asJsonPrimitive.isBoolean) { IllegalArgumentException("应当传入一个布尔值") }
            val result = CompoundTag()
            BooleanConfig.from(json.asBoolean).saveTo(result)
            result
        }

        @JvmStatic
        fun from(compoundTag: CompoundTag): BooleanConfig {
            val value = compoundTag.getBoolean("v")
            return if (value) TRUE else FALSE
        }

        @JvmStatic
        fun from(value: Boolean): BooleanConfig {
            return if (value) TRUE else FALSE
        }

    }

}