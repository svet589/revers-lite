package com.revers.messenger.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.revers.messenger.database.AppDatabase
import com.revers.messenger.database.entities.ContactEntity
import com.revers.messenger.network.WebSocketManager
import javax.inject.Inject

@HiltViewModel
class ChatsViewModel @Inject constructor(
    private val database: AppDatabase,
    private val webSocketManager: WebSocketManager
) : ViewModel() {

    private val _contacts = MutableStateFlow<List<ContactEntity>>(emptyList())
    val contacts: StateFlow<List<ContactEntity>> = _contacts.asStateFlow()

    init {
        loadContacts()
        observeWebSocketEvents()
    }

    private fun loadContacts() {
        viewModelScope.launch {
            database.contactDao().getAllContacts().collect { contacts ->
                _contacts.value = contacts
            }
        }
    }

    private fun observeWebSocketEvents() {
        webSocketManager.addListener(object : WebSocketManager.WebSocketListener {
            override fun onConnected() {}

            override fun onDisconnected() {}

            override fun onNewMessage(data: WebSocketManager.NewMessageEvent) {
                viewModelScope.launch {
                    val contact = database.contactDao().getContact(data.senderId)
                    contact?.let {
                        database.contactDao().updateLastMessage(
                            it.id,
                            "Новое сообщение",
                            data.timestamp
                        )
                        database.contactDao().incrementUnread(it.id)
                    }
                }
            }

            override fun onMessageSent(data: WebSocketManager.MessageSentEvent) {}

            override fun onUserOnline(userId: String) {
                viewModelScope.launch {
                    database.contactDao().setOnline(userId, true)
                }
            }

            override fun onUserOffline(userId: String) {
                viewModelScope.launch {
                    database.contactDao().setOnline(userId, false)
                }
            }

            override fun onTyping(chatId: String, userId: String, isTyping: Boolean) {}

            override fun onError(error: String) {}
        })
    }
}
