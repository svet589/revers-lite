package com.revers.messenger.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.revers.messenger.crypto.CryptoManager
import com.revers.messenger.network.ApiService
import com.revers.messenger.network.WebSocketManager
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val cryptoManager: CryptoManager,
    private val apiService: ApiService,
    private val webSocketManager: WebSocketManager
) : ViewModel() {

    data class UiState(
        val name: String = "",
        val userId: String = "",
        val hasKeys: Boolean = false,
        val isLoading: Boolean = false,
        val error: String = ""
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        // Проверяем наличие ключей
        try {
            val publicKey = cryptoManager.getIdentityPublicKeyHex()
            if (publicKey.isNotEmpty()) {
                _uiState.update { it.copy(hasKeys = true) }
            }
        } catch (e: Exception) {
            // Ключей нет
        }
    }

    fun updateName(name: String) {
        _uiState.update { it.copy(name = name) }
    }

    fun register() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = "") }

            try {
                // Генерируем ключи
                val keyPair = cryptoManager.generateIdentity()
                val publicKey = cryptoManager.getIdentityPublicKeyHex()
                val userId = "rev_" + System.currentTimeMillis().toString(36)

                // Регистрация на сервере
                val response = apiService.registerUser(
                    com.revers.messenger.network.models.RegisterRequest(
                        id = userId,
                        publicKey = publicKey,
                        name = _uiState.value.name.ifEmpty { "User" }
                    )
                )

                if (response.success) {
                    _uiState.update {
                        it.copy(
                            userId = userId,
                            hasKeys = true,
                            isLoading = false
                        )
                    }
                    // Подключаем WebSocket
                    webSocketManager.connect(userId, publicKey)
                } else {
                    _uiState.update {
                        it.copy(
                            error = response.error ?: "Ошибка регистрации",
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = e.message ?: "Ошибка подключения",
                        isLoading = false
                    )
                }
            }
        }
    }

    fun login() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = "") }

            try {
                val publicKey = cryptoManager.getIdentityPublicKeyHex()
                val userId = "rev_" + System.currentTimeMillis().toString(36)

                // Подключаем WebSocket
                webSocketManager.connect(userId, publicKey)
                
                _uiState.update {
                    it.copy(
                        userId = userId,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = e.message ?: "Ошибка входа",
                        isLoading = false
                    )
                }
            }
        }
    }
}
