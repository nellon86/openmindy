package com.katoaapps.openminilaunch.features.conversations

import android.app.Activity
import android.app.ActivityOptions
import android.app.Notification
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.service.notification.NotificationListenerService
import androidx.compose.runtime.mutableStateListOf
import androidx.core.app.NotificationManagerCompat
import java.util.Locale
import com.katoaapps.openminilaunch.features.demo.DemoConversationState
import com.katoaapps.openminilaunch.features.demo.DemoScenario

data class ConversationMessage(
    val id: String,
    val conversationId: String,
    val notificationKey: String,
    val packageName: String,
    val appName: String,
    val text: String,
    val timestamp: Long,
    val senderName: String?,
    val isOutgoing: Boolean,
)

internal fun isConversationNotification(category: String?, hasStructuredMessages: Boolean): Boolean =
    hasStructuredMessages ||
        category == Notification.CATEGORY_MESSAGE ||
        category == Notification.CATEGORY_EMAIL

data class ConversationNotification(
    val key: String,
    val conversationId: String,
    val conversationName: String,
    val packageName: String,
    val appName: String,
    val postedAt: Long,
    val isDismissible: Boolean,
    val messages: List<ConversationMessage>,
    internal val contentIntent: PendingIntent?,
    internal val replyAction: Notification.Action?,
    val actions: List<ConversationNotificationAction> = emptyList(),
    val identityHints: Set<String> = emptySet(),
    val isDemo: Boolean = false,
    /** Conversation shortcut published with the notification; opens that exact chat. */
    val shortcutId: String? = null,
    /** True for a conversation restored from history rather than a live notification. */
    val isHistory: Boolean = false,
) {
    val canReply: Boolean get() = isDemo || replyAction != null
}

data class NotificationConversation(
    val id: String,
    val name: String,
    val notifications: List<ConversationNotification>,
    val messages: List<ConversationMessage>,
) {
    // Derived values are computed once per immutable snapshot instead of on every UI read.
    val latestNotification: ConversationNotification by lazy(LazyThreadSafetyMode.NONE) {
        notifications.maxBy(ConversationNotification::postedAt)
    }
    val openTarget: ConversationNotification by lazy(LazyThreadSafetyMode.NONE) {
        notifications.firstOrNull { it.contentIntent != null } ?: latestNotification
    }
    val latestMessage: ConversationMessage? by lazy(LazyThreadSafetyMode.NONE) {
        messages.maxByOrNull(ConversationMessage::timestamp)
    }
    val replyTarget: ConversationNotification? by lazy(LazyThreadSafetyMode.NONE) {
        notifications
            .filter(ConversationNotification::canReply)
            .maxByOrNull(ConversationNotification::postedAt)
    }
    val sourcePackages: List<String> by lazy(LazyThreadSafetyMode.NONE) {
        notifications
            .sortedByDescending(ConversationNotification::postedAt)
            .map(ConversationNotification::packageName)
            .distinct()
    }
    val identityHints: Set<String> by lazy(LazyThreadSafetyMode.NONE) {
        notifications.flatMapTo(linkedSetOf(), ConversationNotification::identityHints)
    }
    val actions: List<ConversationNotificationAction> by lazy(LazyThreadSafetyMode.NONE) {
        notifications
            .sortedByDescending(ConversationNotification::postedAt)
            .flatMap(ConversationNotification::actions)
            .distinctBy { action ->
                action.packageName to action.title.lowercase(Locale.ROOT)
            }
    }
    val canDismiss: Boolean by lazy(LazyThreadSafetyMode.NONE) {
        notifications.any(ConversationNotification::isDismissible)
    }
    /** True while at least one real notification of this conversation is still posted. */
    val isLive: Boolean by lazy(LazyThreadSafetyMode.NONE) {
        notifications.any { !it.isHistory }
    }
    val shortcutTarget: ConversationNotification? by lazy(LazyThreadSafetyMode.NONE) {
        notifications.firstOrNull { !it.shortcutId.isNullOrBlank() }
    }
}

internal fun latestConversationActivityAt(
    conversations: List<NotificationConversation>,
): Long? = conversations.maxOfOrNull(::conversationActivityAt)

internal fun conversationActivityAt(conversation: NotificationConversation): Long =
    conversation.messages.asSequence()
        .filterNot(ConversationMessage::isOutgoing)
        .maxOfOrNull(ConversationMessage::timestamp)
        ?: 0L

internal fun shouldShowConversationAttention(
    latestActivityAt: Long?,
    acknowledgedThrough: Long,
): Boolean = latestActivityAt != null && latestActivityAt > acknowledgedThrough

object ConversationNotificationStore {
    val activeNotifications = mutableStateListOf<ConversationNotification>()
    private val sentReplies = mutableStateListOf<ConversationMessage>()
    private val demoReplies = mutableStateListOf<ConversationMessage>()
    private val notificationReconciler = ConversationNotificationReconciler()

    fun hasNotificationAccess(context: Context): Boolean =
        context.packageName in NotificationManagerCompat.getEnabledListenerPackages(context)

    fun notificationAccessSettingsIntent(): Intent =
        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)

    internal fun currentConversations(
        useDemoData: Boolean = false,
        demoScenario: DemoScenario = DemoScenario.PRODUCT_TOUR,
        demoConversationState: DemoConversationState = DemoConversationState.ACTIVE_INBOX,
        demoSessionStartedAt: Long = System.currentTimeMillis(),
    ): List<NotificationConversation> {
        if (useDemoData) {
            return groupConversations(
                sourceNotifications = DemoConversationData.notifications(
                    now = demoSessionStartedAt,
                    scenario = demoScenario,
                    state = demoConversationState,
                ),
                replies = demoReplies,
            )
        }
        val history = if (ConversationPreferences.historyEnabled) {
            ConversationHistoryStore.snapshots()
        } else {
            emptyList()
        }
        return groupConversationsReusing(
            sourceNotifications = (activeNotifications + history)
                .filter { ConversationPreferences.allows(it.packageName) },
            replies = sentReplies,
        )
    }

    /**
     * Live conversations, computed once and shared by every screen (Home header, Conversations,
     * MindySpace). Recomputed only when notifications, history, replies or app filters change.
     */
    val liveConversations: androidx.compose.runtime.State<List<NotificationConversation>> =
        androidx.compose.runtime.derivedStateOf { currentConversations(useDemoData = false) }

    private class GroupCacheEntry(
        val notifications: List<ConversationNotification>,
        val replies: List<ConversationMessage>,
        val conversation: NotificationConversation,
    )

    // Main thread only (read through liveConversations during composition).
    private val groupCache = HashMap<String, GroupCacheEntry>()

    /**
     * Same result as [groupConversations], but a conversation whose notifications and replies are
     * the very same objects as last time is reused instead of rebuilt. A new message in one chat
     * then costs one merge instead of re-sorting every message of the whole history.
     */
    private fun groupConversationsReusing(
        sourceNotifications: List<ConversationNotification>,
        replies: List<ConversationMessage>,
    ): List<NotificationConversation> {
        val repliesById = replies.groupBy(ConversationMessage::conversationId)
        val grouped = sourceNotifications.groupBy(ConversationNotification::conversationId)
        groupCache.keys.retainAll(grouped.keys)
        return grouped.map { (id, notifications) ->
            val conversationReplies = repliesById[id].orEmpty()
            val cached = groupCache[id]
            if (
                cached != null &&
                cached.notifications.sameItems(notifications) &&
                cached.replies.sameItems(conversationReplies)
            ) {
                cached.conversation
            } else {
                groupConversations(notifications, conversationReplies).single().also { built ->
                    groupCache[id] = GroupCacheEntry(notifications, conversationReplies, built)
                }
            }
        }.sortedByDescending { it.latestMessage?.timestamp ?: it.latestNotification.postedAt }
    }

    private fun <T> List<T>.sameItems(other: List<T>): Boolean =
        size == other.size && indices.all { this[it] === other[it] }

    internal fun groupConversations(
        sourceNotifications: List<ConversationNotification>,
        replies: List<ConversationMessage>,
    ): List<NotificationConversation> = sourceNotifications
        .groupBy(ConversationNotification::conversationId)
        .map { (id, grouped) ->
            val orderedNotifications = grouped.sortedByDescending(ConversationNotification::postedAt)
            val providerMessages = grouped.flatMap(ConversationNotification::messages)
            val localReplies = replies.filter { it.conversationId == id }.filterNot { local ->
                providerMessages.any { provider ->
                    provider.isOutgoing && provider.text == local.text &&
                        kotlin.math.abs(provider.timestamp - local.timestamp) < 120_000L
                }
            }
            val messages = (providerMessages + localReplies)
                .distinctBy(ConversationMessage::id)
                .sortedBy(ConversationMessage::timestamp)
            NotificationConversation(
                id = id,
                name = (orderedNotifications.firstOrNull { !it.isHistory } ?: orderedNotifications.first())
                    .conversationName,
                notifications = orderedNotifications,
                messages = messages,
            )
        }
        .sortedByDescending { it.latestMessage?.timestamp ?: it.latestNotification.postedAt }

    /**
     * One chat keeps one id. The contact match behind an id can change between two posts of the
     * same chat (for example when the newest message is the user's own), but the app's
     * conversation shortcut does not, so a known shortcut pins the id that chat already has.
     */
    private fun canonical(item: ConversationNotification): ConversationNotification {
        val shortcutId = item.shortcutId ?: return item
        val knownId = activeNotifications.firstOrNull {
            it.packageName == item.packageName && it.shortcutId == shortcutId && it.key != item.key
        }?.conversationId
            ?: ConversationHistoryStore.conversationIdForShortcut(item.packageName, shortcutId)
            ?: return item
        if (knownId == item.conversationId) return item
        return item.copy(
            conversationId = knownId,
            messages = item.messages.map { it.copy(conversationId = knownId) },
        )
    }

    internal fun replaceNotifications(items: List<ConversationNotification>) {
        val reconciledItems = notificationReconciler.reconcile(items).map(::canonical)
        activeNotifications.clear()
        activeNotifications.addAll(
            reconciledItems.sortedByDescending(ConversationNotification::postedAt),
        )
        reconciledItems.forEach(ConversationHistoryStore::record)
        val activeConversationIds = reconciledItems
            .map(ConversationNotification::conversationId)
            .toSet()
        sentReplies.removeAll { it.conversationId !in activeConversationIds }
    }

    internal fun upsertNotification(incoming: ConversationNotification) {
        if (!notificationReconciler.shouldDisplay(incoming)) return
        val item = canonical(incoming)
        activeNotifications.removeAll { it.key == item.key }
        activeNotifications += item
        activeNotifications.sortByDescending(ConversationNotification::postedAt)
        ConversationHistoryStore.record(item)
    }

    internal fun removeNotification(key: String) {
        val conversationId = activeNotifications.firstOrNull { it.key == key }?.conversationId
        activeNotifications.removeAll { it.key == key }
        if (conversationId != null && activeNotifications.none { it.conversationId == conversationId }) {
            sentReplies.removeAll { it.conversationId == conversationId }
        }
    }

    fun openConversation(context: Context, item: ConversationNotification): Boolean {
        val pendingIntent = item.contentIntent ?: return false
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val options = ActivityOptions.makeBasic()
                    .setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
                    .toBundle()
                pendingIntent.send(context, 0, null, null, null, null, options)
            } else if (context is Activity) {
                context.startIntentSender(pendingIntent.intentSender, null, 0, 0, 0)
            } else {
                pendingIntent.send()
            }
            true
        }.getOrDefault(false)
    }

    /** The newest posted notification of that chat that still accepts an inline reply. */
    fun liveReplyTarget(packageName: String, shortcutId: String): ConversationNotification? =
        activeNotifications
            .filter { it.packageName == packageName && it.shortcutId == shortcutId && it.replyAction != null }
            .maxByOrNull(ConversationNotification::postedAt)

    fun sendReply(item: ConversationNotification, reply: String): Boolean {
        val clean = reply.trim()
        if (clean.isEmpty()) return false
        if (item.isDemo) {
            val now = System.currentTimeMillis()
            demoReplies += ConversationMessage(
                id = "demo-local|${item.conversationId}|$now|${System.nanoTime()}",
                conversationId = item.conversationId,
                notificationKey = item.key,
                packageName = item.packageName,
                appName = item.appName,
                text = clean,
                timestamp = now,
                senderName = null,
                isOutgoing = true,
            )
            return true
        }
        val action = item.replyAction ?: return false
        val inputs = action.remoteInputs
            ?.filter(RemoteInput::getAllowFreeFormInput)
            ?.toTypedArray()
            .orEmpty()
        if (inputs.isEmpty()) return false
        return runCatching {
            val results = Bundle().apply { inputs.forEach { putCharSequence(it.resultKey, clean) } }
            val fillIn = Intent()
            RemoteInput.addResultsToIntent(inputs, fillIn, results)
            action.actionIntent.send(MindyNotificationListenerService.connectedService, 0, fillIn)
            val now = System.currentTimeMillis()
            val sent = ConversationMessage(
                id = "local|${item.conversationId}|$now|${System.nanoTime()}",
                conversationId = item.conversationId,
                notificationKey = item.key,
                packageName = item.packageName,
                appName = item.appName,
                text = clean,
                timestamp = now,
                senderName = null,
                isOutgoing = true,
            )
            sentReplies += sent
            ConversationHistoryStore.recordReply(sent)
            true
        }.getOrDefault(false)
    }

    /**
     * Sends a source-app action. For archive, delete, and mark-as-read style actions the same
     * action is sent for every notification of that app in the conversation (an action on the
     * newest notification alone left older ones behind), and they are hidden immediately rather
     * than after the app eventually cancels them.
     */
    fun performAction(
        context: Context,
        conversation: NotificationConversation,
        action: ConversationNotificationAction,
    ): Boolean {
        if (!action.resolvesNotification) return sendAction(context, action)
        val targets = conversation.notifications.mapNotNull { notification ->
            notification.actions
                .firstOrNull { candidate ->
                    candidate.packageName == action.packageName &&
                        candidate.title.equals(action.title, ignoreCase = true)
                }
                ?.let { matchingAction -> notification to matchingAction }
        }.ifEmpty {
            listOfNotNull(
                activeNotifications.firstOrNull { it.key == action.notificationKey }
                    ?.let { it to action },
            )
        }
        var sentAny = false
        targets.forEach { (notification, matchingAction) ->
            if (sendAction(context, matchingAction)) {
                sentAny = true
                notificationReconciler.suppress(notification)
                removeNotification(notification.key)
            }
        }
        return sentAny || (targets.isEmpty() && sendAction(context, action))
    }

    private fun sendAction(context: Context, action: ConversationNotificationAction): Boolean = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val options = ActivityOptions.makeBasic()
                .setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
                .toBundle()
            action.actionIntent.send(context, 0, null, null, null, null, options)
        } else {
            action.actionIntent.send(context, 0, null)
        }
        true
    }.getOrDefault(false)

    /**
     * Deletes a conversation: clears its notifications and forgets its history. Messages that
     * were already there never come back; a new message starts the conversation again.
     */
    fun dismissConversation(conversation: NotificationConversation): Boolean {
        val hadHistory = ConversationHistoryStore.conversations.containsKey(conversation.id)
        ConversationHistoryStore.delete(conversation.id, conversation.latestMessage?.timestamp)
        sentReplies.removeAll { it.conversationId == conversation.id }
        val service = MindyNotificationListenerService.connectedService
            ?: return hadHistory || !conversation.isLive
        var dismissedAny = hadHistory
        conversation.notifications
            .filter { it.isDismissible && !it.isHistory }
            .forEach { notification ->
                val requested = runCatching {
                    service.cancelNotification(notification.key)
                    true
                }.getOrDefault(false)
                if (requested) {
                    notificationReconciler.suppress(notification)
                    removeNotification(notification.key)
                    dismissedAny = true
                }
            }
        return dismissedAny
    }

    fun clearDemoReplyHistory() {
        demoReplies.clear()
    }

    fun requestListenerReconnect(context: Context) {
        runCatching {
            NotificationListenerService.requestRebind(
                ComponentName(context, MindyNotificationListenerService::class.java),
            )
        }
    }

    fun refreshActiveNotifications(context: Context) {
        val refreshed = MindyNotificationListenerService.connectedService
            ?.refreshActiveNotifications() == true
        if (!refreshed) requestListenerReconnect(context)
    }
}
