package com.github.andreyasadchy.xtra.util

import com.github.andreyasadchy.xtra.XtraApp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import android.os.SystemClock

/** Derived from follows, never copied into the user's manual whitelist. */
object FollowExemptions {
    private val mutex = Mutex()
    private var account: String? = null
    private var ids = emptySet<String>()
    private var refreshed = 0L
    @Volatile private var generation = 0L
    private var loadedGeneration = -1L
    @Synchronized fun invalidate() { generation++ }
    suspend fun changed(id: String?, following: Boolean) {
        invalidate()
        if (id == null) return
        val active = XtraApp.INSTANCE.tokenPrefs().getString(C.USER_ID, null) ?: return
        mutex.withLock {
            if (account != active) { account = active; ids = emptySet(); refreshed = 0L; loadedGeneration = -1L }
            ids = if (following) ids + id else ids - id
        }
    }

    suspend fun current(): Set<String> {
        val app = XtraApp.INSTANCE as XtraApp
        val module = app.xtraModule
        val local = module.localChannelFollowsRepository.getAll().mapNotNull { it.userId }.toSet()
        val active = app.tokenPrefs().getString(C.USER_ID, null) ?: return local
        return mutex.withLock {
            if (account != active) { account = active; ids = emptySet(); refreshed = 0L; loadedGeneration = -1L }
            val stamp = generation
            if (loadedGeneration != stamp || SystemClock.elapsedRealtime() - refreshed > 60000L) {
                val headers = TwitchApiHelper.getGQLHeaders(app, true)
                if (!headers[C.HEADER_TOKEN].isNullOrBlank()) {
                    try {
                        val fetched = withTimeoutOrNull(8000) {
                            val result = mutableSetOf<String>()
                            var cursor: String? = null
                            do {
                                val response = module.graphQLRepository.loadQueryUserFollowedUsers(
                                    app.prefs().getString(C.NETWORK_LIBRARY, C.OKHTTP), headers, 100, cursor)
                                check(response.errors.isNullOrEmpty()) { "Could not refresh follows" }
                                val follows = response.data?.user?.follows ?: error("Missing follow list")
                                follows.edges.orEmpty().forEach { edge -> edge?.node?.id?.let { result += it } }
                                val next = follows.edges?.lastOrNull()?.cursor?.toString()
                                cursor = next.takeIf { follows.pageInfo?.hasNextPage == true && it != cursor }
                            } while (!cursor.isNullOrBlank())
                            result.toSet()
                        }
                        if (fetched != null && app.tokenPrefs().getString(C.USER_ID, null) == active && generation == stamp) {
                            ids = fetched; loadedGeneration = stamp
                        }
                    } catch (e: CancellationException) { throw e } catch (_: Exception) { /* Keep this account's last successful snapshot. */ }
                }
                refreshed = SystemClock.elapsedRealtime()
                // Retry transient failures later rather than for every paging request.
                if (generation == stamp) loadedGeneration = stamp
            }
            if (app.tokenPrefs().getString(C.USER_ID, null) == active) ids + local else emptySet()
        }
    }
}
