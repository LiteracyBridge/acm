package org.literacybridge.talkingbookapp.database

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset


class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): LocalDateTime? {
        return LocalDateTime.ofInstant(
            value?.let { Instant.ofEpochMilli(it) },
            ZoneId.systemDefault()
        )
    }

    @TypeConverter
    fun dateToTimestamp(date: LocalDateTime?): Long? {
        return date?.toInstant(ZoneOffset.UTC)?.toEpochMilli()
    }
}