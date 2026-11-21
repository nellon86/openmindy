package com.katoaapps.openminilaunch.features.apps

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.katoaapps.openminilaunch.model.launcherAppKeyParts
import org.json.JSONObject

private val com.katoaapps.openminilaunch.model.LauncherAppKeyParts.packageName: String?
    get() = android.content.ComponentName.unflattenFromString(flattenedComponent)?.packageName

/**
 * Names the user gave to apps inside OpenMindy, keyed by launcher selection key (component and
 * profile). The system name stays untouched and remains searchable.
 */
internal object AppCustomLabels {
    const val MAX_LENGTH = 40
    private const val PREFS_NAME = "mini_launch"
    private const val KEY = "app_custom_labels"

    @Volatile private var labels: Map<String, String> = emptyMap()
    private var prefs: SharedPreferences? = null

    @Synchronized
    fun ensureLoaded(context: Context) {
        if (prefs != null) return
        val loaded = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        labels = decode(loaded.getString(KEY, null))
        prefs = loaded
    }

    fun labelFor(selectionKey: String): String? = labels[selectionKey]

    /** The custom name of an app whose only renamed launcher entry belongs to [packageName]. */
    fun labelForPackage(packageName: String): String? =
        labels.entries
            .filter { launcherAppKeyParts(it.key)?.packageName == packageName }
            .map { it.value }
            .distinct()
            .singleOrNull()

    fun all(): Map<String, String> = labels

    /** Blank [name] restores the system name. Returns true when something changed. */
    @Synchronized
    fun set(selectionKey: String, name: String?): Boolean {
        val clean = name?.trim()?.take(MAX_LENGTH)?.takeIf(String::isNotEmpty)
        val next = if (clean == null) labels - selectionKey else labels + (selectionKey to clean)
        if (next == labels) return false
        persist(next)
        return true
    }

    @Synchronized
    fun removePackage(packageName: String, userSerial: Long) {
        val next = labels.filterKeys { key ->
            val parts = launcherAppKeyParts(key) ?: return@filterKeys true
            !(parts.packageName == packageName && parts.userSerial == userSerial)
        }
        if (next != labels) persist(next)
    }

    @Synchronized
    fun mergeFromBackup(restored: Map<String, String>) {
        val next = labels + restored
            .mapValues { it.value.trim().take(MAX_LENGTH) }
            .filterValues(String::isNotEmpty)
        if (next != labels) persist(next)
    }

    private fun persist(next: Map<String, String>) {
        labels = next
        prefs?.edit { putString(KEY, encode(next)) }
    }

    internal fun encode(values: Map<String, String>): String =
        JSONObject().apply { values.forEach { (key, name) -> put(key, name) } }.toString()

    internal fun decode(json: String?): Map<String, String> = runCatching {
        if (json.isNullOrBlank()) return emptyMap()
        val source = JSONObject(json)
        buildMap {
            source.keys().forEach { key ->
                source.optString(key).trim().take(MAX_LENGTH).takeIf(String::isNotEmpty)?.let { put(key, it) }
            }
        }
    }.getOrDefault(emptyMap())
}
