package com.nuvio.app.features.livetv

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.random.Random

@Serializable
internal data class DesktopM3uSource(
    val id: String, val name: String, val url: String = "",
    val fileName: String = "", val playlistData: String = "",
)

@Serializable
internal data class DesktopM3uSourcesState(
    val sources: List<DesktopM3uSource> = emptyList(), val activeId: String? = null,
) {
    val active: DesktopM3uSource? get() = sources.firstOrNull { it.id == activeId }
}

internal fun newDesktopM3uSourceId(): String = "m3u-${LiveTvClock.nowEpochMs()}-${Random.nextLong().toString(16)}"

internal object DesktopM3uSourcesStorageBridge {
    var read: (Int) -> String? = { null }
    var write: (Int, String) -> Unit = { _, _ -> }
}

// Storage is injected so migration and profile separation can be tested without user preferences.
internal class DesktopM3uSourceStore(
    private val read: (Int) -> String?, private val write: (Int, String) -> Unit,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val mutableState = MutableStateFlow(DesktopM3uSourcesState())
    val state = mutableState.asStateFlow()
    private var profileId: Int? = null

    fun load(profile: Int, legacyType: LiveTvSourceType, legacyUrl: String, legacyData: String) {
        profileId = profile
        val stored = read(profile)
        val restored = stored?.let { runCatching { json.decodeFromString<DesktopM3uSourcesState>(it) }.getOrNull() }
        if (restored != null) {
            mutableState.value = restored.copy(activeId = restored.activeId?.takeIf { id -> restored.sources.any { it.id == id } })
            return
        }
        val legacy = if (legacyType == LiveTvSourceType.M3u && (legacyUrl.isNotBlank() || legacyData.isNotBlank())) {
            DesktopM3uSource("legacy-m3u", "Ma liste M3U", url = legacyUrl.takeIf { it.startsWith("http://") || it.startsWith("https://") }.orEmpty(),
                fileName = if (legacyData.isNotBlank()) legacyUrl else "", playlistData = legacyData)
        } else null
        mutableState.value = DesktopM3uSourcesState(listOfNotNull(legacy), legacy?.id?.takeIf { legacyType == LiveTvSourceType.M3u })
        persist()
    }

    fun save(source: DesktopM3uSource) {
        require(source.name.isNotBlank()) { "Donnez un nom à la liste M3U." }
        require(source.playlistData.isNotBlank() || source.url.startsWith("http://") || source.url.startsWith("https://")) {
            "Saisissez une adresse M3U en HTTP ou HTTPS."
        }
        val current = mutableState.value
        val sources = if (current.sources.any { it.id == source.id }) current.sources.map { if (it.id == source.id) source else it } else current.sources + source
        mutableState.value = current.copy(sources = sources, activeId = source.id)
        persist()
    }

    fun select(id: String?) {
        require(id == null || mutableState.value.sources.any { it.id == id })
        mutableState.value = mutableState.value.copy(activeId = id)
        persist()
    }

    fun remove(id: String) {
        val current = mutableState.value
        val remaining = current.sources.filterNot { it.id == id }
        mutableState.value = current.copy(sources = remaining, activeId = if (id == current.activeId) remaining.firstOrNull()?.id else current.activeId)
        persist()
    }

    private fun persist() { profileId?.let { write(it, json.encodeToString(mutableState.value)) } }
}

internal val desktopM3uSourceStore = DesktopM3uSourceStore(
    read = { DesktopM3uSourcesStorageBridge.read(it) }, write = { profile, data -> DesktopM3uSourcesStorageBridge.write(profile, data) },
)
