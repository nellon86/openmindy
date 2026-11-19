package com.katoaapps.openminilaunch.data

import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import com.katoaapps.openminilaunch.features.magic.HomeKeyboardButtonAction
import com.katoaapps.openminilaunch.features.magic.HomeKeyboardButtonBinding
import com.katoaapps.openminilaunch.features.magic.HomeKeyboardButtonBindings
import com.katoaapps.openminilaunch.features.magic.HomeKeyboardButtonGesture

/** Tap, long-press and swipe-up actions of the round button on the Home search bar. */
internal class HomeKeyboardButtonPreferenceStore(private val prefs: SharedPreferences) {
    var bindings by mutableStateOf(load())
        private set

    fun update(gesture: HomeKeyboardButtonGesture, binding: HomeKeyboardButtonBinding) {
        bindings = bindings.with(gesture, binding)
        prefs.edit {
            putString(actionKey(gesture), binding.action.storageId)
            binding.appTarget?.let { putString(targetKey(gesture), it) } ?: remove(targetKey(gesture))
        }
    }

    private fun load(): HomeKeyboardButtonBindings {
        val defaults = HomeKeyboardButtonBindings()
        return HomeKeyboardButtonGesture.entries.fold(defaults) { bindings, gesture ->
            val action = HomeKeyboardButtonAction.fromStorageId(prefs.getString(actionKey(gesture), null))
                ?: return@fold bindings
            bindings.with(
                gesture,
                HomeKeyboardButtonBinding(action, prefs.getString(targetKey(gesture), null)),
            )
        }
    }

    private companion object {
        fun actionKey(gesture: HomeKeyboardButtonGesture) = "home_keyboard_button_${gesture.storageId}_action"
        fun targetKey(gesture: HomeKeyboardButtonGesture) = "home_keyboard_button_${gesture.storageId}_target"
    }
}
