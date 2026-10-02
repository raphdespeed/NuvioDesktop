package com.nuvio.app.features.settings

internal expect object NuvioSpeedySettingsStorage {
    fun loadPayload(): String?
    fun savePayload(payload: String)
}
