package com.katoaapps.openminilaunch.ui.mindyspace.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/** Runtime contract implemented by every built-in MindySpace mini-app. */
internal interface MindySpaceApp {
    val manifest: MindySpaceMiniAppManifest

    @Composable
    fun Content(
        scope: MindySpaceScope,
        modifier: Modifier = Modifier,
    )
}

/** Optional runtime contract for content hosted by MindySpace's protected full-window shell. */
internal interface MindySpaceTotalScreenApp : MindySpaceApp {
    @Composable
    fun TotalScreenContent(
        scope: MindySpaceTotalScreenScope,
        modifier: Modifier = Modifier,
    )
}

@Stable
internal class MindySpaceTotalScreenScope internal constructor(
    val use24HourClock: Boolean,
)

/** Platform state and UI primitives supplied to one mini-app by MindySpace. */
@Stable
internal class MindySpaceScope internal constructor(
    val compact: Boolean,
    val contentColor: Color,
    val mutedContentColor: Color,
    val containerColor: Color,
    /** Remains true until the shared collapse animation has finished. */
    val expandedPresentation: Boolean,
    val expansion: MindySpaceExpansionController,
) {
    @Composable
    fun Surface(
        modifier: Modifier = Modifier,
        contentColor: Color = this.contentColor,
        content: @Composable () -> Unit,
    ) {
        MindySpaceMiniAppSurface(
            compact = compact,
            containerColor = containerColor,
            contentColor = contentColor,
            modifier = modifier,
            content = content,
        )
    }
}
