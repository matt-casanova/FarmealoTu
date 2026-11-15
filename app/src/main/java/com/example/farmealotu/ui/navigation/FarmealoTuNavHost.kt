package com.example.farmealotu.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.farmealotu.ui.components.FarmIcons
import com.example.farmealotu.ui.components.FarmMotion
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.farmealotu.AppSettings
import com.example.farmealotu.ui.screens.compare.CompareRotationsScreen
import com.example.farmealotu.ui.screens.compare.StrategyDetailScreen
import com.example.farmealotu.ui.screens.decision.DecisionScreen
import com.example.farmealotu.ui.screens.home.HomeScreen
import com.example.farmealotu.ui.screens.input.FarmerInputScreen
import com.example.farmealotu.viewmodel.AppViewModelFactory
import com.example.farmealotu.viewmodel.MainViewModel
import com.example.farmealotu.viewmodel.dataOrNull

object Routes {
    const val HOME = "home"
    const val INPUT = "input?summary={summary}"
    const val DECISION = "decision"
    const val COMPARE = "compare?priorities={priorities}"
    const val STRATEGY = "strategy/{id}"
    const val TRADEOFFS = "tradeoffs"
    const val RISKS = "risks"
    const val UNCERTAINTY = "uncertainty"
    const val GEO = "geo"
    const val FIELD = "field"
    const val CONNECTION = "connection"
    const val SOURCES = "sources"

    fun input(summary: Boolean) = "input?summary=$summary"
    fun compare(priorities: Boolean = false) = "compare?priorities=$priorities"
    fun strategy(id: String) = "strategy/${android.net.Uri.encode(id)}"
}

/** Destinos de las pestañas del menú de abajo: ir de una a otra no es avanzar ni volver. */
private val TAB_ROOTS = setOf(Routes.HOME, Routes.DECISION, Routes.RISKS, Routes.GEO, Routes.SOURCES)

private fun isTabSwitch(from: String?, to: String?): Boolean = from in TAB_ROOTS && to in TAB_ROOTS

private data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val isSelected: (String?) -> Boolean,
    val onClick: () -> Unit,
)

/**
 * Navegación principal con barra de navegación inferior permanente (`Bottom Navigation Bar`).
 */
@Composable
fun FarmealoTuNavHost(
    settings: AppSettings,
    navController: NavHostController = rememberNavController(),
) {
    val viewModel: MainViewModel = viewModel(factory = AppViewModelFactory.Factory)
    val connection by settings.state.collectAsStateWithLifecycle()
    val demoMode = connection.demoMode

    val compareState by viewModel.compare.collectAsStateWithLifecycle()
    val inputState by viewModel.input.collectAsStateWithLifecycle()

    // Primer uso: solo las preguntas, sin el menú de abajo. El menú aparece al terminar de responder
    // (o si ya había respuestas / un resultado guardado). Se conserva al rotar la pantalla.
    var onboarded by rememberSaveable { mutableStateOf(inputState.isComplete || compareState.dataOrNull != null) }
    val startRoute = rememberSaveable { if (onboarded) Routes.HOME else Routes.INPUT }

    val goHome: () -> Unit = { navController.popBackStack(Routes.HOME, inclusive = false) }
    val goBack: () -> Unit = { if (!navController.popBackStack()) goHome() }
    val open: (String) -> Unit = { route -> navController.navigate(route) { launchSingleTop = true } }
    val editData: () -> Unit = { open(Routes.input(summary = true)) }
    val openWhy: (String) -> Unit = { id -> open(Routes.strategy(id)) }

    val density = LocalDensity.current
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val navItems = listOf(
        BottomNavItem(
            route = Routes.HOME,
            label = "Inicio",
            icon = FarmIcons.Home,
            isSelected = { route -> route == Routes.HOME },
            onClick = {
                navController.navigate(Routes.HOME) {
                    popUpTo(Routes.HOME) { inclusive = true }
                    launchSingleTop = true
                }
            }
        ),
        BottomNavItem(
            route = Routes.DECISION,
            label = "Siembra",
            icon = FarmIcons.Leaf,
            // El recorrido de siembra completo: preguntas, resultado y los módulos 01, 02 y 04
            // (el 03 tiene su propia pestaña «Riesgos»).
            isSelected = { route ->
                route == Routes.DECISION || route?.startsWith("input") == true ||
                    route?.startsWith("compare") == true || route?.startsWith("strategy") == true ||
                    route == Routes.TRADEOFFS || route == Routes.UNCERTAINTY
            },
            onClick = {
                val target = if (compareState.dataOrNull != null) Routes.DECISION else Routes.input(summary = inputState.isComplete)
                open(target)
            }
        ),
        BottomNavItem(
            route = Routes.RISKS,
            label = "Riesgos",
            icon = FarmIcons.Cloud,
            isSelected = { route -> route == Routes.RISKS },
            onClick = { open(Routes.RISKS) }
        ),
        BottomNavItem(
            route = Routes.GEO,
            label = "Mi Zona",
            icon = FarmIcons.Map,
            isSelected = { route -> route == Routes.GEO || route == Routes.FIELD },
            onClick = { open(Routes.GEO) }
        ),
        BottomNavItem(
            route = Routes.SOURCES,
            label = "Más",
            icon = FarmIcons.More,
            isSelected = { route -> route == Routes.SOURCES || route == Routes.CONNECTION },
            onClick = { open(Routes.SOURCES) }
        )
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            // Al terminar las preguntas el menú sube suave desde abajo (no aparece de golpe).
            AnimatedVisibility(
                visible = onboarded,
                enter = slideInVertically(FarmMotion.enter(FarmMotion.LONG)) { it } + fadeIn(FarmMotion.enter(FarmMotion.MEDIUM)),
            ) {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
            ) {
                navItems.forEach { item ->
                    val selected = item.isSelected(currentRoute)
                    NavigationBarItem(
                        selected = selected,
                        onClick = item.onClick,
                        // El ícono es decorativo: la etiqueta ya nombra el destino para TalkBack.
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = {
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                softWrap = false,
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    )
                }
            }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            NavHost(
                navController = navController,
                startDestination = startRoute,
                // Eje compartido (Material): avanzar entra desde la derecha, volver desde la izquierda;
                // cambiar de pestaña del menú es un fundido sin desplazamiento.
                enterTransition = {
                    if (isTabSwitch(initialState.destination.route, targetState.destination.route)) FarmMotion.tabEnter
                    else FarmMotion.forwardEnter(density)
                },
                exitTransition = {
                    if (isTabSwitch(initialState.destination.route, targetState.destination.route)) FarmMotion.tabExit
                    else FarmMotion.forwardExit(density)
                },
                popEnterTransition = {
                    if (isTabSwitch(initialState.destination.route, targetState.destination.route)) FarmMotion.tabEnter
                    else FarmMotion.backEnter(density)
                },
                popExitTransition = {
                    if (isTabSwitch(initialState.destination.route, targetState.destination.route)) FarmMotion.tabExit
                    else FarmMotion.backExit(density)
                },
                modifier = Modifier.fillMaxSize()
            ) {
                composable(Routes.HOME) {
                    val compare by viewModel.compare.collectAsStateWithLifecycle()
                    val input by viewModel.input.collectAsStateWithLifecycle()
                    HomeScreen(
                        viewModel = viewModel,
                        connection = connection,
                        onCompareOptions = {
                            when {
                                compare.dataOrNull != null -> open(Routes.DECISION)
                                else -> open(Routes.input(summary = input.isComplete))
                            }
                        },
                        onOpenDecision = { open(Routes.DECISION) },
                        onOpenCompare = { open(Routes.compare()) },
                        onOpenTradeoffs = { open(Routes.TRADEOFFS) },
                        onOpenRisks = { open(Routes.RISKS) },
                        onOpenUncertainty = { open(Routes.UNCERTAINTY) },
                        onOpenGeo = { open(Routes.GEO) },
                        onOpenField = { open(Routes.FIELD) },
                        onOpenConnection = { open(Routes.CONNECTION) },
                        onOpenSources = { open(Routes.SOURCES) },
                    )
                }

                composable(
                    route = Routes.INPUT,
                    arguments = listOf(navArgument("summary") { type = NavType.BoolType; defaultValue = false }),
                ) { entry ->
                    FarmerInputScreen(
                        viewModel = viewModel,
                        demoMode = demoMode,
                        startAtSummary = entry.arguments?.getBoolean("summary") ?: false,
                        onExit = goBack,
                        onHome = goHome,
                        onCompare = {
                            // Terminó las preguntas: aparece el menú y queda Inicio → Resultado en la pila
                            // (en el primer uso Inicio todavía no existía en la pila).
                            onboarded = true
                            navController.navigate(Routes.HOME) {
                                popUpTo(navController.graph.id) { inclusive = true }
                                launchSingleTop = true
                            }
                            navController.navigate(Routes.DECISION) { launchSingleTop = true }
                        },
                    )
                }

                composable(Routes.DECISION) {
                    DecisionScreen(
                        viewModel = viewModel,
                        demoMode = demoMode,
                        onBack = goBack,
                        onHome = goHome,
                        onEditData = editData,
                        onOpenCompare = { open(Routes.compare()) },
                        onChangePriorities = { open(Routes.compare(priorities = true)) },
                        onOpenWhy = openWhy,
                        onOpenTradeoffs = { open(Routes.TRADEOFFS) },
                        onOpenRisks = { open(Routes.RISKS) },
                        onOpenUncertainty = { open(Routes.UNCERTAINTY) },
                    )
                }

                composable(
                    route = Routes.COMPARE,
                    arguments = listOf(navArgument("priorities") { type = NavType.BoolType; defaultValue = false }),
                ) { entry ->
                    CompareRotationsScreen(
                        viewModel = viewModel,
                        demoMode = demoMode,
                        onBack = goBack,
                        onHome = goHome,
                        onEditData = editData,
                        onOpenWhy = openWhy,
                        expandPriorities = entry.arguments?.getBoolean("priorities") ?: false,
                    )
                }

                composable(
                    route = Routes.STRATEGY,
                    arguments = listOf(navArgument("id") { type = NavType.StringType }),
                ) { entry ->
                    StrategyDetailScreen(
                        viewModel = viewModel,
                        strategyId = entry.arguments?.getString("id").orEmpty(),
                        demoMode = demoMode,
                        onBack = goBack,
                        onHome = goHome,
                        onEditData = editData,
                    )
                }

                moduleDestinations(viewModel, demoMode, goBack, goHome, editData, openWhy, open)
                extraDestinations(viewModel, settings, demoMode, goBack, goHome)
            }
        }
    }
}
