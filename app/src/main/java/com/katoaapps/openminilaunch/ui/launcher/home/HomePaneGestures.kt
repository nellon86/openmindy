package com.katoaapps.openminilaunch.ui.launcher.home

import android.os.SystemClock
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import com.katoaapps.openminilaunch.ui.theme.Dimens

/** Keeps Home's pane navigation gestures separate from its visual layout. */
@Composable
internal fun Modifier.homePaneGestures(
    magicExpanded: Boolean,
    verticalGesturesEnabled: Boolean,
    horizontalGesturesEnabled: Boolean,
    onPaneInteracted: () -> Unit,
    onHorizontalDrag: ((Float) -> Unit)?,
    onHorizontalDragFinished: ((Float, Long) -> Unit)?,
    onSwipeUp: () -> Unit,
    /** Receives true when the swipe started on the right half of the screen. */
    onSwipeDown: (fromRightHalf: Boolean) -> Unit,
    onDoubleTap: () -> Unit,
): Modifier {
    val density = LocalDensity.current
    val verticalSwipeThresholdPx = with(density) { Dimens.dp48.toPx() }
    val currentPaneInteracted by rememberUpdatedState(onPaneInteracted)
    val currentHorizontalDrag by rememberUpdatedState(onHorizontalDrag)
    val currentHorizontalDragFinished by rememberUpdatedState(onHorizontalDragFinished)
    val currentSwipeUp by rememberUpdatedState(onSwipeUp)
    val currentSwipeDown by rememberUpdatedState(onSwipeDown)
    val currentDoubleTap by rememberUpdatedState(onDoubleTap)

    // Home can be one pane of a two-panel layout, so the side is judged against the whole
    // window rather than the pane: left half opens notifications, right half quick settings.
    var paneLeftInRootPx by remember { mutableFloatStateOf(0f) }
    var rootWidthPx by remember { mutableFloatStateOf(0f) }

    val horizontalDragModifier = if (
        onHorizontalDrag != null && !magicExpanded && horizontalGesturesEnabled
    ) {
        Modifier.pointerInput(Unit) {
            var draggedPx = 0f
            var startedAtMillis = 0L
            detectHorizontalDragGestures(
                onDragStart = {
                    draggedPx = 0f
                    startedAtMillis = SystemClock.uptimeMillis()
                },
                onHorizontalDrag = { change, amount ->
                    draggedPx += amount
                    currentHorizontalDrag?.invoke(amount)
                    change.consume()
                },
                onDragEnd = {
                    currentHorizontalDragFinished?.invoke(
                        draggedPx,
                        SystemClock.uptimeMillis() - startedAtMillis,
                    )
                },
                onDragCancel = {
                    currentHorizontalDragFinished?.invoke(
                        draggedPx,
                        SystemClock.uptimeMillis() - startedAtMillis,
                    )
                },
            )
        }
    } else {
        Modifier
    }

    return onGloballyPositioned { coordinates ->
        paneLeftInRootPx = coordinates.positionInRoot().x
        rootWidthPx = coordinates.findRootCoordinates().size.width.toFloat()
    }
        .clipToBounds()
        .then(horizontalDragModifier)
        .pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                currentPaneInteracted()
            }
        }
        .pointerInput(verticalSwipeThresholdPx, magicExpanded, verticalGesturesEnabled) {
            if (verticalGesturesEnabled) {
                var verticalDistance = 0f
                var startedOnRightHalf = false
                detectVerticalDragGestures(
                    onDragStart = { start ->
                        verticalDistance = 0f
                        val windowWidth = rootWidthPx.takeIf { it > 0f } ?: size.width.toFloat()
                        startedOnRightHalf = paneLeftInRootPx + start.x > windowWidth / 2f
                    },
                    onVerticalDrag = { _, amount -> verticalDistance += amount },
                    onDragEnd = {
                        when {
                            verticalDistance >= verticalSwipeThresholdPx -> currentSwipeDown(startedOnRightHalf)
                            verticalDistance <= -verticalSwipeThresholdPx && !magicExpanded -> currentSwipeUp()
                        }
                    },
                )
            }
        }
        .pointerInput(magicExpanded) {
            if (!magicExpanded) detectTapGestures(onDoubleTap = { currentDoubleTap() })
        }
}
