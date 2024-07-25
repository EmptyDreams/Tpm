package top.kmar.mc.tpm.data

import com.google.gson.JsonElement
import net.minecraft.ChatFormatting
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import top.kmar.mc.tpm.commands.TpmCommand.tpmTp
import top.kmar.mc.tpm.getLevelLocalName
import top.kmar.mc.tpm.save.TpmWorldData

data class DimensionalBlockPos(
    val level: ResourceLocation,
    val x: Double,
    val y: Double,
    val z: Double
) : TpmWorldData.NBTSerializable {

    constructor(level: ServerLevel, x: Double, y: Double, z: Double) : this(level.dimension().location(), x, y, z)

    fun serverLevel(server: MinecraftServer): ServerLevel {
        return server.allLevels.find { it.dimension().location() == level }!!
    }

    override fun saveTo(compoundTag: CompoundTag) {
        with (compoundTag) {
            putDouble("x", x)
            putDouble("y", y)
            putDouble("z", z)
            putString("dn", level.namespace)
            putString("dp", level.path)
        }
    }

    fun toComponent(): MutableComponent {
        return Component.translatable(getLevelLocalName(level)).withStyle(ChatFormatting.YELLOW)
            .append(String.format(" %.1f %.1f %.1f", x, y, z))
    }

    companion object {

        @JvmStatic
        val builder = { _: MinecraftServer, compoundTag: CompoundTag? ->
            if (compoundTag == null) null
            else buildFrom(compoundTag)
        }

        @JvmStatic
        val jsonParser = { server: MinecraftServer, json: JsonElement ->
            require(json.isJsonArray) { IllegalArgumentException("传入的参数应当为数组") }
            val array = json.asJsonArray
            require(array.size() == 4) { IllegalArgumentException("数组格式应当为：[level, x, y, z]") }
            val level = server.allLevels.find { it.dimension().location() == ResourceLocation(array[0].asString) }
                ?: throw IllegalArgumentException("传入的维度不存在：$array[0]")
            val x = array[1].asDouble
            val y = array[2].asDouble
            val z = array[3].asDouble
            val result = CompoundTag()
            DimensionalBlockPos(level, x, y, z).saveTo(result)
            result
        }

        @JvmStatic
        fun buildFrom(compoundTag: CompoundTag): DimensionalBlockPos {
            val x = compoundTag.getDouble("x")
            val y = compoundTag.getDouble("y")
            val z = compoundTag.getDouble("z")
            val dn = compoundTag.getString("dn")
            val dp = compoundTag.getString("dp")
            val location = ResourceLocation(dn, dp)
            return DimensionalBlockPos(location, x, y, z)
        }

        @JvmStatic
        fun ServerPlayer.teleportTo(pos: DimensionalBlockPos) {
            tpmTp(pos.x, pos.y, pos.z, level = pos.serverLevel(server))
        }

    }

}