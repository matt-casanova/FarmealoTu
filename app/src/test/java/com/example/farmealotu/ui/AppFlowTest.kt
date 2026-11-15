package com.example.farmealotu.ui

import android.os.Looper
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.farmealotu.FarmealoTuApp
import com.example.farmealotu.MainActivity
import com.example.farmealotu.ui.screens.geo.OsmMapConfig
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.SQLiteMode
import java.util.concurrent.TimeUnit

/**
 * Recorrido de la interfaz en la JVM (Robolectric), con el repositorio DEMO:
 * Inicio -> 5 preguntas -> Resultado -> 4 módulos -> Explorar mi zona -> Datos de terreno,
 * más rotación de pantalla. No reemplaza la prueba F en un teléfono real.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
// SQLite «legacy»: evita cargar el runtime nativo de Robolectric, que falla en Windows cuando la
// carpeta del usuario tiene espacios. La app no usa base de datos propia (solo la caché del mapa).
@SQLiteMode(SQLiteMode.Mode.LEGACY)
class AppFlowTest {

    /** Antes de abrir la actividad: modo demo y mapa sin descargas (la prueba no sale a internet). */
    @get:Rule(order = 0)
    val demoSetup = object : ExternalResource() {
        override fun before() {
            OsmMapConfig.useDataConnection = false
            ApplicationProvider.getApplicationContext<FarmealoTuApp>().container.settings.setDemoMode(true)
        }
    }

    @get:Rule(order = 1)
    val rule = createAndroidComposeRule<MainActivity>()

    /** Deja correr las esperas del repositorio demo y vuelve a componer. */
    private fun settle(millis: Long = 2_000) {
        shadowOf(Looper.getMainLooper()).idleFor(millis, TimeUnit.MILLISECONDS)
        rule.waitForIdle()
    }

    private fun tap(text: String) {
        rule.onNodeWithText(text).performScrollTo().performClick()
        rule.waitForIdle()
    }

    private fun goHome() {
        rule.onNodeWithText("Inicio").performClick()
        rule.waitForIdle()
    }

    /** La app arranca directo en la pregunta 1 (primer uso, sin menú de abajo). */
    private fun answerFiveQuestions() {
        rule.onNodeWithText("¿Qué sembrás actualmente?").assertIsDisplayed()
        rule.onNodeWithText("Pregunta 1 de 5").assertIsDisplayed()
        // Texto libre con sinónimo: «soja» se reconoce como Soya.
        rule.onNode(hasSetTextAction()).performTextInput("soja")
        rule.onNodeWithText("Reconocido: Soya").assertExists()
        tap("Siguiente")

        rule.onNodeWithText("¿Qué sembraste antes?").assertIsDisplayed()
        tap("Sorgo")
        tap("Siguiente")

        rule.onNodeWithText("¿Qué te preocupa más?").assertIsDisplayed()
        tap("Falta de agua")
        tap("Siguiente")

        rule.onNodeWithText("¿Tenés riego?").assertIsDisplayed()
        tap("No")
        tap("Siguiente")

        rule.onNodeWithText("¿Querés contar algo de tu terreno?").assertIsDisplayed()
        tap("Seguir sin contar nada")

        rule.onNodeWithText("Revisá tus datos").assertIsDisplayed()
        rule.onNodeWithText("Tus respuestas").assertExists()
    }

    @Test
    fun recorridoCompletoEnModoDemo() {
        settle()
        // Primer uso: solo las preguntas (con la franja demo) y sin el menú de abajo.
        rule.onNodeWithText("MODO DEMO · DATOS DE EJEMPLO").assertIsDisplayed()
        rule.onNodeWithText("Mi Zona").assertDoesNotExist()

        answerFiveQuestions()
        tap("Comparar opciones de siembra")
        settle()

        // Al terminar las preguntas aparece el menú de abajo.
        rule.onNodeWithText("Mi Zona").assertExists()

        // Resultado principal.
        rule.onNodeWithText("Opción mejor posicionada").assertExists()
        rule.onNodeWithText("E2 · Cuidar agua").assertExists()
        rule.onNodeWithText("Verano: Soya").assertExists()
        rule.onNodeWithText("Invierno: Girasol").assertExists()
        rule.onNodeWithText("Revisá con tu técnico antes de decidir.").assertExists()
        rule.onNodeWithText("MODO DEMO · DATOS DE EJEMPLO").assertIsDisplayed()

        // Módulo 01.
        tap("Comparar todas")
        rule.onNodeWithText("Comparar formas de sembrar").assertIsDisplayed()
        rule.onNodeWithText("Puntaje para comparar opciones, no probabilidad de éxito.").assertExists()
        rule.onAllNodesWithText("¿Por qué esta opción?").assertCountEquals(4)
        rule.onNodeWithText("E0 · Actual").assertExists()

        // Cambiar prioridades -> «Actualizar comparación» -> el servidor (demo) recalcula con los pesos nuevos.
        tap("Cambiar mis prioridades")
        tap("Volver a los valores iniciales (40 / 35 / 25)")
        tap("Actualizar comparación")
        settle()
        rule.onNodeWithText("Agua 40 % · Suelo 35 % · Riesgo 25 %").assertExists()

        // «¿Por qué esta opción?»: los 5 bloques.
        rule.onAllNodesWithText("¿Por qué esta opción?").onFirst().performScrollTo().performClick()
        rule.waitForIdle()
        listOf("Por qué se sugiere", "Evidencia", "Ventajas y riesgos", "Información que falta", "Preguntas para tu agrónomo")
            .forEach { rule.onNodeWithText(it).assertExists() }
        goHome()

        // Inicio ahora muestra la acción derivada del resultado.
        rule.onNodeWithText("Advertencias").assertExists()

        // Módulo 02: frases primero, radar plegado.
        tap("Ventajas y desventajas")
        rule.onNodeWithText("En pocas palabras").assertExists()
        rule.onNodeWithText("Los mismos números, en tabla").assertDoesNotExist()
        tap("Ver comparación técnica")
        rule.onNodeWithText("Los mismos números, en tabla").assertExists()
        goHome()

        // Módulo 03: indicadores + mapa de calor.
        tap("Conocer los riesgos")
        settle()
        rule.onNodeWithText("Riesgo por mes y año").assertExists()
        rule.onNodeWithText("Baja cobertura del suelo").assertExists()
        rule.onNodeWithText("Lluvia intensa no es un pronóstico de inundación.").assertExists()
        tap("Ver como tabla")
        rule.onNodeWithText("s/d = sin dato").assertExists()
        goHome()

        // Módulo 04: rangos P10–P90.
        tap("¿Qué tan segura es esta comparación?")
        rule.onNodeWithText("Cuánto puede variar cada opción").assertExists()
        rule.onNodeWithText("Rangos según años pasados; no predicen la cosecha.").assertExists()
        rule.onNodeWithText("Qué viene de NASA").assertExists()
        rule.onNodeWithText("Información que falta").assertExists()
        goHome()

        // Datos de terreno: insignia de origen siempre visible.
        tap("Datos de terreno")
        settle()
        rule.onNodeWithText("ORIGEN: SIMULADO").assertExists()
        rule.onNodeWithText("No cambian la recomendación.").assertExists()
        rule.onNodeWithText("Dato regional (~50 km): contexto, no tu campo").assertExists()
        rule.onNodeWithText("Humedad del suelo por zona (%)").assertExists()
        rule.onNodeWithText("Temperatura del suelo por zona (°C)").assertExists()
        goHome()

        // Explorar mi zona: mapa + 5 capas.
        tap("Explorar mi zona")
        settle()
        rule.onNodeWithText("Las 5 capas").assertExists()
        rule.onNodeWithText("Capa: Lluvia").assertExists()
        tap("Agua")
        rule.onNodeWithText("Capa: Agua").assertExists()
        tap("Vegetación")
        rule.onNodeWithText("Sin datos para esta capa.").assertExists()
        goHome()

        // Fuentes y conexión.
        tap("Fuentes y límites")
        settle()
        rule.onNodeWithText("Quién hace qué").assertExists()
        goHome()
        tap("Configurar conexión")
        rule.onNodeWithText("Usar datos de ejemplo").assertExists()
        goHome()

        // Rotación de pantalla: el resultado y las respuestas se conservan.
        rule.activityRule.scenario.recreate()
        settle()
        rule.onNodeWithText("Conviene revisar la opción «E2 · Cuidar agua».").assertExists()
        rule.onNodeWithText("Advertencias").assertExists()
        tap("Ver resultado")
        rule.onNodeWithText("Opción mejor posicionada").assertExists()
    }

    @Test
    fun alIniciarSoloAparecenLasPreguntasSinMenu() {
        settle()
        rule.onNodeWithText("¿Qué sembrás actualmente?").assertIsDisplayed()
        rule.onNodeWithText("Pregunta 1 de 5").assertIsDisplayed()
        listOf("Inicio", "Siembra", "Riesgos", "Mi Zona", "Más").forEach { rule.onNodeWithText(it).assertDoesNotExist() }
    }

    @Test
    fun cultivoNoReconocidoPideElegirDeLaLista() {
        settle()
        rule.onNode(hasSetTextAction()).performTextInput("quinua")
        tap("Siguiente")
        // El aviso queda a la vista (junto al botón) y no se avanza de pregunta.
        rule.onNodeWithText("No reconocí «quinua». Elegí un cultivo de la lista.").assertIsDisplayed()
        rule.onNodeWithText("¿Qué sembrás actualmente?").assertExists()
        rule.onNodeWithText("¿Qué sembraste antes?").assertDoesNotExist()
    }

    @Test
    fun alSalirDelModoDemoSeBorranLosResultadosDeEjemplo() {
        settle()
        answerFiveQuestions()
        tap("Comparar opciones de siembra")
        settle()
        rule.onNodeWithText("Opción mejor posicionada").assertExists()

        // Servidor inexistente en este equipo: la conexión se rechaza de inmediato.
        val settings = ApplicationProvider.getApplicationContext<FarmealoTuApp>().container.settings
        rule.runOnUiThread {
            settings.setBaseUrl("http://127.0.0.1:9/")
            settings.setDemoMode(false)
        }
        settle()
        rule.onNodeWithText("MODO DEMO · DATOS DE EJEMPLO").assertDoesNotExist()
        rule.onNodeWithText("Opción mejor posicionada").assertDoesNotExist()
        rule.onNodeWithText("Primero contanos de tu campo").assertExists()
    }
}
