package top.kmar.mc.tpm.commands

import com.mojang.brigadier.CommandDispatcher
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.arguments.DimensionArgument
import net.minecraft.network.chat.Component
import top.kmar.mc.tpm.commands.config.ConfigRegister
import top.kmar.mc.tpm.commands.config.ConfigRegister.ConfigValue
import top.kmar.mc.tpm.commands.config.DimensionalBlockPos
import top.kmar.mc.tpm.commands.config.MultiLevelBlockPos
import top.kmar.mc.tpm.data.DoubleBlockPos
import top.kmar.mc.tpm.save.TpmWorldData

object TpmTpManager {

    @JvmStatic
    private val configMap = ConfigRegister("tpm").apply {
        this["main"] = ConfigValue(
            commands = {
                it.executes { context ->
                    val player = context.source.playerOrException
                    val posList = TpmWorldData.get("main", MultiLevelBlockPos.builder) ?: MultiLevelBlockPos()
                    posList.put(DimensionalBlockPos(player.serverLevel(), player.x, player.y + 0.5, player.z))
                    TpmWorldData["main"] = posList
                    player.sendSystemMessage(TpmCommand.grayText("已将当前世界主城设置到当前坐标"))
                    1
                }.then(TpmCommand.joinArguments(
                    Commands.argument("level", DimensionArgument.dimension()),
                    *TpmCommand.worldPosArgument
                ) { context ->
                    val level = DimensionArgument.getDimension(context, "level")
                    val (x, y, z) = DoubleBlockPos.readFromContext(context)
                    val posList = TpmWorldData.get("main", MultiLevelBlockPos.builder) ?: MultiLevelBlockPos()
                    posList.put(DimensionalBlockPos(level, x, y, z))
                    TpmWorldData["main"] = posList
                    1
                })
            },
            reader = { _ ->
                val posList = TpmWorldData.get("main", MultiLevelBlockPos.builder)
                if (posList == null) null
                else {
                    var root = Component.literal("各世界中心传送点列表：")
                    for (pos in posList) {
                        root = root.append("\n    ").append(pos.toComponent())
                    }
                    root
                }
            }
        )
    }

    @JvmStatic
    fun registry(dispatcher: CommandDispatcher<CommandSourceStack>) {
        configMap.registry(dispatcher) {
            it.requires { source -> source.hasPermission(3) }
        }
    }

}