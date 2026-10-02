package com.nuvio.app.features.anime

import com.nuvio.app.core.storage.DesktopStorage
import com.nuvio.app.core.storage.ProfileScopedKey
import java.util.Base64
import java.util.concurrent.TimeUnit

internal actual object AnimeTrackingAuthStorage {
    private val store=DesktopStorage.store("anime_tracking_auth")
    private fun key(provider: AnimeTrackingProvider,key: String,profile: Int?=null): String =
        if(profile==null) ProfileScopedKey.of("${provider.name.lowercase()}_$key")
        else ProfileScopedKey.of("${provider.name.lowercase()}_$key",profile)
    actual fun loadMetadata(provider: AnimeTrackingProvider): String? = store.getString(key(provider,"metadata"))
    actual fun saveMetadata(provider: AnimeTrackingProvider,payload: String?) { store.putString(key(provider,"metadata"),payload) }
    actual fun loadSettings(): String? = store.getString(ProfileScopedKey.of("anime_tracking_settings"))
    actual fun saveSettings(payload: String?) { store.putString(ProfileScopedKey.of("anime_tracking_settings"),payload) }
    actual fun loadSecret(provider: AnimeTrackingProvider,key: String): String? =
        store.getString(key(provider,key))?.let { WindowsCredentialProtection.decrypt(it) }
    actual fun saveSecret(provider: AnimeTrackingProvider,key: String,value: String?) {
        store.putString(key(provider,key),value?.takeIf { it.isNotBlank() }?.let { WindowsCredentialProtection.encrypt(it) })
    }
    actual fun removeProfile(profileId: Int) {
        store.remove(ProfileScopedKey.of("anime_tracking_settings",profileId))
        AnimeTrackingProvider.entries.forEach { provider ->
            listOf("metadata","access_token","refresh_token","oauth_state","pkce_verifier").forEach { store.remove(key(provider,it,profileId)) }
        }
    }
}

/** DPAPI binds saved OAuth credentials to the current Windows user. Secrets are
 * passed through standard input, never through process arguments or log files. */
private object WindowsCredentialProtection {
    fun encrypt(value: String): String = transform(Base64.getEncoder().encodeToString(value.toByteArray()),true)
    fun decrypt(value: String): String = Base64.getDecoder().decode(transform(value,false)).toString(Charsets.UTF_8)
    private fun transform(value: String,protect: Boolean): String {
        val method=if(protect) "Protect" else "Unprotect"
        val script="Add-Type -AssemblyName System.Security; "+
            "\$b=[Convert]::FromBase64String([Console]::In.ReadToEnd().Trim()); "+
            "[Convert]::ToBase64String([Security.Cryptography.ProtectedData]::$method(\$b,\$null,[Security.Cryptography.DataProtectionScope]::CurrentUser))"
        val process=ProcessBuilder("powershell.exe","-NoProfile","-NonInteractive","-Command",script).start()
        process.outputStream.bufferedWriter().use { it.write(value) }
        check(process.waitFor(30,TimeUnit.SECONDS)) { process.destroyForcibly();"Windows credential protection timed out" }
        check(process.exitValue()==0) { "Windows credential protection failed" }
        return process.inputStream.bufferedReader().readText().trim().also { require(it.isNotBlank()) }
    }
}
