package com.katoaapps.openminilaunch.ui.components

import androidx.compose.ui.graphics.Color
import com.katoaapps.openminilaunch.model.IconAppearance
import com.katoaapps.openminilaunch.model.IconSource

/** Tint for app artwork; null tells the renderer to keep the app's original icon. */
internal fun IconAppearance.mindyAppIconTint(automaticTint: Color?): Color? {
    if (source != IconSource.MINDY) return null
    return mindyIconColorArgb?.let(::Color) ?: automaticTint
}

/** Tint for Mindy-owned vector icons, which have no system or icon-pack replacement. */
internal fun IconAppearance.mindyBuiltInIconTint(automaticTint: Color): Color {
    if (source != IconSource.MINDY) return automaticTint
    return mindyIconColorArgb?.let(::Color) ?: automaticTint
}
