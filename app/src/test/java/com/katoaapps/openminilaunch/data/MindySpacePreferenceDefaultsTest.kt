package com.katoaapps.openminilaunch.data

import com.katoaapps.openminilaunch.features.mindyspace.MindySpaceMiniApp
import org.junit.Assert.assertEquals
import org.junit.Test

class MindySpacePreferenceDefaultsTest {
    @Test
    fun missingPreferenceStartsWithTodoEnabled() {
        assertEquals(
            setOf(MindySpaceMiniApp.TODO),
            enabledMindySpaceMiniAppsFromStableIds(null),
        )
    }

    @Test
    fun removedUpstreamMemoriesAndVCardIdsAreIgnored() {
        assertEquals(
            setOf(MindySpaceMiniApp.TODO, MindySpaceMiniApp.WIDGET),
            enabledMindySpaceMiniAppsFromStableIds(setOf("todo", "media", "vcard", "widget")),
        )
    }

    @Test
    fun threeQuickWidgetSlotsHaveStableIds() {
        assertEquals(
            listOf("widget", "widget_2", "widget_3"),
            MindySpaceMiniApp.quickWidgets.map(MindySpaceMiniApp::stableId),
        )
    }

    @Test
    fun savedEmptyPreferenceKeepsEveryMiniAppDisabled() {
        assertEquals(
            emptySet<MindySpaceMiniApp>(),
            enabledMindySpaceMiniAppsFromStableIds(emptySet()),
        )
    }

    @Test
    fun savedPreferenceDoesNotReEnableTodo() {
        assertEquals(
            setOf(MindySpaceMiniApp.MUSIC),
            enabledMindySpaceMiniAppsFromStableIds(setOf("music")),
        )
    }

    @Test
    fun unavailableAndUnknownMiniAppsAreIgnored() {
        assertEquals(
            setOf(MindySpaceMiniApp.CALCULATOR),
            enabledMindySpaceMiniAppsFromStableIds(setOf("calculator", "recorder", "future-app")),
        )
    }

    @Test
    fun visibleReorderKeepsUnavailableMiniAppsInTheirExistingSlots() {
        assertEquals(
            listOf(
                MindySpaceMiniApp.WIDGET,
                MindySpaceMiniApp.TODO,
                MindySpaceMiniApp.RECORDER,
                MindySpaceMiniApp.CALCULATOR,
                MindySpaceMiniApp.MUSIC,
                MindySpaceMiniApp.WIDGET_2,
                MindySpaceMiniApp.WIDGET_3,
            ),
            mergeVisibleMindySpaceMiniAppOrder(
                currentOrder = MindySpaceMiniApp.defaultOrder,
                requestedVisibleOrder = listOf(
                    MindySpaceMiniApp.WIDGET,
                    MindySpaceMiniApp.TODO,
                    MindySpaceMiniApp.CALCULATOR,
                    MindySpaceMiniApp.MUSIC,
                    MindySpaceMiniApp.WIDGET_2,
                    MindySpaceMiniApp.WIDGET_3,
                ),
            ),
        )
    }

    @Test
    fun portableRestoreKeepsEveryQuickWidgetInItsCurrentSlot() {
        val currentOrder = listOf(
            MindySpaceMiniApp.TODO,
            MindySpaceMiniApp.WIDGET,
            MindySpaceMiniApp.CALCULATOR,
            MindySpaceMiniApp.RECORDER,
            MindySpaceMiniApp.WIDGET_3,
            MindySpaceMiniApp.MUSIC,
            MindySpaceMiniApp.WIDGET_2,
        )

        assertEquals(
            listOf(
                MindySpaceMiniApp.MUSIC,
                MindySpaceMiniApp.WIDGET,
                MindySpaceMiniApp.CALCULATOR,
                MindySpaceMiniApp.RECORDER,
                MindySpaceMiniApp.WIDGET_3,
                MindySpaceMiniApp.TODO,
                MindySpaceMiniApp.WIDGET_2,
            ),
            mergePortableMindySpaceMiniAppOrder(
                currentOrder = currentOrder,
                restoredOrder = listOf(
                    MindySpaceMiniApp.WIDGET_2,
                    MindySpaceMiniApp.MUSIC,
                    MindySpaceMiniApp.CALCULATOR,
                    MindySpaceMiniApp.TODO,
                ),
            ),
        )
    }

    @Test
    fun portableRestoreKeepsConfiguredWidgetEnabled() {
        assertEquals(
            setOf(MindySpaceMiniApp.MUSIC, MindySpaceMiniApp.WIDGET, MindySpaceMiniApp.WIDGET_3),
            mergePortableMindySpaceEnabledMiniApps(
                currentEnabledMiniApps = setOf(
                    MindySpaceMiniApp.TODO,
                    MindySpaceMiniApp.WIDGET,
                    MindySpaceMiniApp.WIDGET_3,
                ),
                restoredEnabledMiniApps = setOf(MindySpaceMiniApp.MUSIC),
            ),
        )
    }

    @Test
    fun portableRestoreDoesNotEnableWidgetFromAnotherInstallation() {
        assertEquals(
            setOf(MindySpaceMiniApp.MUSIC),
            mergePortableMindySpaceEnabledMiniApps(
                currentEnabledMiniApps = setOf(MindySpaceMiniApp.TODO),
                restoredEnabledMiniApps = setOf(MindySpaceMiniApp.MUSIC, MindySpaceMiniApp.WIDGET),
            ),
        )
    }
}
