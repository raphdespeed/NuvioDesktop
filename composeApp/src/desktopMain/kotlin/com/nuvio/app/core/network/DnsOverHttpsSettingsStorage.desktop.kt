package com.nuvio.app.core.network

import com.nuvio.app.core.storage.DesktopStorage
import com.nuvio.app.core.storage.DesktopPreferences

actual object DnsOverHttpsSettingsStorage {
    private const val preferencesName = "nuvio_dns_over_https_settings"
    private const val providerKey = "dns_over_https_provider"

    private val preferences: DesktopPreferences? = DesktopPreferences(preferencesName)



    actual fun loadProviderId(): String? =
        preferences?.getString(providerKey, null)

    actual fun saveProviderId(providerId: String) {
        preferences
            ?.edit()
            ?.putString(providerKey, providerId)
            ?.apply()
    }
}
