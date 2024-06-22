package top.kmar.mc.tpm

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import top.kmar.mc.tpm.commands.TpmCommand
import top.kmar.mc.tpm.save.TpmWorldData
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService

object Tpm : ModInitializer {

    @JvmStatic
    val logger: Logger = LoggerFactory.getLogger("TPM")

    @JvmStatic
    val scheduledExecutor: ScheduledExecutorService by lazy(LazyThreadSafetyMode.NONE) { initScheduledExecutor() }

    override fun onInitialize() {
        TpmWorldData.initWorld()
        TpRequestManager.initEvent()
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            logger.info("开始注册指令")
            TpmCommand.registry(dispatcher)
        }
    }

    @Suppress("Since15")
    private fun initScheduledExecutor() = try {
        val executor = Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().factory())
        logger.info("检测到虚拟线程可用，TPM 时钟迁移到虚拟线程中执行")
        executor
    } catch (_: Throwable) {
        logger.info("虚拟线程不可用，TPM 时钟回退到线程池中执行")
        Executors.newSingleThreadScheduledExecutor()
    }

}