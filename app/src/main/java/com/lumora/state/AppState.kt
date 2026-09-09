package com.lumora.state

import com.lumora.anime.AnimeCatalogClient
import com.lumora.model.CategoryFilter
import com.lumora.model.Channel
import com.lumora.model.ContentShelf
import java.util.Collections
import java.util.LinkedHashMap
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory state shared by the current application host.
 *
 * This is intentionally not persisted: the existing stores and caches remain the source of
 * truth across launches. It gives the Activity a single owner for the catalogue and navigation
 * values while the legacy XML/View flows are migrated incrementally.
 */
class AppState(
    val catalog: CatalogState = CatalogState(),
    val navigation: NavigationCoordinator = NavigationCoordinator(),
    val status: StatusState = StatusState()
)

/**
 * Derived catalogue values currently consumed by the legacy Activity and its extension files.
 *
 * Keeping these values together does not change their derivation, cache keys, or update timing;
 * callers continue to use the compatibility properties exposed by MainActivity during migration.
 */
data class CatalogState(
    var allChannels: List<Channel> = emptyList(),
    var liveChannels: List<Channel> = emptyList(),
    var seriesList: List<Channel> = emptyList(),
    var filmList: List<Channel> = emptyList(),
    var filmVersions: Map<String, List<Channel>> = emptyMap(),
    var seriesVersions: Map<String, List<Channel>> = emptyMap(),
    var liveVersions: Map<String, List<Channel>> = emptyMap(),
    var filmShelves: List<ContentShelf> = emptyList(),
    var seriesShelves: List<ContentShelf> = emptyList(),
    var cachedSeriesCategoryRows: List<CategoryFilter> = emptyList(),
    /** Section membership from the latest Anime catalogue fetch. */
    var animeSections: List<AnimeCatalogClient.Section> = emptyList(),
    /** Current category/shelf selection used to filter the visible derived catalogue. */
    var selectedCategoryIds: Set<String>? = null,
    var selectedBrandChannelIds: Set<String>? = null,
    var selectedRowId: String? = null,
    var selectedCategoryLabel: String? = null,
    var selectedShelfItems: List<Channel>? = null,
    /** Guards asynchronous category-filter results from overwriting a newer selection. */
    var categoryFilterGeneration: Int = 0,
    /** Expanded category groups and their last calculated children. */
    val expandedGroupKeys: MutableSet<String> = mutableSetOf(),
    var categoryChildrenCache: Map<String, List<CategoryFilter>> = emptyMap(),
    /** Per-server Home rows kept apart from the primary merged catalogue. */
    val jellyfinResumeByServer: MutableMap<String, List<Channel>> = mutableMapOf(),
    val jellyfinNextUpByServer: MutableMap<String, List<Channel>> = mutableMapOf(),
    val plexResumeByServer: MutableMap<String, List<Channel>> = mutableMapOf(),
    val plexNextUpByServer: MutableMap<String, List<Channel>> = mutableMapOf(),
    /** Thread-safe memoization for the derived Up Next shelf. */
    val upNextTiles: MutableMap<String, Channel?> =
        Collections.synchronizedMap(LinkedHashMap<String, Channel?>()),
    val upNextFetching: MutableSet<String> = Collections.synchronizedSet(mutableSetOf()),
    @Volatile var upNextEpoch: Int = 0,
    val upNextQueues: ConcurrentHashMap<String, List<Channel>> = ConcurrentHashMap()
) {
    val jellyfinResumeItems: List<Channel>
        get() = jellyfinResumeByServer.values.flatten()
    val jellyfinNextUpItems: List<Channel>
        get() = jellyfinNextUpByServer.values.flatten()
    val plexResumeItems: List<Channel>
        get() = plexResumeByServer.values.flatten()
    val plexNextUpItems: List<Channel>
        get() = plexNextUpByServer.values.flatten()
}
