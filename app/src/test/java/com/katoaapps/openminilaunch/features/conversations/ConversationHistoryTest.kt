package com.katoaapps.openminilaunch.features.conversations

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationHistoryTest {
    private fun message(id: String, text: String, at: Long, outgoing: Boolean = false) = ConversationMessage(
        id = id,
        conversationId = "c",
        notificationKey = "k",
        packageName = "com.whatsapp",
        appName = "WhatsApp",
        text = text,
        timestamp = at,
        senderName = if (outgoing) null else "Ana",
        isOutgoing = outgoing,
    )

    private fun notification(vararg messages: ConversationMessage, shortcut: String? = "chat-ana") =
        ConversationNotification(
            key = "k",
            conversationId = "c",
            conversationName = "Ana",
            packageName = "com.whatsapp",
            appName = "WhatsApp",
            postedAt = messages.maxOf { it.timestamp },
            isDismissible = true,
            messages = messages.toList(),
            contentIntent = null,
            replyAction = null,
            shortcutId = shortcut,
        )

    @Test fun messagesAccumulateAcrossNotifications() {
        val first = mergeIntoHistory(null, notification(message("1", "hola", 1_000)))
        val second = mergeIntoHistory(first, notification(message("2", "¿qué tal?", 2_000)))!!

        assertEquals(listOf("hola", "¿qué tal?"), second.messages.map { it.text })
        assertEquals("chat-ana", second.shortcutId)
        assertEquals(2_000, second.updatedAt)
    }

    @Test fun repostedNotificationChangesNothing() {
        val stored = mergeIntoHistory(null, notification(message("1", "hola", 1_000)))
        assertSame(stored, mergeIntoHistory(stored, notification(message("1", "hola", 1_000))))
    }

    @Test fun deletedMessagesDoNotComeBack() {
        val item = notification(message("1", "viejo", 1_000), message("2", "nuevo", 5_000))
        assertNull(mergeIntoHistory(null, notification(message("1", "viejo", 1_000)), deletedThrough = 3_000))
        assertEquals(listOf("nuevo"), mergeIntoHistory(null, item, deletedThrough = 3_000)!!.messages.map { it.text })
    }

    @Test fun localReplyIsReplacedByTheAppsEcho() {
        val local = message("local|c|10000|1", "vale", 10_000, outgoing = true)
        val stored = mergeMessages(listOf(message("1", "hola", 1_000), local), emptyList())
        val merged = mergeMessages(stored, listOf(message("wa-out", "vale", 10_500, outgoing = true)))

        assertEquals(listOf("1", "wa-out"), merged.map { it.id })
    }

    @Test fun historyIsCapped() {
        val many = (1..MAX_STORED_MESSAGES_PER_CONVERSATION + 20).map { message("$it", "m$it", it.toLong()) }
        val merged = mergeMessages(emptyList(), many)
        assertEquals(MAX_STORED_MESSAGES_PER_CONVERSATION, merged.size)
        assertTrue(merged.last().text == "m${MAX_STORED_MESSAGES_PER_CONVERSATION + 20}")
    }

    @Test fun historySnapshotHasNoLiveActions() {
        val snapshot = mergeIntoHistory(null, notification(message("1", "hola", 1_000)))!!.toConversationNotification()
        assertTrue(snapshot.isHistory)
        assertEquals(false, snapshot.canReply)
        val grouped = ConversationNotificationStore.groupConversations(listOf(snapshot), emptyList()).single()
        assertEquals(false, grouped.isLive)
        assertEquals("chat-ana", grouped.shortcutTarget?.shortcutId)
    }

    @Test fun repostedPlainNotificationIsNotStoredTwice() {
        val first = message("${FALLBACK_MESSAGE_ID_PREFIX}app|1000|Nuevo correo", "Nuevo correo", 1_000)
        val repost = message("${FALLBACK_MESSAGE_ID_PREFIX}app|61000|Nuevo correo", "Nuevo correo", 61_000)
        assertEquals(1, mergeMessages(listOf(first), listOf(repost)).size)
    }

    @Test fun chatMessagesWithTheSameTextAreKept() {
        val first = message("wa|1000|Ana|ok", "ok", 1_000)
        val second = message("wa|61000|Ana|ok", "ok", 61_000)
        assertEquals(2, mergeMessages(listOf(first), listOf(second)).size)
    }
}
