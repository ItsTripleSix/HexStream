package com.github.andreyasadchy.xtra.repository.datasource

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.github.andreyasadchy.xtra.graphql.type.Language
import com.github.andreyasadchy.xtra.graphql.type.StreamSort
import com.github.andreyasadchy.xtra.model.ui.Stream
import com.github.andreyasadchy.xtra.repository.GraphQLRepository
import com.github.andreyasadchy.xtra.repository.HelixRepository
import com.github.andreyasadchy.xtra.util.C

class GameStreamsDataSource(
    private val gameId: String?,
    private val gameSlug: String?,
    private val gameName: String?,
    private val gqlQueryLanguages: List<Language>?,
    private val gqlQuerySort: StreamSort?,
    private val gqlLanguages: List<String>?,
    private val gqlSort: String?,
    private val tags: List<String>?,
    private val gqlHeaders: Map<String, String>,
    private val graphQLRepository: GraphQLRepository,
    private val helixHeaders: Map<String, String>,
    private val helixRepository: HelixRepository,
    private val enableIntegrity: Boolean,
    private val networkLibrary: String?,
) : PagingSource<Int, Stream>() {
    private var offset: String? = null
    private val session = BrowseSession(
        paths = listOf(C.GQL, C.GQL_PERSISTED_QUERY) + if (
            !helixHeaders[C.HEADER_TOKEN].isNullOrBlank() &&
            (gqlSort == "VIEWER_COUNT" || gqlSort == null) &&
            (gqlQuerySort == StreamSort.VIEWER_COUNT || gqlQuerySort == null) &&
            tags.isNullOrEmpty() && gqlQueryLanguages.isNullOrEmpty() && gqlLanguages.isNullOrEmpty()
        ) listOf(C.HELIX) else emptyList(),
        identity = { stream: Stream -> BrowseSession.Identity(stream.channelId, stream.channelLogin, stream.id) },
        fetch = { path, cursor ->
            offset = cursor
            when (path) {
                C.GQL -> gqlQueryLoad()
                C.GQL_PERSISTED_QUERY -> gqlLoad()
                C.HELIX -> helixLoad()
                else -> error("Unsupported directory path")
            }
        },
        select = { com.github.andreyasadchy.xtra.util.ContentFilters.filter(it) },
        report = { if (com.github.andreyasadchy.xtra.BuildConfig.DEBUG) android.util.Log.d("CategorySnapshot", it.toString()) },
    )

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Stream> = try {
        val page = session.load(params.loadSize)
        LoadResult.Page(page.items, null, if (page.hasNext) (params.key ?: 0) + 1 else null)
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (e: Exception) {
        LoadResult.Error(e)
    }
    private suspend fun gqlQueryLoad(): BrowseSession.Batch<Stream> {
        val response = graphQLRepository.loadQueryGameStreams(
            networkLibrary = networkLibrary,
            headers = gqlHeaders,
            id = gameId,
            slug = gameSlug.takeIf { gameId.isNullOrBlank() },
            name = gameName.takeIf { gameId.isNullOrBlank() && gameSlug.isNullOrBlank() },
            sort = gqlQuerySort,
            tags = tags,
            languages = gqlQueryLanguages,
            first = BrowseSession.UPSTREAM_BATCH_SIZE,
            after = offset
        )
        if (enableIntegrity) {
            response.errors?.find { it.message == C.FAILED_INTEGRITY_CHECK }?.let { throw Exception(it.message) }
        }
        val data = response.data!!.game!!.streams!!
        val items = data.edges!!
        val list = items.mapNotNull { item ->
            item?.node?.let {
                Stream(
                    id = it.id,
                    channelId = it.broadcaster?.id,
                    channelLogin = it.broadcaster?.login,
                    channelName = it.broadcaster?.displayName,
                    channelImageURL = it.broadcaster?.profileImageURL,
                    gameId = gameId,
                    gameSlug = gameSlug,
                    gameName = gameName,
                    title = it.broadcaster?.broadcastSettings?.title,
                    language = it.broadcaster?.broadcastSettings?.language,
                    thumbnailURL = it.previewImageURL,
                    createdAt = it.createdAt?.toString(),
                    viewerCount = it.viewersCount,
                    tags = it.freeformTags?.mapNotNull { tag -> tag.name },
                )
            }
        }
        offset = items.lastOrNull()?.cursor?.toString()
        val nextPage = data.pageInfo?.hasNextPage != false
        return BrowseSession.Batch(list, offset, nextPage)
    }

    private suspend fun gqlLoad(): BrowseSession.Batch<Stream> {
        val response = graphQLRepository.loadGameStreams(networkLibrary, gqlHeaders, gameSlug, gqlSort, tags, gqlLanguages, BrowseSession.UPSTREAM_BATCH_SIZE, offset)
        if (enableIntegrity) {
            response.errors?.find { it.message == C.FAILED_INTEGRITY_CHECK }?.let { throw Exception(it.message) }
        }
        val data = response.data!!.game.streams
        val items = data.edges
        val list = items.mapNotNull { item ->
            item.node.let {
                Stream(
                    id = it.id,
                    channelId = it.broadcaster?.id,
                    channelLogin = it.broadcaster?.login,
                    channelName = it.broadcaster?.displayName,
                    channelImageURL = it.broadcaster?.profileImageURL,
                    gameId = gameId,
                    gameSlug = gameSlug,
                    gameName = gameName,
                    title = it.title,
                    thumbnailURL = it.previewImageURL,
                    createdAt = it.createdAt,
                    viewerCount = it.viewersCount,
                    tags = it.freeformTags?.mapNotNull { tag -> tag.name },
                )
            }
        }
        offset = items.lastOrNull()?.cursor
        val nextPage = data.pageInfo?.hasNextPage != false
        return BrowseSession.Batch(list, offset, nextPage)
    }

    private suspend fun helixLoad(): BrowseSession.Batch<Stream> {
        val response = helixRepository.getStreams(
            networkLibrary = networkLibrary,
            headers = helixHeaders,
            gameId = gameId,
            limit = BrowseSession.UPSTREAM_BATCH_SIZE,
            offset = offset,
        )
        val users = response.data.mapNotNull { it.channelId }.let {
            helixRepository.getUsers(
                networkLibrary = networkLibrary,
                headers = helixHeaders,
                ids = it,
            ).data
        }
        val list = response.data.mapNotNull {
            Stream(
                id = it.id,
                channelId = it.channelId,
                channelLogin = it.channelLogin,
                channelName = it.channelName,
                channelImageURL = it.channelId?.let { id ->
                    users.find { user -> user.id == id }?.profileImageURL
                },
                gameId = gameId,
                gameName = gameName,
                title = it.title,
                language = it.language,
                thumbnailURL = it.thumbnailURL,
                createdAt = it.startedAt,
                viewerCount = it.viewerCount,
                tags = it.tags,
            )
        }
        offset = response.pagination?.cursor
        return BrowseSession.Batch(list, offset)
    }

    override fun getRefreshKey(state: PagingState<Int, Stream>): Int? = null
}
