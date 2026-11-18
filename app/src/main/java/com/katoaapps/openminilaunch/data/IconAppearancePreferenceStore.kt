package com.katoaapps.openminilaunch.data

import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.katoaapps.openminilaunch.model.IconAppearance
import com.katoaapps.openminilaunch.model.IconSource

internal class IconAppearancePreferenceStore(private val prefs: SharedPreferences) {
    var appearance by mutableStateOf(load())
        private set

    fun useMindyIcons() = save(appearance.copy(source = IconSource.MINDY, iconPackPackage = null))

    fun useSystemIcons() = save(appearance.copy(source = IconSource.SYSTEM, iconPackPackage = null))

    fun useIconPack(packageName: String) {
        save(appearance.copy(source = IconSource.ICON_PACK, iconPackPackage = packageName))
    }

    fun setMindyIconColor(argb: Int?) {
        save(appearance.copy(mindyIconColorArgb = argb?.or(0xFF000000.toInt())))
    }

    private fun load(): IconAppearance {
        val source = runCatching {
            IconSource.valueOf(prefs.getString(SOURCE_KEY, IconSource.MINDY.name).orEmpty())
        }.getOrDefault(IconSource.MINDY)
        val packageName = prefs.getString(PACKAGE_KEY, null)
        val mindyIconColor = if (prefs.contains(MINDY_COLOR_KEY)) {
            prefs.getInt(MINDY_COLOR_KEY, 0)
        } else {
            null
        }
        return if (source == IconSource.ICON_PACK && packageName.isNullOrBlank()) {
            IconAppearance(mindyIconColorArgb = mindyIconColor)
        } else {
            IconAppearance(source, packageName, mindyIconColor)
        }
    }

    private fun save(value: IconAppearance) {
        appearance = value
        prefs.edit()
            .putString(SOURCE_KEY, value.source.name)
            .apply {
                value.iconPackPackage?.let { putString(PACKAGE_KEY, it) } ?: remove(PACKAGE_KEY)
                value.mindyIconColorArgb?.let { putInt(MINDY_COLOR_KEY, it) } ?: remove(MINDY_COLOR_KEY)
            }
            .apply()
    }

    private companion object {
        const val PACKAGE_KEY = "icon_pack_package"
        const val SOURCE_KEY = "icon_source"
        const val MINDY_COLOR_KEY = "mink_icon_color"
    }
}
