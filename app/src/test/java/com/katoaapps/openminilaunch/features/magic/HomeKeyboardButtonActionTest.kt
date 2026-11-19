package com.katoaapps.openminilaunch.features.magic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeKeyboardButtonActionTest {
    @Test fun storageIdsAreUniqueAndRoundTrip() {
        val ids = HomeKeyboardButtonAction.entries.map { it.storageId }
        assertEquals(ids.size, ids.toSet().size)
        HomeKeyboardButtonAction.entries.forEach { action ->
            assertEquals(action, HomeKeyboardButtonAction.fromStorageId(action.storageId))
        }
    }

    @Test fun defaultsKeepTodaysBehaviour() {
        val bindings = HomeKeyboardButtonBindings()
        assertEquals(HomeKeyboardButtonAction.MAGIC_BOX, bindings.tap.action)
        assertEquals(HomeKeyboardButtonAction.MINDY_SPACE_FULL_SCREEN, bindings.longPress.action)
        assertFalse(bindings.swipeUp.isActive)
    }

    @Test fun launchAppNeedsATarget() {
        assertFalse(HomeKeyboardButtonBinding(HomeKeyboardButtonAction.LAUNCH_APP).isActive)
        assertTrue(HomeKeyboardButtonBinding(HomeKeyboardButtonAction.LAUNCH_APP, "app:x").isActive)
    }

    @Test fun withReplacesOnlyOneGesture() {
        val updated = HomeKeyboardButtonBindings().with(
            HomeKeyboardButtonGesture.SWIPE_UP,
            HomeKeyboardButtonBinding(HomeKeyboardButtonAction.FLASHLIGHT),
        )
        assertEquals(HomeKeyboardButtonAction.FLASHLIGHT, updated[HomeKeyboardButtonGesture.SWIPE_UP].action)
        assertEquals(HomeKeyboardButtonAction.MAGIC_BOX, updated[HomeKeyboardButtonGesture.TAP].action)
    }
}
