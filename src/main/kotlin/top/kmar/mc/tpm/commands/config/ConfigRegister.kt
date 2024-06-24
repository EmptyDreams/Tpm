package top.kmar.mc.tpm.commands.config

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import top.kmar.mc.tpm.commands.TpmCommand
import java.util.concurrent.ConcurrentHashMap

class ConfigRegister(
    private val commandName: String
) {

    private val configMap = ConcurrentHashMap<String, ConfigValue>()

    operator fun set(key: String, config: ConfigValue) {
        configMap[key] = config
    }

    /**
     * 注册指令
     * @param dispatcher 指令注册机
     */
    fun registry(
        dispatcher: CommandDispatcher<CommandSourceStack>,
        editor: (LiteralArgumentBuilder<CommandSourceStack>) -> LiteralArgumentBuilder<CommandSourceStack>
    ) {
        var rootSet = Commands.literal("set")
        var rootGet = Commands.literal("get")
        configMap.forEach { (key, config) ->
            var base = Commands.literal(key)
            rootGet = rootGet.then(base.executes { context ->
                val player = context.source.playerOrException
                readConfig(player, key)
                1
            })
            base = config.commands(base)
            rootSet = rootSet.then(base)
        }
        dispatcher.register(
            editor(Commands.literal(commandName)).then(rootSet).then(rootGet)
        )
    }

    private fun readConfig(player: ServerPlayer, key: String) {
        val config = configMap[key]
        if (config == null) {
            player.sendSystemMessage(TpmCommand.errorText("配置名不存在"))
            return
        }
        val data = config.reader(player) ?: TpmCommand.grayText("配置项未设置值")
        player.sendSystemMessage(data)
    }

    data class ConfigValue(
        val commands: (LiteralArgumentBuilder<CommandSourceStack>) -> LiteralArgumentBuilder<CommandSourceStack>,
        val reader: (ServerPlayer) -> Component?
    )

}