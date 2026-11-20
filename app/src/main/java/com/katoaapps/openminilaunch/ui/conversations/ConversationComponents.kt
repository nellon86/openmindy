package com.katoaapps.openminilaunch.ui.conversations

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.conversations.DemoConversationData
import com.katoaapps.openminilaunch.features.conversations.NotificationConversation
import com.katoaapps.openminilaunch.model.FavoriteAndroidContact
import com.katoaapps.openminilaunch.model.FavoriteContact
import com.katoaapps.openminilaunch.model.FavoriteIntegratedContact
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.components.AppIcon
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.MindyWhite
import com.katoaapps.openminilaunch.ui.theme.Muted
import com.katoaapps.openminilaunch.ui.theme.MindyFeatureColors
import com.katoaapps.openminilaunch.ui.theme.readableContentColor
import java.text.DateFormat
import java.util.Date

internal data class ConversationFavoriteRowStyle(
    val color: Color,
    val isUnread: Boolean,
)

@Composable
internal fun ConversationCard(
    conversation: NotificationConversation,
    favoriteContact: FavoriteContact?,
    actions: DeviceActions,
    favoriteStyle: ConversationFavoriteRowStyle?,
    isUnread: Boolean,
    onOpen: () -> Unit,
) {
    val latest = conversation.latestMessage
    // Favourites look like every other conversation here; they are only pinned to the top.
    val containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    val contentColor = MaterialTheme.colorScheme.onSurface
    val secondaryContentColor = contentColor.copy(alpha = .72f)
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onOpen),
        shape = RoundedCornerShape(Dimens.dp18),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
        // Every conversation is outlined: red while it has unread messages, the theme colour once
        // it has been read.
        border = BorderStroke(
            Dimens.dp2,
            if (isUnread) MindyFeatureColors.ConversationCount else MaterialTheme.colorScheme.primary,
        ),
    ) {
        Row(Modifier.fillMaxWidth().padding(Dimens.dp14), verticalAlignment = Alignment.CenterVertically) {
            ConversationParticipantIcon(
                packages = conversation.sourcePackages,
                favoriteContact = favoriteContact,
                actions = actions,
            )
            Column(Modifier.weight(1f).padding(horizontal = Dimens.dp12)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        favoriteContact?.displayName ?: conversation.name,
                        Modifier.weight(1f),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                    Text(
                        conversationTimeLabel(latest?.timestamp ?: conversation.latestNotification.postedAt),
                        color = if (favoriteStyle?.isUnread == true) secondaryContentColor else Muted,
                        fontSize = Dimens.sp11,
                    )
                }
                Text(
                    latest?.text.orEmpty(),
                    color = secondaryContentColor,
                    maxLines = 2,
                    modifier = Modifier.padding(top = Dimens.dp4),
                )
                if (conversation.messages.size > 1) {
                    Text(
                        pluralStringResource(
                            R.plurals.message_count,
                            conversation.messages.size,
                            conversation.messages.size,
                        ),
                        color = if (favoriteStyle?.isUnread == true) {
                            contentColor.copy(alpha = .88f)
                        } else {
                            MindyFeatureColors.ConversationAttention
                        },
                        fontSize = Dimens.sp11,
                        modifier = Modifier.padding(top = Dimens.dp4),
                    )
                }
            }
        }
    }
}

/**
 * A matched one-to-one favorite represents a person, so it uses that person's artwork. Mixed
 * provider rows keep their source stack because no single provider image represents the thread.
 */
@Composable
internal fun ConversationParticipantIcon(
    packages: List<String>,
    favoriteContact: FavoriteContact?,
    actions: DeviceActions,
) {
    if (favoriteContact == null || packages.size != 1) {
        ConversationSourceIcons(packages, actions)
        return
    }
    when (favoriteContact) {
        is FavoriteAndroidContact -> FavoriteContactAvatar(
            displayName = favoriteContact.displayName,
            photoUri = favoriteContact.photoUri,
            size = Dimens.dp38,
        )
        is FavoriteIntegratedContact -> {
            val availability = remember(favoriteContact, actions) {
                actions.integratedContactAvailability(favoriteContact)
            }
            FavoriteIntegratedContactAvatar(
                contact = favoriteContact,
                availability = availability,
                actions = actions,
                size = Dimens.dp38,
            )
        }
    }
}

@Composable
internal fun ConversationSourceIcons(packages: List<String>, actions: DeviceActions) {
    Box(Modifier.size(if (packages.size > 1) Dimens.dp46 else Dimens.dp38)) {
        packages.take(3).forEachIndexed { index, packageName ->
            Box(
                Modifier.align(
                    when (index) {
                        0 -> Alignment.TopStart
                        1 -> Alignment.Center
                        else -> Alignment.BottomEnd
                    },
                ).background(MaterialTheme.colorScheme.surface, CircleShape).padding(Dimens.dp2),
            ) {
                ConversationSourceIcon(packageName, actions, if (index == 0) Dimens.dp34 else Dimens.dp22)
            }
        }
    }
}

@Composable
internal fun ConversationSourceIcon(packageName: String, actions: DeviceActions, size: Dp) {
    val installedIconAvailable = remember(packageName, actions) { actions.isAppInstalled(packageName) }
    if (installedIconAvailable) {
        AppIcon(packageName, actions, size)
        return
    }
    when (packageName) {
        DemoConversationData.SLACK_PACKAGE -> DemoSourceIcon(
            Icons.Default.Forum,
            MindyFeatureColors.Slack,
            size,
        )
        DemoConversationData.GMAIL_PACKAGE -> DemoSourceIcon(
            Icons.Default.Email,
            MindyFeatureColors.Gmail,
            size,
        )
        DemoConversationData.GOOGLE_MESSAGES_PACKAGE -> DemoSourceIcon(
            Icons.Default.Forum,
            MindyFeatureColors.GoogleMessages,
            size,
        )
        else -> AppIcon(packageName, actions, size)
    }
}

@Composable
private fun DemoSourceIcon(icon: ImageVector, background: Color, size: Dp) {
    Box(
        Modifier.size(size).background(background, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, Modifier.size(size * .56f), tint = MindyWhite)
    }
}

/** Time for today's messages, the date for older ones kept in history. */
internal fun conversationTimeLabel(timestamp: Long, now: Long = System.currentTimeMillis()): String {
    val zone = java.util.TimeZone.getDefault()
    val day = 86_400_000L
    val sameDay = (timestamp + zone.getOffset(timestamp)) / day == (now + zone.getOffset(now)) / day
    val format = if (sameDay) DateFormat.getTimeInstance(DateFormat.SHORT) else DateFormat.getDateInstance(DateFormat.SHORT)
    return format.format(Date(timestamp))
}
