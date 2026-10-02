package com.nuvio.app.features.collection

import com.nuvio.app.core.storage.DesktopStorage
import com.nuvio.app.core.storage.DesktopPreferences
import com.nuvio.app.core.storage.ProfileScopedKey

actual object CollectionMobileSettingsStorage {
    private const val preferencesName = "nuvio_collection_mobile_settings"
    private const val payloadKey = "collection_mobile_settings_payload"

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
