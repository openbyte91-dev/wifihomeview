package com.openbyt91dev.wifihomeview.data.local

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromList(list: List<Int>?): String? {
        return list?.joinToString(",")
    }

    @TypeConverter
    fun toList(data: String?): List<Int>? {
        return data?.split(",")?.filter { it.isNotEmpty() }?.map { it.toInt() }
    }
}
