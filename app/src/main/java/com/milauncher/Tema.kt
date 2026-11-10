package com.milauncher

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

object Paleta {
    val FondoAmoled = Color(0xFF09090D)
    val Superficie = Color(0xFF24232B)
    val SuperficieSuave = Color(0xFF16151B)
    val AcentoBase = Color(0xFF751BCE)
    val Lavanda = Color(0xFFC9B6F2)
    val Blanco = Color(0xFFF4F2F8)
    val GrisLavanda = Color(0xFFA9A3B8)
    val Peligro = Color(0xFFFF6B6B)
}

data class Tema(val acento: Color, val acentoClaro: Color)

fun crearTema(acento: Color): Tema = Tema(acento, lerp(acento, Color.White, 0.45f))

val LocalTema = staticCompositionLocalOf { crearTema(Paleta.AcentoBase) }

@Composable
fun TemaLauncher(acento: Int, content: @Composable () -> Unit) {
    val tema = remember(acento) { crearTema(Color(acento)) }
    CompositionLocalProvider(LocalTema provides tema) {
        content()
    }
}