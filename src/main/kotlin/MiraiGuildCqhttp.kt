@file:Suppress("SpellCheckingInspection")

package xyz.xszq

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.serialization.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import net.mamoe.mirai.Bot
import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.value
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.BotOnlineEvent
import net.mamoe.mirai.utils.info
import xyz.xszq.gocqhttp.GOCQEvent
import xyz.xszq.gocqhttp.GlobalGuildEventChannel
import xyz.xszq.gocqhttp.Guild3rdPartyBot

object MiraiGuildCqhttp : KotlinPlugin(
    JvmPluginDescription(
        id = "xyz.xszq.mirai-guild-cqhttp",
        name = "MiraiGuild",
        version = "0.1.0",
    ) {
        author("xszqxszq")
    }
) {
    val bot by lazy {
        runBlocking {
            Guild3rdPartyBot.get()
        }
    }
    lateinit var miraiBot: Bot
    override fun onEnable() {
        laterFindBot()
        CoroutineScope(Dispatchers.IO).launch {
            GlobalGuildEventChannel.start()
        }
        GlobalGuildEventChannel.subscribeAlways {
            logger.info("[${channel.name}<${bot.guilds.find { it.id == guild_id }!!.name}>(${channel.id})] " +
                    "${sender.nickname}(${sender.user_id}) -> $message")
        }
        embeddedServer(Netty, port=Config.receivePort) {
            install(ContentNegotiation) {
                json(Json {
                    isLenient = true
                    ignoreUnknownKeys = true
                })
            }
            routing {
                post("/") {
                    GOCQEvent.parse(call.receive()) ?.let {
                        GlobalGuildEventChannel.send(it)
                        call.respond("")
                    }
                }
            }
        }.start(wait = false)
        logger.info { "Plugin loaded" }
    }
    private fun laterFindBot() {
        GlobalEventChannel.subscribeAlways<BotOnlineEvent> {
            miraiBot = this.bot
        }
    }
}
object Config: AutoSavePluginConfig("config") {
    val receivePort by value(19199)
    val sendPort by value(19198)
}