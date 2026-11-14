package com.example.farmealotu.ui.navigation

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import com.example.farmealotu.ui.screens.field.FieldDataScreen
import com.example.farmealotu.ui.screens.geo.GeoViewerScreen
import com.example.farmealotu.ui.screens.settings.ConnectionScreen
import com.example.farmealotu.ui.screens.settings.SourcesScreen
import androidx.navigation.compose.composable
import com.example.farmealotu.ui.screens.risks.RisksScreen
import com.example.farmealotu.ui.screens.tradeoffs.TradeoffsScreen
import com.example.farmealotu.ui.screens.uncertainty.UncertaintyScreen
import com.example.farmealotu.AppSettings
import com.example.farmealotu.viewmodel.MainViewModel

/** Módulos 02, 03 y 04 (Fase 3). */
fun NavGraphBuilder.moduleDestinations(
    viewModel: MainViewModel,
    demoMode: Boolean,
    goBack: () -> Unit,
    goHome: () -> Unit,
    editData: () -> Unit,
    openWhy: (String) -> Unit,
    open: (String) -> Unit,
) {
    composable(Routes.TRADEOFFS) {
        TradeoffsScreen(viewModel, demoMode, onBack = goBack, onHome = goHome, onEditData = editData, onOpenWhy = openWhy)
    }
    composable(Routes.RISKS) {
        RisksScreen(viewModel, demoMode, onBack = goBack, onHome = goHome, onEditData = editData)
    }
    composable(Routes.UNCERTAINTY) {
        UncertaintyScreen(viewModel, demoMode, onBack = goBack, onHome = goHome, onEditData = editData)
    }
}

/** Visor geoespacial, datos de terreno, conexión y fuentes (Fase 4). */
fun NavGraphBuilder.extraDestinations(
    viewModel: MainViewModel,
    settings: AppSettings,
    demoMode: Boolean,
    goBack: () -> Unit,
    goHome: () -> Unit,
) {
    composable(Routes.GEO) {
        val input by viewModel.input.collectAsStateWithLifecycle()
        GeoViewerScreen(input = input, demoMode = demoMode, onBack = goBack, onHome = goHome)
    }
    composable(Routes.FIELD) {
        FieldDataScreen(demoMode = demoMode, onBack = goBack, onHome = goHome)
    }
    composable(Routes.CONNECTION) {
        ConnectionScreen(settings = settings, viewModel = viewModel, onBack = goBack, onHome = goHome)
    }
    composable(Routes.SOURCES) {
        SourcesScreen(viewModel = viewModel, demoMode = demoMode, onBack = goBack, onHome = goHome)
    }
}
