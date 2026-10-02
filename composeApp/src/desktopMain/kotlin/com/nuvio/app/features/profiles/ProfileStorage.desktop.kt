package com.nuvio.app.features.profiles

import com.nuvio.app.core.storage.DesktopStorage
import com.nuvio.app.core.storage.DesktopPreferences

actual object ProfileStorage {
    private const val preferencesName = "nuvio_profile_cache"
    private const val payloadKey = "profile_payload"

    private val preferences: DesktopPreferences? = DesktopPreferences(preferencesName)



    actual fun loadPayload(): String? =
        preferences?.getString(payloadKey, null)

    actual fun savePayload(payload: String) {
        preferences
            ?.edit()
            ?.putString(payloadKey, payload)
            ?.apply()
    }
}