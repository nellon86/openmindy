package com.katoaapps.openminilaunch.ui.mindyspace.widget

import android.appwidget.AppWidgetManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.features.mindyspace.MindySpaceMiniApp
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.launcher.MainActivity
import com.katoaapps.openminilaunch.ui.widgets.WidgetConfigurationConfirmationDialog
import com.katoaapps.openminilaunch.ui.widgets.WidgetPickerPresentation
import com.katoaapps.openminilaunch.ui.widgets.WidgetProviderDialog
import com.katoaapps.openminilaunch.ui.widgets.homeScreenProviders
import com.katoaapps.openminilaunch.ui.widgets.rememberWidgetBindingController

/** Reusable picker and Android bind flow for one Quick Widget slot. */
@Composable
internal fun MindySpaceWidgetSetup(
    slot: MindySpaceMiniApp,
    visible: Boolean,
    store: LauncherStore,
    actions: DeviceActions,
    onDismiss: () -> Unit,
    onComplete: () -> Unit = onDismiss,
) {
    val context = LocalContext.current
    val activity = context as MainActivity
    val manager = remember(context) { AppWidgetManager.getInstance(context) }
    val density = context.resources.displayMetrics.density
    val appRevision by actions.launcherAppsRevision.collectAsState()
    val viewport = rememberMindySpaceWidgetViewport(store.twoPanelModeForLargeDisplays)
    val binding = rememberWidgetBindingController(
        onBound = { newId, _ ->
            val oldId = store.mindySpaceWidgetId(slot)
            store.setMindySpaceWidget(slot, newId)
            store.setMindySpaceMiniAppEnabled(slot, true)
            if (oldId != null && oldId != newId) {
                runCatching { activity.appWidgetHost.deleteAppWidgetId(oldId) }
            }
            onComplete()
        },
        onFailed = { onDismiss() },
    )

    if (visible && !binding.isBusy) {
        val providers = remember(manager, appRevision) {
            manager.homeScreenProviders(context.packageManager)
        }
        WidgetProviderDialog(
            providers = providers,
            actions = actions,
            presentation = WidgetPickerPresentation(
                titleRes = if (store.mindySpaceWidgetId(slot) == null) {
                    R.string.add_widget
                } else {
                    R.string.replace_widget
                },
                descriptionRes = R.string.mindy_space_widget_picker_description,
                badgesFor = { info -> mindySpaceWidgetBadges(info, density, viewport) },
            ),
            onSelect = { info ->
                binding.begin(info, mindySpaceWidgetBindingOptions(viewport))
            },
            onDismiss = onDismiss,
        )
    }

    binding.configurationToConfirm?.let { info ->
        WidgetConfigurationConfirmationDialog(
            info = info,
            onUseWidget = binding::useConfiguredWidget,
            onDiscard = binding::discardConfiguredWidget,
        )
    }
}
