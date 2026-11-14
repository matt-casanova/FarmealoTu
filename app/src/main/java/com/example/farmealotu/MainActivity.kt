package com.example.farmealotu

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.farmealotu.ui.navigation.FarmealoTuNavHost
import com.example.farmealotu.ui.theme.FarmealoTuTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // La app usa siempre tema claro (legibilidad al sol): las barras del sistema también.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        val container = (application as FarmealoTuApp).container
        setContent {
            FarmealoTuTheme {
                FarmealoTuNavHost(settings = container.settings)
            }
        }
    }
}
