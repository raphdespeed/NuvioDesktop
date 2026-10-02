package com.nuvio.app.features.cloudstream

import com.nuvio.app.core.storage.DesktopStorage
import java.nio.file.Files
import java.nio.file.StandardCopyOption

internal actual object CloudStreamPlatformRuntime {
    // Android DEX packages cannot run on the JVM desktop. Reviewed portable
    // providers are selected by the shared CloudStream provider registry.
    actual val supportsAndroidDex = false
    actual fun initialize(context: Any?) = Unit
    actual suspend fun provider(plugin: CloudStreamPluginItem): CloudStreamProvider? = null
    actual fun unload(pluginId: String) = Unit
    actual fun clear() = Unit
}

internal actual object CloudStreamPlatformStorage {
    private val store = DesktopStorage.store("cloudstream")
    private val root = DesktopStorage.rootDir.resolve("cloudstream/packages")
    private var profile = 1
    private fun file(key: String): java.nio.file.Path {
        require(key.isNotBlank() && key.none { it == '/' || it == '\\' } && key != "..")
        val directory = root.resolve("profile-$profile")
        Files.createDirectories(directory)
        return directory.resolve("$key.cs3")
    }
    actual fun initialize(context: Any?) { CloudStreamPlatformRuntime.initialize(context) }
    actual fun setActiveProfile(profileId: Int) { profile = profileId.coerceAtLeast(1) }
    actual fun loadState(profileId: Int): String? = store.getString("state_$profileId")
    actual fun saveState(profileId: Int, payload: String) { store.putString("state_$profileId", payload) }
    actual fun savePackageAtomically(storageKey: String, bytes: ByteArray) {
        val destination = file(storageKey)
        val temporary = Files.createTempFile(destination.parent,"package-", ".tmp")
        try {
            Files.write(temporary, bytes)
            try { Files.move(temporary,destination,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING) }
            catch (_: java.nio.file.AtomicMoveNotSupportedException) {
                Files.move(temporary,destination,StandardCopyOption.REPLACE_EXISTING)
            }
        } finally { Files.deleteIfExists(temporary) }
    }
    actual fun packageExists(storageKey: String) = Files.isRegularFile(file(storageKey))
    actual fun migratePackage(oldStorageKey: String, newStorageKey: String): Boolean = runCatching {
        if (packageExists(newStorageKey)) true
        else { Files.move(file(oldStorageKey),file(newStorageKey));true }
    }.getOrDefault(false)
    actual fun packagePath(storageKey: String): String? = file(storageKey).takeIf { Files.isRegularFile(it) }?.toString()
    actual fun deletePackage(storageKey: String) { Files.deleteIfExists(file(storageKey)) }
    actual fun clearPackages() {
        val directory=file("clear").parent
        Files.list(directory).use { files -> files.filter { Files.isRegularFile(it) }.forEach { Files.deleteIfExists(it) } }
    }
    actual fun clearAllState() { store.putString("state_$profile",null);clearPackages() }
}
