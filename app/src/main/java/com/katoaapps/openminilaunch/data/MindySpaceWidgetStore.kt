package com.katoaapps.openminilaunch.data

import android.appwidget.AppWidgetManager
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateMapOf
import androidx.core.content.edit
import com.katoaapps.openminilaunch.features.mindyspace.MindySpaceMiniApp

/**
 * Local Android widget bindings for the Quick Widget mini-apps, one per slot.
 *
 * Slot 1 keeps the upstream preference key, so a widget placed before OpenMindy added more slots
 * stays where it was.
 */
internal class MindySpaceWidgetStore(private val prefs: SharedPreferences) {
    private val appWidgetIds = mutableStateMapOf<MindySpaceMiniApp, Int>().apply {
        MindySpaceMiniApp.quickWidgets.forEach { slot ->
            loadAppWidgetId(slot)?.let { put(slot, it) }
        }
    }

    fun appWidgetId(slot: MindySpaceMiniApp): Int? = appWidgetIds[slot]

    fun replace(slot: MindySpaceMiniApp, appWidgetId: Int) {
        require(slot.isQuickWidget) { "${slot.stableId} is not a Quick Widget slot" }
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return
        appWidgetIds[slot] = appWidgetId
        prefs.edit { putInt(keyFor(slot), appWidgetId) }
    }

    fun clear(slot: MindySpaceMiniApp) {
        appWidgetIds.remove(slot)
        prefs.edit { remove(keyFor(slot)) }
    }

    private fun loadAppWidgetId(slot: MindySpaceMiniApp): Int? = prefs
        .getInt(keyFor(slot), AppWidgetManager.INVALID_APPWIDGET_ID)
        .takeIf { it != AppWidgetManager.INVALID_APPWIDGET_ID }

    private companion object {
        const val KEY_APP_WIDGET_ID = "mink_space_app_widget_id"

        fun keyFor(slot: MindySpaceMiniApp): String = when (slot) {
            MindySpaceMiniApp.WIDGET -> KEY_APP_WIDGET_ID
            else -> "${KEY_APP_WIDGET_ID}_${slot.stableId}"
        }
    }
}
