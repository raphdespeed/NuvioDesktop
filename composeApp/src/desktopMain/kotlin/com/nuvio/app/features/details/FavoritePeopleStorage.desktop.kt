package com.nuvio.app.features.details

import com.nuvio.app.core.storage.DesktopStorage
import com.nuvio.app.core.storage.DesktopPreferences

internal actual object FavoritePeopleStorage {
    private const val preferencesName = "nuvio_favorite_people"
    private fun payloadKey(profileId: Int) = "favorite_people_payload_$profileId"

    private val preferences: DesktopPreferences? = DesktopPreferences(preferencesName)



    actual fun loadPayload(profileId: Int): String? =
        preferences?.getString(payloadKey(profileId), null)

    actual fun savePayload(profileId: Int, payload: String) {
        preferences
            ?.edit()
            ?.putString(payloadKey(profileId), payload)
            ?.apply()
    }
}

internal actual fun currentTimeMillis(): Long = System.currentTimeMillis()
