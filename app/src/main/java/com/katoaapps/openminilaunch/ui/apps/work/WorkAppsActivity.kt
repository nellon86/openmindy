package com.katoaapps.openminilaunch.ui.apps.work

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.theme.MindyLauncherTheme

class WorkAppsActivity : ComponentActivity() {
    private lateinit var actions: DeviceActions

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        actions = DeviceActions(this)
        val store = LauncherStore.get(this)
        setContent {
            MindyLauncherTheme(store) {
                WorkAppsScreen(
                    actions = actions,
                    demoMode = store.demoModeEnabled,
                    onClose = ::finish,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        actions.invalidateInstalledApps()
    }
}
