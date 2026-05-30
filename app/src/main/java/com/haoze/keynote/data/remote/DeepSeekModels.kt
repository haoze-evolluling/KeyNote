package com.haoze.keynote.data.remote

import com.google.gson.annotations.SerializedName

data class DeepSeekRequest(
    val model: String = "deepseek-v4-flash",
    val messages: List<Message>,
    val temperature: Double = 0.3,
    @SerializedName("max_tokens")
    val maxTokens: Int = 150
)

data class Message(
    val role: String,
    val content: String
)

data class DeepSeekResponse(
    val choices: List<Choice>
)

data class Choice(
    val message: Message
)
