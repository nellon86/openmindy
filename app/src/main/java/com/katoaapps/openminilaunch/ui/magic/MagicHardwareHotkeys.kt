package com.katoaapps.openminilaunch.ui.magic

import android.view.KeyEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.katoaapps.openminilaunch.features.magic.MAGIC_HOTKEY_TRIGGERS
import com.katoaapps.openminilaunch.features.magic.printableHardwareText

/** Lets Home catch Magic Box hotkeys before focus routing on unusual physical keyboards. */
interface MagicHardwareHotkeyHost {
    fun registerMagicHardwareHotkeyHandler(handler: (KeyEvent) -> Boolean): () -> Unit

    /**
     * Receives key events that nothing in the window consumed. Home uses it so a printable key
     * still reaches the Magic Box while no editor or armed target holds focus.
     */
    fun registerMagicUnhandledKeyHandler(handler: (KeyEvent) -> Boolean): () -> Unit = { }
}

/**
 * Catches printable keys that fell through every focused element, so the first letter typed on
 * Home is never lost while focus is briefly elsewhere: after returning from another app, after
 * touching a widget or MindySpace, or in the frames between tapping the bar and the editor
 * taking focus. Keys consumed by an editor, the calculator, or an IME never arrive here.
 */
@Composable
internal fun MagicUnhandledKeyEffect(
    enabled: Boolean,
    onPrintableKey: (typedText: String, keyCode: Int) -> Unit,
) {
    val host = LocalContext.current as? MagicHardwareHotkeyHost
    val currentOnPrintableKey by rememberUpdatedState(onPrintableKey)

    DisposableEffect(host, enabled) {
        if (!enabled || host == null) return@DisposableEffect onDispose { }

        val unregister = host.registerMagicUnhandledKeyHandler { event ->
            val typedText = printableHardwareText(event.unicodeChar)
            val plainTyping = !event.isCtrlPressed && !event.isMetaPressed
            if (event.action != KeyEvent.ACTION_DOWN || typedText == null || !plainTyping) {
                false
            } else {
                if (event.repeatCount == 0) currentOnPrintableKey(typedText, event.keyCode)
                true
            }
        }
        onDispose(unregister)
    }
}

/** Registers only while the collapsed Home Magic Box is eligible to receive hotkeys. */
@Composable
internal fun MagicHardwareHotkeyEffect(
    enabled: Boolean,
    onHotkey: (trigger: String, keyCode: Int) -> Unit,
) {
    val host = LocalContext.current as? MagicHardwareHotkeyHost
    val currentOnHotkey by rememberUpdatedState(onHotkey)

    DisposableEffect(host, enabled) {
        if (!enabled || host == null) return@DisposableEffect onDispose { }

        val unregister = host.registerMagicHardwareHotkeyHandler { event ->
            val trigger = printableHardwareText(event.unicodeChar)
                ?.singleOrNull()
                ?.takeIf { it in MAGIC_HOTKEY_TRIGGERS }
            if (event.action != KeyEvent.ACTION_DOWN || trigger == null) {
                false
            } else {
                if (event.repeatCount == 0) currentOnHotkey(trigger.toString(), event.keyCode)
                true
            }
        }
        onDispose(unregister)
    }
}
