package top.kmar.mc.tpm

import it.unimi.dsi.fastutil.objects.ObjectRBTreeSet
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTED
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPING
import net.minecraft.server.level.ServerPlayer
import top.kmar.mc.tpm.Tpm.logger
import top.kmar.mc.tpm.commands.TpmCommand
import top.kmar.mc.tpm.commands.TpmCommand.teleportTo
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

object TpRequestManager {

    /** 传送请求有效时间（ms） */
    const val TP_REQUEST_TIME_LIMIT = 10000
    private val lock = ReentrantReadWriteLock()
    @JvmStatic
    private val tpRequestList = ObjectRBTreeSet<TpRequest>()

    @JvmStatic
    fun initEvent() {
        val future = AtomicReference<ScheduledFuture<*>?>(null)
        SERVER_STARTED.register {
            future.compareAndSet(
                null,
                Tpm.scheduledExecutor.scheduleAtFixedRate({
                    val timestamp = System.currentTimeMillis()
                    lock.write {
                        tpRequestList.removeIf {
                            val timeout = it.timestamp < timestamp
                            if (timeout) {
                                it.cancelEvent?.invoke(it)
                                it.sender.sendSystemMessage(TpmCommand.grayText("您的请求已超时取消"))
                            }
                            timeout
                        }
                    }
                }, 1, 1, TimeUnit.SECONDS)
            )
        }
        SERVER_STOPPING.register {
            val scheduledFuture = future.get()
            scheduledFuture?.cancel(true) ?: false
            if (scheduledFuture?.isDone == true) {
                future.set(null)
            } else {
                Tpm.scheduledExecutor.shutdownNow()
                logger.error("严重警告：TPA 时刻管理任务注销失败！")
                throw AssertionError("代码不应进入此分支")
            }
        }
    }

    /**
     * 添加一个传送请求
     * @param force 当 srcPlayer 重复时是否强制替换
     * @param skipWrite 是否跳过对列表的写入
     * @return 是否成功，若失败表明 sender 或 srcPlayer 重复
     */
    @JvmStatic
    fun appendRequest(request: TpRequest, force: Boolean, skipWrite: Boolean = false): Boolean {
        lock.write {
            val isInList = request in tpRequestList
            if (isInList && !force) {
                return false
            }
            if (!skipWrite || isInList) {
                tpRequestList.add(request)
            }
        }
        return true
    }

    @JvmStatic
    fun find(sender: ServerPlayer, receiver: ServerPlayer): TpRequest? {
        return lock.read {
            tpRequestList.find { it.isSender(sender) && it.isReceiver(receiver) }
        }
    }

    @JvmStatic
    fun findReceiveBy(player: ServerPlayer): List<TpRequest> {
        return lock.read {
            tpRequestList.filter { it.isReceiver(player) }
        }
    }

    @JvmStatic
    fun findReceiveByAndIsTarget(player: ServerPlayer): List<TpRequest> {
        return lock.read {
            tpRequestList.filter { it.isReceiver(player) && it.isTargetPlayer(player) }
        }
    }

    @JvmStatic
    fun findSendBy(player: ServerPlayer): TpRequest? {
        return lock.read {
            tpRequestList.find { it.isSender(player) }
        }
    }

    @JvmStatic
    private fun removeAllRequests(requests: Iterable<TpRequest>) {
        lock.write {
            for (request in requests) {
                tpRequestList.remove(request)
            }
        }
    }

    @JvmStatic
    fun rejectRequests(requests: List<TpRequest>) {
        removeAllRequests(requests)
        for (request in requests) {
            request.rejectEvent?.invoke(request)
        }
    }

    @JvmStatic
    fun acceptRequests(requests: List<TpRequest>) {
        removeAllRequests(requests)
        for (request in requests) {
            request.srcPlayer.teleportTo(request.targetPlayer)
            request.acceptEvent?.invoke(request)
        }
    }

    data class TpRequest(
        val sender: ServerPlayer,
        val srcPlayer: ServerPlayer,
        val targetPlayer: ServerPlayer,
        val acceptEvent: ((request: TpRequest) -> Unit)? = null,
        val rejectEvent: ((request: TpRequest) -> Unit)? = null,
        val cancelEvent: ((request: TpRequest) -> Unit)? = null
    ) : Comparable<TpRequest> {

        val timestamp = System.currentTimeMillis() + TP_REQUEST_TIME_LIMIT

        val receiver: ServerPlayer
            get() = if (sender === srcPlayer) targetPlayer else srcPlayer

        fun accept() {
            lock.write {
                tpRequestList.remove(this)
            }
            srcPlayer.teleportTo(targetPlayer)
            acceptEvent?.invoke(this)
        }

        fun reject() {
            lock.write {
                tpRequestList.remove(this)
            }
            rejectEvent?.invoke(this)
        }

        fun cancel() {
            lock.write {
                tpRequestList.remove(this)
            }
            cancelEvent?.invoke(this)
        }

        fun isSender(player: ServerPlayer): Boolean {
            return player.uuid == sender.uuid
        }

        fun isReceiver(player: ServerPlayer): Boolean {
            return player.uuid == receiver.uuid
        }

        fun isSrcPlayer(player: ServerPlayer): Boolean {
            return player.uuid == srcPlayer.uuid
        }

        fun isTargetPlayer(player: ServerPlayer): Boolean {
            return player.uuid == targetPlayer.uuid
        }

        override fun compareTo(other: TpRequest): Int {
            return sender.uuid.compareTo(other.sender.uuid)
        }

        override fun equals(other: Any?): Boolean {
            if (other == null || javaClass != other.javaClass) return false
            other as TpRequest
            return sender.uuid == other.sender.uuid
        }

        override fun hashCode(): Int {
            return sender.hashCode()
        }
    }

}