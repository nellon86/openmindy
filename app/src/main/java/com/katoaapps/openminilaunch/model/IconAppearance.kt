package com.katoaapps.openminilaunch.model

enum class IconSource {
    MINDY,
    SYSTEM,
    ICON_PACK,
}

data class IconAppearance(
    val source: IconSource = IconSource.MINDY,
    val iconPackPackage: String? = null,
    val mindyIconColorArgb: Int? = null,
)
