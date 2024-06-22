package top.kmar.mc.tpm.commands

import com.mojang.brigadier.CommandDispatcher
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component
import top.kmar.mc.tpm.commands.TpmCommand.teleportTo

object TpmTpSpawn {

    fun registry(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(
            Commands.literal("tpspawn").executes { context ->
                val player = context.source.playerOrException
                val respawnPos = player.respawnPosition
                val respawnDimension = player.respawnDimension
                if (respawnPos != null) {
                    val respawnLevel = player.server.getLevel(respawnDimension)
                    if (respawnLevel != null) {
                        player.teleportTo(respawnLevel, respawnPos)
                        player.sendSystemMessage(TpmCommand.grayText("已将您传送到重生点"))
                        return@executes 1
                    }
                }
                val overworld = player.server.overworld()
                val isOverworld = player.serverLevel() === overworld
                var message = Component.literal("您没有重生点或床已遗失 ")
                if (isOverworld) {
                    val pos = overworld.sharedSpawnPos
                    message = message.append(
                        TpmCommand.clickableButton("[传送到世界出生点]", "/tpp ${pos.x} ${pos.y} ${pos.z}")
                    )
                } else {
                    val overworldPos = overworld.sharedSpawnPos
                    val nowPos = player.serverLevel().sharedSpawnPos
                    message = message.append(
                        TpmCommand.clickableButton(
                            "[传送到主世界出生点]",
                            "/tpp ${overworld.dimension().location()} ${overworldPos.x} ${overworldPos.y} ${overworldPos.z}"
                        )
                    )
                    if (nowPos.y > player.serverLevel().minBuildHeight) {
                        message = message.append(" ")
                            .append(TpmCommand.clickableButton(
                                "[传送到当前世界出生点]",
                                "/tpp ${nowPos.x} ${nowPos.y} ${nowPos.z}"
                            ))
                    }
                }
                player.sendSystemMessage(message)
                1
            }
        )
    }

}