package com.example.farmealotu.ui.screens.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.farmealotu.ui.components.SowltLogo
import com.example.farmealotu.ui.theme.FarmealoTuTheme
import kotlinx.coroutines.delay

/**
 * Pantalla de bienvenida (Splash Screen) con fondo blanco puro y logo Sowlt centrado.
 * Aparece al abrir la aplicación durante 1.8 segundos.
 */
@Composable
fun SplashScreen(
    onFinished: () -> Unit
) {
    val scale = remember { Animatable(0.85f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        alpha.animateTo(1f, animationSpec = tween(600))
        scale.animateTo(1f, animationSpec = tween(600))
        delay(1200)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        SowltLogo(
            modifier = Modifier
                .scale(scale.value)
                .alpha(alpha.value),
            size = 200.dp,
            showText = true
        )
    }
}

@Preview(showBackground = true, name = "Splash Screen - Sowlt")
@Composable
fun SplashScreenPreview() {
    FarmealoTuTheme {
        SplashScreen(onFinished = {})
    }
}
