package com.nuvio.app.features.search

import com.nuvio.app.core.storage.DesktopStorage
import com.nuvio.app.core.storage.DesktopPreferences
import com.nuvio.app.core.storage.ProfileScopedKey

actual object DiscoverSelectionStorage {
    private const val preferencesName = "nuvio_discover_selection"
    private const val catalogKey = "discover_catalog_key"

    private val preferences: DesktopPreferences? = DesktopPreferences(preferencesName)



    actual fun loadCatalogKey(): String? =
        preferences?.getString(ProfileScopedKey.of(catalogKey), null)

    actual fun saveCatalogKey(catalogKey: String) {
        preferences
            ?.edit()
            ?.putString(ProfileScopedKey.of(DiscoverSelectionStorage.catalogKey), catalogKey)
            ?.apply()
    }
}
