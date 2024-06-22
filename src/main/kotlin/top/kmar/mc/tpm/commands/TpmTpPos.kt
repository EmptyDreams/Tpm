package top.kmar.mc.tpm.commands

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.DoubleArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.SuggestionProvider
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import net.minecraft.ChatFormatting
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.argument
import net.minecraft.commands.Commands.literal
import net.minecraft.commands.arguments.DimensionArgument
import net.minecraft.commands.arguments.coordinates.BlockPosArgument
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import top.kmar.mc.tpm.commands.TpmCommand.teleportTo
import top.kmar.mc.tpm.commands.config.DimensionalBlockPos.Companion.teleportTo
import top.kmar.mc.tpm.commands.config.MultiLevelBlockPos
import top.kmar.mc.tpm.localName
import top.kmar.mc.tpm.save.TpmWorldData
import java.util.concurrent.CompletableFuture

object TpmTpPos {

    @JvmStatic
    fun registry(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(
            literal("tpp")
                .executes { context ->
                    val player = context.source.player ?: return@executes 0
                    val level = player.serverLevel()
                    player.teleportToMain(level)
                    1
                }.then(TpmCommand.joinArguments(
                    argument("pos", BlockPosArgument.blockPos())
                ) { context ->
                    val player = context.source.player ?: return@joinArguments 0
                    val pos = BlockPosArgument.getBlockPos(context, "pos")
                    player.teleportTo(pos)
                    player.sendSystemMessage(TpmCommand.grayText("成功传送到 ${pos.x} ${pos.y} ${pos.z}"))
                    1
                }).then(
                    TpmCommand.joinArguments(
                        argument("level", DimensionArgument.dimension())
                            .suggests(WorldLevelSuggestionProvider)
                            .executes { context ->
                                val player = context.source.player ?: return@executes 0
                                val level = DimensionArgument.getDimension(context, "level")
                                player.teleportToMain(level)
                                1
                            },
                        argument("x", DoubleArgumentType.doubleArg()),
                        argument("y", DoubleArgumentType.doubleArg()),
                        argument("z", DoubleArgumentType.doubleArg())
                    ) { context ->
                        @Suppress("DuplicatedCode")
                        val player = context.source.player ?: return@joinArguments 0
                        val x = DoubleArgumentType.getDouble(context, "x")
                        val y = DoubleArgumentType.getDouble(context, "y")
                        val z = DoubleArgumentType.getDouble(context, "z")
                        val serverLevel = DimensionArgument.getDimension(context, "level")
                        if (serverLevel == null) {
                            player.sendSystemMessage(TpmCommand.errorText("输入的维度不存在"))
                        } else {
                            player.teleportTo(serverLevel, x, y, z, player.yRot, player.xRot)
                            player.sendSystemMessage(
                                Component.literal("成功传送到 ")
                                    .append(Component.translatable(serverLevel.localName))
                                    .append(" $x $y $z")
                                    .withStyle(ChatFormatting.GRAY)
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
            sendSystemMessage(TpmCommand.grayText("已将您传送到当前世界主城"))
        }
    }

}

object WorldLevelSuggestionProvider : SuggestionProvider<CommandSourceStack> {

    override fun getSuggestions(
        context: CommandContext<CommandSourceStack>,
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> {
        val server = context.source.server
        val player = context.source.playerOrException
        val input = context.input.lowercase().split(' ').filter { it.isNotEmpty() }
        val isDigit = input.size == 2 && input.last().all { it == '-' || it == '.' || it.isDigit() }
        if (input.size == 1) {
            builder.suggest(player.x.toString())
        }
        if (!isDigit) {
            for (level in server.allLevels) {
                val content = level.dimension().location().toString()
                if (content.contains(input.last())) {
                    builder.suggest(content)
                }
            }
        } else if (input.size == 1) {
            for (level in server.allLevels) {
                builder.suggest(level.dimension().location().toString())
            }
        }
        return builder.buildFuture()
    }

}