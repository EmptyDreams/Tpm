package top.kmar.mc.tpm.save

import net.minecraft.nbt.CompoundTag
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import top.kmar.mc.tpm.commands.config.BooleanConfig
import top.kmar.mc.tpm.commands.config.DimensionalBlockPos

/**
 * 玩家的离线数据，该对象不会被缓存，每次调用将返回一个新的对象
 */
val ServerPlayer.tpmData: TpmPlayerDataMap
    get() = TpmPlayerDataMap(this)

/** 写入离线数据 */
fun ServerPlayer.setTpmOfflineData(key: String, value: TpmWorldData.NBTSerializable?) {
    val realKey = "p-$uuid-$key"
    if (value == null) TpmWorldData.remove(realKey)
    else TpmWorldData[realKey] = value
}

/** 读取离线数据 */
fun <T : TpmWorldData.NBTSerializable> ServerPlayer.readTpmOfflineData(
    key: String, builder: (MinecraftServer, CompoundTag?) -> T?
): T? {
    val realKey = "p-$uuid-$key"
    return TpmWorldData.get(realKey) { server, compoundTag ->
        builder(server, compoundTag ?: DefaultConfigData.readDefault(this, key))
    }
}

class TpmPlayerDataMap(
    val player: ServerPlayer
) {

    /** 家的坐标 */
    var home: DimensionalBlockPos?
        get() = player.readTpmOfflineData("home", DimensionalBlockPos.builder)
        set(value) = player.setTpmOfflineData("home", value)

    /** 自动拒绝 */
    var autoReject: Boolean
        get() = player.readTpmOfflineData("auto_reject", BooleanConfig.builder)?.value ?: false
        set(value) = player.setTpmOfflineData("auto_reject", BooleanConfig.from(value))

    /** 自动接受 */
    var autoAccept: Boolean
        get() = player.readTpmOfflineData("auto_accept", BooleanConfig.builder)?.value ?: false
        set(value) = player.setTpmOfflineData("auto_accept", BooleanConfig.from(value))

}