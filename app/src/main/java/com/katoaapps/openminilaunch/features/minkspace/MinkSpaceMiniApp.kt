package com.katoaapps.openminilaunch.features.minkspace

/** Stable mini-app identities used by settings, persistence, and the MinkSpace registry. */
internal enum class MinkSpaceMiniApp(
    val stableId: String,
    val readyForHome: Boolean,
) {
    TODO("todo", readyForHome = true),
    CALCULATOR("calculator", readyForHome = true),
    RECORDER("recorder", readyForHome = false),
    MUSIC("music", readyForHome = true),
    /** Quick Widget 1 keeps the upstream "widget" ID so existing placements survive. */
    WIDGET("widget", readyForHome = true),
    WIDGET_2("widget_2", readyForHome = true),
    WIDGET_3("widget_3", readyForHome = true),
    ;

    /** Quick Widget slots hold an installation-local Android widget binding. */
    val isQuickWidget: Boolean get() = this in quickWidgets

    companion object {
        /** OpenMindy removed the upstream "media" (Memories, Adrift) and "vcard" mini-apps. */
        val quickWidgets: List<MinkSpaceMiniApp> = listOf(WIDGET, WIDGET_2, WIDGET_3)
        val defaultOrder: List<MinkSpaceMiniApp> = entries
        val defaultEnabled: Set<MinkSpaceMiniApp> = setOf(TODO)

        fun fromStableId(stableId: String): MinkSpaceMiniApp? =
            entries.firstOrNull { it.stableId == stableId }
    }
}
