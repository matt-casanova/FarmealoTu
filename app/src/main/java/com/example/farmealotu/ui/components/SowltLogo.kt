package com.example.farmealotu.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val SowltDarkGreen = Color(0xFF00563B)
val SowltMidGreen = Color(0xFF006847)
val SowltLeafGreen = Color(0xFF38B449)

/**
 * Logo oficial Sowlt dibujado en Canvas vectorial.
 * Representa la 'S' en forma de campo/camino agrícola con hojas superiores y el texto Sowlt.
 */
@Composable
fun SowltLogo(
    modifier: Modifier = Modifier,
    size: Dp = 180.dp,
    showText: Boolean = true
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height

            // 1. Hoja superior izquierda (verde claro)
            val leafLeft = Path().apply {
                moveTo(w * 0.58f, h * 0.22f)
                cubicTo(w * 0.52f, h * 0.12f, w * 0.55f, h * 0.05f, w * 0.65f, h * 0.08f)
                cubicTo(w * 0.70f, h * 0.15f, w * 0.65f, h * 0.22f, w * 0.58f, h * 0.22f)
                close()
            }
            drawPath(leafLeft, color = SowltLeafGreen)

            // 2. Hoja superior derecha (verde medio)
            val leafRight = Path().apply {
                moveTo(w * 0.64f, h * 0.25f)
                cubicTo(w * 0.68f, h * 0.10f, w * 0.78f, h * 0.02f, w * 0.88f, h * 0.06f)
                cubicTo(w * 0.82f, h * 0.18f, w * 0.72f, h * 0.26f, w * 0.64f, h * 0.25f)
                close()
            }
            drawPath(leafRight, color = SowltMidGreen)

            // 3. Trazo superior en 'S' (verde oscuro / verde medio)
            val topS = Path().apply {
                moveTo(w * 0.64f, h * 0.25f)
                cubicTo(w * 0.55f, h * 0.22f, w * 0.38f, h * 0.18f, w * 0.28f, h * 0.30f)
                cubicTo(w * 0.16f, h * 0.45f, w * 0.35f, h * 0.55f, w * 0.60f, h * 0.52f)
                cubicTo(w * 0.32f, h * 0.50f, w * 0.18f, h * 0.38f, w * 0.32f, h * 0.25f)
                cubicTo(w * 0.42f, h * 0.16f, w * 0.58f, h * 0.18f, w * 0.64f, h * 0.25f)
                close()
            }
            drawPath(topS, color = SowltMidGreen)

            // 4. Base del campo (verde verde claro con forma de colina)
            val fieldBase = Path().apply {
                moveTo(w * 0.10f, h * 0.70f)
                cubicTo(w * 0.25f, h * 0.48f, w * 0.68f, h * 0.52f, w * 0.80f, h * 0.65f)
                cubicTo(w * 0.82f, h * 0.75f, w * 0.70f, h * 0.88f, w * 0.50f, h * 0.88f)
                cubicTo(w * 0.30f, h * 0.88f, w * 0.12f, h * 0.80f, w * 0.10f, h * 0.70f)
                close()
            }
            drawPath(fieldBase, color = SowltLeafGreen)

            // 5. Líneas blancas de surcos de cultivo en el campo
            val lineStroke = Stroke(width = w * 0.022f)

            val line1 = Path().apply {
                moveTo(w * 0.12f, h * 0.72f)
                cubicTo(w * 0.25f, h * 0.58f, w * 0.55f, h * 0.58f, w * 0.78f, h * 0.68f)
            }
            drawPath(line1, color = Color.White, style = lineStroke)

            val line2 = Path().apply {
                moveTo(w * 0.20f, h * 0.78f)
                cubicTo(w * 0.32f, h * 0.64f, w * 0.58f, h * 0.63f, w * 0.76f, h * 0.72f)
            }
            drawPath(line2, color = Color.White, style = lineStroke)

            val line3 = Path().apply {
                moveTo(w * 0.32f, h * 0.84f)
                cubicTo(w * 0.42f, h * 0.72f, w * 0.62f, h * 0.70f, w * 0.72f, h * 0.78f)
            }
            drawPath(line3, color = Color.White, style = lineStroke)
        }

        if (showText) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Sowlt",
                fontSize = (size.value * 0.28f).sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
                color = SowltDarkGreen,
                letterSpacing = (-0.5).sp
            )
        }
    }
}
