package com.nuvio.app.features.tmdb

import com.nuvio.app.core.storage.DesktopStorage
import com.nuvio.app.core.storage.DesktopPreferences
import com.nuvio.app.core.storage.ProfileScopedKey

internal actual object TmdbPreviewArtworkStorage {
    private const val preferencesName = "nuvio_tmdb_preview_artwork"
    private const val cacheKeyPrefix = "preview_artwork_"

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
