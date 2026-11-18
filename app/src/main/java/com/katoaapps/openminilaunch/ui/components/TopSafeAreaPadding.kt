package com.katoaapps.openminilaunch.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Top padding that clears the status bar and any camera cutout.
 *
 * Home hides the status bar by default, which makes `statusBarsPadding()` collapse to zero on every
 * page of the Home pager. Home's header places itself around the cutout, but full-width pages such
 * as Mindy's Day and Widgets would otherwise draw their titles under a punch-hole camera.
 */
@Composable
internal fun Modifier.statusBarsAndCutoutPadding(): Modifier = windowInsetsPadding(
    WindowInsets.statusBars
        .union(WindowInsets.displayCutout)
        .only(WindowInsetsSides.Top),
)
