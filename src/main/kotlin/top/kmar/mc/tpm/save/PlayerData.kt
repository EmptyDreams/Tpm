package top.kmar.mc.tpm.save

import net.minecraft.nbt.CompoundTag
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import top.kmar.mc.tpm.commands.config.DimensionalBlockPos

var ServerPlayer.tpmHome: DimensionalBlockPos?
    get() = readOfflineData("home", DimensionalBlockPos.builder)
    set(value) {
        if (value == null) deleteOfflineData("home")
        else setOfflineData("home", value)
    }

fun ServerPlayer.setOfflineData(key: String, value: TpmWorldData.NBTSerializable) {
    val realKey = "p-$uuid-$key"
    TpmWorldData[realKey] = value
}

fun ServerPlayer.deleteOfflineData(key: String) {
    val realKey = "p-$uuid-$key"
    TpmWorldData.remove(realKey)
}

fun <T : TpmWorldData.NBTSerializable> ServerPlayer.readOfflineData(
    key: String, builder: (MinecraftServer, CompoundTag?) -> T?
): T? {
    val realKey = "p-$uuid-$key"
    return TpmWorldData.get(realKey, builder)
}