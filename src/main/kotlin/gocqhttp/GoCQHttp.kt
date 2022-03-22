@file:Suppress("SpellCheckingInspection")

package xyz.xszq.gocqhttp

import kotlinx.serialization.Serializable

@Serializable
data class GOCQResponse<T>(
    val status: String,
    val retcode: Int,
    val data: T?
)
