package com.katoaapps.openminilaunch.ui.mindyspace.core

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.mindyspace.MindySpaceMiniApp
import com.katoaapps.openminilaunch.ui.mindyspace.calculator.CalculatorMindySpaceApp
import com.katoaapps.openminilaunch.ui.mindyspace.music.MusicMindySpaceApp
import com.katoaapps.openminilaunch.ui.mindyspace.todo.TodoMindySpaceApp
import com.katoaapps.openminilaunch.ui.mindyspace.widget.WidgetMindySpaceApp

/** The one registration point for built-in MindySpace mini-apps. */
internal object MindySpaceMiniAppCatalog {
    private val registrations = listOf(
        MindySpaceMiniAppRegistration(TodoMindySpaceApp.Manifest) { dependencies ->
            TodoMindySpaceApp(
                store = dependencies.store,
                openTodos = dependencies.openTodos,
                jumpToken = dependencies.todoJumpToken,
                itemsPerPage = dependencies.todoItemsPerPage,
                textMetrics = dependencies.todoTextMetrics,
            )
        },
        MindySpaceMiniAppRegistration(CalculatorMindySpaceApp.Manifest) {
            CalculatorMindySpaceApp()
        },
        MindySpaceMiniAppRegistration(
            manifest = MindySpaceMiniAppManifest(
                miniApp = MindySpaceMiniApp.RECORDER,
                labelRes = R.string.recorder,
                icon = Icons.Default.Mic,
            ),
            create = null,
        ),
        MindySpaceMiniAppRegistration(MusicMindySpaceApp.Manifest) {
            MusicMindySpaceApp()
        },
    ) + MindySpaceMiniApp.quickWidgets.map { slot ->
        MindySpaceMiniAppRegistration(WidgetMindySpaceApp.manifestFor(slot)) { dependencies ->
            WidgetMindySpaceApp(
                store = dependencies.store,
                actions = dependencies.actions,
                slot = slot,
            )
        }
    }

    private val byMiniApp = registrations.associateBy { it.manifest.miniApp }

    init {
        check(
            registrations.size == MindySpaceMiniApp.entries.size &&
                byMiniApp.keys == MindySpaceMiniApp.entries.toSet(),
        ) {
            "Every MindySpaceMiniApp must have exactly one catalog registration"
        }
        check(registrations.none { it.manifest.miniApp.readyForHome && it.create == null }) {
            "Every ready MindySpace mini-app must provide a runtime implementation"
        }
        check(
            registrations.all { registration ->
                registration.manifest.supportsTotalScreen ==
                    (registration.createTotalScreen != null)
            },
        ) {
            "Every total-screen MindySpace manifest must provide a total-screen factory"
        }
    }

    fun get(miniApp: MindySpaceMiniApp): MindySpaceMiniAppManifest =
        checkNotNull(byMiniApp[miniApp]) {
            "Missing MindySpace manifest for ${miniApp.stableId}"
        }.manifest

    val totalScreenMiniApps: List<MindySpaceMiniApp>
        get() = registrations.mapNotNull { registration ->
            registration.manifest.miniApp.takeIf {
                registration.createTotalScreen != null
            }
        }

    fun createTotalScreenApp(
        miniApp: MindySpaceMiniApp,
        dependencies: MindySpaceTotalScreenDependencies,
    ): MindySpaceTotalScreenApp? = byMiniApp[miniApp]
        ?.createTotalScreen
        ?.invoke(dependencies)
        ?.also { app ->
            check(app.manifest == get(miniApp)) {
                "Total-screen factory manifest does not match the catalog entry for " +
                    miniApp.stableId
            }
        }

    fun createRegistry(dependencies: MindySpaceHostDependencies): MindySpaceMiniAppRegistry =
        MindySpaceMiniAppRegistry(
            registrations.mapNotNull { registration ->
                registration.create?.invoke(dependencies)?.also { app ->
                    check(app.manifest == registration.manifest) {
                        "Factory manifest does not match the catalog entry for " +
                            registration.manifest.miniApp.stableId
                    }
                }
            },
        )
}
