package top.kmar.mc.tpm.commands

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.context.CommandContext
import net.minecraft.ChatFormatting
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.argument
import net.minecraft.commands.Commands.literal
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.server.level.ServerPlayer
import top.kmar.mc.tpm.TpRequestManager
import top.kmar.mc.tpm.commands.TpmCommand.clickableButton
import top.kmar.mc.tpm.commands.TpmCommand.teleportTo
import top.kmar.mc.tpm.save.tpmData

object TpmTpAsk {

    fun registry(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(
            literal("tpa").then(
                TpmCommand.joinArguments(
                argument("player", EntityArgument.player())
                    .executes { context -> executeTpaCommand(context, false) },
                argument("force", BoolArgumentType.bool())
                    .executes { context ->
                        val force = BoolArgumentType.getBool(context, "force")
                        executeTpaCommand(context, force)
                    }
            ))
        )
        dispatcher.register(
            literal("tphere").then(
                TpmCommand.joinArguments(
                argument("player", EntityArgument.player())
                    .executes { context -> executeTphereCommand(context, false) },
                argument("force", BoolArgumentType.bool())
                    .executes { context ->
                        val force = BoolArgumentType.getBool(context, "force")
                        executeTphereCommand(context, force)
                    }
            ))
        )
        dispatcher.register(
            literal("tpaccept").executes { context ->
                val player = context.source.playerOrException
                val list = TpRequestManager.findReceiveByAndIsTarget(player)
                if (list.isEmpty()) {
                    player.sendSystemMessage(TpmCommand.errorText("没有可接受的传送请求"))
                } else {
                    TpRequestManager.acceptRequests(list)
                    player.sendSystemMessage(Component.literal("已接受所有传送请求"))
                }
                1
            }.then(
                argument("player", EntityArgument.player())
                    .executes { context ->
                        val player = context.source.playerOrException
                        val otherPlayer = EntityArgument.getPlayer(context, "player")
                        val request = TpRequestManager.find(otherPlayer, player)
                        if (request == null) {
                            player.sendSystemMessage(TpmCommand.errorText("没有可接受的传送请求"))
                        } else {
                            request.accept()
                        }
                        1
                    }
            )
        )
        dispatcher.register(
            literal("tpreject").executes { context ->
                val player = context.source.playerOrException
                val list = TpRequestManager.findReceiveBy(player)
                if (list.isEmpty()) {
                    player.sendSystemMessage(TpmCommand.errorText("没有可拒绝的传送请求"))
                } else {
                    TpRequestManager.rejectRequests(list)
                    player.sendSystemMessage(Component.literal("已拒绝所有传送请求"))
                }
                1
            }.then(
                argument("player", EntityArgument.player())
                    .executes { context ->
                        val player = context.source.playerOrException
                        val otherPlayer = EntityArgument.getPlayer(context, "player")
                        val request = TpRequestManager.find(otherPlayer, player)
                        if (request == null) {
                            player.sendSystemMessage(TpmCommand.errorText("没有可拒绝的传送请求"))
                        } else {
                            request.reject()
                            val callbackCommand = if (request.isTargetPlayer(player)) {
                                "/tphere ${request.sender.name.string}"
                            } else {
                                "/tpa ${request.sender.name.string}"
                            }
                            player.sendSystemMessage(
                                Component.literal("已拒绝 ")
                                    .append(request.sender.name)
                                    .append(" 的传送请求 ")
                                    .append(clickableButton("[回请]", callbackCommand))
                            )
                        }
                        1
                    }
            )
        )
        dispatcher.register(
            literal("tpcancel").executes { context ->
                val player = context.source.playerOrException

                val request = TpRequestManager.findSendBy(player)
                if (request == null) {
                    player.sendSystemMessage(TpmCommand.errorText("没有可取消的传送请求"))
                } else {
                    request.cancel()
                    val receiver = request.receiver
                    val repeatCommand = if (request.isSrcPlayer(player)) {
                        "/tpa ${receiver.name.string}"
                    } else {
                        "/tphere ${receiver.name.string}"
                    }
                    player.sendSystemMessage(
                        Component.literal("已取消向 ")
                            .append(receiver.name)
                            .append(" 发起的传送请求 ")
                            .append(clickableButton("[重发]", repeatCommand))
                    )
                }
                1
            }
        )
    }

    @Suppress("SameReturnValue", "DuplicatedCode")
    @JvmStatic
    private fun executeTphereCommand(context: CommandContext<CommandSourceStack>, force: Boolean): Int {
        val targetPlayer = context.source.playerOrException
        val player = EntityArgument.getPlayer(context, "player")
        if (player == targetPlayer) {
            player.sendSystemMessage(TpmCommand.errorText("传送自己听起来像是未来的科技，目前我们的魔法还不足以让你和自己玩捉迷藏。"))
            return 0
        }
        if (player.tpmData.autoReject) {
            targetPlayer.sendSystemMessage(TpmCommand.grayText("您的传送请求已被自动拒绝"))
            return 1
        }
        val success = TpRequestManager.appendRequest(
            TpRequestManager.TpRequest(
            sender = targetPlayer, srcPlayer = player, targetPlayer = targetPlayer,
            rejectEvent = {
                targetPlayer.sendSystemMessage(Component.empty().append(player.name).append(" 拒绝了您的传送请求"))
            }
        ), force)
        if (!success) {
            player.sendSystemMessage(
                TpmCommand.errorText("您存在未完成的传送请求 ")
                    .append(clickableButton("[取消并重试]", "/tphere ${player.name.string} true"))
            )
            return 1
        }
        targetPlayer.sendSystemMessage(
            Component.literal("正在请求将 ")
                .append(player.name)
                .append(" 传送到您 ")
                .append(clickableButton("[取消]", "/tpcancel"))
        )
        player.sendSystemMessage(
            genReceiverOpMenu(
                targetPlayer,
                Component.empty().append(targetPlayer.name).append(" 正在请求将您传送到他的位置"),
                false
            )
        )
        return 1
    }

    @Suppress("SameReturnValue", "DuplicatedCode")
    @JvmStatic
    private fun executeTpaCommand(context: CommandContext<CommandSourceStack>, force: Boolean): Int {
        val player = context.source.playerOrException
        val targetPlayer = EntityArgument.getPlayer(context, "player")
        val targetPlayerConfig = targetPlayer.tpmData
        if (player == targetPlayer) {
            player.sendSystemMessage(TpmCommand.errorText("传送自己听起来像是未来的科技，目前我们的魔法还不足以让你和自己玩捉迷藏。"))
            return 0
        }
        if (targetPlayerConfig.autoReject) {
            player.sendSystemMessage(TpmCommand.grayText("您的传送请求已被自动拒绝"))
            return 1
        }
        val autoAccept = targetPlayerConfig.autoAccept
        val success = TpRequestManager.appendRequest(TpRequestManager.TpRequest(
            sender = player, srcPlayer = player, targetPlayer = targetPlayer,
            rejectEvent = {
                player.sendSystemMessage(Component.empty().append(targetPlayer.name).append(" 拒绝了您的传送请求"))
            },
            cancelEvent = {
                targetPlayer.sendSystemMessage(
                    Component.empty().append(player.name)
                        .append(" 发起的传送已取消 ")
                        .withStyle {
                            it.withColor(ChatFormatting.GRAY)
                        }.append(clickableButton("[回请]", "/tphere ${player.name.string}"))
                )
            }
        ), force, autoAccept)
        if (!success) {
            player.sendSystemMessage(
                TpmCommand.errorText("您存在未完成的传送请求 ")
                    .append(clickableButton("[取消并重试]", "/tpa ${targetPlayer.name.string} true"))
            )
            return 1
        }
        if (autoAccept) {
            player.teleportTo(targetPlayer)
        } else {
            player.sendSystemMessage(
                Component.literal("正在请求传送到 ")
                    .append(targetPlayer.name)
                    .append(" ")
                    .append(clickableButton("[取消]", "/tpcancel"))
            )
            targetPlayer.sendSystemMessage(
                genReceiverOpMenu(player, Component.empty().append(player.name).append(" 正在请求传送到您 "), true)
            )
        }
        return 1
    }

    /**
     * @param allMenu 是否生成完整菜单
     */
    private fun genReceiverOpMenu(sender: ServerPlayer, component: MutableComponent, allMenu: Boolean): MutableComponent {
        val base = component.append(clickableButton("[接受]", "/tpaccept ${sender.name.string}"))
            .append(" ")
            .append(clickableButton("[拒绝]", "/tpreject ${sender.name.string}"))
        if (!allMenu) return base
        return base.append(" ")
            .append(clickableButton("[接受全部]", "/tpaccept"))
            .append(" ")
            .append(clickableButton("[拒绝全部]", "/tpreject"))
    }

}