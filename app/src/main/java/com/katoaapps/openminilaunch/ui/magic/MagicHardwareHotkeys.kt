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
    onPrintableKey: (typedText: String, keyCode: Int, alternates: Set<String>) -> Unit,
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
                if (event.repeatCount == 0) {
                    currentOnPrintableKey(typedText, event.keyCode, hardwareKeyAlternates(event, typedText))
                }
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

private val ALTERNATE_META_STATES = intArrayOf(
    0,
    KeyEvent.META_SHIFT_ON,
    KeyEvent.META_ALT_ON,
    KeyEvent.META_ALT_ON or KeyEvent.META_SHIFT_ON,
    KeyEvent.META_SYM_ON,
)

/**
 * Every character the physical key behind [event] can produce: its case variants, the Shift,
 * Alt and Sym layers of the key character map, and the label printed on the key. An IME
 * long-press replacement always comes from this set, so it is the only text allowed to replace
 * the first character captured on Home.
 */
internal fun hardwareKeyAlternates(event: KeyEvent, typedText: String): Set<String> {
    val alternates = linkedSetOf(typedText.lowercase(), typedText.uppercase())
    val characterMap = runCatching { event.keyCharacterMap }.getOrNull()
    if (characterMap != null) {
        ALTERNATE_META_STATES.forEach { metaState ->
            val codePoint = runCatching { characterMap.get(event.keyCode, metaState) }.getOrDefault(0)
            // Combining accents (dead keys) are not standalone characters.
            if (codePoint and android.view.KeyCharacterMap.COMBINING_ACCENT == 0) {
                printableHardwareText(codePoint)?.let(alternates::add)
            }
        }
        runCatching { characterMap.getNumber(event.keyCode) }.getOrNull()
            ?.takeIf { it != '\u0000' }
            ?.let { alternates += it.toString() }
        runCatching { characterMap.getDisplayLabel(event.keyCode) }.getOrNull()
            ?.takeIf { it != '\u0000' }
            ?.let { label -> alternates += label.toString(); alternates += label.lowercase() }
    }
    alternates -= typedText
    return alternates
}
