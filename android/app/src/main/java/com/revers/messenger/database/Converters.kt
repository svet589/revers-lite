package com.revers.messenger.database

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromLongList(value: String?): List<Long>? {
        return value?.split(",")?.mapNotNull { it.toLongOrNull() }
    }

    @TypeConverter
    fun toLongList(list: List<Long>?): String? {
        return list?.joinToString(",")
    }
}
