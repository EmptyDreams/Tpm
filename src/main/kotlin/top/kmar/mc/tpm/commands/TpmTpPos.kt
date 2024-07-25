package top.kmar.mc.tpm.commands

import com.mojang.brigadier.CommandDispatcher
import net.minecraft.ChatFormatting
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.argument
import net.minecraft.commands.Commands.literal
import net.minecraft.commands.arguments.DimensionArgument
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import top.kmar.mc.tpm.arrayMap
import top.kmar.mc.tpm.commands.TpmCommand.tpmTp
import top.kmar.mc.tpm.commands.config.DimensionalBlockPos.Companion.teleportTo
import top.kmar.mc.tpm.commands.config.DoublePosSuggestionProvider
import top.kmar.mc.tpm.commands.config.MultiLevelBlockPos
import top.kmar.mc.tpm.commands.config.SuggestionProvidersConnector
import top.kmar.mc.tpm.commands.config.WorldSuggestionProvider
import top.kmar.mc.tpm.data.DoubleBlockPos
import top.kmar.mc.tpm.localName
import top.kmar.mc.tpm.save.TpmWorldData

object TpmTpPos {

    @JvmStatic
    fun registry(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(
            literal("tpp").executes { context ->
                val player = context.source.playerOrException
                val level = player.serverLevel()
                player.teleportToMain(level)
                1
            }.then(
                TpmCommand.joinArguments(
                    *(TpmCommand.worldPosArgument.arrayMap { it, i ->
                        if (i == 0) it.suggests(SuggestionProvidersConnector(DoublePosSuggestionProvider, WorldSuggestionProvider))
                        else it.suggests(DoublePosSuggestionProvider)
                    })
                ) { context ->
                    val player = context.source.playerOrException
                    val (x, y, z) = DoubleBlockPos.readFromContext(context)
                    player.tpmTp(x, y, z)
                    player.sendSystemMessage(TpmCommand.grayText("成功传送到 $x $y $z"))
                    1
                }
            ).then(
                TpmCommand.joinArguments(
                    argument("level", DimensionArgument.dimension())
                        .executes { context ->
                            val player = context.source.playerOrException
                            val level = DimensionArgument.getDimension(context, "level")
                            player.teleportToMain(level)
                            1
                        }, *(TpmCommand.worldPosArgument.arrayMap { it, _ -> it.suggests(DoublePosSuggestionProvider) })
                ) { context ->
                    val player = context.source.playerOrException
                    val (x, y, z) = DoubleBlockPos.readFromContext(context)
                    val serverLevel = DimensionArgument.getDimension(context, "level")
                    if (serverLevel == null) {
                        player.sendSystemMessage(TpmCommand.errorText("输入的维度不存在"))
                    } else {
                        player.tpmTp(x, y, z, level = serverLevel)
                        player.sendSystemMessage(
                            Component.literal("成功传送到 ").append(Component.translatable(serverLevel.localName))
                                .append(" $x $y $z").withStyle(ChatFormatting.GRAY)
                        )
                    }
                    1
                }
            )
        )
    }

    @JvmStatic
    private fun ServerPlayer.teleportToMain(level: ServerLevel) {
        val posList = TpmWorldData.get("main", MultiLevelBlockPos.builder)
        val pos = posList?.get(level)
        if (pos == null) {
            val sharedPos = level.sharedSpawnPos
            if (sharedPos.y <= level.minBuildHeight) {
                sendSystemMessage(TpmCommand.grayText("暂无可用主城"))
                return
            }
            tpmTp(level, sharedPos)
            sendSystemMessage(TpmCommand.grayText("已将您传送到世界出生点"))
        } else {
            teleportTo(pos)
            sendSystemMessage(TpmCommand.grayText("已将您传送到世界主城"))
        }
    }

}