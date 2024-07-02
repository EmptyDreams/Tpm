package top.kmar.mc.tpm.save

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import it.unimi.dsi.fastutil.objects.ObjectArraySet
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import top.kmar.mc.tpm.Tpm
import top.kmar.mc.tpm.commands.TpmTpConfig
import java.nio.file.Files
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object DefaultConfigData {

    @JvmStatic
    private val map = ConcurrentHashMap<String, List<ConfigData>>()

    @JvmStatic
    fun reloadConfig(server: MinecraftServer) {
        map.clear()
        val file = server.getFile("config/tpm.json")
        if (file.exists()) {
            val text = Files.readString(file.toPath())
            val json = try {
                JsonParser.parseString(text).asJsonObject
            } catch (e: Exception) {
                Tpm.logger.error("配置文件语法或格式错误")
                return
            }
            when (val version = json["version"].asInt) {
                1 -> handleVersion1(server, json["default"].asJsonObject)
                else -> {
                    Tpm.logger.error("无法解析版本号为 $version 的配置文件")
                }
            }
            Tpm.logger.info("配置文件加载完毕")
        } else {
            Files.writeString(file.toPath(), """
                {
                    "version": 1,
                    "default": {
                        "[key]": {
                            "regex": "<regex>",
                            "value": null
                        }
                    }
                }
            """.trimIndent(), Charsets.UTF_8)
            Tpm.logger.info("已自动生成配置文件")
        }
    }

    @JvmStatic
    private fun handleVersion1(server: MinecraftServer, json: JsonObject) {
        TpmTpConfig.configMap.forEach { (key, config) ->
            if (!json.has(key)) return@forEach
            val array = json[key].asJsonArray
            val list = ArrayList<ConfigData>(array.size())
            for (element in array) {
                val obj = element.asJsonObject
                val regexOption = ObjectArraySet<RegexOption>(2).apply {
                    add(RegexOption.CANON_EQ)
                    if (obj.has("ignoreCase") && obj["ignoreCase"].asBoolean) {
                        add(RegexOption.IGNORE_CASE)
                    }
                }
                val regex = try {
                    Regex(obj["regex"].asString, regexOption)
                } catch (e: Exception) {
                    Tpm.logger.error("配置文件中存在错误的正则表达式：default.${key}.regex")
                    continue
                }
                val value = try {
                    config.parseJson!!(server, obj["value"])
                } catch (e: Exception) {
                    Tpm.logger.error("配置文件中存在错误的配置规则：default.${key}.value", e)
                    continue
                }
                list.add(ConfigData(regex, value))
            }
            map[key] = Collections.unmodifiableList(list)
        }
    }

    @JvmStatic
    fun readDefault(player: ServerPlayer, key: String): CompoundTag? {
        val list = map[key] ?: return null
        return list.find { it.regex.containsMatchIn(player.name.string) }?.value
    }

    private data class ConfigData(
        val regex: Regex,
        val value: CompoundTag
    )

}