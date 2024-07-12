package top.kmar.mc.tpm.commands

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.SuggestionProvider
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import net.minecraft.ChatFormatting
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.argument
import net.minecraft.commands.Commands.literal
import net.minecraft.commands.arguments.DimensionArgument
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import top.kmar.mc.tpm.arrayMap
import top.kmar.mc.tpm.commands.TpmCommand.teleportTo
import top.kmar.mc.tpm.commands.config.DimensionalBlockPos.Companion.teleportTo
import top.kmar.mc.tpm.commands.config.MultiLevelBlockPos
import top.kmar.mc.tpm.data.DoubleBlockPos
import top.kmar.mc.tpm.formatToString
import top.kmar.mc.tpm.localName
import top.kmar.mc.tpm.save.TpmWorldData
import java.util.concurrent.CompletableFuture

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
                    *(TpmCommand.worldPosArgument.arrayMap { it.suggests(DoublePosSuggestionProvider) })
                ) { context ->
                    val player = context.source.playerOrException
                    val (x, y, z) = DoubleBlockPos.readFromContext(context)
                    player.teleportTo(x, y, z)
                    player.sendSystemMessage(TpmCommand.grayText("成功传送到 $x $y $z"))
                    1
                }
            ).then(
                TpmCommand.joinArguments(
                    argument("level", DimensionArgument.dimension()).executes { context ->
                        val player = context.source.playerOrException
                        val level = DimensionArgument.getDimension(context, "level")
                        player.teleportToMain(level)
                        1
                    }, *(TpmCommand.worldPosArgument.arrayMap { it.suggests(DoublePosSuggestionProvider) })
                ) { context ->
                    val player = context.source.playerOrException
                    val (x, y, z) = DoubleBlockPos.readFromContext(context)
                    val serverLevel = DimensionArgument.getDimension(context, "level")
                    if (serverLevel == null) {
                        player.sendSystemMessage(TpmCommand.errorText("输入的维度不存在"))
                    } else {
                        player.teleportTo(serverLevel, x, y, z, player.yRot, player.xRot)
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
            teleportTo(level, sharedPos)
            sendSystemMessage(TpmCommand.grayText("已将您传送到世界出生点"))
        } else {
            teleportTo(pos)
            sendSystemMessage(TpmCommand.grayText("已将您传送到世界主城"))
        }
    }

}

object DoublePosSuggestionProvider : SuggestionProvider<CommandSourceStack> {

    override fun getSuggestions(
        context: CommandContext<CommandSourceStack>, builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> {
        val player = context.source.playerOrException
        val input = context.input.lowercase().split(" ")
        if (input.isEmpty() || input.last().isNotEmpty()) {
            return builder.buildFuture()
        }
        // 判断首位参数的是否是维度名称
        val isDimFirst = input[1].any { it != '-' && it != '.' && !it.isDigit() }
        if (isDimFirst && input.size == 1) return builder.buildFuture()
        // 偏移量，当第一位输入的是维度名称时向后偏移 1
        val offset = if (isDimFirst) 1 else 0
        when (input.size) {
            offset + 2 -> {
                builder.suggest(player.x.formatToString())
                builder.suggest("${player.x.formatToString()} ${player.y.formatToString()}")
                builder.suggest("${player.x.formatToString()} ${player.y.formatToString()} ${player.z.formatToString()}")
            }

            offset + 3 -> {
                builder.suggest(player.y.formatToString())
                builder.suggest("${player.y.formatToString()} ${player.z.formatToString()}")
            }

            offset + 4 -> {
                builder.suggest(player.z.formatToString())
            }
        }
        return builder.buildFuture()
    }

}