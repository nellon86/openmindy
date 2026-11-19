package com.katoaapps.openminilaunch.ui.settings

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.conversations.ConversationHistoryStore
import com.katoaapps.openminilaunch.features.conversations.ConversationNotificationStore
import com.katoaapps.openminilaunch.features.conversations.ConversationPreferences
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.components.AppIcon
import com.katoaapps.openminilaunch.ui.components.SectionLabel
import com.katoaapps.openminilaunch.ui.components.SettingsRow
import com.katoaapps.openminilaunch.ui.components.SettingsSwitchRow
import com.katoaapps.openminilaunch.ui.theme.Dimens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** History and app selection of the Conversations screen. */
@Composable
internal fun ConversationSettingsSection(actions: DeviceActions) {
    val context = LocalContext.current
    remember(context) { ConversationHistoryStore.ensureLoaded(context) }
    var showAppPicker by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }

    fun applyFilterChange() {
        // Re-read the posted notifications so the list follows the new selection right away.
        ConversationNotificationStore.refreshActiveNotifications(context)
    }

    Column(verticalArrangement = Arrangement.spacedBy(Dimens.dp8)) {
        SectionLabel(stringResource(R.string.conversations))
        SettingsSwitchRow(
            title = stringResource(R.string.conversation_history_keep),
            subtitle = stringResource(R.string.conversation_history_keep_description),
            checked = ConversationPreferences.historyEnabled,
            onCheckedChange = ConversationPreferences::updateHistoryEnabled,
        )
        SettingsRow(
            title = stringResource(R.string.conversation_history_clear),
            subtitle = stringResource(R.string.conversation_history_clear_description),
            icon = Icons.Default.DeleteSweep,
            enabled = ConversationHistoryStore.conversations.isNotEmpty(),
            onClick = { confirmClear = true },
        )
        SettingsSwitchRow(
            title = stringResource(R.string.conversation_apps_only_selected),
            subtitle = stringResource(R.string.conversation_apps_only_selected_description),
            checked = ConversationPreferences.onlySelectedApps,
            onCheckedChange = { enabled ->
                if (enabled && ConversationPreferences.selectedApps.isEmpty()) {
                    // Start from the apps already showing up, so turning this on hides nothing.
                    ConversationPreferences.updateSelectedApps(
                        (ConversationNotificationStore.activeNotifications.map { it.packageName } +
                            ConversationHistoryStore.conversations.values.map { it.packageName }).toSet(),
                    )
                }
                ConversationPreferences.updateOnlySelectedApps(enabled)
                applyFilterChange()
                if (enabled) showAppPicker = true
            },
        )
        SettingsRow(
            title = stringResource(R.string.conversation_apps),
            subtitle = if (ConversationPreferences.onlySelectedApps) {
                stringResource(
                    R.string.conversation_apps_selected_count,
                    ConversationPreferences.selectedApps.size,
                )
            } else {
                stringResource(R.string.conversation_apps_automatic)
            },
            icon = Icons.Default.Apps,
            enabled = ConversationPreferences.onlySelectedApps,
            onClick = { showAppPicker = true },
        )
    }

    if (showAppPicker) {
        ConversationAppsDialog(
            actions = actions,
            initialSelection = ConversationPreferences.selectedApps,
            onSave = { selection ->
                ConversationPreferences.updateSelectedApps(selection)
                applyFilterChange()
                showAppPicker = false
            },
            onDismiss = { showAppPicker = false },
        )
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.conversation_history_clear)) },
            text = { Text(stringResource(R.string.conversation_history_clear_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        ConversationHistoryStore.clearAll()
                        confirmClear = false
                        Toast.makeText(context, R.string.conversation_history_cleared, Toast.LENGTH_SHORT).show()
                    },
                ) { Text(stringResource(R.string.conversation_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text(stringResource(android.R.string.cancel)) }
            },
        )
    }
}

private class PickableApp(val packageName: String, val label: String)

@Composable
private fun ConversationAppsDialog(
    actions: DeviceActions,
    initialSelection: Set<String>,
    onSave: (Set<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    var selection by remember { mutableStateOf(initialSelection) }
    val apps by produceState<List<PickableApp>?>(null) {
        value = withContext(Dispatchers.IO) {
            actions.installedApps()
                .filter { it.isAvailable }
                .distinctBy { it.packageName }
                .map { PickableApp(it.packageName, it.label) }
                // Chosen apps first, then alphabetical, so the current choice is visible at once.
                .sortedWith(
                    compareByDescending<PickableApp> { it.packageName in initialSelection }
                        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.label },
                )
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.conversation_apps)) },
        text = {
            val loaded = apps
            if (loaded == null) {
                CircularProgressIndicator(Modifier.padding(Dimens.dp20))
            } else {
                LazyColumn(Modifier.heightIn(max = Dimens.dp720)) {
                    items(loaded, key = PickableApp::packageName) { app ->
                        val checked = app.packageName in selection
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selection = if (checked) selection - app.packageName else selection + app.packageName
                                }
                                .padding(vertical = Dimens.dp6),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AppIcon(app.packageName, actions = actions, size = Dimens.dp32)
                            Text(
                                app.label,
                                Modifier.weight(1f).padding(horizontal = Dimens.dp12),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Checkbox(checked = checked, onCheckedChange = null)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(selection) }) { Text(stringResource(android.R.string.ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
        },
    )
}
