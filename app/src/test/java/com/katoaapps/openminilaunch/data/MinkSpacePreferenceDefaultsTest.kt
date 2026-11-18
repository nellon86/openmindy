package com.katoaapps.openminilaunch.data

import com.katoaapps.openminilaunch.features.minkspace.MinkSpaceMiniApp
import org.junit.Assert.assertEquals
import org.junit.Test

class MinkSpacePreferenceDefaultsTest {
    @Test
    fun missingPreferenceStartsWithTodoEnabled() {
        assertEquals(
            setOf(MinkSpaceMiniApp.TODO),
            enabledMinkSpaceMiniAppsFromStableIds(null),
        )
    }

    @Test
    fun removedUpstreamMemoriesAndVCardIdsAreIgnored() {
        assertEquals(
            setOf(MinkSpaceMiniApp.TODO, MinkSpaceMiniApp.WIDGET),
            enabledMinkSpaceMiniAppsFromStableIds(setOf("todo", "media", "vcard", "widget")),
        )
    }

    @Test
    fun threeQuickWidgetSlotsHaveStableIds() {
        assertEquals(
            listOf("widget", "widget_2", "widget_3"),
            MinkSpaceMiniApp.quickWidgets.map(MinkSpaceMiniApp::stableId),
        )
    }

    @Test
    fun savedEmptyPreferenceKeepsEveryMiniAppDisabled() {
        assertEquals(
            emptySet<MinkSpaceMiniApp>(),
            enabledMinkSpaceMiniAppsFromStableIds(emptySet()),
        )
    }

    @Test
    fun savedPreferenceDoesNotReEnableTodo() {
        assertEquals(
            setOf(MinkSpaceMiniApp.MUSIC),
            enabledMinkSpaceMiniAppsFromStableIds(setOf("music")),
        )
    }

    @Test
    fun unavailableAndUnknownMiniAppsAreIgnored() {
        assertEquals(
            setOf(MinkSpaceMiniApp.CALCULATOR),
            enabledMinkSpaceMiniAppsFromStableIds(setOf("calculator", "recorder", "future-app")),
        )
    }

    @Test
    fun visibleReorderKeepsUnavailableMiniAppsInTheirExistingSlots() {
        assertEquals(
            listOf(
                MinkSpaceMiniApp.WIDGET,
                MinkSpaceMiniApp.TODO,
                MinkSpaceMiniApp.RECORDER,
                MinkSpaceMiniApp.CALCULATOR,
                MinkSpaceMiniApp.MUSIC,
                MinkSpaceMiniApp.WIDGET_2,
                MinkSpaceMiniApp.WIDGET_3,
            ),
            mergeVisibleMinkSpaceMiniAppOrder(
                currentOrder = MinkSpaceMiniApp.defaultOrder,
                requestedVisibleOrder = listOf(
                    MinkSpaceMiniApp.WIDGET,
                    MinkSpaceMiniApp.TODO,
                    MinkSpaceMiniApp.CALCULATOR,
                    MinkSpaceMiniApp.MUSIC,
                    MinkSpaceMiniApp.WIDGET_2,
                    MinkSpaceMiniApp.WIDGET_3,
                ),
            ),
        )
    }

    @Test
    fun portableRestoreKeepsEveryQuickWidgetInItsCurrentSlot() {
        val currentOrder = listOf(
            MinkSpaceMiniApp.TODO,
            MinkSpaceMiniApp.WIDGET,
            MinkSpaceMiniApp.CALCULATOR,
            MinkSpaceMiniApp.RECORDER,
            MinkSpaceMiniApp.WIDGET_3,
            MinkSpaceMiniApp.MUSIC,
            MinkSpaceMiniApp.WIDGET_2,
        )

        assertEquals(
            listOf(
                MinkSpaceMiniApp.MUSIC,
                MinkSpaceMiniApp.WIDGET,
                MinkSpaceMiniApp.CALCULATOR,
                MinkSpaceMiniApp.RECORDER,
                MinkSpaceMiniApp.WIDGET_3,
                MinkSpaceMiniApp.TODO,
                MinkSpaceMiniApp.WIDGET_2,
            ),
            mergePortableMinkSpaceMiniAppOrder(
                currentOrder = currentOrder,
                restoredOrder = listOf(
                    MinkSpaceMiniApp.WIDGET_2,
                    MinkSpaceMiniApp.MUSIC,
                    MinkSpaceMiniApp.CALCULATOR,
                    MinkSpaceMiniApp.TODO,
                ),
            ),
        )
    }

    @Test
    fun portableRestoreKeepsConfiguredWidgetEnabled() {
        assertEquals(
            setOf(MinkSpaceMiniApp.MUSIC, MinkSpaceMiniApp.WIDGET, MinkSpaceMiniApp.WIDGET_3),
            mergePortableMinkSpaceEnabledMiniApps(
                currentEnabledMiniApps = setOf(
                    MinkSpaceMiniApp.TODO,
                    MinkSpaceMiniApp.WIDGET,
                    MinkSpaceMiniApp.WIDGET_3,
                ),
                restoredEnabledMiniApps = setOf(MinkSpaceMiniApp.MUSIC),
            ),
        )
    }

    @Test
    fun portableRestoreDoesNotEnableWidgetFromAnotherInstallation() {
        assertEquals(
            setOf(MinkSpaceMiniApp.MUSIC),
            mergePortableMinkSpaceEnabledMiniApps(
                currentEnabledMiniApps = setOf(MinkSpaceMiniApp.TODO),
                restoredEnabledMiniApps = setOf(MinkSpaceMiniApp.MUSIC, MinkSpaceMiniApp.WIDGET),
            ),
        )
    }
}
