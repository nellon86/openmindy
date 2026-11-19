package com.katoaapps.openminilaunch.ui.conversations

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.ui.theme.Dimens

/** Swiping a conversation to either side deletes it, notifications and history included. */
@Composable
internal fun SwipeToDeleteConversation(
    modifier: Modifier = Modifier,
    onDelete: () -> Unit,
    content: @Composable () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val currentOnDelete by rememberUpdatedState(onDelete)
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value != SwipeToDismissBoxValue.Settled) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                currentOnDelete()
            }
            // The row disappears from the list itself; never leave it parked off-screen.
            false
        },
        positionalThreshold = { distance -> distance * 0.4f },
    )
    SwipeToDismissBox(
        state = state,
        modifier = modifier,
        backgroundContent = {
            val alignment = if (state.dismissDirection == SwipeToDismissBoxValue.StartToEnd) {
                Alignment.CenterStart
            } else {
                Alignment.CenterEnd
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(Dimens.dp18))
                    .padding(horizontal = Dimens.dp20),
                contentAlignment = alignment,
            ) {
                Icon(
                    Icons.Default.Delete,
                    stringResource(R.string.conversation_delete),
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        },
        content = { content() },
    )
}
