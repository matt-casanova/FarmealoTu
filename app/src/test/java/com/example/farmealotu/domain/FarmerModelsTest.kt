package com.example.farmealotu.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FarmerModelsTest {

    // ---------- Normalización de cultivos (diccionario del PDF 4.4) ----------

    @Test
    fun `sinonimos de soya llevan a soy`() {
        listOf("soya", "Soja", "SOY", "  soya  ").forEach { text ->
            assertEquals(text, CropMatch.Found(Crop.SOY), CropNormalizer.match(text))
        }
    }

    @Test
    fun `tildes y mayusculas se normalizan`() {
        assertEquals(CropMatch.Found(Crop.MAIZE), CropNormalizer.match("Maíz"))
        assertEquals(CropMatch.Found(Crop.MAIZE), CropNormalizer.match("MAIZ"))
        assertEquals(CropMatch.Found(Crop.SORGHUM), CropNormalizer.match("sorgo"))
        assertEquals(CropMatch.Found(Crop.SUNFLOWER), CropNormalizer.match("Girasol"))
        assertEquals(CropMatch.Found(Crop.WHEAT), CropNormalizer.match("trigo"))
        assertEquals(CropMatch.Found(Crop.FALLOW), CropNormalizer.match("Barbecho"))
        assertEquals(CropMatch.Found(Crop.FALLOW), CropNormalizer.match("descanso"))
    }

    @Test
    fun `los codigos de la api son los del contrato`() {
        assertEquals(
            listOf("soy", "maize", "sorghum", "sunflower", "wheat", "fallow"),
            Crop.entries.map { it.apiCode },
        )
    }

    @Test
    fun `un cultivo desconocido no se adivina`() {
        assertEquals(CropMatch.Unknown("quinua"), CropNormalizer.match("quinua"))
        assertEquals(CropMatch.Unknown("soyita"), CropNormalizer.match("soyita"))
    }

    @Test
    fun `texto vacio no es un cultivo`() {
        assertEquals(CropMatch.Empty, CropNormalizer.match("   "))
    }

    @Test
    fun `una frase con un solo cultivo se reconoce`() {
        assertEquals(CropMatch.Found(Crop.SOY), CropNormalizer.match("Estoy sembrando soya este año"))
    }

    @Test
    fun `una frase con dos cultivos es ambigua y pide elegir`() {
        val match = CropNormalizer.match("Estoy sembrando soya y antes tenía sorgo")
        assertEquals(CropMatch.Ambiguous(listOf(Crop.SOY, Crop.SORGHUM)), match)
    }

    @Test
    fun `barbecho no vale como cultivo actual`() {
        assertEquals(CropMatch.Unknown("barbecho"), CropNormalizer.match("barbecho", allowFallow = false))
    }

    // ---------- Pesos ----------

    @Test
    fun `los pesos iniciales son 40 35 25`() {
        assertEquals(Priorities(0.40, 0.35, 0.25), Priorities.DEFAULT)
        assertEquals(Priorities.DEFAULT, Concern.BALANCE.startingPriorities)
        assertEquals(0.55, Concern.WATER.startingPriorities.water, 1e-9)
    }

    @Test
    fun `los pesos se normalizan para sumar uno`() {
        val normalized = Priorities(2.0, 1.0, 1.0).normalized()!!
        assertEquals(0.5, normalized.water, 1e-9)
        assertEquals(0.25, normalized.soil, 1e-9)
        assertEquals(0.25, normalized.risk, 1e-9)
        assertEquals(1.0, normalized.water + normalized.soil + normalized.risk, 1e-9)
    }

    @Test
    fun `todas las preocupaciones dan pesos que suman uno`() {
        Concern.entries.forEach { concern ->
            val p = concern.startingPriorities
            assertEquals(concern.name, 1.0, p.water + p.soil + p.risk, 1e-9)
        }
    }

    @Test
    fun `pesos en cero negativos o no finitos no se normalizan`() {
        assertNull(Priorities(0.0, 0.0, 0.0).normalized())
        assertNull(Priorities(-0.1, 0.6, 0.5).normalized())
        assertNull(Priorities(Double.NaN, 0.5, 0.5).normalized())
    }

    // ---------- Ubicación y respuestas ----------

    @Test
    fun `el punto piloto esta dentro del area y uno lejano no`() {
        assertTrue(PilotArea.contains(PilotArea.LAT, PilotArea.LON))
        assertTrue(PilotArea.contains(-17.0, -62.0))
        assertFalse(PilotArea.contains(-16.5, -68.15))
        assertFalse(PilotArea.contains(Double.NaN, -62.0))
    }

    @Test
    fun `la entrada esta completa solo con las cuatro respuestas obligatorias`() {
        val partial = FarmerInput(currentCrop = Crop.SOY, previousCrop = Crop.SORGHUM)
        assertFalse(partial.isComplete)
        val complete = partial.copy(concern = Concern.WATER, irrigation = Irrigation.NO)
        assertTrue(complete.isComplete)
        assertEquals(listOf("soy", "sorghum"), complete.currentRotationCodes)
    }

    @Test
    fun `la confianza alta nunca se rotula como alta`() {
        assertEquals(ConfidenceDisplay.MEDIUM_CAPPED, ConfidenceDisplay.from("high"))
        assertEquals(ConfidenceDisplay.MEDIUM_CAPPED, ConfidenceDisplay.from("Alta"))
        assertEquals(ConfidenceDisplay.MEDIUM, ConfidenceDisplay.from("medium"))
        assertEquals(ConfidenceDisplay.LOW, ConfidenceDisplay.from("low"))
        assertEquals(ConfidenceDisplay.UNKNOWN, ConfidenceDisplay.from(null))
        assertFalse(ConfidenceDisplay.entries.any { it.label.equals("Alta", ignoreCase = true) })
    }
}
