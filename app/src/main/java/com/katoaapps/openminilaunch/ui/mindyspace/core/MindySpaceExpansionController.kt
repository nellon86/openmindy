package com.katoaapps.openminilaunch.ui.mindyspace.core

import androidx.compose.runtime.Stable
import com.katoaapps.openminilaunch.features.mindyspace.MindySpaceMiniApp

/** Platform handle that lets one mini-app request MindySpace presentation changes. */
@Stable
internal class MindySpaceExpansionController internal constructor(
    val manifest: MindySpaceMiniAppManifest,
    private val presentationState: MindySpacePresentationState,
) {
    val miniApp: MindySpaceMiniApp get() = manifest.miniApp
    val enabled: Boolean get() = manifest.supportsExpansion
    val totalScreenEnabled: Boolean get() = manifest.supportsTotalScreen
    val expanded: Boolean get() = enabled && presentationState.expandedMiniApp == miniApp
    val totalScreen: Boolean get() = totalScreenEnabled && presentationState.totalScreenMiniApp == miniApp

    fun expand() {
        if (enabled) presentationState.expand(manifest)
    }

    fun showTotalScreen() {
        if (totalScreenEnabled) presentationState.showTotalScreen(manifest)
    }

    fun collapse() = presentationState.collapse(miniApp)
}
