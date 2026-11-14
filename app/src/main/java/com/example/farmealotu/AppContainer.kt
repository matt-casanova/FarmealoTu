package com.example.farmealotu

import android.content.Context
import android.content.SharedPreferences
import com.example.farmealotu.data.remote.ApiClient
import com.example.farmealotu.data.repository.FakeFarmealoTuRepository
import com.example.farmealotu.data.repository.FarmealoTuRepository
import com.example.farmealotu.data.repository.RemoteFarmealoTuRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** De dónde saca los datos la app. Se cambia desde la pantalla «Conexión». */
data class ConnectionSettings(
    val baseUrl: String,
    val demoMode: Boolean,
)

/**
 * Guarda la URL del backend y el modo demo. Los valores iniciales salen de BuildConfig
 * (local.properties, no versionado); lo que se cambie en la app queda en el teléfono.
 */
class AppSettings(
    private val prefs: SharedPreferences,
    private val defaults: ConnectionSettings,
) {
    private val _state = MutableStateFlow(
        ConnectionSettings(
            baseUrl = prefs.getString(KEY_URL, null)?.let(ApiClient::normalizeBaseUrl) ?: defaults.baseUrl,
            demoMode = prefs.getBoolean(KEY_DEMO, defaults.demoMode),
        )
    )
    val state: StateFlow<ConnectionSettings> = _state.asStateFlow()

    val defaultBaseUrl: String get() = defaults.baseUrl

    /** @return false si la URL no es válida (no se guarda nada). */
    fun setBaseUrl(raw: String): Boolean {
        val normalized = ApiClient.normalizeBaseUrl(raw) ?: return false
        prefs.edit().putString(KEY_URL, normalized).apply()
        _state.value = _state.value.copy(baseUrl = normalized)
        return true
    }

    fun setDemoMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DEMO, enabled).apply()
        _state.value = _state.value.copy(demoMode = enabled)
    }

    fun resetToDefaults() {
        prefs.edit().clear().apply()
        _state.value = defaults
    }

    private companion object {
        const val KEY_URL = "api_base_url"
        const val KEY_DEMO = "demo_mode"
    }
}

/** Inyección manual y simple: un contenedor por proceso. */
class AppContainer(context: Context) {

    val settings = AppSettings(
        prefs = context.getSharedPreferences("farmealotu_connection", Context.MODE_PRIVATE),
        defaults = ConnectionSettings(
            baseUrl = ApiClient.normalizeBaseUrl(BuildConfig.API_BASE_URL) ?: "http://10.0.2.2:8000/",
            demoMode = BuildConfig.DEMO_MODE_DEFAULT,
        ),
    )

    private val fakeRepository: FarmealoTuRepository by lazy { FakeFarmealoTuRepository(ApiClient.json) }

    private var remote: Pair<String, FarmealoTuRepository>? = null

    /** Repositorio vigente según la configuración: DEMO (fixtures rotulados) o API real. */
    @Synchronized
    fun repository(): FarmealoTuRepository {
        val current = settings.state.value
        if (current.demoMode) return fakeRepository
        remote?.takeIf { it.first == current.baseUrl }?.let { return it.second }
        val created = RemoteFarmealoTuRepository(
            api = ApiClient.create(current.baseUrl, debugLogging = BuildConfig.DEBUG),
            json = ApiClient.json,
        )
        remote = current.baseUrl to created
        return created
    }
}
