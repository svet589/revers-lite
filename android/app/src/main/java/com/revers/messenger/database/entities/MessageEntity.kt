package com.revers.messenger.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey
    val id: Long,
    val chatId: String,
    val senderId: String,
    val ciphertext: String,
    val timestamp: Long,
    val isRead: Boolean,
    val isOutgoing: Boolean,
    val replyToId: Long? = null
)
