package com.nuvio.app.core.sync

import com.nuvio.app.core.storage.DesktopStorage
import com.nuvio.app.core.storage.DesktopPreferences

actual object SyncClientIdentityStorage {
    private const val preferencesName = "nuvio_sync_client_identity"
    private const val clientIdKey = "client_instance_id"

    private val preferences: DesktopPreferences? = DesktopPreferences(preferencesName)



    actual fun loadClientId(): String? =
        preferences?.getString(clientIdKey, null)

    actual fun saveClientId(clientId: String) {
        preferences
            ?.edit()
            ?.putString(clientIdKey, clientId)
            ?.apply()
    }
}
