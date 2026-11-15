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
 * Splash -> 5 preguntas -> Resultado -> 4 módulos -> Explorar mi zona -> Datos de terreno.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
@SQLiteMode(SQLiteMode.Mode.LEGACY)
class AppFlowTest {

    @get:Rule(order = 0)
    val demoSetup = object : ExternalResource() {
        override fun before() {
            OsmMapConfig.useDataConnection = false
            ApplicationProvider.getApplicationContext<FarmealoTuApp>().container.settings.setDemoMode(true)
        }
    }

    @get:Rule(order = 1)
    val rule = createAndroidComposeRule<MainActivity>()

    private fun settle(millis: Long = 3_000) {
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

    private fun answerFiveQuestions() {
        rule.onNodeWithText("¿Qué sembrás actualmente?").assertIsDisplayed()
        rule.onNodeWithText("Pregunta 1 de 5").assertIsDisplayed()
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
        settle(3_000) // Espera a que termine el Splash Screen (Sowlt logo) y navegue a las 5 preguntas
        answerFiveQuestions()
        tap("Comparar opciones de siembra")
        settle(3_000)

        // Resultado principal.
        rule.onNodeWithText("Opción mejor posicionada").assertExists()
        rule.onNodeWithText("E2 · Cuidar agua").assertExists()
        rule.onNodeWithText("Verano: Soya").assertExists()
        rule.onNodeWithText("Invierno: Girasol").assertExists()
        rule.onNodeWithText("Revisá con tu técnico antes de decidir.").assertExists()

        // Módulo 01.
        tap("Comparar todas")
        rule.onNodeWithText("Comparar formas de sembrar").assertIsDisplayed()
        rule.onNodeWithText("Puntaje relativo entre opciones, no probabilidad de éxito.").assertExists()
        rule.onAllNodesWithText("¿Por qué esta opción?").assertCountEquals(4)
        rule.onNodeWithText("E0 · Actual").assertExists()

        // Cambiar prioridades
        tap("Cambiar mis prioridades")
        tap("Volver a los valores iniciales (40 / 35 / 25)")
        tap("Actualizar comparación")
        settle(3_000)
        rule.onNodeWithText("Agua 40 % · Suelo 35 % · Riesgo 25 %").assertExists()

        // «¿Por qué esta opción?»
        rule.onAllNodesWithText("¿Por qué esta opción?").onFirst().performScrollTo().performClick()
        rule.waitForIdle()
        listOf("Por qué se sugiere", "Evidencia", "Ventajas y riesgos", "Información que falta", "Preguntas para tu agrónomo")
            .forEach { rule.onNodeWithText(it).assertExists() }
        goHome()

        // Módulo 02
        tap("Ventajas y desventajas")
        rule.onNodeWithText("En pocas palabras").assertExists()
        rule.onNodeWithText("Los mismos números, en tabla").assertDoesNotExist()
        tap("Ver comparación técnica")
        rule.onNodeWithText("Los mismos números, en tabla").assertExists()
        goHome()

        // Módulo 03
        tap("Conocer los riesgos")
        settle(3_000)
        rule.onNodeWithText("Riesgo por mes y año").assertExists()
        rule.onNodeWithText("Baja cobertura del suelo").assertExists()
        rule.onNodeWithText("Estar expuesto a lluvia intensa no equivale a predecir un desborde del río.").assertExists()
        tap("Ver como tabla")
        rule.onNodeWithText("s/d = sin dato").assertExists()
        goHome()

        // Módulo 04
        tap("¿Qué tan segura es esta comparación?")
        rule.onNodeWithText("Cuánto puede variar cada opción").assertExists()
        rule.onNodeWithText("Qué viene de NASA").assertExists()
        rule.onNodeWithText("Información que falta").assertExists()
        goHome()

        // Datos de terreno
        tap("Datos de terreno")
        settle(3_000)
        rule.onNodeWithText("ORIGEN: SIMULADO").assertExists()
        rule.onNodeWithText("Estos datos no cambian la recomendación en esta versión.").assertExists()
        rule.onNodeWithText("Dato regional (~50 km): contexto, no tu campo").assertExists()
        rule.onNodeWithText("Humedad del suelo por zona (%)").assertExists()
        rule.onNodeWithText("Temperatura del suelo por zona (°C)").assertExists()
        goHome()

        // Explorar mi zona
        tap("Explorar mi zona")
        settle(3_000)
        rule.onNodeWithText("Las 5 capas").assertExists()
        rule.onNodeWithText("Capa: Lluvia").assertExists()
        tap("Agua")
        rule.onNodeWithText("Capa: Agua").assertExists()
        tap("Vegetación")
        rule.onNodeWithText("No hay datos válidos de esta capa. No se dibuja nada en su lugar.").assertExists()
        goHome()

        // Fuentes y conexión
        tap("Fuentes y límites")
        settle(3_000)
        rule.onNodeWithText("Quién hace qué").assertExists()
        goHome()
        tap("Configurar conexión")
        rule.onNodeWithText("Usar datos de ejemplo").assertExists()
        goHome()

        // Rotación de pantalla
        rule.activityRule.scenario.recreate()
        settle(3_000)
        rule.onNodeWithText("Conviene revisar la opción «E2 · Cuidar agua».").assertExists()
        rule.onNodeWithText("Advertencias").assertExists()
        tap("Ver resultado")
        rule.onNodeWithText("Opción mejor posicionada").assertExists()
    }

    @Test
    fun sinConsultaLosModulosInvitanARegistrarElCampo() {
        settle(3_000)
        answerFiveQuestions()
        tap("Comparar opciones de siembra")
        settle(3_000)
        tap("Comparar todas")
        rule.onNodeWithText("Comparar formas de sembrar").assertIsDisplayed()
    }

    @Test
    fun cultivoNoReconocidoPideElegirDeLaLista() {
        settle(3_000)
        rule.onNode(hasSetTextAction()).performTextInput("quinua")
        tap("Siguiente")
        rule.onNodeWithText("No reconocí «quinua». Elegí un cultivo de la lista.").assertIsDisplayed()
        rule.onNodeWithText("¿Qué sembrás actualmente?").assertExists()
        rule.onNodeWithText("¿Qué sembraste antes?").assertDoesNotExist()
    }

    @Test
    fun alSalirDelModoDemoSeBorranLosResultadosDeEjemplo() {
        settle(3_000)
        answerFiveQuestions()
        tap("Comparar opciones de siembra")
        settle(3_000)
        rule.onNodeWithText("Opción mejor posicionada").assertExists()

        val settings = ApplicationProvider.getApplicationContext<FarmealoTuApp>().container.settings
        rule.runOnUiThread {
            settings.setBaseUrl("http://127.0.0.1:9/")
            settings.setDemoMode(false)
        }
        settle(3_000)
        rule.onNodeWithText("Opción mejor posicionada").assertDoesNotExist()
    }
}
