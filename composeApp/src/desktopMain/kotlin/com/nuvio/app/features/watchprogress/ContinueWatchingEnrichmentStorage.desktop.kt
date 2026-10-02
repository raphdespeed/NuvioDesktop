package com.nuvio.app.features.watchprogress

import com.nuvio.app.core.storage.DesktopStorage
import com.nuvio.app.core.storage.DesktopPreferences

actual object ContinueWatchingEnrichmentStorage {
    private const val preferencesName = "nuvio_cw_enrichment"

    private val preferences: DesktopPreferences? = DesktopPreferences(preferencesName)



    actual fun loadPayload(key: String): String? =
        preferences?.getString(key, null)

    actual fun savePayload(key: String, payload: String) {
        preferences
            ?.edit()
            ?.putString(key, payload)
            ?.apply()
    }

    actual fun removePayload(key: String) {
        preferences
            ?.edit()
            ?.remove(key)
            ?.apply()
    }
}
