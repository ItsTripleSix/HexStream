package com.github.andreyasadchy.xtra.repository.datasource

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class BrowseSessionTest {
    private data class Row(val id: String?, val login: String? = null, val stream: String? = id, val viewers: Int = 1)
    private fun batch(vararg ids: String, cursor: String? = null) = BrowseSession.Batch(ids.map { Row(it) }, cursor)
    private fun session(pages: List<BrowseSession.Batch<Row>>, paths: List<String> = listOf("gql")): BrowseSession<Row> {
        var index = 0
        return BrowseSession(paths, { BrowseSession.Identity(it.id, it.login, it.stream) }, { _, _ -> pages[index++] })
    }

    @Test fun uniqueRowsKeepOrder() = runBlocking {
        val s = session(listOf(batch("a", "b", cursor = "c1"), batch("c")))
        assertEquals(listOf("a", "b", "c"), s.load(3).items.map { it.id })
    }
    @Test fun adjacentDuplicatesAndChangedStreamIdKeepFirstMetadata() = runBlocking {
        val original = Row("a", "alice", "stream1", 5)
        val s = session(listOf(BrowseSession.Batch(listOf(original), "c1"),
            BrowseSession.Batch(listOf(Row("a", "alice", "stream2", 500), Row("b")), null)))
        assertSame(original, s.load(1).items.single())
        assertEquals(listOf("b"), s.load(1).items.map { it.id })
        assertEquals(5, original.viewers)
        assertEquals(1, s.counters.duplicatesDropped)
    }
    @Test fun duplicateHeavyPageAggregatesInternally() = runBlocking {
        val s = session(listOf(batch("a", "b", cursor = "c1"), batch("a", "b", "c", cursor = "c2"), batch("d")))
        s.load(2)
        assertEquals(listOf("c", "d"), s.load(2).items.map { it.id })
        assertEquals(3, s.counters.upstreamPagesRequested)
    }
    @Test fun fullyRepeatedPageAdvancesToValidPage() = runBlocking {
        val s = session(listOf(batch("a", cursor = "c1"), batch("a", cursor = "c2"), batch("b")))
        s.load(1)
        assertEquals("b", s.load(1).items.single().id)
    }
    @Test fun repeatedCursorSwitchesPathWithGlobalDedupe() = runBlocking {
        val calls = mutableListOf<Pair<String, String?>>()
        val pages = listOf(batch("a", cursor = "same"), batch("a", cursor = "same"), batch("a", "b"))
        val s = BrowseSession(listOf("gql", "persisted"), { r: Row -> BrowseSession.Identity(r.id, r.login, r.stream) },
            { path, cursor -> calls.add(path to cursor); pages[calls.lastIndex] })
        assertEquals(listOf("a", "b"), s.load(2).items.map { it.id })
        assertEquals("persisted" to null, calls.last())
        assertEquals(1, s.counters.repeatedCursorsDetected)
        assertEquals(1, s.counters.APIPathSwitches)
    }
    @Test fun repeatedCursorWithoutFallbackTerminates() = runBlocking {
        val s = session(listOf(batch("a", cursor = "same"), batch("a", cursor = "same")))
        assertFalse(s.load(3).hasNext)
        assertEquals(2, s.counters.upstreamPagesRequested)
    }
    @Test fun emptyPageWithContinuationDoesNotEndList() = runBlocking {
        val s = session(listOf(batch(cursor = "c1"), batch("a")))
        assertEquals("a", s.load(1).items.single().id)
        assertEquals(1, s.counters.emptyPagesReceived)
    }
    @Test fun emptyGraphqlEdgesWithContinuationSwitchCompatiblePath() = runBlocking {
        val s = session(listOf(batch(), batch("a")), listOf("gql", "persisted"))
        assertEquals("a", s.load(1).items.single().id)
        assertEquals(1, s.counters.APIPathSwitches)
    }
    @Test fun blankFinalCursorEndsAfterBufferedRows() = runBlocking {
        val s = session(listOf(batch("a", "b", cursor = " ")))
        assertTrue(s.load(1).hasNext)
        assertFalse(s.load(1).hasNext)
        assertEquals(1, s.counters.upstreamPagesRequested)
    }
    @Test fun errorFallbackDoesNotReintroduceOverlappingRows() = runBlocking {
        var calls = 0
        val s = BrowseSession(listOf("gql", "persisted"), { r: Row -> BrowseSession.Identity(r.id, r.login, r.stream) }, { _, _ ->
            when (calls++) { 0 -> batch("a", cursor = "c1"); 1 -> error("synthetic failure"); else -> batch("a", "b") }
        })
        assertEquals(listOf("a", "b"), s.load(2).items.map { it.id })
    }
    @Test fun missingBroadcasterUsesCaseInsensitiveLoginThenStream() = runBlocking {
        val rows = listOf(Row(null, "Alice", "s1"), Row("b1", "alice", "s2"), Row("b1", null, "s3"),
            Row(null, null, "s4"), Row(null, null, "s4"), Row(null, null, null))
        val s = session(listOf(BrowseSession.Batch(rows, null)))
        assertEquals(3, s.load(10).items.size)
        assertEquals(3, s.counters.duplicatesDropped)
    }
    @Test fun tenThousandIdentitiesRemainUnique() = runBlocking {
        val pages = (0 until 100).map { page -> BrowseSession.Batch((0 until 100).map { Row("${page * 100 + it}") }, if (page == 99) null else "c$page") }
        val s = session(pages)
        val rows = mutableListOf<Row>()
        do { val result = s.load(30); rows.addAll(result.items) } while (result.hasNext)
        assertEquals(10000, rows.size)
        assertEquals(10000, rows.map { it.id }.toSet().size)
    }
    @Test fun noProgressHasHardBudget() = runBlocking {
        var calls = 0
        val s = BrowseSession(listOf("a", "b", "c"), { r: Row -> BrowseSession.Identity(r.id, r.login, r.stream) }, { _, _ ->
            calls++; batch(cursor = "c$calls")
        })
        assertFalse(s.load(100).hasNext)
        assertEquals(BrowseSession.MAX_REQUESTS, calls)
    }
    @Test fun freshSessionAcceptsPreviouslySeenIdentity() = runBlocking {
        repeat(2) { assertEquals("a", session(listOf(batch("a"))).load(1).items.single().id) }
    }
    @Test fun existingGlobalFiltersAreAppliedBeforeFillingVisiblePage() = runBlocking {
        var calls = 0
        val s = BrowseSession(listOf("gql"), { r: Row -> BrowseSession.Identity(r.id, r.login, r.stream) },
            { _, _ -> if (calls++ == 0) batch("blocked", cursor = "c1") else batch("allowed") },
            select = { rows -> rows.filter { it.id != "blocked" } })
        assertEquals("allowed", s.load(1).items.single().id)
        assertEquals(2, s.counters.upstreamItemsReceived)
        assertEquals(1, s.counters.uniqueItemsAccepted)
    }
    @Test fun bufferedRowsSurviveNetworkRetry() = runBlocking {
        var calls = 0
        val s = BrowseSession(listOf("a"), { r: Row -> BrowseSession.Identity(r.id, r.login, r.stream) }, { _, _ ->
            when (calls++) { 0 -> batch("a", cursor = "c1"); 1 -> error("network"); else -> batch("b") }
        })
        try { s.load(2); fail("Expected network error") } catch (_: IllegalStateException) { }
        assertEquals(listOf("a", "b"), s.load(2).items.map { it.id })
    }
}
