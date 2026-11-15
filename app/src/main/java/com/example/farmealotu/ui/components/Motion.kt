package com.example.farmealotu.ui.components

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

/**
 * Lenguaje de movimiento de FarmealoTu (Material 3, superficie «Operate»): el movimiento explica
 * continuidad (adónde fui, de dónde vuelvo) y confirma acciones; nunca hace esperar.
 *
 * - Curvas de Material 3: «emphasized decelerate» para llegar, «emphasized accelerate» para salir.
 * - Duraciones cortas: 150 ms respuesta, 300 ms cambio de estado, 400 ms entre pantallas.
 * - Si el celular tiene las animaciones desactivadas (Opciones de desarrollador / Accesibilidad →
 *   «Quitar animaciones»), Compose escala estas duraciones a 0: todo cambia sin movimiento.
 */
object FarmMotion {
    val EmphasizedDecelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    val EmphasizedAccelerate: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
    val Standard: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    const val SHORT = 150
    const val MEDIUM = 300
    const val LONG = 400

    /** Momento principal: las barras de puntaje crecen hasta su valor. */
    const val GROW = 700

    fun <T> enter(duration: Int = MEDIUM, delay: Int = 0): FiniteAnimationSpec<T> =
        tween(duration, delay, EmphasizedDecelerate)

    fun <T> exit(duration: Int = SHORT): FiniteAnimationSpec<T> = tween(duration, 0, EmphasizedAccelerate)

    /** Desplazamiento corto del eje compartido (Material «shared axis X»): 30 dp, no la pantalla entera. */
    private val SHIFT = 30.dp

    private fun shift(density: Density): Int = with(density) { SHIFT.roundToPx() }

    /** Avanzar: lo nuevo entra desde la derecha; lo anterior se va a la izquierda y se desvanece. */
    fun forwardEnter(density: Density): EnterTransition =
        slideInHorizontally(enter(LONG)) { shift(density) } + fadeIn(enter(LONG, delay = 60))

    fun forwardExit(density: Density): ExitTransition =
        slideOutHorizontally(exit(MEDIUM)) { -shift(density) } + fadeOut(exit(SHORT))

    /** Volver: el recorrido inverso, para que «atrás» se sienta como atrás. */
    fun backEnter(density: Density): EnterTransition =
        slideInHorizontally(enter(LONG)) { -shift(density) } + fadeIn(enter(LONG, delay = 60))

    fun backExit(density: Density): ExitTransition =
        slideOutHorizontally(exit(MEDIUM)) { shift(density) } + fadeOut(exit(SHORT))

    /** Cambio de pestaña del menú de abajo: fundido «fade through», sin desplazamiento (no hay jerarquía). */
    val tabEnter: EnterTransition = fadeIn(enter(MEDIUM, delay = 70))
    val tabExit: ExitTransition = fadeOut(exit(90))

    /** Abrir / cerrar un desplegable: crece hacia abajo; al cerrar se va más rápido de lo que llegó. */
    val revealEnter: EnterTransition =
        expandVertically(enter(MEDIUM), expandFrom = androidx.compose.ui.Alignment.Top) + fadeIn(enter(MEDIUM, delay = 60))
    val revealExit: ExitTransition =
        shrinkVertically(exit(200), shrinkTowards = androidx.compose.ui.Alignment.Top) + fadeOut(exit(120))

    /** Paso de las 5 preguntas: eje compartido en la dirección del avance. */
    fun <S : Comparable<S>> AnimatedContentTransitionScope<S>.stepTransform(density: Density): ContentTransform {
        val forward = targetState > initialState
        val enterT = if (forward) forwardEnter(density) else backEnter(density)
        val exitT = if (forward) forwardExit(density) else backExit(density)
        return (enterT togetherWith exitT).using(SizeTransform(clip = false))
    }
}

/**
 * Fracción animada para las barras de puntaje: la primera vez crece desde 0 hasta su valor (el momento
 * principal de la app: ver cómo se separan las opciones); si el valor cambia (nuevas prioridades), se
 * desliza desde el valor anterior. El dato final es siempre el mismo que se dibujaba antes.
 */
@Composable
fun animatedFraction(target: Float, delayMillis: Int = 0): Float {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(target) {
        anim.animateTo(target, tween(FarmMotion.GROW, delayMillis, FarmMotion.EmphasizedDecelerate))
    }
    return anim.value
}
