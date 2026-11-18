package com.katoaapps.openminilaunch.ui.mindyspace.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.ui.widgets.WidgetProviderBadges
import kotlin.math.roundToInt

internal data class MindySpaceWidgetSizeDp(
    val width: Float,
    val height: Float,
)

internal data class MindySpaceWidgetViewport(
    val collapsed: MindySpaceWidgetSizeDp,
    val expanded: MindySpaceWidgetSizeDp,
)

internal enum class MindySpaceWidgetFit(@param:StringRes val labelRes: Int) {
    COLLAPSED(R.string.widget_fits_mindy_space),
    EXPANDED(R.string.widget_fits_expanded),
    CRAMPED(R.string.widget_may_be_cramped),
    UNKNOWN(R.string.widget_fit_unknown),
}

@Composable
internal fun rememberMindySpaceWidgetViewport(
    twoPanelModeForLargeDisplays: Boolean,
): MindySpaceWidgetViewport {
    val configuration = LocalConfiguration.current
    return remember(configuration, twoPanelModeForLargeDisplays) {
        mindySpaceWidgetViewport(
            screenWidthDp = configuration.screenWidthDp.toFloat(),
            screenHeightDp = configuration.screenHeightDp.toFloat(),
            smallestWidthDp = configuration.smallestScreenWidthDp,
            orientation = configuration.orientation,
            twoPanelModeForLargeDisplays = twoPanelModeForLargeDisplays,
        )
    }
}

internal fun mindySpaceWidgetViewport(
    screenWidthDp: Float,
    screenHeightDp: Float,
    smallestWidthDp: Int,
    orientation: Int,
    twoPanelModeForLargeDisplays: Boolean,
): MindySpaceWidgetViewport {
    val twoPanel = twoPanelModeForLargeDisplays &&
        smallestWidthDp >= 600 && orientation == Configuration.ORIENTATION_LANDSCAPE
    val homePaneWidth = if (twoPanel) {
        ((screenWidthDp - 12f) / 2f).coerceAtLeast(1f)
    } else {
        screenWidthDp
    }
    val compact = screenHeightDp <= homePaneWidth * 1.55f
    val homeHorizontalPadding = if (compact) 14f else 22f
    val panelWidth = (homePaneWidth - homeHorizontalPadding * 2f).coerceIn(1f, 620f)
    val panelHeight = when {
        twoPanel -> (screenHeightDp * .7f).coerceIn(350f, 420f)
        compact -> (screenHeightDp - 150f).coerceAtLeast(180f)
        else -> (screenWidthDp * .78f).coerceIn(310f, 350f)
    }.coerceAtMost(screenHeightDp)
    val rowPadding = if (compact) 10f else 14f
    val rowSpacing = if (compact) 8f else 12f
    val rowContentWidth = (panelWidth - rowPadding * 2f - rowSpacing).coerceAtLeast(1f)
    val collapsedWidth = (rowContentWidth * 2f / 3f).coerceAtLeast(96f)
    val collapsedHeight = (panelHeight - rowPadding * 2f).coerceAtLeast(96f)

    return MindySpaceWidgetViewport(
        collapsed = MindySpaceWidgetSizeDp(collapsedWidth, collapsedHeight),
        expanded = MindySpaceWidgetSizeDp(panelWidth, panelHeight),
    )
}

internal fun mindySpaceWidgetFit(
    info: AppWidgetProviderInfo,
    density: Float,
    viewport: MindySpaceWidgetViewport,
): MindySpaceWidgetFit {
    if (density <= 0f) return MindySpaceWidgetFit.UNKNOWN
    val horizontallyResizable =
        info.resizeMode and AppWidgetProviderInfo.RESIZE_HORIZONTAL != 0
    val verticallyResizable =
        info.resizeMode and AppWidgetProviderInfo.RESIZE_VERTICAL != 0
    val minWidthPx = if (horizontallyResizable && info.minResizeWidth > 0) {
        info.minResizeWidth
    } else {
        info.minWidth
    }
    val minHeightPx = if (verticallyResizable && info.minResizeHeight > 0) {
        info.minResizeHeight
    } else {
        info.minHeight
    }
    if (minWidthPx <= 0 || minHeightPx <= 0) return MindySpaceWidgetFit.UNKNOWN

    val minWidthDp = minWidthPx / density
    val minHeightDp = minHeightPx / density
    return mindySpaceWidgetFit(minWidthDp, minHeightDp, viewport)
}

internal fun mindySpaceWidgetFit(
    minWidthDp: Float,
    minHeightDp: Float,
    viewport: MindySpaceWidgetViewport,
): MindySpaceWidgetFit {
    if (minWidthDp <= 0f || minHeightDp <= 0f) return MindySpaceWidgetFit.UNKNOWN
    return when {
        minWidthDp <= viewport.collapsed.width && minHeightDp <= viewport.collapsed.height ->
            MindySpaceWidgetFit.COLLAPSED
        minWidthDp <= viewport.expanded.width && minHeightDp <= viewport.expanded.height ->
            MindySpaceWidgetFit.EXPANDED
        else -> MindySpaceWidgetFit.CRAMPED
    }
}

internal fun mindySpaceWidgetBadges(
    info: AppWidgetProviderInfo,
    density: Float,
    viewport: MindySpaceWidgetViewport,
): WidgetProviderBadges = WidgetProviderBadges(
    fitLabelRes = mindySpaceWidgetFit(info, density, viewport).labelRes,
    needsSetup = info.configure != null,
)

internal fun mindySpaceWidgetBindingOptions(viewport: MindySpaceWidgetViewport): Bundle {
    val collapsed = viewport.collapsed
    val expanded = viewport.expanded
    return Bundle().apply {
        putInt(
            AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY,
            AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN,
        )
        putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, collapsed.width.roundToInt())
        putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, collapsed.height.roundToInt())
        putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, expanded.width.roundToInt())
        putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, expanded.height.roundToInt())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            putParcelableArrayList(
                AppWidgetManager.OPTION_APPWIDGET_SIZES,
                arrayListOf(
                    SizeF(collapsed.width, collapsed.height),
                    SizeF(expanded.width, expanded.height),
                ).distinctBy { size -> size.width to size.height }.let(::ArrayList),
            )
        }
    }
}
