package top.kmar.mc.tpm.commands

import com.mojang.brigadier.CommandDispatcher
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.Commands.argument
import net.minecraft.commands.arguments.DimensionArgument
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.network.chat.Component
import top.kmar.mc.tpm.Tpm
import top.kmar.mc.tpm.commands.config.ConfigRegister
import top.kmar.mc.tpm.commands.config.ConfigRegister.ConfigValue
import top.kmar.mc.tpm.commands.config.MultiLevelBlockPos
import top.kmar.mc.tpm.commands.config.PlayerSuggestionProvider
import top.kmar.mc.tpm.commands.config.WorldSuggestionProvider
import top.kmar.mc.tpm.data.DoubleBlockPos
import top.kmar.mc.tpm.data.PlayerBlockPos
import top.kmar.mc.tpm.permissions
import top.kmar.mc.tpm.save.DefaultConfigData
import top.kmar.mc.tpm.save.TpmWorldData

object TpmTpManager {

    @JvmStatic
    private val configMap = ConfigRegister("tpm").apply {
        this["main"] = ConfigValue(
            commands = { it, _ ->
                it.executes { context ->
                    writer(context.source.player, context)
                }.then(TpmCommand.joinArguments(
                    argument("level", DimensionArgument.dimension()).suggests(WorldSuggestionProvider),
                    *TpmCommand.worldPosArgument
                ) { context -> writer(null, context) })
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
            },
            writer = { player, context ->
                val pos = if (player != null) {
                    player.sendSystemMessage(TpmCommand.grayText("已将世界主城设置到当前位置"))
                    PlayerBlockPos(player)
                } else {
                    Tpm.logger.info("已将世界主城设置到指定位置")
                    val level = DimensionArgument.getDimension(context, "level")
                    val (x, y, z) = DoubleBlockPos.readFromContext(context)
                    PlayerBlockPos(level.dimension().location(), x, y, z, 0f, 0f)
                }
                val posList = TpmWorldData.get("main", MultiLevelBlockPos.builder) ?: MultiLevelBlockPos()
                posList.put(pos)
                TpmWorldData["main"] = posList
                1
            }
        )
        this["person"] = ConfigValue(
            commands = { it, _ ->
                var root = argument("tpm_player", EntityArgument.player())
                    .suggests(PlayerSuggestionProvider)
                TpmTpConfig.configMap.forEach { (key, value) ->
                    root = root.then(value.commands(value, Commands.literal(key)) { context ->
                        val targetPlayer = EntityArgument.getPlayer(context, "tpm_player")
                        val sourcePlayer = context.source.player
                        if (sourcePlayer != null && sourcePlayer.permissions <= targetPlayer.permissions) {
                            sourcePlayer.sendSystemMessage(TpmCommand.errorText("您没有权限修改对方的配置"))
                            return@commands null
                        }
                        targetPlayer
                    })
                }
                it.then(root)
            },
            reader = null,
            writer = { _, _ -> 1 },
        )
    }

    @JvmStatic
    fun registry(dispatcher: CommandDispatcher<CommandSourceStack>) {
        configMap.registry(dispatcher) {
            it.requires { source -> source.hasPermission(3) }
                .then(
                    Commands.literal("reload")
                        .executes { context ->
                            val player = context.source.player
                            val server = context.source.server
                            DefaultConfigData.reloadConfig(server)
                            TpmWorldData.clearCache()
                            if (player == null) Tpm.logger.info("用户缺省配置已重新加载")
                            else player.sendSystemMessage(TpmCommand.grayText("用户缺省配置已重新加载"))
                            1
                        }
                )
        }
    }

}