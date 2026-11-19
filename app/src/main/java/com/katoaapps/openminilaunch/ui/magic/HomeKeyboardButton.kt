package com.katoaapps.openminilaunch.ui.magic

import android.os.SystemClock
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.magic.HomeKeyboardButtonAction
import com.katoaapps.openminilaunch.features.magic.HomeKeyboardButtonAction.*
import kotlin.math.abs

internal val HomeKeyboardButtonAction.icon: ImageVector
    get() = when (this) {
        NONE -> Icons.Default.Block
        MAGIC_BOX -> Icons.Default.Keyboard
        VOICE_INPUT -> Icons.Default.Mic
        WEB_SEARCH -> Icons.Default.Public
        ASSISTANT -> Icons.Default.AutoAwesome
        NEW_TODO -> Icons.Default.Checklist
        NEW_NOTE -> Icons.Default.EditNote
        NEW_EVENT -> Icons.Default.Event
        NEW_MESSAGE -> Icons.Default.Sms
        NEW_CALL -> Icons.Default.Call
        QUICK_APPS -> Icons.Default.GridView
        ALL_APPS -> Icons.Default.Apps
        CONVERSATIONS -> Icons.Default.Forum
        MINDY_SPACE_FULL_SCREEN -> Icons.Default.OpenInFull
        NOTIFICATIONS -> Icons.Default.Notifications
        QUICK_SETTINGS -> Icons.Default.Tune
        CAMERA -> Icons.Default.PhotoCamera
        FLASHLIGHT -> Icons.Default.FlashlightOn
        LOCK_SCREEN -> Icons.Default.Lock
        LAUNCH_APP -> Icons.Default.OpenInNew
    }

@Composable
internal fun HomeKeyboardButtonAction.label(): String {
    @Composable fun magic(routeLabel: Int): String = stringResource(R.string.home_button_magic_route, stringResource(routeLabel))
    return when (this) {
        NONE -> stringResource(R.string.home_button_action_none)
        MAGIC_BOX -> stringResource(R.string.magic_box)
        VOICE_INPUT -> stringResource(R.string.home_button_action_voice)
        WEB_SEARCH -> magic(R.string.home_button_action_web)
        ASSISTANT -> magic(R.string.ai)
        NEW_TODO -> magic(R.string.command_todo)
        NEW_NOTE -> magic(R.string.command_note)
        NEW_EVENT -> magic(R.string.command_event)
        NEW_MESSAGE -> magic(R.string.command_text)
        NEW_CALL -> magic(R.string.command_call)
        QUICK_APPS -> stringResource(R.string.quick_apps)
        ALL_APPS -> stringResource(R.string.all_apps)
        CONVERSATIONS -> stringResource(R.string.conversations)
        MINDY_SPACE_FULL_SCREEN -> stringResource(R.string.home_button_action_mindy_space)
        NOTIFICATIONS -> stringResource(R.string.home_button_action_notifications)
        QUICK_SETTINGS -> stringResource(R.string.home_button_action_quick_settings)
        CAMERA -> stringResource(R.string.home_button_action_camera)
        FLASHLIGHT -> stringResource(R.string.home_button_action_flashlight)
        LOCK_SCREEN -> stringResource(R.string.home_button_action_lock)
        LAUNCH_APP -> stringResource(R.string.home_button_action_launch_app)
    }
}

/** Upward travel that turns a press on the button into a swipe-up. */
private val SWIPE_UP_DISTANCE = 32.dp

/**
 * Tap, double tap, long press and swipe-up on one small target. With [onDoubleTap] set, a single
 * tap waits for the system double-tap timeout (~300 ms) before running, so the two never clash. The whole gesture is consumed so Home's own
 * vertical swipes (shade, All apps) never fire from the button. A gesture without an action
 * (null callback) simply does nothing; a hold without a long-press action still counts as a tap.
 */
@Composable
internal fun Modifier.homeKeyboardButtonGestures(
    onTap: () -> Unit,
    onDoubleTap: (() -> Unit)?,
    onLongPress: (() -> Unit)?,
    onSwipeUp: (() -> Unit)?,
): Modifier {
    val currentOnTap by rememberUpdatedState(onTap)
    val currentOnDoubleTap by rememberUpdatedState(onDoubleTap)
    val currentOnLongPress by rememberUpdatedState(onLongPress)
    val currentOnSwipeUp by rememberUpdatedState(onSwipeUp)
    return pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            down.consume()
            val touchSlop = viewConfiguration.touchSlop
            val swipeDistance = SWIPE_UP_DISTANCE.toPx()
            val longPressAt = SystemClock.uptimeMillis() + viewConfiguration.longPressTimeoutMillis
            var moved = false
            var handled = false
            var longPressWindowOpen = true
            var tapped = false
            while (true) {
                val event = if (longPressWindowOpen && !moved && !handled) {
                    val wait = (longPressAt - SystemClock.uptimeMillis()).coerceAtLeast(0L)
                    withTimeoutOrNull(wait) { awaitPointerEvent() }
                } else {
                    awaitPointerEvent()
                }
                if (event == null) {
                    longPressWindowOpen = false
                    currentOnLongPress?.let { longPress ->
                        handled = true
                        longPress()
                    }
                    continue
                }
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (!change.pressed) {
                    change.consume()
                    tapped = !handled && !moved
                    break
                }
                if (!handled) {
                    val delta = change.position - down.position
                    val upward = -delta.y
                    if (upward >= swipeDistance && upward > abs(delta.x)) {
                        handled = true
                        currentOnSwipeUp?.invoke()
                    } else if (delta.getDistance() > touchSlop) {
                        moved = true
                    }
                }
                change.consume()
            }
            if (!tapped) return@awaitEachGesture
            val doubleTap = currentOnDoubleTap
            if (doubleTap == null) {
                currentOnTap()
                return@awaitEachGesture
            }
            val secondDown = withTimeoutOrNull(viewConfiguration.doubleTapTimeoutMillis) {
                awaitFirstDown(requireUnconsumed = false)
            }
            if (secondDown == null) {
                currentOnTap()
                return@awaitEachGesture
            }
            secondDown.consume()
            // Swallow the rest of the second press so it cannot start a new tap.
            while (true) {
                val change = awaitPointerEvent().changes.firstOrNull { it.id == secondDown.id } ?: break
                change.consume()
                if (!change.pressed) break
            }
            doubleTap()
        }
    }
}
