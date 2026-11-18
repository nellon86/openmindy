package com.katoaapps.openminilaunch.features.wellbeing

import java.time.LocalDate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MindyDaySummaryCacheTest {
    private val day = LocalDate.of(2026, 11, 18)
    private val key = MindyDaySummaryCache.inputsKey(setOf("b.app", "a.app"), true, 30)
    private val summary = MindyDaySummary(
        accessGranted = true,
        state = MindyState.WALKING,
        headline = "Out walking",
        detail = "",
    )

    @After fun clear() = MindyDaySummaryCache.clearForTest()

    @Test fun summaryStaysFreshForThreeHours() {
        MindyDaySummaryCache.store(key, summary, nowElapsedMillis = 1_000L, today = day)

        assertTrue(MindyDaySummaryCache.isFresh(key, 1_000L + MINDY_DAY_SUMMARY_MAX_AGE_MILLIS - 1, day))
        assertFalse(MindyDaySummaryCache.isFresh(key, 1_000L + MINDY_DAY_SUMMARY_MAX_AGE_MILLIS, day))
        assertEquals(summary, MindyDaySummaryCache.latest(key, day))
    }

    @Test fun newDayOrDifferentSettingsNeedANewSummary() {
        MindyDaySummaryCache.store(key, summary, nowElapsedMillis = 1_000L, today = day)
        val otherGoal = MindyDaySummaryCache.inputsKey(setOf("a.app", "b.app"), true, 45)

        assertFalse(MindyDaySummaryCache.isFresh(key, 2_000L, day.plusDays(1)))
        assertNull(MindyDaySummaryCache.latest(key, day.plusDays(1)))
        assertFalse(MindyDaySummaryCache.isFresh(otherGoal, 2_000L, day))
        assertNull(MindyDaySummaryCache.latest(otherGoal, day))
    }

    @Test fun packageOrderDoesNotChangeTheSettingsKey() {
        assertEquals(key, MindyDaySummaryCache.inputsKey(setOf("a.app", "b.app"), true, 30))
    }

    @Test fun summariesWithoutUsageAccessAreNotCached() {
        MindyDaySummaryCache.store(key, summary.copy(accessGranted = false), 1_000L, day)

        assertNull(MindyDaySummaryCache.latest(key, day))
        assertFalse(MindyDaySummaryCache.isFresh(key, 1_000L, day))
    }
}
