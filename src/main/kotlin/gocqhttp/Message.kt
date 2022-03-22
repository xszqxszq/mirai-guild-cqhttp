@file:Suppress("unused")

package xyz.xszq.gocqhttp

import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.message.data.Image.Key.queryUrl
import net.mamoe.mirai.utils.ExternalResource
import net.mamoe.mirai.utils.ExternalResource.Companion.uploadAsImage
import xyz.xszq.MiraiGuildCqhttp

open class CQMessage(private val type: String, private val args: Map<String, String>): MessageContent {
    override fun contentToString(): String = "[CQ:$type," +
            args.map { "${it.key}=${it.value}" }.joinToString(",") +
            "]"
    override fun toString(): String = contentToString()
}

data class GuildAt(val qq: String): CQMessage("at",
    buildMap {
        put("qq", qq)
    }
)
// Use this when sending image plz
data class GuildImage(val md5: String, val url: String): CQMessage("image", buildMap {
    put("file", "$md5.image")
    put("url", url)
})
@Suppress("EXPERIMENTAL_API_USAGE")
suspend fun MessageChain.serializeToCQ(): String {
    val result = StringBuilder()
    forEach {
        when (it) {
            is PlainText -> result.append(it.content)
            is Image -> result.append(GuildImage(it.md5.toString(), it.queryUrl()).contentToString())
            else -> result.append(it.contentToString())
        }
    }
    return result.toString()
}

suspend fun ExternalResource.uploadAsGuildImage(uploadTo: Contact = MiraiGuildCqhttp.miraiBot.asFriend) =
    uploadAsImage(uploadTo).run {
        GuildImage(md5.toString(), queryUrl())
    }

suspend fun GuildMessageEvent.quoteReply(message: MessageChain) = channel.sendMessage(buildMessageChain {
    // add(GuildReply(message_id))
    add(GuildAt(sender.user_id.toString()))
    addAll(message)
})
suspend fun GuildMessageEvent.quoteReply(message: SingleMessage) = quoteReply(messageChainOf(message))
suspend fun GuildMessageEvent.quoteReply(message: String) = quoteReply(message.toPlainText())