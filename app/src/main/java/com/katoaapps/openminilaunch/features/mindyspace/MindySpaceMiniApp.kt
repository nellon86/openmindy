package com.katoaapps.openminilaunch.features.mindyspace

/** Stable mini-app identities used by settings, persistence, and the MindySpace registry. */
internal enum class MindySpaceMiniApp(
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
        val quickWidgets: List<MindySpaceMiniApp> = listOf(WIDGET, WIDGET_2, WIDGET_3)
        val defaultOrder: List<MindySpaceMiniApp> = entries
        val defaultEnabled: Set<MindySpaceMiniApp> = setOf(TODO)

        fun fromStableId(stableId: String): MindySpaceMiniApp? =
            entries.firstOrNull { it.stableId == stableId }
    }
}
