package com.nuvio.app.features.watchprogress

import com.nuvio.app.core.storage.DesktopStorage
import com.nuvio.app.core.storage.DesktopPreferences

actual object WatchProgressStorage {
    private const val preferencesName = "nuvio_watch_progress"
    private const val payloadKey = "watch_progress_payload"

    private val preferences: DesktopPreferences? = DesktopPreferences(preferencesName)



    actual fun loadPayload(profileId: Int): String? =
        preferences?.getString("${payloadKey}_$profileId", null)

    actual fun savePayload(profileId: Int, payload: String) {
        preferences
            ?.edit()
            ?.putString("${payloadKey}_$profileId", payload)
            ?.apply()
    }
}
