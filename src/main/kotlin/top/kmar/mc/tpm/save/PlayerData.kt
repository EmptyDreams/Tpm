package top.kmar.mc.tpm.save

import net.minecraft.nbt.CompoundTag
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import top.kmar.mc.tpm.commands.config.BooleanConfig
import top.kmar.mc.tpm.commands.config.DimensionalBlockPos

val ServerPlayer.tpmConfig: TpmPlayerConfigMap
    get() = TpmPlayerConfigMap(this)

fun ServerPlayer.setOfflineData(key: String, value: TpmWorldData.NBTSerializable?) {
    val realKey = "p-$uuid-$key"
    if (value == null) TpmWorldData.remove(realKey)
    else TpmWorldData[realKey] = value
}

fun <T : TpmWorldData.NBTSerializable> ServerPlayer.readOfflineData(
    key: String, builder: (MinecraftServer, CompoundTag?) -> T?
): T? {
    val realKey = "p-$uuid-$key"
    return TpmWorldData.get(realKey) { server, compoundTag ->
        builder(server, compoundTag ?: DefaultConfigData.readDefault(this, key))
    }
}

class TpmPlayerConfigMap(
    val player: ServerPlayer
) {

    /** 家的坐标 */
    var home: DimensionalBlockPos?
        get() = player.readOfflineData("home", DimensionalBlockPos.builder)
        set(value) = player.setOfflineData("home", value)

    /** 自动拒绝 */
    var autoReject: Boolean
        get() = player.readOfflineData("auto_reject", BooleanConfig.builder)?.value ?: false
        set(value) = player.setOfflineData("auto_reject", if (value) BooleanConfig.TRUE else null)

    /** 自动接受 */
    var autoAccept: Boolean
        get() = player.readOfflineData("auto_accept", BooleanConfig.builder)?.value ?: false
        set(value) = player.setOfflineData("auto_accept", if (value) BooleanConfig.TRUE else null)

}