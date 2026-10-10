package com.github.andreyasadchy.xtra.model.chat

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recent_emotes")
class RecentEmote(
    @PrimaryKey
    val name: String,
    @ColumnInfo(name = "used_at")
    val usedAt: Long,
) {

    companion object {
        const val MAX_SIZE = 50
    }
}
