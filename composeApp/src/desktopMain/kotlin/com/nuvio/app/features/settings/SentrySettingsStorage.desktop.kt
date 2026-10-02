package com.nuvio.app.features.settings

import com.nuvio.app.core.storage.DesktopStorage
import com.nuvio.app.core.storage.DesktopPreferences

internal actual object SentrySettingsPlatform {
    actual val crashReportsSupported: Boolean = true
}

internal actual object SentrySettingsStorage {
    private const val preferencesName = "nuvio_sentry_settings"
    private const val enabledKey = "enabled"

    private val preferences: DesktopPreferences? = DesktopPreferences(preferencesName)



    actual fun loadEnabled(): Boolean? =
        preferences?.let { prefs ->
            if (prefs.contains(enabledKey)) prefs.getBoolean(enabledKey, true) else null
        }

    actual fun saveEnabled(enabled: Boolean) {
        preferences
            ?.edit()
            ?.putBoolean(enabledKey, enabled)
            ?.apply()
    }
}
