package top.kmar.mc.tpm.data

import net.minecraft.ChatFormatting
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import top.kmar.mc.tpm.commands.TpmCommand.tpmTp
import top.kmar.mc.tpm.findLevel
import top.kmar.mc.tpm.getLevelLocalName
import top.kmar.mc.tpm.save.TpmWorldData

data class PlayerBlockPos(
    val level: ResourceLocation,
    val x: Double,
    val y: Double,
    val z: Double,
    val yRot: Float,
    val xRot: Float
) : TpmWorldData.NBTSerializable {

    constructor(player: ServerPlayer): this(
        player.level().dimension().location(),
        player.x, player.y + 0.5, player.z,
        player.yRot, player.xRot
    )

    fun serverLevel(server: MinecraftServer) = server.findLevel(level)!!

    fun toComponent(): MutableComponent {
        return Component.translatable(getLevelLocalName(level)).withStyle(ChatFormatting.YELLOW)
            .append(String.format(" %.1f %.1f %.1f", x, y, z))
    }

    override fun saveTo(compoundTag: CompoundTag) {
        with (compoundTag) {
            putDouble("x", x)
            putDouble("y", y)
            putDouble("z", z)
            putFloat("yr", yRot)
            putFloat("xr", xRot)
            putString("dn", level.namespace)
            putString("dp", level.path)
        }
    }

    companion object {

        @JvmStatic
        val builder = { _: MinecraftServer, compoundTag: CompoundTag? ->
            if (compoundTag == null) null
            else buildFrom(compoundTag)
        }

        @JvmStatic
        fun buildFrom(compoundTag: CompoundTag): PlayerBlockPos {
            val x = compoundTag.getDouble("x")
            val y = compoundTag.getDouble("y")
            val z = compoundTag.getDouble("z")
            val yRot = compoundTag.getFloat("yr")
            val xRot = compoundTag.getFloat("xr")
            val dn = compoundTag.getString("dn")
            val dp = compoundTag.getString("dp")
            val location = ResourceLocation(dn, dp)
            return PlayerBlockPos(location, x, y, z, yRot, xRot)
        }

        @JvmStatic
        fun ServerPlayer.teleportTo(pos: PlayerBlockPos) {
            tpmTp(pos.x, pos.y, pos.z, pos.yRot, pos.xRot, pos.serverLevel(server))
        }

    }

}