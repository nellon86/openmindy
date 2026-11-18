package com.katoaapps.openminilaunch.model

import androidx.annotation.StringRes
import com.katoaapps.openminilaunch.R

/** Controls the source of app colors independently from light and dark mode. */
enum class PalettePreference(@param:StringRes val labelRes: Int) {
    MINDY(R.string.palette_mindy),
    SYSTEM(R.string.palette_system),
}
