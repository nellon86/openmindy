package com.katoaapps.openminilaunch.ui.minkspace.core

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Widgets
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.minkspace.MinkSpaceMiniApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class MinkSpacePresentationStateTest {
    @Test fun everyPersistedIdentityHasOneMatchingManifest() {
        val resolved = MinkSpaceMiniApp.entries.map { miniApp ->
            MinkSpaceMiniAppCatalog.get(miniApp).miniApp
        }

        assertEquals(MinkSpaceMiniApp.entries, resolved)
    }

    @Test fun catalogDeclaresExpansionCapability() {
        assertFalse(MinkSpaceMiniAppCatalog.get(MinkSpaceMiniApp.TODO).supportsExpansion)
        assertFalse(MinkSpaceMiniAppCatalog.get(MinkSpaceMiniApp.CALCULATOR).supportsExpansion)
        assertTrue(MinkSpaceMiniAppCatalog.get(MinkSpaceMiniApp.MUSIC).supportsExpansion)
        assertTrue(MinkSpaceMiniAppCatalog.get(MinkSpaceMiniApp.WIDGET).supportsExpansion)
        assertTrue(MinkSpaceMiniAppCatalog.get(MinkSpaceMiniApp.WIDGET_2).supportsExpansion)
        assertTrue(MinkSpaceMiniAppCatalog.get(MinkSpaceMiniApp.WIDGET_3).supportsExpansion)
        assertTrue(MinkSpaceMiniAppCatalog.get(MinkSpaceMiniApp.WIDGET).showExpansionButton)
        assertTrue(MinkSpaceMiniAppCatalog.get(MinkSpaceMiniApp.WIDGET_3).showExpansionButton)
        assertFalse(MinkSpaceMiniAppCatalog.get(MinkSpaceMiniApp.MUSIC).showExpansionButton)
    }

    @Test fun catalogDeclaresNoTotalScreenMiniAppsAfterMemoriesRemoval() {
        assertEquals(emptyList<MinkSpaceMiniApp>(), MinkSpaceMiniAppCatalog.totalScreenMiniApps)
        assertFalse(MinkSpaceMiniAppCatalog.get(MinkSpaceMiniApp.MUSIC).supportsTotalScreen)
    }

    @Test fun eachQuickWidgetSlotHasItsOwnLabel() {
        val labels = MinkSpaceMiniApp.quickWidgets.map { MinkSpaceMiniAppCatalog.get(it).labelRes }
        assertEquals(3, labels.toSet().size)
    }

    @Test fun expandedPresentationLastsThroughTheCollapseAnimation() {
        assertTrue(shouldPresentExpandedContent(expanded = true, appGridAlpha = 1f))
        assertTrue(shouldPresentExpandedContent(expanded = false, appGridAlpha = 0f))
        assertTrue(shouldPresentExpandedContent(expanded = false, appGridAlpha = .75f))
        assertFalse(shouldPresentExpandedContent(expanded = false, appGridAlpha = 1f))
    }

    @Test fun standardExpansionButtonRequiresExpansionSupport() {
        val widget = MinkSpaceMiniAppCatalog.get(MinkSpaceMiniApp.WIDGET)

        assertThrows(IllegalArgumentException::class.java) {
            widget.copy(supportsExpansion = false)
        }
    }

    @Test fun customExpansionLabelRequiresStandardButton() {
        val widget = MinkSpaceMiniAppCatalog.get(MinkSpaceMiniApp.WIDGET)

        assertThrows(IllegalArgumentException::class.java) {
            widget.copy(
                showExpansionButton = false,
                expansionButtonLabelRes = R.string.expand_view,
            )
        }
    }

    @Test fun disabledControllerCannotExpand() {
        val state = MinkSpacePresentationState()
        val todo = state.expansionController(MinkSpaceMiniAppCatalog.get(MinkSpaceMiniApp.TODO))

        todo.expand()

        assertFalse(todo.enabled)
        assertFalse(todo.expanded)
        assertNull(state.expandedMiniApp)
    }

    @Test fun disabledMiniAppCannotRestoreAsExpanded() {
        val state = MinkSpacePresentationState(MinkSpaceMiniApp.TODO)

        assertNull(state.expandedMiniApp)
        assertFalse(state.isExpanded)
    }

    @Test fun miniAppControlsItsOwnExpansionState() {
        val state = MinkSpacePresentationState()
        val music = state.expansionController(MinkSpaceMiniAppCatalog.get(MinkSpaceMiniApp.MUSIC))

        music.expand()
        assertTrue(music.expanded)
        assertEquals(MinkSpaceMiniApp.MUSIC, state.expandedMiniApp)

        music.collapse()
        assertFalse(music.expanded)
        assertNull(state.expandedMiniApp)
    }

    @Test fun expandingAnotherMiniAppTransfersShellOwnership() {
        val state = MinkSpacePresentationState()
        val music = state.expansionController(MinkSpaceMiniAppCatalog.get(MinkSpaceMiniApp.MUSIC))
        val widget = state.expansionController(MinkSpaceMiniAppCatalog.get(MinkSpaceMiniApp.WIDGET))

        music.expand()
        widget.expand()

        assertFalse(music.expanded)
        assertTrue(widget.expanded)
        assertEquals(MinkSpaceMiniApp.WIDGET, state.expandedMiniApp)
    }

    @Test fun leavingExpandedPageRestoresCollapsedShell() {
        val state = MinkSpacePresentationState(MinkSpaceMiniApp.WIDGET_2)

        state.retainOnly(MinkSpaceMiniApp.MUSIC)

        assertNull(state.expandedMiniApp)
    }

    @Test fun totalScreenRequestTransfersShellOwnership() {
        val state = MinkSpacePresentationState()
        val music = state.expansionController(MinkSpaceMiniAppCatalog.get(MinkSpaceMiniApp.MUSIC))
        val media = state.expansionController(totalScreenTestManifest)

        music.expand()
        media.showTotalScreen()

        assertFalse(music.expanded)
        assertTrue(media.totalScreen)
        assertEquals(totalScreenTestManifest.miniApp, state.totalScreenMiniApp)
        assertNull(state.expandedMiniApp)
    }

    @Test fun unsupportedMiniAppCannotTakeTotalScreenOwnership() {
        val state = MinkSpacePresentationState()
        val music = state.expansionController(MinkSpaceMiniAppCatalog.get(MinkSpaceMiniApp.MUSIC))

        music.showTotalScreen()

        assertFalse(music.totalScreenEnabled)
        assertFalse(state.isTotalScreen)
    }

    @Test fun pagerChangesDoNotDismissTotalScreenPresentation() {
        val state = MinkSpacePresentationState()
        val media = state.expansionController(totalScreenTestManifest)

        media.showTotalScreen()
        state.retainOnly(MinkSpaceMiniApp.TODO)

        assertEquals(totalScreenTestManifest.miniApp, state.totalScreenMiniApp)
    }

    /** The shell stays generic; this stand-in exercises it now that no built-in app uses it. */
    private val totalScreenTestManifest = MinkSpaceMiniAppManifest(
        miniApp = MinkSpaceMiniApp.CALCULATOR,
        labelRes = 0,
        icon = Icons.Default.Widgets,
        supportsExpansion = true,
        supportsTotalScreen = true,
    )
}
