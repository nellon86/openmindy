package com.katoaapps.openminilaunch.model

/** A launchable app or app-published shortcut that can occupy a Mindy launcher slot. */
sealed interface LauncherTarget {
    val label: String
    val packageName: String
    val userSerial: Long
    val isWorkProfile: Boolean
    val isAvailable: Boolean
    val selectionKey: String
}
