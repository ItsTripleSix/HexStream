package com.github.andreyasadchy.xtra.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.github.andreyasadchy.xtra.model.chat.RecentEmote
import kotlinx.coroutines.flow.Flow


@Dao
interface RecentEmotesDao {

    @Query("SELECT * FROM recent_emotes ORDER BY used_at DESC")
    fun getAllFlow(): Flow<List<RecentEmote>>

    @Query("SELECT * FROM recent_emotes ORDER BY used_at DESC")
    fun getAll(): List<RecentEmote>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(emotes: Collection<RecentEmote>)

    @Query("DELETE FROM recent_emotes WHERE name NOT IN (SELECT name FROM recent_emotes ORDER BY used_at DESC LIMIT ${RecentEmote.MAX_SIZE})")
    fun deleteOld()

    @Transaction
    fun ensureMaxSizeAndInsert(emotes: Collection<RecentEmote>) {
        insertAll(emotes)
        deleteOld()
    }
}
