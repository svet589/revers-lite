package com.revers.messenger.network.models

import com.google.gson.annotations.SerializedName

data class User(
    val id: String,
    @SerializedName("public_key")
    val publicKey: String,
    val name: String,
    val avatar: String?,
    @SerializedName("last_seen")
    val lastSeen: Long?,
    val isOnline: Boolean = false
)

data class MessageDto(
    val id: Long,
    @SerializedName("chat_id")
    val chatId: String,
    @SerializedName("sender_id")
    val senderId: String,
    val ciphertext: String,
    val timestamp: Long,
    @SerializedName("is_read")
    val isRead: Boolean,
    @SerializedName("reply_to_id")
    val replyToId: Long?,
    val isOutgoing: Boolean = false
)

data class RegisterRequest(
    val id: String,
    val publicKey: String,
    val name: String
)

data class RegisterResponse(
    val success: Boolean,
    val id: String? = null,
    val error: String? = null
)
