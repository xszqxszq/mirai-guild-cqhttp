@file:Suppress("unused")

package xyz.xszq.gocqhttp

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import xyz.xszq.MiraiGuildCqhttp.bot
import java.util.Collections.synchronizedList

object GlobalGuildEventChannel {
    private val events = Channel<GOCQEvent>()
    private val listeners: MutableList<CQEventListener> = synchronizedList(mutableListOf())
    suspend fun start() {
        coroutineScope {
            while (true) {
                val event = events.receive()
                launch {
                    listeners.forEach { listener ->
                        launch {
                            kotlin.runCatching {
                                if (event is GuildMessageEvent && listener is GuildMessageEventListener)
                                    listener.callOnSuccess(event)
                            }.onFailure {
                                it.printStackTrace()
                            }
                        }
                    }
                }
            }
        }
    }
    suspend fun send(event: GOCQEvent) = events.send(event)
    fun subscribeAlways(onEvent: suspend GuildMessageEvent.() -> Unit) =
        listeners.add(GuildMessageEventBasicListener({ true }, onEvent))
    fun subscribeMessages(actions: suspend SubscribeBuilder.() -> Unit) =
        runBlocking { actions.invoke(SubscribeBuilder) }
    object SubscribeBuilder {
        fun startsWith(prefix: String, removePrefix: Boolean = true, trim: Boolean = true,
                               onEvent: suspend GuildMessageEvent.(String) -> Unit) {
            listeners.add(GuildMessageEventBasicListener({
                this.message.startsWith(prefix)
            }) {
                var param = if (removePrefix) this.message.substringAfter(prefix) else this.message
                if (trim) param = param.trim()
                onEvent.invoke(this, param)
            })
        }
        fun endsWith(suffix: String, removeSuffix: Boolean = true, trim: Boolean = true,
                               onEvent: suspend GuildMessageEvent.(String) -> Unit) {
            listeners.add(GuildMessageEventBasicListener({
                this.message.endsWith(suffix)
            }) {
                var param = if (removeSuffix) this.message.substringBeforeLast(suffix) else this.message
                if (trim) param = param.trim()
                onEvent.invoke(this, param)
            })
        }
        fun always(onEvent: suspend GuildMessageEvent.() -> Unit) {
            listeners.add(GuildMessageEventBasicListener({ true }, onEvent))
        }
        fun atBot(onEvent: suspend GuildMessageEvent.() -> Unit) {
            listeners.add(GuildMessageEventBasicListener({
                GuildAt(bot.id).contentToString() in message
            }, onEvent))
        }
        fun finding(regex: Regex, onEvent: suspend GuildMessageEvent.(MatchResult) -> Unit) {
            listeners.add(GuildMessageEventListenerWithResult({
                regex.find(message)
            }, onEvent))
        }
        operator fun String.invoke(onEvent: suspend GuildMessageEvent.() -> Unit) = let { str ->
            listeners.add(GuildMessageEventBasicListener({ message == str }, onEvent))
        }
    }
}

interface CQEventListener {
    val case: Any
    val onEvent: Any
}
open class GuildMessageEventListener(override val case: Any, override val onEvent: Any): CQEventListener {
    open suspend fun callOnSuccess(event: GuildMessageEvent) {}
}

class GuildMessageEventBasicListener(
    override val case: suspend GuildMessageEvent.() -> Boolean, override val onEvent: suspend GuildMessageEvent.() -> Unit
) : GuildMessageEventListener(case, onEvent) {
    override suspend fun callOnSuccess(event: GuildMessageEvent) {
        kotlin.runCatching {
            if (case.invoke(event)) {
                onEvent.invoke(event)
            }
        }.onFailure {
            it.printStackTrace()
        }
    }
}
class GuildMessageEventListenerWithResult<T>(
    override val case: suspend GuildMessageEvent.() -> T?, override val onEvent: suspend GuildMessageEvent.(T) -> Unit
): GuildMessageEventListener(case, onEvent) {
    override suspend fun callOnSuccess(event: GuildMessageEvent) {
        kotlin.runCatching {
            case.invoke(event) ?.let {
                onEvent.invoke(event, it)
            }
        }.onFailure {
            it.printStackTrace()
        }
    }
}
