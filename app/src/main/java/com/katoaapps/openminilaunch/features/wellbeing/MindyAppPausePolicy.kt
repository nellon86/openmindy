package com.katoaapps.openminilaunch.features.wellbeing

import com.katoaapps.openminilaunch.model.MindyAppPauseMode
import com.katoaapps.openminilaunch.model.LauncherAppTarget
import com.katoaapps.openminilaunch.model.LauncherTarget

internal data class MindyAppAccessState(
    val mode: MindyAppPauseMode,
    val trackedPackages: Set<String>,
    val collectiveUsageMillis: Long,
    val dailyLimitMinutes: Int,
    val usageAccessGranted: Boolean,
    val isResolved: Boolean,
) {
    fun isPaused(packageName: String): Boolean = shouldPauseMindyApp(
        packageName = packageName,
        trackedPackages = trackedPackages,
        mode = mode,
        collectiveUsageMillis = collectiveUsageMillis,
        dailyLimitMinutes = dailyLimitMinutes,
        usageAccessGranted = usageAccessGranted,
    )

    fun isPaused(target: LauncherAppTarget): Boolean = shouldPauseLauncherApp(
        packageName = target.packageName,
        isWorkProfile = target.isWorkProfile,
        trackedPackages = trackedPackages,
        mode = mode,
        collectiveUsageMillis = collectiveUsageMillis,
        dailyLimitMinutes = dailyLimitMinutes,
        usageAccessGranted = usageAccessGranted,
    )

    fun isPaused(target: LauncherTarget): Boolean = shouldPauseLauncherApp(
        packageName = target.packageName,
        isWorkProfile = target.isWorkProfile,
        trackedPackages = trackedPackages,
        mode = mode,
        collectiveUsageMillis = collectiveUsageMillis,
        dailyLimitMinutes = dailyLimitMinutes,
        usageAccessGranted = usageAccessGranted,
    )
}

internal fun shouldPauseLauncherApp(
    packageName: String,
    isWorkProfile: Boolean,
    trackedPackages: Set<String>,
    mode: MindyAppPauseMode,
    collectiveUsageMillis: Long,
    dailyLimitMinutes: Int,
    usageAccessGranted: Boolean,
): Boolean = !isWorkProfile && shouldPauseMindyApp(
    packageName = packageName,
    trackedPackages = trackedPackages,
    mode = mode,
    collectiveUsageMillis = collectiveUsageMillis,
    dailyLimitMinutes = dailyLimitMinutes,
    usageAccessGranted = usageAccessGranted,
)

internal fun shouldPauseMindyApp(
    packageName: String,
    trackedPackages: Set<String>,
    mode: MindyAppPauseMode,
    collectiveUsageMillis: Long,
    dailyLimitMinutes: Int,
    usageAccessGranted: Boolean,
): Boolean {
    if (packageName !in trackedPackages) return false
    return when (mode) {
        MindyAppPauseMode.ALWAYS -> true
        MindyAppPauseMode.AFTER_DAILY_LIMIT -> usageAccessGranted &&
            collectiveUsageMillis >= dailyLimitMinutes.coerceAtLeast(0) * 60_000L
        MindyAppPauseMode.NEVER -> false
    }
}
