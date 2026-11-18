package com.katoaapps.openminilaunch.ui.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.LauncherStore

/** Color controls shown only while Mindy's monochrome icon treatment is selected. */
@Composable
internal fun MindyIconColorSetting(store: LauncherStore) {
    val context = LocalContext.current
    val selectedColor = store.iconAppearance.mindyIconColorArgb
    val presets = listOf(
        HomePanelColorPreset(
            R.string.color_mindy,
            androidx.core.content.ContextCompat.getColor(context, R.color.home_panel_mindy),
        ),
        HomePanelColorPreset(
            R.string.color_rust,
            androidx.core.content.ContextCompat.getColor(context, R.color.rust),
        ),
        HomePanelColorPreset(
            R.string.color_navy,
            androidx.core.content.ContextCompat.getColor(context, R.color.home_panel_navy),
        ),
        HomePanelColorPreset(
            R.string.color_plum,
            androidx.core.content.ContextCompat.getColor(context, R.color.home_panel_plum),
        ),
        HomePanelColorPreset(
            R.string.color_forest,
            androidx.core.content.ContextCompat.getColor(context, R.color.mindy_forest),
        ),
    )

    AppearanceColorSetting(
        selectedArgb = selectedColor,
        pickerArgb = selectedColor ?: MaterialTheme.colorScheme.primary.toArgb(),
        titleRes = R.string.mindy_icon_color,
        descriptionRes = R.string.mindy_icon_color_description,
        customTitleRes = R.string.custom_mindy_icon_color,
        customDescriptionRes = R.string.custom_mindy_icon_color_description,
        presets = presets,
        onColorSelected = store::setMindyIconColor,
        onUseThemeDefault = { store.setMindyIconColor(null) },
        defaultValueLabelRes = R.string.automatic,
        useDefaultLabelRes = R.string.use_automatic_icon_color,
    )
}
