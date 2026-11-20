package com.katoaapps.openminilaunch.features.conversations

import android.Manifest
import android.app.Notification
import android.app.RemoteInput
import android.content.ComponentName
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.ContactsContract
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.content.ContextCompat
import com.katoaapps.openminilaunch.model.ProviderAddressType
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Converts supported notification payloads into the local conversation model.
 *
 * Android delivers every notification of every app here on the launcher's main thread. Parsing a
 * message notification is comparatively expensive (style recovery, package and contact lookups),
 * so non-message notifications are rejected from cheap fields first and the remaining work runs
 * on one background thread. Results reach [ConversationNotificationStore] on the main thread in
 * the order Android delivered them.
 */
class MindyNotificationListenerService : NotificationListenerService() {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val worker: ExecutorService = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "openmindy-conversations").apply { priority = Thread.NORM_PRIORITY - 1 }
    }
    private val refreshQueued = AtomicBoolean(false)

    /** Incremented on connect and disconnect so stale background results are dropped. */
    @Volatile private var generation = 0

    // The caches below are only touched on [worker].
    private val parsedByKey = object : LinkedHashMap<String, ParsedNotification>(64, .75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ParsedNotification>?) =
            size > MAX_PARSED_NOTIFICATIONS
    }
    private val appNameByPackage = HashMap<String, String>()
    private val contactIdentityCache = object : LinkedHashMap<String, CachedContactIdentity>(64, .75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CachedContactIdentity>?) =
            size > MAX_CACHED_CONTACT_IDENTITIES
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        generation++
        connectedService = this
        ConversationHistoryStore.ensureLoaded(this)
        refreshActiveNotifications()
    }

    override fun onListenerDisconnected() {
        generation++
        if (connectedService === this) connectedService = null
        worker.execute { clearCaches() }
        ConversationNotificationStore.replaceNotifications(emptyList())
        super.onListenerDisconnected()
    }

    override fun onDestroy() {
        generation++
        if (connectedService === this) connectedService = null
        worker.shutdownNow()
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!mayBeConversation(sbn)) return
        val postedGeneration = generation
        runOnWorker {
            val item = parseCached(sbn) ?: return@runOnWorker
            postToStore(postedGeneration) { ConversationNotificationStore.upsertNotification(item) }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        val key = sbn.key
        val removedGeneration = generation
        // Queued behind any pending parse of the same key so a late result cannot resurrect it.
        runOnWorker {
            parsedByKey.remove(key)
            postToStore(removedGeneration) { ConversationNotificationStore.removeNotification(key) }
        }
    }

    /**
     * Reconciles Mindy's in-memory view with Android's authoritative active notification set.
     * Returns false when the listener is not connected; the work itself runs in the background
     * and repeated requests while one is queued are coalesced.
     */
    internal fun refreshActiveNotifications(): Boolean {
        if (connectedService !== this) return false
        if (!refreshQueued.compareAndSet(false, true)) return true
        val refreshGeneration = generation
        runOnWorker {
            refreshQueued.set(false)
            val items = runCatching {
                activeNotifications.orEmpty()
                    .filter(::mayBeConversation)
                    .mapNotNull(::parseCached)
            }.getOrElse {
                mainHandler.post { requestListenerRebind() }
                return@runOnWorker
            }
            postToStore(refreshGeneration) { ConversationNotificationStore.replaceNotifications(items) }
        }
        return true
    }

    private fun requestListenerRebind() {
        runCatching {
            NotificationListenerService.requestRebind(
                ComponentName(this, MindyNotificationListenerService::class.java),
            )
        }
    }

    private fun runOnWorker(block: () -> Unit) {
        runCatching { worker.execute(block) }
    }

    private fun postToStore(expectedGeneration: Int, block: () -> Unit) {
        mainHandler.post { if (generation == expectedGeneration) block() }
    }

    private fun clearCaches() {
        parsedByKey.clear()
        appNameByPackage.clear()
        contactIdentityCache.clear()
    }

    /** Cheap, allocation-free test using only fields that every real conversation must carry. */
    private fun mayBeConversation(sbn: StatusBarNotification): Boolean {
        if (sbn.packageName == packageName) return false
        if (!ConversationPreferences.allows(sbn.packageName)) return false
        val notification = sbn.notification ?: return false
        if (notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return false
        // Apps picked by the user count even when they do not mark notifications as messages;
        // ongoing ones (uploads, calls, music) are never conversations.
        if (ConversationPreferences.forces(sbn.packageName)) {
            return notification.flags and Notification.FLAG_ONGOING_EVENT == 0
        }
        val extras = notification.extras
        return mayBeConversationNotification(
            category = notification.category,
            hasMessagePayload = extras != null && (
                extras.containsKey(Notification.EXTRA_MESSAGES) ||
                    extras.containsKey(Notification.EXTRA_HISTORIC_MESSAGES)
                ),
        )
    }

    /** Each posted version of a notification has its own post time, so a key and time pair is stable. */
    private fun parseCached(sbn: StatusBarNotification): ConversationNotification? {
        parsedByKey[sbn.key]?.takeIf { it.postTime == sbn.postTime }?.let { return it.item }
        val item = runCatching { toConversationNotification(sbn) }.getOrNull()
        parsedByKey[sbn.key] = ParsedNotification(sbn.postTime, item)
        return item
    }

    private fun appName(packageName: String): String = appNameByPackage.getOrPut(packageName) {
        runCatching {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        }.getOrDefault(packageName)
    }

    @Suppress("DEPRECATION")
    private fun toConversationNotification(sbn: StatusBarNotification): ConversationNotification? {
        if (sbn.packageName == packageName) return null
        val notification = sbn.notification ?: return null
        if (notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return null
        val extras = notification.extras ?: Bundle.EMPTY
        val style = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            runCatching {
                Notification.Builder.recoverBuilder(this, notification).style as? Notification.MessagingStyle
            }.getOrNull()
        } else {
            null
        }
        val replyAction = notification.actions?.firstOrNull { action ->
            action.remoteInputs?.any(RemoteInput::getAllowFreeFormInput) == true
        }
        val appName = appName(sbn.packageName)
        val conversationActions = extractConversationActions(sbn, notification, appName)
        val rawTitle = style?.conversationTitle
            ?: extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)
            ?: extras.getCharSequence(Notification.EXTRA_TITLE)
            ?: extras.getCharSequence(Notification.EXTRA_TITLE_BIG)
        // Apps such as WhatsApp add the user's own replies as messages from a "You" person
        // instead of leaving the sender empty. Without recognising them, a reply looked like an
        // incoming message from someone called "You" and split the chat into a second thread.
        val selfPerson = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) style?.user else null
        val selfPersonName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) selfPerson?.name else null
        val selfName = (selfPersonName ?: extras.getCharSequence(Notification.EXTRA_SELF_DISPLAY_NAME))
            ?.toString()
            ?.takeIf(String::isNotBlank)
        val selfKey = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            selfPerson?.key?.takeIf(String::isNotBlank)
        } else {
            null
        }
        val styleMessages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && style != null) {
            (style.historicMessages + style.messages).mapNotNull { message ->
                val body = message.text?.toString()?.takeIf(String::isNotBlank) ?: return@mapNotNull null
                val person = message.senderPerson
                val sender = person?.name?.toString() ?: message.sender?.toString()
                ExtractedMessage(
                    text = body,
                    timestamp = message.timestamp,
                    senderName = sender,
                    personUri = person?.uri,
                    isOutgoing = isOwnMessage(sender, person?.key, selfName, selfKey),
                )
            }
        } else {
            extractLegacyMessages(extras, Notification.EXTRA_HISTORIC_MESSAGES, selfName) +
                extractLegacyMessages(extras, Notification.EXTRA_MESSAGES, selfName)
        }
        if (
            !ConversationPreferences.forces(sbn.packageName) &&
            !isConversationNotification(notification.category, styleMessages.isNotEmpty())
        ) return null
        val latestIncoming = styleMessages.lastOrNull { !it.isOutgoing }
        val identityHints = buildSet {
            styleMessages.asSequence()
                .filterNot(ExtractedMessage::isOutgoing)
                .mapNotNull(ExtractedMessage::personUri)
                .flatMap { providerIdentityHints(it).asSequence() }
                .forEach(::add)
            conversationShortcutIdentityHint(sbn.packageName, notification.shortcutId)
                ?.let(::add)
        }
        val senderName = latestIncoming?.senderName
        val isGroup = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            style?.isGroupConversation == true
        } else {
            extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE) != null
        }
        val conversationName = when {
            isGroup && !rawTitle.isNullOrBlank() -> rawTitle.toString()
            !senderName.isNullOrBlank() -> senderName
            !rawTitle.isNullOrBlank() -> rawTitle.toString()
            else -> appName
        }
        val contactIdentity = if (isGroup) {
            null
        } else {
            resolveContactIdentity(latestIncoming?.personUri, conversationName)
        }
        val providerIdentity = notification.shortcutId
            ?.takeIf(String::isNotBlank)
            ?: normalizeName(conversationName)
        val conversationId = when {
            isGroup -> "${sbn.packageName}:group:$providerIdentity"
            contactIdentity != null -> "contact:$contactIdentity"
            else -> "${sbn.packageName}:direct:$providerIdentity"
        }
        val messages = styleMessages.mapNotNull { message ->
            val body = message.text.takeIf(String::isNotBlank) ?: return@mapNotNull null
            val sender = message.senderName
            val timestamp = message.timestamp.takeIf { it > 0 } ?: sbn.postTime
            ConversationMessage(
                id = "${sbn.packageName}|$timestamp|${sender.orEmpty()}|$body",
                conversationId = conversationId,
                notificationKey = sbn.key,
                packageName = sbn.packageName,
                appName = appName,
                text = body,
                timestamp = timestamp,
                senderName = sender,
                isOutgoing = message.isOutgoing,
            )
        }.ifEmpty {
            val body = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
                ?: extras.getCharSequence(Notification.EXTRA_TEXT)
                ?: extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)
            // notification.when stays the same when an app re-posts an unchanged notification,
            // unlike postTime, so a repost does not look like a new message.
            val sentAt = notification.`when`.takeIf { it > 0 } ?: sbn.postTime
            listOfNotNull(body?.toString()?.takeIf(String::isNotBlank)?.let {
                ConversationMessage(
                    id = "$FALLBACK_MESSAGE_ID_PREFIX${sbn.packageName}|$sentAt|$it",
                    conversationId = conversationId,
                    notificationKey = sbn.key,
                    packageName = sbn.packageName,
                    appName = appName,
                    text = it,
                    timestamp = sbn.postTime,
                    senderName = conversationName,
                    isOutgoing = false,
                )
            })
        }
        if (messages.isEmpty()) return null

        return ConversationNotification(
            key = sbn.key,
            conversationId = conversationId,
            conversationName = conversationName,
            packageName = sbn.packageName,
            appName = appName,
            postedAt = sbn.postTime,
            isDismissible = isDismissibleNotification(notification.flags),
            messages = messages,
            contentIntent = notification.contentIntent,
            replyAction = replyAction,
            actions = conversationActions,
            identityHints = identityHints,
            shortcutId = notification.shortcutId?.takeIf(String::isNotBlank),
        )
    }

    private fun providerIdentityHints(personUri: String?): Set<String> {
        val uri = personUri?.let { runCatching { Uri.parse(it) }.getOrNull() } ?: return emptySet()
        val value = uri.schemeSpecificPart.orEmpty()
            .substringBefore('?')
            .substringBefore(';')
        val type = when {
            uri.scheme.equals("tel", ignoreCase = true) -> ProviderAddressType.PHONE
            uri.scheme.equals("mailto", ignoreCase = true) ||
                uri.scheme.equals("sip", ignoreCase = true) -> ProviderAddressType.USERNAME
            else -> return emptySet()
        }
        return setOfNotNull(providerAddressIdentityHint(type, value))
    }

    private fun extractConversationActions(
        sbn: StatusBarNotification,
        notification: Notification,
        appName: String,
    ): List<ConversationNotificationAction> =
        notification.actions.orEmpty().mapIndexedNotNull { index, action ->
            val title = action.title?.toString()?.trim().orEmpty()
            val requiresRemoteInput = !action.remoteInputs.isNullOrEmpty()
            if (!shouldExposeConversationAction(title, requiresRemoteInput)) {
                return@mapIndexedNotNull null
            }
            ConversationNotificationAction(
                id = "${sbn.key}|$index|$title",
                title = title,
                packageName = sbn.packageName,
                appName = appName,
                notificationKey = sbn.key,
                resolvesNotification = resolvesConversationNotification(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        action.semanticAction
                    } else {
                        Notification.Action.SEMANTIC_ACTION_NONE
                    },
                    title,
                ),
                actionIntent = action.actionIntent,
            )
        }

    @Suppress("DEPRECATION")
    private fun extractLegacyMessages(extras: Bundle, key: String, selfName: String?): List<ExtractedMessage> =
        extras.getParcelableArray(key).orEmpty().mapNotNull { it as? Bundle }.mapNotNull { bundle ->
            val body = bundle.getCharSequence("text")?.toString()?.takeIf(String::isNotBlank)
                ?: return@mapNotNull null
            val sender = bundle.getCharSequence("sender")?.toString()
            ExtractedMessage(
                text = body,
                timestamp = bundle.getLong("time"),
                senderName = sender,
                personUri = null,
                isOutgoing = isOwnMessage(sender, null, selfName, null),
            )
        }

    /** Contact lookups hit the contacts database; repeat messages from one person reuse the answer. */
    private fun resolveContactIdentity(personUri: String?, displayName: String): String? {
        val cacheKey = "${personUri.orEmpty()}\u0000$displayName"
        val now = SystemClock.elapsedRealtime()
        contactIdentityCache[cacheKey]?.takeIf { now - it.resolvedAt < CONTACT_IDENTITY_TTL_MILLIS }
            ?.let { return it.identity }
        return lookupContactIdentity(personUri, displayName).also { identity ->
            contactIdentityCache[cacheKey] = CachedContactIdentity(identity, now)
        }
    }

    private fun lookupContactIdentity(personUri: String?, displayName: String): String? {
        val parsed = personUri?.let { runCatching { Uri.parse(it) }.getOrNull() }
        if (parsed?.scheme.equals("tel", ignoreCase = true)) {
            val digits = parsed?.schemeSpecificPart.orEmpty().filter(Char::isDigit)
            if (digits.isNotBlank()) {
                val lookupUri = Uri.withAppendedPath(
                    ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                    Uri.encode(digits),
                )
                queryLookupKey(lookupUri, ContactsContract.PhoneLookup.LOOKUP_KEY)?.let { return it }
                return "tel:$digits"
            }
        }
        if (
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) !=
            PackageManager.PERMISSION_GRANTED
        ) return null
        val matches = linkedSetOf<String>()
        val uri = Uri.withAppendedPath(
            ContactsContract.Contacts.CONTENT_FILTER_URI,
            Uri.encode(displayName),
        )
        runCatching {
            contentResolver.query(
                uri,
                arrayOf(
                    ContactsContract.Contacts.LOOKUP_KEY,
                    ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
                ),
                null,
                null,
                null,
            )?.use { cursor ->
                val keyIndex = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.LOOKUP_KEY)
                val nameIndex = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
                while (cursor.moveToNext()) {
                    if (normalizeName(cursor.getString(nameIndex)) == normalizeName(displayName)) {
                        matches += cursor.getString(keyIndex)
                    }
                }
            }
        }
        return matches.singleOrNull()
    }

    private fun queryLookupKey(uri: Uri, column: String): String? {
        if (
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) !=
            PackageManager.PERMISSION_GRANTED
        ) return null
        return runCatching {
            contentResolver.query(uri, arrayOf(column), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull()
    }

    private fun normalizeName(value: String): String =
        value.lowercase(java.util.Locale.ROOT).filter(Char::isLetterOrDigit)

    companion object {
        @Volatile internal var connectedService: MindyNotificationListenerService? = null
        private const val MAX_PARSED_NOTIFICATIONS = 128
        private const val MAX_CACHED_CONTACT_IDENTITIES = 128
        private const val CONTACT_IDENTITY_TTL_MILLIS = 10 * 60 * 1_000L
    }
}

/** Marks messages taken from a plain notification text rather than a chat-style payload. */
internal const val FALLBACK_MESSAGE_ID_PREFIX = "fallback|"

private class ParsedNotification(val postTime: Long, val item: ConversationNotification?)

private class CachedContactIdentity(val identity: String?, val resolvedAt: Long)

/**
 * A notification can only become a conversation when it is categorized as a message or email, or
 * carries message bundles (MessagingStyle stores its messages there). Everything else, such as
 * media, download, navigation, and timer updates, is rejected before any parsing work.
 */
internal fun mayBeConversationNotification(category: String?, hasMessagePayload: Boolean): Boolean =
    hasMessagePayload ||
        category == Notification.CATEGORY_MESSAGE ||
        category == Notification.CATEGORY_EMAIL

/** True for a message written by the device owner rather than the other side of the chat. */
internal fun isOwnMessage(sender: String?, senderKey: String?, selfName: String?, selfKey: String?): Boolean =
    sender == null ||
        (selfKey != null && senderKey == selfKey) ||
        (selfName != null && sender == selfName && (senderKey == null || selfKey == null || senderKey == selfKey))

internal fun isDismissibleNotification(flags: Int): Boolean =
    flags and (Notification.FLAG_ONGOING_EVENT or Notification.FLAG_NO_CLEAR) == 0

private data class ExtractedMessage(
    val text: String,
    val timestamp: Long,
    val senderName: String?,
    val personUri: String?,
    val isOutgoing: Boolean,
)
