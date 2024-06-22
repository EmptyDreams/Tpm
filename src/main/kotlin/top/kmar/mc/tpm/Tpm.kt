package top.kmar.mc.tpm

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import top.kmar.mc.tpm.commands.TpmCommand
import top.kmar.mc.tpm.save.TpmWorldData
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

object Tpm : ModInitializer {

    @JvmStatic
    val logger: Logger = LoggerFactory.getLogger("TPM")

    @JvmStatic
    var scheduledExecutor: ScheduledExecutorService = initScheduledExecutor()
        private set

    override fun onInitialize() {
        TpmWorldData.initWorld()
        TpRequestManager.initEvent()
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            logger.info("开始注册指令")
            TpmCommand.registry(dispatcher)
        }
    }

    @JvmStatic
    fun resetExecutor() {
        scheduledExecutor.shutdownNow()
        val success = scheduledExecutor.awaitTermination(30, TimeUnit.SECONDS)
        if (!success) {
            logger.error("严重错误！！！TPM 时钟退出失败")
            throw AssertionError("正常情况下不应当进入此分支")
        }
        scheduledExecutor = initScheduledExecutor()
    }

    private fun initScheduledExecutor() = try {
        val executor = Executors.newScheduledThreadPool(100, Thread.ofVirtual().factory())
        logger.info("检测到虚拟线程可用，TPM 时钟迁移到虚拟线程中执行")
        executor
    } catch (_: Throwable) {
        logger.info("虚拟线程不可用，TPM 时钟回退到线程池中执行")
        Executors.newSingleThreadScheduledExecutor()
    }

}