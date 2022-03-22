@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package xyz.xszq.gocqhttp

import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.SingleMessage
import net.mamoe.mirai.message.data.buildMessageChain
import xyz.xszq.Config
import xyz.xszq.MiraiGuildCqhttp


class Guild3rdPartyBot(
    val nickname: String,
    val id: String,
    val avatarUrl: String,
    val guilds: MutableList<Guild> = mutableListOf()
) {
    suspend fun getGuildList() {
        guilds.clear()
        coroutineScope {
            client.get<GOCQResponse<List<GuildInfo>>>("$api/get_guild_list").data!!.forEach {
                launch {
                    kotlin.runCatching {
                        guilds.add(client.get<GOCQResponse<Guild>>("$api/get_guild_meta_by_guest") {
                            parameter("guild_id", it.guild_id)
                        }.data!!.apply {
                            id = it.guild_id
                            displayId = it.guild_display_id
                            setOwner()
                            setChannels()
                        })
                    }.onFailure {
                        it.printStackTrace()
                    }
                }
            }
        }
    }
    companion object {
        val api = "http://127.0.0.1:${Config.sendPort}"
        val client = HttpClient {
            install(JsonFeature) {
                serializer = KotlinxSerializer(kotlinx.serialization.json.Json {
                    isLenient = true
                    ignoreUnknownKeys = true
                })
            }
            install(HttpTimeout) {
                connectTimeoutMillis = 60000L
                requestTimeoutMillis = 60000L
                socketTimeoutMillis = 60000L
            }
            expectSuccess = false
        }
        suspend fun get(): Guild3rdPartyBot {
            val info = client.get<GOCQResponse<Guild3rdPartyBotInfo>>("$api/get_guild_service_profile").data!!
            val result = Guild3rdPartyBot(info.nickname, info.tiny_id, info.avatar_url)
            result.getGuildList()
            return result
        }
    }
}

@Serializable
data class Guild3rdPartyBotInfo(
    val nickname: String, val tiny_id: String, val avatar_url: String
)
@Serializable
data class GuildInfo(
    val guild_id: String, val guild_name: String, val guild_display_id: ULong
)

@Serializable
data class Guild(
    private val guild_id: String,
    private val guild_name: String,
    private val guild_profile: String,
    private val create_time: Long,
    private val max_member_count: Long,
    private val max_robot_count: Long,
    private val max_admin_count: Long,
    private val member_count: Int,
    private val owner_id: String,
    var id: String = guild_id,
    val name: String = guild_name,
    val profile: String = guild_profile,
    val createTime: Long = create_time,
    val maxMember: Long = max_member_count,
    val maxRobot: Long = max_robot_count,
    val maxAdmin: Long = max_admin_count,
    var channels: List<GuildChannel> = emptyList(),
    var owner: GuildMember ?= null,
    var displayId: ULong = 0uL,
    var members: Int = member_count
) {
    suspend fun getMember(userId: String) = Guild3rdPartyBot.client
        .get<GOCQResponse<GuildMember>>("${Guild3rdPartyBot.api}/get_guild_member_profile") {
            parameter("guild_id", id)
            parameter("user_id", userId)
        }.data
    suspend fun setOwner() {
        kotlin.runCatching {
            owner = getMember(owner_id)
        }.onFailure {
            it.printStackTrace()
        }
    }
    suspend fun setChannels() {
        kotlin.runCatching {
            channels = Guild3rdPartyBot.client
                .get<GOCQResponse<List<GuildChannelInfo>>>("${Guild3rdPartyBot.api}/get_guild_channel_list") {
                    parameter("guild_id", id)
                    parameter("no_cache", false)
                }.data!!.map {
                    GuildChannel(it.channel_id, it.channel_type, it.channel_name, it.owner_guild_id, it.create_time,
                        it.talk_permission, it.visible_type, it.current_slow_mode, it.slow_modes,
                        getMember(it.creator_tiny_id))
                }
        }.onFailure {
            it.printStackTrace()
        }
    }
}

@Serializable
data class GuildChannelInfo(
    val owner_guild_id: String,
    val channel_id: String,
    val channel_type: Int,
    val channel_name: String,
    val create_time: Long,
    val creator_tiny_id: String,
    val talk_permission: Int,
    val visible_type: Int,
    val current_slow_mode: Int,
    val slow_modes: List<GuildSlowModeInfo>,
)

@Serializable
data class GuildChannel(
    var id: String,
    var type: Int,
    var name: String,
    var guildId: String,
    var createTime: Long,
    var talkPermission: Int,
    var visible: Int,
    var slowModeNow: Int,
    var slowModes: List<GuildSlowModeInfo> = emptyList(),
    var creator: GuildMember ?= null,
) {
    suspend fun sendMessage(message: MessageChain): GuildMessageFeedback? {
        val msg = message.serializeToCQ()
        Guild3rdPartyBot.client
            .get<GOCQResponse<GuildMessageFeedback>>("${Guild3rdPartyBot.api}/send_guild_channel_msg") {
                parameter("guild_id", guildId)
                parameter("channel_id", id)
                parameter("message", msg)
            }.data ?.let {
                MiraiGuildCqhttp.logger.info("[$name" +
                        "<${MiraiGuildCqhttp.bot.guilds.find { g -> g.id == guildId }!!.name}>($id)] <- $msg")
                return it
            }
        return null
    }
    suspend fun sendMessage(single: SingleMessage) = sendMessage(buildMessageChain { add(single) })
    suspend fun sendMessage(str: String) = sendMessage(buildMessageChain { add(str
        .replace("\n", "\\n")) })
}


@Serializable
data class GuildMessageFeedback(
    val message_id: String
)

@Serializable
data class GuildSlowModeInfo(
    private val slow_mode_key: Int,
    private val slow_mode_text: String,
    private val slow_mode_circle: Int,
    private val speak_frequency: Int,
    val slowModeKey: Int = slow_mode_key,
    val slowModeText: String = slow_mode_text,
    val slowModeCircle: Int = slow_mode_circle,
    val speakFrequency: Int = speak_frequency,
)

@Serializable
data class GuildMember(
    private val tiny_id: String,
    private val join_time: Long,
    private val avatar_url: String,
    val id: String = tiny_id,
    val nickname: String,
    val avatarUrl: String = avatar_url,
    val joinTime: Long = join_time,
    val roles: List<GuildPermGroup> = emptyList()
)

@Serializable
data class GuildPermGroup(
    private val role_id: String,
    private val role_name: String,
    val id: String = role_id,
    val name: String = role_name
)