package com.nuvio.app.features.profiles

import com.nuvio.app.core.storage.DesktopStorage
import com.nuvio.app.core.storage.DesktopPreferences

actual object ProfilePinCacheStorage {
    private const val preferencesName = "nuvio_profile_pin_cache"

    private val preferences: DesktopPreferences? = DesktopPreferences(preferencesName)



    actual fun loadPayload(profileIndex: Int): String? =
        preferences?.getString(payloadKey(profileIndex), null)

    actual fun savePayload(profileIndex: Int, payload: String) {
        preferences
            ?.edit()
            ?.putString(payloadKey(profileIndex), payload)
            ?.apply()
    }

    actual fun removePayload(profileIndex: Int) {
        preferences
            ?.edit()
            ?.remove(payloadKey(profileIndex))
            ?.apply()
    }

    private fun payloadKey(profileIndex: Int): String = "profile_pin_cache_$profileIndex"
}