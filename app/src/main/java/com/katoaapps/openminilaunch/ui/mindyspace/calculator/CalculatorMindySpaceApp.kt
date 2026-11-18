package com.katoaapps.openminilaunch.ui.mindyspace.calculator

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.mindyspace.MindySpaceMiniApp
import com.katoaapps.openminilaunch.features.mindyspace.calculator.CalculatorHistoryRepository
import com.katoaapps.openminilaunch.ui.mindyspace.core.MindySpaceApp
import com.katoaapps.openminilaunch.ui.mindyspace.core.MindySpaceMiniAppManifest
import com.katoaapps.openminilaunch.ui.mindyspace.core.MindySpaceScope

/** Calculator implementation registered with the MindySpace platform. */
internal class CalculatorMindySpaceApp : MindySpaceApp {
    override val manifest: MindySpaceMiniAppManifest = Manifest

    @Composable
    override fun Content(scope: MindySpaceScope, modifier: Modifier) {
        val context = LocalContext.current
        val historyRepository = remember(context) { CalculatorHistoryRepository.get(context) }
        scope.Surface(modifier = modifier) {
            CalculatorMiniApp(
                history = historyRepository.entries,
                onHistoryAdded = historyRepository::add,
                onHistoryCleared = historyRepository::clear,
                modifier = Modifier.fillMaxSize(),
                mutedContentColor = scope.mutedContentColor,
            )
        }
    }

    companion object {
        val Manifest = MindySpaceMiniAppManifest(
            miniApp = MindySpaceMiniApp.CALCULATOR,
            labelRes = R.string.calculator,
            icon = Icons.Default.Calculate,
        )
    }
}
