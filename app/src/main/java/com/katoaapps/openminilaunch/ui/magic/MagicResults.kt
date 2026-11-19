package com.katoaapps.openminilaunch.ui.magic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.features.files.FileSearchRepository
import com.katoaapps.openminilaunch.features.apps.launcherDiscoveryLabel
import com.katoaapps.openminilaunch.features.magic.MAGIC_DEFAULT_ROUTE
import com.katoaapps.openminilaunch.features.magic.MAGIC_FILE_SEARCH_ENABLED
import com.katoaapps.openminilaunch.features.magic.MagicBoxRoute
import com.katoaapps.openminilaunch.model.ContactResult
import com.katoaapps.openminilaunch.model.FileSearchResult
import com.katoaapps.openminilaunch.model.LauncherTarget
import com.katoaapps.openminilaunch.model.LauncherAppTarget
import com.katoaapps.openminilaunch.model.LauncherShortcutTarget
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.components.LauncherTargetIcon
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.MagicTodoColor
import com.katoaapps.openminilaunch.ui.theme.Muted

@Composable
internal fun MagicResultsPanel(
    store: LauncherStore,
    actions: DeviceActions,
    rawText: String,
    route: MagicBoxRoute,
    launcherAppsRevision: Long,
    launcherShortcutsRevision: Long,
    plainQuery: String,
    fileSearchLoading: Boolean,
    fileResults: List<FileSearchResult>,
    fileSearchRepository: FileSearchRepository,
    hasMediaAccess: Boolean,
    canSearchContacts: Boolean,
    contactResults: List<ContactResult>,
    recentConversations: List<LauncherShortcutTarget>,
    forcedRecipient: String?,
    appResults: List<LauncherTarget>,
    includeAppShortcuts: Boolean,
    showClearMessage: Boolean,
    scrollState: ScrollState,
    onSelectHistory: (String) -> Unit,
    onOpenFile: (FileSearchResult) -> Unit,
    onRequestMedia: () -> Unit,
    onRequestFolder: () -> Unit,
    onSubmitWeb: () -> Unit,
    onSearchWebFallback: () -> Unit,
    onSubmitAi: () -> Unit,
    onOpenTodos: (() -> Unit)?,
    onSelectContact: (ContactResult) -> Unit,
    onSelectRecentConversation: (LauncherShortcutTarget) -> Unit,
    onSelectForcedRecipient: (String) -> Unit,
    onSelectApp: (LauncherTarget) -> Unit,
    onLongPressApp: (LauncherAppTarget) -> Unit,
    onIncludeAppShortcutsChange: (Boolean) -> Unit,
    onRequestContacts: () -> Unit,
    onClearMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.BottomCenter) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(Dimens.dp8),
        ) {
            MagicResultsContent(
                store = store,
                actions = actions,
                rawText = rawText,
                route = route,
                launcherAppsRevision = launcherAppsRevision,
                launcherShortcutsRevision = launcherShortcutsRevision,
                plainQuery = plainQuery,
                fileSearchLoading = fileSearchLoading,
                fileResults = fileResults,
                fileSearchRepository = fileSearchRepository,
                hasMediaAccess = hasMediaAccess,
                canSearchContacts = canSearchContacts,
                contactResults = contactResults,
                recentConversations = recentConversations,
                forcedRecipient = forcedRecipient,
                appResults = appResults,
                includeAppShortcuts = includeAppShortcuts,
                onSelectHistory = onSelectHistory,
                onOpenFile = onOpenFile,
                onRequestMedia = onRequestMedia,
                onRequestFolder = onRequestFolder,
                onSubmitWeb = onSubmitWeb,
                onSearchWebFallback = onSearchWebFallback,
                onSubmitAi = onSubmitAi,
                onOpenTodos = onOpenTodos,
                onSelectContact = onSelectContact,
                onSelectRecentConversation = onSelectRecentConversation,
                onSelectForcedRecipient = onSelectForcedRecipient,
                onSelectApp = onSelectApp,
                onLongPressApp = onLongPressApp,
                onIncludeAppShortcutsChange = onIncludeAppShortcutsChange,
                onRequestContacts = onRequestContacts,
            )
        }
        if (showClearMessage) {
            FilledTonalIconButton(
                onClick = onClearMessage,
                modifier = Modifier.align(Alignment.TopEnd),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
            ) {
                Icon(Icons.Default.DeleteOutline, stringResource(R.string.clear_message_draft))
            }
        }
    }
}

/** Search and command suggestions shown above the Magic Box input. */
@Composable
internal fun MagicResultsContent(
    store: LauncherStore,
    actions: DeviceActions,
    rawText: String,
    route: MagicBoxRoute,
    launcherAppsRevision: Long,
    launcherShortcutsRevision: Long,
    plainQuery: String,
    fileSearchLoading: Boolean,
    fileResults: List<FileSearchResult>,
    fileSearchRepository: FileSearchRepository,
    hasMediaAccess: Boolean,
    canSearchContacts: Boolean,
    contactResults: List<ContactResult>,
    recentConversations: List<LauncherShortcutTarget>,
    forcedRecipient: String?,
    appResults: List<LauncherTarget>,
    includeAppShortcuts: Boolean,
    onSelectHistory: (String) -> Unit,
    onOpenFile: (FileSearchResult) -> Unit,
    onRequestMedia: () -> Unit,
    onRequestFolder: () -> Unit,
    onSubmitWeb: () -> Unit,
    onSearchWebFallback: () -> Unit,
    onSubmitAi: () -> Unit,
    onOpenTodos: (() -> Unit)?,
    onSelectContact: (ContactResult) -> Unit,
    onSelectRecentConversation: (LauncherShortcutTarget) -> Unit,
    onSelectForcedRecipient: (String) -> Unit,
    onSelectApp: (LauncherTarget) -> Unit,
    onLongPressApp: (LauncherAppTarget) -> Unit,
    onIncludeAppShortcutsChange: (Boolean) -> Unit,
    onRequestContacts: () -> Unit,
) {
    if (onOpenTodos != null && route == MagicBoxRoute.TODO && rawText.isBlank()) {
        SuggestionRow(
            text = stringResource(R.string.open_todos),
            icon = Icons.Default.Checklist,
            onClick = onOpenTodos,
        )
    }
    if (rawText.isBlank() && route == MAGIC_DEFAULT_ROUTE && store.searchHistory.isNotEmpty()) {
        SearchHistoryList(
            queries = store.searchHistory,
            onSelect = onSelectHistory,
            onDelete = store::removeSearchQuery,
            onClearAll = store::clearSearchHistory,
        )
    }
    if (MAGIC_FILE_SEARCH_ENABLED && plainQuery.isNotBlank() && fileSearchLoading) {
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.dp6)) {
            Text(
                text = stringResource(R.string.searching_this_device),
                style = MaterialTheme.typography.labelMedium,
                color = Muted,
            )
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }
    }
    if (fileResults.isNotEmpty()) {
        FileResultsGrid(fileResults, fileSearchRepository, onOpenFile)
    }
    if (MAGIC_FILE_SEARCH_ENABLED && plainQuery.length >= 2 && !hasMediaAccess && !store.demoModeEnabled) {
        FilledTonalButton(onClick = onRequestMedia) {
            Icon(Icons.Default.PhotoLibrary, null, Modifier.size(Dimens.dp18))
            Text(stringResource(R.string.search_media_filenames), Modifier.padding(start = Dimens.dp8))
        }
    }
    if (MAGIC_FILE_SEARCH_ENABLED && plainQuery.isNotBlank() && store.searchFolders.isEmpty() && !store.demoModeEnabled) {
        Surface(
            onClick = onRequestFolder,
            shape = RoundedCornerShape(Dimens.dp14),
            color = MagicTodoColor.copy(alpha = .18f),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(Dimens.dp12),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.FolderOff, null, tint = MagicTodoColor)
                Column(Modifier.weight(1f).padding(start = Dimens.dp10)) {
                    Text(stringResource(R.string.document_search_not_set_up), fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.choose_document_search_folder), color = Muted, fontSize = Dimens.sp12)
                }
                Icon(Icons.Default.ChevronRight, null)
            }
        }
    }
    if (route == MagicBoxRoute.SEARCH && rawText.isNotBlank()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.dp8)) {
            SearchDestinationButton(
                label = stringResource(R.string.web),
                detail = stringResource(R.string.search_browser),
                icon = Icons.Default.Public,
                modifier = Modifier.weight(1f),
                onClick = onSubmitWeb,
            )
            SearchDestinationButton(
                label = stringResource(R.string.ai),
                detail = store.preferredAiPackage?.let(actions::appLabel) ?: stringResource(R.string.choose_an_app),
                icon = Icons.Default.AutoAwesome,
                modifier = Modifier.weight(1f),
                onClick = onSubmitAi,
            )
        }
    }
    // With automatic sending every @ result goes to one app; say which.
    val messagingAppLabel = if (route == MagicBoxRoute.MESSAGE && store.sendMessagesAutomatically) {
        (store.preferredMessagingPackage ?: actions.defaultMessagingPackage())?.let(actions::appLabel)
    } else {
        null
    }
    if (route == MagicBoxRoute.MESSAGE && contactResults.isNotEmpty()) {
        MagicResultSectionTitle(
            messagingAppLabel?.let { stringResource(R.string.magic_contacts_in_app, it) }
                ?: stringResource(R.string.contacts),
        )
    }
    contactResults.forEach { contact ->
        SuggestionRow(
            stringResource(R.string.two_part_label, contact.name, contact.phoneLabel),
            Icons.Default.Person,
        ) { onSelectContact(contact) }
    }
    RecentConversationResults(
        conversations = recentConversations,
        actions = actions,
        appsRevision = launcherAppsRevision,
        shortcutsRevision = launcherShortcutsRevision,
        onOpen = onSelectRecentConversation,
    )
    forcedRecipient?.let { identifier ->
        val isMessage = route == MagicBoxRoute.MESSAGE
        SuggestionRow(
            text = stringResource(
                if (isMessage) R.string.message_unknown_recipient else R.string.call_unknown_recipient,
                identifier,
            ),
            supportingText = when {
                isMessage && messagingAppLabel != null ->
                    stringResource(R.string.unknown_recipient_hint_in_app, messagingAppLabel)
                else -> stringResource(R.string.unknown_recipient_hint)
            },
            icon = Icons.Default.Person,
        ) { onSelectForcedRecipient(identifier) }
    }
    if (route == MagicBoxRoute.APP && rawText.isNotBlank()) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.include_app_shortcuts),
                modifier = Modifier.padding(end = Dimens.dp10),
            )
            Switch(
                checked = includeAppShortcuts,
                onCheckedChange = onIncludeAppShortcutsChange,
            )
        }
    }
    if (route == MagicBoxRoute.APP && rawText.isNotBlank() && appResults.isEmpty()) {
        SuggestionRow(
            text = stringResource(R.string.search_web_for_query, rawText.trim()),
            icon = Icons.Default.Public,
            onClick = onSearchWebFallback,
        )
    }
    appResults.forEach { app ->
        SuggestionRow(
            text = launcherDiscoveryLabel(app, actions::appLabel),
            leadingContent = { LauncherTargetIcon(app, actions, Dimens.dp26) },
            supportingText = stringResource(R.string.work_profile)
                .takeIf { app.isWorkProfile },
            onLongClick = (app as? LauncherAppTarget)?.let { target ->
                { onLongPressApp(target) }
            },
        ) { onSelectApp(app) }
    }
    if (
        (route == MagicBoxRoute.MESSAGE || route == MagicBoxRoute.CALL) &&
        !canSearchContacts
    ) {
        FilledTonalButton(onClick = onRequestContacts) {
            Text(stringResource(R.string.allow_contacts_to_search_people))
        }
    }
}
