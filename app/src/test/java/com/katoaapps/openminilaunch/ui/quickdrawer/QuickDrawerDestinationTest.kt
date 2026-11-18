package com.katoaapps.openminilaunch.ui.quickdrawer

import org.junit.Assert.assertEquals
import org.junit.Test

class QuickDrawerDestinationTest {
    @Test
    fun `available navigation follows the requested order`() {
        assertEquals(
            listOf(
                QuickDrawerDestination.ALL_APPS,
                QuickDrawerDestination.PRIVATE_SPACE,
                QuickDrawerDestination.WORK_SPACE,
            ),
            quickDrawerDestinations(
                showPrivateSpace = true,
                privateSpaceAvailable = true,
                showWorkSpace = true,
                workSpaceAvailable = true,
            ),
        )
    }

    @Test
    fun `optional destinations require visibility and availability`() {
        assertEquals(
            listOf(QuickDrawerDestination.ALL_APPS),
            quickDrawerDestinations(
                showPrivateSpace = true,
                privateSpaceAvailable = false,
                showWorkSpace = false,
                workSpaceAvailable = true,
            ),
        )
    }
}
