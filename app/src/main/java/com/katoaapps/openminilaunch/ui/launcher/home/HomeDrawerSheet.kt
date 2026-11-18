package com.katoaapps.openminilaunch.ui.launcher.home


import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.features.wellbeing.MinkAppAccessState
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.quickdrawer.QuickAppsFolderDialog
import com.katoaapps.openminilaunch.ui.quickdrawer.QuickDrawerDestination
import com.katoaapps.openminilaunch.ui.quickdrawer.quickDrawerDestinations
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.readableContentColor

/**
 * The Quick Drawer opens straight into the Quick Apps folder. Navigation sits below the apps:
 * All Apps on the left, the right cell intentionally empty for now, and Private or Work Space
 * on an extra row only when the user enabled them and they exist on this device.
 */
@Composable
internal fun HomeDrawerSheet(
    store: LauncherStore,
    actions: DeviceActions,
    appAccessState: MinkAppAccessState,
    onDismiss: () -> Unit,
    onManageQuickApps: () -> Unit,
    onPausedApp: (String) -> Unit,
) {
    val context = LocalContext.current
    val demoModeEnabled = store.demoModeEnabled
    val showDemoPreview = {
        Toast.makeText(context, R.string.demo_preview_action, Toast.LENGTH_SHORT).show()
    }
    val drawerColor = store.quickDrawerColorArgb?.let(::Color) ?: MaterialTheme.colorScheme.background
    val drawerContentColor = readableContentColor(drawerColor)
    val destinations = quickDrawerDestinations(
        showPrivateSpace = store.showPrivateSpaceInQuickDrawer,
        privateSpaceAvailable = store.demoModeEnabled ||
            actions.privateContainerEntryPointAvailable(store.privateContainerGatewaySelectionKey),
        showWorkSpace = store.showWorkProfileInQuickDrawer,
        workSpaceAvailable = store.demoModeEnabled || actions.workProfileAvailable(),
    )
    val openDestination: (QuickDrawerDestination) -> Unit = { destination ->
        if (!demoModeEnabled) {
            onDismiss()
            when (destination) {
                QuickDrawerDestination.ALL_APPS -> actions.openAllApps()
                QuickDrawerDestination.PRIVATE_SPACE -> actions.openPrivateApps()
                QuickDrawerDestination.WORK_SPACE -> actions.openWorkApps()
            }
        } else {
            showDemoPreview()
        }
    }

    QuickAppsFolderDialog(
        store = store,
        actions = actions,
        appAccessState = appAccessState,
        onEdit = {
            onDismiss()
            onManageQuickApps()
        },
        onLaunch = onDismiss,
        onPausedApp = { packageName ->
            onDismiss()
            onPausedApp(packageName)
        },
        onDismiss = onDismiss,
        footer = {
            QuickDrawerFooter(
                destinations = destinations,
                contentColor = drawerContentColor,
                onDestination = openDestination,
            )
        },
    )
}

@Composable
private fun QuickDrawerFooter(
    destinations: List<QuickDrawerDestination>,
    contentColor: Color,
    onDestination: (QuickDrawerDestination) -> Unit,
) {
    val spaces = destinations.filterNot { it == QuickDrawerDestination.ALL_APPS }
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = Dimens.dp8),
        verticalArrangement = Arrangement.spacedBy(Dimens.dp8),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.dp8),
        ) {
            QuickDrawerNavigationButton(
                destination = QuickDrawerDestination.ALL_APPS,
                contentColor = contentColor,
                onClick = { onDestination(QuickDrawerDestination.ALL_APPS) },
                modifier = Modifier.weight(1f),
            )
            // Reserved for a future shortcut; intentionally empty.
            Spacer(Modifier.weight(1f))
        }
        spaces.chunked(2).forEach { rowDestinations ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.dp8),
            ) {
                rowDestinations.forEach { destination ->
                    QuickDrawerNavigationButton(
                        destination = destination,
                        contentColor = contentColor,
                        onClick = { onDestination(destination) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (rowDestinations.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun QuickDrawerNavigationButton(
    destination: QuickDrawerDestination,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(Dimens.dp84),
        shape = RoundedCornerShape(Dimens.dp18),
        color = contentColor.copy(alpha = .09f),
        contentColor = contentColor,
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(Dimens.dp12),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            when (destination) {
                QuickDrawerDestination.ALL_APPS -> Icon(Icons.Default.Apps, null)
                QuickDrawerDestination.PRIVATE_SPACE -> Icon(Icons.Default.Shield, null)
                QuickDrawerDestination.WORK_SPACE -> Icon(Icons.Default.BusinessCenter, null)
            }
            Spacer(Modifier.height(Dimens.dp6))
            Text(
                text = when (destination) {
                    QuickDrawerDestination.ALL_APPS -> stringResource(R.string.all_apps)
                    QuickDrawerDestination.PRIVATE_SPACE -> stringResource(R.string.private_space)
                    QuickDrawerDestination.WORK_SPACE -> stringResource(R.string.work_space)
                },
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
