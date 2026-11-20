package com.katoaapps.openminilaunch.ui.launcher.home

import com.katoaapps.openminilaunch.ui.conversations.rememberCurrentConversations
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.conversations.isConversationUnread
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.settings.SettingsDestination
import com.katoaapps.openminilaunch.ui.onboarding.HomeTourTarget
import com.katoaapps.openminilaunch.ui.onboarding.homeTourTarget
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.MindyFeatureColors
import com.katoaapps.openminilaunch.ui.wellbeing.MindyHomeIcon

@Composable
internal fun HomeHeader(
    store: LauncherStore,
    actions: DeviceActions,
    mindyStatusActive: Boolean,
    qwertyHome: Boolean,
    horizontalPadding: Dp,
    actionSize: Dp,
    iconSize: Dp,
    followHorizontalWindowOffset: Boolean,
    updateAvailable: Boolean,
    onMindyDay: () -> Unit,
    onUpdate: () -> Unit,
    onConversations: () -> Unit,
    openSettings: (SettingsDestination) -> Unit,
) {
    val context = LocalContext.current
    val headerContentColor = launcherBackgroundContentColor(store, header = true)
    val density = LocalDensity.current
    val verticalPadding = if (qwertyHome) Dimens.dp0 else Dimens.dp8
    val groupGap = Dimens.dp6
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides actionSize) {
        val conversations = rememberCurrentConversations(store)
        val demoMode = store.demoModeEnabled
        val unreadConversationCount by remember(conversations, demoMode) {
            derivedStateOf {
                conversations.count { conversation ->
                    isConversationUnread(
                        conversation,
                        acknowledgedThrough = if (demoMode) 0L else store.acknowledgedConversationActivityAt(conversation.id),
                    )
                }
            }
        }
        val favoriteAttention = rememberPendingFavoriteAttention(
            store = store,
            conversations = conversations,
            defaultColorArgb = MindyFeatureColors.ConversationAttention.toArgb(),
        )
        val dateTime = rememberHomeDateTimeParts(
            datePattern = stringResource(R.string.home_date_pattern),
            showDate = store.showDate,
            showClock = store.showClock,
            use24HourClock = store.use24HourClock,
        )
        val clockHint = if (!store.hasOpenedClockFromDate) {
            stringResource(R.string.tap_for_clock)
        } else {
            null
        }
        val openClock = {
            if (actions.openClock()) {
                store.markClockOpenedFromDate()
            } else {
                Toast.makeText(context, R.string.no_clock_app_found, Toast.LENGTH_SHORT).show()
            }
        }
        CutoutAwareHomeHeaderLayout(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars.only(WindowInsetsSides.Top))
                .homeTourTarget(HomeTourTarget.HEADER),
            horizontalPaddingPx = with(density) { horizontalPadding.roundToPx() },
            verticalPaddingPx = with(density) { verticalPadding.roundToPx() },
            rowHeightPx = with(density) { actionSize.roundToPx() },
            minimumFlexibleWidthPx = with(density) { Dimens.dp64.roundToPx() },
            groupGapPx = with(density) { groupGap.roundToPx() },
            followHorizontalWindowOffset = followHorizontalWindowOffset,
            leadingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MindyHomeIcon(
                        store = store,
                        isActive = mindyStatusActive,
                        onClick = onMindyDay,
                        modifier = Modifier.size(actionSize),
                    )
                    if (store.showBatteryPercentage) {
                        rememberBatteryPercentageText()?.let { batteryPercentage ->
                            Text(
                                batteryPercentage,
                                modifier = Modifier.padding(end = Dimens.dp8),
                                color = headerContentColor,
                                fontSize = Dimens.sp13,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                            )
                        }
                    }
                }
            },
            dateContent = {
                HomeHeaderDateTimeItem(
                    text = dateTime.date,
                    hint = clockHint,
                    contentColor = headerContentColor,
                    onClick = openClock,
                )
            },
            timeContent = {
                HomeHeaderDateTimeItem(
                    text = dateTime.time,
                    hint = clockHint.takeIf { dateTime.date.isEmpty() },
                    contentColor = headerContentColor,
                    onClick = openClock,
                )
            },
            trailingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (updateAvailable) {
                        IconButton(onClick = onUpdate, modifier = Modifier.size(actionSize)) {
                            Icon(
                                Icons.Default.SystemUpdateAlt,
                                stringResource(R.string.update_available),
                                Modifier.size(iconSize),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    ConversationAttentionButton(
                        conversationCount = unreadConversationCount,
                        attentionColors = favoriteAttention.map { Color(it.colorArgb) },
                        actionSize = actionSize,
                        iconSize = iconSize,
                        restingColor = headerContentColor,
                        onClick = onConversations,
                    )
                    IconButton(
                        onClick = { openSettings(SettingsDestination.OVERVIEW) },
                        modifier = Modifier.size(actionSize),
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            stringResource(R.string.settings),
                            Modifier.size(iconSize),
                            tint = headerContentColor,
                        )
                    }
                }
            },
        )
    }
}

@Composable
private fun HomeHeaderDateTimeItem(
    text: String,
    hint: String?,
    contentColor: Color,
    onClick: () -> Unit,
) {
    if (text.isEmpty()) {
        Spacer(Modifier)
        return
    }
    Column(modifier = Modifier.clickable(onClick = onClick)) {
        Text(
            text,
            color = contentColor,
            letterSpacing = Dimens.sp1_5,
            fontSize = Dimens.sp13,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        hint?.let {
            Text(
                it,
                color = contentColor.copy(alpha = .64f),
                fontSize = Dimens.sp9,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
