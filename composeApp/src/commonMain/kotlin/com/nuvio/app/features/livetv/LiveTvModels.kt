package com.nuvio.app.features.livetv

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

data class LiveTvChannel(
    val id: String,
    val name: String,
    val streamUrl: String,
    val tvgId: String? = null,
    val logoUrl: String? = null,
    val group: String = "",
    val headers: Map<String, String> = emptyMap(),
    val streamType: String? = null,
    val stalkerCommand: String? = null,
)

data class LiveTvRecentChannel(
    val streamUrl: String,
    val name: String,
    val logoUrl: String? = null,
    val group: String = "",
    val tvgId: String? = null,
)

data class LiveTvProgramme(
    val title: String,
    val startEpochMs: Long,
    val stopEpochMs: Long,
    val timeLabel: String,
    val subtitle: String? = null,
    val description: String? = null,
    val categories: List<String> = emptyList(),
    val episode: String? = null,
    val iconUrl: String? = null,
    val rating: String? = null,
    val credits: List<LiveTvProgrammeCredit> = emptyList(),
    val date: String? = null,
    val country: String? = null,
    val language: String? = null,
    val isNew: Boolean = false,
    val isPremiere: Boolean = false,
    val isPreviouslyShown: Boolean = false,
)

data class LiveTvProgrammeCredit(
    val role: String,
    val name: String,
)

data class LiveTvEpgCacheEntry(
    val url: String,
    val content: String,
    val savedAtEpochMs: Long,
)

data class LiveTvUiState(
    val sourceType: LiveTvSourceType = LiveTvSourceType.M3u,
    val sourceUrl: String = "",
    val stalkerSettings: LiveTvStalkerSettings = LiveTvStalkerSettings(),
    val xtreamSettings: LiveTvXtreamSettings = LiveTvXtreamSettings(),
    val channels: List<LiveTvChannel> = emptyList(),
    val currentProgrammes: Map<String, LiveTvProgramme> = emptyMap(),
    val programmesByChannel: Map<String, List<LiveTvProgramme>> = emptyMap(),
    val recentChannel: LiveTvRecentChannel? = null,
    val favoriteUrls: Set<String> = emptySet(),
    val isEpgLoading: Boolean = false,
    val isLoading: Boolean = false,
    val isLoaded: Boolean = false,
    val errorMessage: String? = null,
)

enum class LiveTvSourceType {
    M3u,
    Stalker,
    Xtream,
}

data class LiveTvStalkerSettings(
    val portalUrl: String = "",
    val macAddress: String = "",
    val username: String = "",
    val password: String = "",
) {
    val isConfigured: Boolean
        get() = portalUrl.isNotBlank() && macAddress.isNotBlank()
}

data class LiveTvXtreamSettings(
    val serverUrl: String = "",
    val username: String = "",
    val password: String = "",
) {
    val isConfigured: Boolean
        get() = serverUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank()
}

