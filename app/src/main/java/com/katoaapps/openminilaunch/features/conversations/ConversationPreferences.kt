package com.katoaapps.openminilaunch.features.conversations

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit

/**
 * Which apps feed Conversations and whether their history is kept.
 *
 * Shared by the notification listener (any thread) and the UI, so values live in snapshot state
 * that both can read and that recomposes the conversation list when they change.
 */
internal object ConversationPreferences {
    private var prefs: SharedPreferences? = null

    /** False: every app that posts message notifications. True: only [selectedApps]. */
    var onlySelectedApps by mutableStateOf(false)
        private set
    var selectedApps by mutableStateOf<Set<String>>(emptySet())
        private set
    var historyEnabled by mutableStateOf(true)
        private set

    @Synchronized
    fun ensureLoaded(context: Context) {
        if (prefs != null) return
        val loaded = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        onlySelectedApps = loaded.getBoolean(KEY_ONLY_SELECTED, false)
        selectedApps = loaded.getStringSet(KEY_SELECTED_APPS, null).orEmpty().toSet()
        historyEnabled = loaded.getBoolean(KEY_HISTORY_ENABLED, true)
        prefs = loaded
    }

    /** True when notifications from [packageName] may appear in Conversations. */
    fun allows(packageName: String): Boolean = !onlySelectedApps || packageName in selectedApps

    /**
     * True when the user picked [packageName] explicitly, so its notifications count as messages
     * even when the app does not mark them as such.
     */
    fun forces(packageName: String): Boolean = onlySelectedApps && packageName in selectedApps

    fun updateOnlySelectedApps(enabled: Boolean) {
        onlySelectedApps = enabled
        prefs?.edit { putBoolean(KEY_ONLY_SELECTED, enabled) }
    }

    fun updateSelectedApps(packages: Set<String>) {
        selectedApps = packages.toSet()
        prefs?.edit { putStringSet(KEY_SELECTED_APPS, selectedApps) }
    }

    fun updateHistoryEnabled(enabled: Boolean) {
        historyEnabled = enabled
        prefs?.edit { putBoolean(KEY_HISTORY_ENABLED, enabled) }
    }

    private const val PREFS_NAME = "mini_launch"
    private const val KEY_ONLY_SELECTED = "conversation_only_selected_apps"
    private const val KEY_SELECTED_APPS = "conversation_selected_apps"
    private const val KEY_HISTORY_ENABLED = "conversation_history_enabled"
}
