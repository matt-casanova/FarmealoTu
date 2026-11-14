package com.example.farmealotu.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Íconos de la app, dibujados con los trazados de Material Icons (Google, licencia Apache 2.0).
 * Se escriben aquí como ImageVector para no sumar una dependencia de Gradle solo por unos pocos íconos.
 * Todos comparten la misma grilla de 24 × 24 y se tiñen con `Icon(tint = …)`.
 */
object FarmIcons {
    val ArrowBack: ImageVector by lazy {
        icon("ArrowBack", "M20,11H7.83l5.59,-5.59L12,4l-8,8 8,8 1.41,-1.41L7.83,13H20v-2z")
    }
    val Home: ImageVector by lazy {
        icon("Home", "M10,20v-6h4v6h5v-8h3L12,3 2,12h3v8z")
    }
    val ChevronRight: ImageVector by lazy {
        icon("ChevronRight", "M10,6L8.59,7.41 13.17,12l-4.58,4.59L10,18l6,-6z")
    }
    val Edit: ImageVector by lazy {
        icon(
            "Edit",
            "M3,17.25V21h3.75L17.81,9.94l-3.75,-3.75L3,17.25zM20.71,7.04c0.39,-0.39 0.39,-1.02 0,-1.41l-2.34,-2.34" +
                "c-0.39,-0.39 -1.02,-0.39 -1.41,0l-1.83,1.83 3.75,3.75 1.83,-1.83z",
        )
    }
    val Help: ImageVector by lazy {
        icon(
            "HelpOutline",
            "M11,18h2v-2h-2v2zM12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2zM12,20" +
                "c-4.41,0 -8,-3.59 -8,-8s3.59,-8 8,-8 8,3.59 8,8 -3.59,8 -8,8zM12,6c-2.21,0 -4,1.79 -4,4h2" +
                "c0,-1.1 0.9,-2 2,-2s2,0.9 2,2c0,2 -3,1.75 -3,5h2c0,-2.25 3,-2.5 3,-5 0,-2.21 -1.79,-4 -4,-4z",
        )
    }
    /** Hoja: «Siembra». */
    val Leaf: ImageVector by lazy {
        icon(
            "Eco",
            "M6.05,8.05c-2.73,2.73 -2.73,7.15 -0.02,9.88c1.47,-3.4 4.09,-6.24 7.36,-7.93c-2.77,2.34 -4.71,5.61 -5.39,9.32" +
                "c2.6,1.23 5.8,0.78 7.95,-1.37C19.43,14.47 20,4 20,4S9.53,4.57 6.05,8.05z",
        )
    }
    /** Nube: «Riesgos» del clima. */
    val Cloud: ImageVector by lazy {
        icon(
            "Cloud",
            "M19.35,10.04C18.67,6.59 15.64,4 12,4 9.11,4 6.6,5.64 5.35,8.04 2.34,8.36 0,10.91 0,14c0,3.31 2.69,6 6,6h13" +
                "c2.76,0 5,-2.24 5,-5 0,-2.64 -2.05,-4.78 -4.65,-4.96z",
        )
    }
    val Map: ImageVector by lazy {
        icon(
            "Map",
            "M20.5,3l-0.16,0.03L15,5.1 9,3 3.36,4.9c-0.21,0.07 -0.36,0.25 -0.36,0.48V20.5c0,0.28 0.22,0.5 0.5,0.5" +
                "l0.16,-0.03L9,18.9l6,2.1 5.64,-1.9c0.21,-0.07 0.36,-0.25 0.36,-0.48V3.5c0,-0.28 -0.22,-0.5 -0.5,-0.5z" +
                "M15,19l-6,-2.11V5l6,2.11V19z",
        )
    }
    val More: ImageVector by lazy {
        icon(
            "MoreHoriz",
            "M6,10c-1.1,0 -2,0.9 -2,2s0.9,2 2,2 2,-0.9 2,-2 -0.9,-2 -2,-2zM18,10c-1.1,0 -2,0.9 -2,2s0.9,2 2,2 2,-0.9 2,-2" +
                " -0.9,-2 -2,-2zM12,10c-1.1,0 -2,0.9 -2,2s0.9,2 2,2 2,-0.9 2,-2 -0.9,-2 -2,-2z",
        )
    }

    private fun icon(name: String, pathData: String): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).addPath(pathData = addPathNodes(pathData), fill = SolidColor(Color.Black)).build()
}
