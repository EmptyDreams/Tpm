package top.kmar.mc.tpm.commands

import com.mojang.brigadier.CommandDispatcher
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import top.kmar.mc.tpm.commands.TpmCommand.clickableUrl
import top.kmar.mc.tpm.commands.TpmCommand.grayText
import top.kmar.mc.tpm.commands.TpmCommand.whiteText
import top.kmar.mc.tpm.commands.TpmCommand.yellowText

object TpmTpHelp {

    fun registry(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(
            Commands.literal("tphelp")
                .executes { context ->
                    val player = context.source.playerOrException
                    val message = arrayOf(
                        yellowText("===================== TPM 帮助菜单 ====================="),
                        yellowText("/tpp").append(whiteText(" 传送到当前所在世界的主城")),
                        yellowText("/tpp <leve>").append(whiteText(" 传送到指定世界的主城")),
                        yellowText("/tpp <x> <y> <z>").append(whiteText(" 传送到当前世界指定坐标")),
                        yellowText("/tpp <level> <x> <y> <z>").append(whiteText(" 传送到指定世界指定坐标")),
                        yellowText("/tpa <player>").append(whiteText(" 请求传送到指定玩家")),
                        yellowText("/tphere <player>").append(whiteText(" 请求将对方传送到自己")),
                        yellowText("/tphome").append(whiteText(" 传送到自己家")),
                        yellowText("/tpspwan").append(whiteText(" 传送到重生点")),
                        yellowText("/tpconfig set home").append(whiteText(" 设置家到当前位置")),
                        yellowText("/tpconfig set auto_accept <true/false>").append(whiteText(" 调整自动接受开关")),
                        yellowText("/tpconfig set auto_reject <true/false>").append(whiteText(" 调整自动拒绝开关")),
                        yellowText("===================== TPM 帮助菜单 ====================="),
                        grayText("嘿，史蒂夫！展开聊天框，别让折叠的菜单挡住了你的冒险指南！"),
                        grayText("想要更多秘籍？去 ")
                            .append(clickableUrl("Github", "https://github.com/EmptyDreams/Tpm#readme"))
                            .append(grayText(" 挖掘更详细的说明吧！"))
                    )
                    player.sendSystemMessage(message.reduce { acc, value -> acc.append("\n").append(value) })
                    1
                }
        )
    }

}