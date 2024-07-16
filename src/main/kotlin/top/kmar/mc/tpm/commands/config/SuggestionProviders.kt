package top.kmar.mc.tpm.commands.config

import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.SuggestionProvider
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import net.minecraft.commands.CommandSourceStack
import net.minecraft.server.level.ServerPlayer
import top.kmar.mc.tpm.containsWithoutUnderline
import top.kmar.mc.tpm.formatToString
import top.kmar.mc.tpm.save.tpmData
import java.util.concurrent.CompletableFuture

/** 按顺序连接 provider */
class SuggestionProvidersConnector(
    private val first: SuggestionProvider<CommandSourceStack>,
    private val second: SuggestionProvider<CommandSourceStack>
) : SuggestionProvider<CommandSourceStack> {

    override fun getSuggestions(
        context: CommandContext<CommandSourceStack>?,
        builder: SuggestionsBuilder?
    ): CompletableFuture<Suggestions> {
        return first.getSuggestions(context, builder)
            .thenCompose { second.getSuggestions(context, builder) }
    }
}

/** 为浮点坐标提供补全 */
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

/** 为世界提供补全 */
object WorldSuggestionProvider : SuggestionProvider<CommandSourceStack> {

    override fun getSuggestions(
        context: CommandContext<CommandSourceStack>, builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> {
        val levels = context.source.server.allLevels
        val input = context.input.substringAfterLast(' ', "").lowercase().split(":")
        val lazyList = ArrayList<String>(2)
        if (input.size != 2) {  // 没有输入冒号
            for (level in levels) {
                val location = level.dimension().location()
                val contain = location.path.containsWithoutUnderline(input[0])
                if (contain == 1) builder.suggest(location.toString())
                else if (contain == -1) lazyList += location.toString()
                else if (location.namespace.containsWithoutUnderline(input[0]) != 0) lazyList += location.toString()
            }
        } else {                // 输入了冒号
            for (level in levels) {
                val location = level.dimension().location()
                if (location.namespace != input[0]) continue
                val contain = location.path.containsWithoutUnderline(input[0])
                if (contain == 1) builder.suggest(location.toString())
                else if (contain == -1) lazyList += location.toString()
            }
        }
        for (value in lazyList) {
            builder.suggest(value)
        }
        return builder.buildFuture()
    }

}

/** 支持过滤的玩家自动补全 */
open class PlayerFilterSuggestionProvider(
    val filter: (sourcePlayer: ServerPlayer, otherPlayer: ServerPlayer) -> Boolean,
) : SuggestionProvider<CommandSourceStack> {

    override fun getSuggestions(
        context: CommandContext<CommandSourceStack>, builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> {
        val sourcePlayer = context.source.playerOrException
        val playerList = context.source.server.playerList.players
        val input = context.input.substringAfterLast(' ', "").lowercase()
        val lazyList = ArrayList<String>()
        for (player in playerList) {
            if (!filter(sourcePlayer, player)) continue
            val playerName = player.gameProfile.name
            val contain = playerName.lowercase().containsWithoutUnderline(input)
            if (contain == 1) builder.suggest(playerName)
            else if (contain == -1) lazyList += playerName
        }
        for (value in lazyList) {
            builder.suggest(value)
        }
        return builder.buildFuture()
    }
}

/** 排除开启自动拒绝的玩家的自动补全 */
object PlayerWithoutRejectSuggestionProvider :
    PlayerFilterSuggestionProvider({ sourcePlayer, otherPlayer -> sourcePlayer.uuid != otherPlayer.uuid && !otherPlayer.tpmData.autoReject })

/** 排除自身的玩家自动补全 */
object PlayerWithoutSelfSuggestionProvider : PlayerFilterSuggestionProvider({ sourcePlayer, otherPlayer -> sourcePlayer.uuid != otherPlayer.uuid })

/** 玩家自动补全 */
object PlayerSuggestionProvider : PlayerFilterSuggestionProvider({ _, _ -> true })