package com.katoaapps.openminilaunch.features.magic

/** The three gestures the round button at the end of the Home search bar understands. */
internal enum class HomeKeyboardButtonGesture(val storageId: String) {
    TAP("tap"),
    LONG_PRESS("long_press"),
    SWIPE_UP("swipe_up"),
}

/**
 * What the Home keyboard button does for one gesture.
 *
 * [storageId] is persisted, so never rename it. [magicRoute] marks actions that open the Magic
 * Box directly in one of its modes; those are resolved inside the Magic Box itself.
 */
internal enum class HomeKeyboardButtonAction(
    val storageId: String,
    val magicRoute: MagicBoxRoute? = null,
) {
    NONE("none"),

    // Magic Box
    MAGIC_BOX("magic_box", MagicBoxRoute.APP),
    VOICE_INPUT("voice_input"),
    WEB_SEARCH("web_search", MagicBoxRoute.SEARCH),
    ASSISTANT("assistant", MagicBoxRoute.ASSISTANT),
    NEW_TODO("new_todo", MagicBoxRoute.TODO),
    NEW_NOTE("new_note", MagicBoxRoute.NOTE),
    NEW_EVENT("new_event", MagicBoxRoute.EVENT),
    NEW_MESSAGE("new_message", MagicBoxRoute.MESSAGE),
    NEW_CALL("new_call", MagicBoxRoute.CALL),

    // Launcher surfaces
    QUICK_APPS("quick_apps"),
    ALL_APPS("all_apps"),
    CONVERSATIONS("conversations"),
    MINDY_SPACE_FULL_SCREEN("mindy_space_full_screen"),

    // System
    NOTIFICATIONS("notifications"),
    QUICK_SETTINGS("quick_settings"),
    CAMERA("camera"),
    FLASHLIGHT("flashlight"),
    LOCK_SCREEN("lock_screen"),
    LAUNCH_APP("launch_app"),
    ;

    companion object {
        fun fromStorageId(id: String?): HomeKeyboardButtonAction? =
            entries.firstOrNull { it.storageId == id }
    }
}

/** One gesture's action; [appTarget] is the launcher selection key used by [HomeKeyboardButtonAction.LAUNCH_APP]. */
internal data class HomeKeyboardButtonBinding(
    val action: HomeKeyboardButtonAction,
    val appTarget: String? = null,
) {
    val isActive: Boolean
        get() = action != HomeKeyboardButtonAction.NONE &&
            (action != HomeKeyboardButtonAction.LAUNCH_APP || !appTarget.isNullOrBlank())
}

internal data class HomeKeyboardButtonBindings(
    val tap: HomeKeyboardButtonBinding = HomeKeyboardButtonBinding(HomeKeyboardButtonAction.MAGIC_BOX),
    val longPress: HomeKeyboardButtonBinding =
        HomeKeyboardButtonBinding(HomeKeyboardButtonAction.MINDY_SPACE_FULL_SCREEN),
    val swipeUp: HomeKeyboardButtonBinding = HomeKeyboardButtonBinding(HomeKeyboardButtonAction.NONE),
) {
    operator fun get(gesture: HomeKeyboardButtonGesture): HomeKeyboardButtonBinding = when (gesture) {
        HomeKeyboardButtonGesture.TAP -> tap
        HomeKeyboardButtonGesture.LONG_PRESS -> longPress
        HomeKeyboardButtonGesture.SWIPE_UP -> swipeUp
    }

    fun with(gesture: HomeKeyboardButtonGesture, binding: HomeKeyboardButtonBinding) = when (gesture) {
        HomeKeyboardButtonGesture.TAP -> copy(tap = binding)
        HomeKeyboardButtonGesture.LONG_PRESS -> copy(longPress = binding)
        HomeKeyboardButtonGesture.SWIPE_UP -> copy(swipeUp = binding)
    }
}
