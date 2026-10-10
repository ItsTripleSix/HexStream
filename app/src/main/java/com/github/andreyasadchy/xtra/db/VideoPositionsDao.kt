package com.github.andreyasadchy.xtra.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.github.andreyasadchy.xtra.model.VideoPosition
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoPositionsDao {

    @Query("SELECT * FROM video_positions")
    fun getAll(): Flow<List<VideoPosition>>

    @Query("SELECT * FROM video_positions WHERE id = :id")
    fun getById(id: Long): VideoPosition?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(position: VideoPosition)

    @Query("DELETE FROM video_positions")
    fun deleteAll()
}