@file:Suppress("unused", "PropertyName")
package xyz.xszq.gocqhttp

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import xyz.xszq.MiraiGuildCqhttp.bot
import kotlin.properties.Delegates

@Serializable
open class GOCQEvent {
    lateinit var post_type: String
    var time by Delegates.notNull<Long>()
    companion object {
        private val json = Json {
            isLenient = true
            ignoreUnknownKeys = true
        }
        fun parse(raw: JsonObject) = when (json.decodeFromJsonElement<GOCQEvent>(raw).post_type) {
            "message" -> {
                when (json.decodeFromJsonElement<GOCQMessageEvent>(raw).message_type) {
                    "guild" -> json.decodeFromJsonElement<GuildMessageEvent>(raw)
                    "group" -> json.decodeFromJsonElement<GOCQGroupMessageEvent>(raw)
                    "private" -> json.decodeFromJsonElement<GOCQPrivateMessageEvent>(raw)
                    else -> null
                }
            }
            "notice" -> null
            "meta_event" -> {
                if (json.decodeFromJsonElement<GOCQMetaEvent>(raw).meta_event_type == "heartbeat")
                    json.decodeFromJsonElement<GOCQHeartbeatEvent>(raw)
                else
                    null
            }
            else -> null
        }
    }
}

@Serializable
open class GOCQMessageEvent: GOCQEvent() {
    lateinit var message_type: String
    lateinit var message: String
    lateinit var sub_type: String
    lateinit var sender: GOCQSender
    var self_id by Delegates.notNull<Long>()
}

@Serializable
open class GOCQMetaEvent: GOCQEvent() {
    lateinit var meta_event_type: String
}
@Serializable
class GOCQHeartbeatEvent: GOCQMetaEvent() {
    var interval by Delegates.notNull<Long>()
    var self_id by Delegates.notNull<Long>()
}

@Serializable
class GuildMessageEvent: GOCQMessageEvent() {
    lateinit var channel_id: String
    lateinit var guild_id: String
    lateinit var message_id: String
    lateinit var user_id: String
    lateinit var self_tiny_id: String
    var channel: GuildChannel
        get() = bot.guilds.find { it.id == guild_id }!!.channels.find { it.id == channel_id }!!
        set(_) {}
}
@Serializable
open class GOCQNormalMessageEvent: GOCQMessageEvent() {
    lateinit var raw_message: String
    var font by Delegates.notNull<Int>()
    var message_id by Delegates.notNull<Long>()
}

@Serializable
class GOCQGroupMessageEvent: GOCQNormalMessageEvent() {
    var group_id by Delegates.notNull<Long>()
}
@Serializable
class GOCQPrivateMessageEvent: GOCQNormalMessageEvent() {
    var temp_source by Delegates.notNull<Int>()
}

@Serializable
data class GOCQSender(
    val age: Int = 0, val area: String = "", val card: String = "", val level: String = "", val nickname: String = "",
    val role: String = "", val sex: String = "", val title: String = "", val user_id: Long = 0
)

@Serializable
open class GOCQNoticeEvent: GOCQEvent() {
    lateinit var notice_type: String
}
@Serializable
open class GuildNoticeEvent: GOCQNoticeEvent() {
    lateinit var guild_id: String
    lateinit var channel_id: String
    lateinit var user_id: String
}
@Serializable
open class GuildReactionsUpdatedEvent: GOCQNoticeEvent() {
    lateinit var message_id: String
    lateinit var current_reactions: List<GuildReactionInfo>
}
@Serializable
data class GuildReactionInfo(
    val emoji_id: String, val emoji_index: Int, val emoji_type: Int, val emoji_name: String, val count: Int,
    val clicked: Boolean
)
@Serializable
open class GuildChannelUpdateEvent: GOCQNoticeEvent() {
    lateinit var operator_id: String
    lateinit var old_info: GuildChannelInfo
    lateinit var new_info: GuildChannelInfo
}