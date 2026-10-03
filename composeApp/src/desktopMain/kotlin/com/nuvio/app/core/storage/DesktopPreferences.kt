package com.nuvio.app.core.storage

/** Shared preference semantics for the portable Speedy settings adapters. */
internal class DesktopPreferences(name: String) {
    private val store = DesktopStorage.store(name)
    fun contains(key: String) = store.contains(key)
    fun getString(key: String, fallback: String?) = store.getString(key) ?: fallback
    fun getBoolean(key: String, fallback: Boolean) = store.getBoolean(key) ?: fallback
    fun getInt(key: String, fallback: Int) = store.getInt(key) ?: fallback
    fun getFloat(key: String, fallback: Float) = store.getFloat(key) ?: fallback
    fun getLong(key: String, fallback: Long) = store.getString(key)?.toLongOrNull() ?: fallback
    fun getStringSet(key: String, fallback: Set<String>?) = store.getStringSet(key) ?: fallback
    fun edit() = Editor()
    inner class Editor {
        private val changes = mutableListOf<() -> Unit>()
        fun putString(key: String, value: String?) = apply { changes += { store.putString(key, value) } }
        fun putBoolean(key: String, value: Boolean) = apply { changes += { store.putBoolean(key, value) } }
        fun putInt(key: String, value: Int) = apply { changes += { store.putInt(key, value) } }
        fun putFloat(key: String, value: Float) = apply { changes += { store.putFloat(key, value) } }
        fun putLong(key: String, value: Long) = apply { changes += { store.putString(key, value.toString()) } }
        fun putStringSet(key: String, value: Set<String>) = apply { changes += { store.putStringSet(key, value) } }
        fun remove(key: String) = apply { changes += { store.remove(key) } }
        fun apply() { changes.forEach { it() }; changes.clear() }
        fun commit(): Boolean { apply(); return true }
    }
}
