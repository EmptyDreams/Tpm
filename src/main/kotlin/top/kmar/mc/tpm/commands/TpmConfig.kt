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
    val configMap = ConfigRegister("tpconfig").apply {
        this["home"] = ConfigValue(
            commands = { it, playerGetter ->
                if (playerGetter == null) {
                    it.executes { context ->
                        writer(context.source.playerOrException, context)
                    }.then(TpmCommand.joinArguments(*TpmCommand.worldPosArgument) { context ->
                        writer(context.source.playerOrException, context)
                    })
                } else {
                    it.then(TpmCommand.joinArguments(*TpmCommand.worldPosArgument) { context ->
                        context.source.player?.sendSystemMessage(TpmCommand.grayText("已成功修改目标家的坐标"))
                        writer(playerGetter(context), context)
                    })
                }
            },
            reader = { player ->
                val tpmHome = player.tpmHome ?: return@ConfigValue null
                Component.literal("当前您的家的坐标为：")
                    .append(tpmHome.toComponent())
            },
            writer = { player, context ->
                player!!
                val pos = if (context.input.endsWith('e')) {
                    player.sendSystemMessage(TpmCommand.grayText("已将您的家设置到当前位置"))
                    DimensionalBlockPos(player.serverLevel(), player.x, player.y + 0.5, player.z)
                } else {
                    val (x, y, z) = DoubleBlockPos.readFromContext(context)
                    player.sendSystemMessage(TpmCommand.grayText("已将您的家设置到指定位置"))
                    DimensionalBlockPos(player.serverLevel(), x, y, z)
                }
                player.tpmHome = pos
                1
            }
        )
        this["auto_reject"] = ConfigValue(
            commands = { it, playerGetter ->
                it.then(
                    Commands.argument("value", BoolArgumentType.bool())
                        .executes {context ->
                            if (playerGetter == null) {
                                writer(context.source.playerOrException, context)
                            } else {
                                context.source.player?.sendSystemMessage(TpmCommand.grayText("已成功修改目标的自动拒绝配置"))
                                writer(playerGetter(context), context)
                            }
                        }
                )
            },
            reader = { player ->
                val value = player.readOfflineData("auto_reject", BooleanConfig.builder)!!.value
                Component.literal("是否启用自动拒绝：")
                    .append(Component.literal(value.toString().uppercase()).withStyle(ChatFormatting.DARK_GRAY))
            },
            writer = { player, context ->
                player!!
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
        this["auto_accept"] = ConfigValue(
            commands = { it, playerGetter ->
                it.then(
                    Commands.argument("value", BoolArgumentType.bool())
                        .executes { context ->
                            if (playerGetter == null) {
                                writer(context.source.playerOrException, context)
                            } else {
                                context.source.player?.sendSystemMessage(TpmCommand.grayText("已成功修改目标的自动接受配置"))
                                writer(playerGetter(context), context)
                            }
                        }
                )
            },
            reader = { player ->
                val value = player.readOfflineData("auto_accept", BooleanConfig.builder)!!.value
                Component.literal("是否启用自动接受：")
                    .append(Component.literal(value.toString().uppercase()).withStyle(ChatFormatting.DARK_GRAY))
            },
            writer = { player, context ->
                player!!
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
    }

    @JvmStatic
    fun registry(dispatcher: CommandDispatcher<CommandSourceStack>) {
        configMap.registry(dispatcher) { it }
    }

}