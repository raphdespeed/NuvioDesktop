package com.nuvio.app.features.home

import com.nuvio.app.core.storage.DesktopStorage
import com.nuvio.app.core.storage.DesktopPreferences
import com.nuvio.app.core.storage.ProfileScopedKey

actual object HomeCatalogSettingsStorage {
    private const val preferencesName = "nuvio_home_catalog_settings"
    private const val payloadKey = "catalog_settings_payload"

    private val preferences: DesktopPreferences? = DesktopPreferences(preferencesName)



    actual fun loadPayload(): String? =
        preferences?.getString(ProfileScopedKey.of(payloadKey), null)

    actual fun savePayload(payload: String) {
        preferences
            ?.edit()
            ?.putString(ProfileScopedKey.of(payloadKey), payload)
            ?.apply()
    }
}
