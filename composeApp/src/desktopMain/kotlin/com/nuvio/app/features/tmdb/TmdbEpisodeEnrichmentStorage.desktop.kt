package com.nuvio.app.features.tmdb

import com.nuvio.app.core.storage.DesktopStorage
import com.nuvio.app.core.storage.DesktopPreferences
import com.nuvio.app.core.storage.ProfileScopedKey

internal actual object TmdbEpisodeEnrichmentStorage {
    private const val preferencesName = "nuvio_tmdb_episode_enrichment"
    private const val cacheKeyPrefix = "episode_enrichment_"

    private val preferences: DesktopPreferences? = DesktopPreferences(preferencesName)



    actual fun load(cacheKey: String): String? =
        preferences?.getString(storageKey(cacheKey), null)

    actual fun save(cacheKey: String, payload: String) {
        preferences
            ?.edit()
            ?.putString(storageKey(cacheKey), payload)
            ?.apply()
    }

    private fun storageKey(cacheKey: String): String =
        ProfileScopedKey.of("$cacheKeyPrefix${cacheKey.hashCode()}")
}
