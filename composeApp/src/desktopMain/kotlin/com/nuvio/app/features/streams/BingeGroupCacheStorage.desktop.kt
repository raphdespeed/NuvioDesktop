package com.nuvio.app.features.streams

import com.nuvio.app.core.storage.DesktopStorage
import com.nuvio.app.core.storage.DesktopPreferences
import com.nuvio.app.core.storage.ProfileScopedKey

actual object BingeGroupCacheStorage {
    private const val preferencesName = "nuvio_binge_group_cache"

    private val preferences: DesktopPreferences? = DesktopPreferences(preferencesName)



    actual fun load(hashedKey: String): String? =
        preferences?.getString(ProfileScopedKey.of(hashedKey), null)

    actual fun save(hashedKey: String, value: String) {
        preferences
            ?.edit()
            ?.putString(ProfileScopedKey.of(hashedKey), value)
            ?.apply()
    }

    actual fun remove(hashedKey: String) {
        preferences
            ?.edit()
            ?.remove(ProfileScopedKey.of(hashedKey))
            ?.apply()
    }
}
