package com.katoaapps.openminilaunch.ui.mindyspace.core

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Widgets
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.mindyspace.MindySpaceMiniApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class MindySpacePresentationStateTest {
    @Test fun everyPersistedIdentityHasOneMatchingManifest() {
        val resolved = MindySpaceMiniApp.entries.map { miniApp ->
            MindySpaceMiniAppCatalog.get(miniApp).miniApp
        }

        assertEquals(MindySpaceMiniApp.entries, resolved)
    }

    @Test fun catalogDeclaresExpansionCapability() {
        assertFalse(MindySpaceMiniAppCatalog.get(MindySpaceMiniApp.TODO).supportsExpansion)
        assertFalse(MindySpaceMiniAppCatalog.get(MindySpaceMiniApp.CALCULATOR).supportsExpansion)
        assertTrue(MindySpaceMiniAppCatalog.get(MindySpaceMiniApp.MUSIC).supportsExpansion)
        assertTrue(MindySpaceMiniAppCatalog.get(MindySpaceMiniApp.WIDGET).supportsExpansion)
        assertTrue(MindySpaceMiniAppCatalog.get(MindySpaceMiniApp.WIDGET_2).supportsExpansion)
        assertTrue(MindySpaceMiniAppCatalog.get(MindySpaceMiniApp.WIDGET_3).supportsExpansion)
        assertTrue(MindySpaceMiniAppCatalog.get(MindySpaceMiniApp.WIDGET).showExpansionButton)
        assertTrue(MindySpaceMiniAppCatalog.get(MindySpaceMiniApp.WIDGET_3).showExpansionButton)
        assertFalse(MindySpaceMiniAppCatalog.get(MindySpaceMiniApp.MUSIC).showExpansionButton)
    }

    @Test fun catalogDeclaresNoTotalScreenMiniAppsAfterMemoriesRemoval() {
        assertEquals(emptyList<MindySpaceMiniApp>(), MindySpaceMiniAppCatalog.totalScreenMiniApps)
        assertFalse(MindySpaceMiniAppCatalog.get(MindySpaceMiniApp.MUSIC).supportsTotalScreen)
    }

    @Test fun eachQuickWidgetSlotHasItsOwnLabel() {
        val labels = MindySpaceMiniApp.quickWidgets.map { MindySpaceMiniAppCatalog.get(it).labelRes }
        assertEquals(3, labels.toSet().size)
    }

    @Test fun expandedPresentationLastsThroughTheCollapseAnimation() {
        assertTrue(shouldPresentExpandedContent(expanded = true, appGridAlpha = 1f))
        assertTrue(shouldPresentExpandedContent(expanded = false, appGridAlpha = 0f))
        assertTrue(shouldPresentExpandedContent(expanded = false, appGridAlpha = .75f))
        assertFalse(shouldPresentExpandedContent(expanded = false, appGridAlpha = 1f))
    }

    @Test fun standardExpansionButtonRequiresExpansionSupport() {
        val widget = MindySpaceMiniAppCatalog.get(MindySpaceMiniApp.WIDGET)

        assertThrows(IllegalArgumentException::class.java) {
            widget.copy(supportsExpansion = false)
        }
    }

    @Test fun customExpansionLabelRequiresStandardButton() {
        val widget = MindySpaceMiniAppCatalog.get(MindySpaceMiniApp.WIDGET)

        assertThrows(IllegalArgumentException::class.java) {
            widget.copy(
                showExpansionButton = false,
                expansionButtonLabelRes = R.string.expand_view,
            )
        }
    }

    @Test fun disabledControllerCannotExpand() {
        val state = MindySpacePresentationState()
        val todo = state.expansionController(MindySpaceMiniAppCatalog.get(MindySpaceMiniApp.TODO))

        todo.expand()

        assertFalse(todo.enabled)
        assertFalse(todo.expanded)
        assertNull(state.expandedMiniApp)
    }

    @Test fun disabledMiniAppCannotRestoreAsExpanded() {
        val state = MindySpacePresentationState(MindySpaceMiniApp.TODO)

        assertNull(state.expandedMiniApp)
        assertFalse(state.isExpanded)
    }

    @Test fun miniAppControlsItsOwnExpansionState() {
        val state = MindySpacePresentationState()
        val music = state.expansionController(MindySpaceMiniAppCatalog.get(MindySpaceMiniApp.MUSIC))

        music.expand()
        assertTrue(music.expanded)
        assertEquals(MindySpaceMiniApp.MUSIC, state.expandedMiniApp)

        music.collapse()
        assertFalse(music.expanded)
        assertNull(state.expandedMiniApp)
    }

    @Test fun expandingAnotherMiniAppTransfersShellOwnership() {
        val state = MindySpacePresentationState()
        val music = state.expansionController(MindySpaceMiniAppCatalog.get(MindySpaceMiniApp.MUSIC))
        val widget = state.expansionController(MindySpaceMiniAppCatalog.get(MindySpaceMiniApp.WIDGET))

        music.expand()
        widget.expand()

        assertFalse(music.expanded)
        assertTrue(widget.expanded)
        assertEquals(MindySpaceMiniApp.WIDGET, state.expandedMiniApp)
    }

    @Test fun leavingExpandedPageRestoresCollapsedShell() {
        val state = MindySpacePresentationState(MindySpaceMiniApp.WIDGET_2)

        state.retainOnly(MindySpaceMiniApp.MUSIC)

        assertNull(state.expandedMiniApp)
    }

    @Test fun totalScreenRequestTransfersShellOwnership() {
        val state = MindySpacePresentationState()
        val music = state.expansionController(MindySpaceMiniAppCatalog.get(MindySpaceMiniApp.MUSIC))
        val media = state.expansionController(totalScreenTestManifest)

        music.expand()
        media.showTotalScreen()

        assertFalse(music.expanded)
        assertTrue(media.totalScreen)
        assertEquals(totalScreenTestManifest.miniApp, state.totalScreenMiniApp)
        assertNull(state.expandedMiniApp)
    }

    @Test fun unsupportedMiniAppCannotTakeTotalScreenOwnership() {
        val state = MindySpacePresentationState()
        val music = state.expansionController(MindySpaceMiniAppCatalog.get(MindySpaceMiniApp.MUSIC))

        music.showTotalScreen()

        assertFalse(music.totalScreenEnabled)
        assertFalse(state.isTotalScreen)
    }

    @Test fun pagerChangesDoNotDismissTotalScreenPresentation() {
        val state = MindySpacePresentationState()
        val media = state.expansionController(totalScreenTestManifest)

        media.showTotalScreen()
        state.retainOnly(MindySpaceMiniApp.TODO)

        assertEquals(totalScreenTestManifest.miniApp, state.totalScreenMiniApp)
    }

    /** The shell stays generic; this stand-in exercises it now that no built-in app uses it. */
    private val totalScreenTestManifest = MindySpaceMiniAppManifest(
        miniApp = MindySpaceMiniApp.CALCULATOR,
        labelRes = 0,
        icon = Icons.Default.Widgets,
        supportsExpansion = true,
        supportsTotalScreen = true,
    )
}
