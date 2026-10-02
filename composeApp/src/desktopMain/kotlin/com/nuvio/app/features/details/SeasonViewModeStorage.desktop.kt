package com.nuvio.app.features.details

import com.nuvio.app.core.storage.DesktopStorage
import com.nuvio.app.core.storage.DesktopPreferences
import com.nuvio.app.core.storage.ProfileScopedKey

actual object SeasonViewModeStorage {
    private const val preferencesName = "nuvio_season_view_mode"
    private const val key = "season_view_mode"

    private val preferences: DesktopPreferences? = DesktopPreferences(preferencesName)



    actual fun load(): SeasonViewMode? =
        preferences?.getString(ProfileScopedKey.of(key), null)?.let(SeasonViewMode::parse)

    actual fun save(mode: SeasonViewMode) {
        preferences
            ?.edit()
            ?.putString(ProfileScopedKey.of(key), SeasonViewMode.persist(mode))
            ?.apply()
    }
}
