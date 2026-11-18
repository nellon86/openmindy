package com.katoaapps.openminilaunch.ui.settings

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.zIndex
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.features.mindyspace.MindySpaceMiniApp
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.components.SectionLabel
import com.katoaapps.openminilaunch.ui.components.SettingsDropdownOption
import com.katoaapps.openminilaunch.ui.components.SettingsDropdownRow
import com.katoaapps.openminilaunch.ui.components.moveItem
import com.katoaapps.openminilaunch.ui.mindyspace.core.MindySpaceMiniAppCatalog
import com.katoaapps.openminilaunch.ui.mindyspace.core.MindySpaceMiniAppManifest
import com.katoaapps.openminilaunch.ui.mindyspace.core.MindySpaceMiniAppSettingsTarget
import com.katoaapps.openminilaunch.ui.mindyspace.widget.MindySpaceWidgetSetup
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted
import com.katoaapps.openminilaunch.ui.theme.Rust
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
internal fun MindySpaceSettingsPage(
    store: LauncherStore,
    actions: DeviceActions,
    openTodos: () -> Unit,
    goBack: () -> Unit,
) {
    // Stable ID of the Quick Widget slot whose picker is open; null when closed.
    var widgetSetupSlotId by rememberSaveable { mutableStateOf<String?>(null) }
    val widgetSetupSlot = widgetSetupSlotId?.let(MindySpaceMiniApp::fromStableId)
    val listState = rememberLazyListState()
    var draggedMiniApp by remember { mutableStateOf<MindySpaceMiniApp?>(null) }
    val visibleMiniApps = remember {
        mutableStateListOf<MindySpaceMiniApp>().apply {
            addAll(store.mindySpaceMiniAppOrder.filter(MindySpaceMiniApp::readyForHome))
        }
    }
    val hapticFeedback = LocalHapticFeedback.current
    val reorderableState = rememberReorderableLazyListState(
        lazyListState = listState,
        scrollThresholdPadding = WindowInsets.systemBars.asPaddingValues(),
    ) { from, to ->
        val fromIndex = visibleMiniApps.indexOfFirst { it.stableId == from.key }
        val toIndex = visibleMiniApps.indexOfFirst { it.stableId == to.key }
        if (fromIndex >= 0 && toIndex >= 0 && fromIndex != toIndex) {
            visibleMiniApps.moveItem(fromIndex, toIndex)
            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }
    val storedVisibleOrder = store.mindySpaceMiniAppOrder.filter(MindySpaceMiniApp::readyForHome)
    LaunchedEffect(storedVisibleOrder, draggedMiniApp) {
        if (draggedMiniApp == null && visibleMiniApps != storedVisibleOrder) {
            visibleMiniApps.clear()
            visibleMiniApps.addAll(storedVisibleOrder)
        }
    }
    SettingsLazyPage(
        title = stringResource(R.string.mindy_space_mini_apps),
        goBack = goBack,
        state = listState,
    ) {
        item(key = "mink-space-description") {
            Text(
                text = stringResource(R.string.mindy_space_settings_description),
                color = Muted,
            )
        }
        if (MindySpaceMiniAppCatalog.totalScreenMiniApps.isNotEmpty()) item(key = "mink-space-total-screen-shortcut") {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.dp8)) {
                SectionLabel(stringResource(R.string.full_screen_shortcut))
                val totalScreenApps = MindySpaceMiniAppCatalog.totalScreenMiniApps
                SettingsDropdownRow(
                    title = stringResource(R.string.press_and_hold_action),
                    selectedValue = store.mindySpaceTotalScreenShortcut
                        ?.takeIf(totalScreenApps::contains),
                    options = buildList {
                        add(SettingsDropdownOption(null, stringResource(R.string.off)))
                        totalScreenApps.forEach { miniApp ->
                            val manifest = MindySpaceMiniAppCatalog.get(miniApp)
                            add(
                                SettingsDropdownOption(
                                    miniApp,
                                    stringResource(
                                        manifest.totalScreenLabelRes ?: manifest.labelRes,
                                    ),
                                ),
                            )
                        }
                    },
                    onValueSelected = store::setMindySpaceTotalScreenShortcut,
                )
                Text(
                    text = stringResource(R.string.full_screen_shortcut_description),
                    color = Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        item(key = "mink-space-section-label") {
            SectionLabel(stringResource(R.string.show_in_mindy_space))
        }
        items(visibleMiniApps, key = MindySpaceMiniApp::stableId) { miniApp ->
            val spec = MindySpaceMiniAppCatalog.get(miniApp)
            val onSettingsClick: (() -> Unit)? = when (spec.settingsTarget) {
                MindySpaceMiniAppSettingsTarget.TODOS -> openTodos
                MindySpaceMiniAppSettingsTarget.WIDGET -> ({ widgetSetupSlotId = miniApp.stableId })
                null -> null
            }
            ReorderableItem(reorderableState, key = miniApp.stableId) { isDragging ->
                val elevation by animateFloatAsState(
                    targetValue = if (isDragging) 12f else 0f,
                    label = "mini-app drag elevation",
                )
                val scale by animateFloatAsState(
                    targetValue = if (isDragging) 1.025f else 1f,
                    label = "mini-app drag scale",
                )
                val jiggle = if (isDragging) {
                    val transition = rememberInfiniteTransition(label = "mini-app drag jiggle")
                    transition.animateFloat(
                        initialValue = -0.7f,
                        targetValue = 0.7f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(95),
                            repeatMode = RepeatMode.Reverse,
                        ),
                        label = "mini-app drag rotation",
                    ).value
                } else {
                    0f
                }
                val interactionSource = remember { MutableInteractionSource() }
                MindySpaceMiniAppRow(
                    spec = spec,
                    shown = miniApp in store.enabledMindySpaceMiniApps,
                    onShownChange = { shown ->
                        if (shown && miniApp.isQuickWidget &&
                            store.mindySpaceWidgetId(miniApp) == null
                        ) {
                            widgetSetupSlotId = miniApp.stableId
                        } else {
                            store.setMindySpaceMiniAppEnabled(miniApp, shown)
                        }
                    },
                    onSettingsClick = onSettingsClick,
                    modifier = Modifier
                        .zIndex(if (isDragging) 1f else 0f)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            rotationZ = jiggle
                            shadowElevation = elevation
                        },
                    dragHandle = {
                        Icon(
                            imageVector = Icons.Default.DragHandle,
                            contentDescription = stringResource(R.string.drag_to_reorder),
                            tint = if (isDragging) Rust else Muted,
                            modifier = Modifier
                                .size(Dimens.dp44)
                                .draggableHandle(
                                    interactionSource = interactionSource,
                                    onDragStarted = {
                                        draggedMiniApp = miniApp
                                        hapticFeedback.performHapticFeedback(
                                            HapticFeedbackType.LongPress,
                                        )
                                    },
                                    onDragStopped = {
                                        store.setMindySpaceMiniAppOrder(visibleMiniApps.toList())
                                        draggedMiniApp = null
                                    },
                                )
                                .padding(Dimens.dp10),
                        )
                    },
                )
            }
        }
    }
    MindySpaceWidgetSetup(
        slot = widgetSetupSlot ?: MindySpaceMiniApp.WIDGET,
        visible = widgetSetupSlot != null,
        store = store,
        actions = actions,
        onDismiss = { widgetSetupSlotId = null },
        onComplete = { widgetSetupSlotId = null },
    )
}

@Composable
private fun MindySpaceMiniAppRow(
    spec: MindySpaceMiniAppManifest,
    shown: Boolean,
    onShownChange: (Boolean) -> Unit,
    onSettingsClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    dragHandle: @Composable () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.dp8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = spec.icon,
            contentDescription = null,
            modifier = Modifier.padding(end = Dimens.dp10),
        )
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(spec.labelRes),
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.swipe_vertically_to_switch),
                color = Muted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        dragHandle()
        onSettingsClick?.let {
            IconButton(onClick = it) {
                Icon(Icons.Default.Settings, stringResource(R.string.settings))
            }
        }
        Switch(
            checked = shown,
            onCheckedChange = onShownChange,
        )
    }
}
