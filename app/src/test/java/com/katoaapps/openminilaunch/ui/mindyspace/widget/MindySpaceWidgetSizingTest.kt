package com.katoaapps.openminilaunch.ui.mindyspace.widget

import android.content.res.Configuration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MindySpaceWidgetSizingTest {
    @Test fun regularPhoneUsesMeasuredPanelProportions() {
        val viewport = mindySpaceWidgetViewport(
            screenWidthDp = 412f,
            screenHeightDp = 915f,
            smallestWidthDp = 412,
            orientation = Configuration.ORIENTATION_PORTRAIT,
            twoPanelModeForLargeDisplays = false,
        )

        assertTrue(viewport.collapsed.width < viewport.expanded.width)
        assertEquals(368f, viewport.expanded.width, .01f)
        assertEquals(321.36f, viewport.expanded.height, .01f)
    }

    @Test fun landscapeTabletUsesLargeDisplayHeightCap() {
        val viewport = mindySpaceWidgetViewport(
            screenWidthDp = 1200f,
            screenHeightDp = 800f,
            smallestWidthDp = 800,
            orientation = Configuration.ORIENTATION_LANDSCAPE,
            twoPanelModeForLargeDisplays = true,
        )

        assertEquals(566f, viewport.expanded.width, .01f)
        assertEquals(420f, viewport.expanded.height, .01f)
    }

    @Test fun compatibilityDistinguishesCollapsedExpandedAndCramped() {
        val viewport = MindySpaceWidgetViewport(
            collapsed = MindySpaceWidgetSizeDp(220f, 280f),
            expanded = MindySpaceWidgetSizeDp(380f, 330f),
        )

        assertEquals(
            MindySpaceWidgetFit.COLLAPSED,
            mindySpaceWidgetFit(200f, 200f, viewport),
        )
        assertEquals(
            MindySpaceWidgetFit.EXPANDED,
            mindySpaceWidgetFit(300f, 300f, viewport),
        )
        assertEquals(
            MindySpaceWidgetFit.CRAMPED,
            mindySpaceWidgetFit(500f, 400f, viewport),
        )
        assertEquals(
            MindySpaceWidgetFit.UNKNOWN,
            mindySpaceWidgetFit(0f, 200f, viewport),
        )
    }
}
