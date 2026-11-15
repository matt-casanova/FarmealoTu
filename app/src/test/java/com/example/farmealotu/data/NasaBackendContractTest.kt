package com.example.farmealotu.data

import com.example.farmealotu.data.model.DtoMappers
import com.example.farmealotu.data.remote.ApiClient
import com.example.farmealotu.domain.LayerAvailability
import com.example.farmealotu.domain.LayerKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contrato real de la rama alvin/datos-nasa (dev_main.py), capturado el 14-nov-2026 con
 * uvicorn local. Son extractos recortados de /api/nasa/status, /api/indicators y /api/map/layers.
 */
class NasaBackendContractTest {

    private val json = ApiClient.json

    @Test
    fun `estado NASA separa ultima observacion de la fecha de descarga`() {
        val status = DtoMappers.toNasaStatus(
            json.parseToJsonElement(
                """{"generated_at":"2026-11-15T00:32:55+00:00","reference_date":"2026-11-14",
                    "last_observation_date":"2026-11-09","last_download":"2026-11-14T17:06:17+00:00",
                    "sources":[{"id":"power_daily","status":"available","from_cache":true,
                                "last_observation_date":"2026-11-09","scale":"contexto regional (~50 km)"},
                               {"id":"smap_l4","status":"unavailable","reason":"P2: sin archivos descargados"}]}"""
            )
        )
        assertEquals("2026-11-09", status.lastObservationDate)
        assertEquals("2026-11-14T17:06:17+00:00", status.lastDownload)
        assertEquals(true, status.usesCache)
    }

    @Test
    fun `indicadores reales conservan null y la grilla mensual`() {
        val result = DtoMappers.toIndicators(
            json.parseToJsonElement(
                """{"last_observation_date":"2026-11-11","period":"2026-01-01 → 2026-11-11",
                    "indicators":[
                      {"id":"rain_total","name":"Lluvia acumulada","value":1108.9,"unit":"mm","evidence_status":"derived",
                       "source":"NASA POWER PRECTOTCORR","scale":"contexto regional (~50 km)",
                       "quality":{"valid_days":315,"n_days":315,"missing_pct":0.0}},
                      {"id":"et0_total","name":"ET0","value":null,"unit":"mm","evidence_status":"derived",
                       "quality":{"valid_days":315,"n_days":315,"missing_pct":0.0}}],
                    "monthly_grid":{"years":[2015],"months":[1,2],"indicators":[
                      {"id":"rain_anomaly","name":"Anomalía","unit":"%","source":"NASA POWER","evidence_status":"derived",
                       "cells":[{"year":2015,"month":1,"value":12.5,"valid_days":31,"quality":"ok"},
                                {"year":2015,"month":2,"value":null,"valid_days":0,"quality":"missing"}]}]}}"""
            )
        )
        assertEquals("2026-11-11", result.lastObservationDate)
        assertEquals(2, result.summaries.size)
        assertNull("null no debe volverse 0", result.summaries.first { it.id == "et0_total" }.valueText)
        assertEquals("mm", result.summaries.first { it.id == "rain_total" }.unit)
        assertEquals(315, result.summaries.first { it.id == "rain_total" }.validDays)
        val grid = result.grids.single()
        assertEquals(2, grid.cells.size)
        assertNull(grid.cells.first { it.month == 2 }.value)
        assertEquals("missing", grid.cells.first { it.month == 2 }.quality)
    }

    @Test
    fun `capas GIBS reales traen plantilla de tesela y atribucion de la raiz`() {
        val layers = DtoMappers.toMapLayers(
            json.parseToJsonElement(
                """{"attribution":"Imagery: NASA GIBS (Global Imagery Browse Services), NASA EOSDIS",
                    "layers":[
                     {"id":"rain","name":"Lluvia","status":"available","date":"2026-11-13","resolution":"~10 km (0.1°)",
                      "legend":[{"label":"0.2","color":"#009424"}],"unit":"mm/hr","max_zoom":6,
                      "tile_url":"https://gibs.earthdata.nasa.gov/wmts/epsg3857/best/IMERG_Precipitation_Rate/default/2026-11-13/GoogleMapsCompatible_Level6/{TileMatrix}/{TileRow}/{TileCol}.png"},
                     {"id":"water","name":"Agua","status":"unavailable","tile_url":null,
                      "reason":"Requiere la ET0 FAO-56"}]}"""
            )
        )
        val rain = layers.first { it.kind == LayerKind.RAIN }
        assertEquals(LayerAvailability.AVAILABLE, rain.availability)
        assertNotNull(rain.tileUrlTemplate)
        assertTrue(rain.tileUrlTemplate!!.contains("{TileMatrix}/{TileRow}/{TileCol}"))
        assertEquals("2026-11-13", rain.date)
        assertEquals("max_zoom del backend llega al visor", 6, rain.maxZoom)
        assertNull(layers.first { it.kind == LayerKind.WATER }.maxZoom)
        assertEquals("Imagery: NASA GIBS (Global Imagery Browse Services), NASA EOSDIS", rain.attribution)
        assertEquals(LayerAvailability.UNAVAILABLE, layers.first { it.kind == LayerKind.WATER }.availability)
        assertNull(layers.first { it.kind == LayerKind.WATER }.tileUrlTemplate)
        // Capas que el backend no envía: nunca se inventan.
        assertEquals(LayerAvailability.UNAVAILABLE, layers.first { it.kind == LayerKind.SOIL_MOISTURE }.availability)
    }
}
