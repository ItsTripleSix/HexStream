package com.github.andreyasadchy.xtra.repository.datasource

import kotlinx.coroutines.CancellationException
import java.util.Locale

/** One append-only category snapshot. A new PagingSource creates a new session. */
internal class BrowseSession<T>(
    private val paths: List<String>,
    private val identity: (T) -> Identity,
    private val fetch: suspend (String, String?) -> Batch<T>,
    private val select: suspend (List<T>) -> List<T> = { it },
    private val report: (Counters) -> Unit = {},
) {
    data class Identity(val broadcaster: String?, val login: String?, val stream: String?)
    data class Batch<T>(val items: List<T>, val cursor: String?, val hasNext: Boolean = true)
    data class Result<T>(val items: List<T>, val hasNext: Boolean)
    data class Counters(
        var upstreamPagesRequested: Int = 0,
        var upstreamItemsReceived: Int = 0,
        var uniqueItemsAccepted: Int = 0,
        var duplicatesDropped: Int = 0,
        var repeatedCursorsDetected: Int = 0,
        var emptyPagesReceived: Int = 0,
        var recoveryRequests: Int = 0,
        var APIPathSwitches: Int = 0,
        var consecutiveNoProgressPages: Int = 0,
    )
    val counters = Counters()
    private val seen = HashSet<String>()
    private val cursors = HashSet<String>()
    private val pending = ArrayDeque<T>()
    private var path = 0
    private var cursor: String? = null
    private var ended = false

    private fun switchPath(): Boolean {
        if (path + 1 >= paths.size) { ended = true; return false }
        path++
        cursor = null // Cursors are private to each API; retain identities across paths.
        cursors.clear()
        counters.consecutiveNoProgressPages = 0
        counters.APIPathSwitches++
        return true
    }

    suspend fun load(target: Int): Result<T> {
        var requests = 0
        while (pending.size < target && !ended && requests < MAX_REQUESTS) {
            requests++
            counters.upstreamPagesRequested++
            if (requests > 1) counters.recoveryRequests++
            val batch = try {
                fetch(paths[path], cursor)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (switchPath()) continue
                // Keep accepted rows buffered if a network failure requires a Paging retry.
                ended = false
                report(counters.copy())
                throw e
            }
            counters.upstreamItemsReceived += batch.items.size
            if (batch.items.isEmpty()) counters.emptyPagesReceived++
            var accepted = 0
            for (item in select(batch.items)) {
                val id = identity(item)
                val keys = listOfNotNull(
                    id.broadcaster?.takeIf { it.isNotBlank() }?.let { "b:$it" },
                    id.login?.takeIf { it.isNotBlank() }?.lowercase(Locale.ROOT)?.let { "l:$it" },
                    id.stream?.takeIf { it.isNotBlank() }?.let { "s:$it" },
                )
                if (keys.any { it in seen }) {
                    counters.duplicatesDropped++
                    seen.addAll(keys) // Link a formerly missing broadcaster ID to its login.
                } else {
                    seen.addAll(keys)
                    pending.addLast(item)
                    accepted++
                    counters.uniqueItemsAccepted++
                }
            }
            counters.consecutiveNoProgressPages = if (accepted == 0) counters.consecutiveNoProgressPages + 1 else 0
            val next = batch.cursor?.takeIf { it.isNotBlank() }
            if (next == null && batch.hasNext && batch.items.isEmpty()) {
                // Edge-based GraphQL can indicate continuation without yielding a cursor.
                // Never retry the old cursor: restart the next compatible path instead.
                switchPath()
            } else if (next == null || !batch.hasNext) {
                ended = true
            } else if (!cursors.add(next)) {
                counters.repeatedCursorsDetected++
                counters.consecutiveNoProgressPages++
                switchPath()
            } else {
                cursor = next
                if (counters.consecutiveNoProgressPages >= MAX_NO_PROGRESS) switchPath()
            }
        }
        if (requests == MAX_REQUESTS && pending.isEmpty()) ended = true
        val rows = ArrayList<T>(minOf(target, pending.size))
        repeat(minOf(target, pending.size)) { rows.add(pending.removeFirst()) }
        report(counters.copy())
        return Result(rows, pending.isNotEmpty() || !ended)
    }

    companion object {
        const val UPSTREAM_BATCH_SIZE = 100
        const val MAX_REQUESTS = 6
        const val MAX_NO_PROGRESS = 3
    }
}
