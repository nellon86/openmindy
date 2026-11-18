package com.katoaapps.openminilaunch.ui.launcher.layout

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.launcher.home.HomeScreen
import com.katoaapps.openminilaunch.ui.launcher.home.HomeBackground
import com.katoaapps.openminilaunch.ui.magic.MagicBoxSessionState
import com.katoaapps.openminilaunch.ui.mindyspace.core.MindySpaceMiniAppCatalog
import com.katoaapps.openminilaunch.ui.mindyspace.core.MindySpacePresentationState
import com.katoaapps.openminilaunch.ui.mindyspace.core.MindySpaceTotalScreenDependencies
import com.katoaapps.openminilaunch.ui.mindyspace.core.MindySpaceTotalScreenHost
import com.katoaapps.openminilaunch.ui.settings.SettingsDestination
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.wellbeing.MindyDayScreen
import com.katoaapps.openminilaunch.ui.widgets.WidgetPage
import kotlinx.coroutines.launch

internal const val MINDY_DAY_PAGE = 0
internal const val HOME_PAGE = 1
internal const val WIDGET_PAGE = 2

/** Selects the single-page pager or the continuous two-panel launcher. */
@Composable
internal fun LauncherHomePager(
    store: LauncherStore,
    actions: DeviceActions,
    pagerState: PagerState,
    twoPanelState: LazyListState,
    focusedPage: Int,
    onFocusedPageChange: (Int) -> Unit,
    onTwoPanelActiveChange: (Boolean) -> Unit,
    homePageRequestToken: Int,
    keyboardInputEnabled: Boolean,
    homeRequestToken: Int,
    magicBoxSessionState: MagicBoxSessionState,
    mindySpacePresentation: MindySpacePresentationState,
    openSettings: (SettingsDestination) -> Unit,
    openTodos: () -> Unit,
    openConversations: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val contentLayoutDirection = LocalLayoutDirection.current
    val fold = currentFoldLayoutFeature()
    val homeStateHolder = rememberSaveableStateHolder()
    var settledPair by remember { mutableIntStateOf(1) }
    var initialized by remember { mutableStateOf(false) }
    var previousModeWasTwoPanel by remember { mutableStateOf(false) }
    var mindySpaceExpanded by remember { mutableStateOf(false) }
    val magicBoxExpanded = magicBoxSessionState.expanded

    LaunchedEffect(fold, configuration.smallestScreenWidthDp) {
        store.maybeEnableTwoPanelForUnfoldedBookDisplay(
            unfoldedBookDisplay = fold?.vertical == true &&
                configuration.smallestScreenWidthDp >= 600,
        )
    }

    BoxWithConstraints(modifier) {
        val geometry = twoPanelGeometry(
            enabled = store.twoPanelModeForLargeDisplays,
            smallestWidthDp = configuration.smallestScreenWidthDp,
            orientation = configuration.orientation,
            windowWidthPx = constraints.maxWidth,
            density = density.density,
            fold = fold,
        )
        val twoPanelActive = geometry != null

        LaunchedEffect(geometry) {
            if (twoPanelActive) {
                val pair = if (initialized && previousModeWasTwoPanel) settledPair
                    else pairForFocusedPage(focusedPage)
                settledPair = pair
                twoPanelState.scrollToItem(pair)
            } else if (!initialized || previousModeWasTwoPanel) {
                pagerState.scrollToPage(focusedPage)
            }
            previousModeWasTwoPanel = twoPanelActive
            initialized = true
            onTwoPanelActiveChange(twoPanelActive)
        }

        LaunchedEffect(pagerState.settledPage) {
            if (!twoPanelActive) onFocusedPageChange(pagerState.settledPage)
        }

        LaunchedEffect(
            twoPanelActive,
            twoPanelState.isScrollInProgress,
            twoPanelState.firstVisibleItemIndex,
        ) {
            if (twoPanelActive && initialized && !twoPanelState.isScrollInProgress) {
                val pair = twoPanelState.firstVisibleItemIndex.coerceIn(0, 1)
                if (pair != settledPair) {
                    settledPair = pair
                    onFocusedPageChange(newlyRevealedPage(pair))
                }
            }
        }

        LaunchedEffect(homePageRequestToken) {
            if (homePageRequestToken > 0) {
                onFocusedPageChange(HOME_PAGE)
                // A back/host request can arrive while the pager is still settling an edge swipe.
                // Jumping cancels that in-flight motion; starting a second animation makes RTL
                // layouts visibly oscillate between logical and physical directions.
                if (twoPanelActive) {
                    settledPair = 1
                    twoPanelState.scrollToItem(1)
                } else {
                    pagerState.scrollToPage(HOME_PAGE)
                }
            }
        }

        val goHome: () -> Unit = {
            onFocusedPageChange(HOME_PAGE)
            scope.launch {
                if (twoPanelActive) {
                    settledPair = 1
                    twoPanelState.animateScrollToItem(1)
                } else {
                    pagerState.animateScrollToPage(HOME_PAGE)
                }
            }
        }
        val goToMindyDay: () -> Unit = {
            onFocusedPageChange(MINDY_DAY_PAGE)
            scope.launch {
                if (twoPanelActive) {
                    settledPair = 0
                    twoPanelState.animateScrollToItem(0)
                } else {
                    pagerState.animateScrollToPage(MINDY_DAY_PAGE)
                }
            }
        }

        HomeBackground(
            store = store,
            modifier = Modifier.blur(
                if (magicBoxExpanded && (twoPanelActive || pagerState.currentPage == HOME_PAGE)) {
                    Dimens.dp10
                } else {
                    Dimens.dp0
                },
            ),
        )

        if (geometry != null) {
            LauncherTwoPanel(
                store = store,
                actions = actions,
                geometry = geometry,
                viewportWidthPx = constraints.maxWidth,
                listState = twoPanelState,
                homeStateHolder = homeStateHolder,
                magicBoxExpanded = magicBoxExpanded,
                mindySpaceExpanded = mindySpaceExpanded,
                homeRequestToken = homeRequestToken,
                magicBoxSessionState = magicBoxSessionState,
                mindySpacePresentation = mindySpacePresentation,
                onFocusPage = onFocusedPageChange,
                onMindyDay = goToMindyDay,
                onGoHome = goHome,
                openSettings = openSettings,
                openTodos = openTodos,
                openConversations = openConversations,
                onMindySpaceExpandedChange = { mindySpaceExpanded = it },
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            // These pages are fixed physical surfaces rather than reading-order content. Keeping
            // pager physics LTR avoids RTL settle oscillation while each page still renders using
            // the locale's real layout direction.
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    userScrollEnabled = !magicBoxExpanded && !mindySpaceExpanded,
                ) { page ->
                    CompositionLocalProvider(LocalLayoutDirection provides contentLayoutDirection) {
                        when (page) {
                            MINDY_DAY_PAGE -> MindyDayScreen(
                                store = store,
                                isActive = pagerState.currentPage == MINDY_DAY_PAGE,
                                goHome = goHome,
                            )
                            HOME_PAGE -> homeStateHolder.SaveableStateProvider("home") {
                                HomeScreen(
                                    store = store,
                                    actions = actions,
                                    openSettings = openSettings,
                                    openTodos = openTodos,
                                    openConversations = openConversations,
                                    openMindyDay = goToMindyDay,
                                    mindyStatusActive = pagerState.currentPage == HOME_PAGE,
                                    onMagicExpandedChange = {},
                                    onMindySpaceExpandedChange = { mindySpaceExpanded = it },
                                    keyboardInputEnabled = keyboardInputEnabled,
                                    homeRequestToken = homeRequestToken,
                                    magicBoxSessionState = magicBoxSessionState,
                                    mindySpacePresentation = mindySpacePresentation,
                                )
                            }
                            else -> WidgetPage(store = store, actions = actions, goHome = goHome)
                        }
                    }
                }
            }
        }

        mindySpacePresentation.totalScreenMiniApp?.let { miniApp ->
            val dependencies = remember(actions) {
                MindySpaceTotalScreenDependencies()
            }
            val totalScreenApp = remember(miniApp, dependencies) {
                MindySpaceMiniAppCatalog.createTotalScreenApp(miniApp, dependencies)
            }
            if (totalScreenApp == null) {
                LaunchedEffect(miniApp) { mindySpacePresentation.collapse(miniApp) }
            } else {
                MindySpaceTotalScreenHost(
                    store = store,
                    app = totalScreenApp,
                    onDismiss = { mindySpacePresentation.collapse(miniApp) },
                    onOpenConversations = openConversations,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
