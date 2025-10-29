package com.example.letslink.API_related

import androidx.room.TypeConverter
import java.util.UUID

class UUIDConverter {
    @TypeConverter
    fun fromUUID(uuid: UUID?): String? {
        return uuid?.toString()
    }

    @TypeConverter
    fun toUUID(uuidString: String?): UUID? {
        return uuidString?.let { UUID.fromString(it) }
    }
}
/*
* Ref list
* A.Butakidis (2018). Room TypeConverter.
* [online] Stack Overflow. Available at: https://stackoverflow.com/questions/53085704/room-typeconverter.
* [Accessed 7 October 2025]
* */