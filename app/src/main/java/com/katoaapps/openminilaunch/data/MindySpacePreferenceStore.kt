package com.katoaapps.openminilaunch.data

import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import com.katoaapps.openminilaunch.features.mindyspace.MindySpaceMiniApp

/** Owns MindySpace ordering and visibility independently of its UI host. */
internal class MindySpacePreferenceStore(private val prefs: SharedPreferences) {
    var order by mutableStateOf(loadOrder())
        private set

    var enabledMiniApps by mutableStateOf(loadEnabledMiniApps())
        private set

    var totalScreenShortcut by mutableStateOf(loadTotalScreenShortcut())
        private set

    fun setEnabled(miniApp: MindySpaceMiniApp, enabled: Boolean) {
        enabledMiniApps = if (enabled) {
            enabledMiniApps + miniApp
        } else {
            enabledMiniApps - miniApp
        }
        saveEnabledMiniApps()
    }

    fun updateOrder(visibleOrder: List<MindySpaceMiniApp>) {
        val updatedOrder = mergeVisibleMindySpaceMiniAppOrder(order, visibleOrder)
        if (updatedOrder == order) return

        order = updatedOrder
        saveOrder()
    }

    fun updateTotalScreenShortcut(miniApp: MindySpaceMiniApp?) {
        totalScreenShortcut = miniApp
        prefs.edit { putString(KEY_TOTAL_SCREEN_SHORTCUT, miniApp?.stableId ?: TOTAL_SCREEN_OFF) }
    }

    fun replaceFromBackup(
        restoredOrder: List<MindySpaceMiniApp>,
        restoredEnabledMiniApps: Set<MindySpaceMiniApp>,
    ) {
        // Android widget bindings belong to this installation. Portable backups do not
        // contain the AppWidget ID or its provider configuration, so importing one must
        // not hide or reposition the widget that is already configured on this device.
        order = mergePortableMindySpaceMiniAppOrder(order, restoredOrder)
        enabledMiniApps = mergePortableMindySpaceEnabledMiniApps(
            currentEnabledMiniApps = enabledMiniApps,
            restoredEnabledMiniApps = restoredEnabledMiniApps,
        )

        prefs.edit {
            putString(KEY_ORDER, order.joinToString(",") { it.stableId })
            putStringSet(KEY_ENABLED, enabledMiniApps.mapTo(mutableSetOf()) { it.stableId })
        }
    }

    private fun loadOrder(): List<MindySpaceMiniApp> {
        val saved = prefs.getString(KEY_ORDER, null)
            ?.split(',')
            .orEmpty()
            .mapNotNull(MindySpaceMiniApp::fromStableId)
            .distinct()
        return saved + MindySpaceMiniApp.defaultOrder.filterNot(saved::contains)
    }

    private fun loadEnabledMiniApps(): Set<MindySpaceMiniApp> =
        enabledMindySpaceMiniAppsFromStableIds(prefs.getStringSet(KEY_ENABLED, null))

    private fun loadTotalScreenShortcut(): MindySpaceMiniApp? {
        if (!prefs.contains(KEY_TOTAL_SCREEN_SHORTCUT)) return null
        return prefs.getString(KEY_TOTAL_SCREEN_SHORTCUT, null)
            ?.takeUnless { it == TOTAL_SCREEN_OFF }
            ?.let(MindySpaceMiniApp::fromStableId)
    }

    private fun saveEnabledMiniApps() {
        prefs.edit {
            putStringSet(KEY_ENABLED, enabledMiniApps.mapTo(mutableSetOf()) { it.stableId })
        }
    }

    private fun saveOrder() {
        prefs.edit { putString(KEY_ORDER, order.joinToString(",") { it.stableId }) }
    }

    private companion object {
        const val KEY_ORDER = "mink_space_mini_app_order"
        const val KEY_ENABLED = "mink_space_enabled_mini_apps"
        const val KEY_TOTAL_SCREEN_SHORTCUT = "mink_space_total_screen_shortcut"
        const val TOTAL_SCREEN_OFF = "off"
    }
}

internal fun mergeVisibleMindySpaceMiniAppOrder(
    currentOrder: List<MindySpaceMiniApp>,
    requestedVisibleOrder: List<MindySpaceMiniApp>,
): List<MindySpaceMiniApp> {
    val completeCurrentOrder = currentOrder.distinct() +
        MindySpaceMiniApp.entries.filterNot(currentOrder::contains)
    val requestedReadyOrder = requestedVisibleOrder
        .filter(MindySpaceMiniApp::readyForHome)
        .distinct()
    val completeReadyOrder = requestedReadyOrder + MindySpaceMiniApp.entries.filter { miniApp ->
        miniApp.readyForHome && miniApp !in requestedReadyOrder
    }
    val readyIterator = completeReadyOrder.iterator()
    return completeCurrentOrder.map { miniApp ->
        if (miniApp.readyForHome) readyIterator.next() else miniApp
    }
}

/**
 * Restores portable MindySpace ordering without changing installation-local mini-apps.
 *
 * Quick Widget mini-apps own Android AppWidget bindings that cannot travel in a backup.
 * It therefore keeps both its current position and its current visibility on import.
 */
internal fun mergePortableMindySpaceMiniAppOrder(
    currentOrder: List<MindySpaceMiniApp>,
    restoredOrder: List<MindySpaceMiniApp>,
): List<MindySpaceMiniApp> {
    val completeCurrentOrder = currentOrder.distinct() +
        MindySpaceMiniApp.entries.filterNot(currentOrder::contains)
    val requestedPortableOrder = restoredOrder
        .filter { it.readyForHome && !it.isQuickWidget }
        .distinct()
    val completePortableOrder = requestedPortableOrder + MindySpaceMiniApp.entries.filter { miniApp ->
        miniApp.readyForHome &&
            !miniApp.isQuickWidget &&
            miniApp !in requestedPortableOrder
    }
    val portableIterator = completePortableOrder.iterator()

    return completeCurrentOrder.map { miniApp ->
        if (miniApp.readyForHome && !miniApp.isQuickWidget) {
            portableIterator.next()
        } else {
            miniApp
        }
    }
}

internal fun mergePortableMindySpaceEnabledMiniApps(
    currentEnabledMiniApps: Set<MindySpaceMiniApp>,
    restoredEnabledMiniApps: Set<MindySpaceMiniApp>,
): Set<MindySpaceMiniApp> = buildSet {
    addAll(
        restoredEnabledMiniApps.filter { miniApp ->
            miniApp.readyForHome && !miniApp.isQuickWidget
        },
    )
    addAll(currentEnabledMiniApps.filter(MindySpaceMiniApp::isQuickWidget))
}

internal fun enabledMindySpaceMiniAppsFromStableIds(
    savedStableIds: Set<String>?,
): Set<MindySpaceMiniApp> = savedStableIds
    ?.mapNotNull(MindySpaceMiniApp::fromStableId)
    ?.filterTo(mutableSetOf(), MindySpaceMiniApp::readyForHome)
    ?: MindySpaceMiniApp.defaultEnabled
