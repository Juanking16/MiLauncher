package com.milauncher.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Paleta minimalista premium: morado y negro. */
object Tema {
    val morado = Color(0xFF7C4DFF)
    val moradoOscuro = Color(0xFF4A148C)
    val moradoClaro = Color(0xFFB388FF)
    val negro = Color(0xFF0A0A0F)
    val grisOscuro = Color(0xFF1A1A24)
    val blanco = Color(0xFFF5F5FA)
    val grisTexto = Color(0xFFB0B0C0)

    fun tema(nombre: String) = when (nombre) {
        "negro" -> mapOf("acento" to Color(0xFFB0B0C0), "fondo" to Color(0xFF000000))
        "azul" -> mapOf("acento" to Color(0xFF448AFF), "fondo" to Color(0xFF0A0A12))
        "verde" -> mapOf("acento" to Color(0xFF69F0AE), "fondo" to Color(0xFF0A0F0C))
        else -> mapOf("acento" to morado, "fondo" to negro)
    }
}

@Composable
fun MiLauncherTheme(contenido: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Tema.morado,
            secondary = Tema.moradoClaro,
            background = Tema.negro,
            surface = Tema.grisOscuro,
            onPrimary = Color.White,
            onBackground = Tema.blanco,
            onSurface = Tema.blanco
        ),
        content = contenido
    )
}

/** Fondo con esquinas redondeadas y color acento translúcido. */
@Composable
fun PanelAcento(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Tema.moradoOscuro.copy(alpha = 0.55f))
            .padding(16.dp),
        content = content
    )
}

/** Animación de escala al pulsar/aparecer. */
fun animEntrada(animacionesActivas: Boolean): Float =
    if (animacionesActivas) 1f else 1f

/** Espec de animación estándar del launcher. */
object AnimSpec {
    val cajon = if (false) spring() else tween(280, easing = FastOutSlowInEasing)
    val suave = tween<Float>(220, easing = FastOutSlowInEasing)
}
