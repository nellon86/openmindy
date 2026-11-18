package com.katoaapps.openminilaunch.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState

/**
 * True while the hosting activity is at least STARTED (visible).
 *
 * Home stays composed while other apps are in front, so periodic work keyed on this value stops
 * waking the launcher process when nobody can see its result.
 */
@Composable
internal fun rememberLauncherVisible(): Boolean {
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    return lifecycleState.isAtLeast(Lifecycle.State.STARTED)
}
