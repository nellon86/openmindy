package com.katoaapps.openminilaunch.ui.launcher.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import android.os.SystemClock
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.ui.components.rememberLauncherVisible
import com.katoaapps.openminilaunch.ui.theme.MindyFeatureColors
import com.katoaapps.openminilaunch.ui.theme.readableContentColor
import kotlinx.coroutines.delay

/** A persistent home action that becomes a software notification light for new activity. */
@Composable
internal fun ConversationAttentionButton(
    hasUnread: Boolean,
    attentionColors: List<Color>,
    actionSize: Dp,
    iconSize: Dp,
    restingColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Favourites light up in their own colours; any other unread conversation lights it red.
    val palette = remember(attentionColors, hasUnread) {
        attentionColors.distinct().ifEmpty {
            if (hasUnread) listOf(MindyFeatureColors.ConversationCount) else emptyList()
        }
    }
    var colorIndex by remember(palette) { mutableIntStateOf(0) }
    var animating by remember { mutableStateOf(false) }
    val launcherVisible = rememberLauncherVisible()
    // The light blinks for a short while when activity arrives or Home comes back, then stays
    // lit without animating: an endless blink redraws Home every frame for as long as it is open.
    LaunchedEffect(palette, launcherVisible) {
        colorIndex = 0
        animating = launcherVisible && palette.isNotEmpty()
        if (!animating) return@LaunchedEffect
        val endAt = SystemClock.uptimeMillis() + ATTENTION_ANIMATION_MILLIS
        while (SystemClock.uptimeMillis() < endAt) {
            delay(1_100L)
            if (palette.size > 1) colorIndex = (colorIndex + 1) % palette.size
        }
        colorIndex = 0
        animating = false
    }
    val needsAttention = palette.isNotEmpty()
    val alertColor by animateColorAsState(
        targetValue = palette.getOrNull(colorIndex) ?: Color.Transparent,
        animationSpec = tween(durationMillis = 240),
        label = "favorite attention color",
    )
    val alertContentColor = readableContentColor(alertColor)
    val pulse = if (needsAttention && animating) conversationAttentionPulse() else 0f

    Box(modifier.size(actionSize), contentAlignment = Alignment.Center) {
        if (needsAttention) {
            Canvas(Modifier.fillMaxSize()) {
                val half = size.minDimension / 2f
                drawCircle(
                    color = alertColor.copy(alpha = .3f + pulse * .65f),
                    radius = half * (.66f + pulse * .12f),
                )
                drawCircle(
                    color = alertColor.copy(alpha = (1f - pulse) * .78f),
                    radius = half * (.72f + pulse * .26f),
                )
            }
        }
        IconButton(onClick = onClick, modifier = Modifier.fillMaxSize()) {
            Icon(
                Icons.Default.Forum,
                stringResource(R.string.conversations),
                Modifier.size(iconSize).scale(if (needsAttention) .9f + pulse * .24f else 1f),
                tint = if (needsAttention) alertContentColor else restingColor,
            )
        }
    }
}

/** Exists only while attention is required, so the idle launcher owns no running animation. */
@Composable
private fun conversationAttentionPulse(): Float {
    val transition = rememberInfiniteTransition(label = "conversation attention")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 550, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "conversation attention blink",
    )
    return pulse
}

/** How long the attention light blinks before it rests. */
private const val ATTENTION_ANIMATION_MILLIS = 12_000L
