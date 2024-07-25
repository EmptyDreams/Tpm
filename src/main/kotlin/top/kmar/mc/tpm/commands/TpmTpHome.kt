package top.kmar.mc.tpm.commands

import com.mojang.brigadier.CommandDispatcher
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import top.kmar.mc.tpm.data.DimensionalBlockPos.Companion.teleportTo
import top.kmar.mc.tpm.save.tpmData

object TpmTpHome {

    fun registry(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(
            Commands.literal("tphome").executes { context ->
                val player = context.source.playerOrException
                val tpmHome = player.tpmData.home
                if (tpmHome == null) {
                    player.sendSystemMessage(
                        TpmCommand.errorText("未设置家 ")
                            .append(TpmCommand.clickableButton("[设置到当前位置]", "/tpconfig set home"))
                    )
                    return@executes 0
                }
                player.teleportTo(tpmHome)
                1
            }
        )
    }

}