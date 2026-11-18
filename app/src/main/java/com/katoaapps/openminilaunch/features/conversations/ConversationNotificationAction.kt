package com.katoaapps.openminilaunch.features.conversations

import android.app.PendingIntent
import java.text.Normalizer
import java.util.Locale

/** A no-input action supplied by the app that posted a conversation notification. */
data class ConversationNotificationAction(
    val id: String,
    val title: String,
    val packageName: String,
    val appName: String,
    internal val notificationKey: String,
    internal val resolvesNotification: Boolean,
    internal val actionIntent: PendingIntent,
)

internal fun shouldExposeConversationAction(
    title: String,
    requiresRemoteInput: Boolean,
): Boolean = title.isNotBlank() && !requiresRemoteInput

/**
 * True when performing the action removes the message from the source app's inbox, so the
 * notification can be hidden at once instead of waiting for the app to cancel it.
 *
 * Many apps never declare a semantic action, so common archive, delete, and mark-as-read titles
 * are recognized as well. Titles that undo one of those states (mark as unread, restore) are not.
 */
internal fun resolvesConversationNotification(semanticAction: Int, title: String = ""): Boolean {
    when (semanticAction) {
        android.app.Notification.Action.SEMANTIC_ACTION_MARK_AS_READ,
        android.app.Notification.Action.SEMANTIC_ACTION_DELETE,
        android.app.Notification.Action.SEMANTIC_ACTION_ARCHIVE,
        -> return true
        android.app.Notification.Action.SEMANTIC_ACTION_MARK_AS_UNREAD -> return false
    }
    return isResolvingActionTitle(title)
}

internal fun isResolvingActionTitle(title: String): Boolean {
    val normalized = normalizedActionTitle(title)
    if (normalized.isEmpty()) return false
    if (NON_RESOLVING_TITLE_FRAGMENTS.any(normalized::contains)) return false
    return RESOLVING_TITLE_FRAGMENTS.any(normalized::contains)
}

private fun normalizedActionTitle(title: String): String =
    Normalizer.normalize(title.trim().lowercase(Locale.ROOT), Normalizer.Form.NFD)
        .replace(DIACRITICS, "")
        .replace(WHITESPACE, " ")

private val DIACRITICS = Regex("\\p{Mn}+")
private val WHITESPACE = Regex("\\s+")

private val RESOLVING_TITLE_FRAGMENTS = listOf(
    // English
    "archive", "delete", "mark as read", "mark read", "mark all read", "mark all as read",
    // Spanish
    "archivar", "eliminar", "borrar", "marcar como leido", "marcar leido", "marcar todo como leido",
    // Portuguese
    "arquivar", "excluir", "marcar como lida", "marcar como lido",
    // French
    "archiver", "supprimer", "marquer comme lu",
    // Italian
    "archivia", "elimina", "segna come letto", "segna come gia letto",
    // German
    "archivieren", "loschen", "als gelesen markieren",
)

private val NON_RESOLVING_TITLE_FRAGMENTS = listOf(
    "unread", "no leido", "nao lida", "nao lido", "non lu", "non letto", "ungelesen",
    "undo", "deshacer", "restore", "restaurar", "unarchive", "desarchivar",
)
