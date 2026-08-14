package com.revers.messenger.network

import com.revers.messenger.network.models.MessageDto
import com.revers.messenger.network.models.RegisterRequest
import com.revers.messenger.network.models.RegisterResponse
import com.revers.messenger.network.models.User
import retrofit2.http.*

interface ApiService {
    @POST("api/users/register")
    suspend fun registerUser(
        @Body request: RegisterRequest
    ): RegisterResponse

    @GET("api/users/{id}")
    suspend fun getUser(
        @Path("id") id: String
    ): User

    @GET("api/users")
    suspend fun searchUsers(
        @Query("q") query: String
    ): List<User>

    @GET("api/messages/{chatId}")
    suspend fun getHistory(
        @Path("chatId") chatId: String,
        @Query("limit") limit: Int = 100
    ): List<MessageDto>
}
