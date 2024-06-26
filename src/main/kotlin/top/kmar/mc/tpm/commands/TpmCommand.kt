package top.kmar.mc.tpm.commands

import com.mojang.brigadier.Command
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.DoubleArgumentType
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.suggestion.SuggestionProvider
import net.minecraft.ChatFormatting
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.MutableComponent
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer

object TpmCommand {

    @JvmStatic
    fun registry(dispatcher: CommandDispatcher<CommandSourceStack>) {
        TpmTpConfig.registry(dispatcher)
        TpmTpManager.registry(dispatcher)
        TpmTpPos.registry(dispatcher)
        TpmTpAsk.registry(dispatcher)
        TpmTpHome.registry(dispatcher)
        TpmTpSpawn.registry(dispatcher)
        TpmTpHelp.registry(dispatcher)
    }

    /** 构建一个可点击的按钮消息，点击事件为执行一条指令 */
    @JvmStatic
    internal fun clickableButton(text: String, command: String): Component {
        return Component.literal(text)
            .withStyle {
                it.withUnderlined(true)
                    .withBold(true)
                    .withColor(ChatFormatting.YELLOW)
                    .withClickEvent(ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                    .withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("或输入 $command")))
            }
    }

    /** 构建一个可点击的链接消息 */
    @JvmStatic
    internal fun clickableUrl(text: String, url: String): Component {
        return Component.literal(text)
            .withStyle {
                it.withUnderlined(true)
                    .withColor(ChatFormatting.BLUE)
                    .withClickEvent(ClickEvent(ClickEvent.Action.OPEN_URL, url))
                    .withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(url)))
            }
    }

    /** 构建一个错误消息 */
    @JvmStatic
    internal fun errorText(text: String): MutableComponent {
        return Component.literal(text)
            .withStyle(ChatFormatting.RED)
    }

    /** 构建一个灰色的消息 */
    @JvmStatic
    internal fun grayText(text: String): MutableComponent {
        return Component.literal(text)
            .withStyle(ChatFormatting.GRAY)
    }

    @JvmStatic
    internal fun yellowText(text: String): MutableComponent {
        return Component.literal(text)
            .withStyle(ChatFormatting.YELLOW)
    }

    @JvmStatic
    internal fun whiteText(text: String): MutableComponent {
        return Component.literal(text)
            .withStyle(ChatFormatting.WHITE)
    }

    @JvmStatic
    internal val worldPosArgument: Array<RequiredArgumentBuilder<CommandSourceStack, *>>
        get() = arrayOf(
            Commands.argument("x", DoubleArgumentType.doubleArg()),
            Commands.argument("y", DoubleArgumentType.doubleArg()),
            Commands.argument("z", DoubleArgumentType.doubleArg())
        )

    /** 构建一条指令 */
    @JvmStatic
    internal fun joinArguments(
        vararg argList: RequiredArgumentBuilder<CommandSourceStack, *>,
        suggestion: SuggestionProvider<CommandSourceStack>? = null,
        command: Command<CommandSourceStack>? = null,
    ): RequiredArgumentBuilder<CommandSourceStack, *> {
        var argument = argList.last()
        if (suggestion != null) {
            argument = argument.suggests(suggestion)
        }
        if (command != null)
            argument = argument.executes(command)
        for (i in argList.lastIndex - 1 downTo 0) {
            argument = argList[i].then(argument)
            if (suggestion != null)
                argument = argument.suggests(suggestion)
        }
        return argument
    }

    /** 将当前玩家传送到指定玩家 */
    @JvmStatic
    internal fun ServerPlayer.teleportTo(targetPlayer: ServerPlayer) {
        teleportTo(
            targetPlayer.serverLevel(),
            targetPlayer.x, targetPlayer.y, targetPlayer.z,
            targetPlayer.yRot, targetPlayer.xRot
        )
        sendSystemMessage(
            Component.literal("已将您传送到 ")
                .append(targetPlayer.name)
                .withStyle(ChatFormatting.GRAY)
        )
        targetPlayer.sendSystemMessage(
            Component.literal("已将 ")
                .append(name)
                .append(" 传送到您")
                .withStyle(ChatFormatting.GRAY)
        )
    }

    /** 将玩家传送到指定世界的指定坐标 */
    @JvmStatic
    internal fun ServerPlayer.teleportTo(level: ServerLevel, pos: BlockPos) {
        teleportTo(level, pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(), yRot, xRot)
    }

}