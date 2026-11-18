package com.katoaapps.openminilaunch

import com.katoaapps.openminilaunch.data.normalizedSocialGoalMinutes
import com.katoaapps.openminilaunch.features.wellbeing.shouldPauseMindyApp
import com.katoaapps.openminilaunch.features.wellbeing.shouldPauseLauncherApp
import com.katoaapps.openminilaunch.model.MindyAppPauseMode
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class MindyAppPausePolicyTest {
    private val tracked = setOf("example.social")

    @Test
    fun alwaysPausesOnlyTrackedApps() {
        assertTrue(paused("example.social", MindyAppPauseMode.ALWAYS))
        assertFalse(paused("example.maps", MindyAppPauseMode.ALWAYS))
    }

    @Test
    fun afterLimitWaitsForCollectiveUsage() {
        assertFalse(
            paused(
                packageName = "example.social",
                mode = MindyAppPauseMode.AFTER_DAILY_LIMIT,
                usageMillis = 59 * 60_000L,
                limitMinutes = 60,
            ),
        )
        assertTrue(
            paused(
                packageName = "example.social",
                mode = MindyAppPauseMode.AFTER_DAILY_LIMIT,
                usageMillis = 60 * 60_000L,
                limitMinutes = 60,
            ),
        )
    }

    @Test
    fun afterLimitRequiresUsageAccess() {
        assertFalse(
            paused(
                packageName = "example.social",
                mode = MindyAppPauseMode.AFTER_DAILY_LIMIT,
                usageMillis = 10 * 60 * 60_000L,
                usageAccessGranted = false,
            ),
        )
    }

    @Test
    fun zeroHourLimitPausesImmediately() {
        assertTrue(
            paused(
                packageName = "example.social",
                mode = MindyAppPauseMode.AFTER_DAILY_LIMIT,
                usageMillis = 0,
                limitMinutes = 0,
            ),
        )
    }

    @Test
    fun neverModeDoesNotPauseTrackedApps() {
        assertFalse(paused("example.social", MindyAppPauseMode.NEVER))
    }

    @Test
    fun workProfileCopyIsNeverPausedByMindyDay() {
        assertFalse(
            shouldPauseLauncherApp(
                packageName = "example.social",
                isWorkProfile = true,
                trackedPackages = tracked,
                mode = MindyAppPauseMode.ALWAYS,
                collectiveUsageMillis = 0,
                dailyLimitMinutes = 0,
                usageAccessGranted = true,
            ),
        )
    }

    @Test
    fun savedGoalsMigrateToWholeHoursWithinRange() {
        assertEquals(60, normalizedSocialGoalMinutes(30))
        assertEquals(120, normalizedSocialGoalMinutes(90))
        assertEquals(0, normalizedSocialGoalMinutes(-20))
        assertEquals(23 * 60, normalizedSocialGoalMinutes(40 * 60))
    }

    private fun paused(
        packageName: String,
        mode: MindyAppPauseMode,
        usageMillis: Long = 0,
        limitMinutes: Int = 60,
        usageAccessGranted: Boolean = true,
    ): Boolean = shouldPauseMindyApp(
        packageName = packageName,
        trackedPackages = tracked,
        mode = mode,
        collectiveUsageMillis = usageMillis,
        dailyLimitMinutes = limitMinutes,
        usageAccessGranted = usageAccessGranted,
    )
}
