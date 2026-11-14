package com.example.farmealotu.ui.components

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import com.example.farmealotu.AppSettings
import com.example.farmealotu.ConnectionSettings
import com.example.farmealotu.data.repository.FakeFarmealoTuRepository
import com.example.farmealotu.domain.Concern
import com.example.farmealotu.domain.Crop
import com.example.farmealotu.domain.Irrigation
import com.example.farmealotu.viewmodel.FieldDataViewModel
import com.example.farmealotu.viewmodel.GeoViewModel
import com.example.farmealotu.viewmodel.MainViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.json.Json

private val previewRepository = FakeFarmealoTuRepository(
    json = Json { ignoreUnknownKeys = true },
    latencyMillis = 0,
)

private val previewSettings = MutableStateFlow(
    ConnectionSettings(baseUrl = "http://localhost:8000/", demoMode = true)
)

fun sampleMainViewModel(withComparison: Boolean = true): MainViewModel {
    val viewModel = MainViewModel(
        savedState = SavedStateHandle(),
        repositoryProvider = { previewRepository },
        settings = previewSettings,
    )
    viewModel.setCurrentCrop(Crop.SOY)
    viewModel.setPreviousCrop(Crop.MAIZE)
    viewModel.setConcern(Concern.WATER)
    viewModel.setIrrigation(Irrigation.NO)
    viewModel.setNotes("Suelo franco en zona llana.")
    if (withComparison) {
        viewModel.submitCompare()
        viewModel.loadIndicators()
        viewModel.loadSources()
    }
    return viewModel
}

fun sampleFieldDataViewModel(): FieldDataViewModel {
    val viewModel = FieldDataViewModel(
        repositoryProvider = { previewRepository },
        settings = previewSettings,
    )
    viewModel.refresh()
    return viewModel
}

fun sampleGeoViewModel(): GeoViewModel {
    val viewModel = GeoViewModel(
        repositoryProvider = { previewRepository },
        settings = previewSettings,
    )
    viewModel.load(force = true)
    return viewModel
}

fun sampleAppSettings(context: Context): AppSettings {
    val prefs = context.getSharedPreferences("preview_app_settings", Context.MODE_PRIVATE)
    val defaults = ConnectionSettings("http://localhost:8000/", demoMode = true)
    return AppSettings(prefs, defaults)
}
