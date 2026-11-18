package com.katoaapps.openminilaunch.features.wellbeing

import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.apps.LauncherAppRepository
import com.katoaapps.openminilaunch.model.LaunchableApp
import com.katoaapps.openminilaunch.model.LauncherAppTarget

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.Process
import android.provider.Settings
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.ConcurrentHashMap

internal class UsageInsightsRepository(private val context: Context) {
    private val usageStats = context.getSystemService(UsageStatsManager::class.java)
    private val packageManager = context.packageManager
    private val labelCache = ConcurrentHashMap<String, String>()
    private val socialCategoryCache = ConcurrentHashMap<String, Boolean>()
    private val launcherApps = LauncherAppRepository.get(context)
    private val homePackages: Set<String> by lazy {
        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        packageManager.queryIntentActivities(homeIntent, 0).mapTo(mutableSetOf()) { it.activityInfo.packageName }
    }

    fun hasAccess(): Boolean {
        val appOps = context.getSystemService(AppOpsManager::class.java)
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun accessSettingsIntent(): Intent {
        val usageIntent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        return if (usageIntent.resolveActivity(packageManager) != null) usageIntent else Intent(Settings.ACTION_SETTINGS)
    }

    fun summary(
        socialPackages: Set<String>,
        usesAutomaticSocialApps: Boolean,
        socialGoalMinutes: Int,
        nowMillis: Long = System.currentTimeMillis(),
    ): MindyDaySummary {
        val zone = ZoneId.systemDefault()
        val hour = Instant.ofEpochMilli(nowMillis).atZone(zone).hour
        if (!hasAccess()) return noAccessSummary(hour)
        val dayStart = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
            .atStartOfDay(zone).toInstant().toEpochMilli()
        val events = readTimelineEvents(dayStart - EVENT_LOOKBACK_MILLIS, nowMillis)
        val observedPackages = events.mapNotNull(UsageTimelineEvent::packageName).toSet()
        val personalPackages = personalLauncherPackages()
        val automaticCandidates = if (personalPackages.isEmpty()) {
            observedPackages
        } else {
            observedPackages.intersect(personalPackages)
        }
        val automaticPackages = automaticCandidates.filterTo(mutableSetOf()) { isSocial(it, emptySet()) }
        val requestedPackages = effectiveTrackedPackages(socialPackages, automaticPackages, usesAutomaticSocialApps)
        // Usage events are already scoped to the current user. Only intersect when LauncherApps
        // returned a personal inventory, so a transient profile-query failure does not erase a day.
        val trackedPackages = if (personalPackages.isEmpty()) {
            requestedPackages
        } else {
            requestedPackages.intersect(personalPackages)
        }
        val ignored = observedPackages.filterTo(mutableSetOf(), ::isIgnoredPackage)
        val analysis = analyzeUsageTimeline(
            dayStart = dayStart,
            now = nowMillis,
            events = events,
            trackedPackages = trackedPackages,
            ignoredPackages = ignored,
        )
        val allApps = analysis.packageDurations.map { (packageName, duration) ->
            MindyAppUsage(packageName, appLabel(packageName), duration)
        }.sortedByDescending(MindyAppUsage::foregroundMillis)
        val socialMillis = allApps.sumOf(MindyAppUsage::foregroundMillis)
        val longestSocial = allApps.maxByOrNull { analysis.longestSessions[it.packageName] ?: 0L }
        val longestSocialMillis = longestSocial?.let { analysis.longestSessions[it.packageName] } ?: 0L
        val state = chooseMindyState(
            hour = hour,
            socialMillis = socialMillis,
            socialGoalMinutes = socialGoalMinutes,
            socialOpensLastHour = analysis.opensLastHour,
            longestSocialSession = longestSocialMillis,
        )
        val (headline, detail) = stateCopy(
            state = state,
            socialMillis = socialMillis,
            socialGoalMinutes = socialGoalMinutes,
            socialOpensLastHour = analysis.opensLastHour,
            longestSocial = longestSocial,
            longestSocialMillis = longestSocialMillis,
            topSocial = allApps.firstOrNull { it.foregroundMillis >= DISPLAY_THRESHOLD_MILLIS },
        )
        return MindyDaySummary(
            accessGranted = true,
            state = state,
            socialMillis = socialMillis,
            socialOpensToday = analysis.opensToday,
            socialOpensLastHour = analysis.opensLastHour,
            topApps = allApps.filter { it.foregroundMillis >= DISPLAY_THRESHOLD_MILLIS }.take(5),
            headline = headline,
            detail = detail,
        )
    }

    fun launchableApps(): List<LaunchableApp> {
        val profileAwareApps = launcherApps.targets().asSequence()
            .filterNot { it.isWorkProfile }
            .map { LaunchableApp(it.label, it.packageName) }
            .distinctBy(LaunchableApp::packageName)
            .sortedBy { it.label.lowercase() }
            .toList()
        if (profileAwareApps.isNotEmpty()) return profileAwareApps

        // Mindy's Day is deliberately personal-profile only. This fallback preserves its picker
        // if LauncherApps is temporarily unavailable without broadening it to managed profiles.
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return packageManager.queryIntentActivities(launcherIntent, 0)
            .map { LaunchableApp(it.loadLabel(packageManager).toString(), it.activityInfo.packageName) }
            .distinctBy(LaunchableApp::packageName)
            .sortedBy { it.label.lowercase() }
    }

    fun automaticSocialPackages(apps: List<LaunchableApp>): Set<String> = apps.asSequence()
        .map(LaunchableApp::packageName)
        .filter { isSocial(it, emptySet()) }
        .toSet()

    private fun readTimelineEvents(begin: Long, end: Long): List<UsageTimelineEvent> {
        val result = mutableListOf<UsageTimelineEvent>()
        val event = UsageEvents.Event()
        usageStats.queryEvents(begin, end).let { events ->
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                // RESUMED/PAUSED retain the same event values used by the pre-29
                // MOVE_TO_FOREGROUND/BACKGROUND names, so one branch covers minSdk 26+.
                val kind = when (event.eventType) {
                    UsageEvents.Event.ACTIVITY_RESUMED -> UsageEventKind.FOREGROUND
                    UsageEvents.Event.ACTIVITY_PAUSED -> UsageEventKind.BACKGROUND
                    UsageEvents.Event.SCREEN_INTERACTIVE -> UsageEventKind.SCREEN_ON
                    UsageEvents.Event.SCREEN_NON_INTERACTIVE -> UsageEventKind.SCREEN_OFF
                    else -> null
                }
                if (kind != null) result += UsageTimelineEvent(event.timeStamp, event.packageName, kind)
            }
        }
        return result
    }

    private fun noAccessSummary(hour: Int) = MindyDaySummary(
        accessGranted = false,
        state = if (hour >= 22 || hour < 5) MindyState.SLEEPING else MindyState.WALKING,
        headline = context.getString(if (hour >= 22 || hour < 5) R.string.mindy_made_it_home else R.string.mindy_ready_for_day),
        detail = context.getString(R.string.mindy_enable_usage_detail),
    )

    private fun personalLauncherPackages(): Set<String> = launcherApps.targets().asSequence()
        .filterNot { it.isWorkProfile }
        .map(LauncherAppTarget::packageName)
        .toSet()

    private fun stateCopy(
        state: MindyState,
        socialMillis: Long,
        socialGoalMinutes: Int,
        socialOpensLastHour: Int,
        longestSocial: MindyAppUsage?,
        longestSocialMillis: Long,
        topSocial: MindyAppUsage?,
    ): Pair<String, String> = when (state) {
        MindyState.SLEEPING -> context.getString(R.string.mindy_made_it_home) to context.getString(R.string.mindy_sleeping_detail)
        MindyState.PHONE -> context.getString(R.string.mindy_stopped_to_scroll) to if (topSocial != null) {
            context.getString(
                R.string.mindy_top_app_goal_detail,
                topSocial.label,
                formatDuration(context, socialMillis),
                formatDuration(context, socialGoalMinutes * 60_000L),
            )
        } else context.getString(R.string.mindy_goal_passed_detail, formatDuration(context, socialGoalMinutes * 60_000L))
        MindyState.DISTRACTED -> context.getString(R.string.mindy_checking_headline) to
            context.resources.getQuantityString(
                R.plurals.mindy_opens_detail,
                socialOpensLastHour,
                socialOpensLastHour,
            )
        MindyState.RESTING -> context.getString(R.string.mindy_pause_headline) to if (longestSocial != null) {
            context.getString(R.string.mindy_longest_visit_detail, formatDuration(context, longestSocialMillis), longestSocial.label)
        } else context.getString(R.string.mindy_break_detail)
        MindyState.PURPOSEFUL -> context.getString(R.string.mindy_quiet_trail_headline) to context.getString(R.string.mindy_no_social_detail)
        MindyState.WALKING -> context.getString(R.string.mindy_moving_headline) to if (topSocial == null) {
            context.getString(R.string.mindy_not_enough_detail)
        } else context.getString(R.string.mindy_leading_detail, topSocial.label, formatDuration(context, topSocial.foregroundMillis))
    }

    private fun isSocial(packageName: String, selected: Set<String>): Boolean {
        if (packageName in selected) return true
        if (selected.isNotEmpty()) return false
        return socialCategoryCache.getOrPut(packageName) {
            val info = runCatching { packageManager.getApplicationInfo(packageName, 0) }.getOrNull()
            info?.category == ApplicationInfo.CATEGORY_SOCIAL
        }
    }

    private fun appLabel(packageName: String): String = labelCache.getOrPut(packageName) {
        runCatching {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString()
        }.getOrDefault(packageName.substringAfterLast('.'))
    }

    private fun isIgnoredPackage(packageName: String): Boolean = packageName == context.packageName ||
        packageName == "com.android.systemui" ||
        packageName.contains("permissioncontroller", ignoreCase = true) ||
        packageName in homePackages ||
        packageName.contains("launcher", ignoreCase = true)

    private companion object {
        const val EVENT_LOOKBACK_MILLIS = 24 * 60 * 60_000L
        const val DISPLAY_THRESHOLD_MILLIS = 60_000L
    }
}
