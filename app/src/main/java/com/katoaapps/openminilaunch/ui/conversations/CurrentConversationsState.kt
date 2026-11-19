package com.katoaapps.openminilaunch.ui.conversations

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.features.conversations.ConversationHistoryStore
import com.katoaapps.openminilaunch.features.conversations.ConversationNotificationStore
import com.katoaapps.openminilaunch.features.conversations.NotificationConversation

/**
 * Grouped conversations that are rebuilt only when the notification store changes.
 *
 * Reading [ConversationNotificationStore.currentConversations] directly in composition regrouped
 * every notification on each recomposition (the Home header recomposes for the clock, battery,
 * and layout changes), and returned a new list that invalidated every `remember` keyed on it.
 */
@Composable
internal fun rememberCurrentConversations(store: LauncherStore): List<NotificationConversation> {
    val context = androidx.compose.ui.platform.LocalContext.current
    remember(context) { ConversationHistoryStore.ensureLoaded(context) }
    val demoModeEnabled = store.demoModeEnabled
    val demoScenario = store.demoScenario
    val demoConversationState = store.demoConversationState
    val demoSessionStartedAt = store.demoSessionStartedAt
    val conversations by remember(
        demoModeEnabled,
        demoScenario,
        demoConversationState,
        demoSessionStartedAt,
    ) {
        derivedStateOf {
            ConversationNotificationStore.currentConversations(
                useDemoData = demoModeEnabled,
                demoScenario = demoScenario,
                demoConversationState = demoConversationState,
                demoSessionStartedAt = demoSessionStartedAt,
            )
        }
    }
    return conversations
}
