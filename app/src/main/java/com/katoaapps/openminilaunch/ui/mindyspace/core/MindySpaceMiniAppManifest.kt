package com.katoaapps.openminilaunch.ui.mindyspace.core

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import com.katoaapps.openminilaunch.features.mindyspace.MindySpaceMiniApp

/** Static identity, presentation metadata, and platform capabilities declared by one mini-app. */
@Immutable
internal data class MindySpaceMiniAppManifest(
    val miniApp: MindySpaceMiniApp,
    @param:StringRes val labelRes: Int,
    val icon: ImageVector,
    /** Grants access to [MindySpaceExpansionController]; it does not render a control. */
    val supportsExpansion: Boolean = false,
    /** Asks MindySpace to draw its standard collapsed-state expansion action. */
    val showExpansionButton: Boolean = false,
    /** Overrides the standard "Expand view" label when [showExpansionButton] is true. */
    @param:StringRes val expansionButtonLabelRes: Int? = null,
    /** Lets this mini-app provide content inside MindySpace's protected full-window shell. */
    val supportsTotalScreen: Boolean = false,
    /** User-facing title for total-screen presentation. Defaults to [labelRes]. */
    @param:StringRes val totalScreenLabelRes: Int? = null,
    val settingsTarget: MindySpaceMiniAppSettingsTarget? = null,
) {
    init {
        require(supportsExpansion || !showExpansionButton) {
            "A MindySpace expansion button requires expansion support"
        }
        require(showExpansionButton || expansionButtonLabelRes == null) {
            "A MindySpace expansion label requires an expansion button"
        }
        require(supportsTotalScreen || totalScreenLabelRes == null) {
            "A MindySpace total-screen label requires total-screen support"
        }
    }
}

internal enum class MindySpaceMiniAppSettingsTarget {
    TODOS,
    WIDGET,
}
