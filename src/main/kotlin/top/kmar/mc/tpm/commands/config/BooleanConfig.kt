package top.kmar.mc.tpm.commands.config

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