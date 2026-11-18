package com.katoaapps.openminilaunch.ui.mindyspace.music

import android.content.ActivityNotFoundException
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.conversations.ConversationNotificationStore
import com.katoaapps.openminilaunch.features.mindyspace.MindySpaceMiniApp
import com.katoaapps.openminilaunch.features.mindyspace.music.MusicSessionRepository
import com.katoaapps.openminilaunch.ui.mindyspace.core.MindySpaceApp
import com.katoaapps.openminilaunch.ui.mindyspace.core.MindySpaceMiniAppManifest
import com.katoaapps.openminilaunch.ui.mindyspace.core.MindySpaceScope

/** Music implementation registered with the MindySpace platform. */
internal class MusicMindySpaceApp : MindySpaceApp {
    override val manifest: MindySpaceMiniAppManifest = Manifest

    @Composable
    override fun Content(scope: MindySpaceScope, modifier: Modifier) {
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current
        val repository = remember(context) { MusicSessionRepository.get(context) }
        var foregroundVisible by rememberSaveable { mutableStateOf(true) }

        DisposableEffect(repository) {
            repository.start()
            onDispose { repository.stop() }
        }
        DisposableEffect(lifecycleOwner, repository) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) repository.refresh()
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        scope.Surface(
            modifier = modifier,
            contentColor = Color.White,
        ) {
            MusicMiniApp(
                state = repository.state,
                expansion = scope.expansion,
                foregroundVisible = foregroundVisible,
                onForegroundVisibleChanged = { foregroundVisible = it },
                onOpenPlayer = { repository.openPlayerApp() },
                onRequestAccess = {
                    try {
                        context.startActivity(ConversationNotificationStore.notificationAccessSettingsIntent())
                    } catch (_: ActivityNotFoundException) {
                        // Some OEMs remove this settings surface. Leave the current explanation visible.
                    }
                },
                onSeekBack = { repository.seekBy(-10_000L) },
                onRestart = repository::restart,
                onPrevious = repository::skipPrevious,
                onTogglePlayback = repository::togglePlayback,
                onSeekToFraction = repository::seekToFraction,
                onSeekForward = { repository.seekBy(10_000L) },
                onNext = repository::skipNext,
                onShuffle = repository::requestShuffle,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    companion object {
        val Manifest = MindySpaceMiniAppManifest(
            miniApp = MindySpaceMiniApp.MUSIC,
            labelRes = R.string.music_player,
            icon = Icons.Default.MusicNote,
            supportsExpansion = true,
        )
    }
}
