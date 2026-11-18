package com.katoaapps.openminilaunch.ui.quickdrawer

internal enum class QuickDrawerDestination {
    ALL_APPS,
    PRIVATE_SPACE,
    WORK_SPACE,
}

internal fun quickDrawerDestinations(
    showPrivateSpace: Boolean,
    privateSpaceAvailable: Boolean,
    showWorkSpace: Boolean,
    workSpaceAvailable: Boolean,
): List<QuickDrawerDestination> = buildList {
    add(QuickDrawerDestination.ALL_APPS)
    if (showPrivateSpace && privateSpaceAvailable) add(QuickDrawerDestination.PRIVATE_SPACE)
    if (showWorkSpace && workSpaceAvailable) add(QuickDrawerDestination.WORK_SPACE)
}
