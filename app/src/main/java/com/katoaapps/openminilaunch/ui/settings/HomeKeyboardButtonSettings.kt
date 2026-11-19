package com.katoaapps.openminilaunch.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.features.magic.HomeKeyboardButtonAction
import com.katoaapps.openminilaunch.features.magic.HomeKeyboardButtonBinding
import com.katoaapps.openminilaunch.features.magic.HomeKeyboardButtonGesture
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.components.SectionLabel
import com.katoaapps.openminilaunch.ui.components.SettingsDropdownOption
import com.katoaapps.openminilaunch.ui.components.SettingsDropdownRow
import com.katoaapps.openminilaunch.ui.magic.label
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Tap, long-press and swipe-up actions of the round button on the Home search bar. */
@Composable
internal fun HomeKeyboardButtonSettingsSection(store: LauncherStore, actions: DeviceActions) {
    var appPickerGesture by remember { mutableStateOf<HomeKeyboardButtonGesture?>(null) }
    val bindings = store.homeKeyboardButtonBindings

    Column(verticalArrangement = Arrangement.spacedBy(Dimens.dp8)) {
        SectionLabel(stringResource(R.string.home_button))
        Text(
            stringResource(R.string.home_button_summary),
            color = Muted,
            style = MaterialTheme.typography.bodySmall,
        )
        HomeKeyboardButtonGesture.entries.forEach { gesture ->
            val binding = bindings[gesture]
            val appLabel = binding.appTarget
                ?.takeIf { binding.action == HomeKeyboardButtonAction.LAUNCH_APP }
                ?.let(actions::launcherTargetLabel)
            SettingsDropdownRow(
                title = stringResource(gesture.titleRes),
                selectedValue = binding.action,
                options = HomeKeyboardButtonAction.entries
                    // A tap must always do something.
                    .filterNot { gesture == HomeKeyboardButtonGesture.TAP && it == HomeKeyboardButtonAction.NONE }
                    .map { action ->
                        val label = if (action == HomeKeyboardButtonAction.LAUNCH_APP && appLabel != null) {
                            stringResource(R.string.home_button_action_launch_app_named, appLabel)
                        } else {
                            action.label()
                        }
                        SettingsDropdownOption(action, label)
                    },
                onValueSelected = { action ->
                    if (action == HomeKeyboardButtonAction.LAUNCH_APP) {
                        appPickerGesture = gesture
                    } else {
                        store.updateHomeKeyboardButton(gesture, HomeKeyboardButtonBinding(action))
                    }
                },
            )
        }
    }

    appPickerGesture?.let { gesture ->
        HomeKeyboardButtonAppPicker(
            store = store,
            actions = actions,
            gesture = gesture,
            onDismiss = { appPickerGesture = null },
        )
    }
}

@Composable
private fun HomeKeyboardButtonAppPicker(
    store: LauncherStore,
    actions: DeviceActions,
    gesture: HomeKeyboardButtonGesture,
    onDismiss: () -> Unit,
) {
    val appsRevision by actions.launcherAppsRevision.collectAsState()
    val shortcutsRevision by actions.launcherShortcutsRevision.collectAsState()
    val apps by produceState(LauncherAppListLoadState(), appsRevision) {
        value = LauncherAppListLoadState(
            apps = withContext(Dispatchers.IO) { actions.installedApps() },
            loaded = true,
        )
    }
    val shortcuts by produceState(LauncherShortcutListLoadState(), shortcutsRevision) {
        value = LauncherShortcutListLoadState(
            shortcuts = withContext(Dispatchers.IO) { actions.installedShortcuts() },
            loaded = true,
        )
    }
    val current = store.homeKeyboardButtonBindings[gesture]
    LauncherTargetPickerDialog(
        title = stringResource(R.string.home_button_action_launch_app),
        apps = apps.apps,
        shortcuts = shortcuts.shortcuts,
        selected = setOfNotNull(
            current.appTarget.takeIf { current.action == HomeKeyboardButtonAction.LAUNCH_APP },
        ),
        actions = actions,
        appsLoading = !apps.loaded,
        shortcutsLoading = !shortcuts.loaded,
        onTarget = { target ->
            store.updateHomeKeyboardButton(
                gesture,
                HomeKeyboardButtonBinding(HomeKeyboardButtonAction.LAUNCH_APP, target.selectionKey),
            )
            onDismiss()
        },
        onDismiss = onDismiss,
    )
}

private val HomeKeyboardButtonGesture.titleRes: Int
    get() = when (this) {
        HomeKeyboardButtonGesture.TAP -> R.string.home_button_tap
        HomeKeyboardButtonGesture.LONG_PRESS -> R.string.home_button_long_press
        HomeKeyboardButtonGesture.SWIPE_UP -> R.string.home_button_swipe_up
    }
