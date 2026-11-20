package com.katoaapps.openminilaunch.features.conversations

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.mutableStateMapOf
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.Executors

/** One conversation kept after its notifications are gone. */
internal data class StoredConversation(
    val id: String,
    val name: String,
    val packageName: String,
    val appName: String,
    val shortcutId: String?,
    val identityHints: Set<String>,
    val updatedAt: Long,
    val messages: List<ConversationMessage>,
)

internal const val HISTORY_NOTIFICATION_KEY_PREFIX = "history:"
internal const val MAX_STORED_MESSAGES_PER_CONVERSATION = 300
internal const val MAX_STORED_CONVERSATIONS = 150
private const val LOCAL_REPLY_MATCH_WINDOW_MILLIS = 120_000L

/**
 * Adds the messages of [item] to [existing]. Messages at or before [deletedThrough] (the moment
 * the user deleted this conversation) are ignored, so a notification that is still on screen
 * cannot bring a deleted conversation back. Returns [existing] itself when nothing changed.
 */
internal fun mergeIntoHistory(
    existing: StoredConversation?,
    item: ConversationNotification,
    deletedThrough: Long = 0L,
): StoredConversation? {
    val fresh = item.messages.filter { it.timestamp > deletedThrough }
    if (existing == null && fresh.isEmpty()) return null
    val merged = mergeMessages(existing?.messages.orEmpty(), fresh)
    val updatedAt = maxOf(existing?.updatedAt ?: 0L, merged.lastOrNull()?.timestamp ?: item.postedAt)
    val next = StoredConversation(
        id = item.conversationId,
        name = item.conversationName.ifBlank { existing?.name.orEmpty() },
        packageName = item.packageName,
        appName = item.appName,
        shortcutId = item.shortcutId ?: existing?.shortcutId,
        identityHints = existing?.identityHints.orEmpty() + item.identityHints,
        updatedAt = updatedAt,
        messages = merged,
    )
    return if (next == existing) existing else next
}

/**
 * Union of two message lists without duplicates, oldest first and capped. A reply sent from
 * OpenMindy is dropped once the source app echoes the same text back in its own notification.
 */
internal fun mergeMessages(
    stored: List<ConversationMessage>,
    incoming: List<ConversationMessage>,
): List<ConversationMessage> {
    if (incoming.isEmpty()) return stored
    val providerOutgoing = incoming.filter { it.isOutgoing && !it.isLocalReply }
    val kept = stored.filterNot { local ->
        local.isLocalReply && providerOutgoing.any { provider ->
            provider.text == local.text &&
                kotlin.math.abs(provider.timestamp - local.timestamp) < LOCAL_REPLY_MATCH_WINDOW_MILLIS
        }
    }
    val incomingDeduped = incoming.filterNot { candidate ->
        candidate.isLocalReply && kept.any { provider ->
            provider.isOutgoing && !provider.isLocalReply && provider.text == candidate.text &&
                kotlin.math.abs(provider.timestamp - candidate.timestamp) < LOCAL_REPLY_MATCH_WINDOW_MILLIS
        }
    }
    return (kept + incomingDeduped)
        .distinctBy(ConversationMessage::id)
        .sortedBy(ConversationMessage::timestamp)
        .takeLast(MAX_STORED_MESSAGES_PER_CONVERSATION)
}

private val ConversationMessage.isLocalReply: Boolean get() = id.startsWith("local|")

/** A stored conversation shown through the same model as a live notification, minus its actions. */
internal fun StoredConversation.toConversationNotification(): ConversationNotification =
    ConversationNotification(
        key = HISTORY_NOTIFICATION_KEY_PREFIX + id,
        conversationId = id,
        conversationName = name,
        packageName = packageName,
        appName = appName,
        postedAt = updatedAt,
        isDismissible = true,
        messages = messages,
        contentIntent = null,
        replyAction = null,
        identityHints = identityHints,
        shortcutId = shortcutId,
        isHistory = true,
    )

/**
 * Conversation history kept on this device only. It lives in the no-backup folder, so neither
 * Android backup nor OpenMindy's own backup file ever contains message text.
 *
 * State is changed on the main thread; reading and writing the file happens on one background
 * thread, and writes are batched so a burst of notifications costs one write.
 */
internal object ConversationHistoryStore {
    val conversations = mutableStateMapOf<String, StoredConversation>()
    private val deletedThrough = HashMap<String, Long>()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val io = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "openmindy-history").apply { priority = Thread.MIN_PRIORITY }
    }
    private var file: File? = null
    private var loaded = false
    private var savePending = false
    private val saveRunnable = Runnable { writeNow() }

    fun ensureLoaded(context: Context) {
        ConversationPreferences.ensureLoaded(context)
        if (file != null) return
        val target = File(context.applicationContext.noBackupFilesDir, FILE_NAME)
        file = target
        io.execute {
            val parsed = runCatching { if (target.exists()) parse(target.readText()) else null }.getOrNull()
            mainHandler.post { finishLoading(parsed) }
        }
    }

    private fun finishLoading(parsed: Pair<List<StoredConversation>, Map<String, Long>>?) {
        parsed?.let { (stored, deleted) ->
            deleted.forEach { (id, at) -> deletedThrough[id] = maxOf(deletedThrough[id] ?: 0L, at) }
            stored.forEach { conversation ->
                val cutoff = deletedThrough[conversation.id] ?: 0L
                val inMemory = conversations[conversation.id]
                val messages = mergeMessages(
                    conversation.messages.filter { it.timestamp > cutoff },
                    inMemory?.messages.orEmpty(),
                )
                if (messages.isNotEmpty()) {
                    conversations[conversation.id] = (inMemory ?: conversation).copy(
                        messages = messages,
                        updatedAt = maxOf(conversation.updatedAt, inMemory?.updatedAt ?: 0L),
                    )
                }
            }
        }
        loaded = true
        if (savePending) scheduleSave()
    }

    /** Keeps the messages of a live conversation notification. */
    fun record(item: ConversationNotification) {
        if (item.isDemo || item.isHistory || !ConversationPreferences.historyEnabled) return
        val existing = conversations[item.conversationId]
        val next = mergeIntoHistory(existing, item, deletedThrough[item.conversationId] ?: 0L) ?: return
        if (next === existing) return
        conversations[next.id] = next
        trimConversations()
        scheduleSave()
    }

    /** Keeps a reply sent from OpenMindy. */
    fun recordReply(message: ConversationMessage) {
        if (!ConversationPreferences.historyEnabled) return
        val existing = conversations[message.conversationId] ?: return
        conversations[existing.id] = existing.copy(
            messages = mergeMessages(existing.messages, listOf(message)),
            updatedAt = maxOf(existing.updatedAt, message.timestamp),
        )
        scheduleSave()
    }

    /** Removes a conversation; the same messages are never stored again. */
    fun delete(conversationId: String, latestMessageAt: Long?) {
        val stored = conversations.remove(conversationId)
        val through = maxOf(
            System.currentTimeMillis(),
            latestMessageAt ?: 0L,
            stored?.messages?.lastOrNull()?.timestamp ?: 0L,
        )
        deletedThrough.remove(conversationId)
        deletedThrough[conversationId] = through
        while (deletedThrough.size > MAX_DELETED_MARKERS) {
            deletedThrough.remove(deletedThrough.minByOrNull { it.value }?.key ?: break)
        }
        scheduleSave()
    }

    fun clearAll() {
        val now = System.currentTimeMillis()
        conversations.keys.toList().forEach { deletedThrough[it] = now }
        conversations.clear()
        scheduleSave()
    }

    fun conversationIdForShortcut(packageName: String, shortcutId: String): String? =
        conversations.values
            .filter { it.packageName == packageName && it.shortcutId == shortcutId }
            .maxByOrNull(StoredConversation::updatedAt)
            ?.id

    fun snapshots(): List<ConversationNotification> =
        conversations.values.map(StoredConversation::toConversationNotification)

    private fun trimConversations() {
        if (conversations.size <= MAX_STORED_CONVERSATIONS) return
        conversations.values
            .sortedBy(StoredConversation::updatedAt)
            .take(conversations.size - MAX_STORED_CONVERSATIONS)
            .forEach { conversations.remove(it.id) }
    }

    private fun scheduleSave() {
        if (!loaded) {
            // Writing before the file was read would wipe the history on disk.
            savePending = true
            return
        }
        savePending = false
        mainHandler.removeCallbacks(saveRunnable)
        mainHandler.postDelayed(saveRunnable, SAVE_DELAY_MILLIS)
    }

    private fun writeNow() {
        val target = file ?: return
        // Immutable snapshots are taken here; turning them into JSON happens off the main thread.
        val stored = conversations.values.toList()
        val deleted = HashMap(deletedThrough)
        io.execute {
            runCatching {
                val json = serialize(stored, deleted)
                val temp = File(target.parentFile, "$FILE_NAME.tmp")
                temp.writeText(json)
                if (!temp.renameTo(target)) {
                    target.writeText(json)
                    temp.delete()
                }
            }
        }
    }

    internal fun serialize(stored: List<StoredConversation>, deleted: Map<String, Long>): String {
        val root = JSONObject().put("v", 1)
        root.put(
            "conversations",
            JSONArray().apply {
                stored.forEach { conversation ->
                    put(
                        JSONObject()
                            .put("id", conversation.id)
                            .put("name", conversation.name)
                            .put("pkg", conversation.packageName)
                            .put("app", conversation.appName)
                            .putOpt("shortcut", conversation.shortcutId)
                            .put("hints", JSONArray(conversation.identityHints.toList()))
                            .put("updated", conversation.updatedAt)
                            .put(
                                "messages",
                                JSONArray().apply {
                                    conversation.messages.forEach { message ->
                                        put(
                                            JSONObject()
                                                .put("id", message.id)
                                                .put("pkg", message.packageName)
                                                .put("app", message.appName)
                                                .put("text", message.text)
                                                .put("ts", message.timestamp)
                                                .putOpt("sender", message.senderName)
                                                .put("out", message.isOutgoing),
                                        )
                                    }
                                },
                            ),
                    )
                }
            },
        )
        root.put("deleted", JSONObject().apply { deleted.forEach { (id, at) -> put(id, at) } })
        return root.toString()
    }

    internal fun parse(text: String): Pair<List<StoredConversation>, Map<String, Long>> {
        val root = JSONObject(text)
        val conversationsJson = root.optJSONArray("conversations") ?: JSONArray()
        val stored = (0 until conversationsJson.length()).mapNotNull { index ->
            val item = conversationsJson.optJSONObject(index) ?: return@mapNotNull null
            val id = item.optString("id").takeIf(String::isNotBlank) ?: return@mapNotNull null
            val messagesJson = item.optJSONArray("messages") ?: JSONArray()
            val messages = (0 until messagesJson.length()).mapNotNull { messageIndex ->
                val message = messagesJson.optJSONObject(messageIndex) ?: return@mapNotNull null
                ConversationMessage(
                    id = message.optString("id"),
                    conversationId = id,
                    notificationKey = HISTORY_NOTIFICATION_KEY_PREFIX + id,
                    packageName = message.optString("pkg"),
                    appName = message.optString("app"),
                    text = message.optString("text").takeIf(String::isNotEmpty) ?: return@mapNotNull null,
                    timestamp = message.optLong("ts"),
                    senderName = message.optString("sender").takeIf { message.has("sender") },
                    isOutgoing = message.optBoolean("out"),
                )
            }
            if (messages.isEmpty()) return@mapNotNull null
            val hintsJson = item.optJSONArray("hints") ?: JSONArray()
            StoredConversation(
                id = id,
                name = item.optString("name"),
                packageName = item.optString("pkg"),
                appName = item.optString("app"),
                shortcutId = item.optString("shortcut").takeIf { item.has("shortcut") },
                identityHints = (0 until hintsJson.length()).map(hintsJson::optString).toSet(),
                updatedAt = item.optLong("updated"),
                messages = messages,
            )
        }
        val deletedJson = root.optJSONObject("deleted")
        val deleted = deletedJson?.keys()?.asSequence()?.associateWith { deletedJson.optLong(it) }.orEmpty()
        return stored to deleted
    }

    private const val FILE_NAME = "conversation_history.json"
    private const val SAVE_DELAY_MILLIS = 5_000L
    private const val MAX_DELETED_MARKERS = 300
}
