package com.katoaapps.openminilaunch.features.wellbeing

import java.time.LocalDate

/** How long a computed Mindy's Day summary is reused before usage stats are read again. */
internal const val MINDY_DAY_SUMMARY_MAX_AGE_MILLIS = 3 * 60 * 60 * 1_000L

/**
 * Process-wide memory of the last Mindy's Day summary.
 *
 * Reading usage events for the whole day takes noticeable time, and both the Mindy's Day page
 * and the Home icon asked for it every time they became visible. Now a summary is shown at once
 * from here and only recomputed when it is older than [MINDY_DAY_SUMMARY_MAX_AGE_MILLIS], belongs
 * to an earlier day, was computed for different settings, or a refresh is forced (for example
 * after granting usage access). Summaries without usage access are never cached, so granting
 * access is picked up immediately.
 */
internal object MindyDaySummaryCache {
    private class Entry(
        val inputsKey: String,
        val day: LocalDate,
        val computedAtElapsedMillis: Long,
        val summary: MindyDaySummary,
    )

    @Volatile private var entry: Entry? = null

    /** The last summary for these settings today, whatever its age, to show without waiting. */
    fun latest(inputsKey: String, today: LocalDate = LocalDate.now()): MindyDaySummary? =
        entry?.takeIf { it.inputsKey == inputsKey && it.day == today }?.summary

    fun isFresh(
        inputsKey: String,
        nowElapsedMillis: Long,
        today: LocalDate = LocalDate.now(),
    ): Boolean {
        val current = entry ?: return false
        return current.inputsKey == inputsKey &&
            current.day == today &&
            nowElapsedMillis - current.computedAtElapsedMillis in 0 until MINDY_DAY_SUMMARY_MAX_AGE_MILLIS
    }

    fun store(
        inputsKey: String,
        summary: MindyDaySummary,
        nowElapsedMillis: Long,
        today: LocalDate = LocalDate.now(),
    ) {
        entry = if (summary.accessGranted) {
            Entry(inputsKey, today, nowElapsedMillis, summary)
        } else {
            null
        }
    }

    fun inputsKey(
        socialPackages: Set<String>,
        usesAutomaticSocialApps: Boolean,
        socialGoalMinutes: Int,
    ): String = buildString {
        append(socialGoalMinutes).append('|').append(usesAutomaticSocialApps).append('|')
        socialPackages.sorted().joinTo(this, separator = ",")
    }

    internal fun clearForTest() {
        entry = null
    }
}
