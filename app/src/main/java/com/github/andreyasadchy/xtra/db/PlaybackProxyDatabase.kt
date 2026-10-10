package com.github.andreyasadchy.xtra.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.github.andreyasadchy.xtra.model.ui.CustomProxy

/** Playback URLs belong to the installation, outside all account databases. */
@Database(entities = [CustomProxy::class], version = 1, exportSchema = false)
abstract class PlaybackProxyDatabase : RoomDatabase() {
    abstract fun proxies(): CustomProxiesDao
}
