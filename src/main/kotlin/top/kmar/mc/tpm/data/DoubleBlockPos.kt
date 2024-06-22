package top.kmar.mc.tpm.data

import com.mojang.brigadier.arguments.DoubleArgumentType
import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack

data class DoubleBlockPos(
    val x: Double,
    val y: Double,
    val z: Double
) {

    companion object {

        /** 从指令上下文中提取坐标 */
        @JvmStatic
        fun readFromContext(context: CommandContext<CommandSourceStack>): DoubleBlockPos {
            val x = DoubleArgumentType.getDouble(context, "x")
            val y = DoubleArgumentType.getDouble(context, "y")
            val z = DoubleArgumentType.getDouble(context, "z")
            return DoubleBlockPos(x, y, z)
        }

    }

}