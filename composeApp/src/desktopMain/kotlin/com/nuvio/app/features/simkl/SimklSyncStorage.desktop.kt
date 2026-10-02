package com.nuvio.app.features.simkl

import com.nuvio.app.core.storage.DesktopStorage
import com.nuvio.app.core.storage.DesktopPreferences
import com.nuvio.app.core.storage.ProfileScopedKey

internal actual object SimklSyncStorage {
    private const val PREFERENCES_NAME = "nuvio_simkl_sync"
    private const val PAYLOAD_KEY = "simkl_sync_snapshot"

    private val preferences: DesktopPreferences? = DesktopPreferences(PREFERENCES_NAME)



    actual fun loadPayload(): String? =
        preferences?.getString(ProfileScopedKey.of(PAYLOAD_KEY), null)

    actual fun savePayload(payload: String) {
        preferences?.edit()?.putString(ProfileScopedKey.of(PAYLOAD_KEY), payload)?.apply()
    }

    actual fun removeProfile(profileId: Int) {
        preferences?.edit()
            ?.remove(ProfileScopedKey.of(PAYLOAD_KEY, profileId))
            ?.apply()
    }
}
