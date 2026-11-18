package com.katoaapps.openminilaunch.ui.mindyspace.music

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.mindyspace.music.MusicPlaybackAvailability
import com.katoaapps.openminilaunch.features.mindyspace.music.MusicPlaybackState
import com.katoaapps.openminilaunch.ui.mindyspace.core.MindySpaceExpansionController
import com.katoaapps.openminilaunch.ui.mindyspace.core.mindySpaceExpansionBackgroundTap
import com.katoaapps.openminilaunch.ui.theme.Dimens

/** Art-led media UI. Platform state and transport commands remain in the owning page/repository. */
@Composable
internal fun MusicMiniApp(
    state: MusicPlaybackState,
    expansion: MindySpaceExpansionController,
    foregroundVisible: Boolean,
    onForegroundVisibleChanged: (Boolean) -> Unit,
    onOpenPlayer: () -> Unit,
    onRequestAccess: () -> Unit,
    onSeekBack: () -> Unit,
    onRestart: () -> Unit,
    onPrevious: () -> Unit,
    onTogglePlayback: () -> Unit,
    onSeekToFraction: (Float) -> Unit,
    onSeekForward: () -> Unit,
    onNext: () -> Unit,
    onShuffle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val expanded = expansion.expanded
    Box(
        modifier.mindySpaceExpansionBackgroundTap(
            expansion = expansion,
            enabled = expanded || state.hasSession,
        ),
    ) {
        MusicArtworkBackground(
            state = state,
            modifier = Modifier.fillMaxSize(),
        )

        if (!foregroundVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        onClickLabel = stringResource(R.string.music_show_controls),
                        onClick = { onForegroundVisibleChanged(true) },
                    ),
            )
            return@Box
        }

        MusicHeading(
            state = state,
            expansion = expansion,
            onOpenPlayer = onOpenPlayer,
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth(0.76f)
                .padding(Dimens.dp16),
        )
        PlayerAppIcon(
            state = state,
            onOpenPlayer = onOpenPlayer,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(
                    top = Dimens.dp14,
                    end = if (expanded) Dimens.dp64 else Dimens.dp14,
                ),
        )

        if (state.availability != MusicPlaybackAvailability.READY) {
            TextButton(
                onClick = if (state.availability == MusicPlaybackAvailability.ACCESS_REQUIRED) {
                    onRequestAccess
                } else {
                    onOpenPlayer
                },
                modifier = Modifier.align(Alignment.Center),
            ) {
                Text(
                    text = stringResource(
                        if (state.availability == MusicPlaybackAvailability.ACCESS_REQUIRED) {
                            R.string.music_open_notification_access
                        } else {
                            R.string.music_open_music_app
                        },
                    ),
                    color = Color.White,
                )
            }
        }

        MusicControls(
            state = state,
            expanded = expanded,
            onSeekBack = onSeekBack,
            onRestart = onRestart,
            onPrevious = onPrevious,
            onTogglePlayback = onTogglePlayback,
            onOpenPlayer = onOpenPlayer,
            onHideForeground = { onForegroundVisibleChanged(false) },
            onSeekToFraction = onSeekToFraction,
            onSeekForward = onSeekForward,
            onNext = onNext,
            onShuffle = onShuffle,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(
                    start = Dimens.dp16,
                    end = Dimens.dp16,
                    bottom = Dimens.dp14,
                ),
        )
    }
}
