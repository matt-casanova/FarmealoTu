package com.example.farmealotu.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
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

/**
 * Navegación: Inicio -> 5 preguntas -> Resultado -> 01 Comparar / 02 Ventajas y desventajas /
 * 03 Riesgos / 04 Incertidumbre, con accesos a «Explorar mi zona» y «Datos de terreno».
 * El [MainViewModel] se comparte entre pantallas: respuestas y resultados se conservan al navegar.
 */
@Composable
fun FarmealoTuNavHost(
    settings: AppSettings,
    navController: NavHostController = rememberNavController(),
) {
    val viewModel: MainViewModel = viewModel(factory = AppViewModelFactory.Factory)
    val connection by settings.state.collectAsStateWithLifecycle()
    val demoMode = connection.demoMode

    val goHome: () -> Unit = { navController.popBackStack(Routes.HOME, inclusive = false) }
    val goBack: () -> Unit = { if (!navController.popBackStack()) goHome() }
    val open: (String) -> Unit = { route -> navController.navigate(route) { launchSingleTop = true } }
    val editData: () -> Unit = { open(Routes.input(summary = true)) }
    val openWhy: (String) -> Unit = { id -> open(Routes.strategy(id)) }

    NavHost(navController = navController, startDestination = Routes.HOME) {
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
                    // El resultado reemplaza al formulario en la pila: «volver» lleva al inicio.
                    navController.navigate(Routes.DECISION) {
                        popUpTo(Routes.HOME)
                        launchSingleTop = true
                    }
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
