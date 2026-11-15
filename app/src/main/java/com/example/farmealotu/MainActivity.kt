    package com.example.farmealotu

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.farmealotu.ui.navigation.FarmealoTuNavHost
import com.example.farmealotu.ui.theme.FarmealoTuTheme
//xd
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Barra de estado: íconos claros sobre la barra superior verde (en ambos temas).
        // Barra de navegación: sigue al tema del teléfono, igual que la app.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        // Sin la franja de contraste del sistema: la barra de navegación de la app ya llega hasta abajo.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) window.isNavigationBarContrastEnforced = false
        val container = (application as FarmealoTuApp).container
        setContent {
            FarmealoTuTheme {
                FarmealoTuNavHost(settings = container.settings)
            }
        }
    }
}
