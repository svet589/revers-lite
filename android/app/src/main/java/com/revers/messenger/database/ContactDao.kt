package com.revers.messenger.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.revers.messenger.database.entities.ContactEntity

@Dao
interface ContactDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: ContactEntity)

    @Update
    suspend fun updateContact(contact: ContactEntity)

    @Query("SELECT * FROM contacts ORDER BY lastMessageTimestamp DESC")
    suspend fun getAllContacts(): List<ContactEntity>

    @Query("SELECT * FROM contacts WHERE id = :id")
    suspend fun getContact(id: String): ContactEntity?

    @Query("DELETE FROM contacts WHERE id = :id")
    suspend fun deleteContact(id: String)

    @Query("UPDATE contacts SET lastMessage = :lastMessage, lastMessageTimestamp = :timestamp WHERE id = :contactId")
    suspend fun updateLastMessage(contactId: String, lastMessage: String, timestamp: Long)

    @Query("UPDATE contacts SET unreadCount = unreadCount + 1 WHERE id = :contactId")
    suspend fun incrementUnread(contactId: String)

    @Query("UPDATE contacts SET unreadCount = 0 WHERE id = :contactId")
    suspend fun clearUnread(contactId: String)

    @Query("UPDATE contacts SET isOnline = :isOnline WHERE id = :contactId")
    suspend fun setOnline(contactId: String, isOnline: Boolean)
}
