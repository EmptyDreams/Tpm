package top.kmar.mc.tpm.commands

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.BoolArgumentType
import net.minecraft.ChatFormatting
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component
import top.kmar.mc.tpm.commands.config.BooleanConfig
import top.kmar.mc.tpm.commands.config.ConfigRegister
import top.kmar.mc.tpm.commands.config.ConfigRegister.ConfigValue
import top.kmar.mc.tpm.commands.config.DimensionalBlockPos
import top.kmar.mc.tpm.data.DoubleBlockPos
import top.kmar.mc.tpm.save.readOfflineData
import top.kmar.mc.tpm.save.setOfflineData
import top.kmar.mc.tpm.save.tpmHome

object TpmConfig {

    @JvmStatic
    private val configMap = ConfigRegister("tpconfig").apply {
        this["home"] = ConfigValue(
            commands = {
                it.executes { context ->
                    val player = context.source.playerOrException
                    player.tpmHome = DimensionalBlockPos(player.serverLevel(), player.x, player.y + 0.5, player.z)
                    player.sendSystemMessage(TpmCommand.grayText("已将家设置到当前位置"))
                    1
                }.then(TpmCommand.joinArguments(*TpmCommand.worldPosArgument) { context ->
                    @Suppress("DuplicatedCode")
                    val player = context.source.playerOrException
                    val (x, y, z) = DoubleBlockPos.readFromContext(context)
                    player.tpmHome = DimensionalBlockPos(player.serverLevel(), x, y, z)
                    1
                })
            },
            reader = { player ->
                val tpmHome = player.tpmHome ?: return@ConfigValue null
                Component.literal("当前您的家的坐标为：")
                    .append(tpmHome.toComponent())
            }
        )
        this["auto_reject"] = ConfigValue(
            commands = {
                it.then(
                    Commands.argument("value", BoolArgumentType.bool())
                        .executes { context ->
                            val player = context.source.playerOrException
                            val value = BoolArgumentType.getBool(context, "value")
                            player.setOfflineData("auto_reject", BooleanConfig.from(value))
                            if (value && player.readOfflineData("auto_accept", BooleanConfig.builder) == BooleanConfig.TRUE) {
                                player.setOfflineData("auto_accept", BooleanConfig.FALSE)
                                player.sendSystemMessage(TpmCommand.grayText("自动拒绝已启用，自动接受自动关闭"))
                            } else {
                                player.sendSystemMessage(TpmCommand.grayText("自动拒绝已启用"))
                            }
                            1
                        }
                )
            },
            reader = { player ->
                val value = player.readOfflineData("auto_reject", BooleanConfig.builder)!!.value
                Component.literal("是否启用自动拒绝：")
                    .append(Component.literal(value.toString().uppercase()).withStyle(ChatFormatting.DARK_GRAY))
            }
        )
        this["auto_accept"] = ConfigValue(
            commands = {
                it.then(
                    Commands.argument("value", BoolArgumentType.bool())
                        .executes { context ->
                            val player = context.source.playerOrException
                            val value = BoolArgumentType.getBool(context, "value")
                            player.setOfflineData("auto_accept", BooleanConfig.from(value))
                            if (
                                value &&
                                player.readOfflineData("auto_reject", BooleanConfig.builder) == BooleanConfig.TRUE
                            ) {
                                player.setOfflineData("auto_reject", BooleanConfig.FALSE)
                                player.sendSystemMessage(TpmCommand.grayText("自动接受已启用，自动拒绝自动关闭"))
                            } else {
                                player.sendSystemMessage(TpmCommand.grayText("自动接受已启用"))
                            }
                            1
                        }
                )
            },
            reader = { player ->
                val value = player.readOfflineData("auto_accept", BooleanConfig.builder)!!.value
                Component.literal("是否启用自动接受：")
                    .append(Component.literal(value.toString().uppercase()).withStyle(ChatFormatting.DARK_GRAY))
            }
        )
    }

    @JvmStatic
    fun registry(dispatcher: CommandDispatcher<CommandSourceStack>) {
        configMap.registry(dispatcher) { it }
    }

}