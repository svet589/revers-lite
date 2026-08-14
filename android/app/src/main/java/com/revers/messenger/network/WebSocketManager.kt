package com.revers.messenger.network

import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject

class WebSocketManager(
    private val serverUrl: String
) {
    private var socket: Socket? = null
    private val listeners = mutableListOf<WebSocketListener>()

    interface WebSocketListener {
        fun onConnected()
        fun onDisconnected()
        fun onNewMessage(data: NewMessageEvent)
        fun onMessageSent(data: MessageSentEvent)
        fun onUserOnline(userId: String)
        fun onUserOffline(userId: String)
        fun onTyping(chatId: String, userId: String, isTyping: Boolean)
        fun onError(error: String)
    }

    data class NewMessageEvent(
        val id: Long,
        val chatId: String,
        val senderId: String,
        val ciphertext: String,
        val timestamp: Long,
        val replyToId: Long? = null
    )

    data class MessageSentEvent(
        val id: Long,
        val timestamp: Long
    )

    fun connect(userId: String, publicKey: String) {
        try {
            val opts = IO.Options().apply {
                reconnection = true
                reconnectionAttempts = 10
                reconnectionDelay = 1000
            }

            socket = IO.socket(serverUrl, opts)

            socket?.on(Socket.EVENT_CONNECT) {
                Log.d("WS", "Подключено к серверу")
                val data = JSONObject().apply {
                    put("userId", userId)
                    put("publicKey", publicKey)
                }
                socket?.emit("register", data)
                listeners.forEach { it.onConnected() }
            }

            socket?.on(Socket.EVENT_DISCONNECT) {
                Log.d("WS", "Отключено от сервера")
                listeners.forEach { it.onDisconnected() }
            }

            socket?.on("new_message") { args ->
                try {
                    val data = args[0] as JSONObject
                    val event = NewMessageEvent(
                        id = data.getLong("id"),
                        chatId = data.getString("chatId"),
                        senderId = data.getString("senderId"),
                        ciphertext = data.getString("ciphertext"),
                        timestamp = data.getLong("timestamp"),
                        replyToId = if (data.has("replyToId") && !data.isNull("replyToId"))
                            data.getLong("replyToId") else null
                    )
                    listeners.forEach { it.onNewMessage(event) }
                } catch (e: Exception) {
                    Log.e("WS", "Ошибка парсинга", e)
                }
            }

            socket?.on("message_sent") { args ->
                try {
                    val data = args[0] as JSONObject
                    listeners.forEach {
                        it.onMessageSent(MessageSentEvent(
                            id = data.getLong("id"),
                            timestamp = data.getLong("timestamp")
                        ))
                    }
                } catch (e: Exception) {
                    Log.e("WS", "Ошибка парсинга", e)
                }
            }

            socket?.on("user_online") { args ->
                try {
                    val data = args[0] as JSONObject
                    listeners.forEach { it.onUserOnline(data.getString("userId")) }
                } catch (e: Exception) {
                    Log.e("WS", "Ошибка парсинга", e)
                }
            }

            socket?.on("user_offline") { args ->
                try {
                    val data = args[0] as JSONObject
                    listeners.forEach { it.onUserOffline(data.getString("userId")) }
                } catch (e: Exception) {
                    Log.e("WS", "Ошибка парсинга", e)
                }
            }

            socket?.on("typing") { args ->
                try {
                    val data = args[0] as JSONObject
                    listeners.forEach {
                        it.onTyping(
                            chatId = data.getString("chatId"),
                            userId = data.getString("userId"),
                            isTyping = data.getBoolean("isTyping")
                        )
                    }
                } catch (e: Exception) {
                    Log.e("WS", "Ошибка парсинга", e)
                }
            }

            socket?.connect()
        } catch (e: Exception) {
            Log.e("WS", "Ошибка подключения", e)
            listeners.forEach { it.onError("Не удалось подключиться") }
        }
    }

    fun disconnect() {
        socket?.disconnect()
        socket?.close()
        socket = null
    }

    fun sendMessage(chatId: String, ciphertext: String, replyToId: Long? = null) {
        val data = JSONObject().apply {
            put("chatId", chatId)
            put("ciphertext", ciphertext)
            if (replyToId != null) put("replyToId", replyToId)
        }
        socket?.emit("send_message", data)
    }

    fun sendTyping(chatId: String, isTyping: Boolean) {
        val data = JSONObject().apply {
            put("chatId", chatId)
            put("isTyping", isTyping)
        }
        socket?.emit("typing", data)
    }

    fun addListener(listener: WebSocketListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: WebSocketListener) {
        listeners.remove(listener)
    }

    fun isConnected(): Boolean = socket?.connected() ?: false
}
