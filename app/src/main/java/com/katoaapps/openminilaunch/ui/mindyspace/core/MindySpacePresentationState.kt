package com.katoaapps.openminilaunch.ui.mindyspace.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.katoaapps.openminilaunch.features.mindyspace.MindySpaceMiniApp

internal enum class MindySpacePresentationMode {
    EXPANDED,
    TOTAL_SCREEN,
}

internal data class MindySpacePresentation(
    val miniApp: MindySpaceMiniApp,
    val mode: MindySpacePresentationMode,
)

/**
 * Shared presentation state for mini-apps that temporarily need the Home pill or full window.
 *
 * The shell owns layout and gesture changes. Mini-apps only request or release their expansion.
 */
@Stable
internal class MindySpacePresentationState(
    initiallyExpanded: MindySpaceMiniApp? = null,
) {
    var presentation by mutableStateOf(
        initiallyExpanded
            ?.takeIf { MindySpaceMiniAppCatalog.get(it).supportsExpansion }
            ?.let { MindySpacePresentation(it, MindySpacePresentationMode.EXPANDED) },
    )
        private set

    val expandedMiniApp: MindySpaceMiniApp?
        get() = presentation?.takeIf { it.mode == MindySpacePresentationMode.EXPANDED }?.miniApp
    val totalScreenMiniApp: MindySpaceMiniApp?
        get() = presentation?.takeIf { it.mode == MindySpacePresentationMode.TOTAL_SCREEN }?.miniApp
    val isExpanded: Boolean get() = expandedMiniApp != null
    val isTotalScreen: Boolean get() = totalScreenMiniApp != null

    internal fun expansionController(manifest: MindySpaceMiniAppManifest): MindySpaceExpansionController =
        MindySpaceExpansionController(manifest, this)

    internal fun expand(manifest: MindySpaceMiniAppManifest) {
        if (manifest.supportsExpansion) {
            presentation = MindySpacePresentation(
                miniApp = manifest.miniApp,
                mode = MindySpacePresentationMode.EXPANDED,
            )
        }
    }

    internal fun showTotalScreen(manifest: MindySpaceMiniAppManifest) {
        if (manifest.supportsTotalScreen) {
            presentation = MindySpacePresentation(
                miniApp = manifest.miniApp,
                mode = MindySpacePresentationMode.TOTAL_SCREEN,
            )
        }
    }

    fun collapse(miniApp: MindySpaceMiniApp? = null) {
        if (miniApp == null || presentation?.miniApp == miniApp) {
            presentation = null
        }
    }

    fun retainOnly(visibleMiniApp: MindySpaceMiniApp?) {
        if (presentation?.mode != MindySpacePresentationMode.EXPANDED) return
        if (expandedMiniApp != null && expandedMiniApp != visibleMiniApp) collapse()
    }
}

private val MindySpacePresentationStateSaver = Saver<MindySpacePresentationState, String>(
    // A total-screen experience is transient and must never reopen after recreation.
    save = { state -> state.expandedMiniApp?.stableId.orEmpty() },
    restore = { stableId ->
        MindySpacePresentationState(MindySpaceMiniApp.fromStableId(stableId))
    },
)

@Composable
internal fun rememberMindySpacePresentationState(): MindySpacePresentationState =
    rememberSaveable(saver = MindySpacePresentationStateSaver) {
        MindySpacePresentationState()
    }
