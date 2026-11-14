package com.example.farmealotu.ui.screens.geo

import android.content.Context
import com.example.farmealotu.BuildConfig
import android.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.MapTileProviderBasic
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.TilesOverlay
import java.io.File

data class MapPoint(val lat: Double, val lon: Double, val title: String, val description: String)

/** Capa de teselas publicada por el backend en /api/map/layers. La app nunca arma estas URL. */
data class TileLayerSpec(val id: String, val urlTemplate: String)

/** Permite apagar la descarga de teselas (se usa en las pruebas automáticas, que no deben salir a internet). */
object OsmMapConfig {
    @Volatile
    var useDataConnection: Boolean = true
}

private fun configureOsmdroid(context: Context) {
    val config = Configuration.getInstance()
    // Caché dentro del almacenamiento privado de la app: no pide permisos de almacenamiento.
    val base = File(context.cacheDir, "osmdroid")
    config.osmdroidBasePath = base
    config.osmdroidTileCache = File(base, "tiles")
    // La política de teselas de OpenStreetMap exige identificar a la aplicación con un nombre propio.
    // OSM bloquea (HTTP 403) los identificadores genéricos como «com.example.*», que es nuestro
    // applicationId: por eso no se usa context.packageName. Sin datos personales en el encabezado.
    config.userAgentValue = "FarmealoTu-Android/${BuildConfig.VERSION_NAME}"
}

/** Fuente XYZ/WMTS-REST a partir de una plantilla con {z}/{x}/{y} o {TileMatrix}/{TileCol}/{TileRow}. */
private class TemplateTileSource(name: String, private val template: String) :
    OnlineTileSourceBase(name, 0, 12, 256, "", arrayOf(template)) {

    override fun getTileURLString(pMapTileIndex: Long): String {
        val z = MapTileIndex.getZoom(pMapTileIndex).toString()
        val x = MapTileIndex.getX(pMapTileIndex).toString()
        val y = MapTileIndex.getY(pMapTileIndex).toString()
        return template
            .replace("{z}", z).replace("{x}", x).replace("{y}", y)
            .replace("{TileMatrix}", z).replace("{TileCol}", x).replace("{TileRow}", y)
    }
}

/** true si la plantilla trae los tres marcadores que esta versión sabe reemplazar. */
fun isDrawableTileTemplate(template: String): Boolean =
    (template.startsWith("https://") || template.startsWith("http://")) &&
        ((("{z}" in template) && ("{x}" in template) && ("{y}" in template)) ||
            (("{TileMatrix}" in template) && ("{TileCol}" in template) && ("{TileRow}" in template)))

/**
 * Mapa 2D con OpenStreetMap (osmdroid, sin clave comercial) integrado con AndroidView.
 * Muestra el punto piloto y, si existe, el punto indicado por el agricultor.
 */
@Composable
fun OsmMap(
    points: List<MapPoint>,
    tileLayer: TileLayerSpec?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember {
        configureOsmdroid(context.applicationContext)
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setUseDataConnection(OsmMapConfig.useDataConnection)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT)
            minZoomLevel = 5.0
            maxZoomLevel = 16.0
            controller.setZoom(9.0)
            // Dentro de una pantalla con scroll: mientras se toca el mapa, el mapa manda.
            setOnTouchListener { view, _ ->
                view.parent?.requestDisallowInterceptTouchEvent(true)
                false
            }
        }
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    val holder = remember { OverlayHolder() }
    AndroidView(
        modifier = modifier,
        factory = { mapView },
        update = { map ->
            // Capa de teselas: solo se reconstruye cuando cambia la capa elegida.
            if (holder.spec != tileLayer) {
                holder.tiles?.let { old ->
                    map.overlays.remove(old)
                    old.onDetach(map)
                }
                holder.tiles = if (tileLayer != null && isDrawableTileTemplate(tileLayer.urlTemplate)) {
                    val provider = MapTileProviderBasic(
                        map.context.applicationContext,
                        TemplateTileSource(tileLayer.id, tileLayer.urlTemplate),
                    )
                    TilesOverlay(provider, map.context).apply { loadingBackgroundColor = Color.TRANSPARENT }
                        .also { map.overlays.add(0, it) }
                } else {
                    null
                }
                holder.spec = tileLayer
            }
            // Marcadores: solo se recentra cuando cambian los puntos, no en cada redibujo.
            if (holder.points != points) {
                map.overlays.removeAll { it is Marker }
                points.forEach { point ->
                    map.overlays.add(
                        Marker(map).apply {
                            position = GeoPoint(point.lat, point.lon)
                            title = point.title
                            snippet = point.description
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        }
                    )
                }
                points.lastOrNull()?.let { map.controller.setCenter(GeoPoint(it.lat, it.lon)) }
                holder.points = points
            }
            if (!holder.copyrightAdded) {
                map.overlays.add(CopyrightOverlay(map.context))
                holder.copyrightAdded = true
            }
            map.invalidate()
        },
    )
}

private class OverlayHolder {
    var spec: TileLayerSpec? = null
    var tiles: TilesOverlay? = null
    var points: List<MapPoint>? = null
    var copyrightAdded: Boolean = false
}
